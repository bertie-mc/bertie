package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.DamageFamily;
import io.github.bertie_mc.bertieprogression.combat.DamageRoutes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "net.sweenus.simplyswords.item.custom.EmberIreSwordItem", remap = false)
public abstract class EmberbladeExplosionMixin {
    @Redirect(
            method = "releaseUsing",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"),
            require = 1)
    private boolean bertie$damageType(LivingEntity recipient, DamageSource original, float amount) {
        return recipient.hurt(
                DamageRoutes.typed(recipient, original, "emberblade_burst", DamageFamily.PHYSICAL), amount);
    }
}
