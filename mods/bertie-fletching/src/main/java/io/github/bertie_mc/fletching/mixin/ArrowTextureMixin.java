package io.github.bertie_mc.fletching.mixin;

import com.fletchery.mod.client.render.CustomArrowEntityRenderer;
import com.fletchery.mod.entity.CustomArrowEntity;
import io.github.bertie_mc.fletching.client.CoatingModels;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CustomArrowEntityRenderer.class, remap = false)
public abstract class ArrowTextureMixin {
    @Inject(
            method =
                    "getTextureLocation(Lcom/fletchery/mod/entity/CustomArrowEntity;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("RETURN"),
            cancellable = true)
    private void bertie$coatTexture(CustomArrowEntity arrow, CallbackInfoReturnable<ResourceLocation> cir) {
        var tag = arrow.getCustomProperties();
        if (tag.getBoolean("bertieCoating"))
            cir.setReturnValue(CoatingModels.coatTexture(cir.getReturnValue(), tag.getInt("bertiePotionColor")));
    }
}
