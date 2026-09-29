package io.github.bertie_mc.bertieprogression.mixin.combat;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.bertie_mc.bertieprogression.combat.DamageFamilies;
import io.github.bertie_mc.bertieprogression.combat.DamageRoutes;
import io.github.bertie_mc.bertieprogression.combat.EnergyLayers;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "io.github.bertie_mc.betterhorses.HorseEvents", remap = false)
public abstract class WarriorSaddleTransferMixin {
    @WrapMethod(method = "transferDamage")
    private static void bertie$lateShare(LivingDamageEvent.Pre event, Operation<Void> original) {
        if (DamageFamilies.isPure(event.getSource()) || DamageRoutes.redirected(event.getSource())) return;
        var state = EnergyLayers.current(event.getEntity());
        if (state != null && !state.finished()) state.deferTransfer(() -> original.call(event));
        else original.call(event);
    }

    @Redirect(
            method = "transferDamage",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/player/Player;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"),
            require = 1)
    private static boolean bertie$singleHop(Player rider, DamageSource source, float amount) {
        return rider.hurt(DamageRoutes.transfer(source), amount);
    }
}
