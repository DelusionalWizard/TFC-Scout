package com.cooper.terrafirmascout.client;
import java.util.*;
import java.util.function.*;
import com.cooper.terrafirmascout.score.CandidateScorer;
import com.cooper.terrafirmascout.search.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
/** Every confirmed match from this search, plus close calls that missed exactly one check and can get a closer look. */
final class ScoutResultsScreen extends Screen {
    private final Screen parent; private final Supplier<SearchSession> session; private final Consumer<SeedResult> view,lookCloser; private final BooleanSupplier canLook;
    private int page=0,shownMatches=-1,shownClose=-1; private final List<Button> lookButtons=new ArrayList<>();
    private String fit(String text,int pixels) { return font.width(text)<=pixels?text:font.plainSubstrByWidth(text,Math.max(10,pixels-font.width("...")))+"..."; }
    ScoutResultsScreen(Screen parent,Supplier<SearchSession> session,BooleanSupplier canLook,Consumer<SeedResult> view,Consumer<SeedResult> lookCloser) {
        super(Lang.t("terrafirmascout.results.title")); this.parent=parent; this.session=session; this.canLook=canLook; this.view=view; this.lookCloser=lookCloser;
    }
    private List<SeedResult> matches() { var s=session.get(); return s==null?List.of():s.matches(); }
    private List<SeedResult> closeCalls() { var s=session.get(); return s==null?List.of():s.closeCalls(); }
    @Override protected void init() {
        lookButtons.clear(); var matches=matches(); var close=closeCalls(); shownMatches=matches.size(); shownClose=close.size();
        int rows=Math.max(1,(height-104)/24),total=matches.size()+close.size(); page=Math.min(page,Math.max(0,(total-1)/rows));
        int rowWidth=Math.min(500,width-16),left=(width-rowWidth)/2;
        for(int i=page*rows;i<Math.min(total,(page+1)*rows);i++) {
            int y=58+(i-page*rows)*24;
            if(i<matches.size()) {
                var r=matches.get(i);
                addRenderableWidget(Button.builder(Component.literal(fit(Lang.s("terrafirmascout.results.row_match",r.seed(),r.profile().displayName(),r.score()),rowWidth-12)),b->{view.accept(r);minecraft.setScreen(parent);}).bounds(left,y,rowWidth,20).build());
            } else {
                var r=close.get(i-matches.size()); var open=CandidateScorer.unconfirmedRequired(r.evidence(),r.profile());
                String missing=open.isEmpty()?"":open.get(0).label;
                addRenderableWidget(Button.builder(Component.literal(fit(Lang.s("terrafirmascout.results.row_close",r.seed(),missing,r.score()),rowWidth-94-12)),b->{view.accept(r);minecraft.setScreen(parent);}).bounds(left,y,rowWidth-94,20).build());
                var look=addRenderableWidget(Button.builder(Lang.t("terrafirmascout.results.look_harder"),b->{lookCloser.accept(r);minecraft.setScreen(parent);}).bounds(left+rowWidth-90,y,90,20).build());
                look.active=canLook.getAsBoolean(); lookButtons.add(look);
            }
        }
        var previous=addRenderableWidget(Button.builder(Lang.t("terrafirmascout.common.previous"),b->{page--;rebuildWidgets();}).bounds(width/2-154,height-52,100,20).build());previous.active=page>0;
        var next=addRenderableWidget(Button.builder(Lang.t("terrafirmascout.common.next"),b->{page++;rebuildWidgets();}).bounds(width/2+54,height-52,100,20).build());next.active=(page+1)*rows<total;
        addRenderableWidget(Button.builder(Lang.t("terrafirmascout.common.back"),b->minecraft.setScreen(parent)).bounds(width/2-75,height-27,150,20).build());
    }
    @Override public void tick() {
        if(matches().size()!=shownMatches||closeCalls().size()!=shownClose) rebuildWidgets();
        boolean can=canLook.getAsBoolean(); for(var b:lookButtons) b.active=can;
    }
    @Override public void render(GuiGraphics g,int x,int y,float tick) {
        renderBackground(g); super.render(g,x,y,tick); g.drawCenteredString(font,title,width/2,12,0xffffff);
        boolean none=matches().isEmpty()&&closeCalls().isEmpty();
        g.drawCenteredString(font,font.plainSubstrByWidth(none?"Nothing yet. Confirmed matches and close calls from this search appear here.":"Pick a match to use it. A close call needs a closer look before it can be used.",width-16),width/2,28,0xb0b0b0);
        if(!none&&!canLook.getAsBoolean()) g.drawCenteredString(font,font.plainSubstrByWidth("Stop or finish the search to take a closer look at a close call.",width-16),width/2,42,0xffcc77);
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
}
