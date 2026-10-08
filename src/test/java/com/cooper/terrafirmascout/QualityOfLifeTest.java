package com.cooper.terrafirmascout;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import com.cooper.terrafirmascout.config.ScoutPrefs;
import com.cooper.terrafirmascout.profile.*;
import com.cooper.terrafirmascout.score.*;
import com.cooper.terrafirmascout.search.*;
class QualityOfLifeTest {
    private static final String FINGERPRINT="abcdef0123456789abcdef";
    private Map<Criterion,Evidence> complete() {
        var map=new EnumMap<Criterion,Evidence>(Criterion.class);
        for(var c:Criterion.values()) map.put(c,new Evidence(VerificationState.VERIFIED,150,1,64,1,1,"Real placement"));
        return map;
    }
    private SeedResult result(long seed,Map<Criterion,Evidence> evidence) { return new SeedResult(seed,10,64,20,FINGERPRINT,ScoutProfile.beginner(),evidence); }
    private SeedResult withMisses(long seed,VerificationState state,Criterion... missed) {
        var map=complete(); for(var c:missed) map.put(c,state==VerificationState.FAILED?Evidence.failed("no"):Evidence.absent("not found")); return result(seed,map);
    }

    // --- preset names ---
    @Test void presetsUseTheNewNamesAndKeepTheirIds() {
        assertEquals("Dream Start",SeedQuality.GOD.label); assertEquals("Easy Start",SeedQuality.GOOD.label); assertEquals("Fair Start",SeedQuality.AVERAGE.label);
        assertEquals("Rugged Start",SeedQuality.HARD.label); assertEquals("Wilderness Start",SeedQuality.SUPER_HARD.label);
        assertEquals("god",SeedQuality.GOD.id); assertEquals("good",SeedQuality.GOOD.id); assertEquals("average",SeedQuality.AVERAGE.id);
        assertEquals("hard",SeedQuality.HARD.id); assertEquals("super_hard",SeedQuality.SUPER_HARD.id);
        for(var q:SeedQuality.values()) assertEquals(q,ScoutProfile.preset(q).quality(),q.name());
    }
    @Test void oldPresetNamesStillLoadAsTheSamePresets() {
        assertEquals(SeedQuality.GOD,SeedQuality.fromName("God")); assertEquals(SeedQuality.GOOD,SeedQuality.fromName("Good")); assertEquals(SeedQuality.AVERAGE,SeedQuality.fromName("Average"));
        assertEquals(SeedQuality.HARD,SeedQuality.fromName("Hard")); assertEquals(SeedQuality.SUPER_HARD,SeedQuality.fromName("Super Hard")); assertEquals(SeedQuality.SUPER_HARD,SeedQuality.fromName("super hard"));
        assertEquals(SeedQuality.GOOD,SeedQuality.fromName("Balanced God Seed")); assertEquals(SeedQuality.GOOD,SeedQuality.fromName("balanced"));
        for(var q:SeedQuality.values()) { assertEquals(q,SeedQuality.fromName(q.label)); assertEquals(q,SeedQuality.fromName(q.id)); assertEquals(q,SeedQuality.fromName(q.label.toUpperCase())); }
    }
    @Test void savedSeedsFromOlderVersionsShowTheCurrentName() {
        var oldNamed=new ScoutProfile("Super Hard",80,12000,-20,-10,-3,0,50,75,200,300,0.25,1000,1500,1000,100,10,true,ScoutProfile.preset(SeedQuality.SUPER_HARD).distances(),12);
        assertEquals("Wilderness Start",oldNamed.displayName()); assertEquals(SeedQuality.SUPER_HARD,oldNamed.quality());
        for(var q:SeedQuality.values()) assertEquals(q.label,ScoutProfile.preset(q).displayName());
    }

