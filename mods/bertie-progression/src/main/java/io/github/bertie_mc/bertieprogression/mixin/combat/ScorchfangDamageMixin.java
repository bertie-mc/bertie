package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.DamageFamily;
import io.github.bertie_mc.bertieprogression.combat.DamageRoutes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "net.mcreator.armageddonmod.procedures.TheScorchfangLivingEntityIsHitWithToolProcedure", remap = false)
public abstract class ScorchfangDamageMixin {
    @Redirect(
            method = "*",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"),
            require = 1)
    private static boolean bertie$damageType(Entity recipient, DamageSource original, float amount) {
        return recipient.hurt(DamageRoutes.typed(recipient, original, "scorchfang", DamageFamily.ENERGY), amount);
    }
}
