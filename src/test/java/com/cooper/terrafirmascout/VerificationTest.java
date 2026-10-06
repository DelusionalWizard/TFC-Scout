package com.cooper.terrafirmascout;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.cooper.terrafirmascout.profile.ScoutProfile;
import com.cooper.terrafirmascout.score.*;
import com.cooper.terrafirmascout.search.*;
class VerificationTest {
    private Map<Criterion,Evidence> complete() {
        var map=new EnumMap<Criterion,Evidence>(Criterion.class);
        for(var c:Criterion.values()) map.put(c,new Evidence(VerificationState.VERIFIED,100,1,64,1,1,"Real placement"));
        return map;
    }
    @Test void everyProfileRejectsEachUnverifiedRequirement() {
        for(var q:com.cooper.terrafirmascout.profile.SeedQuality.values()) {
            var p=ScoutProfile.preset(q);
            for(var c:p.requiredCriteria()) {
                var map=complete(); map.put(c,Evidence.inferred(100,1,1,"candidate only"));
                assertFalse(new SeedResult(1,0,64,0,"same",p,map).selectable("same"),q+" "+c);
            }
        }
    }
    @Test void optionalLateResourcesDoNotBlockAverageOrHard() {
        for(var q:List.of(com.cooper.terrafirmascout.profile.SeedQuality.AVERAGE,com.cooper.terrafirmascout.profile.SeedQuality.HARD)) {
            var p=ScoutProfile.preset(q); var map=complete();
            map.put(Criterion.KAOLIN,Evidence.absent("not inspected")); map.put(Criterion.GRAPHITE,Evidence.absent("not inspected"));
            assertTrue(new SeedResult(1,0,64,0,"same",p,map).selectable("same"));
        }
    }
    @Test void challengeProfilesHaveColdClimateAndSuperHardRuggedness() {
        var hard=ScoutProfile.preset(com.cooper.terrafirmascout.profile.SeedQuality.HARD);
        var extreme=ScoutProfile.preset(com.cooper.terrafirmascout.profile.SeedQuality.SUPER_HARD);
        assertEquals(6,hard.temperatureMax()); assertEquals(0,extreme.temperatureMax()); assertEquals(12,extreme.minimumRoughness());
        assertTrue(extreme.requires(Criterion.CHALLENGE));
    }
    @Test void scoreWeightsTotalOneHundred() { assertEquals(100,Arrays.stream(Criterion.values()).mapToInt(c->c.weight).sum()); }
    @Test void noInferredHardRequirementCanSelectEvenAtNinetyFive() {
        for(var c:Criterion.values()) if(c.hard()) {
            var map=complete(); map.put(c,Evidence.inferred(100,1,1,"geology only"));
            assertFalse(new SeedResult(1,0,64,0,"same",ScoutProfile.beginner(),map).selectable("same"),c.name());
        }
    }
    @Test void verifiedResultRequiresMatchingFingerprintAndThreshold() {
        var r=new SeedResult(1,0,64,0,"a",ScoutProfile.beginner(),complete());
        assertTrue(r.selectable("a")); assertFalse(r.selectable("b"));
        var map=complete(); map.put(Criterion.STARTER_COPPER,new Evidence(VerificationState.VERIFIED,750,1,64,1,0.6,"placed"));
        assertFalse(new SeedResult(1,0,64,0,"a",ScoutProfile.beginner(),map).selectable("a"));
    }
    @Test void missingOrFailedEvidenceNeverPasses() {
        var map=complete(); map.remove(Criterion.CLAY); assertFalse(CandidateScorer.allHardVerified(map));
        map.put(Criterion.CLAY,Evidence.failed("no clay")); assertFalse(CandidateScorer.allHardVerified(map));
    }
    @Test void scoringHandlesBoundsAndNonFiniteDistances() {
        assertEquals(0,CandidateScorer.proximity(Double.POSITIVE_INFINITY,100,300));
        assertEquals(0,CandidateScorer.proximity(301,100,300)); assertEquals(1,CandidateScorer.proximity(80,100,300));
        assertEquals(0,CandidateScorer.climate(18,7,10,15,17)); assertEquals(1,CandidateScorer.climate(12,7,10,15,17));
    }
    @Test void cancellationWakesPausedWorkers() throws Exception {
        var session=new SearchSession(); session.pause(); var thrown=new java.util.concurrent.atomic.AtomicBoolean();
        var t=new Thread(()-> { try { session.checkpoint(); } catch(java.util.concurrent.CancellationException e) { thrown.set(true); } });
        t.start(); session.cancel(); t.join(2000); assertFalse(t.isAlive()); assertTrue(thrown.get());
    }
    @Test void balancedAndBeginnerKeepAllResourceDistancesInsideSearchRadius() {
        for(var p:List.of(ScoutProfile.beginner(),ScoutProfile.balanced())) p.distances().values().forEach(d->assertTrue(d<=p.radius()));
    }
}
