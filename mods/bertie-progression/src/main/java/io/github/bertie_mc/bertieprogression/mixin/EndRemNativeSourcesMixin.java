package io.github.bertie_mc.bertieprogression.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native callbacks are replaced by the pack's cost-gated enchant and level-four trade rules. */
@Pseudo
@Mixin(targets = "com.teamremastered.endrem.config.ConfigHandler", remap = false)
public abstract class EndRemNativeSourcesMixin {
    @Shadow
    @Final
    @Mutable
    public static boolean IS_CRYPTIC_EYE_OBTAINABLE;

    @Shadow
    @Final
    @Mutable
    public static boolean IS_EVIL_EYE_OBTAINABLE;

    @Inject(method = "<clinit>", at = @At("TAIL"), require = 1)
    private static void bertie$replacedCallbacks(CallbackInfo ci) {
        IS_CRYPTIC_EYE_OBTAINABLE = false;
        IS_EVIL_EYE_OBTAINABLE = false;
    }
}
