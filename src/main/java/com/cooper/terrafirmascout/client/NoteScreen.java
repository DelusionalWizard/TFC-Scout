package com.cooper.terrafirmascout.client;
import com.cooper.terrafirmascout.search.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
/** A short note on a saved seed, such as where you plan to build. */
final class NoteScreen extends Screen {
    private final HistoryScreen parent; private final SeedResult result; private EditBox box; private String error="";
    NoteScreen(HistoryScreen parent,SeedResult result) { super(Lang.t("terrafirmascout.note.title",result.seed())); this.parent=parent; this.result=result; }
    @Override protected void init() {
        int w=Math.min(300,width-16),left=(width-w)/2;
        box=new EditBox(font,left,60,w,20,Lang.t("terrafirmascout.common.note")); box.setMaxLength(60); box.setValue(ResultHistory.note(result)); addRenderableWidget(box); setInitialFocus(box);
        addRenderableWidget(Button.builder(Lang.t("terrafirmascout.common.save"),b-> {
            try { ResultHistory.setNote(result,box.getValue()); minecraft.setScreen(parent); } catch(Exception e) { error="Could not save the note: "+e.getMessage(); }
        }).bounds(width/2-100,92,96,20).build());
        addRenderableWidget(Button.builder(Lang.t("terrafirmascout.common.cancel"),b->minecraft.setScreen(parent)).bounds(width/2+4,92,96,20).build());
    }
    @Override public void render(GuiGraphics g,int x,int y,float tick) {
        renderBackground(g); super.render(g,x,y,tick); g.drawCenteredString(font,title,width/2,20,0xffffff);
        g.drawCenteredString(font,"A short reminder, up to 60 characters. Leave it empty to remove the note.",width/2,40,0xb0b0b0);
        if(!error.isEmpty()) g.drawCenteredString(font,font.plainSubstrByWidth(error,width-16),width/2,124,0xff7777);
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
}