    // --- early stop and close calls ---
    @Test void remainingChecksAreOnlySkippedAfterASecondMissAndOnlyWhenTheySafelyCannotMatter() {
        var p=ScoutProfile.beginner(); var map=complete(); var required=List.copyOf(p.requiredCriteria());
        var confirmedBest=new SearchSession(); confirmedBest.offer(result(1,complete()));
        var one=EnumSet.of(required.get(0)); var two=EnumSet.of(required.get(0),required.get(1));
        map.put(required.get(0),Evidence.absent("x")); map.put(required.get(1),Evidence.absent("x"));
        assertFalse(CandidateScorer.remainingChecksCannotMatter(map,p,one,confirmedBest.bestRank()),"one miss may still be a close call");
        assertTrue(CandidateScorer.remainingChecksCannotMatter(map,p,two,confirmedBest.bestRank()),"two misses can never beat a confirmed match");
        assertFalse(CandidateScorer.remainingChecksCannotMatter(map,p,two,new SearchSession().bestRank()),"nothing shown yet: never skip");
    }
    @Test void aCloseCallMissedExactlyOneCheckThatOnlyRanOutOfBudget() {
        assertTrue(SearchSession.isCloseCall(withMisses(1,VerificationState.INFERRED,Criterion.TIN)));
        assertFalse(SearchSession.isCloseCall(withMisses(2,VerificationState.INFERRED,Criterion.TIN,Criterion.CLAY)),"two misses");
        assertFalse(SearchSession.isCloseCall(withMisses(3,VerificationState.FAILED,Criterion.TIN)),"shown not to match: a closer look cannot help");
        assertFalse(SearchSession.isCloseCall(result(4,complete())),"a confirmed match is not a close call");
        var missingRow=complete(); missingRow.remove(Criterion.TIN); assertFalse(SearchSession.isCloseCall(result(5,missingRow)),"a row never reached is not a budget miss"); assertDoesNotThrow(()->new SearchSession().addCloseCall(result(5,missingRow)));
    }
    @Test void sessionKeepsOnlyConfirmedMatchesAndTheBestCloseCalls() {
        var session=new SearchSession();
        session.addMatch(result(1,complete())); session.addMatch(result(1,complete())); session.addMatch(result(2,complete()));
        assertEquals(List.of(1L,2L),session.matches().stream().map(SeedResult::seed).toList(),"no duplicates, oldest first");
        assertThrows(IllegalArgumentException.class,()->session.addMatch(withMisses(3,VerificationState.INFERRED,Criterion.TIN)),"an unconfirmed result can never be a match");
        session.addCloseCall(result(9,complete())); session.addCloseCall(withMisses(10,VerificationState.INFERRED,Criterion.TIN,Criterion.CLAY));
        assertTrue(session.closeCalls().isEmpty(),"only real close calls are kept");
        var misses=List.of(Criterion.TIN,Criterion.FLUX,Criterion.GRAPHITE,Criterion.KAOLIN,Criterion.CLAY,Criterion.FOREST);
        for(int i=0;i<SearchSession.MAX_CLOSE_CALLS+5;i++) session.addCloseCall(withMisses(100+i,VerificationState.INFERRED,misses.get(i%misses.size())));
        assertEquals(SearchSession.MAX_CLOSE_CALLS,session.closeCalls().size());
        var scores=session.closeCalls().stream().map(SeedResult::score).toList(); assertEquals(scores.stream().sorted(Comparator.reverseOrder()).toList(),scores,"best first");
        session.removeCloseCall(session.closeCalls().getFirst().seed()); assertEquals(SearchSession.MAX_CLOSE_CALLS-1,session.closeCalls().size());
    }
    @Test void aCloseCallIsNeverSelectableNoMatterItsScore() {
        for(var q:SeedQuality.values()) {
            var p=ScoutProfile.preset(q);
            for(var c:p.requiredCriteria()) {
                var map=complete(); map.put(c,Evidence.absent("not found"));
                var r=new SeedResult(1,0,64,0,FINGERPRINT,p,map);
                assertFalse(r.selectable(FINGERPRINT),q+" "+c);
                assertThrows(IllegalArgumentException.class,()->new SearchSession().addMatch(r));
            }
        }
    }

