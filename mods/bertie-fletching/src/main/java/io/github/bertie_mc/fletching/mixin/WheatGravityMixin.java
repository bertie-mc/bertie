package io.github.bertie_mc.fletching.mixin;

import com.fletchery.mod.arrow.ArrowComponentResolver;
import com.fletchery.mod.arrow.ArrowProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ArrowComponentResolver.class, remap = false)
public abstract class WheatGravityMixin {
    @Inject(method = "applyFeather", at = @At("TAIL"))
    private static void bertie$addWheatGravity(String feather, ArrowProperties properties, CallbackInfo ci) {
        if ("minecraft:wheat".equals(feather)) {
            // The setting is additional gravity: 0.05 means 105% of normal gravity.
            properties.gravityMultiplier += 1.0F;
        }
    }
}
