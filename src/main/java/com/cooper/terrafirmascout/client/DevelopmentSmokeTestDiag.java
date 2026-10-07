package com.cooper.terrafirmascout.client;
import java.nio.file.*;
import java.util.*;
import com.cooper.terrafirmascout.profile.*;
import com.cooper.terrafirmascout.score.*;
import com.cooper.terrafirmascout.search.*;
import com.cooper.terrafirmascout.tfc.*;
/**
 * Opt-in development tool (env SCOUT_DIAG): reports which TFC vein features a world's biomes actually list, then checks a few seeds against every resource with a
 * generous budget and writes which requirements were confirmed, to see what a given world (for example a modpack) can ever produce. Excluded from release jars.
 */
final class DevelopmentSmokeTestDiag {
    private DevelopmentSmokeTestDiag() {}
    static void run(SearchWorldContext c,Path out) throws Exception {
        var report=new StringBuilder(); var tally=new EnumMap<Criterion,Integer>(Criterion.class); int seeds=Integer.getInteger("terrafirmascout.diagSeeds",6);
        var registries=c.creation().worldgenLoadContext();
        var catalog=VeinCatalog.of(registries,c.biomes().self());
        report.append("Unavailable resources reported by Scout: ").append(ResourceAvailability.unavailable(registries,c.biomes().self(),c.creation().dataPackResources().getRecipeManager())).append('\n');
        for(var vein:catalog.veins()) report.append("  active vein ").append(vein.id()).append(" -> ").append(vein.resources()).append(vein.indicators()?" (surface indicators)":"").append('\n');
        report.append("  copper indicator blocks: ").append(catalog.copperIndicatorBlocks()).append('\n');
        for(var criterion:List.of(Criterion.COPPER_VEIN,Criterion.TIN,Criterion.GRAPHITE,Criterion.IRON,Criterion.COAL,Criterion.KAOLIN)) report.append("  ore blocks for ").append(criterion).append(": ").append(catalog.oreBlocks(criterion).size()).append(" e.g. ").append(catalog.oreBlocks(criterion).stream().limit(3).toList()).append('\n');
        Files.writeString(out.resolve("diag.txt"),report.toString());
        var draft=SpecificationDraft.fromPreset(ScoutProfile.preset(SeedQuality.AVERAGE));
        draft.required.remove(Criterion.OPEN_GROUND); draft.required.remove(Criterion.TERRAIN); draft.required.remove(Criterion.MAINLAND); draft.required.remove(Criterion.LAND_RATIO); draft.required.add(Criterion.IRON); draft.required.add(Criterion.COAL); draft.numbers.put("min_score",0d);
        var profile=draft.build();
        var random=new SplittableRandom(20261007L);
        for(int i=0;i<seeds;i++) {
            long seed=random.nextLong(); var session=new SearchSession(); long began=System.nanoTime();
            var result=SeedChecker.check(c,profile,seed,128,session);
            report.append("seed ").append(seed).append(" (").append((System.nanoTime()-began)/1_000_000_000L).append(" s): ");
            var confirmed=new ArrayList<String>(); var missing=new ArrayList<String>();
            for(var criterion:Criterion.values()) {
                var e=result.evidence().get(criterion); if(e==null||!result.profile().requires(criterion)) continue;
                if(e.state()==VerificationState.VERIFIED) { confirmed.add(criterion.name()); tally.merge(criterion,1,Integer::sum); } else missing.add(criterion.name()+"("+e.state()+")");
            }
            report.append("confirmed ").append(confirmed).append("; not confirmed ").append(missing).append('\n');
            Files.writeString(out.resolve("diag.txt"),report.toString());
        }
        report.append("Confirmed counts over ").append(seeds).append(" seeds: ").append(tally).append('\n');
        Files.writeString(out.resolve("diag.txt"),report.toString());
    }
}
