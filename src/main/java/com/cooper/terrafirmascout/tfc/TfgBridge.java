package com.cooper.terrafirmascout.tfc;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.layer.framework.AreaFactory;
import net.dries007.tfc.world.region.RegionGenerator;
/**
 * Optional support for TerraFirmaGreg-Core, found only by name so Scout has no compile or run-time dependency on it.
 * TerraFirmaGreg replaces TFC's biome layers with its own and builds part of its region generator from one global world seed
 * ({@code Seed.worldSeed}) that it also re-reads while generating chunks. Two consequences for Scout:
 * a seed-local adapter must set that value while it constructs a region generator and use TerraFirmaGreg's biome layers, and nothing may change the value
 * while a scratch world is generating. {@link #hold} serializes those two uses. Without TerraFirmaGreg this is all a no-op.
 */
public final class TfgBridge {
    private static final MethodHandle USE_PIPELINE,CREATE_LAYER,FROM_LAYER_ID; private static final Field WORLD_SEED;
    private static final ReentrantLock LOCK=new ReentrantLock(true);
    static {
        MethodHandle pipeline=null,layer=null,ids=null; Field seed=null;
        try {
            var loader=TfgBridge.class.getClassLoader(); var lookup=MethodHandles.publicLookup();
            var state=Class.forName("su.terrafirmagreg.core.world.new_ow_wg.TfgClientPreviewState",true,loader);
            var layers=Class.forName("su.terrafirmagreg.core.world.new_ow_wg.TFGLayers",true,loader);
            var seeds=Class.forName("su.terrafirmagreg.core.world.new_ow_wg.Seed",true,loader);
            pipeline=lookup.findStatic(state,"useTfgOverworldPipeline",MethodType.methodType(boolean.class));
            layer=lookup.findStatic(layers,"createRegionBiomeLayer",MethodType.methodType(AreaFactory.class,RegionGenerator.class,long.class));
            ids=lookup.findStatic(layers,"getFromLayerId",MethodType.methodType(BiomeExtension.class,int.class));
            seed=seeds.getField("worldSeed");
        } catch(ReflectiveOperationException|LinkageError e) { pipeline=layer=ids=null; seed=null; }
        USE_PIPELINE=pipeline; CREATE_LAYER=layer; FROM_LAYER_ID=ids; WORLD_SEED=seed;
    }
    private TfgBridge() {}
    public static boolean present() { return WORLD_SEED!=null; }
    /** True when TerraFirmaGreg would currently replace TFC's biome layers with its own, which is the decision its chunk generator makes. */
    public static boolean pipelineActive() {
        if(!present()) return false;
        try { return (boolean)USE_PIPELINE.invokeExact(); } catch(Throwable t) { throw rethrow(t); }
    }
    public static AreaFactory createLayer(RegionGenerator region,long seed) {
        try { return (AreaFactory)CREATE_LAYER.invokeExact(region,seed); } catch(Throwable t) { throw rethrow(t); }
    }
    public static BiomeExtension biomeForLayerId(int id) {
        try { return (BiomeExtension)FROM_LAYER_ID.invokeExact(id); } catch(Throwable t) { throw rethrow(t); }
    }
    /** An open hold on the global seed. Close it on the thread that opened it. */
    public static final class Hold implements AutoCloseable {
        private final boolean locked; private final long previous;
        private Hold(boolean locked,long previous) { this.locked=locked; this.previous=previous; }
        @Override public void close() {
            if(!locked) return;
            try { WORLD_SEED.setLong(null,previous); } catch(IllegalAccessException e) { throw new IllegalStateException(e); } finally { LOCK.unlock(); }
        }
    }
    /**
     * Takes exclusive use of TerraFirmaGreg's global world seed and sets it to {@code seed}. Waits politely so a cancelled search is not stuck here.
     * A no-op hold when TerraFirmaGreg is absent.
     */
    public static Hold hold(long seed) {
        if(!present()) return new Hold(false,0);
        try { while(!LOCK.tryLock(50,TimeUnit.MILLISECONDS)) ScanLimit.checkCancelled();
            ScanLimit.restart(); }
        catch(InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("Interrupted while waiting to start a seed",e); }
        try { long previous=WORLD_SEED.getLong(null); WORLD_SEED.setLong(null,seed); return new Hold(true,previous); }
        catch(IllegalAccessException|RuntimeException e) { LOCK.unlock(); throw new IllegalStateException("Cannot set TerraFirmaGreg's world seed",e); }
    }
    @SuppressWarnings("unchecked") private static <T extends Throwable> RuntimeException rethrow(Throwable t) throws T { throw (T)t; }
}
