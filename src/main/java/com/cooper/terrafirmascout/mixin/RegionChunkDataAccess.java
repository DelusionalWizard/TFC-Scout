package com.cooper.terrafirmascout.mixin;
import net.dries007.tfc.world.chunkdata.RegionChunkDataGenerator;
import net.dries007.tfc.world.layer.framework.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=RegionChunkDataGenerator.class, remap=false)
public interface RegionChunkDataAccess {
    @Accessor("rockLayerArea") ThreadLocal<Area> scout$rockLayerArea();
    @Accessor("forestTypeLayer") ConcurrentArea<?> scout$forestTypeLayer();
}
