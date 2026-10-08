package com.cooper.terrafirmascout.tfc;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.BiomeSource;
/**
 * Groundwork for animal checks: which land animals the selected world's biomes list as spawns, read from the biome data rather than from names, so addon
 * animals and packs that change spawns are covered. It is a list of what a biome may spawn, not proof that an animal will be near a given spot (spawning also
 * depends on the animal's own rules and on chance), so any check built on it has to say "can spawn" and not "will be there".
 */
public final class AnimalCatalog {
    private static final Map<BiomeSource,AnimalCatalog> CACHE=Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<ResourceLocation,Map<ResourceLocation,Integer>> byBiome; private final Set<ResourceLocation> species;
    private AnimalCatalog(Map<ResourceLocation,Map<ResourceLocation,Integer>> byBiome) {
        this.byBiome=byBiome; var all=new TreeSet<ResourceLocation>(); byBiome.values().forEach(m->all.addAll(m.keySet())); this.species=Collections.unmodifiableSet(all);
    }
    /** The catalog for a biome source (cached per source). */
    public static AnimalCatalog of(BiomeSource source) { return CACHE.computeIfAbsent(source,AnimalCatalog::build); }
    private static AnimalCatalog build(BiomeSource source) {
        var result=new HashMap<ResourceLocation,Map<ResourceLocation,Integer>>();
        for(var holder:source.possibleBiomes()) {
            var key=holder.unwrapKey(); if(key.isEmpty()) continue;
            var animals=new TreeMap<ResourceLocation,Integer>();
            for(var spawn:holder.value().getMobSettings().getMobs(MobCategory.CREATURE).unwrap()) {
                var id=BuiltInRegistries.ENTITY_TYPE.getKey(spawn.type); if(id!=null) animals.merge(id,spawn.getWeight().asInt(),Integer::sum);
            }
            if(!animals.isEmpty()) result.put(key.get().location(),Collections.unmodifiableMap(animals));
        }
        return new AnimalCatalog(Collections.unmodifiableMap(result));
    }
    /** Every animal any biome of this world lists. */
    public Set<ResourceLocation> species() { return species; }
    /** The animals one biome lists, with their spawn weights (empty when it lists none). */
    public Map<ResourceLocation,Integer> inBiome(ResourceLocation biome) { return byBiome.getOrDefault(biome,Map.of()); }
    /** How many different animals the given biomes list between them. */
    public int speciesIn(Collection<ResourceLocation> biomes) {
        var found=new HashSet<ResourceLocation>(); for(var biome:biomes) found.addAll(inBiome(biome).keySet()); return found.size();
    }
    private static final int FARM_NEEDED=3,PREY_NEEDED=2;
    private volatile Set<ResourceLocation> farmIds,preyIds;
    /** What the biomes in an area list: farm animals (livestock you can keep) and wild prey (what you can hunt), by TFC's own entity tags. */
    public record Fit(int farm,int prey,boolean farmListed,boolean preyListed,List<String> farmNames) {
        public boolean ok() { return (!farmListed||farm>=FARM_NEEDED)&&(!preyListed||prey>=PREY_NEEDED); }
        public String describe(int distance) {
            if(!farmListed&&!preyListed) return "This world lists no farm or wild animals in its biomes, so this was not required";
            String names=farmNames.isEmpty()?"":" ("+String.join(", ",farmNames.subList(0,Math.min(5,farmNames.size())))+(farmNames.size()>5?", ...":"")+")";
            return "Within "+distance+" blocks the biomes list "+farm+" farm animals"+names+" and "+prey+" wild prey species that can spawn (wanted: "+(farmListed?FARM_NEEDED:0)+" and "+(preyListed?PREY_NEEDED:0)+"). Spawning is not guaranteed.";
        }
    }
    private Set<ResourceLocation> ids(TagKey<EntityType<?>> tag) {
        var found=new HashSet<ResourceLocation>();
        BuiltInRegistries.ENTITY_TYPE.getTag(tag).ifPresent(set->set.forEach(holder->holder.unwrapKey().ifPresent(k->found.add(k.location()))));
        return found;
    }
    /** How many farm animals and wild prey species the given biomes list between them. */
    public Fit fit(Collection<ResourceLocation> biomes) {
        if(farmIds==null) {
            farmIds=ids(TagKey.create(Registries.ENTITY_TYPE,ResourceLocation.fromNamespaceAndPath("tfc","farm_animals"))); preyIds=ids(TagKey.create(Registries.ENTITY_TYPE,ResourceLocation.fromNamespaceAndPath("tfc","wild_prey_animals")));
        }
        var here=new TreeSet<ResourceLocation>(); for(var biome:biomes) here.addAll(inBiome(biome).keySet());
        var farm=new ArrayList<String>(); int prey=0;
        for(var id:here) { if(farmIds.contains(id)) farm.add(id.getPath()); if(preyIds.contains(id)) prey++; }
        boolean farmListed=species.stream().anyMatch(farmIds::contains),preyListed=species.stream().anyMatch(preyIds::contains);
        return new Fit(farm.size(),prey,farmListed,preyListed,farm);
    }
    /** Number of biomes that list at least one animal. */
    public int biomeCount() { return byBiome.size(); }
}
