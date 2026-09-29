package io.github.bertie_mc.bertieprogression.mixin.combat;

import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "team.lodestar.lodestone.handlers.LodestoneAttributeEventHandler", remap = false)
public abstract class LodestoneEnergyMixin {
    @Inject(method = "processAttributes", at = @At("HEAD"), cancellable = true, require = 1)
    private static void bertie$centralEnergy(LivingDamageEvent.Pre event, CallbackInfo ci) {
        ci.cancel();
    }
}
