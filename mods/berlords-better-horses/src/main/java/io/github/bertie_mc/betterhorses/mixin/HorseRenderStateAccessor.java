package io.github.bertie_mc.betterhorses.mixin;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderType.CompositeState.class)
public interface HorseRenderStateAccessor {
    @Accessor("textureState")
    RenderStateShard.EmptyTextureStateShard betterhorses$texture();
}
