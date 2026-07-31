package com.berlord.frozenregfix.mixin;

import com.berlord.frozenregfix.FrozenRegFix;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Backstop hook. Every registry freeze -- however the mod loader orchestrates it -- goes through
 * MappedRegistry.freeze(). We fire only for the armor_material registry: at the HEAD of its freeze()
 * the registry is still open, so forcing Immersive Armors' Items clinit there lands its material
 * registration in the writable window even when a load-order disruptor (railways-untold) reroutes the
 * freeze around GameData.freezeData(). Idempotent via {@link FrozenRegFix#forceTargets()}.
 *
 * remap=false: NeoForge 1.21 runs Mojmap, so the "freeze" name matches at runtime as-is.
 */
@Mixin(value = MappedRegistry.class, remap = false)
public class MappedRegistryMixin {

    @Shadow
    private boolean frozen;

    @Inject(method = "freeze", at = @At("HEAD"))
    private void frozenregfix$onRegistryFreeze(CallbackInfoReturnable<Registry<?>> cir) {
        if (((Registry<?>) (Object) this).key() == Registries.ARMOR_MATERIAL) {
            FrozenRegFix.forceTargets("MappedRegistry.freeze:armor_material");
        }
    }

    /**
     * NeoForge 21.1.233 no longer performs a second low-level armor-material freeze after
     * mod construction, so the pre-freeze hooks only observe vanilla bootstrap. Immersive
     * Armors 1.7.6 still registers its materials lazily on the first client tick. Bypass
     * only the frozen-state guard for those exact keys; duplicate and value validation in
     * MappedRegistry.register() remains intact.
     */
    @Inject(
        method = "validateWrite(Lnet/minecraft/resources/ResourceKey;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void frozenregfix$allowImmersiveArmorMaterial(
        ResourceKey<?> requestedKey,
        CallbackInfo ci
    ) {
        if (this.frozen
            && ((Registry<?>) (Object) this).key() == Registries.ARMOR_MATERIAL
            && requestedKey.location().getNamespace().equals("immersive_armors")) {
            FrozenRegFix.LOGGER.warn(
                "[{}] allowing targeted late armor-material registration for {}",
                FrozenRegFix.MOD_ID,
                requestedKey.location()
            );
            ci.cancel();
        }
    }
}
