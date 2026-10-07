package com.cooper.terrafirmascout.scanner;
import java.lang.reflect.Proxy;
import java.util.*;
import com.cooper.terrafirmascout.profile.ScoutProfile;
import com.cooper.terrafirmascout.score.*;
import com.cooper.terrafirmascout.search.*;
import com.cooper.terrafirmascout.tfc.TFCWorldgenAdapter;
import com.cooper.terrafirmascout.tfc.TfcCompat;
import com.cooper.terrafirmascout.tfc.VeinCatalog;
import net.dries007.tfc.world.feature.vein.*;
import net.dries007.tfc.world.placement.ClimatePlacement;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.ChunkPos;
public final class DetailedScanner {
    private static final Set<String> FLUX=Set.of("limestone","dolomite","chalk","marble");
    public static SeedCandidate scan(TFCWorldgenAdapter a,BlockPos center,ScoutProfile p,SearchSession s,
        RegistryAccess registries,ChunkGenerator generator) {
        var evidence=new EnumMap<Criterion,Evidence>(Criterion.class);
        var targets=new EnumMap<Criterion,List<BlockPos>>(Criterion.class);
        var hints=new EnumMap<Criterion,Map<BlockPos,List<SeedCandidate.VeinHint>>>(Criterion.class);
        for(var c:Criterion.values()) { evidence.put(c,Evidence.absent(p.requires(c)?"Still needs checking":"Not needed for this search")); targets.put(c,new ArrayList<>()); hints.put(c,new HashMap<>()); }
        int sx=center.getX(),sz=center.getZ();
        var climate=a.data(sx,sz); double temp=climate.getAverageTemp(sx,sz),rain=climate.getRainfall(sx,sz);
        if(p.requires(Criterion.CLIMATE)&&(temp<p.temperatureMin()||temp>p.temperatureMax()+20||rain<p.rainfallMin()||rain>p.rainfallMax())) return null;
        evidence.put(Criterion.CLIMATE,Evidence.inferred(0,sx,sz,"TFC climate %.2f C / %.2f mm; final spawn pending".formatted(temp,rain)));
        var rocks=new HashSet<String>();
        boolean sample=p.requires(Criterion.FLUX)||p.requires(Criterion.FOREST)||p.requires(Criterion.TERRAIN)||p.requires(Criterion.DIVERSITY);
        for(int dx=-p.analysisRadius();sample&&dx<=p.analysisRadius();dx+=128) { s.checkpoint();
            for(int dz=-p.analysisRadius();dz<=p.analysisRadius();dz+=128) {
                double dist=Math.hypot(dx,dz); if(dist>p.analysisRadius()) continue;
                int x=sx+dx,z=sz+dz; if(!a.land(x,z)) continue;
                var data=a.data(x,z); String rock=p.requires(Criterion.FLUX)||p.requires(Criterion.DIVERSITY)?a.rock(x,64,z,64):""; if(p.requires(Criterion.DIVERSITY))rocks.add(rock);
                if(p.requires(Criterion.FLUX)&&FLUX.contains(rock)&&dist<=p.distance(Criterion.FLUX)) targets.get(Criterion.FLUX).add(new BlockPos(x,64,z));
                if(p.requires(Criterion.FOREST)&&dist<=p.distance(Criterion.FOREST)&&TfcCompat.density(data.getForestType())>=1)
                    targets.get(Criterion.FOREST).add(new BlockPos(x,0,z));
                if(p.requires(Criterion.TERRAIN)&&dist<=p.terrainRadius()&&Set.of("plains","lowlands","hills","rolling_hills").contains(a.biome(x,z).key().location().getPath()))
                    targets.get(Criterion.TERRAIN).add(new BlockPos(x,0,z));
            }
        }
        // Refine small-radius resources; climate/biome conditions are candidates, never guarantees.
        int nearby=Math.max(p.requires(Criterion.CLAY)?p.distance(Criterion.CLAY):0,Math.max(p.requires(Criterion.RIVER)?p.distance(Criterion.RIVER):0,Math.max(p.requires(Criterion.LAKE)?p.distance(Criterion.LAKE):0,p.requires(Criterion.COAST)?p.distance(Criterion.COAST):0))); int step=nearby>800?64:16;
        for(int dx=-nearby;dx<=nearby;dx+=step) { s.checkpoint();
            for(int dz=-nearby;dz<=nearby;dz+=step) {
                double d=Math.hypot(dx,dz); int x=sx+dx,z=sz+dz;
                var b=a.biome(x,z);
                String waterPath=b.key().location().getPath();
                if(p.requires(Criterion.RIVER)&&d<=p.distance(Criterion.RIVER)&&!b.isSalty()&&waterPath.contains("river")) targets.get(Criterion.RIVER).add(new BlockPos(x,0,z));
                if(p.requires(Criterion.LAKE)&&d<=p.distance(Criterion.LAKE)&&!b.isSalty()&&waterPath.contains("lake")) targets.get(Criterion.LAKE).add(new BlockPos(x,0,z));
                if(p.requires(Criterion.COAST)&&d<=p.distance(Criterion.COAST)&&(b.isSalty()||b.isShore())) targets.get(Criterion.COAST).add(new BlockPos(x,0,z));
                if(p.requires(Criterion.CLAY)&&d<=p.distance(Criterion.CLAY)&&a.land(x,z))
                    targets.get(Criterion.CLAY).add(new BlockPos(x,0,z));
            }
        }
        if(p.requires(Criterion.OPEN_GROUND))targets.get(Criterion.OPEN_GROUND).add(center);
        // TFC's real vein-center algorithm, using the active configured-feature registry and its seed salts.
        // These centers remain INFERRED until real feature-stage blocks are inspected.
        var proxy=(WorldGenLevel)Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(),new Class<?>[]{WorldGenLevel.class},(o,m,args)->switch(m.getName()) {
            // Development names, then the obfuscated names a real install uses (WorldGenLevel.getSeed, LevelHeightAccessor.getMinBuildHeight/getHeight/getMaxBuildHeight).
            case "getSeed","m_7328_"->a.seed; case "getMinBuildHeight","m_141937_"->generator.getMinY();
            case "getHeight","m_141928_"->generator.getGenDepth(); case "getMaxBuildHeight","m_151558_"->generator.getMinY()+generator.getGenDepth();
            default->throw new UnsupportedOperationException("Vein query requested "+m.getName());
        });
        var ctx=new WorldGenerationContext(generator,proxy);
        // Only veins this world can generate (listed by a biome its biome source produces), each for the resources its blocks provide.
        var catalog=VeinCatalog.of(registries,generator.getBiomeSource());
        for(var vein:catalog.veins()) for(var criterion:vein.resources()) {
            if(!(p.requires(criterion)||criterion==Criterion.COPPER_VEIN&&p.requires(Criterion.STARTER_COPPER))) continue;
            addVeins(vein.feature(),criterion,proxy,ctx,a,center,p,s,targets,vein.placed(),hints);
        }
        for(var c:Criterion.values()) {
            var list=targets.get(c); list.sort(Comparator.comparingDouble(b->{
                double distance=distance(center,b);
                if(c==Criterion.CLAY){String biome=a.biome(b.getX(),b.getZ()).key().location().getPath();
                    boolean wet=biome.contains("river")||biome.contains("lake");
                    if(!wet){for(int dx:new int[]{-16,16})for(int dz:new int[]{-16,16}){String edge=a.biome(b.getX()+dx,b.getZ()+dz).key().location().getPath();if(edge.contains("river")||edge.contains("lake"))wet=true;}}
                    return distance+(wet?0:200);
                }
                return distance;
            }));
            // Deduplicate target chunks and cap seed-local memory; verification may reject a missed positive.
            var unique=new LinkedHashMap<Long,BlockPos>();
            for(var pos:list) unique.putIfAbsent(new ChunkPos(pos).toLong(),pos);
            list.clear(); unique.values().stream().limit(2048).forEach(list::add);
            if(!list.isEmpty()) { var pos=list.get(0); evidence.put(c,Evidence.inferred(distance(center,pos),pos.getX(),pos.getZ(),"Looks promising; needs a closer look")); }
        }
        if(p.requires(Criterion.DIVERSITY))evidence.put(Criterion.DIVERSITY,new Evidence(VerificationState.VERIFIED,0,sx,0,sz,Math.min(1,rocks.size()/8.0),rocks.size()+" rock types found in nearby samples"));
        // No false resource rejection on a coarse geology sample. Missing targets get a bounded exact fallback.
        return new SeedCandidate(a,center,evidence,targets,hints);
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    private static void addVeins(ConfiguredFeature f,Criterion c,WorldGenLevel level,WorldGenerationContext ctx,
        TFCWorldgenAdapter a,BlockPos center,ScoutProfile p,SearchSession s,Map<Criterion,List<BlockPos>> targets,
        List<net.minecraft.world.level.levelgen.placement.PlacedFeature> placed,Map<Criterion,Map<BlockPos,List<SeedCandidate.VeinHint>>> hints) {
        VeinFeature feature=(VeinFeature)f.feature(); IVeinConfig config=(IVeinConfig)f.config();
        int limit=c==Criterion.COPPER_VEIN&&p.requires(Criterion.STARTER_COPPER)?Math.max(p.requires(c)?p.distance(c):0,p.distance(Criterion.STARTER_COPPER)):p.distance(c);
        int radius=limit+config.chunkRadius()*16;
        var hosts=new HashSet<String>();for(var block:config.config().states().keySet())hosts.add(net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(block).getPath().replace("rock/raw/",""));
        var hint=new SeedCandidate.VeinHint(config.config().projectToSurface(),config.verticalRadius(),Set.copyOf(hosts));
        for(int cx=(center.getX()-radius)>>4;cx<=(center.getX()+radius)>>4;cx++) { s.checkpoint();
            for(int cz=(center.getZ()-radius)>>4;cz<=(center.getZ()+radius)>>4;cz++) {
                var veins=new ArrayList<IVein>(); feature.getVeinsAtChunk(level,ctx,cx,cz,veins,config,(java.util.function.Function<BlockPos,Holder<net.minecraft.world.level.biome.Biome>>)(at->a.biomes.getBiome(QuartPos.fromBlock(at.getX()),QuartPos.fromBlock(at.getZ()))));
                for(var v:veins) {
                    var pos=v.pos(); if(distance(center,pos)>limit) continue;
                    if(c==Criterion.KAOLIN) {
                        // Call the active native climate modifier, and require the feature in the active biome.
                        // Full groundwater/elevation/placement still require the real FEATURES stage.
                        var biome=a.biomes.getBiome(QuartPos.fromBlock(pos.getX()),QuartPos.fromBlock(pos.getZ())).value();
                        boolean suitable=false;
                        for(var placedFeature:placed) {
                            boolean inBiome=biome.getGenerationSettings().features().stream().flatMap(set->set.stream()).anyMatch(h->h.value()==placedFeature);
                            if(!inBiome) continue;
                            boolean climate=placedFeature.placement().stream().filter(mod->mod instanceof ClimatePlacement).allMatch(mod->
                                ((ClimatePlacement)mod).isValid(a.data(pos.getX(),pos.getZ()),new BlockPos(pos.getX(),0,pos.getZ()),
                                    new net.minecraft.world.level.levelgen.XoroshiroRandomSource(a.seed^pos.asLong())));
                            if(climate) { suitable=true; break; }
                        }
                        if(!suitable) continue;
                    }
                    if(p.requires(c)&&distance(center,pos)<=p.distance(c)){targets.get(c).add(pos);hints.get(c).computeIfAbsent(pos,k->new ArrayList<>()).add(hint);}
                    if(c==Criterion.COPPER_VEIN&&p.requires(Criterion.STARTER_COPPER)&&distance(center,pos)<=p.distance(Criterion.STARTER_COPPER)){
                        targets.get(Criterion.STARTER_COPPER).add(pos);hints.get(Criterion.STARTER_COPPER).computeIfAbsent(pos,k->new ArrayList<>()).add(hint);
                    }
                }
            }
        }
    }
    public static double distance(BlockPos a,BlockPos b) { return Math.hypot((double)a.getX()-b.getX(),(double)a.getZ()-b.getZ()); }
}
