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
        var generator=ctx.selectedDimensions().bake(ctx.datapackDimensions()).dimensions().getOrThrow(LevelStem.OVERWORLD).generator();
        if(!(generator instanceof TFCChunkGenerator tfc)||!(generator.getBiomeSource() instanceof BiomeSourceExtension source))
            throw new IllegalArgumentException("Select the TerraFirmaCraft world preset first");
        var ops=RegistryOps.create(JsonOps.INSTANCE,ctx.worldgenLoadContext());
        var json=ChunkGenerator.CODEC.encodeStart(ops,generator).getOrThrow();
        return new SearchWorldContext(ctx,dataPacks,preset,json,tfc.settings(),source);
    }
    public ChunkGenerator copyGenerator() {
        return ChunkGenerator.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE,creation.worldgenLoadContext()),generatorJson.deepCopy()).getOrThrow();
    }
}
