package com.cooper.terrafirmascout.client;
import java.util.*;
import java.util.function.Consumer;
import com.cooper.terrafirmascout.config.ScoutConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
/** All numeric hard thresholds are editable; verification and a 90-point floor are mandatory. */
final class CustomProfileScreen extends Screen {
    private final Screen parent; private final Consumer<Map<String,Double>> done;
    private final LinkedHashMap<String,Double> values=new LinkedHashMap<>();
    private final Map<String,EditBox> fields=new LinkedHashMap<>(); private int offset; private String error="";
    CustomProfileScreen(Screen parent,Map<String,Double> previous,Consumer<Map<String,Double>> done) {
        super(Component.literal("Custom God settings")); this.parent=parent; this.done=done;
        ScoutConfig.CUSTOM.forEach((key,v)-> { if(key.startsWith("custom.")) values.put(key.substring(7),v.get()); });
        if(previous!=null) values.putAll(previous);
    }
    @Override protected void init() {
        fields.clear(); int row=0;
        for(var e:values.entrySet()) {
            var box=new EditBox(font,width/2+70,42+row++*24-offset,100,18,Component.literal(e.getKey()));
            box.setValue(String.valueOf(e.getValue())); box.setFilter(s->s.matches("-?[0-9]*\\.?[0-9]*")); fields.put(e.getKey(),box); addRenderableWidget(box);
        }
        addRenderableWidget(Button.builder(Component.literal("Save"),b->{
            try { collect(); ScoutConfig.fromValues("custom",values); done.accept(Map.copyOf(values)); minecraft.setScreen(parent); }
            catch(Exception ex) { error="Check these settings: "+ex.getMessage(); }
        }).bounds(width/2-154,height-27,150,20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"),b->minecraft.setScreen(parent)).bounds(width/2+4,height-27,150,20).build());
        position();
    }
    private void collect() { fields.forEach((key,box)-> { double n=Double.parseDouble(box.getValue()); if(!Double.isFinite(n))throw new IllegalArgumentException(key); values.put(key,n); }); }
    private void position() {
        int row=0; for(var box:fields.values()) { box.setY(42+row++*24-offset); box.visible=box.getY()>=40&&box.getY()+18<=height-52; }
    }
    @Override public void render(GuiGraphics g,int x,int y,float tick) {
        renderBackground(g); super.render(g,x,y,tick); g.drawCenteredString(font,title,width/2,12,0xffffff);
        g.drawCenteredString(font,"All God resources and a land route are still required.",width/2,27,0xb0b0b0);
        int row=0; for(var e:fields.entrySet()) { int top=42+row++*24-offset; if(e.getValue().visible)
            g.drawString(font,SpecificationScreen.settingLabel(e.getKey()),Math.max(8,width/2-200),top+4,0xd0d0d0); }
        g.drawCenteredString(font,font.plainSubstrByWidth(error,width-20),width/2,height-43,0xff7777);
    }
    @Override public boolean mouseScrolled(double x,double y,double v) { offset=Math.max(0,Math.min(Math.max(0,values.size()*24-(height-96)),offset-(int)(v*24))); position(); return true; }
    @Override public void onClose() { minecraft.setScreen(parent); }
}
