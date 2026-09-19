package io.github.bertie_mc.betterhorses.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.bertie_mc.betterhorses.client.HorseFade;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(LivingEntityRenderer.class)
public abstract class HorseFadeMixin {
    @ModifyVariable(
            method =
                    "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            argsOnly = true)
    private MultiBufferSource betterhorses$fade(
            MultiBufferSource original,
            LivingEntity entity,
            float yaw,
            float partial,
            PoseStack pose,
            MultiBufferSource buffers,
            int light) {
        return HorseFade.buffers(original, HorseFade.alphaFor(entity, partial));
    }
}