    // --- timing and limits ---
    @Test void elapsedTimeDoesNotCountPausedTime() throws Exception {
        var session=new SearchSession(); Thread.sleep(60); session.pause(); long atPause=session.elapsedNanos(); Thread.sleep(250);
        assertTrue(session.elapsedNanos()-atPause<100_000_000L,"time frozen while paused"); session.resume(); Thread.sleep(40);
        long after=session.elapsedNanos(); assertTrue(after>=atPause&&after<atPause+200_000_000L,"paused time excluded after resume");
    }
    @Test void elapsedTimeStopsWhenTheSearchFinishes() throws Exception {
        var session=new SearchSession(); Thread.sleep(50); session.finish(); long first=session.elapsedNanos(); Thread.sleep(150);
        assertTrue(session.finished); assertEquals(first,session.elapsedNanos(),"clock frozen at the end"); session.finish(); assertEquals(first,session.elapsedNanos(),"finishing twice changes nothing");
    }
    @Test void limitsNeverGoNegative() {
        var l=new SearchLimits(-3,-1,-9); assertEquals(0,l.workers()); assertEquals(0,l.stopAfterMatches()); assertEquals(0,l.stopAfterMinutes());
    }
    @Test void searchSpeedMapsToWorkerCounts() {
        assertEquals(1,ScoutPrefs.workers("low",4,24)); assertEquals(4,ScoutPrefs.workers("normal",4,24)); assertEquals(7,ScoutPrefs.workers("high",4,8));
        assertEquals(8,ScoutPrefs.workers("high",4,64),"capped"); assertEquals(4,ScoutPrefs.workers("high",4,2),"never below the normal setting");
        assertEquals(3,ScoutPrefs.workers("surprise",3,24)); assertEquals(3,ScoutPrefs.workers(null,3,24)); assertEquals(1,ScoutPrefs.workers("high",1,1));
    }

    // --- remembered settings ---
    @Test void settingsRoundTripAndSurviveDamagedFiles(@TempDir Path dir) throws Exception {
        var file=dir.resolve("terrafirmascout").resolve("prefs.json"); var prefs=new ScoutPrefs();
        prefs.preset="hard"; prefs.speed="high"; prefs.stopAfterMatches=3; prefs.stopAfterMinutes=20; prefs.matchSound=false; prefs.minScore.put("hard",70); prefs.radius.put("hard",9000);
        prefs.save(file); var loaded=ScoutPrefs.load(file);
        assertEquals("hard",loaded.preset); assertEquals("high",loaded.speed); assertEquals(3,loaded.stopAfterMatches); assertEquals(20,loaded.stopAfterMinutes);
        assertFalse(loaded.matchSound); assertEquals(70,loaded.minScore.get("hard")); assertEquals(9000,loaded.radius.get("hard"));
        Files.writeString(file,"{ this is not json"); assertEquals("god",ScoutPrefs.load(file).preset,"damaged file gives defaults");
        Files.writeString(file,"{\"speed\":\"warp\",\"stopAfterMatches\":-4,\"stopAfterMinutes\":99999,\"radius\":{\"x\":5}}");
        var clean=ScoutPrefs.load(file); assertEquals("normal",clean.speed); assertEquals(0,clean.stopAfterMatches); assertEquals(999,clean.stopAfterMinutes); assertTrue(clean.radius.isEmpty());
        assertEquals("god",ScoutPrefs.load(dir.resolve("missing.json")).preset);
    }

    // --- sharing ---
    @Test void reportsHideLocationsUnlessRevealed() {
        var r=result(123456,complete()); var hidden=ReportText.text(r,false); var shown=ReportText.text(r,true);
        assertTrue(hidden.contains("Seed: 123456")); assertTrue(hidden.contains("Dream Start")); assertTrue(hidden.contains("Confirmed"));
        assertFalse(hidden.contains("X=")); assertFalse(hidden.contains("Start: ")); assertTrue(shown.contains("X=10 Z=20")); assertTrue(shown.contains("X=1 Y=64 Z=1"));
        assertTrue(hidden.contains("within 200 m"),"distance is rounded up to a bucket, not exact");
        var unconfirmed=ReportText.text(withMisses(5,VerificationState.INFERRED,Criterion.TIN),false); assertTrue(unconfirmed.contains("Tin: Not confirmed")); assertTrue(unconfirmed.contains("Not confirmed yet"));
    }

    // --- spawn rock choices ---
    @Test void spawnRockPickerKeepsRocksAndDropsSlabsStairsAndWalls() {
        var paths=List.of("rock/raw/andesite","rock/raw/andesite_slab","rock/raw/andesite_stairs","rock/raw/andesite_wall","rock/raw/basalt","rock/raw/chalk","rock/raw/chalk_wall","rock/raw/claystone",
            "rock/bricks/andesite","rock/hardened/basalt","dirt/silt","rock/raw/");
        assertEquals(List.of("andesite","basalt","chalk","claystone"),com.cooper.terrafirmascout.profile.RockChoices.spawnRocks(paths));
        assertEquals(List.of("odd_wall"),com.cooper.terrafirmascout.profile.RockChoices.spawnRocks(List.of("rock/raw/odd_wall")),"a rock that only looks like a variant is kept when no base rock exists");
        assertEquals(List.of(),com.cooper.terrafirmascout.profile.RockChoices.spawnRocks(List.of()));
    }

