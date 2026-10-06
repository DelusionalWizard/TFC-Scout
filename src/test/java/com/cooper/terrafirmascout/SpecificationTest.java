package com.cooper.terrafirmascout;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.cooper.terrafirmascout.profile.*;
import com.cooper.terrafirmascout.score.*;
import com.cooper.terrafirmascout.search.*;
class SpecificationTest {
    private WorldSpecification spec(Set<Criterion> required,boolean all){return new WorldSpecification(true,required,Set.of("tfc:plains","tfc:river"),all,1000,
        Set.of("tfc:plains"),Set.of("granite"),Set.of("grassland"),0,1,60,100,16,8,3,0.5);}
    private ScoutProfile profile(WorldSpecification spec){var p=ScoutProfile.beginner();return new ScoutProfile("Specification",0,4000,7,10,15,17,220,250,350,400,0,1000,250,160,1,1,true,p.distances(),0,spec);}
    @Test void zeroWeightWishlistCanMeetAFullMatchThreshold() {
        var p=profile(spec(Set.of(),false)).withScoreAndRadius(100,4000);
        var m=new EnumMap<Criterion,Evidence>(Criterion.class);
        m.put(Criterion.SPAWN,new Evidence(VerificationState.VERIFIED,0,1,70,2,1,"spawn"));
        m.put(Criterion.SPECIFICATION,new Evidence(VerificationState.VERIFIED,0,1,70,2,1,"matched"));
        var r=new SeedResult(42,1,70,2,"fp",p,m);
        assertEquals(100,r.score());assertTrue(r.selectable("fp"));
        m.put(Criterion.SPECIFICATION,Evidence.absent("not checked"));
        assertEquals(0,new SeedResult(42,1,70,2,"fp",p,m).score());
        assertFalse(new SeedResult(42,1,70,2,"fp",p,m).selectable("fp"));
    }
    @Test void biomeAnyAndAllHaveDifferentSemantics(){assertTrue(spec(Set.of(),false).biomesMatch(Set.of("tfc:plains")));assertFalse(spec(Set.of(),true).biomesMatch(Set.of("tfc:plains")));assertTrue(spec(Set.of(),true).biomesMatch(Set.of("tfc:plains","tfc:river")));}
    @Test void exactSpawnChoicesAndRangesMustAllMatch(){var s=spec(Set.of(),false);assertTrue(s.spawnMatches("tfc:plains","granite","grassland",0,70));
        assertFalse(s.spawnMatches("tfc:plains","chalk","grassland",0,70));assertFalse(s.spawnMatches("tfc:plains","granite","grassland",2,70));assertFalse(s.spawnMatches("tfc:plains","granite","grassland",0,101));}
    @Test void selectedResourcesRemainHardEvenWithZeroScore(){var p=profile(spec(Set.of(Criterion.CLAY),false));var m=new EnumMap<Criterion,Evidence>(Criterion.class);
        m.put(Criterion.SPAWN,new Evidence(VerificationState.VERIFIED,0,1,70,2,1,"spawn"));m.put(Criterion.SPECIFICATION,new Evidence(VerificationState.VERIFIED,0,1,70,2,1,"matched"));
        m.put(Criterion.CLAY,Evidence.absent("not found"));assertFalse(new SeedResult(42,1,70,2,"fp",p,m).selectable("fp"));
        m.put(Criterion.CLAY,new Evidence(VerificationState.VERIFIED,100,100,70,2,1,"clay"));assertTrue(new SeedResult(42,1,70,2,"fp",p,m).selectable("fp"));assertFalse(p.requires(Criterion.TIN));}
    @Test void connectivityCannotBeRequestedWithoutKaolin(){assertThrows(IllegalArgumentException.class,()->spec(Set.of(Criterion.CONNECTIVITY),false));}
    @Test void historyRoundTripPreservesSpecificationAndIncompleteEvidence(){var p=profile(spec(Set.of(),false));var m=new EnumMap<Criterion,Evidence>(Criterion.class);
        m.put(Criterion.SPAWN,new Evidence(VerificationState.VERIFIED,0,1,70,2,1,"spawn"));m.put(Criterion.SPECIFICATION,new Evidence(VerificationState.VERIFIED,0,1,70,2,1,"match"));m.put(Criterion.TIN,Evidence.absent("ignored"));
        var result=new SeedResult(42,1,70,2,"fingerprint",p,m);var encoded=ResultHistory.encode(result);assertFalse(encoded.contains("Infinity"));assertEquals(result,ResultHistory.decode(encoded));
        assertTrue(ResultHistory.decode(encoded).selectable("fingerprint"));var hidden=ResultHistory.report(result,false);assertFalse(hidden.has("spawnX"));assertFalse(hidden.getAsJsonObject("evidence").getAsJsonObject("SPAWN").has("x"));
        assertTrue(ResultHistory.report(result,true).has("spawnX"));}
    @Test void selectedTerrainUsesItsWholeRadiusAndCannotExceedSearch(){
        var spec=new WorldSpecification(true,Set.of(Criterion.TERRAIN),Set.of(),false,1000,Set.of(),Set.of(),Set.of(),0,4,-64,320,16,8,3,.5);
        var p=new ScoutProfile("Specification",0,4000,7,10,15,17,220,250,350,400,0,1000,2000,160,1,1,false,ScoutProfile.beginner().distances(),0,spec);
        assertEquals(2000,p.analysisRadius());assertThrows(IllegalArgumentException.class,()->p.withScoreAndRadius(0,1000));
    }

}
