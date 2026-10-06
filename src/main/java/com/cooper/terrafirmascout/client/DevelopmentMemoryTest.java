package com.cooper.terrafirmascout.client;
import com.cooper.terrafirmascout.tfc.*;
import java.lang.ref.WeakReference;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
/** Opt-in retention regression; explicit GC is confined to this development harness. */
public final class DevelopmentMemoryTest {
    public static void run(SearchWorldContext context) throws Exception {
        var report=new StringBuilder("TFC 4.2.11 native seed cache retention regression\nTwo persistent scanner threads; identical deterministic biome/climate/rock queries.\nHeap measured after explicit GC at checkpoints; these are focused native scans, not full resource searches.\n");
        if(!Boolean.getBoolean("terrafirmascout.memoryScratchOnly")) {
            sample(context,true,100,new StringBuilder());
            long baseline=sample(context,false,100,report);
            long comparison=sample(context,true,100,report);
            long fixed=sample(context,true,1200,report);
            if(baseline!=comparison) throw new AssertionError("Cache cleanup changed native results");
            report.append("First 100 native output checksums equal before/after; 1200 fixed checksum: ").append(fixed).append('\n');
        }
        var references=new ArrayList<WeakReference<Object>>();
        int initialThreads=threads();
        String hash=null;
        for(int i=0;i<24;i++) {
            var next=scratch(context,references);
            if(hash!=null&&!hash.equals(next)) throw new AssertionError("Scratch generation changed on repetition"); hash=next;
        }
        gc(); long alive=references.stream().filter(r->r.get()!=null).count();
        report.append("24 scratch worlds, repeated FEATURES hash: ").append(hash).append("\nScratch levels/generators retained after close+GC: ").append(alive).append("/72\nScratch generation threads: ").append(initialThreads).append(" before, ").append(threads()).append(" after\n");
        Files.writeString(Path.of(System.getProperty("terrafirmascout.reportDir"),"memory-regression.txt"),report);
        System.out.println(report);
        if(alive!=0||threads()!=initialThreads) throw new AssertionError("Scratch worlds or generation threads retained");
    }
    // Returning from a helper also removes the final closed world's local variables from GC roots.
    private static String scratch(SearchWorldContext context,List<WeakReference<Object>> references) throws Exception {
        try(var world=new ScratchWorld(context,123456789L)) {
            var chunk=world.chunk(0,0);
            references.add(new WeakReference<>(world.level));
            references.add(new WeakReference<>(world.level.getChunkSource().getGenerator()));
            references.add(new WeakReference<>(Objects.requireNonNull(((com.cooper.terrafirmascout.mixin.RegionBiomeSourceAccess)(Object)world.level.getChunkSource().getGenerator().getBiomeSource()).scout$regionGenerator())));
            var digest=java.security.MessageDigest.getInstance("SHA-256");
            var bytes=java.nio.ByteBuffer.allocate(4);
            for(int y=chunk.getMinBuildHeight();y<chunk.getMaxBuildHeight();y++) for(int x=0;x<16;x++) for(int z=0;z<16;z++) {
                bytes.clear();bytes.putInt(net.minecraft.world.level.block.Block.getId(chunk.getBlockState(new net.minecraft.core.BlockPos(x,y,z))));digest.update(bytes.array());
            }
            return HexFormat.of().formatHex(digest.digest());
        }
    }
    private static long sample(SearchWorldContext context,boolean cleanup,int total,StringBuilder report) throws Exception {
        var pool=Executors.newFixedThreadPool(2);
        var refs=Collections.synchronizedList(new ArrayList<WeakReference<Object>>());
        long checksum=0; int completed=0;
        gc();report.append(cleanup?"FIXED":"BASELINE (cleanup disabled)").append(" start heap MiB=").append(heap()).append('\n');
        try {
            for(int end:new int[]{100,250,500,1000,1200}) {
                if(end>total) break;
                long started=System.nanoTime();
                while(completed<end) {
                    int first=completed;int count=Math.min(2,end-completed);
                    var futures=new ArrayList<Future<Long>>();
                    for(int i=0;i<count;i++) { final int index=first+i; futures.add(pool.submit(()->query(context,index,cleanup,refs))); }
                    for(var future:futures) checksum+=future.get(); completed+=count;
                }
                double seconds=(System.nanoTime()-started)/1e9;
                gc(); long alive=refs.stream().filter(r->r.get()!=null).count();
                var line="  seeds="+completed+", retained regions="+alive+", heap MiB="+heap()+", batch seconds="+seconds+"\n";
                report.append(line);System.out.print(line);
                if(cleanup&&alive!=0) throw new AssertionError("Seed regions retained after cleanup: "+alive);
                if(!cleanup&&alive!=completed) throw new AssertionError("Baseline did not reproduce seed retention: "+alive);
            }
        } finally { pool.shutdown();if(!pool.awaitTermination(30,TimeUnit.SECONDS)) throw new AssertionError("Scanner threads did not close"); }
        gc();report.append("  After scanner thread exit: retained regions=").append(refs.stream().filter(r->r.get()!=null).count()).append(", heap MiB=").append(heap()).append('\n');
        return checksum;
    }
    private static long query(SearchWorldContext context,int index,boolean cleanup,List<WeakReference<Object>> refs) {
        var adapter=new TFCWorldgenAdapter(new SplittableRandom(index).nextLong(),context.settings(),context.biomes());
        refs.add(new WeakReference<>(adapter.region));long hash=1;
        try {
            for(int i=0;i<9;i++) {
                int x=(i%3-1)*512,z=(i/3-1)*512;
                hash=31*hash+adapter.biome(x,z).key().location().hashCode();
                hash=31*hash+Float.floatToIntBits(adapter.data(x,z).getAverageRainfall(x,z));
                hash=31*hash+adapter.rock(x,64,z,64).hashCode();
            }
            return hash;
        } finally { if(cleanup) adapter.releaseThreadCaches(); }
    }
    private static int threads() { return (int)Thread.getAllStackTraces().keySet().stream().filter(t->t.isAlive()&&t.getName().equals("TerraFirmaScout chunk generator")).count(); }
    private static long heap() { var runtime=Runtime.getRuntime(); return (runtime.totalMemory()-runtime.freeMemory())/(1024*1024); }
    private static void gc() throws InterruptedException { System.gc();Thread.sleep(200);System.gc();Thread.sleep(200); }
}
