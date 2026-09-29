package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.LifeStealRules;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CommonHooks.class, remap = false)
public abstract class SharedLifeStealMixin {
    @Inject(method = "onLivingDamagePost", at = @At("TAIL"), require = 1)
    private static void bertie$lifeSteal(LivingEntity target, DamageContainer container, CallbackInfo ci) {
        LifeStealRules.finish(target, container);
    }
}
