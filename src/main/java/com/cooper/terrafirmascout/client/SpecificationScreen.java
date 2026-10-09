package com.cooper.terrafirmascout.client;
import java.util.*;
import java.util.function.Consumer;
import com.cooper.terrafirmascout.profile.*;
import com.cooper.terrafirmascout.score.Criterion;
import net.dries007.tfc.world.chunkdata.ForestType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
/** Active registry choices, strict requirements and player-visible ranges. */
final class SpecificationScreen extends Screen {
    private final Screen parent; private final SpecificationDraft draft; private final Consumer<SpecificationDraft> done;
    private final List<String> biomes,rocks;
    private final List<AbstractWidget> rows=new ArrayList<>(); private final List<String> rowText=new ArrayList<>(); private EditBox search; private String query=""; private final Map<String,EditBox> fields=new LinkedHashMap<>();
    private String tab="Resources",error=""; private int offset;
    SpecificationScreen(Screen parent,SpecificationDraft draft,List<String> biomes,List<String> rocks,Consumer<SpecificationDraft> done) {
        super(Lang.t("terrafirmascout.world.pick"));this.parent=parent;this.draft=draft;this.biomes=biomes;this.rocks=rocks;this.done=done;
    }
    @Override protected void init() {
        rows.clear();rowText.clear();fields.clear(); int content=Math.min(500,width-16),left=(width-content)/2;
        search=new EditBox(font,left+content-140,57,140,16,Lang.t("terrafirmascout.spec.search")); search.setHint(Lang.t("terrafirmascout.spec.search")); search.setMaxLength(40); search.setValue(query);
        search.setResponder(v->{query=v.trim().toLowerCase(Locale.ROOT);offset=0;position();}); addRenderableWidget(search);
        var tabs=List.of("Resources","Limits","Nearby","Spawn","Rocks","Forest"); int tw=(content-10)/6;
        for(int i=0;i<tabs.size();i++) { String name=tabs.get(i); var b=Button.builder(Component.literal(name),button->{
            try { collect();tab=name;offset=0;query="";rebuildWidgets(); }catch(Exception e){error=e.getMessage();}
        }).bounds(left+i*(tw+2),33,tw,20).build(); b.active=!tab.equals(name);addRenderableWidget(b); }
        if(tab.equals("Resources")) {
            for(var c:Criterion.values())if(c!=Criterion.SPAWN&&c!=Criterion.SPECIFICATION&&c!=Criterion.DIVERSITY&&c!=Criterion.FRESHWATER)
                toggle(c.label,draft.required.contains(c),b->{if(!draft.required.remove(c))draft.required.add(c);b.setMessage(check(c.label,draft.required.contains(c)));});
        } else if(tab.equals("Limits")) {
            for(var e:draft.numbers.entrySet()) {
                var box=new EditBox(font,left+content-100,80+rows.size()*24-offset,100,18,Component.literal(e.getKey()));
                box.setValue(e.getValue()==Math.rint(e.getValue())?Long.toString(e.getValue().longValue()):e.getValue().toString());box.setFilter(v->v.matches("-?[0-9]*\\.?[0-9]*"));fields.put(e.getKey(),box);rows.add(box);rowText.add(settingLabel(e.getKey()));addRenderableWidget(box);
            }
        } else if(tab.equals("Nearby")) {
            toggle("Require every chosen biome",draft.allBiomes,b->{draft.allBiomes=!draft.allBiomes;b.setMessage(check("Require every chosen biome",draft.allBiomes));});
            for(var id:biomes)choice(id,draft.nearbyBiomes);
        } else if(tab.equals("Spawn"))for(var id:biomes)choice(id,draft.spawnBiomes);
        else if(tab.equals("Rocks"))for(var id:rocks)choice(id,draft.spawnRocks);
        else for(var type:ForestType.values())choice(type.getSerializedName(),draft.forestTypes);
        addRenderableWidget(Button.builder(Lang.t("terrafirmascout.spec.save"),b->{
            try {collect();draft.save();done.accept(draft);minecraft.setScreen(parent);}catch(Exception e){error=e.getMessage();}
        }).bounds(width/2-154,height-27,150,20).build());
        addRenderableWidget(Button.builder(Lang.t("terrafirmascout.common.cancel"),b->minecraft.setScreen(parent)).bounds(width/2+4,height-27,150,20).build());
        position();
    }
    void showTab(String name){collect();tab=name;offset=0;query="";rebuildWidgets();}
    static String settingLabel(String key){return switch(key){
        case "min_score"->"Minimum match (0-100)";case "search_radius"->"How far to search";
        case "temperature_min"->"Coldest yearly average (C)";case "temperature_max"->"Warmest yearly average (C)";
        case "temperature_ideal_min"->"Preferred temperature, from (C)";case "temperature_ideal_max"->"Preferred temperature, to (C)";
        case "rainfall_min"->"Least rainfall (mm)";case "rainfall_max"->"Most rainfall (mm)";
        case "rainfall_ideal_min"->"Preferred rainfall, from (mm)";case "rainfall_ideal_max"->"Preferred rainfall, to (mm)";
        case "minimum_land_fraction"->"Land needed (0-1)";case "land_radius"->"Area to check for land";
        case "terrain_distance"->"Max distance to a building site";case "camp_distance"->"Max distance to a camp spot";
        case "starter_copper_units"->"Copper to melt (mB)";case "starter_copper_pieces"->"Loose copper pieces needed";
        case "challenge_surface_span"->"Minimum ruggedness (blocks)";case "biome_radius"->"How far to look for biomes";
        case "forest_density_min"->"Least tree cover (0-4)";case "forest_density_max"->"Most tree cover (0-4)";
        case "spawn_elevation_min"->"Lowest spawn height (Y)";case "spawn_elevation_max"->"Highest spawn height (Y)";
        case "terrain_patch_size"->"Building site width (blocks)";case "camp_patch_size"->"Camp width (blocks)";
        case "maximum_slope"->"Allowed height difference";case "minimum_grass_fraction"->"Grass needed (0-1)";
        default->"Max distance to "+key.replace("_distance","").replace('_',' ').replace("starter copper","loose copper").replace("coast","coast or ocean").replace("animals","animals that can spawn");
    };}
    private static Component check(String label,boolean on) {return Component.literal((on?"[x] ":"[ ] ")+label);}
    private void choice(String id,Set<String> selection) {String raw=id.contains(":")?id.substring(id.indexOf(':')+1):id;String label=raw.replace('_',' ');label=Character.toUpperCase(label.charAt(0))+label.substring(1);if(id.contains(":"))label+=" ("+(id.startsWith("tfc:")?"TFC":id.startsWith("minecraft:")?"Minecraft":id.substring(0,id.indexOf(':')))+")"; final String shown=label;toggle(shown,selection.contains(id),b->{if(!selection.remove(id))selection.add(id);b.setMessage(check(shown,selection.contains(id)));});}
    private void toggle(String label,boolean on,Consumer<Button> action) {
        int content=Math.min(500,width-16),left=(width-content)/2;
        var b=Button.builder(check(label,on),action::accept).bounds(left,80+rows.size()*24-offset,content,20).build();rows.add(b);rowText.add(label);addRenderableWidget(b);
    }
    private void collect() {fields.forEach((key,box)->draft.numbers.put(key,Double.parseDouble(box.getValue())));}
    private boolean matches(int i) {return query.isEmpty()||rowText.get(i).toLowerCase(Locale.ROOT).contains(query);}
    private int shown() {int n=0;for(int i=0;i<rows.size();i++)if(matches(i))n++;return n;}
    private void position() {int slot=0;for(int i=0;i<rows.size();i++){var row=rows.get(i);if(!matches(i)){row.visible=false;continue;}row.setY(80+slot++*24-offset);row.visible=row.getY()>=78&&row.getY()+20<=height-54;}}
    @Override public void render(GuiGraphics g,int x,int y,float tick) {
        super.render(g,x,y,tick);g.drawCenteredString(font,title,width/2,12,0xffffff);
        String help=switch(tab){case "Resources"->"Tick what you need. Scout will check each choice before offering a seed.";case "Nearby"->"Pick nearby biomes. Leave this empty if you do not mind.";
            case "Spawn"->"Start in one of these biomes. Leave empty for any biome.";case "Rocks"->"Pick the rock beneath your spawn. Leave empty for any rock.";
            case "Forest"->"Pick the kind of forest you want around spawn.";default->"Distances use blocks. For land and grass, 0.5 means 50%.";};
        int left=Math.max(8,(width-500)/2);g.drawString(font,font.plainSubstrByWidth(help,Math.max(60,Math.min(500,width-16)-150)),left,62,0xb0b0b0);
        if(!query.isEmpty()&&shown()==0)g.drawCenteredString(font,Lang.t("terrafirmascout.spec.no_matches"),width/2,100,0xa0a0a0);
        if(tab.equals("Limits"))for(var e:fields.entrySet())if(e.getValue().visible)g.drawString(font,font.plainSubstrByWidth(settingLabel(e.getKey()),Math.max(80,width-140-left)),left,e.getValue().getY()+4,0xd0d0d0);
        if(error!=null&&error.isEmpty()&&shown()*24>height-136)g.drawCenteredString(font,"Scroll to see more",width/2,height-43,0xa0a0a0);
        g.drawCenteredString(font,font.plainSubstrByWidth(error==null?"Check your wishlist":error,width-20),width/2,height-43,0xff7777);
    }
    @Override public boolean mouseScrolled(double x,double y,double h,double v){offset=Math.max(0,Math.min(Math.max(0,shown()*24-(height-136)),offset-(int)(v*24)));position();return true;}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
