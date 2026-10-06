package com.cooper.terrafirmascout.search;
import java.util.*;
import com.cooper.terrafirmascout.score.Criterion;
import com.cooper.terrafirmascout.tfc.TFCWorldgenAdapter;
import net.minecraft.core.BlockPos;
public record SeedCandidate(TFCWorldgenAdapter adapter,BlockPos center,Map<Criterion,Evidence> evidence,
    Map<Criterion,List<BlockPos>> targets,Map<Criterion,Map<BlockPos,List<VeinHint>>> veinHints) {
    /** Ranking metadata from active native configs; never a placement guarantee. */
    public record VeinHint(boolean projected,int verticalRadius,Set<String> hosts) {}
}
