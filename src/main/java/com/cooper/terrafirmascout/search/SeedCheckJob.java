package com.cooper.terrafirmascout.search;
import java.util.concurrent.*;
import com.cooper.terrafirmascout.profile.ScoutProfile;
import com.cooper.terrafirmascout.tfc.SearchWorldContext;
/** One background check of a single seed ("Check this seed", "Look harder"). Never runs alongside a search. */
public final class SeedCheckJob {
    public final SearchSession session=new SearchSession();
    public final long seed; public final String label;
    public volatile SeedResult result; public volatile String error=""; public volatile boolean done;
    private final ExecutorService executor=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"TerraFirmaScout checker");t.setDaemon(true);return t;});
    private final SearchWorldContext context; private final ScoutProfile profile; private final int budget; private final String requiredFingerprint;
    /** @param requiredFingerprint fingerprint the earlier result was found under, or null when any current settings are fine */
    public SeedCheckJob(String label,SearchWorldContext context,ScoutProfile profile,long seed,int budget,String requiredFingerprint) {
        this.label=label; this.context=context; this.profile=profile; this.seed=seed; this.budget=budget; this.requiredFingerprint=requiredFingerprint;
    }
    public void start() {
        executor.submit(()-> {
            try {
                var fingerprint=com.cooper.terrafirmascout.tfc.WorldgenFingerprint.compute(context);
                if(requiredFingerprint!=null&&!requiredFingerprint.equals(fingerprint)) { error="Your world settings changed since this seed was found. Run a search again."; return; }
                var checked=SeedChecker.check(context,profile,seed,fingerprint,budget,session);
                if(checked.selectable(fingerprint)) ResultHistory.save(checked);
                result=checked;
            } catch(CancellationException e) { error="Stopped.";
            } catch(Throwable t) {
                error="Could not check this seed: "+String.valueOf(t.getMessage());
                com.cooper.terrafirmascout.TerraFirmaScout.LOGGER.error("TerraFirmaScout seed check failed",t);
            } finally { done=true; session.finish(); executor.shutdown(); }
        });
    }
    public void cancel() { session.cancel(); }
}
