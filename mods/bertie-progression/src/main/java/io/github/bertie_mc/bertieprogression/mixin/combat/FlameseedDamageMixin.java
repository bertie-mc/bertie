package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.CombatRegistry;
import io.github.bertie_mc.bertieprogression.combat.DamageFamily;
import io.github.bertie_mc.bertieprogression.combat.DamageRoutes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "net.sweenus.simplyswords.effect.FlameSeedEffect", remap = false)
public abstract class FlameseedDamageMixin {
    @Shadow
    public LivingEntity sourceEntity;

    @Redirect(
            method = "applyEffectTick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
                            ordinal = 0),
            require = 1)
    private boolean bertie$burst(LivingEntity target, DamageSource source, float amount) {
        return target.hurt(DamageRoutes.typed(target, source, "flameseed_burst", DamageFamily.PHYSICAL), amount);
    }

    @Redirect(
            method = "applyEffectTick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
                            ordinal = 1),
            require = 1)
    private boolean bertie$burnOrFinalBurst(LivingEntity target, DamageSource source, float amount) {
        var effect = CombatRegistry.effect("simplyswords:flameseed")
                .map(target::getEffect)
                .orElse(null);
        boolean burst = effect != null && effect.getDuration() < 20 && sourceEntity != null;
        return target.hurt(
                DamageRoutes.typed(
                        target,
                        source,
                        burst ? "flameseed_burst" : "flameseed_fire",
                        burst ? DamageFamily.PHYSICAL : DamageFamily.ENERGY),
                amount);
    }
}
