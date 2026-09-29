package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.DamageRoutes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.cozary.nameless_trinkets.events.ReverseCardHandler", remap = false)
public abstract class ReverseCardFamilyMixin {
    @Inject(method = "reverseDamage", at = @At("HEAD"), cancellable = true, require = 1)
    private static void bertie$noReflectionLoop(Player wearer, DamageSource source, float damage, CallbackInfo ci) {
        if (DamageRoutes.redirected(source)) ci.cancel();
    }

    @Redirect(
            method = "reverseDamage",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"),
            require = 1)
    private static boolean bertie$reflectedFamily(
            Entity attacker,
            DamageSource nativeCounter,
            float amount,
            Player wearer,
            DamageSource original,
            float originalDamage) {
        return attacker.hurt(DamageRoutes.reflect(original, nativeCounter, wearer), amount);
    }
}
