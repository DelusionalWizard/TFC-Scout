package com.cooper.terrafirmascout.tfc;
import java.nio.file.Path;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.dries007.tfc.world.TFCChunkGenerator;
import net.dries007.tfc.world.biome.BiomeSourceExtension;
import net.dries007.tfc.world.settings.Settings;
public record SearchWorldContext(WorldCreationContext creation,Path dataPacks,String preset,JsonElement generatorJson,Settings settings,BiomeSourceExtension biomes) {
    public static SearchWorldContext capture(WorldCreationContext ctx,Path dataPacks,String preset) {
        var generator=ctx.selectedDimensions().bake(ctx.datapackDimensions()).dimensions().get(LevelStem.OVERWORLD).generator();
        if(!(generator instanceof TFCChunkGenerator tfc)||!(generator.getBiomeSource() instanceof BiomeSourceExtension source))
            throw new IllegalArgumentException("Select the TerraFirmaCraft world preset first");
        var ops=RegistryOps.create(JsonOps.INSTANCE,ctx.worldgenLoadContext());
        var json=TfcCompat.orThrow(ChunkGenerator.CODEC.encodeStart(ops,generator));
        return new SearchWorldContext(ctx,dataPacks,preset,json,tfc.settings(),source);
    }
    /** The profile with any vein resource this world cannot generate switched off, so a search never waits for something that cannot exist. */
    public com.cooper.terrafirmascout.profile.ScoutProfile adapt(com.cooper.terrafirmascout.profile.ScoutProfile profile) {
        // TerraFirmaGreg's progression needs iron and coal soon after bronze, so its presets also look for them. Only when that mod is present.
        if(TfgBridge.present()&&!profile.specification().enabled()&&!profile.quality().challenging()) profile=profile.withExtra(java.util.Set.of(com.cooper.terrafirmascout.score.Criterion.IRON,com.cooper.terrafirmascout.score.Criterion.COAL));
        var missing=new java.util.HashSet<>(ResourceAvailability.unavailable(creation.worldgenLoadContext(),biomes.self(),creation.dataPackResources().getRecipeManager())); missing.retainAll(profile.requiredCriteria());
        return missing.isEmpty()?profile:profile.withSkipped(missing);
    }
    public ChunkGenerator copyGenerator() {
        return TfcCompat.orThrow(ChunkGenerator.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE,creation.worldgenLoadContext()),generatorJson.deepCopy()));
    }
}
