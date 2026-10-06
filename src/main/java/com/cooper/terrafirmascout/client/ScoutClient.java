package com.cooper.terrafirmascout.client;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
@EventBusSubscriber(modid="terrafirmascout",value=Dist.CLIENT)
public final class ScoutClient {
    @SubscribeEvent public static void init(ScreenEvent.Init.Post event) {
        if(event.getScreen() instanceof CreateWorldScreen parent) {
            event.addListener(Button.builder(Component.literal("TerraFirmaScout"),b->Minecraft.getInstance().setScreen(new ScoutWorldCreationScreen(parent)))
                .bounds(Math.max(4,parent.width-114),parent.height-52,110,20).build());
        }
    }
}
