package com.cooper.terrafirmascout.mixin;

import com.cooper.terrafirmascout.client.Lang;import com.cooper.terrafirmascout.client.ScoutWorldCreationScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** The World tab owns and positions this button, including removal when changing tabs. */
@Mixin(targets="net.minecraft.client.gui.screens.worldselection.CreateWorldScreen$WorldTab")
public abstract class ScoutWorldTab extends GridLayoutTab {
    protected ScoutWorldTab(Component title) { super(title); }
    @Inject(method="<init>",at=@At("TAIL"))
    private void scout$addButton(CreateWorldScreen parent,CallbackInfo ci) {
        layout.addChild(Button.builder(Lang.t("terrafirmascout.title"),
            button->Minecraft.getInstance().setScreen(new ScoutWorldCreationScreen(parent)))
            .width(310).build(),3,0,1,2);
    }
}
