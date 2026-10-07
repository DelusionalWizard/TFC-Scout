package com.cooper.terrafirmascout;
import com.cooper.terrafirmascout.config.ScoutConfig;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;
@Mod(TerraFirmaScout.ID)
public final class TerraFirmaScout {
    public static final String ID="terrafirmascout";
    public static final Logger LOGGER=LogUtils.getLogger();
    public TerraFirmaScout() {
        var context=ModLoadingContext.get();
        context.registerConfig(ModConfig.Type.COMMON,ScoutConfig.COMMON,"terrafirmascout-common.toml");
        context.registerConfig(ModConfig.Type.CLIENT,ScoutConfig.CLIENT,"terrafirmascout-client.toml");
    }
}
