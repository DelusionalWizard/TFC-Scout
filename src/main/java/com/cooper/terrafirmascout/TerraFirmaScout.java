package com.cooper.terrafirmascout;
import com.cooper.terrafirmascout.config.ScoutConfig;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
@Mod(TerraFirmaScout.ID)
public final class TerraFirmaScout {
    public static final String ID="terrafirmascout";
    public static final Logger LOGGER=LogUtils.getLogger();
    public TerraFirmaScout(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON,ScoutConfig.COMMON,"terrafirmascout-common.toml");
        container.registerConfig(ModConfig.Type.CLIENT,ScoutConfig.CLIENT,"terrafirmascout-client.toml");
    }
}
