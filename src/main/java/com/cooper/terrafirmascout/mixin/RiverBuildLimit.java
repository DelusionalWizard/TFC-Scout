package com.cooper.terrafirmascout.mixin;
import net.dries007.tfc.world.river.River;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.cooper.terrafirmascout.tfc.ScanLimit;
/**
 * TFC's river builder tests each new edge against every existing edge, which takes minutes on rare seeds. This only adds a check that
 * fires on a Scout scan thread that asked to be abandoned or cancelled; for every other thread it does nothing and results are unchanged.
 */
@Mixin(value=River.MultiParallelBuilder.class, remap=false)
public abstract class RiverBuildLimit {
    @Inject(method="intersectAny", at=@At("HEAD"))
    private void scout$checkAbandon(River.Edge edge,CallbackInfoReturnable<Boolean> cir) { ScanLimit.check(); }
}
