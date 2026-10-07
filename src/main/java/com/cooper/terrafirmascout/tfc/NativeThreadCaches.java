package com.cooper.terrafirmascout.tfc;
import com.cooper.terrafirmascout.mixin.*;
import net.dries007.tfc.world.layer.framework.ConcurrentArea;
import net.dries007.tfc.world.region.RegionGenerator;
/** Remove only this seed's native caches, only on the calling thread. */
public final class NativeThreadCaches {
    private NativeThreadCaches() {}
    public static void clear(RegionGenerator region, Object chunkDataGenerator, ConcurrentArea<?> biomeLayer) {
        if(biomeLayer!=null) clear(biomeLayer);
        if(region!=null) { region.biomeArea.remove(); region.rockArea.remove(); }
        if(chunkDataGenerator instanceof RegionChunkDataAccess data) {
            data.scout$rockLayerArea().remove(); clear(data.scout$forestTypeLayer());
        }
    }
    private static void clear(ConcurrentArea<?> layer) { ((ConcurrentAreaAccess)(Object)layer).scout$area().remove(); }
    /** Clears the biome source layers and the chunk data generator of a TFC chunk generator, on the calling thread only. */
    public static void clearGenerator(net.minecraft.world.level.chunk.ChunkGenerator generator) {
        Object data=generator instanceof net.dries007.tfc.world.TFCChunkGenerator tfc&&tfc.chunkDataProvider()!=null?tfc.chunkDataProvider().generator():null;
        if(generator.getBiomeSource() instanceof RegionBiomeSourceAccess b) clear(b.scout$regionGenerator(),data,b.scout$biomeLayer()); else if(data!=null) clear(null,data,null);
    }
    public static void clearBiomeSource(Object source) {
        if(source instanceof RegionBiomeSourceAccess b) clear(b.scout$regionGenerator(),null,b.scout$biomeLayer());
    }
}
