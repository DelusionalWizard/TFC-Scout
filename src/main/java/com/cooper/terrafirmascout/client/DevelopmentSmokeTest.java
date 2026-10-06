package com.cooper.terrafirmascout.client;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import com.cooper.terrafirmascout.mixin.CreateWorldAccess;
import com.cooper.terrafirmascout.tfc.*;
import com.cooper.terrafirmascout.search.*;
import com.cooper.terrafirmascout.scanner.*;
import com.cooper.terrafirmascout.profile.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.*;
/** Opt-in development harness. Gradle excludes this class from release jars. */
@EventBusSubscriber(modid="terrafirmascout",value=Dist.CLIENT)
public final class DevelopmentSmokeTest {
    private static int state,ticks;
    private static CreateWorldScreen creationScreen; private static ScoutWorldCreationScreen mainScreen;
    private static volatile ScoutSearchEngine benchmarkEngine;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("terrafirmascout.smoke")) return;
        var mc=Minecraft.getInstance();
        try {
            if(state==0&&mc.screen!=null&&mc.getOverlay()==null) { state=1; CreateWorldScreen.openFresh(mc,mc.screen); }
            else if(state==1&&mc.screen instanceof CreateWorldScreen parent) {
                state=3; // Prevent re-entry while Minecraft pumps tasks during world-preset setup.
                var registry=parent.getUiState().getSettings().worldgenLoadContext().registryOrThrow(Registries.WORLD_PRESET);
                var preset=registry.getHolderOrThrow(ResourceKey.create(Registries.WORLD_PRESET,ResourceLocation.fromNamespaceAndPath("tfc","overworld")));
                parent.getUiState().setWorldType(new WorldCreationUiState.WorldTypeEntry(preset));
                var context=SearchWorldContext.capture(parent.getUiState().getSettings(),((CreateWorldAccess)parent).scout$dataPackDir(),"tfc:overworld");
                creationScreen=parent;mainScreen=new ScoutWorldCreationScreen(parent); mc.setScreen(mainScreen); state=2;
                CompletableFuture.runAsync(()->run(context)).whenComplete((v,e)->mc.execute(()-> {
                    try { if(e!=null) { e.printStackTrace(); Files.writeString(output("smoke-failure.txt"),e.toString()); } }
                    catch(Exception ex) { ex.printStackTrace(); }
                    mc.stop();
                }));
            } else if(state==2&&!(mc.screen instanceof ScoutWorldCreationScreen)&&!(mc.screen instanceof SpecificationScreen)) { mc.setScreen(new ScoutWorldCreationScreen(creationScreen)); }
            else if(state==2) {
                if(benchmarkEngine!=null&&mc.screen instanceof ScoutWorldCreationScreen) {
                    var field=ScoutWorldCreationScreen.class.getDeclaredField("engine"); field.setAccessible(true); field.set(mc.screen,benchmarkEngine);
                }
                ticks++;
                boolean suite=Boolean.getBoolean("terrafirmascout.benchmarkAll");
                if(ticks==12||!suite&&ticks==300)try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(output(ticks==12?"world-creation-screen.png":"world-search-progress.png"));}
                if(suite&&ticks==13){
                    var registry=creationScreen.getUiState().getSettings().worldgenLoadContext().registryOrThrow(Registries.BIOME);
                    var biomes=registry.keySet().stream().map(Object::toString).sorted().toList();
                    var rocks=BuiltInRegistries.BLOCK.keySet().stream().filter(id->id.getNamespace().equals("tfc")&&id.getPath().startsWith("rock/raw/")).map(id->id.getPath().replace("rock/raw/","")).sorted().toList();
                    mc.setScreen(new SpecificationScreen(mainScreen,SpecificationDraft.fromPreset(ScoutProfile.beginner()),biomes,rocks,d->{}));
                }
                if(suite&&ticks>=33&&ticks<=133&&ticks%20==13&&mc.screen instanceof SpecificationScreen screen){
                    String[] tabs={"Resources","Limits","Nearby","Spawn","Rocks","Forest"};int i=(ticks-33)/20;
                    try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(output("specification-"+tabs[i].toLowerCase()+".png"));}
                    if(i+1<tabs.length)screen.showTab(tabs[i+1]);
                }
                if(suite&&ticks==153)mc.setScreen(mainScreen);

            }
        } catch(Exception e) { e.printStackTrace(); mc.stop(); }
    }
    private static Path output(String file) { return Path.of(System.getProperty("terrafirmascout.reportDir")).resolve(file); }
    private static void run(SearchWorldContext c) {
        try {
            if(Boolean.getBoolean("terrafirmascout.memoryTest")) DevelopmentMemoryTest.run(c);
            long seed=123456789L; var a=new TFCWorldgenAdapter(seed,c.settings(),c.biomes());
            var b=new TFCWorldgenAdapter(seed,c.settings(),c.biomes());
            var spawn=a.spawnBiome(); if(!spawn.equals(b.spawnBiome())) throw new AssertionError("spawn biome differs");
            for(int x=-1024;x<=1024;x+=128) for(int z=-1024;z<=1024;z+=128) {
                if(!a.biome(x,z).key().equals(b.biome(x,z).key())) throw new AssertionError("biome differs");
                if(Float.floatToIntBits(a.data(x,z).getAverageRainfall(x,z))!=Float.floatToIntBits(b.data(x,z).getAverageRainfall(x,z))) throw new AssertionError("rain differs");
                if(!a.rock(x,64,z,64).equals(b.rock(x,64,z,64))) throw new AssertionError("rock differs");
            }
            a.releaseThreadCaches();
            if(!spawn.equals(a.spawnBiome())) throw new AssertionError("spawn changed after cache release");
            for(int x=-1024;x<=1024;x+=128) for(int z=-1024;z<=1024;z+=128) {
                if(!a.biome(x,z).key().equals(b.biome(x,z).key())) throw new AssertionError("biome changed after cache release");
                if(Float.floatToIntBits(a.data(x,z).getAverageRainfall(x,z))!=Float.floatToIntBits(b.data(x,z).getAverageRainfall(x,z))) throw new AssertionError("rain changed after cache release");
                if(!a.rock(x,64,z,64).equals(b.rock(x,64,z,64))) throw new AssertionError("rock changed after cache release");
            }
            String first,second; long start=System.nanoTime(); var cp=new ChunkPos(spawn);
            try(var w=new ScratchWorld(c,seed)) {
                var chunk=w.chunk(cp.x,cp.z); first=hash(chunk);
                var data=net.dries007.tfc.world.chunkdata.ChunkData.get(chunk);
                if(Math.abs(data.getAverageSeaLevelTemp(spawn)-a.data(spawn.getX(),spawn.getZ()).getAverageSeaLevelTemp(spawn))>0.0001)
                    throw new AssertionError("adapter differs from real chunk climate");
            }
            try(var w=new ScratchWorld(c,seed)) { second=hash(w.chunk(cp.x,cp.z)); }
            if(!first.equals(second)) throw new AssertionError("FEATURES blocks differ on repeat generation");
            var fingerprint=WorldgenFingerprint.compute(c);
            for(var grade:SeedQuality.values()) { var configured=com.cooper.terrafirmascout.config.ScoutConfig.profile(grade.id); if(configured.quality()!=grade)throw new AssertionError("Wrong configured grade"); }
            boolean suite=Boolean.getBoolean("terrafirmascout.benchmarkAll");
            String benchmark="";
            if(suite){
                Thread.sleep(8000); // Allow all specification pages to render and be captured by the UI thread.
                var draft=SpecificationDraft.fromPreset(ScoutProfile.beginner());draft.required.clear();draft.required.add(com.cooper.terrafirmascout.score.Criterion.CLIMATE);
                draft.nearbyBiomes.add(a.biome(spawn.getX(),spawn.getZ()).key().location().toString());draft.allBiomes=true;
                draft.numbers.put("temperature_min",-20d);draft.numbers.put("temperature_ideal_min",-20d);draft.numbers.put("temperature_ideal_max",40d);draft.numbers.put("temperature_max",40d);
                draft.numbers.put("rainfall_min",0d);draft.numbers.put("rainfall_ideal_min",0d);draft.numbers.put("rainfall_ideal_max",500d);draft.numbers.put("rainfall_max",500d);
                draft.numbers.put("min_score",100d);
                var custom=draft.build();var candidate=DetailedScanner.scan(a,spawn,custom,new SearchSession(),c.creation().worldgenLoadContext(),c.copyGenerator());
                var match=new TFCFeatureProbe(c,custom,new SearchSession(),24).verify(candidate,fingerprint);
                for(var criterion:com.cooper.terrafirmascout.score.Criterion.values())
                    if(!custom.requires(criterion)&&!candidate.targets().get(criterion).isEmpty())throw new AssertionError("Optional targets scheduled: "+criterion);
                if(match.score()!=100)throw new AssertionError("Optional evidence reduced the wishlist score");
                if(!match.selectable(fingerprint))throw new AssertionError("Known seed did not meet broad climate + selected native biome specification");
                var mc=Minecraft.getInstance();
                mc.submit(()-> {
                    try {
                        var field=ScoutWorldCreationScreen.class.getDeclaredField("savedResult");field.setAccessible(true);field.set(mainScreen,match);
                        mc.setScreen(mainScreen);
                    }catch(Exception e){throw new RuntimeException(e);}
                }).get();
                Thread.sleep(1500);
                mc.submit(()-> {try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(output("optional-checks-result.png"));}catch(Exception e){throw new RuntimeException(e);}}).get();
                ResultHistory.save(match);ResultHistory.export(match,false);draft.save();
                if(ResultHistory.load().stream().noneMatch(r->r.seed()==match.seed()&&r.profile().equals(match.profile())))throw new AssertionError("History did not round-trip");
                Files.writeString(output("specification-verified-result.json"),ResultHistory.encode(match));
                benchmark+="Specification exact verification, history round-trip and coordinate-hidden export: PASSED\n";
                var exactSpawn=new BlockPos(match.spawnX(),match.spawnY(),match.spawnZ());
                try(var world=new ScratchWorld(c,match.seed())){
                    var nativeData=net.dries007.tfc.world.chunkdata.ChunkData.get(world.level.getChunk(exactSpawn));
                    draft.spawnBiomes.add(a.biome(exactSpawn.getX(),exactSpawn.getZ()).key().location().toString());
                    draft.spawnRocks.add(BuiltInRegistries.BLOCK.getKey(nativeData.getRockData().getSurfaceRock(exactSpawn.getX(),exactSpawn.getZ()).raw()).getPath().replace("rock/raw/",""));
                    draft.forestTypes.add(nativeData.getForestType().getSerializedName());
                    draft.numbers.put("forest_density_min",(double)nativeData.getForestType().getDensity());draft.numbers.put("forest_density_max",(double)nativeData.getForestType().getDensity());
                    draft.numbers.put("spawn_elevation_min",(double)exactSpawn.getY());draft.numbers.put("spawn_elevation_max",(double)exactSpawn.getY());
                    if(SpecificationVerifier.verify(draft.build().specification(),a,world,exactSpawn,new SearchSession()).state()!=com.cooper.terrafirmascout.score.VerificationState.VERIFIED)
                        throw new AssertionError("Exact native spawn choices failed");
                    draft.spawnBiomes.clear();draft.spawnBiomes.add("minecraft:the_void");
                    if(SpecificationVerifier.verify(draft.build().specification(),a,world,exactSpawn,new SearchSession()).state()!=com.cooper.terrafirmascout.score.VerificationState.FAILED)
                        throw new AssertionError("Mismatching spawn biome was not rejected");
                }
                benchmark+="Exact spawn biome, surface rock, forest type/density and elevation filters; wrong-biome rejection: PASSED\n";

                if(Integer.getInteger("terrafirmascout.otherSeconds",60)>0)for(var check:Map.of(3589375073047842521L,ScoutProfile.preset(SeedQuality.GOOD),-7538705592502360260L,ScoutProfile.preset(SeedQuality.HARD)).entrySet()) {
                    var nativeAdapter=new TFCWorldgenAdapter(check.getKey(),c.settings(),c.biomes());var session=new SearchSession();
                    var probeCandidate=DetailedScanner.scan(nativeAdapter,nativeAdapter.spawnBiome(),check.getValue(),session,c.creation().worldgenLoadContext(),c.copyGenerator());
                    var checked=new TFCFeatureProbe(c,check.getValue(),session,48).verify(probeCandidate,fingerprint);
                    // Stronger decorated-ground checks may correctly reject a previously accepted tree-covered patch.
                    Files.writeString(output("regression-"+check.getValue().quality().id+".json"),ResultHistory.encode(checked));
                    benchmark+="Known "+check.getValue().name()+" seed at 48 target chunks: "+checked.status()+", fit="+checked.score()+"\n";
                }
                for(var grade:new SeedQuality[]{SeedQuality.GOD,SeedQuality.GOOD,SeedQuality.AVERAGE,SeedQuality.HARD,SeedQuality.SUPER_HARD})
                    benchmark+=benchmark(c,ScoutProfile.preset(grade),grade==SeedQuality.GOD?Integer.getInteger("terrafirmascout.smokeSeconds",300):Integer.getInteger("terrafirmascout.otherSeconds",60));
            }else benchmark=benchmark(c,ScoutProfile.beginner(),Integer.getInteger("terrafirmascout.smokeSeconds",90));
            var report="Smoke test PASSED\nTFC 4.2.11 / Minecraft 1.21.1 / NeoForge 21.1.234\nSeed: "+seed+
                "\nNative spawn biome: "+spawn+"\n289 repeat region samples: equal, including after native cache release\nFeature-stage SHA-256: "+first+
                "\nReal chunk climate matches adapter\nRepeat feature-stage block hash: equal\nWorldgen fingerprint: "+fingerprint+"\n"+benchmark+"Total smoke elapsed: "+(System.nanoTime()-start)/1e9+" seconds\n";
            Files.writeString(output("smoke-test.txt"),report); System.out.println(report);
        } catch(Throwable e) { throw new CompletionException(e); }
    }
    private static String benchmark(SearchWorldContext c,ScoutProfile profile,int seconds)throws Exception {
        var search=new ScoutSearchEngine(c,profile);benchmarkEngine=search;search.start();long deadline=System.nanoTime()+seconds*1_000_000_000L;long started=System.nanoTime();
        while(!search.session.finished&&!search.session.paused&&System.nanoTime()<deadline)Thread.sleep(200);
        search.close();while(!search.session.finished)Thread.sleep(200);
        if(!search.session.error.isEmpty())throw new AssertionError(profile.name()+": "+search.session.error);
        String report=profile.name()+" ("+seconds+"s budget): tested="+search.session.tested+", pass1="+search.session.pass1+", pass2="+search.session.pass2+", qualifying="+search.session.verified+", elapsed="+(System.nanoTime()-started)/1e9+"s\n";
        var best=search.session.best.get();if(best!=null){report+="Best: "+best.status()+", score="+best.score()+", seed="+best.seed()+"\n";
            for(var criterion:best.profile().requiredCriteria())report+=criterion+": "+best.evidence().get(criterion)+"\n";
            if(best.selectable(search.fingerprint))Files.writeString(output("verified-"+profile.quality().id+".json"),ResultHistory.encode(best));}
        System.out.println(report);Files.writeString(output("benchmark-"+profile.quality().id+".txt"),report);return report;
    }
    private static String hash(ChunkAccess chunk) throws Exception {
        var digest=MessageDigest.getInstance("SHA-256");
        for(int y=chunk.getMinBuildHeight();y<chunk.getMaxBuildHeight();y++) for(int x=0;x<16;x++) for(int z=0;z<16;z++) {
            var pos=new BlockPos(chunk.getPos().getMinBlockX()+x,y,chunk.getPos().getMinBlockZ()+z);
            digest.update(chunk.getBlockState(pos).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)); digest.update((byte)0);
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
