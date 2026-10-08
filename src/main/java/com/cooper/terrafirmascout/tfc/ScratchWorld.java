package com.cooper.terrafirmascout.tfc;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import com.mojang.serialization.Lifecycle;
import com.cooper.terrafirmascout.mixin.ServerAccess;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.*;
/** Uses Minecraft's real chunk dependency scheduler. FEATURES skips lighting and mob spawning. */
public final class ScratchWorld implements AutoCloseable {
    private static final boolean TIMING=Boolean.getBoolean("terrafirmascout.timing");
    private static final org.slf4j.Logger LOGGER=com.mojang.logging.LogUtils.getLogger();
    public final ServerLevel level; public final VerificationServer server;
    private final Path root; private final LevelStorageSource.LevelStorageAccess storage;
    private final java.util.concurrent.ExecutorService executor=ScratchExecutors.create();
    private final PackRepository packs; private final CloseableResourceManager resources;
    private TfgBridge.Hold seedHold; private ScratchResources lease;
    public ScratchWorld(SearchWorldContext c,long seed) throws Exception {
        long t0=System.nanoTime();
        root=Files.createTempDirectory("terrafirmascout-");
        LevelStorageSource.LevelStorageAccess tempStorage=null; PackRepository tempPacks=null;
        CloseableResourceManager tempResources=null; ServerLevel tempLevel=null;
        try {
            tempStorage=LevelStorageSource.createDefault(root).createAccess("probe");
            // The pack repository and resource manager are shared by the scratch worlds of one search (opening them reads every installed mod).
            lease=ScratchResources.acquire(c); tempPacks=lease.packs; tempResources=lease.resources;
            var dimensions=c.creation().selectedDimensions().replaceOverworldGenerator(c.creation().worldgenLoadContext(),c.copyGenerator()).bake(c.creation().datapackDimensions());
            var registries=c.creation().worldgenRegistries().replaceFrom(RegistryLayer.DIMENSIONS,dimensions.dimensionsRegistryAccess());
            var settings=new LevelSettings("TerraFirmaScout probe",GameType.SURVIVAL,false,Difficulty.NORMAL,false,new GameRules(),c.creation().dataConfiguration());
            var options=new WorldOptions(seed,c.creation().options().generateStructures(),false);
            var data=new PrimaryLevelData(settings,options,dimensions.specialWorldProperty(),Lifecycle.stable());
            var stem=new WorldStem(tempResources,c.creation().dataPackResources(),registries,data);
            var progress=new ChunkProgressListener() {
                public void updateSpawnPos(net.minecraft.world.level.ChunkPos p) {} public void onStatusChange(net.minecraft.world.level.ChunkPos p,ChunkStatus status) {}
                public void start() {} public void stop() {}
            };
            long t1=System.nanoTime();
            // With TerraFirmaGreg the world's generator reads one global seed for as long as it generates, so no other seed may be set until generation has stopped.
            // Everything above (temporary folder, packs, resources) does not need it, so the seed is taken as late as possible and given back as early as possible.
            seedHold=TfgBridge.hold(seed);
            long t2=System.nanoTime();
            server=new VerificationServer(tempStorage,tempPacks,stem,progress);
            tempLevel=new ServerLevel(server,executor,tempStorage,data,Level.OVERWORLD,
                dimensions.dimensions().getOrThrow(LevelStem.OVERWORLD),progress,false,BiomeManager.obfuscateSeed(seed),List.of(),false,null);
            ((ServerAccess)(Object)server).scout$levels().put(Level.OVERWORLD,tempLevel);
            tempLevel.noSave=true;
            level=tempLevel; storage=tempStorage; packs=tempPacks; resources=tempResources;
            if(TIMING) LOGGER.info("Scout timing: scratch world prepared in {} ms, waited {} ms for the seed, built the level in {} ms",(t1-t0)/1_000_000,(t2-t1)/1_000_000,(System.nanoTime()-t2)/1_000_000);
        } catch(Exception e) {
            try { if(tempLevel!=null) tempLevel.close(); } catch(Exception cleanup) { e.addSuppressed(cleanup); }
            try { ScratchExecutors.close(executor); } catch(Exception cleanup) { e.addSuppressed(cleanup); }
            if(tempLevel!=null) NativeThreadCaches.clearGenerator(tempLevel.getChunkSource().getGenerator());
            if(lease!=null) lease.release();
            try { if(tempStorage!=null) tempStorage.close(); } catch(Exception cleanup) { e.addSuppressed(cleanup); }
            try { deleteTree(root); } catch(Exception cleanup) { e.addSuppressed(cleanup); }
            if(seedHold!=null) seedHold.close();
            throw e;
        }
    }
    public net.minecraft.world.level.chunk.ChunkAccess chunk(int x,int z) { return level.getChunk(x,z,ChunkStatus.FEATURES,true); }
    @Override public void close() throws IOException {
        long start=System.nanoTime();
        try { level.close(); } finally {
            try { ScratchExecutors.close(executor); } finally {
                try {
                    NativeThreadCaches.clearGenerator(level.getChunkSource().getGenerator());
                    ((ServerAccess)(Object)server).scout$levels().clear();
                } finally {
                    // Generation has stopped, so the seed can go to the next search thread while the folders are cleaned up.
                    try { if(seedHold!=null) seedHold.close(); } finally {
                        long released=System.nanoTime();
                        try { lease.release(); } finally { try { storage.close(); } finally { try { deleteTree(root); } finally {
                            if(TIMING) LOGGER.info("Scout timing: scratch world closed, seed released after {} ms, cleanup {} ms",(released-start)/1_000_000,(System.nanoTime()-released)/1_000_000);
                        } } }
                    }
                }
            }
        }
    }
    static void copyTree(Path from,Path to) throws IOException {
        try(var stream=Files.walk(from)) { for(var p:stream.toList()) {
            var dest=to.resolve(from.relativize(p));
            if(Files.isDirectory(p)) Files.createDirectories(dest); else { Files.createDirectories(dest.getParent()); Files.copy(p,dest,StandardCopyOption.REPLACE_EXISTING); }
        } }
    }
    static void deleteTree(Path root) throws IOException {
        // Only the exact Files.createTempDirectory result is ever removed.
        if(!root.toAbsolutePath().getFileName().toString().startsWith("terrafirmascout-")) throw new IOException("Invalid scratch path");
        try(var stream=Files.walk(root)) { for(var p:stream.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p); }
    }
}
