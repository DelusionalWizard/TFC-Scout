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
    /**
     * Highest ranking a candidate can still reach when the criteria in {@code missed} are unconfirmed for good.
     * Never optimistic-low: used only to skip checks that cannot change which result is shown or selected.
     */
    public static double maxPossibleRank(Map<Criterion,Evidence> evidence,com.cooper.terrafirmascout.profile.ScoutProfile profile,java.util.Set<Criterion> missed) {
        double earned=0,possible=0; int required=0;
        for(var c:Criterion.values()) if(profile.requires(c)) {
            required++; possible+=c.weight; var e=evidence.get(c);
            if(missed.contains(c)) continue;
            earned+=e!=null&&e.state()==VerificationState.VERIFIED?c.weight*e.quality():c.weight;
        }
        if(possible==0) return Double.POSITIVE_INFINITY;
        // Same flooring as score(), plus the most the unconfirmed-row tie-break in SearchSession.rank can add.
        return (int)Math.floor(100*earned/possible+1e-8)+required*0.001+(missed.isEmpty()?1000:0);
    }
    /** Required checks that are not confirmed (missing rows count as unconfirmed), in criterion order. */
    public static java.util.List<Criterion> unconfirmedRequired(Map<Criterion,Evidence> evidence,com.cooper.terrafirmascout.profile.ScoutProfile profile) {
        var open=new java.util.ArrayList<Criterion>();
        for(var c:Criterion.values()) if(profile.requires(c)) { var e=evidence.get(c); if(e==null||e.state()!=VerificationState.VERIFIED) open.add(c); }
        return open;
    }
    /**
     * Whether the remaining checks on a seed can be skipped. Only after a second missed check: a seed with one miss may still be a
     * close call worth showing, and one with none may still be a match. The bound makes sure the seed could not outrank the best result.
     */
    public static boolean remainingChecksCannotMatter(Map<Criterion,Evidence> evidence,com.cooper.terrafirmascout.profile.ScoutProfile profile,java.util.Set<Criterion> missed,double bestRank) {
        return missed.size()>=2&&maxPossibleRank(evidence,profile,missed)<bestRank;
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
