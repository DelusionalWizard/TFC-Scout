package com.cooper.terrafirmascout.client;
import java.util.*;
import java.util.function.Consumer;
import com.cooper.terrafirmascout.search.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
final class HistoryScreen extends Screen {
    private final Screen parent;private final Consumer<SeedResult> choose;private final List<SeedResult> results=new ArrayList<>(ResultHistory.load());private int page;
    private boolean byScore; private Long armedDelete; private String notice="";
    HistoryScreen(Screen parent,Consumer<SeedResult> choose){super(Lang.t("terrafirmascout.history.title"));this.parent=parent;this.choose=choose;}
    private List<SeedResult> sorted() {
        var list=new ArrayList<>(results);
        if(byScore) list.sort(Comparator.comparingInt(SeedResult::score).reversed()); // load() is already newest first
        return list;
    }
    private String fit(String text,int pixels) { return font.width(text)<=pixels?text:font.plainSubstrByWidth(text,Math.max(10,pixels-font.width("...")))+"..."; }
    @Override protected void init(){
        var list=sorted(); int count=Math.max(1,(height-112)/24);page=Math.min(page,Math.max(0,(list.size()-1)/count));
        int rowWidth=Math.min(500,width-16),left=(width-rowWidth)/2,noteWidth=44,deleteWidth=52,mainWidth=rowWidth-noteWidth-deleteWidth-8;
        for(int i=page*count;i<Math.min(list.size(),(page+1)*count);i++){
            var result=list.get(i); int y=58+(i-page*count)*24; var note=ResultHistory.note(result);
            addRenderableWidget(Button.builder(Component.literal(fit(Lang.s("terrafirmascout.history.row",result.seed(),result.profile().displayName(),result.score())+(note.isEmpty()?"":" | "+note),mainWidth-12)),b->{choose.accept(result);minecraft.setScreen(parent);})
                .bounds(left,y,mainWidth,20).build());
            addRenderableWidget(Button.builder(Lang.t("terrafirmascout.common.note"),b->minecraft.setScreen(new NoteScreen(this,result))).bounds(left+mainWidth+4,y,noteWidth,20).build());
            boolean armed=armedDelete!=null&&armedDelete==result.seed();
            addRenderableWidget(Button.builder(Lang.t(armed?"terrafirmascout.history.delete_confirm":"terrafirmascout.history.delete"),b->{
                if(!armed){armedDelete=result.seed();rebuildWidgets();return;}
                try{ResultHistory.delete(result);results.removeIf(r->r.seed()==result.seed()&&ResultHistory.fileFor(r).equals(ResultHistory.fileFor(result)));notice="Deleted seed "+result.seed()+".";}
                catch(Exception e){notice="Could not delete: "+e.getMessage();}
                armedDelete=null;rebuildWidgets();
            }).bounds(left+mainWidth+noteWidth+8,y,deleteWidth,20).build());
        }
        var previous=addRenderableWidget(Button.builder(Lang.t("terrafirmascout.common.previous"),b->{page--;armedDelete=null;rebuildWidgets();}).bounds(width/2-154,height-52,70,20).build());previous.active=page>0;
        addRenderableWidget(Button.builder(Lang.t(byScore?"terrafirmascout.history.sort_best":"terrafirmascout.history.sort_newest"),b->{byScore=!byScore;page=0;armedDelete=null;rebuildWidgets();}).bounds(width/2-80,height-52,160,20).build());
        var next=addRenderableWidget(Button.builder(Lang.t("terrafirmascout.common.next"),b->{page++;armedDelete=null;rebuildWidgets();}).bounds(width/2+84,height-52,70,20).build());next.active=(page+1)*count<list.size();
        addRenderableWidget(Button.builder(Lang.t("terrafirmascout.common.back"),b->minecraft.setScreen(parent)).bounds(width/2-75,height-27,150,20).build());
    }
    @Override public void render(GuiGraphics g,int x,int y,float tick){renderBackground(g); super.render(g,x,y,tick);g.drawCenteredString(font,title,width/2,12,0xffffff);
        g.drawCenteredString(font,results.isEmpty()?"No saved seeds yet. Find a match and Scout will save it here.":"Pick a seed. Scout will check your current settings before using it.",width/2,28,0xb0b0b0);
        if(!notice.isEmpty())g.drawCenteredString(font,font.plainSubstrByWidth(notice,width-16),width/2,42,0x88ee99);}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
