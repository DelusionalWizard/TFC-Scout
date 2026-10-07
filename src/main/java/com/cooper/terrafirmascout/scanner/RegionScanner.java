package com.cooper.terrafirmascout.scanner;
import com.cooper.terrafirmascout.tfc.TFCWorldgenAdapter;
import com.cooper.terrafirmascout.profile.ScoutProfile;
import com.cooper.terrafirmascout.search.SearchSession;
import net.minecraft.core.BlockPos;
/** Prefilter only: region estimates never become final evidence. */
public final class RegionScanner {
    public static boolean accepts(TFCWorldgenAdapter a,BlockPos spawn,ScoutProfile p,SearchSession s) {
        int x=spawn.getX(),z=spawn.getZ(); var point=a.point(x,z);
        if(!point.land()||(p.requires(com.cooper.terrafirmascout.score.Criterion.MAINLAND)&&(point.island()||point.mountain()||a.biome(x,z).isVolcanic()))) return false;
        var d=a.data(x,z); double t=d.getAverageTemp(x,z),r=d.getRainfall(x,z);
        if(p.requires(com.cooper.terrafirmascout.score.Criterion.CLIMATE)&&(t<p.temperatureMin()-1||t>p.temperatureMax()+20||r<p.rainfallMin()-20||r>p.rainfallMax()+20)) return false;
        if(!p.requires(com.cooper.terrafirmascout.score.Criterion.LAND_RATIO))return true;
        int land=0,total=0;
        for(int dx=-p.landRadius();dx<=p.landRadius();dx+=128) { s.checkpoint();
            for(int dz=-p.landRadius();dz<=p.landRadius();dz+=128) if((long)dx*dx+(long)dz*dz<=(long)p.landRadius()*p.landRadius()) {
                total++; if(a.land(x+dx,z+dz)) land++;
            }
        }
        return !p.requires(com.cooper.terrafirmascout.score.Criterion.LAND_RATIO)||(total>0&&(double)land/total>=p.minimumLand()-0.05);
    }
}
