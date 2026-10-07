package com.cooper.terrafirmascout.search;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
public final class SearchSession {
    /** Close calls kept for the Results screen; the best ones win when more arrive. */
    public static final int MAX_CLOSE_CALLS=12;
    public final AtomicLong tested=new AtomicLong(),pass1=new AtomicLong(),pass2=new AtomicLong(),verified=new AtomicLong(),skippedSlow=new AtomicLong();
    public final AtomicReference<SeedResult> best=new AtomicReference<>();
    public volatile String stage="Starting",error="",notice="";
    public volatile boolean paused,cancelled,finished,verifierWaiting; private volatile long endedNanos;
    private final Object monitor=new Object();
    private final List<SeedResult> matches=new CopyOnWriteArrayList<>(),closeCalls=new CopyOnWriteArrayList<>();
    private final long startedNanos=System.nanoTime(); private long pausedTotalNanos,pauseStartedNanos;
    public void checkpoint() {
        synchronized(monitor) {
            boolean waited=false,isVerifier=Thread.currentThread().getName().equals("TerraFirmaScout verifier");
            try { while(paused&&!cancelled) try { waited=true; if(isVerifier) verifierWaiting=true; monitor.wait(); } catch(InterruptedException e) { Thread.currentThread().interrupt(); throw new CancellationException(); } }
            finally { if(isVerifier) verifierWaiting=false; }
            if(waited) com.cooper.terrafirmascout.tfc.ScanLimit.restart();
        }
        if(cancelled||Thread.currentThread().isInterrupted()) throw new CancellationException();
    }
    public void pause() { synchronized(monitor) { if(!paused) pauseStartedNanos=System.nanoTime(); paused=true; } }
    public void resume() { synchronized(monitor) { if(paused) pausedTotalNanos+=System.nanoTime()-pauseStartedNanos; paused=false; monitor.notifyAll(); } }
    public void cancel() { cancelled=true; resume(); }
    /** Marks the search over and freezes the elapsed time. */
    public void finish() { synchronized(monitor) { if(endedNanos==0) endedNanos=System.nanoTime(); } finished=true; }
    /** Time spent searching, not counting time spent paused. */
    public long elapsedNanos() {
        synchronized(monitor) { long now=endedNanos!=0?endedNanos:System.nanoTime(); return now-startedNanos-pausedTotalNanos-(paused&&endedNanos==0?now-pauseStartedNanos:0); }
    }
    /** Ranking of the best result so far; never decreases. */
    public double bestRank() { var b=best.get(); return b==null?Double.NEGATIVE_INFINITY:rank(b); }
    public void offer(SeedResult r) { best.accumulateAndGet(r,(a,b)->a==null||rank(b)>rank(a)?b:a); }
    /** Every confirmed match found by this search, oldest first. Only fully confirmed results are ever added. */
    public List<SeedResult> matches() { return List.copyOf(matches); }
    public void addMatch(SeedResult r) {
        if(!r.selectable(r.fingerprint())) throw new IllegalArgumentException("Only confirmed results can be matches");
        if(matches.stream().noneMatch(m->m.seed()==r.seed())) matches.add(r);
    }
    /** Seeds that missed exactly one required check within the inspection budget. Shown for a closer look; never selectable as they are. */
    public List<SeedResult> closeCalls() { return List.copyOf(closeCalls); }
    public synchronized void addCloseCall(SeedResult r) {
        if(!isCloseCall(r)) return;
        closeCalls.removeIf(c->c.seed()==r.seed());
        closeCalls.add(r);
        var sorted=new ArrayList<>(closeCalls); sorted.sort(Comparator.comparingInt(SeedResult::score).reversed());
        closeCalls.clear(); closeCalls.addAll(sorted.subList(0,Math.min(MAX_CLOSE_CALLS,sorted.size())));
    }
    public synchronized void removeCloseCall(long seed) { closeCalls.removeIf(c->c.seed()==seed); }
    /** Exactly one required check is unconfirmed, and that check only ran out of budget (it was not shown to be wrong). */
    public static boolean isCloseCall(SeedResult r) {
        var open=com.cooper.terrafirmascout.score.CandidateScorer.unconfirmedRequired(r.evidence(),r.profile());
        if(open.size()!=1) return false;
        var only=r.evidence().get(open.get(0)); // a row that was never reached is not a budget miss: nothing says a closer look would help
        return only!=null&&only.state()==com.cooper.terrafirmascout.score.VerificationState.INFERRED;
    }
    private static double rank(SeedResult r) {
        return (com.cooper.terrafirmascout.score.CandidateScorer.allHardVerified(r.evidence(),r.profile())?1000:0)+r.score()
            +r.profile().requiredCriteria().stream().map(r.evidence()::get).filter(java.util.Objects::nonNull).filter(e->e.state()==com.cooper.terrafirmascout.score.VerificationState.INFERRED).count()*0.001;
    }
}
