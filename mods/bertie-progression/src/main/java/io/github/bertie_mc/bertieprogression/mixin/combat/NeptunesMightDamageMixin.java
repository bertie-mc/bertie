package io.github.bertie_mc.bertieprogression.mixin.combat;

import net.neoforged.neoforge.common.damagesource.DamageContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.teammetallurgy.aquaculture.misc.NeptunesMight", remap = false)
public abstract class NeptunesMightDamageMixin {
    @Redirect(
            method = "onAttack",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/neoforged/neoforge/common/damagesource/DamageContainer;getOriginalDamage()F"),
            require = 1)
    private static float bertie$remainingDamage(DamageContainer damage) {
        return damage.getNewDamage();
    }
}
