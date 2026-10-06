package com.cooper.terrafirmascout.mixin;
import net.dries007.tfc.world.biome.RegionBiomeSource;
import net.dries007.tfc.world.layer.framework.ConcurrentArea;
import net.dries007.tfc.world.region.RegionGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=RegionBiomeSource.class, remap=false)
public interface RegionBiomeSourceAccess {
    @Accessor("regionGenerator") RegionGenerator scout$regionGenerator();
    @Accessor("biomeLayer") ConcurrentArea<?> scout$biomeLayer();
}
