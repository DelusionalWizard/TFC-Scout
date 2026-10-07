package com.cooper.terrafirmascout.mixin;
import java.util.concurrent.ExecutorService;
import net.dries007.tfc.world.TFCChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.cooper.terrafirmascout.tfc.ScratchExecutors;
/** Only tasks already running in a Scout scratch executor stay in that executor. */
@Mixin(TFCChunkGenerator.class)
public abstract class ScoutGenerationExecutor {
    @Redirect(method={"createBiomes", "fillFromNoise"}, at=@At(value="INVOKE", target="Lnet/minecraft/Util;backgroundExecutor()Ljava/util/concurrent/ExecutorService;"))
    private ExecutorService scout$executor() { return ScratchExecutors.currentOrGameExecutor(); }
}
