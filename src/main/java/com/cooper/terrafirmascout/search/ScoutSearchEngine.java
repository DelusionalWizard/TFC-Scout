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
    private final ExecutorService workers;
    private final SearchWorldContext context; private final ScoutProfile profile;
    private final int maxSeeds,finalists,targetChunks;
    public ScoutSearchEngine(SearchWorldContext context,ScoutProfile profile) {
        this.context=context; this.profile=profile;
        workers=Executors.newFixedThreadPool(ScoutConfig.WORKERS.get(),r->{var t=new Thread(r,"TerraFirmaScout region scanner");t.setDaemon(true);return t;});
        maxSeeds=ScoutConfig.MAX_SEEDS.get(); finalists=ScoutConfig.FINALISTS.get(); targetChunks=ScoutConfig.TARGET_CHUNKS.get();
    }
    public void start() { coordinator.submit(this::run); }
    private void run() {
        try {
            session.stage="Fingerprinting world generation"; fingerprint=WorldgenFingerprint.compute(context);
            var random=new SplittableRandom();
            var completions=new ExecutorCompletionService<SeedCandidate>(workers);
            int submitted=0,inflight=0;
            var candidates=new ArrayList<SeedCandidate>();
            while(!session.cancelled&&submitted<maxSeeds) {
                session.checkpoint(); session.stage="Looking for promising starts...";
                while(inflight<ScoutConfig.WORKERS.get()&&submitted<maxSeeds) {
                    long seed=random.nextLong(); submitted++; inflight++;
                    completions.submit(()->scan(seed));
                }
                var candidate=awaitCandidate(completions); inflight--;
                if(candidate!=null) {
                    candidates.add(candidate);
                    session.offer(new SeedResult(candidate.adapter().seed,candidate.center().getX(),0,candidate.center().getZ(),fingerprint,profile,candidate.evidence()));
                }
                if(candidates.size()>=finalists||(submitted==maxSeeds&&inflight==0)) {
                    verifyBatch(candidates); candidates.clear();
                }
            }
            while(inflight-->0&&!session.cancelled) {
                var candidate=awaitCandidate(completions); if(candidate!=null) candidates.add(candidate);
            }
            if(!candidates.isEmpty()&&!session.cancelled) verifyBatch(candidates);
            session.stage=session.cancelled?"Cancelled":"Search limit reached. Try different choices or search again.";
        } catch(CancellationException e) { session.stage="Cancelled";
        } catch(InterruptedException e) { Thread.currentThread().interrupt(); session.stage="Cancelled";
        } catch(Exception e) {
            var cause=e instanceof ExecutionException&&e.getCause()!=null?e.getCause():e;
            if(session.cancelled||cause instanceof CancellationException||cause instanceof InterruptedException) session.stage="Cancelled";
            else {
                session.error="The search ran into a problem: "+String.valueOf(cause.getMessage()); session.stage="Search stopped";
                com.cooper.terrafirmascout.TerraFirmaScout.LOGGER.error("TerraFirmaScout search failed; no unverifiable result will be selected",cause);
            }
        } finally {
            workers.shutdownNow();
            boolean interrupted=Thread.interrupted();
            while(!workers.isTerminated()) {
                try { workers.awaitTermination(200,TimeUnit.MILLISECONDS); }
                catch(InterruptedException e) { interrupted=true; }
            }
            coordinator.shutdown(); session.finished=true; if(interrupted) Thread.currentThread().interrupt();
        }
    }
    private SeedCandidate awaitCandidate(ExecutorCompletionService<SeedCandidate> completions) throws Exception {
        while(true) { session.checkpoint(); var f=completions.poll(200,TimeUnit.MILLISECONDS); if(f!=null) return f.get(); }
    }
    private SeedCandidate scan(long seed) {
        session.checkpoint(); session.tested.incrementAndGet();
        var a=new TFCWorldgenAdapter(seed,context.settings(),context.biomes());
        try {
            var spawn=a.spawnBiome();
            if(!RegionScanner.accepts(a,spawn,profile,session)) return null;
            session.pass1.incrementAndGet();
            var candidate=DetailedScanner.scan(a,spawn,profile,session,context.creation().worldgenLoadContext(),context.copyGenerator());
            if(candidate==null) return null;
            // Require real deterministic centers or region candidates for all costly resources before scheduling chunks.
            for(var c:List.of(com.cooper.terrafirmascout.score.Criterion.FRESHWATER,com.cooper.terrafirmascout.score.Criterion.FOREST,
                com.cooper.terrafirmascout.score.Criterion.COPPER_VEIN,com.cooper.terrafirmascout.score.Criterion.TIN,
                com.cooper.terrafirmascout.score.Criterion.FLUX,com.cooper.terrafirmascout.score.Criterion.GRAPHITE,com.cooper.terrafirmascout.score.Criterion.KAOLIN))
                if(profile.requires(c)&&candidate.targets().get(c).isEmpty()) return null;
            session.pass2.incrementAndGet(); return candidate;
        } finally { a.releaseThreadCaches(); }
    }
    private void verifyBatch(List<SeedCandidate> candidates) throws Exception {
        // Best target proximity first; expensive world generation remains serial to bound memory.
        candidates.sort(Comparator.comparingDouble(c->c.evidence().values().stream().mapToDouble(e->Double.isFinite(e.distance())?e.distance():10000).sum()));
        for(var candidate:candidates) {
            session.checkpoint(); SeedResult result;
            try { result=new TFCFeatureProbe(context,profile,session,targetChunks).verify(candidate,fingerprint); }
            finally { candidate.adapter().releaseThreadCaches(); }
            session.offer(result);
            if(result.selectable(fingerprint)) { ResultHistory.save(result); session.verified.incrementAndGet(); session.stage="Found a match! Use this seed or keep looking."; session.pause(); session.checkpoint(); }
        }
    }
    @Override public void close() { session.cancel(); workers.shutdownNow(); /* The coordinator closes its active scratch world cooperatively. */ }
}
