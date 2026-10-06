package com.cooper.terrafirmascout.search;
import java.util.*;
import com.cooper.terrafirmascout.profile.ScoutProfile;
import com.cooper.terrafirmascout.score.*;
public record SeedResult(long seed,int spawnX,int spawnY,int spawnZ,String fingerprint,
    ScoutProfile profile,Map<Criterion,Evidence> evidence) {
    public SeedResult { evidence=Map.copyOf(evidence); }
    public int score() { return CandidateScorer.score(evidence,profile); }
    public boolean selectable(String currentFingerprint) {
        return fingerprint.equals(currentFingerprint)&&score()>=profile.minScore()&&CandidateScorer.allHardVerified(evidence,profile);
    }
    public String status() {
        if(!CandidateScorer.allHardVerified(evidence,profile))return "Not confirmed yet";
        if(score()<profile.minScore())return "Checks passed; below your minimum match";
        return profile.specification().enabled()?"Matches your wishlist":profile.quality().label+" seed - confirmed";
    }
}
