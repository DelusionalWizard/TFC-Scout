package com.cooper.terrafirmascout.search;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import com.cooper.terrafirmascout.config.ScoutConfig;
import com.cooper.terrafirmascout.profile.ScoutProfile;
import com.cooper.terrafirmascout.scanner.*;
import com.cooper.terrafirmascout.tfc.*;
public final class ScoutSearchEngine implements AutoCloseable {
    public final SearchSession session=new SearchSession();
    public volatile String fingerprint="";
    private final ExecutorService coordinator=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"TerraFirmaScout coordinator");t.setDaemon(true);return t;});
    private final ExecutorService verifier=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"TerraFirmaScout verifier");t.setDaemon(true);return t;});
    private final ExecutorService workers;
    private static final long STOP_GRACE_NANOS=TimeUnit.SECONDS.toNanos(5);
    /** Normal scans take about a second; the slowest seen on a normal seed took 3 s, slow river-heavy seeds 40-270 s. */
    private static final long SCAN_LIMIT_NANOS=TimeUnit.SECONDS.toNanos(20);
    private final SearchWorldContext context; private final ScoutProfile profile;
    private final int maxSeeds,finalists,targetChunks,workerCount; private final SearchLimits limits; private volatile String stopReason;
    // Shortlisted seeds waiting for a real-world check. Bounded, so a slow verifier holds back scanning instead of piling up generators.
    private final Object pendingLock=new Object(); private final List<SeedCandidate> pending=new ArrayList<>(); private boolean scanningDone;
    private volatile boolean verifying; private volatile Throwable verifierFailure;
    public ScoutSearchEngine(SearchWorldContext context,ScoutProfile profile) { this(context,profile,SearchLimits.DEFAULT); }
    public ScoutSearchEngine(SearchWorldContext context,ScoutProfile profile,SearchLimits limits) {
        this.limits=limits; this.workerCount=limits.workers()>0?limits.workers():ScoutConfig.WORKERS.get();
        this.context=context; this.profile=context.adapt(profile);
        session.notice=(this.profile.extra().isEmpty()?"":ResourceAvailability.extraNotice(this.profile.extra())+" ")+ResourceAvailability.notice(this.profile.skipped());
        if(TfgBridge.present()) com.cooper.terrafirmascout.TerraFirmaScout.LOGGER.info("Scout: TerraFirmaGreg-Core detected; using its world generation{}",this.profile.extra().isEmpty()?"":" and also requiring "+this.profile.extra());
        if(!this.profile.skipped().isEmpty()) com.cooper.terrafirmascout.TerraFirmaScout.LOGGER.info("Scout: not requiring {} (this world does not generate them)",this.profile.skipped());
        workers=Executors.newFixedThreadPool(workerCount,r->{var t=new Thread(r,"TerraFirmaScout region scanner");t.setDaemon(true);return t;});
        maxSeeds=ScoutConfig.MAX_SEEDS.get(); finalists=ScoutConfig.FINALISTS.get(); targetChunks=ScoutConfig.TARGET_CHUNKS.get();
    }
    public void start() { coordinator.submit(this::run); }
    private void run() {
        try {
            session.stage="Fingerprinting world generation"; fingerprint=WorldgenFingerprint.compute(context);
            var random=new SplittableRandom();
            var completions=new ExecutorCompletionService<SeedCandidate>(workers);
            var verifierDone=verifier.submit(this::verifyLoop);
            int submitted=0,inflight=0;
            while(!session.cancelled&&submitted<maxSeeds) {
                session.checkpoint(); checkVerifier(); checkTimeLimit();
                if(!verifying&&!session.paused) session.stage="Looking for promising starts...";
                while(inflight<workerCount&&submitted<maxSeeds) {
                    long seed=random.nextLong(); submitted++; inflight++;
                    completions.submit(()->scan(seed));
                }
                var candidate=awaitCandidate(completions); inflight--;
                if(candidate!=null) {
                    session.offer(new SeedResult(candidate.adapter().seed,candidate.center().getX(),0,candidate.center().getZ(),fingerprint,profile,candidate.evidence()));
                    enqueue(candidate);
                }
            }
            while(inflight-->0&&!session.cancelled) {
                var candidate=awaitCandidate(completions); if(candidate!=null) enqueue(candidate);
            }
            synchronized(pendingLock) { scanningDone=true; pendingLock.notifyAll(); }
            while(true) {
                session.checkpoint(); checkVerifier(); checkTimeLimit();
                try { verifierDone.get(200,TimeUnit.MILLISECONDS); break; } catch(TimeoutException e) { /* still verifying */ }
            }
            checkVerifier();
            session.stage=session.cancelled?cancelledStage():"Search limit reached. Try different choices or search again.";
        } catch(CancellationException e) { session.stage=cancelledStage();
        } catch(InterruptedException e) { Thread.currentThread().interrupt(); session.stage=cancelledStage();
        } catch(Exception e) {
            var cause=verifierFailure!=null?verifierFailure:e instanceof ExecutionException&&e.getCause()!=null?e.getCause():e;
            if(session.cancelled||cause instanceof CancellationException||cause instanceof InterruptedException) session.stage=cancelledStage();
            else {
                session.error="The search ran into a problem: "+String.valueOf(cause.getMessage()); session.stage="Search stopped";
                com.cooper.terrafirmascout.TerraFirmaScout.LOGGER.error("TerraFirmaScout search failed; no unverifiable result will be selected",cause);
            }
        } finally {
            session.cancel(); /* lets a still-running verifier stop at its next checkpoint; it is never interrupted mid-world */
            workers.shutdownNow(); verifier.shutdown();
            boolean interrupted=Thread.interrupted();
            // On rare seeds TFC's own region generation (river building) runs for minutes and cannot be interrupted. Give scan
            // workers a short grace, then report the search finished: a straggler cleans its per-seed caches on its own thread
            // in scan()'s finally block when it returns, and it is a daemon thread. The verifier gets the same grace: it may be inside a
            // TFC chunk generation that cannot be interrupted, and it closes its temporary world by itself when that returns.
            long grace=System.nanoTime()+STOP_GRACE_NANOS;
            while((!verifier.isTerminated()||!workers.isTerminated())&&System.nanoTime()<grace) {
                try { workers.awaitTermination(200,TimeUnit.MILLISECONDS); verifier.awaitTermination(200,TimeUnit.MILLISECONDS); }
                catch(InterruptedException e) { interrupted=true; }
            }
            if(!workers.isTerminated()||!verifier.isTerminated()) com.cooper.terrafirmascout.TerraFirmaScout.LOGGER.warn("TerraFirmaScout: a scan worker or check is still inside TFC world generation; it will clean up when that call returns");
            coordinator.shutdown(); session.finish(); com.cooper.terrafirmascout.TerraFirmaScout.LOGGER.info("Scout: search finished (\"{}\", {} seeds, {} matches, {} s)",session.stage,session.tested.get(),session.verified.get(),session.elapsedNanos()/1_000_000_000L); if(interrupted) Thread.currentThread().interrupt();
        }
    }
    /** True while a shortlisted seed is being checked in a temporary world. */
    public boolean verifying() { return verifying; }
    private void checkVerifier() { if(verifierFailure!=null) throw new IllegalStateException(verifierFailure); }
    private SeedCandidate awaitCandidate(ExecutorCompletionService<SeedCandidate> completions) throws Exception {
        while(true) { session.checkpoint(); checkVerifier(); checkTimeLimit(); var f=completions.poll(200,TimeUnit.MILLISECONDS); if(f!=null) return f.get(); }
    }
    private void enqueue(SeedCandidate candidate) throws InterruptedException {
        while(true) {
            synchronized(pendingLock) {
                if(pending.size()<finalists) { pending.add(candidate); pendingLock.notifyAll(); return; }
                pendingLock.wait(200);
            }
            session.checkpoint(); checkVerifier();
        }
    }
    /** Runs on the verifier thread: one real-world check at a time, the closest-looking waiting seed first. */
    private void verifyLoop() {
        try {
            while(true) {
                session.checkpoint(); SeedCandidate candidate;
                synchronized(pendingLock) {
                    while(pending.isEmpty()) { if(scanningDone||session.cancelled) return; pendingLock.wait(200); }
                    candidate=Collections.min(pending,Comparator.comparingDouble(c->com.cooper.terrafirmascout.score.CandidateScorer.targetDistance(c.evidence(),profile)));
                    pending.remove(candidate); pendingLock.notifyAll();
                }
                verifyOne(candidate);
            }
        } catch(CancellationException|InterruptedException e) { /* cancelled: the coordinator reports it */
        } catch(Throwable t) { if(!session.cancelled) verifierFailure=t; }
    }
    private void verifyOne(SeedCandidate candidate) throws Exception {
        verifying=true;
        try {
            session.checkpoint(); SeedResult result;
            try { result=new TFCFeatureProbe(context,profile,session,targetChunks).verify(candidate,fingerprint); }
            finally { candidate.adapter().releaseThreadCaches(); }
            session.offer(result);
            if(result.selectable(fingerprint)) {
                ResultHistory.save(result); session.addMatch(result); long found=session.verified.incrementAndGet();
                if(limits.stopAfterMatches()>0) {
                    // The player asked for a number of matches: keep going without pausing until that many are found.
                    if(found>=limits.stopAfterMatches()) { stopEarly("Found "+found+(found==1?" match":" matches")+". Search finished."); session.checkpoint(); }
                    session.stage="Found "+found+" of "+limits.stopAfterMatches()+" matches. Still searching...";
                } else { session.stage="Found a match! Use this seed or keep looking."; session.pause(); session.checkpoint(); }
            } else session.addCloseCall(result);
        } finally { verifying=false; }
    }
    /** Ends the search normally (not as a cancel) with a message for the player. */
    private void stopEarly(String reason) { if(stopReason==null) stopReason=reason; session.cancel(); }
    private String cancelledStage() { var reason=stopReason; return reason!=null?reason:"Cancelled"; }
    private void checkTimeLimit() {
        if(limits.stopAfterMinutes()>0&&session.elapsedNanos()>TimeUnit.MINUTES.toNanos(limits.stopAfterMinutes())) {
            stopEarly("Time limit of "+limits.stopAfterMinutes()+(limits.stopAfterMinutes()==1?" minute":" minutes")+" reached. Search finished."); session.checkpoint();
        }
    }
    private SeedCandidate scan(long seed) {
        session.checkpoint(); session.tested.incrementAndGet();
        var a=new TFCWorldgenAdapter(seed,context.settings(),context.biomes());
        // A seed whose TFC river generation runs for minutes is skipped; Stop also ends a scan inside that generation at once.
        ScanLimit.begin(SCAN_LIMIT_NANOS,()->session.cancelled);
        try {
            var spawn=a.spawnBiome();
            if(!RegionScanner.accepts(a,spawn,profile,session)) return null;
            session.pass1.incrementAndGet();
            var candidate=DetailedScanner.scan(a,spawn,profile,session,context.creation().worldgenLoadContext(),context.copyGenerator());
            if(candidate==null) return null;
            // Require real deterministic centers or region candidates for all costly resources before scheduling chunks.
            for(var c:List.of(com.cooper.terrafirmascout.score.Criterion.FRESHWATER,com.cooper.terrafirmascout.score.Criterion.FOREST,
                com.cooper.terrafirmascout.score.Criterion.COPPER_VEIN,com.cooper.terrafirmascout.score.Criterion.TIN,
                com.cooper.terrafirmascout.score.Criterion.FLUX,com.cooper.terrafirmascout.score.Criterion.GRAPHITE,com.cooper.terrafirmascout.score.Criterion.IRON,com.cooper.terrafirmascout.score.Criterion.COAL,com.cooper.terrafirmascout.score.Criterion.KAOLIN))
                if(profile.requires(c)&&candidate.targets().get(c).isEmpty()) return null;
            session.pass2.incrementAndGet(); return candidate;
        } catch(ScanLimit.Abandoned e) {
            if(!session.cancelled) session.skippedSlow.incrementAndGet();
            return null;
        } finally { ScanLimit.end(); a.releaseThreadCaches(); }
    }
    @Override public void close() { session.cancel(); workers.shutdownNow(); /* The verifier stops cooperatively at its next checkpoint; the coordinator closes its active scratch world cooperatively. */ }
}
