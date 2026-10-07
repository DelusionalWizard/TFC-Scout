package com.cooper.terrafirmascout.tfc;
import java.util.*;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.world.level.dimension.LevelStem;
/**
 * The biomes a player can ask for. The game's biome registry also holds every vanilla biome, but a TerraFirmaCraft world only ever
 * produces the biomes its own biome source lists, so asking for any other biome could never match. This asks the world itself.
 */
public final class BiomeChoices {
    private BiomeChoices() {}
    /** Biome ids (for example {@code tfc:plains}) the selected world's overworld biome source can produce, sorted. */
    public static List<String> forWorld(WorldCreationContext context) {
        var generator=context.selectedDimensions().bake(context.datapackDimensions()).dimensions().getOrThrow(LevelStem.OVERWORLD).generator();
        var ids=new TreeSet<String>();
        for(var holder:generator.getBiomeSource().possibleBiomes()) holder.unwrapKey().ifPresent(key->ids.add(key.location().toString()));
        return List.copyOf(ids);
    }
}
