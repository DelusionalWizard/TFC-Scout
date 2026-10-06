package com.cooper.terrafirmascout.search;
import com.cooper.terrafirmascout.score.VerificationState;
public record Evidence(VerificationState state, double distance, int x, int y, int z, double quality, String detail) {
    public Evidence { if (!Double.isFinite(quality)||quality<0||quality>1) throw new IllegalArgumentException("quality"); }
    public static Evidence inferred(double distance,int x,int z,String detail) {
        return new Evidence(VerificationState.INFERRED,distance,x,0,z,1,detail);
    }
    public static Evidence absent(String detail) { return new Evidence(VerificationState.INFERRED,Double.POSITIVE_INFINITY,0,0,0,0,detail); }
    public static Evidence failed(String detail) { return new Evidence(VerificationState.FAILED,Double.POSITIVE_INFINITY,0,0,0,0,detail); }
}
