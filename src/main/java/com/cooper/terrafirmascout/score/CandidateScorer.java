package com.cooper.terrafirmascout.score;
import java.util.Map;
import com.cooper.terrafirmascout.search.Evidence;
public final class CandidateScorer {
    public static int score(Map<Criterion,Evidence> evidence,com.cooper.terrafirmascout.profile.ScoutProfile profile) {
        double earned=0,possible=0;
        for(var c:Criterion.values()) if(profile.requires(c)) {
            possible+=c.weight; var e=evidence.get(c); if(e!=null&&e.state()==VerificationState.VERIFIED) earned+=c.weight*e.quality();
        }
        return possible==0?(allHardVerified(evidence,profile)?100:0):(int)Math.floor(100*earned/possible+1e-8);
    }
    public static double targetDistance(Map<Criterion,Evidence> evidence,com.cooper.terrafirmascout.profile.ScoutProfile profile) {
        return profile.requiredCriteria().stream().map(evidence::get)
            .mapToDouble(e->e!=null&&Double.isFinite(e.distance())?e.distance():10000).sum();
    }
    public static boolean allHardVerified(Map<Criterion,Evidence> evidence,com.cooper.terrafirmascout.profile.ScoutProfile profile) {
        for(var c:profile.requiredCriteria()) if(evidence.get(c)==null||evidence.get(c).state()!=VerificationState.VERIFIED) return false;
        return true;
    }
    public static int score(Map<Criterion,Evidence> evidence) {
        double total=0;
        for(var c:Criterion.values()) { var e=evidence.get(c); if(e!=null&&e.state()==VerificationState.VERIFIED) total+=c.weight*e.quality(); }
        return (int)Math.floor(total+1e-8);
    }
    public static boolean allHardVerified(Map<Criterion,Evidence> evidence) {
        for(var c:Criterion.values()) if(c.hard()&&(evidence.get(c)==null||evidence.get(c).state()!=VerificationState.VERIFIED)) return false;
        return true;
    }
    public static double proximity(double distance,double ideal,double maximum) {
        if(!Double.isFinite(distance)||distance>maximum) return 0;
        if(distance<=ideal) return 1;
        return maximum==ideal?1:1-0.15*(distance-ideal)/(maximum-ideal);
    }
    public static double climate(double value,double min,double idealMin,double idealMax,double max) {
        if(!Double.isFinite(value)||value<min||value>max) return 0;
        if(value>=idealMin&&value<=idealMax) return 1;
        return value<idealMin?1-0.3*(idealMin-value)/(idealMin-min):1-0.3*(value-idealMax)/(max-idealMax);
    }
}
