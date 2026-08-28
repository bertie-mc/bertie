package io.github.bertie_mc.cooparticlesfix.mixin;

import io.github.bertie_mc.cooparticlesfix.CooParticlesFix;
import io.github.bertie_mc.cooparticlesfix.logic.ClientOnlyListenerPolicy;
import net.neoforged.fml.loading.FMLEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "cn.coostack.cooparticlesapi.event.CooEventBus", remap = false)
abstract class CooEventBusMixin {
    @Inject(method = "findListenerHandlers", at = @At("HEAD"), cancellable = true, remap = false)
    private void cooparticlesfix$skipClientOnlyTestListener(
            String listenerClassName, String ignoredModId, CallbackInfo callback) {
        if (ClientOnlyListenerPolicy.shouldSkip(FMLEnvironment.dist, listenerClassName)) {
            CooParticlesFix.LOGGER.info(
                    "Skipping client-only CooParticles listener {} on a dedicated server", listenerClassName);
            callback.cancel();
        }
    }
}
