package com.cooper.terrafirmascout.config;
import java.util.*;
import com.cooper.terrafirmascout.profile.ScoutProfile;
import com.cooper.terrafirmascout.profile.SeedQuality;
import com.cooper.terrafirmascout.score.Criterion;
import net.minecraftforge.common.ForgeConfigSpec;
public final class ScoutConfig {
    public static final ForgeConfigSpec COMMON,CLIENT;
    public static final ForgeConfigSpec.IntValue WORKERS,TARGET_CHUNKS,MAX_SEEDS,FINALISTS;
    public static final ForgeConfigSpec.BooleanValue REVEAL;
    public static final Map<String,ForgeConfigSpec.DoubleValue> CUSTOM=new LinkedHashMap<>();
    static {
        var b=new ForgeConfigSpec.Builder(); b.push("search");
        WORKERS=b.comment("How many seeds to shortlist at once. Close-up world checks still run one at a time.")
            .defineInRange("max_workers",Math.max(1,Math.min(4,Runtime.getRuntime().availableProcessors()-2)),1,32);
        TARGET_CHUNKS=b.comment("Chunks to check for each resource. Higher values can find more supplies, but take longer.")
            .defineInRange("target_chunks_per_resource",24,1,512);
        MAX_SEEDS=b.defineInRange("max_seeds_per_search",100000,1,10000000);
        FINALISTS=b.defineInRange("finalists_per_batch",5,1,20); b.pop();
        for(var quality:SeedQuality.values()) defineProfile(b,quality.id,ScoutProfile.preset(quality));
        defineProfile(b,"custom",ScoutProfile.beginner()); COMMON=b.build();
        var client=new ForgeConfigSpec.Builder(); client.push("display");
        REVEAL=client.define("reveal_locations",false); client.pop(); CLIENT=client.build();
    }
    private static void defineProfile(ForgeConfigSpec.Builder b,String name,ScoutProfile p) {
        b.push(name);
        define(b,name,"min_score",p.minScore(),p.quality()==SeedQuality.GOD?90:0,100); define(b,name,"search_radius",p.radius(),300,12000);
        define(b,name,"temperature_min",p.temperatureMin(),-20,40); define(b,name,"temperature_ideal_min",p.temperatureIdealMin(),-20,40);
        define(b,name,"temperature_ideal_max",p.temperatureIdealMax(),-20,40); define(b,name,"temperature_max",p.temperatureMax(),-20,40);
        define(b,name,"rainfall_min",p.rainfallMin(),0,500); define(b,name,"rainfall_ideal_min",p.rainfallIdealMin(),0,500);
        define(b,name,"rainfall_ideal_max",p.rainfallIdealMax(),0,500); define(b,name,"rainfall_max",p.rainfallMax(),0,500);
        define(b,name,"minimum_land_fraction",p.minimumLand(),p.quality()==SeedQuality.GOD?0.7:0.25,1); define(b,name,"land_radius",p.landRadius(),1000,4000);
        define(b,name,"terrain_distance",p.terrainRadius(),16,12000); define(b,name,"camp_distance",p.campRadius(),16,12000);
        define(b,name,"starter_copper_units",p.minimumCopperUnits(),100,10000); define(b,name,"starter_copper_pieces",p.minimumCopperPieces(),10,1000);
        define(b,name,"challenge_surface_span",p.minimumRoughness(),0,100);
        for(var c:p.distances().keySet().stream().sorted().toList()) define(b,name,c.name().toLowerCase(Locale.ROOT)+"_distance",p.distance(c),1,12000);
        b.pop();
    }
    private static void define(ForgeConfigSpec.Builder b,String group,String key,double value,double min,double max) {
        CUSTOM.put(group+"."+key,b.defineInRange(key,value,min,max));
    }
    public static ScoutProfile profile(String name) {
        var values=new LinkedHashMap<String,Double>();
        CUSTOM.forEach((k,v)-> { if(k.startsWith(name+".")) values.put(k.substring(name.length()+1),v.get()); });
        return fromValues(name,values);
    }
    public static ScoutProfile fromValues(String name,Map<String,Double> v) {
        var d=new EnumMap<Criterion,Integer>(Criterion.class);
        for(var c:ScoutProfile.beginner().distances().keySet()) d.put(c,v.get(c.name().toLowerCase(Locale.ROOT)+"_distance").intValue());
        return new ScoutProfile(name.equals("custom")?"Custom":SeedQuality.fromName(name).label,
            v.get("min_score").intValue(),v.get("search_radius").intValue(),v.get("temperature_min"),v.get("temperature_ideal_min"),
            v.get("temperature_ideal_max"),v.get("temperature_max"),v.get("rainfall_min"),v.get("rainfall_ideal_min"),v.get("rainfall_ideal_max"),
            v.get("rainfall_max"),v.get("minimum_land_fraction"),v.get("land_radius").intValue(),v.get("terrain_distance").intValue(),
            v.get("camp_distance").intValue(),v.get("starter_copper_units").intValue(),v.get("starter_copper_pieces").intValue(),true,d,v.get("challenge_surface_span").intValue());
    }
}
