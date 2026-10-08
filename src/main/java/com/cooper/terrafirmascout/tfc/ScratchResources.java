package com.cooper.terrafirmascout.tfc;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.world.level.storage.LevelStorageSource;
/**
 * The data pack repository and resource manager every scratch world of one search needs. Opening them reads every installed mod and pack, which takes seconds in a
 * large modpack, so one set is shared by all scratch worlds that use the same loaded data, and is closed a short while after the last one is done.
 * It is only read from, so scratch worlds can use it at the same time.
 */
final class ScratchResources {
    private static final long IDLE_NANOS=TimeUnit.SECONDS.toNanos(20);
    private static final Map<Object,ScratchResources> OPEN=new HashMap<>();
    private static final ScheduledExecutorService TIMER=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"TerraFirmaScout resource cleanup");t.setDaemon(true);return t;});
    final PackRepository packs; final CloseableResourceManager resources;
    private final Object key; private final Path root; private final LevelStorageSource.LevelStorageAccess storage;
    private int users; private long idleSince;
    private ScratchResources(Object key,SearchWorldContext c) throws IOException {
        this.key=key; root=Files.createTempDirectory("terrafirmascout-");
        LevelStorageSource.LevelStorageAccess tempStorage=null; CloseableResourceManager tempResources=null; PackRepository tempPacks=null;
        try {
            tempStorage=LevelStorageSource.createDefault(root).createAccess("shared");
            if(c.dataPacks()!=null&&Files.exists(c.dataPacks())) ScratchWorld.copyTree(c.dataPacks(),tempStorage.getLevelPath(net.minecraft.world.level.storage.LevelResource.DATAPACK_DIR));
            tempPacks=ServerPacksSource.createPackRepository(tempStorage);
            tempResources=new WorldLoader.PackConfig(tempPacks,c.creation().dataConfiguration(),false,false).createResourceManager().getSecond();
        } catch(IOException|RuntimeException e) {
            try { if(tempResources!=null) tempResources.close(); } catch(Exception cleanup) { e.addSuppressed(cleanup); }
            try { if(tempStorage!=null) tempStorage.close(); } catch(Exception cleanup) { e.addSuppressed(cleanup); }
            try { ScratchWorld.deleteTree(root); } catch(Exception cleanup) { e.addSuppressed(cleanup); }
            throw e;
        }
        storage=tempStorage; packs=tempPacks; resources=tempResources;
    }
    /** Takes a share of the resources for the data this search loaded; call {@link #release} when the scratch world is closed. */
    static synchronized ScratchResources acquire(SearchWorldContext c) throws IOException {
        var key=c.creation().dataPackResources();
        var found=OPEN.get(key);
        if(found==null) { found=new ScratchResources(key,c); OPEN.put(key,found); }
        found.users++; return found;
    }
    void release() {
        synchronized(ScratchResources.class) {
            if(--users>0) return;
            idleSince=System.nanoTime();
        }
        TIMER.schedule(this::closeIfIdle,IDLE_NANOS+TimeUnit.SECONDS.toNanos(1),TimeUnit.NANOSECONDS);
    }
    private void closeIfIdle() {
        synchronized(ScratchResources.class) {
            if(users>0||System.nanoTime()-idleSince<IDLE_NANOS||OPEN.get(key)!=this) return;
            OPEN.remove(key);
        }
        try { resources.close(); } catch(Exception ignored) { /* nothing is using it any more */ }
        try { storage.close(); } catch(Exception ignored) { /* temporary folder only */ }
        try { ScratchWorld.deleteTree(root); } catch(Exception ignored) { /* left in the system temp folder at worst */ }
    }
}
