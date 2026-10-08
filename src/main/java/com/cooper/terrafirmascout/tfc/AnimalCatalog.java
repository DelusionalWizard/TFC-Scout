package com.cooper.terrafirmascout.tfc;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.MobCategory;
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
    /** Number of biomes that list at least one animal. */
    public int biomeCount() { return byBiome.size(); }
}
