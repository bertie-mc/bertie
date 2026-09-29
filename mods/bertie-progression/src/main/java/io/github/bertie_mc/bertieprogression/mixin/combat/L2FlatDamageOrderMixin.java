package io.github.bertie_mc.bertieprogression.mixin.combat;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.xkmc.l2damagetracker.contents.attack.DamageData;
import io.github.bertie_mc.bertieprogression.combat.DamageFamilies;
import io.github.bertie_mc.bertieprogression.combat.EnergyLayers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.xkmc.l2damagetracker.events.L2DTGeneralAttackListener", remap = false)
public abstract class L2FlatDamageOrderMixin {
    @Inject(method = "onDamage", at = @At("HEAD"), cancellable = true, require = 1)
    private void bertie$pureSkipsGenericReduction(DamageData.Defence data, CallbackInfo ci) {
        if (DamageFamilies.isPure(data.getSource())) ci.cancel();
    }

    @ModifyExpressionValue(
            method = "onDamage",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;getValue()D",
                            ordinal = 1),
            require = 1)
    private double bertie$subtractAfterPercentages(
            double subtraction, @Local(argsOnly = true) DamageData.Defence data) {
        var state = EnergyLayers.current(data.getTarget());
        if (state == null) return subtraction;
        state.subtractLater(subtraction);
        return 0;
    }
}
