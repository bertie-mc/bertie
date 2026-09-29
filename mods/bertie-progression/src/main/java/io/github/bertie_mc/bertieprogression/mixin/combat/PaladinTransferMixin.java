package io.github.bertie_mc.bertieprogression.mixin.combat;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.bertie_mc.bertieprogression.combat.DamageFamilies;
import io.github.bertie_mc.bertieprogression.combat.DamageRedirectContext;
import io.github.bertie_mc.bertieprogression.combat.DamageRoutes;
import io.github.bertie_mc.bertieprogression.combat.EnergyLayers;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "org.confluence.terra_curio.common.item.curio.combat.PaladinsShield", remap = false)
public abstract class PaladinTransferMixin {
    @WrapMethod(method = "apply")
    private static float bertie$lateShare(
            LivingEntity target, DamageSource source, float amount, Operation<Float> original) {
        if (DamageFamilies.isPure(source) || DamageRoutes.redirected(source)) return amount;
        var stack = ((DamageStackAccessor) target).bertie$damageContainers();
        if (stack != null
                && !stack.isEmpty()
                && !EnergyLayers.state(stack.peek()).finished()) {
            var container = stack.peek();
            EnergyLayers.state(container).deferTransfer(() -> {
                DamageRedirectContext.begin(source);
                try {
                    container.setNewDamage(original.call(target, source, container.getNewDamage()));
                } finally {
                    DamageRedirectContext.end();
                }
            });
            return amount;
        }
        DamageRedirectContext.begin(source);
        try {
            return original.call(target, source, amount);
        } finally {
            DamageRedirectContext.end();
        }
    }

    @Redirect(
            method = "*",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/player/Player;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"),
            require = 1)
    private static boolean bertie$preserveFamily(Player recipient, DamageSource nativeSource, float amount) {
        DamageSource source = DamageRedirectContext.current();
        return recipient.hurt(source == null ? nativeSource : DamageRoutes.transfer(source), amount);
    }
}
