package com.cooper.terrafirmascout.mixin;
import net.dries007.tfc.world.layer.framework.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=ConcurrentArea.class, remap=false)
public interface ConcurrentAreaAccess { @Accessor("area") ThreadLocal<Area> scout$area(); }
