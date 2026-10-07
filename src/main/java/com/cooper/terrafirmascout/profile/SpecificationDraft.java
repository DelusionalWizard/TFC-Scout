package com.cooper.terrafirmascout.profile;
import java.util.*;
import java.nio.file.*;
import com.google.gson.GsonBuilder;
import com.cooper.terrafirmascout.config.ScoutConfig;
import com.cooper.terrafirmascout.score.Criterion;
import net.minecraftforge.fml.loading.FMLPaths;
public final class SpecificationDraft {
    public Map<String,Double> numbers=new LinkedHashMap<>();
    public Set<Criterion> required=new HashSet<>();
    public Set<String> nearbyBiomes=new TreeSet<>(),spawnBiomes=new TreeSet<>(),spawnRocks=new TreeSet<>(),forestTypes=new TreeSet<>();
    public boolean allBiomes;
    public static SpecificationDraft fromPreset(ScoutProfile p) {
        var d=new SpecificationDraft();
        String id=p.name().equals("Custom")?"custom":p.quality().id;
        ScoutConfig.CUSTOM.forEach((k,v)-> { if(k.startsWith(id+"."))d.numbers.put(k.substring(id.length()+1),v.get()); });
        d.required.addAll(p.requiredCriteria()); d.required.remove(Criterion.SPAWN); d.required.remove(Criterion.SPECIFICATION);
        d.numbers.put("min_score",0d); d.numbers.put("biome_radius",1000d);
        d.numbers.put("forest_density_min",0d); d.numbers.put("forest_density_max",4d);
        d.numbers.put("spawn_elevation_min",-64d); d.numbers.put("spawn_elevation_max",320d);
        d.numbers.put("terrain_patch_size",16d); d.numbers.put("camp_patch_size",8d);
        d.numbers.put("maximum_slope",3d); d.numbers.put("minimum_grass_fraction",0.5d);
        return d;
    }
    private int integer(String key) { double n=numbers.get(key); if(!Double.isFinite(n)||n!=Math.rint(n))throw new IllegalArgumentException(key.replace('_',' ')+" needs a whole number"); return (int)n; }
    public ScoutProfile build() {
        for(var e:numbers.entrySet())if(!Double.isFinite(e.getValue()))throw new IllegalArgumentException(e.getKey().replace('_',' ')+" needs a number");
        var spec=new WorldSpecification(true,required,nearbyBiomes,allBiomes,integer("biome_radius"),spawnBiomes,spawnRocks,forestTypes,
            integer("forest_density_min"),integer("forest_density_max"),integer("spawn_elevation_min"),integer("spawn_elevation_max"),
            integer("terrain_patch_size"),integer("camp_patch_size"),integer("maximum_slope"),numbers.get("minimum_grass_fraction"));
        var distances=new EnumMap<Criterion,Integer>(Criterion.class);
        for(var c:ScoutProfile.beginner().distances().keySet())distances.put(c,integer(c.name().toLowerCase(Locale.ROOT)+"_distance"));
        if(!nearbyBiomes.isEmpty()&&spec.biomeRadius()>integer("search_radius"))throw new IllegalArgumentException("The biome search cannot be larger than your overall search area");
        return new ScoutProfile("Specification",integer("min_score"),integer("search_radius"),numbers.get("temperature_min"),numbers.get("temperature_ideal_min"),
            numbers.get("temperature_ideal_max"),numbers.get("temperature_max"),numbers.get("rainfall_min"),numbers.get("rainfall_ideal_min"),
            numbers.get("rainfall_ideal_max"),numbers.get("rainfall_max"),numbers.get("minimum_land_fraction"),integer("land_radius"),
            integer("terrain_distance"),integer("camp_distance"),integer("starter_copper_units"),integer("starter_copper_pieces"),required.contains(Criterion.CONNECTIVITY),distances,integer("challenge_surface_span"),spec);
    }
    public void save() throws Exception {
        build(); var path=FMLPaths.GAMEDIR.get().resolve("terrafirmascout/specification.json"); Files.createDirectories(path.getParent());
        Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(this));
    }
    public static SpecificationDraft load(ScoutProfile fallback) {
        var path=FMLPaths.GAMEDIR.get().resolve("terrafirmascout/specification.json");
        try {
            var d=new GsonBuilder().create().fromJson(Files.readString(path),SpecificationDraft.class);
            fromPreset(fallback).numbers.forEach(d.numbers::putIfAbsent); // settings saved by an older version lack newer limits such as the iron and coal distances
            d.build(); return d;
        }
        catch(Exception e) { return fromPreset(fallback); }
    }
}
