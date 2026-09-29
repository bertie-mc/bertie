package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.EnergyLayers;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CommonHooks.class, remap = false)
public abstract class DamageLayersMixin {
    @Inject(method = "onLivingDamagePre", at = @At("RETURN"), cancellable = true, require = 1)
    private static void bertie$finishLayers(
            LivingEntity target, DamageContainer container, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(EnergyLayers.finish(target, container, cir.getReturnValueF()));
    }
}
