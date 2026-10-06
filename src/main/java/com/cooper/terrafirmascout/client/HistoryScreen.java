package com.cooper.terrafirmascout.client;
import java.util.*;
import java.util.function.Consumer;
import com.cooper.terrafirmascout.search.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
final class HistoryScreen extends Screen {
    private final Screen parent;private final Consumer<SeedResult> choose;private final List<SeedResult> results=ResultHistory.load();private int page;
    HistoryScreen(Screen parent,Consumer<SeedResult> choose){super(Component.literal("Saved seeds"));this.parent=parent;this.choose=choose;}
    @Override protected void init(){
        int count=Math.max(1,(height-112)/24);page=Math.min(page,Math.max(0,(results.size()-1)/count));
        for(int i=page*count;i<Math.min(results.size(),(page+1)*count);i++){
            var result=results.get(i);addRenderableWidget(Button.builder(Component.literal(result.seed()+" | "+(result.profile().specification().enabled()?"Wishlist":result.profile().name())+" | match "+result.score()),b->{choose.accept(result);minecraft.setScreen(parent);})
                .bounds(Math.max(8,width/2-250),58+(i-page*count)*24,Math.min(500,width-16),20).build());
        }
        var previous=addRenderableWidget(Button.builder(Component.literal("Previous"),b->{page--;rebuildWidgets();}).bounds(width/2-154,height-52,100,20).build());previous.active=page>0;
        var next=addRenderableWidget(Button.builder(Component.literal("Next"),b->{page++;rebuildWidgets();}).bounds(width/2+54,height-52,100,20).build());next.active=(page+1)*count<results.size();
        addRenderableWidget(Button.builder(Component.literal("Back"),b->minecraft.setScreen(parent)).bounds(width/2-75,height-27,150,20).build());
    }
    @Override public void render(GuiGraphics g,int x,int y,float tick){super.render(g,x,y,tick);g.drawCenteredString(font,title,width/2,12,0xffffff);
        g.drawCenteredString(font,results.isEmpty()?"No saved seeds yet. Find a match and Scout will save it here.":"Pick a seed. Scout will check your current settings before using it.",width/2,35,0xb0b0b0);}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
