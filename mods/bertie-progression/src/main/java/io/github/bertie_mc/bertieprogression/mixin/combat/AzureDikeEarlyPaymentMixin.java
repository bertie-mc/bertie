package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.EnergyLayers;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "earth.terrarium.pastel.attachments.data.azure_dike.AzureDikeProvider", remap = false)
public abstract class AzureDikeEarlyPaymentMixin {
    @Inject(method = "absorbDamage", at = @At("HEAD"), cancellable = true, require = 1)
    private static void bertie$deferOnlyIncomingAbsorption(
            LivingEntity entity, float amount, CallbackInfoReturnable<Float> cir) {
        var state = EnergyLayers.current(entity);
        if (state != null && state.beforeVanillaDamage()) cir.setReturnValue(amount);
    }
}
