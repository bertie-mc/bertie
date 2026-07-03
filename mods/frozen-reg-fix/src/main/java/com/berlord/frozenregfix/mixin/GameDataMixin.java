package com.berlord.frozenregfix.mixin;

import com.berlord.frozenregfix.FrozenRegFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Clean hook: CommonModLoader runs GameData.unfreezeData() -> postRegisterEvents() -> freezeData(),
 * so freezeData() HEAD is "all RegisterEvents fired, nothing frozen yet". Works in normal packs.
 * A load-order disruptor can reroute the freeze so this never runs -- {@link MappedRegistryMixin}
 * is the backstop for that case. Both call the idempotent {@link FrozenRegFix#forceTargets()}.
 */
@Mixin(targets = "net.neoforged.neoforge.registries.GameData", remap = false)
public class GameDataMixin {

    @Inject(method = "freezeData", at = @At("HEAD"))
    private static void frozenregfix$onFreezeData(CallbackInfo ci) {
        FrozenRegFix.forceTargets("GameData.freezeData");
    }
}
