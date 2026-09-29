package io.github.bertie_mc.bertieprogression.mixin.combat;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.bertie_mc.bertieprogression.combat.DamageFamilies;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com.cozary.nameless_trinkets.events.FracturedNullstoneHandler", remap = false)
public abstract class NullstoneClassificationMixin {
    @ModifyExpressionValue(
            method = "reduceMagicDamage",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/damagesource/DamageType;msgId()Ljava/lang/String;"),
            require = 1)
    private static String bertie$energyCategory(String original, @Local(argsOnly = true) DamageSource source) {
        return DamageFamilies.isEnergy(source) ? "magic" : "bertie_non_energy";
    }
}
