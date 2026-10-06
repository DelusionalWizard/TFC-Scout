package com.cooper.terrafirmascout.tfc;
import java.util.*;
import com.cooper.terrafirmascout.profile.WorldSpecification;
import com.cooper.terrafirmascout.search.*;
import com.cooper.terrafirmascout.score.VerificationState;
import net.dries007.tfc.world.chunkdata.ChunkData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
public final class SpecificationVerifier {
    public static Evidence verify(WorldSpecification spec,TFCWorldgenAdapter a,ScratchWorld w,BlockPos spawn,SearchSession session) {
        var data=ChunkData.get(w.level.getChunk(spawn));
        String rock=BuiltInRegistries.BLOCK.getKey(data.getRockData().getSurfaceRock(spawn.getX(),spawn.getZ()).raw()).getPath().replace("rock/raw/","");
        String biome=a.biome(spawn.getX(),spawn.getZ()).key().location().toString(),forest=data.getForestType().getSerializedName();
        int density=data.getForestType().getDensity();
        if(!spec.spawnMatches(biome,rock,forest,density,spawn.getY()))return Evidence.failed("The spawn biome, rock, forest or height does not match your wishlist");
        var found=new TreeSet<String>();found.add(biome);int r=spec.biomeRadius();
        if(!spec.biomesMatch(found)) {
            // Positive native quart-biome queries are exact. An unsuccessful bounded grid is not an absence proof.
            outer:for(int dx=-r;dx<=r;dx+=16){session.checkpoint();for(int dz=-r;dz<=r;dz+=16){
                if((long)dx*dx+(long)dz*dz>(long)r*r)continue;
                found.add(a.biome(spawn.getX()+dx,spawn.getZ()+dz).key().location().toString());
                if(spec.biomesMatch(found))break outer;
            }}
        }
        if(!spec.biomesMatch(found))return Evidence.absent("The scan did not find the nearby biomes you asked for");
        found.retainAll(spec.nearbyBiomes());
        return new Evidence(VerificationState.VERIFIED,0,spawn.getX(),spawn.getY(),spawn.getZ(),1,
            "Spawn: "+biome+", "+rock+", "+forest+" (density "+density+"); nearby biomes found: "+found);
    }
}
