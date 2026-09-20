package io.github.bertie_mc.creatures.mixin;

import io.github.bertie_mc.creatures.server.potion.ACEffectRegistry;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The mage's bubble prevents normal air recovery while its effect drains air. */
@Mixin(LivingEntity.class)
public abstract class BubbledAirMixin {
    @Inject(method = "increaseAirSupply", at = @At("HEAD"), cancellable = true)
    private void creatures$holdAir(int air, CallbackInfoReturnable<Integer> result) {
        if (((LivingEntity) (Object) this).hasEffect(ACEffectRegistry.BUBBLED)) result.setReturnValue(air);
    }
}
