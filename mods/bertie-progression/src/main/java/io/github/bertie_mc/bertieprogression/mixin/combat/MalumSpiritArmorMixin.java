package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.DamageFamilies;
import java.util.Optional;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.sammy.malum.core.handlers.MalumAttributeEventHandler", remap = false)
public abstract class MalumSpiritArmorMixin {
    @Inject(method = "modifyMagicDamageArmorPiercing", at = @At("HEAD"), cancellable = true, require = 1)
    private static void bertie$noEnergyArmor(
            LivingEntity target, DamageSource source, float amount, CallbackInfoReturnable<Optional<Float>> cir) {
        if (!DamageFamilies.of(source).usesArmor()) {
            cir.setReturnValue(Optional.empty());
        }
    }
}
