package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.CombatHitState;
import io.github.bertie_mc.bertieprogression.combat.DamageFamilies;
import io.github.bertie_mc.bertieprogression.combat.EnergyLayers;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.sammy.malum.core.handlers.SoulWardHandler", remap = false)
public abstract class SoulWardOrderMixin {
    @Shadow
    public static void shieldPlayer(LivingDamageEvent.Pre event) {
        throw new AssertionError();
    }

    @Inject(method = "shieldPlayer", at = @At("HEAD"), cancellable = true, require = 1)
    private static void bertie$wardAfterResistance(LivingDamageEvent.Pre event, CallbackInfo ci) {
        if (DamageFamilies.isPure(event.getSource())) {
            ci.cancel();
            return;
        }
        CombatHitState state = EnergyLayers.current(event.getEntity());
        if (state != null && !state.finished() && !state.applyingDefenses()) {
            state.deferDefense(() -> shieldPlayer(event));
            ci.cancel();
        }
    }
}
