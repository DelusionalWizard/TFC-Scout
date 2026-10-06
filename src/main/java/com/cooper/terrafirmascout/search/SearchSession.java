package com.cooper.terrafirmascout.search;
import java.util.concurrent.atomic.*;
import java.util.concurrent.CancellationException;
public final class SearchSession {
    public final AtomicLong tested=new AtomicLong(),pass1=new AtomicLong(),pass2=new AtomicLong(),verified=new AtomicLong();
    public final AtomicReference<SeedResult> best=new AtomicReference<>();
    public volatile String stage="Starting",error="";
    public volatile boolean paused,cancelled,finished;
    private final Object monitor=new Object();
    public void checkpoint() {
        synchronized(monitor) {
            while(paused&&!cancelled) try { monitor.wait(); } catch(InterruptedException e) { Thread.currentThread().interrupt(); throw new CancellationException(); }
        }
        if(cancelled||Thread.currentThread().isInterrupted()) throw new CancellationException();
    }
    public void pause() { paused=true; }
    public void resume() { synchronized(monitor) { paused=false; monitor.notifyAll(); } }
    public void cancel() { cancelled=true; resume(); }
    public void offer(SeedResult r) { best.accumulateAndGet(r,(a,b)->a==null||rank(b)>rank(a)?b:a); }
    private static double rank(SeedResult r) {
        return (com.cooper.terrafirmascout.score.CandidateScorer.allHardVerified(r.evidence(),r.profile())?1000:0)+r.score()
            +r.evidence().values().stream().filter(e->e.state()==com.cooper.terrafirmascout.score.VerificationState.INFERRED).count()*0.001;
    }
}
