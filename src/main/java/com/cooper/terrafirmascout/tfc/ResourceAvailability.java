package com.cooper.terrafirmascout.tfc;
import java.util.*;
import com.cooper.terrafirmascout.score.Criterion;
import net.dries007.tfc.common.recipes.TFCRecipeTypes;
import net.dries007.tfc.common.recipes.inventory.ItemStackInventory;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.level.biome.BiomeSource;
/**
 * Which ore-vein resources the selected world can actually generate. A modpack may remove TFC's default veins and put its own ores in their place
 * (TerraFirmaGreg does), so a seed could never have "TFC copper vein" however long Scout searched. See {@link VeinCatalog} for how that is decided.
 */
public final class ResourceAvailability {
    private ResourceAvailability() {}
    private static final List<Criterion> VEIN_RESOURCES=List.of(Criterion.COPPER_VEIN,Criterion.TIN,Criterion.GRAPHITE,Criterion.IRON,Criterion.COAL,Criterion.KAOLIN);
    /** The vein-based resources this world does not generate. Empty when the world's data cannot be read (no assumptions are made then). */
    public static Set<Criterion> unavailable(RegistryAccess registries,BiomeSource biomeSource,RecipeManager recipes) {
        try {
            var catalog=VeinCatalog.of(registries,biomeSource); var missing=EnumSet.noneOf(Criterion.class);
            for(var c:VEIN_RESOURCES) if(!catalog.available(c)) missing.add(c);
            // Loose surface copper is the small ore pieces that copper veins spawn at the surface, counted by the copper they melt into. A pack that removes TFC's
            // ore melting recipes (TerraFirmaGreg does) leaves nothing that could add up to the copper this check asks for.
            if(catalog.copperIndicatorBlocks().stream().noneMatch(block->meltsToCopper(recipes,block))) missing.add(Criterion.STARTER_COPPER);
            return missing;
        } catch(RuntimeException e) { return Set.of(); }
    }
    /** The same test the loose copper check applies to a found piece: a heating recipe that turns it into copper. */
    static boolean meltsToCopper(RecipeManager recipes,ResourceLocation block) {
        var item=ForgeRegistries.BLOCKS.getValue(block); if(item==null) return false;
        var inventory=new ItemStackInventory(new ItemStack(item.asItem()));
        for(var recipe:recipes.getAllRecipesFor(TFCRecipeTypes.HEATING.get())) {
            if(!recipe.matches(inventory,null)) continue;
            var fluid=recipe.assembleFluid(inventory); var key=ForgeRegistries.FLUIDS.getKey(fluid.getFluid());
            return key!=null&&key.toString().equals("tfc:metal/copper")&&fluid.getAmount()>0;
        }
        return false;
    }
    public static String extraNotice(Set<Criterion> extra) {
        var names=new ArrayList<String>(); for(var c:extra) names.add(c.label.toLowerCase(Locale.ROOT));
        return "TerraFirmaGreg found: also looking for "+String.join(" and ",names)+".";
    }
    public static String notice(Set<Criterion> skipped) {
        if(skipped.isEmpty()) return "";
        var names=new ArrayList<String>(); for(var c:skipped) names.add(c.label.toLowerCase(Locale.ROOT));
        return "Not required, because this world has no TFC "+String.join(", ",names)+" (a pack replaced or removed them).";
    }
}
