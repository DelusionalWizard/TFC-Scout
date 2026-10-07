package com.cooper.terrafirmascout.client;
import java.nio.file.Path;
import java.util.*;
import java.util.function.*;
import com.cooper.terrafirmascout.profile.*;
import com.cooper.terrafirmascout.score.*;
import com.cooper.terrafirmascout.search.*;
import com.cooper.terrafirmascout.tfc.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.levelgen.WorldOptions;
/** Opt-in development checks for the quality-of-life features. Excluded from release jars (the name matches the DevelopmentSmokeTest* pattern). */
final class DevelopmentSmokeTestQol {
    private DevelopmentSmokeTestQol() {}
    private static ScoutProfile easyProfile() {
        var draft=SpecificationDraft.fromPreset(ScoutProfile.beginner()); draft.required.clear(); draft.required.add(Criterion.CLIMATE);
        draft.numbers.put("temperature_min",-20d); draft.numbers.put("temperature_ideal_min",-20d); draft.numbers.put("temperature_ideal_max",40d); draft.numbers.put("temperature_max",40d);
        draft.numbers.put("rainfall_min",0d); draft.numbers.put("rainfall_ideal_min",0d); draft.numbers.put("rainfall_ideal_max",500d); draft.numbers.put("rainfall_max",500d);
        draft.numbers.put("min_score",100d);
        return draft.build();
    }
    private static void await(BooleanSupplier done,long seconds,String what) throws Exception {
        long deadline=System.nanoTime()+seconds*1_000_000_000L;
        while(!done.getAsBoolean()) { if(System.nanoTime()>deadline) throw new AssertionError("Timed out waiting for "+what); Thread.sleep(100); }
    }
    private static long scanners() {
        return Thread.getAllStackTraces().keySet().stream().filter(t->t.isAlive()&&t.getName().equals("TerraFirmaScout region scanner")).count();
    }
    private static Set<Criterion> confirmed(SeedResult r) {
        var set=EnumSet.noneOf(Criterion.class); for(var e:r.evidence().entrySet()) if(e.getValue().state()==VerificationState.VERIFIED) set.add(e.getKey()); return set;
    }
    private static void show(Minecraft mc,Screen screen) throws Exception { mc.submit(()->mc.setScreen(screen)).get(); Thread.sleep(1200); }
    private static void shot(Minecraft mc,Path out,String name) throws Exception {
        mc.submit(()-> { try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(out.resolve(name)); } catch(Exception e) { throw new RuntimeException(e); } }).get();
    }
    /** No two visible buttons or boxes may overlap at any window size a player is likely to use. */
    private static void layout(Minecraft mc,Screen screen,String name,StringBuilder report) throws Exception {
        int[][] sizes={{427,240},{480,270},{640,360},{854,480}}; int originalWidth=screen.width,originalHeight=screen.height;
        for(var size:sizes) {
            mc.submit(()-> {
                screen.resize(mc,size[0],size[1]);
                var widgets=screen.children().stream().filter(w->w instanceof AbstractWidget a&&a.visible).map(w->(AbstractWidget)w).toList();
                for(int i=0;i<widgets.size();i++) for(int j=i+1;j<widgets.size();j++) {
                    var a=widgets.get(i); var b=widgets.get(j);
                    if(a.getX()<b.getRight()&&b.getX()<a.getRight()&&a.getY()<b.getBottom()&&b.getY()<a.getBottom())
                        throw new AssertionError(name+" at "+size[0]+"x"+size[1]+": '"+a.getMessage().getString()+"' overlaps '"+b.getMessage().getString()+"'");
                    if(b.getRight()>size[0]||a.getRight()>size[0]||a.getBottom()>size[1]||b.getBottom()>size[1])
                        throw new AssertionError(name+" at "+size[0]+"x"+size[1]+": a control is outside the window ('"+a.getMessage().getString()+"' / '"+b.getMessage().getString()+"')");
                }
            }).get();
        }
        mc.submit(()->screen.resize(mc,originalWidth,originalHeight)).get();
        report.append("  layout ").append(name).append(": no overlaps at 427x240, 480x270, 640x360, 854x480\n");
    }
    private static Button button(Screen screen,String startsWith) {
        return screen.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().startsWith(startsWith)).map(w->(Button)w).findFirst().orElseThrow(()->new AssertionError("No button "+startsWith));
    }
    private static ScoutSearchEngine engineOf(ScoutWorldCreationScreen screen) throws Exception {
        var f=ScoutWorldCreationScreen.class.getDeclaredField("engine"); f.setAccessible(true); return (ScoutSearchEngine)f.get(screen);
    }
    private static void setField(ScoutWorldCreationScreen screen,String name,Object value) throws Exception {
        var f=ScoutWorldCreationScreen.class.getDeclaredField(name); f.setAccessible(true); f.set(screen,value);
    }
    private static void press(Minecraft mc,Screen screen,String label) throws Exception {
        boolean[] active={false};
        mc.submit(()-> { var b=button(screen,label); active[0]=b.active; if(b.active) b.onPress(); }).get();
        if(!active[0]) throw new AssertionError("The '"+label+"' button was greyed out when a player would press it");
    }
    private static boolean isActiveAny(Minecraft mc,Screen screen,String... labels) throws Exception {
        boolean[] active={false}; mc.submit(()->screen.children().stream().filter(w->w instanceof Button b&&Arrays.asList(labels).contains(b.getMessage().getString())).map(w->(Button)w).findFirst().ifPresent(b->active[0]=b.active)).get(); return active[0];
    }
    private static boolean isActive(Minecraft mc,Screen screen,String label) throws Exception {
        boolean[] active={false}; mc.submit(()->active[0]=button(screen,label).active).get(); return active[0];
    }
    /** Stop pressed while a seed is being checked in a temporary world: how long until the search reports finished, and how long Pause takes to bite. */
    private static String stopLatency(SearchWorldContext c) throws Exception {
        var log=new StringBuilder("  stop and pause while a seed is being checked\n"); long worst=0,worstPause=0;
        for(int trial=1;trial<=4;trial++) {
            var engine=new ScoutSearchEngine(c,ScoutProfile.preset(SeedQuality.AVERAGE)); engine.start();
            await(()->engine.verifying()||engine.session.finished,300,"a seed to reach verification");
            if(engine.session.finished) throw new AssertionError("Search ended before verifying anything");
            Thread.sleep(500+trial*700L);
            boolean verifyingAtPause=engine.verifying(); long p0=System.nanoTime(); engine.session.pause();
            // Pause "bites" when the verifier is parked at a checkpoint (or has gone back to waiting for work).
            await(()->engine.session.verifierWaiting||!engine.verifying(),300,"the verifier to pause"); long pauseMs=(System.nanoTime()-p0)/1_000_000;
            engine.session.resume(); Thread.sleep(300);
            boolean verifyingAtStop=engine.verifying(); long t0=System.nanoTime(); engine.close(); await(()->engine.session.finished,300,"Stop to finish"); long ms=(System.nanoTime()-t0)/1_000_000;
            worst=Math.max(worst,ms); worstPause=Math.max(worstPause,pauseMs);
            log.append("    trial ").append(trial).append(": pause took effect in ").append(pauseMs).append(" ms (checking: ").append(verifyingAtPause).append("); stop finished in ").append(ms).append(" ms (checking: ").append(verifyingAtStop).append(")\n");
        }
        log.append("    worst: pause ").append(worstPause).append(" ms, stop ").append(worst).append(" ms\n");
        return log.toString();
    }
    /** The wishlist's spawn-rock choices come from the real block registry: real rocks stay, slab/stairs/wall variants go. */
    private static String spawnRockChoices() {
        var rocks=RockChoices.spawnRocks(net.minecraft.core.registries.BuiltInRegistries.BLOCK.keySet().stream().filter(id->id.getNamespace().equals("tfc")).map(id->id.getPath()).toList());
        for(var name:List.of("granite","diorite","basalt","limestone","chalk","chert","claystone")) if(!rocks.contains(name)) throw new AssertionError("Spawn rock choices lost a real rock: "+name);
        for(var name:rocks) if(name.endsWith("_slab")||name.endsWith("_stairs")||name.endsWith("_wall")) throw new AssertionError("Spawn rock choices still offer a variant: "+name);
        return "  spawn rock choices: "+rocks.size()+" rocks ("+String.join(", ",rocks.subList(0,Math.min(8,rocks.size())))+", ...), no slab, stairs or wall variants\n";
    }
    /** The wishlist's biome choices come from the world's own biome source: only TFC biomes, no vanilla ones. */
    private static String biomeChoices(SearchWorldContext c) {
        var offered=BiomeChoices.forWorld(c.creation());
        int registered=c.creation().worldgenLoadContext().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).keySet().size();
        if(offered.isEmpty()) throw new AssertionError("The biome picker offers no biomes");
        for(var id:offered) if(!id.startsWith("tfc:")) throw new AssertionError("The biome picker offers a non-TFC biome: "+id);
        if(offered.size()>=registered) throw new AssertionError("The biome picker was not filtered ("+offered.size()+" of "+registered+")");
        return "  biome choices: "+offered.size()+" TFC biomes offered out of "+registered+" registered; no vanilla biomes ("+String.join(", ",offered.subList(0,Math.min(5,offered.size())))+", ...)\n";
    }
    /** The buttons a player presses, in the order a player presses them: search, pause, stop, results. */
    private static String uiFlow(ScoutWorldCreationScreen mainScreen,Path out) throws Exception {
        var mc=Minecraft.getInstance(); var log=new StringBuilder("  UI flow\n");
        show(mc,mainScreen);
        // Search, pause, check the search really stops, stop while paused.
        press(mc,mainScreen,"Find a seed"); await(()-> { try { var e=engineOf(mainScreen); return e!=null&&e.session.tested.get()>=3; } catch(Exception x) { return false; } },90,"the search to start");
        var first=engineOf(mainScreen);
        long t0=System.nanoTime(); press(mc,mainScreen,"Pause");
        if(!first.session.paused) throw new AssertionError("Pause did not pause the search");
        Thread.sleep(4000); long a=first.session.tested.get(), v=first.session.verified.get(); Thread.sleep(3000);
        if(first.session.tested.get()!=a) throw new AssertionError("Seeds kept being tested while paused: "+a+" -> "+first.session.tested.get());
        log.append("    pause: froze at ").append(a).append(" seeds; button now reads Resume; stage text: ").append(first.session.stage).append('\n');
        t0=System.nanoTime(); press(mc,mainScreen,"Stop"); await(()->first.session.finished,60,"Stop to finish");
        Thread.sleep(400); log.append("    stop (while paused): finished after ").append((System.nanoTime()-t0)/1_000_000).append(" ms; Find a seed active again: ").append(isActive(mc,mainScreen,"Find a seed")).append('\n');
        // Stop while the search is running.
        Thread.sleep(400); press(mc,mainScreen,"Find a seed"); await(()-> { try { var e=engineOf(mainScreen); return e!=null&&e!=first&&e.session.tested.get()>=3; } catch(Exception x) { return false; } },90,"the second search to start");
        var second=engineOf(mainScreen); if(second==first) throw new AssertionError("Find a seed did not start a new search");
        t0=System.nanoTime(); press(mc,mainScreen,"Stop"); long afterClick=System.nanoTime();
        Thread.sleep(300); String stageSoon=second.session.stage; await(()->second.session.finished,60,"Stop to finish");
        log.append("    stop (while running): finished after ").append((System.nanoTime()-t0)/1_000_000).append(" ms; stage 300 ms after the click: '").append(stageSoon).append("'\n");
        // A search that finds matches, then Results and a click on a result.
        var draft=SpecificationDraft.fromPreset(ScoutProfile.beginner()); draft.required.clear(); draft.required.add(Criterion.CLIMATE);
        draft.numbers.put("temperature_min",-20d); draft.numbers.put("temperature_ideal_min",-20d); draft.numbers.put("temperature_ideal_max",40d); draft.numbers.put("temperature_max",40d);
        draft.numbers.put("rainfall_min",0d); draft.numbers.put("rainfall_ideal_min",0d); draft.numbers.put("rainfall_ideal_max",500d); draft.numbers.put("rainfall_max",500d); draft.numbers.put("min_score",100d);
        mc.submit(()-> { try { setField(mainScreen,"specification",draft); setField(mainScreen,"preset","specification"); } catch(Exception x) { throw new RuntimeException(x); } }).get();
        Thread.sleep(800); press(mc,mainScreen,"Find a seed"); await(()-> { try { var e=engineOf(mainScreen); return e!=null&&e!=second&&e.session.paused; } catch(Exception x) { return false; } },180,"a match to pause the search");
        var third=engineOf(mainScreen); Thread.sleep(300); long testedAtMatch=third.session.tested.get();
        if(!isActive(mc,mainScreen,"Keep searching")) throw new AssertionError("Keep searching is greyed out after a match");
        log.append("    a match paused the search: ").append(third.session.stage).append("; Pause/Resume active: ").append(isActiveAny(mc,mainScreen,"Pause","Resume")).append(", Keep searching active: ").append(isActive(mc,mainScreen,"Keep searching")).append(", Stop is pressable\n");
        press(mc,mainScreen,"Results"); Thread.sleep(600);
        if(!(mc.screen instanceof ScoutResultsScreen)) throw new AssertionError("Results did not open");
        mc.submit(()-> { var row=mc.screen.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().startsWith("Match")).map(w->(Button)w).findFirst().orElseThrow(()->new AssertionError("No match row")); row.onPress(); }).get();
        Thread.sleep(1500);
        if(mc.screen!=mainScreen) throw new AssertionError("Clicking a result did not return to the search screen");
        if(engineOf(mainScreen)!=third) throw new AssertionError("Clicking a result replaced the search (it restarted)");
        if(third.session.tested.get()!=testedAtMatch) throw new AssertionError("Seeds were tested after clicking a result: the search ran again ("+testedAtMatch+" -> "+third.session.tested.get()+")");
        if(!third.session.paused) throw new AssertionError("Clicking a result resumed the search");
        log.append("    clicked a result in Results: back on the search screen, same search, still paused, no new seeds tested\n");
        press(mc,mainScreen,"Stop"); await(()->third.session.finished,60,"Stop to finish after a result click");
        mc.submit(()-> { try { setField(mainScreen,"preset","god"); setField(mainScreen,"savedResult",null); } catch(Exception x) { throw new RuntimeException(x); } }).get();
        log.append("    stop after viewing a result: finished; search screen idle\n");
        // The saved seeds list: clicking a saved seed must not start or change a search.
        Thread.sleep(600); press(mc,mainScreen,"Saved seeds"); Thread.sleep(700);
        if(!(mc.screen instanceof HistoryScreen)) throw new AssertionError("Saved seeds did not open");
        long testedBefore=third.session.tested.get();
        mc.submit(()-> { var row=mc.screen.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().contains(" | match ")).map(w->(Button)w).findFirst().orElseThrow(()->new AssertionError("No saved seed row")); row.onPress(); }).get();
        Thread.sleep(1500);
        if(mc.screen!=mainScreen) throw new AssertionError("Clicking a saved seed did not return to the search screen");
        if(engineOf(mainScreen)!=third||!third.session.finished||third.session.tested.get()!=testedBefore) throw new AssertionError("Clicking a saved seed started or changed a search");
        if(isActive(mc,mainScreen,"Stop")) throw new AssertionError("Stop is active with no search running");
        log.append("    clicked a saved seed: back on the search screen, nothing started, Stop correctly greyed out\n");
        return log.toString();
    }
    static String run(SearchWorldContext c,String fingerprint,ScoutWorldCreationScreen mainScreen,Consumer<ScoutSearchEngine> showEngine,Path out) throws Exception {
        var report=new StringBuilder("Quality-of-life checks\n"); var mc=Minecraft.getInstance(); var easy=easyProfile();
        report.append(uiFlow(mainScreen,out));
        report.append(spawnRockChoices());
        report.append(biomeChoices(c));
        report.append(stopLatency(c));

        // Text seeds follow Minecraft's own rule, and a typed seed can be checked on its own.
        String text="w96jgic4Taen3+cC"; if(WorldOptions.parseSeed(text).getAsLong()!=text.hashCode()) throw new AssertionError("Text seed rule changed");
        var checkJob=new SeedCheckJob("check",c,easy,123456789L,SeedChecker.closerLookBudget(24),null); checkJob.start(); await(()->checkJob.done,300,"seed check");
        if(!checkJob.error.isEmpty()) throw new AssertionError("Seed check failed: "+checkJob.error);
        if(!checkJob.result.selectable(fingerprint)||checkJob.result.score()!=100) throw new AssertionError("Check this seed did not confirm the known fixture: "+checkJob.result.status());
        report.append("  check one seed: seed 123456789 against a climate wishlist: ").append(checkJob.result.status()).append(", match ").append(checkJob.result.score()).append("%\n");
        var stale=new SeedCheckJob("stale",c,easy,123456789L,24,"0".repeat(64)); stale.start(); await(()->stale.done,60,"stale-settings guard");
        if(!stale.error.contains("settings changed")||stale.result!=null) throw new AssertionError("A result from different world settings was not refused");
        report.append("  a closer look refuses results found under different world settings\n");

        // A bigger budget may find what a small one missed, and never loses what it already confirmed.
        long hardSeed=-7538705592502360260L; var hard=ScoutProfile.preset(SeedQuality.HARD);
        var small=SeedChecker.check(c,hard,hardSeed,fingerprint,24,new SearchSession());
        var closer=SeedChecker.check(c,hard,hardSeed,fingerprint,SeedChecker.closerLookBudget(24),new SearchSession());
        if(!confirmed(closer).containsAll(confirmed(small))) throw new AssertionError("A closer look lost a confirmed check: "+confirmed(small)+" vs "+confirmed(closer));
        if(closer.selectable(fingerprint)&&!CandidateScorer.allHardVerified(closer.evidence(),hard)) throw new AssertionError("Selectable without every check confirmed");
        report.append("  closer look on the known Rugged Start seed: ").append(small.status()).append(" at 24 chunks (").append(confirmed(small).size()).append(" confirmed), ")
            .append(closer.status()).append(" at ").append(SeedChecker.closerLookBudget(24)).append(" chunks (").append(confirmed(closer).size()).append(" confirmed); still unconfirmed: ")
            .append(CandidateScorer.unconfirmedRequired(closer.evidence(),hard)).append('\n');

        // Stop after a number of matches: one scanner, no pausing, exactly that many matches.
        var engine1=new ScoutSearchEngine(c,easy,new SearchLimits(1,2,0)); engine1.start(); long maxScanners=0; long began=System.nanoTime();
        while(!engine1.session.finished) {
            maxScanners=Math.max(maxScanners,scanners());
            if(engine1.session.paused) throw new AssertionError("The search paused although a match limit was set");
            if(System.nanoTime()-began>600_000_000_000L) { engine1.close(); throw new AssertionError("Match-limit search did not finish"); }
            Thread.sleep(100);
        }
        var found=engine1.session.matches();
        if(found.size()!=2||engine1.session.verified.get()!=2) throw new AssertionError("Expected 2 matches, got "+found.size()+" / "+engine1.session.verified.get());
        if(!engine1.session.stage.equals("Found 2 matches. Search finished.")) throw new AssertionError("Unexpected final message: "+engine1.session.stage);
        if(maxScanners!=1) throw new AssertionError("Low speed should run one scanner, saw "+maxScanners);
        var saved=ResultHistory.load(); for(var m:found) { if(!m.selectable(fingerprint)) throw new AssertionError("Unconfirmed match listed"); if(saved.stream().noneMatch(s->s.seed()==m.seed())) throw new AssertionError("Match not saved to history"); }
        report.append("  stop after 2 matches (1 scanner): found ").append(found.get(0).seed()).append(" and ").append(found.get(1).seed()).append(" in ").append((System.nanoTime()-began)/1_000_000_000L).append(" s, never paused, both saved\n");

        // Stop after minutes: the search runs until the limit; a pause at a match would not count toward it.
        var engine2=new ScoutSearchEngine(c,ScoutProfile.preset(SeedQuality.AVERAGE),new SearchLimits(2,0,1)); engine2.start(); began=System.nanoTime(); int resumed=0;
        while(!engine2.session.finished) {
            if(engine2.session.paused) { engine2.session.resume(); resumed++; }
            if(System.nanoTime()-began>240_000_000_000L) { engine2.close(); throw new AssertionError("Time-limit search did not finish"); }
            Thread.sleep(100);
        }
        long wall=(System.nanoTime()-began)/1_000_000_000L;
        if(!engine2.session.stage.startsWith("Time limit of 1 minute reached")) throw new AssertionError("Unexpected final message: "+engine2.session.stage);
        if(wall<55||wall>120) throw new AssertionError("Time limit ended after "+wall+" s");
        report.append("  stop after 1 minute: ").append(engine2.session.stage).append(" (").append(wall).append(" s wall, ").append(engine2.session.tested).append(" seeds, ")
            .append(engine2.session.closeCalls().size()).append(" close calls, resumed ").append(resumed).append(" pauses)\n");

        // A close call from the search can get a closer look through the same job the screen uses.
        var close=engine2.session.closeCalls();
        if(!close.isEmpty()) {
            var r=close.getFirst(); var job=new SeedCheckJob("closer",c,r.profile(),r.seed(),SeedChecker.closerLookBudget(24),r.fingerprint()); job.start(); await(()->job.done,600,"closer look");
            if(!job.error.isEmpty()) throw new AssertionError("Closer look failed: "+job.error);
            if(job.result.selectable(fingerprint)&&!CandidateScorer.allHardVerified(job.result.evidence(),r.profile())) throw new AssertionError("Closer look selected an unconfirmed result");
            report.append("  closer look on search close call ").append(r.seed()).append(": ").append(r.status()).append(" -> ").append(job.result.status()).append('\n');
        } else report.append("  no close calls appeared in the 1-minute search (nothing to look harder at)\n");

        // Screens: populate one session with confirmed matches and close calls, then check layout and look at them.
        var session=engine1.session; for(var r:engine2.session.closeCalls()) session.addCloseCall(r);
        if(session.closeCalls().isEmpty()) {
            var map=new EnumMap<Criterion,Evidence>(Criterion.class); map.putAll(found.getFirst().evidence()); map.put(Criterion.CLIMATE,Evidence.absent("Not found in 24 chunks checked; this resource is still unconfirmed"));
            session.addCloseCall(new SeedResult(found.getFirst().seed()+1,0,64,0,fingerprint,easy,map)); report.append("  (screen check used one synthetic close call for layout)\n");
        }
        ResultHistory.setNote(found.getFirst(),"near the river, flat land to the east");
        mc.submit(()-> { try { var f=ScoutWorldCreationScreen.class.getDeclaredField("savedResult"); f.setAccessible(true); f.set(mainScreen,null); } catch(ReflectiveOperationException e) { throw new RuntimeException(e); } }).get();
        showEngine.accept(engine1); show(mc,mainScreen); shot(mc,out,"qol-main.png");
        if(!button(mainScreen,"Results (").getMessage().getString().equals("Results ("+(session.matches().size()+session.closeCalls().size())+")")) throw new AssertionError("Results button count wrong");
        layout(mc,mainScreen,"main screen",report);
        mc.submit(()->button(mainScreen,"Options").onPress()).get(); Thread.sleep(800);
        if(!(mc.screen instanceof ScoutOptionsScreen options)) throw new AssertionError("Options button did not open the options screen");
        if(engine1.session.matches().size()!=2) throw new AssertionError("Opening Options disturbed the session");
        shot(mc,out,"qol-options.png"); layout(mc,options,"options screen",report);
        mc.submit(options::onClose).get(); Thread.sleep(500); if(mc.screen!=mainScreen) throw new AssertionError("Options did not return to the search screen");
        mc.submit(()->button(mainScreen,"Results").onPress()).get(); Thread.sleep(800);
        if(!(mc.screen instanceof ScoutResultsScreen results)) throw new AssertionError("Results button did not open the results screen");
        shot(mc,out,"qol-results.png"); layout(mc,results,"results screen",report);
        mc.submit(results::onClose).get(); Thread.sleep(500);
        var history=new HistoryScreen(mainScreen,r->{}); show(mc,history); shot(mc,out,"qol-saved-seeds.png"); layout(mc,history,"saved seeds screen",report);
        show(mc,new NoteScreen(history,found.getFirst())); shot(mc,out,"qol-note.png");
        show(mc,mainScreen); showEngine.accept(null);
        report.append("  screens: options, results, saved seeds and note open and close; the search was undisturbed\n");
        return report.toString();
    }
}
