package com.cooper.terrafirmascout.tfc;
import net.dries007.tfc.world.chunkdata.ChunkData;
import net.dries007.tfc.world.chunkdata.ChunkDataProvider;
import net.dries007.tfc.world.chunkdata.ForestType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
/** The few places where TFC 3.2 (Minecraft 1.20.1) differs from the TFC 4.x API that the 1.21.1 version was written against. */
public final class TfcCompat {
    private TfcCompat() {}
    /** Chunk data for a chunk of a generating level, whether it is a partial ProtoChunk or a finished chunk. */
    public static ChunkData data(ServerLevel level,ChunkAccess chunk) { return ChunkDataProvider.get(level.getChunkSource().getGenerator()).get(chunk); }
    /**
     * TFC 3.2 has five forest types (none, sparse, edge, normal, old growth) and no density value, so tree cover 0-4 is the type's position in that order.
     */
    public static int density(ForestType type) { return type.ordinal(); }
    /** The throwing form of a DataResult, which 1.20.1's DFU spells differently. */
    public static <T> T orThrow(com.mojang.serialization.DataResult<T> result) { return result.getOrThrow(false,message->{ throw new IllegalStateException(message); }); }
}
