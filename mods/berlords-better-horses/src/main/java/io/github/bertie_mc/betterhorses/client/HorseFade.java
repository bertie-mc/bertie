package io.github.bertie_mc.betterhorses.client;

import com.mojang.blaze3d.vertex.*;
import io.github.bertie_mc.betterhorses.mixin.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.Horse;

public final class HorseFade {
    private HorseFade() {}

    public static float alphaFor(LivingEntity entity, float partial) {
        Minecraft client = Minecraft.getInstance();
        if (!(entity instanceof Horse)
                || client.player == null
                || client.screen != null
                || client.getCameraEntity() != client.player
                || !client.options.getCameraType().isFirstPerson()
                || client.player.getVehicle() != entity) return 1;
        float progress = Mth.clamp(client.player.getViewXRot(partial) / 90, 0, 1);
        return 1 - 0.9F * progress * progress * (3 - 2 * progress);
    }

    public static MultiBufferSource buffers(MultiBufferSource original, float alpha) {
        if (alpha >= 1) return original;
        return type -> {
            // Wrap the entire horse render so coat, markings, armor, shoes and saddle fade together.
            if (type.format() == DefaultVertexFormat.NEW_ENTITY && type instanceof HorseRenderTypeAccessor composite) {
                var state = (HorseRenderStateAccessor) (Object) composite.betterhorses$state();
                var texture = ((HorseTextureAccessor) state.betterhorses$texture()).betterhorses$textureLocation();
                if (texture.isPresent())
                    return new FadedVertex(original.getBuffer(RenderType.entityTranslucent(texture.get())), alpha);
            }
            return original.getBuffer(type);
        };
    }

    private record FadedVertex(VertexConsumer delegate, float alpha) implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            delegate.setColor(r, g, b, Math.round(a * alpha));
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            delegate.setNormal(x, y, z);
            return this;
        }
    }
}
