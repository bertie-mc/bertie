package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.DamageFamilies;
import io.github.bertie_mc.bertieprogression.combat.LanternPool;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "auviotre.enigmatic.legacy.contents.item.etherium.EtheriumArmor$Events", remap = false)
public abstract class EtheriumShieldMixin {
    @Inject(method = "onAttacked", at = @At("HEAD"), cancellable = true, require = 1)
    private static void bertie$projectilesAndLatePool(LivingIncomingDamageEvent event, CallbackInfo ci) {
        LanternPool.projectileShield(event);
        ci.cancel();
    }

    @Inject(method = "onDamage", at = @At("HEAD"), cancellable = true, require = 1)
    private static void bertie$pureSkipsLowHealthReduction(LivingDamageEvent.Pre event, CallbackInfo ci) {
        if (DamageFamilies.isPure(event.getSource())) ci.cancel();
    }
}
