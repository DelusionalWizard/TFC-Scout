package com.cooper.terrafirmascout.client;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import com.cooper.terrafirmascout.config.ScoutConfig;
import com.cooper.terrafirmascout.config.ScoutPrefs;
import com.cooper.terrafirmascout.mixin.CreateWorldAccess;
import com.cooper.terrafirmascout.profile.*;
import com.cooper.terrafirmascout.score.*;
import com.cooper.terrafirmascout.search.*;
import com.cooper.terrafirmascout.tfc.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.levelgen.WorldOptions;
public final class ScoutWorldCreationScreen extends Screen {
    private static final List<String> MODES=List.of("god","good","average","hard","super_hard","custom","specification");
    private final CreateWorldScreen parent; private final ScoutPrefs prefs=ScoutPrefs.load();
    private String preset,message="Pick a starting style, or tell Scout what you want.";
    private ScoutSearchEngine engine; private SeedCheckJob job; private boolean reveal,busy; private int scroll; private long announcedMatches;
    private EditBox score,radius; private Button use,pause,keep,find,stop,profile,custom,history,export,copySeed,copyReport,options,results; private SeedResult savedResult;
    private Map<String,Double> customValues; private SpecificationDraft specification;
    public ScoutWorldCreationScreen(CreateWorldScreen parent) {
        super(Component.literal("TerraFirmaScout")); this.parent=parent; reveal=ScoutConfig.REVEAL.get();
        preset=MODES.contains(prefs.preset)?prefs.preset:"god";
    }
    @Override protected void init() {
        int content=Math.min(500,width-16),left=(width-content)/2,center=width/2,half=(content-8)/2;
        profile=addRenderableWidget(Button.builder(Component.literal(profileName()),b->{
            preset=MODES.get((MODES.indexOf(preset)+1)%MODES.size()); updateProfileFields();
        }).bounds(left,32,half,20).build());
        custom=addRenderableWidget(Button.builder(Component.literal("Pick your world"),b->{
            try {
                if(specification==null)specification=SpecificationDraft.load(selectedProfile());
                var draft=new com.google.gson.GsonBuilder().create().fromJson(new com.google.gson.GsonBuilder().create().toJson(specification),SpecificationDraft.class);
                var biomes=BiomeChoices.forWorld(parent.getUiState().getSettings());
                var rocks=net.minecraft.core.registries.BuiltInRegistries.BLOCK.keySet().stream().filter(id->id.getNamespace().equals("tfc")).map(id->id.getPath()).collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(),RockChoices::spawnRocks));
                minecraft.setScreen(new SpecificationScreen(this,draft,biomes,rocks,values->{specification=values;preset="specification";}));
            }catch(Exception e){message="Could not open your settings: "+e.getMessage();}
        }).bounds(left+half+8,32,Math.max(60,half-92),20).build());
        history=addRenderableWidget(Button.builder(Component.literal("Saved seeds"),b->minecraft.setScreen(new HistoryScreen(this,result->{savedResult=result;com.cooper.terrafirmascout.TerraFirmaScout.LOGGER.info("Scout: saved seed {} picked (search running={})",result.seed(),running());message="Loaded a "+result.profile().displayName()+" seed. Scout will check your world settings before using it.";})))
            .bounds(left+content-86,32,86,20).build());
        score=new EditBox(font,left+68,58,44,18,Component.literal("Minimum score")); score.setFilter(v->v.matches("[0-9]{0,3}")); addRenderableWidget(score);
        radius=new EditBox(font,left+171,58,60,18,Component.literal("Search radius")); radius.setFilter(v->v.matches("[0-9]{0,5}")); addRenderableWidget(radius);
        int revealWidth=Math.max(65,Math.min(140,content-240));
        addRenderableWidget(Button.builder(Component.literal(reveal?(revealWidth<110?"Hide":"Hide locations"):(revealWidth<110?"Reveal":"Reveal locations")),b->{
            reveal=!reveal;b.setMessage(Component.literal(reveal?(revealWidth<110?"Hide":"Hide locations"):(revealWidth<110?"Reveal":"Reveal locations")));
        }).bounds(left+content-revealWidth,58,revealWidth,20).build());
        options=addRenderableWidget(Button.builder(Component.literal("Options"),b->minecraft.setScreen(new ScoutOptionsScreen(this,prefs,!running()&&!working(),this::startSeedCheck)))
            .bounds(left+content-76,82,76,16).build());
        results=addRenderableWidget(Button.builder(Component.literal("Results"),b->minecraft.setScreen(new ScoutResultsScreen(this,()->engine==null?null:engine.session,()->!running()&&!working()&&!busy,
            result->{savedResult=result;com.cooper.terrafirmascout.TerraFirmaScout.LOGGER.info("Scout: result {} picked from Results (search running={})",result.seed(),running());message=result.selectable(result.fingerprint())?"Showing a confirmed match.":"Showing a close call. It is not confirmed, so it cannot be used yet.";},this::startCloserLook)))
            .bounds(left+content-76,100,76,16).build());
        int space=content-18,findWidth=(int)(space*0.33),pauseWidth=(int)(space*0.18),keepWidth=(int)(space*0.29),cancelWidth=space-findWidth-pauseWidth-keepWidth;
        find=addRenderableWidget(Button.builder(Component.literal("Find a seed"),b->start()).bounds(left,height-52,findWidth,20).build());
        pause=addRenderableWidget(Button.builder(Component.literal("Pause"),b->{if(engine!=null) { if(engine.session.paused){engine.session.resume();com.cooper.terrafirmascout.TerraFirmaScout.LOGGER.info("Scout: search resumed by the player");}else{engine.session.pause();com.cooper.terrafirmascout.TerraFirmaScout.LOGGER.info("Scout: search paused by the player");} }})
            .bounds(left+findWidth+6,height-52,pauseWidth,20).build());
        keep=addRenderableWidget(Button.builder(Component.literal("Keep searching"),b->{if(engine!=null){engine.session.resume();com.cooper.terrafirmascout.TerraFirmaScout.LOGGER.info("Scout: Keep searching pressed");}}).bounds(left+findWidth+pauseWidth+12,height-52,keepWidth,20).build());
        stop=addRenderableWidget(Button.builder(Component.literal("Stop"),b->{com.cooper.terrafirmascout.TerraFirmaScout.LOGGER.info("Scout: Stop pressed (running={}, paused={})",running(),engine!=null&&engine.session.paused);if(engine!=null)engine.close();if(job!=null)job.cancel();message="Stopping...";}).bounds(left+content-cancelWidth,height-52,cancelWidth,20).build());
        int row=content-16,useWidth=(int)(row*0.25),seedWidth=(int)(row*0.17),reportWidth=(int)(row*0.20),fileWidth=(int)(row*0.22),backWidth=row-useWidth-seedWidth-reportWidth-fileWidth;
        int x=left;
        use=addRenderableWidget(Button.builder(Component.literal("Use this seed"),b->applySeed()).bounds(x,height-27,useWidth,20).build()); x+=useWidth+4;
        copySeed=addRenderableWidget(Button.builder(Component.literal("Copy seed"),b->{var r=bestResult();if(r!=null){minecraft.keyboardHandler.setClipboard(Long.toString(r.seed()));message="Seed "+r.seed()+" copied.";}}).bounds(x,height-27,seedWidth,20).build()); x+=seedWidth+4;
        copyReport=addRenderableWidget(Button.builder(Component.literal("Copy report"),b->{var r=bestResult();if(r!=null){minecraft.keyboardHandler.setClipboard(ReportText.text(r,reveal));message="Report copied"+(reveal?" (with locations).":" (locations hidden).");}}).bounds(x,height-27,reportWidth,20).build()); x+=reportWidth+4;
        export=addRenderableWidget(Button.builder(Component.literal("Save as file"),b->{try{var path=ResultHistory.export(bestResult(),reveal);message="Report saved: "+path.getFileName();}catch(Exception e){message="Export failed: "+e.getMessage();}}).bounds(x,height-27,fileWidth,20).build()); x+=fileWidth+4;
        addRenderableWidget(Button.builder(Component.literal("Back"),b->onClose()).bounds(x,height-27,backWidth,20).build());
        updateProfileFields();
    }
    private String profileName() { return preset.equals("specification")?"Your wishlist":preset.equals("custom")?"Custom":com.cooper.terrafirmascout.profile.SeedQuality.fromName(preset).label; }
    private void updateProfileFields() {
        try {
            var p=currentProfile();
            score.setValue(String.valueOf(prefs.minScore.getOrDefault(preset,p.minScore()))); radius.setValue(String.valueOf(prefs.radius.getOrDefault(preset,p.radius())));
            profile.setMessage(Component.literal(profileName())); if(find!=null)find.setMessage(Component.literal("Find a seed"));
        } catch(Exception e) { message="Check your settings: "+e.getMessage(); }
    }
    private void savePrefs() {
        prefs.preset=preset;
        try { prefs.minScore.put(preset,Integer.parseInt(score.getValue())); prefs.radius.put(preset,Integer.parseInt(radius.getValue())); } catch(NumberFormatException ignored) { /* blank box: keep the earlier value */ }
        prefs.save();
    }
    private ScoutProfile selectedProfile() {
        var p=currentProfile();
        return p.withScoreAndRadius(Integer.parseInt(score.getValue()),Integer.parseInt(radius.getValue()));
    }
    private ScoutProfile currentProfile() {
        if(preset.equals("specification")){if(specification==null)specification=SpecificationDraft.load(ScoutProfile.beginner());return specification.build();}
        return preset.equals("custom")&&customValues!=null?ScoutConfig.fromValues(preset,customValues):ScoutConfig.profile(preset);
    }
    private SearchWorldContext capture() {
        var ui=parent.getUiState(); var holder=ui.getWorldType().preset();
        String id=holder==null?"custom":holder.unwrapKey().map(k->k.location().toString()).orElse("custom");
        return SearchWorldContext.capture(ui.getSettings(),((CreateWorldAccess)parent).scout$dataPackDir(),id);
    }
    private boolean running() { return engine!=null&&!engine.session.finished; }
    private boolean working() { return job!=null&&!job.done; }
    private SearchLimits limits() {
        return new SearchLimits(ScoutPrefs.workers(prefs.speed,ScoutConfig.WORKERS.get(),Runtime.getRuntime().availableProcessors()),prefs.stopAfterMatches,prefs.stopAfterMinutes);
    }
    private void start() {
        if(running()||working()) return;
        try {
            savePrefs(); savedResult=null; announcedMatches=0;
            engine=new ScoutSearchEngine(capture(),selectedProfile(),limits()); engine.start(); message="";
            com.cooper.terrafirmascout.TerraFirmaScout.LOGGER.info("Scout: search started ({}, speed {}, stop after {} matches / {} min)",profileName(),prefs.speed,prefs.stopAfterMatches,prefs.stopAfterMinutes);
        } catch(NumberFormatException e) { message="Type a whole number in Min. match and Range."; } catch(Exception e) { message="Could not start: "+e.getMessage(); }
    }
    /** A closer look re-checks one close call with a larger inspection budget; the same real-world checks decide the result. */
    private void startCloserLook(SeedResult result) {
        if(running()||working()||busy) return;
        try {
            job=new SeedCheckJob("closer look",capture(),result.profile(),result.seed(),SeedChecker.closerLookBudget(ScoutConfig.TARGET_CHUNKS.get()),result.fingerprint()); job.start();
            savedResult=null; message="Taking a closer look at seed "+result.seed()+"...";
        } catch(Exception e) { message="Could not start: "+e.getMessage(); }
    }
    private void startSeedCheck(String text) {
        if(running()||working()||busy) return;
        try {
            var parsed=WorldOptions.parseSeed(text); if(parsed.isEmpty()) { message="Type a seed first."; return; }
            savePrefs();
            job=new SeedCheckJob("seed check",capture(),selectedProfile(),parsed.getAsLong(),SeedChecker.closerLookBudget(ScoutConfig.TARGET_CHUNKS.get()),null); job.start();
            savedResult=null; message="Checking seed "+parsed.getAsLong()+" against your choices...";
        } catch(Exception e) { message="Could not check that seed: "+e.getMessage(); }
        minecraft.setScreen(this);
    }
    private void finishJob() {
        var done=job; job=null;
        if(!done.error.isEmpty()) { message=done.error; return; }
        var result=done.result; savedResult=result;
        if(result.selectable(result.fingerprint())) {
            if(engine!=null) { try { engine.session.addMatch(result); } catch(IllegalArgumentException ignored) { } engine.session.removeCloseCall(result.seed()); }
            message="Seed "+result.seed()+" is confirmed and saved to Saved seeds."; announceMatch(result);
        } else {
            if(engine!=null) engine.session.addCloseCall(result);
            message="Seed "+result.seed()+": "+result.status().toLowerCase(Locale.ROOT)+".";
        }
    }
    private void announceMatch(SeedResult result) {
        if(!prefs.matchSound) return;
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP,1.0f));
        SystemToast.addOrUpdate(minecraft.getToasts(),SystemToast.SystemToastId.PERIODIC_NOTIFICATION,Component.literal("TerraFirmaScout: match found"),Component.literal("Seed "+result.seed()));
    }
    private void applySeed() {
        if(busy||working()) return; var result=bestResult(); if(result==null||!result.selectable(result.fingerprint())) return;
        busy=true; message="Checking that your world settings still match...";
        SearchWorldContext current;
        try { current=capture(); } catch(Exception e) { busy=false; message=e.getMessage(); return; }
        CompletableFuture.supplyAsync(()-> { try { return WorldgenFingerprint.compute(current); } catch(Exception e) { throw new java.util.concurrent.CompletionException(e); } })
            .whenComplete((fingerprint,error)->minecraft.execute(()-> {
                busy=false;
                if(error!=null) { message="Could not check your world settings. Your seed has not changed."; return; }
                if(!result.selectable(fingerprint)||parent.getUiState().getSettings()!=current.creation()) {
                    message="Your world settings changed. Run another search before using this seed."; return;
                }
                if(engine!=null)engine.close(); parent.getUiState().setSeed(Long.toString(result.seed()));
                minecraft.setScreen(parent);
            }));
    }
    @Override public void tick() {
        boolean running=running(),working=working(),idle=!running&&!working&&!busy;
        if(job!=null&&job.done) finishJob();
        if(engine!=null&&engine.session.finished&&message.equals("Stopping...")) message="Search stopped.";
        if(engine!=null) {
            long found=engine.session.verified.get();
            if(found>announcedMatches) { announcedMatches=found; var all=engine.session.matches(); if(!all.isEmpty()) announceMatch(all.getLast()); }
        }
        find.active=idle; profile.active=idle; custom.active=idle; score.active=!running&&!working; radius.active=!running&&!working;
        var s=engine==null?null:engine.session; boolean stopping=s!=null&&s.cancelled&&!s.finished; pause.active=running&&!stopping&&!busy; keep.active=running&&!stopping&&s.paused&&!busy; stop.active=(running&&!stopping)||working;
        pause.setMessage(Component.literal(s!=null&&s.paused?"Resume":"Pause"));
        var best=bestResult(); use.active=!busy&&!working&&best!=null&&best.selectable(best.fingerprint()); history.active=idle; export.active=use.active;
        copySeed.active=best!=null; copyReport.active=best!=null; options.active=!working;
        int count=s==null?0:s.matches().size()+s.closeCalls().size(); results.setMessage(Component.literal(count==0?"Results":"Results ("+count+")"));
    }
    /** What the search is doing right now, including the moments after Pause or Stop while a check in progress finishes. */
    private String status(SearchSession s) {
        if(s.finished) return s.stage;
        if(s.cancelled) return "Stopping... finishing the check in progress";
        if(s.paused) return s.stage.startsWith("Found")?s.stage:engine!=null&&engine.verifying()&&!s.verifierWaiting?"Pausing... finishing the check in progress":"Paused";
        return s.stage;
    }
    private static String clock(long nanos) {
        long seconds=Math.max(0,nanos/1_000_000_000L),h=seconds/3600,m=seconds/60%60,sec=seconds%60;
        return h>0?String.format(Locale.ROOT,"%d:%02d:%02d",h,m,sec):String.format(Locale.ROOT,"%02d:%02d",m,sec);
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partialTick) {
        super.render(g,mouseX,mouseY,partialTick);
        int left=Math.max(8,(width-500)/2),content=Math.min(500,width-16),textWidth=Math.max(60,content-84);
        g.drawCenteredString(font,title,width/2,12,0xffffff);
        g.drawString(font,"Min. match",left,63,0xd0d0d0); g.drawString(font,"Range",left+126,63,0xd0d0d0);
        var s=engine==null?null:engine.session; var checking=working()?job.session:null;
        if(checking!=null) drawClipped(g,"Closer look: "+checking.stage+"   "+clock(checking.elapsedNanos()),left,86,0xffffff,textWidth);
        else if(s!=null) drawClipped(g,status(s)+"   "+clock(s.elapsedNanos()),left,86,0xffffff,textWidth);
        else drawClipped(g,message,left,86,0xffffff,textWidth);
        if(s!=null) {
            double seconds=s.elapsedNanos()/1e9; String rate=seconds>=3?String.format(Locale.ROOT," (%.1f/s)",s.tested.get()/seconds):"";
            long slow=s.skippedSlow.get();
            drawClipped(g,"Seeds "+s.tested+rate+"   Promising "+s.pass1+"   Checked "+s.pass2+"   Matches "+s.verified+(slow>0?"   Slow skipped "+slow:""),left,98,0xb0b0b0,textWidth);
            if(!s.error.isEmpty()) drawClipped(g,s.error,left,110,0xff7777,textWidth);
            else drawClipped(g,message,left,110,0xb0b0b0,textWidth);
        } else { drawClipped(g,checking!=null?message:"Scout keeps your requirements as they are.",left,98,0xb0b0b0,textWidth);
            try{drawClipped(g,currentProfile().description(),left,110,0xb0b0b0,textWidth);}catch(Exception ignored){} }

        var best=bestResult();
        int start=138,end=height-62;
        if(best!=null) {
            drawClipped(g,"Seed: "+best.seed()+(best.selectable(best.fingerprint())?"   Match: ":"   So far: ")+best.score()+"%   "+best.status(),left,124,best.selectable(best.fingerprint())?0x77ff88:0xffcc66);
            g.enableScissor(left,start,width-8,end);
            int i=0;
            for(var c:Criterion.values()) {
                var e=best.evidence().get(c); if(e==null||c==Criterion.FRESHWATER||!best.profile().requires(c)&&(c==Criterion.RIVER||c==Criterion.LAKE||c==Criterion.COAST||c==Criterion.CROPS||c==Criterion.FARMLAND||c==Criterion.ANIMALS)) continue; int y=start+i++*24-scroll;
                if(!best.profile().requires(c)) {
                    drawClipped(g,c.label+": Not needed for this search",left,y,0x909090);
                    continue;
                }
                String range=Double.isFinite(e.distance())?" — within "+bucket(e.distance()):"";
                String loc=reveal&&e.state()==VerificationState.VERIFIED?"   X="+e.x()+" Y="+e.y()+" Z="+e.z():"";
                int color=e.state()==VerificationState.VERIFIED?0x88ee99:e.state()==VerificationState.FAILED?0xff7777:0xffcc77;
                drawClipped(g,c.label+": "+stateLabel(e.state())+range+loc,left,y,color);
                drawClipped(g,friendlyDetail(e.detail()),left+8,y+11,0xa0a0a0);
            }
            g.disableScissor();
        } else if(start<end) drawClipped(g,"Promising seeds will show up here as Scout checks them.",left,start,0xa0a0a0);
    }
    private static String friendlyDetail(String detail){
        if(detail.startsWith("Found "))try{
            var id=net.minecraft.resources.ResourceLocation.parse(detail.substring(6));
            return "Found "+net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(id).getName().getString();
        }catch(Exception ignored){}
        return detail;
    }
    private static String stateLabel(VerificationState state){return switch(state){case VERIFIED->"Confirmed";case INFERRED->"Unconfirmed";case FAILED->"Does not match";};}
    private SeedResult bestResult(){return savedResult!=null?savedResult:engine==null?null:engine.session.best.get();}
    private void drawClipped(GuiGraphics g,String text,int x,int y,int color) { drawClipped(g,text,x,y,color,Math.max(30,width-x-12)); }
    private void drawClipped(GuiGraphics g,String text,int x,int y,int color,int maxWidth) { g.drawString(font,font.plainSubstrByWidth(text,maxWidth),x,y,color); }
    private static String bucket(double d) { return ReportText.bucket(d); }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        scroll=Math.max(0,Math.min(Math.max(0,Criterion.values().length*24-Math.max(0,height-200)),scroll-(int)(vertical*24))); return true;
    }
    @Override public void onClose() { savePrefs(); if(engine!=null)engine.close(); if(job!=null)job.cancel(); minecraft.setScreen(parent); }
    @Override public void removed() { if(engine!=null&&minecraft.screen!=this) engine.close(); }
}
