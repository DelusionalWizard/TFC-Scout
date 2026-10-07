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
    @Test void optionalEvidenceCannotChangeCandidateOrdering() {
        var p=ScoutProfile.beginner();var a=complete();var b=complete();
        a.put(Criterion.DIVERSITY,Evidence.inferred(1,1,1,"optional"));
        b.put(Criterion.DIVERSITY,Evidence.failed("optional"));
        assertEquals(CandidateScorer.targetDistance(a,p),CandidateScorer.targetDistance(b,p));
        var session=new SearchSession();
        session.offer(new SeedResult(1,0,64,0,"same",p,b));
        session.offer(new SeedResult(2,0,64,0,"same",p,a));
        assertEquals(1,session.best.get().seed());
        a.put(Criterion.CLAY,new Evidence(VerificationState.VERIFIED,1,1,64,1,1,"closer required resource"));
        assertTrue(CandidateScorer.targetDistance(a,p)<CandidateScorer.targetDistance(b,p));
    }
    @Test void optionalEvidenceCannotChangeScoresOrSelection() {
        for(var q:com.cooper.terrafirmascout.profile.SeedQuality.values()) {
            var p=ScoutProfile.preset(q); var map=complete();
            for(var c:Criterion.values()) if(!p.requires(c)) map.remove(c);
            var requiredOnly=new SeedResult(1,0,64,0,"same",p,map);
            assertEquals(100,requiredOnly.score(),q.name()); assertTrue(requiredOnly.selectable("same"),q.name());
            for(var c:Criterion.values()) if(!p.requires(c)) {
                for(var optional:List.of(Evidence.absent("skipped"),Evidence.failed("outside limits"),
                    new Evidence(VerificationState.VERIFIED,12000,1,64,1,0,"optional"))) {
                    map.put(c,optional);
                    var r=new SeedResult(1,0,64,0,"same",p,map);
                    assertEquals(100,r.score(),q+" "+c); assertTrue(r.selectable("same"),q+" "+c);
                }
            }
        }
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
        var everything=ScoutProfile.beginner().withExtra(Set.of(Criterion.IRON,Criterion.COAL,Criterion.RIVER,Criterion.LAKE,Criterion.COAST,Criterion.CROPS,Criterion.FARMLAND)); // requires every hard criterion
        for(var c:Criterion.values()) if(c.hard()) {
            var map=complete(); map.put(c,Evidence.inferred(100,1,1,"geology only"));
            assertFalse(new SeedResult(1,0,64,0,"same",everything,map).selectable("same"),c.name());
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
    @Test void earlyStopBoundNeverUnderestimatesAPartialResult() {
        // Whatever else is verified, the bound must be at least the real rank, or a result that would have been shown could be skipped.
        for(var q:com.cooper.terrafirmascout.profile.SeedQuality.values()) {
            var p=ScoutProfile.preset(q); var required=List.copyOf(p.requiredCriteria());
            for(int mask=1;mask<(1<<Math.min(required.size(),10));mask++) {
                var map=complete(); var missed=EnumSet.noneOf(Criterion.class);
                for(int i=0;i<required.size()&&i<10;i++) if((mask>>i&1)!=0) { map.put(required.get(i),Evidence.absent("not found")); missed.add(required.get(i)); }
                var session=new SearchSession(); session.offer(new SeedResult(1,0,64,0,"f",p,map));
                // Rows still to be checked count as unconfirmed too, so the bound must hold for those as well.
                assertTrue(CandidateScorer.maxPossibleRank(map,p,missed)>=session.bestRank(),q+" "+missed);
            }
        }
    }
    @Test void earlyStopOnlyWhenBestResultIsAtLeastAsGood() {
        var p=ScoutProfile.beginner(); var empty=new SearchSession();
        var map=complete(); var first=p.requiredCriteria().iterator().next(); map.put(first,Evidence.absent("not found"));
        var missed=EnumSet.of(first);
        assertFalse(CandidateScorer.maxPossibleRank(map,p,missed)<empty.bestRank(),"nothing shown yet: never skip");
        var confirmed=new SearchSession(); confirmed.offer(new SeedResult(1,0,64,0,"f",p,complete()));
        assertTrue(CandidateScorer.maxPossibleRank(map,p,missed)<confirmed.bestRank(),"a confirmed result beats any seed with a miss");
        var weak=new SearchSession(); var weakMap=complete(); for(var c:p.requiredCriteria()) weakMap.put(c,Evidence.absent("none"));
        weak.offer(new SeedResult(2,0,64,0,"f",p,weakMap));
        assertFalse(CandidateScorer.maxPossibleRank(map,p,missed)<weak.bestRank(),"a better partial could still be shown");
    }
    @Test void balancedAndBeginnerKeepAllResourceDistancesInsideSearchRadius() {
        for(var p:List.of(ScoutProfile.beginner(),ScoutProfile.balanced())) p.distances().values().forEach(d->assertTrue(d<=p.radius()));
    }
}
