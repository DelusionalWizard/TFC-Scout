package com.cooper.terrafirmascout.tfc;
import java.net.Proxy;
import com.mojang.authlib.GameProfile;
import net.minecraft.SystemReport;
import net.minecraft.server.*;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.storage.LevelStorageSource;
/** An unbound, unticked server used solely by a seed-local ServerLevel. No sockets are opened. */
final class VerificationServer extends MinecraftServer {
    VerificationServer(LevelStorageSource.LevelStorageAccess storage,PackRepository packs,WorldStem stem,ChunkProgressListener progress) {
        super(Thread.currentThread(),storage,packs,stem,Proxy.NO_PROXY,DataFixers.getDataFixer(),new Services(null,null,null,null),r->progress);
        setPlayerList(new PlayerList(this,registries(),playerDataStorage,0) {});
    }
    @Override protected boolean initServer() { return false; }
    @Override public int getOperatorUserPermissionLevel() { return 0; }
    @Override public int getFunctionCompilationLevel() { return 0; }
    @Override public boolean shouldRconBroadcast() { return false; }
    @Override public SystemReport fillServerSystemReport(SystemReport r) { return r; }
    @Override public boolean isDedicatedServer() { return false; }
    @Override public int getRateLimitPacketsPerSecond() { return 0; }
    @Override public boolean isEpollEnabled() { return false; }
    @Override public boolean isCommandBlockEnabled() { return false; }
    @Override public boolean isPublished() { return false; }
    @Override public boolean shouldInformAdmins() { return false; }
    @Override public boolean isSingleplayerOwner(GameProfile p) { return false; }
}
