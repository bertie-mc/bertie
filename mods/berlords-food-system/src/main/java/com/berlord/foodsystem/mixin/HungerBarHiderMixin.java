package com.berlord.foodsystem.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The hunger bar is meaningless under the stomach system; hide it. */
@Mixin(Gui.class)
public abstract class HungerBarHiderMixin {

    @Inject(method = "renderFood", at = @At("HEAD"), cancellable = true)
    private void bfs$hideFoodBar(GuiGraphics guiGraphics, Player player, int x, int y, CallbackInfo ci) {
        ci.cancel();
    }
}
