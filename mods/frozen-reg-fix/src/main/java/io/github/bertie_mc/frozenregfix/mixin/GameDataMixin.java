package io.github.bertie_mc.frozenregfix.mixin;

import io.github.bertie_mc.frozenregfix.FrozenRegFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Primary hook after register events and immediately before NeoForge freezes registries. */
@Mixin(targets = "net.neoforged.neoforge.registries.GameData", remap = false)
public class GameDataMixin {

    @Inject(method = "freezeData", at = @At("HEAD"))
    private static void frozenregfix$onFreezeData(CallbackInfo ci) {
        FrozenRegFix.forceTargets("GameData.freezeData");
    }
}
