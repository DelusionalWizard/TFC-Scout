package com.cooper.terrafirmascout.tfc;
import java.util.*;
import net.minecraft.core.BlockPos;
import com.cooper.terrafirmascout.search.SearchSession;
/** Search a 64-block grid but validate every edge against the native quart biome map. */
public final class ConnectivityScanner {
    public static boolean connected(TFCWorldgenAdapter a,BlockPos start,BlockPos target,int radius,SearchSession session) {
        int tx=Math.floorDiv(target.getX()-start.getX(),64),tz=Math.floorDiv(target.getZ()-start.getZ(),64);
        int bound=(radius+63)/64+1; var queue=new ArrayDeque<Long>(); var seen=new HashSet<Long>();
        queue.add(pack(0,0)); seen.add(pack(0,0));
        while(!queue.isEmpty()) { session.checkpoint(); long key=queue.remove(); int x=(int)(key>>32),z=(int)key;
            if(x==tx&&z==tz) return edge(a,start.getX()+x*64,start.getZ()+z*64,target.getX(),target.getZ());
            for(int[] dir:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                int nx=x+dir[0],nz=z+dir[1]; long n=pack(nx,nz); if(Math.abs(nx)>bound||Math.abs(nz)>bound||seen.contains(n)) continue;
                if(edge(a,start.getX()+x*64,start.getZ()+z*64,start.getX()+nx*64,start.getZ()+nz*64)) { seen.add(n); queue.add(n); }
            }
        }
        return false;
    }
    private static boolean edge(TFCWorldgenAdapter a,int x,int z,int nx,int nz) {
        int count=Math.max(Math.abs(nx-x),Math.abs(nz-z));
        for(int i=0;i<=count;i++) {
            int px=count==0?x:x+(int)Math.round((nx-x)*(double)i/count),pz=count==0?z:z+(int)Math.round((nz-z)*(double)i/count);
            if(!a.land(px,pz)) return false;
        }
        return true;
    }
    private static long pack(int x,int z) { return ((long)x<<32)|(z&0xffffffffL); }
}
