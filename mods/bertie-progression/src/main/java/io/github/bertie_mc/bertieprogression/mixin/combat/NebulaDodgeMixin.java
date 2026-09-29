package io.github.bertie_mc.bertieprogression.mixin.combat;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "auviotre.enigmatic.legacy.contents.item.spellstones.EyeOfNebula$Events", remap = false)
public abstract class NebulaDodgeMixin {
    @ModifyExpressionValue(
            method = "onAttack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextFloat()F", ordinal = 0),
            require = 1)
    private static float bertie$oneDodgeRoll(float original) {
        return Float.POSITIVE_INFINITY;
    }
}
