package com.cooper.terrafirmascout.tfc;
import com.cooper.terrafirmascout.mixin.*;
import net.dries007.tfc.world.layer.framework.ConcurrentArea;
import net.dries007.tfc.world.region.RegionGenerator;
/** Remove only this seed's native caches, only on the calling thread. */
public final class NativeThreadCaches {
    private NativeThreadCaches() {}
    public static void clear(RegionGenerator region, ConcurrentArea<?> biomeLayer) {
        if(biomeLayer!=null) clear(biomeLayer);
        if(region==null) return;
        region.biomeArea.remove(); region.rockArea.remove();
        if(region.chunkDataGenerator() instanceof RegionChunkDataAccess data) {
            data.scout$rockLayerArea().remove(); clear(data.scout$forestTypeLayer());
        }
    }
    private static void clear(ConcurrentArea<?> layer) { ((ConcurrentAreaAccess)(Object)layer).scout$area().remove(); }
    public static void clearBiomeSource(Object source) {
        if(source instanceof RegionBiomeSourceAccess b) clear(b.scout$regionGenerator(),b.scout$biomeLayer());
    }
}
