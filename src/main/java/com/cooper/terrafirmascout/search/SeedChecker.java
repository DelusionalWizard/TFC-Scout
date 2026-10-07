package com.cooper.terrafirmascout.search;
import java.util.*;
import java.util.concurrent.*;
import com.cooper.terrafirmascout.profile.ScoutProfile;
import com.cooper.terrafirmascout.scanner.DetailedScanner;
import com.cooper.terrafirmascout.score.*;
import com.cooper.terrafirmascout.tfc.*;
/**
 * Runs the same checks a search runs on a shortlisted seed, for one chosen seed and without the shortlist filters.
 * Used for "Check this seed" and "Look harder". A result is only ever confirmed by the same real-world checks.
 */
public final class SeedChecker {
    private SeedChecker() {}
    /** Slow seeds take TFC minutes in its own river generation; a seed the player asked about gets much longer than a search allows. */
    private static final long LIMIT_NANOS=TimeUnit.MINUTES.toNanos(10);
    /** A closer look inspects more chunks per resource than a search does, so it can find what the search's budget missed. */
    public static int closerLookBudget(int searchBudget) { return Math.min(512,Math.max(96,searchBudget*4)); }
    public static SeedResult check(SearchWorldContext context,ScoutProfile profile,long seed,int budget,SearchSession session) throws Exception {
        session.stage="Preparing world settings"; var fingerprint=WorldgenFingerprint.compute(context);
        return check(context,profile,seed,fingerprint,budget,session);
    }
    public static SeedResult check(SearchWorldContext context,ScoutProfile requested,long seed,String fingerprint,int budget,SearchSession session) throws Exception {
        var profile=context.adapt(requested); session.notice=(profile.extra().isEmpty()?"":ResourceAvailability.extraNotice(profile.extra())+" ")+ResourceAvailability.notice(profile.skipped());
        session.checkpoint(); session.stage="Finding the start";
        var adapter=new TFCWorldgenAdapter(seed,context.settings(),context.biomes());
        ScanLimit.begin(LIMIT_NANOS,()->session.cancelled);
        try {
            var spawn=adapter.spawnBiome();
            var candidate=DetailedScanner.scan(adapter,spawn,profile,session,context.creation().worldgenLoadContext(),context.copyGenerator());
            if(candidate==null) return notChecked(seed,spawn.getX(),spawn.getZ(),fingerprint,profile,Criterion.CLIMATE,Evidence.failed("The climate at the start does not fit this starting style"));
            session.stage="Checking in a temporary world";
            return new TFCFeatureProbe(context,profile,session,budget).verify(candidate,fingerprint);
        } catch(ScanLimit.Abandoned e) {
            if(session.cancelled) throw new CancellationException();
            return notChecked(seed,0,0,fingerprint,profile,Criterion.SPAWN,Evidence.failed("TFC took too long to generate this seed's start (over 10 minutes)"));
        } finally { ScanLimit.end(); adapter.releaseThreadCaches(); }
    }
    private static SeedResult notChecked(long seed,int x,int z,String fingerprint,ScoutProfile profile,Criterion failed,Evidence why) {
        var evidence=new EnumMap<Criterion,Evidence>(Criterion.class);
        for(var c:Criterion.values()) evidence.put(c,Evidence.absent(profile.requires(c)?"Not checked":"Not needed for this search"));
        evidence.put(failed,why);
        return new SeedResult(seed,x,0,z,fingerprint,profile,evidence);
    }
}