    // --- saved seeds: notes, delete, order ---
    @Test void savedSeedsCanBeNotedAndDeletedWithoutTouchingOthers(@TempDir Path dir) throws Exception {
        ResultHistory.useRoot(dir);
        try {
            var a=result(11,complete()); var b=result(22,complete()); ResultHistory.save(a); ResultHistory.save(b);
            assertEquals(2,ResultHistory.load().size()); assertEquals("",ResultHistory.note(a));
            ResultHistory.setNote(a,"  build by the river  "); assertEquals("build by the river",ResultHistory.note(a)); assertEquals("",ResultHistory.note(b));
            ResultHistory.setNote(a,"x".repeat(100)); assertEquals(60,ResultHistory.note(a).length());
            ResultHistory.setNote(b,"keep me"); ResultHistory.delete(a);
            assertEquals(List.of(22L),ResultHistory.load().stream().map(SeedResult::seed).toList()); assertEquals("",ResultHistory.note(a)); assertEquals("keep me",ResultHistory.note(b));
            ResultHistory.setNote(b,""); assertEquals("",ResultHistory.note(b));
            ResultHistory.delete(a); // already gone: no error
            assertTrue(Files.isDirectory(dir.resolve("history")));
        } finally { ResultHistory.useRoot(null); }
    }
    @Test void oldSavedSeedsKeepWorkingAfterTheRename(@TempDir Path dir) throws Exception {
        ResultHistory.useRoot(dir);
        try {
            var current=ScoutProfile.preset(SeedQuality.GOOD);
            var old=new ScoutProfile("Good",current.minScore(),current.radius(),current.temperatureMin(),current.temperatureIdealMin(),current.temperatureIdealMax(),current.temperatureMax(),current.rainfallMin(),current.rainfallIdealMin(),
                current.rainfallIdealMax(),current.rainfallMax(),current.minimumLand(),current.landRadius(),current.terrainRadius(),current.campRadius(),current.minimumCopperUnits(),current.minimumCopperPieces(),true,current.distances(),current.minimumRoughness());
            var saved=new SeedResult(77,0,64,0,FINGERPRINT,old,complete()); ResultHistory.save(saved);
            var loaded=ResultHistory.load(); assertEquals(1,loaded.size()); assertEquals("Easy Start",loaded.getFirst().profile().displayName()); assertEquals(SeedQuality.GOOD,loaded.getFirst().profile().quality());
            assertTrue(loaded.getFirst().selectable(FINGERPRINT));
        } finally { ResultHistory.useRoot(null); }
    }

    // --- saved seeds across game sessions ---
    @Test void savedSeedsKeepTheirFileNoteAndDeleteAcrossSessions(@TempDir Path dir) throws Exception {
        ResultHistory.useRoot(dir);
        try {
            var saved=ResultHistory.save(result(5,complete()));
            // A file named the way 0.2.9 and earlier named it (by an unstable hash): it must still be found, noted and deleted after a restart.
            var oldName=dir.resolve("history").resolve("5-deadbeef-"+FINGERPRINT.substring(0,12)+".json"); Files.move(saved,oldName);
            var loaded=ResultHistory.load(); assertEquals(1,loaded.size());
            ResultHistory.setNote(loaded.getFirst(),"near the coast"); assertEquals("near the coast",ResultHistory.note(loaded.getFirst()));
            ResultHistory.delete(loaded.getFirst()); assertFalse(Files.exists(oldName)); assertTrue(ResultHistory.load().isEmpty());
        } finally { ResultHistory.useRoot(null); }
    }
    @Test void profileKeyIsTheSameInEveryGameSession() {
        // Fixed expected values: a key built from identity hash codes (as before) would differ from run to run and fail here on some builds.
        assertEquals("52c88ffcab",ResultHistory.profileKey(ScoutProfile.beginner()));
        assertEquals("39cec49079",ResultHistory.profileKey(ScoutProfile.preset(SeedQuality.AVERAGE)));
        assertNotEquals(ResultHistory.profileKey(ScoutProfile.beginner()),ResultHistory.profileKey(ScoutProfile.preset(SeedQuality.AVERAGE)));
    }
}
