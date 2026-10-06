package com.cooper.terrafirmascout.tfc;
import java.util.*;
import net.dries007.tfc.world.Seed;
import net.dries007.tfc.world.biome.*;
import net.dries007.tfc.world.chunkdata.*;
import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.layer.framework.ConcurrentArea;
import net.dries007.tfc.world.region.*;
import net.dries007.tfc.world.settings.*;
import net.minecraft.core.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
/** One seed-local adapter per worker. The Seed.next sequence matches TFCChunkGenerator.initRandomState. */
public final class TFCWorldgenAdapter {
    public final long seed; public final Settings settings; public final RegionGenerator region;
    public final BiomeSourceExtension biomes;
    private final ConcurrentArea<BiomeExtension> biomeLayer;
    private final Map<Long,ChunkData> data=new LinkedHashMap<>(256,0.75f,true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Long,ChunkData> e) { return size()>4096; }
    };
    public TFCWorldgenAdapter(long seed,Settings settings,BiomeSourceExtension source) {
        this.seed=seed; this.settings=settings;
        var sequence=Seed.of(seed);
        region=new RegionGenerator(settings,sequence);
        biomeLayer=new ConcurrentArea<>(TFCLayers.createRegionBiomeLayer(region,sequence),TFCLayers::getFromLayerId);
        biomes=source.copy();
        if(biomes==source) throw new IllegalArgumentException("Biome source cannot be isolated");
        biomes.initRandomState(region,biomeLayer);
    }
    public void releaseThreadCaches() { NativeThreadCaches.clear(region,biomeLayer); }
    public BlockPos spawnBiome() { return biomes.findSpawnBiome(settings,new XoroshiroRandomSource(seed)); }
    public Region.Point point(int x,int z) { return region.getOrCreateRegionPoint(Units.blockToGrid(x),Units.blockToGrid(z)); }
    public BiomeExtension biome(int x,int z) { return biomes.getBiomeExtension(QuartPos.fromBlock(x),QuartPos.fromBlock(z)); }
    public ChunkData data(int x,int z) {
        var p=new ChunkPos(x>>4,z>>4);
        return data.computeIfAbsent(p.toLong(),k->region.chunkDataGenerator().generate(new ChunkData(region.chunkDataGenerator(),p)));
    }
    public String rock(int x,int y,int z,int surface) {
        var r=region.chunkDataGenerator().generateRock(x,y,z,surface,null);
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(r.raw()).getPath().replace("rock/raw/","");
    }
    public boolean land(int x,int z) { var b=biome(x,z); return !b.isSalty()&&!b.isShore(); }
}
