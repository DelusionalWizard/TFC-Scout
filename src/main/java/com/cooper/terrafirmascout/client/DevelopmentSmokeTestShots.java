package com.cooper.terrafirmascout.client;
import java.nio.file.*;
import java.util.*;
import com.cooper.terrafirmascout.profile.*;
import com.cooper.terrafirmascout.score.Criterion;
import com.cooper.terrafirmascout.search.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.core.registries.*;
/** Opt-in development tool (env SCOUT_SHOTS) that plays through a real session and saves screenshots for the mod pages. Excluded from release jars. */
final class DevelopmentSmokeTestShots {
    private DevelopmentSmokeTestShots() {}
    private static void await(java.util.function.BooleanSupplier done,long seconds,String what) throws Exception {
        long deadline=System.nanoTime()+seconds*1_000_000_000L;
        while(!done.getAsBoolean()) { if(System.nanoTime()>deadline) throw new AssertionError("Timed out waiting for "+what); Thread.sleep(100); }
    }
    private static Button button(Screen screen,String startsWith) {
        return screen.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().startsWith(startsWith)).map(w->(Button)w).findFirst().orElseThrow(()->new AssertionError("No button "+startsWith));
    }
    private static void press(Minecraft mc,Screen screen,String label) throws Exception {
        boolean[] active={false}; mc.submit(()-> { var b=button(screen,label); active[0]=b.active; if(b.active) b.onPress(); }).get();
        if(!active[0]) throw new AssertionError("'"+label+"' was greyed out");
    }
    private static void shot(Minecraft mc,Path out,String name) throws Exception {
        mc.submit(()-> { try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(out.resolve(name)); } catch(Exception e) { throw new RuntimeException(e); } }).get();
    }
    private static ScoutSearchEngine engineOf(ScoutWorldCreationScreen screen) throws ReflectiveOperationException {
        var f=ScoutWorldCreationScreen.class.getDeclaredField("engine"); f.setAccessible(true); return (ScoutSearchEngine)f.get(screen);
    }
    private static void setField(ScoutWorldCreationScreen screen,String name,Object value) throws ReflectiveOperationException {
        var f=ScoutWorldCreationScreen.class.getDeclaredField(name); f.setAccessible(true); f.set(screen,value);
    }
    static void run(ScoutWorldCreationScreen mainScreen,CreateWorldScreen creation,Path out) throws Exception {
        Files.createDirectories(out); var mc=Minecraft.getInstance();
        ResultHistory.useRoot(out.resolve("_history")); // a clean saved-seeds list that only holds what this session finds
        try {
            Thread.sleep(6000);
            // 1. Create World, World tab, with the Scout button.
            DevelopmentSmokeTest.holdState(4);
            mc.submit(()-> { mc.setScreen(creation); try { var f=CreateWorldScreen.class.getDeclaredField("tabNavigationBar"); f.setAccessible(true); ((TabNavigationBar)f.get(creation)).selectTab(1,false); } catch(ReflectiveOperationException e) { throw new RuntimeException(e); } }).get();
            Thread.sleep(1800); shot(mc,out,"01-create-world-scout-button.png");
            mc.submit(()->mc.setScreen(mainScreen)).get(); DevelopmentSmokeTest.holdState(2); Thread.sleep(1500);
            // 2. The search screen with a starting style, and a different style.
            shot(mc,out,"02-search-screen-dream-start.png");
            for(int i=0;i<4;i++) { var names=List.of("Dream Start","Easy Start","Fair Start","Rugged Start"); press(mc,mainScreen,names.get(i)); Thread.sleep(300); }
            Thread.sleep(500); shot(mc,out,"03-search-screen-wilderness-start.png");
            mc.submit(()-> { try { setField(mainScreen,"preset","god"); var m=ScoutWorldCreationScreen.class.getDeclaredMethod("updateProfileFields"); m.setAccessible(true); m.invoke(mainScreen); } catch(ReflectiveOperationException e) { throw new RuntimeException(e); } }).get();
            Thread.sleep(500);
            // 3. A real Dream Start search in progress.
            press(mc,mainScreen,"Find a seed"); Thread.sleep(32000); shot(mc,out,"04-searching-dream-start.png");
            press(mc,mainScreen,"Stop"); var first=engineOf(mainScreen); await(()->first.session.finished,60,"Stop"); Thread.sleep(600);
            // 4. A wishlist search that finds real, confirmed matches (and close calls) for the same session.
            var draft=SpecificationDraft.fromPreset(ScoutProfile.preset(SeedQuality.AVERAGE)); draft.required.remove(Criterion.OPEN_GROUND); draft.required.remove(Criterion.TERRAIN); draft.numbers.put("min_score",0d);
            mc.submit(()-> { try { setField(mainScreen,"specification",draft); setField(mainScreen,"preset","specification"); setField(mainScreen,"savedResult",null);
                var m=ScoutWorldCreationScreen.class.getDeclaredMethod("updateProfileFields"); m.setAccessible(true); m.invoke(mainScreen); } catch(ReflectiveOperationException e) { throw new RuntimeException(e); } }).get();
            Thread.sleep(500); press(mc,mainScreen,"Find a seed");
            await(()-> { try { var e=engineOf(mainScreen); return e!=null&&e!=first&&e.session.tested.get()>0; } catch(Exception x) { return false; } },60,"the wishlist search to start");
            var search=engineOf(mainScreen); long began=System.nanoTime(); int shown=0;
            while(search.session.matches().size()<3&&!search.session.finished) {
                if(search.session.paused&&search.session.matches().size()>shown) {
                    shown=search.session.matches().size();
                    if(shown==1) { Thread.sleep(7000); shot(mc,out,"05-confirmed-match.png"); }
                    if(search.session.matches().size()<3) { Thread.sleep(500); press(mc,mainScreen,"Keep searching"); }
                }
                if(System.nanoTime()-began>720_000_000_000L&&search.session.matches().size()>=2) break; if(System.nanoTime()-began>1_200_000_000_000L) break;
                Thread.sleep(300);
            }
            if(search.session.matches().size()<2) throw new AssertionError("The wishlist search found only "+search.session.matches().size()+" matches in 15 minutes");
            Thread.sleep(7000); // let the match toast go away
            press(mc,mainScreen,"Stop"); await(()->search.session.finished,60,"Stop"); Thread.sleep(800);
            int closeCalls=search.session.closeCalls().size(),matches=search.session.matches().size();
            // 5. Results, Options, Saved seeds.
            press(mc,mainScreen,"Results"); Thread.sleep(1200); shot(mc,out,"06-results-matches-and-close-calls.png"); mc.submit(()->mc.screen.onClose()).get(); Thread.sleep(600);
            press(mc,mainScreen,"Options"); Thread.sleep(1000); shot(mc,out,"07-options.png"); mc.submit(()->mc.screen.onClose()).get(); Thread.sleep(600);
            ResultHistory.setNote(search.session.matches().getFirst(),"flat land east of the river");
            press(mc,mainScreen,"Saved seeds"); Thread.sleep(1200); shot(mc,out,"08-saved-seeds.png"); mc.submit(()->mc.screen.onClose()).get(); Thread.sleep(600);
            // 6. The wishlist editor.
            var biomes=com.cooper.terrafirmascout.tfc.BiomeChoices.forWorld(creation.getUiState().getSettings());
            var rocks=BuiltInRegistries.BLOCK.keySet().stream().filter(id->id.getNamespace().equals("tfc")).map(id->id.getPath()).collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(),RockChoices::spawnRocks));
            var editor=new SpecificationScreen(mainScreen,draft,biomes,rocks,d->{}); mc.submit(()->mc.setScreen(editor)).get(); Thread.sleep(1500);
            String[] tabs={"Resources","Limits","Nearby","Spawn","Rocks","Forest"};
            for(int i=0;i<tabs.length;i++) { final String tab=tabs[i]; if(i>0) mc.submit(()->editor.showTab(tab)).get(); Thread.sleep(900); shot(mc,out,"%02d-wishlist-%s.png".formatted(9+i,tab.toLowerCase())); }
            mc.submit(()->mc.setScreen(mainScreen)).get(); Thread.sleep(800);
            Files.writeString(out.resolve("_notes.txt"),"Wishlist search: "+matches+" confirmed matches and "+closeCalls+" close calls in "+(System.nanoTime()-began)/1_000_000_000L+" s (tested "+search.session.tested+" seeds).\n");
        } finally { ResultHistory.useRoot(null); }
    }
}
