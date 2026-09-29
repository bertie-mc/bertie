package io.github.bertie_mc.bertieprogression.mixin.combat;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.bertie_mc.bertieprogression.combat.DamageFamilies;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "auviotre.enigmatic.legacy.contents.item.etherium.EtheriumSword$Events", remap = false)
public abstract class EtheriumSwordDodgeMixin {
    @ModifyExpressionValue(
            method = "onAttacked",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextFloat()F", ordinal = 0),
            require = 1)
    private static float bertie$oneDodgeRoll(float original) {
        return Float.POSITIVE_INFINITY;
    }

    @Inject(method = "onAttacked", at = @At("HEAD"), cancellable = true, require = 1)
    private static void bertie$pure(LivingIncomingDamageEvent event, CallbackInfo ci) {
        if (DamageFamilies.isPure(event.getSource())) ci.cancel();
    }
}
