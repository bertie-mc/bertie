package io.github.bertie_mc.betterhorses.mixin;

import io.github.bertie_mc.betterhorses.HorseAppearance;
import net.minecraft.client.renderer.entity.layers.HorseMarkingLayer;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Markings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(HorseMarkingLayer.class)
public abstract class HorseAppearanceMarkingsMixin {
    @Redirect(
            method =
                    "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/animal/horse/Horse;FFFFFF)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/animal/horse/Horse;getMarkings()Lnet/minecraft/world/entity/animal/horse/Markings;"))
    private Markings betterhorses$hideNormalMarkings(Horse horse) {
        return ((HorseAppearance) horse).betterhorses$appearance() == HorseAppearance.Style.NORMAL
                ? horse.getMarkings()
                : Markings.NONE;
    }
}
