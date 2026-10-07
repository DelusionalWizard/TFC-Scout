package com.cooper.terrafirmascout.tfc;
import java.util.*;
import com.cooper.terrafirmascout.score.Criterion;
import net.dries007.tfc.world.feature.vein.IVeinConfig;
import net.dries007.tfc.world.feature.vein.KaolinDiscVeinFeature;
import net.dries007.tfc.world.feature.vein.VeinFeature;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.registries.ForgeRegistries;
/**
 * The ore veins the selected world can really generate, learned from the world's own data rather than from names.
 * A pack may disable TFC's veins and add its own (TerraFirmaGreg does, with GregTech ore blocks, and with veins for other planets), so a vein only counts when a placed feature
 * for it is listed by a biome that this world's biome source can produce, and what a vein provides is read from the blocks it places.
 */
public final class VeinCatalog {
    /** One active vein: its configured feature, the placed features that make it generate here, the resources its blocks provide, and whether it spawns surface indicators. */
    public record Vein(ConfiguredFeature<?,?> feature,ResourceLocation id,List<PlacedFeature> placed,Set<Criterion> resources,boolean indicators) {}
    private static final Map<RegistryAccess,VeinCatalog> CACHE=Collections.synchronizedMap(new WeakHashMap<>());
    private final List<Vein> veins; private final Map<Criterion,Set<ResourceLocation>> blocks; private final Set<ResourceLocation> copperIndicators;
    private VeinCatalog(List<Vein> veins,Map<Criterion,Set<ResourceLocation>> blocks,Set<ResourceLocation> copperIndicators) { this.veins=List.copyOf(veins); this.blocks=blocks; this.copperIndicators=copperIndicators; }
    /** The catalog for a world's registries and biome source (cached per registry set). */
    public static VeinCatalog of(RegistryAccess registries,BiomeSource biomeSource) {
        return CACHE.computeIfAbsent(registries,r->build(r,biomeSource));
    }
    /** What a block provides, by the words in its name (so "gtceu:andesite_tin_ore" and "tfc:ore/poor_malachite/andesite" are both recognised). */
    public static Criterion resourceOf(ResourceLocation block) {
        var words=new HashSet<>(Arrays.asList(block.getPath().split("[/_]")));
        if(words.contains("cassiterite")||words.contains("tin")) return Criterion.TIN;
        if(words.contains("copper")||words.contains("malachite")||words.contains("tetrahedrite")) return Criterion.COPPER_VEIN;
        if(words.contains("graphite")) return Criterion.GRAPHITE;
        if(words.contains("hematite")||words.contains("limonite")||words.contains("magnetite")||words.contains("goethite")||words.contains("iron")) return Criterion.IRON;
        if(words.contains("coal")||words.contains("lignite")||words.contains("bituminous")||words.contains("anthracite")) return Criterion.COAL;
        if(words.contains("kaolin")) return Criterion.KAOLIN;
        return null;
    }
    private static VeinCatalog build(RegistryAccess registries,BiomeSource biomeSource) {
        var active=Collections.newSetFromMap(new IdentityHashMap<PlacedFeature,Boolean>());
        for(var biome:biomeSource.possibleBiomes()) for(var set:biome.value().getGenerationSettings().features()) for(var holder:set) active.add(holder.value());
        var placedRegistry=registries.registryOrThrow(Registries.PLACED_FEATURE);
        var veins=new ArrayList<Vein>(); var blocks=new EnumMap<Criterion,Set<ResourceLocation>>(Criterion.class); var copperIndicators=new HashSet<ResourceLocation>();
        for(var entry:registries.registryOrThrow(Registries.CONFIGURED_FEATURE).entrySet()) {
            var configured=entry.getValue();
            if(!(configured.feature() instanceof VeinFeature<?,?>)||!(configured.config() instanceof IVeinConfig config)) continue;
            var placed=new ArrayList<PlacedFeature>();
            for(var feature:placedRegistry) if(feature.feature().value()==configured&&active.contains(feature)) placed.add(feature);
            if(placed.isEmpty()) continue;
            var resources=EnumSet.noneOf(Criterion.class);
            for(var states:config.config().states().values()) for(var state:states.values()) {
                var id=ForgeRegistries.BLOCKS.getKey(state.getBlock()); var criterion=id==null?null:resourceOf(id);
                if(criterion!=null) { resources.add(criterion); blocks.computeIfAbsent(criterion,k->new HashSet<>()).add(id); }
            }
            // TFC's kaolin discs choose their clay in code and have no blocks in their config.
            if(configured.feature() instanceof KaolinDiscVeinFeature) resources.add(Criterion.KAOLIN);
            if(resources.isEmpty()) continue;
            if(resources.contains(Criterion.COPPER_VEIN)&&config.config().indicator().isPresent())
                for(var state:config.config().indicator().get().states().values()) { var id=ForgeRegistries.BLOCKS.getKey(state.getBlock()); if(id!=null) copperIndicators.add(id); }
            veins.add(new Vein(configured,entry.getKey().location(),placed,resources,config.config().indicator().isPresent()));
        }
        return new VeinCatalog(veins,blocks,copperIndicators);
    }
    public List<Vein> veins() { return veins; }
    /** True when some vein that can generate here provides this resource. */
    public boolean available(Criterion criterion) { for(var v:veins) if(v.resources().contains(criterion)) return true; return false; }
    /** The ore blocks that confirm this resource when found. Empty when no vein provides it. */
    public Set<ResourceLocation> oreBlocks(Criterion criterion) { return blocks.getOrDefault(criterion,Set.of()); }
    /** The small surface blocks that copper veins here spawn (TFC's small ore pieces), which is what the loose copper check looks for. */
    public Set<ResourceLocation> copperIndicatorBlocks() { return copperIndicators; }
}
