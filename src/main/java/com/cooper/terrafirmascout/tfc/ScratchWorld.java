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
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.*;
/** Uses Minecraft's real chunk dependency scheduler. FEATURES skips lighting and mob spawning. */
public final class ScratchWorld implements AutoCloseable {
    public final ServerLevel level; public final VerificationServer server;
    private final Path root; private final LevelStorageSource.LevelStorageAccess storage;
    private final java.util.concurrent.ExecutorService executor=ScratchExecutors.create();
    private final PackRepository packs; private final CloseableResourceManager resources;
    public ScratchWorld(SearchWorldContext c,long seed) throws Exception {
        root=Files.createTempDirectory("terrafirmascout-");
        LevelStorageSource.LevelStorageAccess tempStorage=null; PackRepository tempPacks=null;
        CloseableResourceManager tempResources=null; ServerLevel tempLevel=null;
        try {
            tempStorage=LevelStorageSource.createDefault(root).createAccess("probe");
            if(c.dataPacks()!=null&&Files.exists(c.dataPacks())) copyTree(c.dataPacks(),tempStorage.getLevelPath(LevelResource.DATAPACK_DIR));
            tempPacks=ServerPacksSource.createPackRepository(tempStorage);
            tempResources=new WorldLoader.PackConfig(tempPacks,c.creation().dataConfiguration(),false,false).createResourceManager().getSecond();
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
            server=new VerificationServer(tempStorage,tempPacks,stem,progress);
            tempLevel=new ServerLevel(server,executor,tempStorage,data,Level.OVERWORLD,
                dimensions.dimensions().getOrThrow(LevelStem.OVERWORLD),progress,false,BiomeManager.obfuscateSeed(seed),List.of(),false,null);
            ((ServerAccess)(Object)server).scout$levels().put(Level.OVERWORLD,tempLevel);
            tempLevel.noSave=true;
            level=tempLevel; storage=tempStorage; packs=tempPacks; resources=tempResources;
        } catch(Exception e) {
            try { if(tempLevel!=null) tempLevel.close(); } catch(Exception cleanup) { e.addSuppressed(cleanup); }
            try { ScratchExecutors.close(executor); } catch(Exception cleanup) { e.addSuppressed(cleanup); }
            if(tempLevel!=null) NativeThreadCaches.clearBiomeSource(tempLevel.getChunkSource().getGenerator().getBiomeSource());
            try { if(tempResources!=null) tempResources.close(); } catch(Exception cleanup) { e.addSuppressed(cleanup); }
            try { if(tempStorage!=null) tempStorage.close(); } catch(Exception cleanup) { e.addSuppressed(cleanup); }
            try { deleteTree(root); } catch(Exception cleanup) { e.addSuppressed(cleanup); }
            throw e;
        }
    }
    public net.minecraft.world.level.chunk.ChunkAccess chunk(int x,int z) { return level.getChunk(x,z,ChunkStatus.FEATURES,true); }
    @Override public void close() throws IOException {
        try { level.close(); } finally {
            try { ScratchExecutors.close(executor); } finally {
                try {
                    NativeThreadCaches.clearBiomeSource(level.getChunkSource().getGenerator().getBiomeSource());
                    ((ServerAccess)(Object)server).scout$levels().clear();
                } finally {
                    try { resources.close(); } finally { try { storage.close(); } finally { deleteTree(root); } }
                }
            }
        }
    }
    private static void copyTree(Path from,Path to) throws IOException {
        try(var stream=Files.walk(from)) { for(var p:stream.toList()) {
            var dest=to.resolve(from.relativize(p));
            if(Files.isDirectory(p)) Files.createDirectories(dest); else { Files.createDirectories(dest.getParent()); Files.copy(p,dest,StandardCopyOption.REPLACE_EXISTING); }
        } }
    }
    private static void deleteTree(Path root) throws IOException {
        // Only the exact Files.createTempDirectory result is ever removed.
        if(!root.toAbsolutePath().getFileName().toString().startsWith("terrafirmascout-")) throw new IOException("Invalid scratch path");
        try(var stream=Files.walk(root)) { for(var p:stream.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p); }
    }
}
