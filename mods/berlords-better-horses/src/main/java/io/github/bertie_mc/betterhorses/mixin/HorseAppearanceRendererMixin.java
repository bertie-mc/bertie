package io.github.bertie_mc.betterhorses.mixin;

import io.github.bertie_mc.betterhorses.HorseAppearance;
import net.minecraft.client.renderer.entity.HorseRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.horse.Horse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HorseRenderer.class)
public abstract class HorseAppearanceRendererMixin {
    @Inject(
            method =
                    "getTextureLocation(Lnet/minecraft/world/entity/animal/horse/Horse;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("HEAD"),
            cancellable = true)
    private void betterhorses$cosmeticTexture(Horse horse, CallbackInfoReturnable<ResourceLocation> cir) {
        switch (((HorseAppearance) horse).betterhorses$appearance()) {
            case ZOMBIE ->
                cir.setReturnValue(ResourceLocation.withDefaultNamespace("textures/entity/horse/horse_zombie.png"));
            case SKELETON ->
                cir.setReturnValue(ResourceLocation.withDefaultNamespace("textures/entity/horse/horse_skeleton.png"));
            default -> {}
        }
    }
}
