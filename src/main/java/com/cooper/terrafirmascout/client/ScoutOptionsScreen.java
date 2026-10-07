package com.cooper.terrafirmascout.client;
import java.util.List;
import java.util.function.Consumer;
import com.cooper.terrafirmascout.config.ScoutPrefs;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
/** Search speed, automatic stops, match sound, and checking one seed. Choices are remembered between sessions. */
final class ScoutOptionsScreen extends Screen {
    private final Screen parent; private final ScoutPrefs prefs; private final Consumer<String> checkSeed; private final boolean canCheck;
    private EditBox matches,minutes,seedBox; private Button speed,sound,check;
    ScoutOptionsScreen(Screen parent,ScoutPrefs prefs,boolean canCheck,Consumer<String> checkSeed) {
        super(Component.literal("Search options")); this.parent=parent; this.prefs=prefs; this.canCheck=canCheck; this.checkSeed=checkSeed;
    }
    private static String speedLabel(String s) { return switch(s) { case "low"->"Low"; case "high"->"High"; default->"Normal"; }; }
    private static String speedHint(String s) { return switch(s) { case "low"->"Gentlest on your game: one scanner at a time."; case "high"->"Fastest, but the game may feel slower while it runs."; default->"A balanced number of scanners for your computer."; }; }
    @Override protected void init() {
        int w=Math.min(300,width-16),left=(width-w)/2;
        speed=addRenderableWidget(Button.builder(Component.literal("Search speed: "+speedLabel(prefs.speed)),b-> {
            var modes=List.of("low","normal","high"); prefs.speed=modes.get((modes.indexOf(prefs.speed)+1)%modes.size());
            b.setMessage(Component.literal("Search speed: "+speedLabel(prefs.speed)));
        }).bounds(left,34,w,20).build());
        matches=new EditBox(font,left+w-50,74,50,18,Component.literal("Stop after matches")); matches.setFilter(v->v.matches("[0-9]{0,2}")); matches.setValue(String.valueOf(prefs.stopAfterMatches)); addRenderableWidget(matches);
        minutes=new EditBox(font,left+w-50,100,50,18,Component.literal("Stop after minutes")); minutes.setFilter(v->v.matches("[0-9]{0,3}")); minutes.setValue(String.valueOf(prefs.stopAfterMinutes)); addRenderableWidget(minutes);
        sound=addRenderableWidget(Button.builder(Component.literal("Sound when a match is found: "+(prefs.matchSound?"On":"Off")),b-> {
            prefs.matchSound=!prefs.matchSound; b.setMessage(Component.literal("Sound when a match is found: "+(prefs.matchSound?"On":"Off")));
        }).bounds(left,126,w,20).build());
        seedBox=new EditBox(font,left,168,w-70,18,Component.literal("Seed to check")); seedBox.setMaxLength(64); seedBox.setHint(Component.literal("A seed number or text")); addRenderableWidget(seedBox);
        check=addRenderableWidget(Button.builder(Component.literal("Check"),b-> {
            var text=seedBox.getValue().strip(); if(text.isEmpty()) return;
            apply(); checkSeed.accept(text);
        }).bounds(left+w-64,167,64,20).build()); check.active=canCheck;
        addRenderableWidget(Button.builder(Component.literal("Done"),b->onClose()).bounds(width/2-75,height-27,150,20).build());
    }
    private void apply() {
        prefs.stopAfterMatches=parse(matches.getValue(),99); prefs.stopAfterMinutes=parse(minutes.getValue(),999); prefs.save();
    }
    private static int parse(String text,int max) { try { return Math.max(0,Math.min(max,Integer.parseInt(text))); } catch(NumberFormatException e) { return 0; } }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float tick) {
        super.render(g,mouseX,mouseY,tick);
        int w=Math.min(300,width-16),left=(width-w)/2;
        g.drawCenteredString(font,title,width/2,12,0xffffff);
        g.drawString(font,font.plainSubstrByWidth(speedHint(prefs.speed),w),left,57,0xa0a0a0);
        g.drawString(font,font.plainSubstrByWidth("Stop after this many matches (0 = off)",w-56),left,79,0xd0d0d0);
        g.drawString(font,font.plainSubstrByWidth("Stop after this many minutes (0 = off)",w-56),left,105,0xd0d0d0);
        g.drawString(font,"Check one seed against your choices",left,155,0xd0d0d0);
        if(!canCheck) g.drawString(font,font.plainSubstrByWidth("Stop the search first to check a seed.",w),left,190,0xffcc77);
        else g.drawString(font,font.plainSubstrByWidth("Text works too, just like the Create World seed box.",w),left,190,0x909090);
    }
    @Override public void onClose() { apply(); minecraft.setScreen(parent); }
}
