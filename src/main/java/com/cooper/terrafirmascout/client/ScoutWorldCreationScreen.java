package com.cooper.terrafirmascout.client;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import com.cooper.terrafirmascout.config.ScoutConfig;
import com.cooper.terrafirmascout.mixin.CreateWorldAccess;
import com.cooper.terrafirmascout.profile.*;
import com.cooper.terrafirmascout.score.*;
import com.cooper.terrafirmascout.search.*;
import com.cooper.terrafirmascout.tfc.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
public final class ScoutWorldCreationScreen extends Screen {
    private final CreateWorldScreen parent;
    private String preset="god",message="Pick a starting style, or tell Scout what you want.";
    private ScoutSearchEngine engine; private boolean reveal,busy; private int scroll;
    private EditBox score,radius; private Button use,pause,keep,find,profile,custom,history,export; private SeedResult savedResult;
    private Map<String,Double> customValues; private SpecificationDraft specification;
    public ScoutWorldCreationScreen(CreateWorldScreen parent) { super(Component.literal("TerraFirmaScout")); this.parent=parent; reveal=ScoutConfig.REVEAL.get(); }
    @Override protected void init() {
        int content=Math.min(500,width-16),left=(width-content)/2,center=width/2,half=(content-8)/2;
        profile=addRenderableWidget(Button.builder(Component.literal(profileName()),b->{
            var modes=List.of("god","good","average","hard","super_hard","custom","specification"); preset=modes.get((modes.indexOf(preset)+1)%modes.size()); updateProfileFields();
        }).bounds(left,32,half,20).build());
        custom=addRenderableWidget(Button.builder(Component.literal("Pick your world"),b->{
            try {
                if(specification==null)specification=SpecificationDraft.load(selectedProfile());
                var draft=new com.google.gson.GsonBuilder().create().fromJson(new com.google.gson.GsonBuilder().create().toJson(specification),SpecificationDraft.class);
                var biomes=parent.getUiState().getSettings().worldgenLoadContext().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).keySet().stream().map(Object::toString).sorted().toList();
                var rocks=net.minecraft.core.registries.BuiltInRegistries.BLOCK.keySet().stream().filter(id->id.getNamespace().equals("tfc")&&id.getPath().startsWith("rock/raw/")).map(id->id.getPath().replace("rock/raw/","")).sorted().toList();
                minecraft.setScreen(new SpecificationScreen(this,draft,biomes,rocks,values->{specification=values;preset="specification";}));
            }catch(Exception e){message="Could not open your settings: "+e.getMessage();}
        }).bounds(left+half+8,32,Math.max(60,half-92),20).build());
        history=addRenderableWidget(Button.builder(Component.literal("Saved seeds"),b->minecraft.setScreen(new HistoryScreen(this,result->{savedResult=result;message="Loaded a "+result.profile().name()+" seed. Scout will check your world settings before using it.";})))
            .bounds(left+content-86,32,86,20).build());
        score=new EditBox(font,left+68,58,44,18,Component.literal("Minimum score")); score.setFilter(v->v.matches("[0-9]{0,3}")); addRenderableWidget(score);
        radius=new EditBox(font,left+171,58,60,18,Component.literal("Search radius")); radius.setFilter(v->v.matches("[0-9]{0,5}")); addRenderableWidget(radius);
        int revealWidth=Math.max(65,Math.min(140,content-240));
        addRenderableWidget(Button.builder(Component.literal(reveal?(revealWidth<110?"Hide":"Hide locations"):(revealWidth<110?"Reveal":"Reveal locations")),b->{
            reveal=!reveal;b.setMessage(Component.literal(reveal?(revealWidth<110?"Hide":"Hide locations"):(revealWidth<110?"Reveal":"Reveal locations")));
        }).bounds(left+content-revealWidth,58,revealWidth,20).build());
        int space=content-18,findWidth=(int)(space*0.33),pauseWidth=(int)(space*0.18),keepWidth=(int)(space*0.29),cancelWidth=space-findWidth-pauseWidth-keepWidth;
        find=addRenderableWidget(Button.builder(Component.literal("Find a seed"),b->start()).bounds(left,height-52,findWidth,20).build());
        pause=addRenderableWidget(Button.builder(Component.literal("Pause"),b->{if(engine!=null) { if(engine.session.paused)engine.session.resume();else engine.session.pause(); }})
            .bounds(left+findWidth+6,height-52,pauseWidth,20).build());
        keep=addRenderableWidget(Button.builder(Component.literal("Keep searching"),b->{if(engine!=null)engine.session.resume();}).bounds(left+findWidth+pauseWidth+12,height-52,keepWidth,20).build());
        addRenderableWidget(Button.builder(Component.literal("Stop"),b->{if(engine!=null)engine.close();message="Search stopped.";}).bounds(left+content-cancelWidth,height-52,cancelWidth,20).build());
        use=addRenderableWidget(Button.builder(Component.literal("Use this seed"),b->applySeed()).bounds(center-154,height-27,150,20).build());
        export=addRenderableWidget(Button.builder(Component.literal("Save report"),b->{try{var path=ResultHistory.export(bestResult(),reveal);message="Report saved: "+path.getFileName();}catch(Exception e){message="Export failed: "+e.getMessage();}}).bounds(center+4,height-27,90,20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"),b->onClose()).bounds(center+100,height-27,54,20).build());
        updateProfileFields();
    }
    private String profileName() { return preset.equals("specification")?"Your wishlist":preset.equals("custom")?"Custom":com.cooper.terrafirmascout.profile.SeedQuality.fromName(preset).label; }
    private void updateProfileFields() {
        try {
            var p=currentProfile();
            score.setValue(String.valueOf(p.minScore())); radius.setValue(String.valueOf(p.radius())); profile.setMessage(Component.literal(profileName())); if(find!=null)find.setMessage(Component.literal("Find a seed"));
        } catch(Exception e) { message="Check your settings: "+e.getMessage(); }
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
    private void start() {
        if(engine!=null&&!engine.session.finished) return;
        try {
            savedResult=null; engine=new ScoutSearchEngine(capture(),selectedProfile()); engine.start(); message="Looking for a start that matches your choices.";
        } catch(Exception e) { message="Could not start: "+e.getMessage(); }
    }
    private void applySeed() {
        if(busy) return; var result=bestResult(); if(result==null||!result.selectable(result.fingerprint())) return;
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
        boolean running=engine!=null&&!engine.session.finished;
        find.active=!running&&!busy; profile.active=!running&&!busy; custom.active=!running&&!busy; score.active=!running; radius.active=!running;
        var s=engine==null?null:engine.session; pause.active=running&&!busy; keep.active=running&&s.paused&&!busy;
        pause.setMessage(Component.literal(s!=null&&s.paused?"Resume":"Pause"));
        var best=bestResult(); use.active=!busy&&best!=null&&best.selectable(best.fingerprint()); history.active=!running&&!busy; export.active=use.active;
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partialTick) {
        super.render(g,mouseX,mouseY,partialTick);
        int left=Math.max(8,(width-500)/2);
        g.drawCenteredString(font,title,width/2,12,0xffffff);
        g.drawString(font,"Min. match",left,63,0xd0d0d0); g.drawString(font,"Range",left+126,63,0xd0d0d0);
        var s=engine==null?null:engine.session;
        drawClipped(g,s==null?message:s.stage,left,86,0xffffff);
        if(s!=null) {
            drawClipped(g,"Seeds: "+s.tested+"   Promising: "+s.pass1+"   Checked: "+s.pass2+"   Matches: "+s.verified,left,100,0xb0b0b0);
            if(!s.error.isEmpty()) drawClipped(g,s.error,left,114,0xff7777);
            else drawClipped(g,message,left,114,0xb0b0b0);
        } else { drawClipped(g,"Scout keeps your requirements as they are.",left,100,0xb0b0b0);
            try{drawClipped(g,currentProfile().description(),left,114,0xb0b0b0);}catch(Exception ignored){} }

        var best=bestResult();
        int start=146,end=height-62;
        if(best!=null) {
            drawClipped(g,"Seed: "+best.seed()+"   Match: "+best.score()+"%   "+best.status(),left,130,best.selectable(best.fingerprint())?0x77ff88:0xffcc66);
            g.enableScissor(left,start,width-8,end);
            int i=0;
            for(var c:Criterion.values()) {
                var e=best.evidence().get(c); if(e==null) continue; int y=start+i++*24-scroll;
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
    private void drawClipped(GuiGraphics g,String text,int x,int y,int color) { g.drawString(font,font.plainSubstrByWidth(text,Math.max(30,width-x-12)),x,y,color); }
    private static String bucket(double d) { int step=d<1000?100:500; int blocks=Math.max(step,(int)Math.ceil(d/step)*step); return blocks>=1000?String.format(Locale.ROOT,"%.1f km",blocks/1000.0):blocks+" m"; }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        scroll=Math.max(0,Math.min(Math.max(0,Criterion.values().length*24-Math.max(0,height-208)),scroll-(int)(vertical*24))); return true;
    }
    @Override public void onClose() { if(engine!=null)engine.close(); minecraft.setScreen(parent); }
    @Override public void removed() { if(engine!=null&&minecraft.screen!=this) engine.close(); }
}
