package com.cooper.terrafirmascout.mixin;
import java.nio.file.Path;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(CreateWorldScreen.class)
public interface CreateWorldAccess { @Invoker("getTempDataPackDir") Path scout$dataPackDir(); }
