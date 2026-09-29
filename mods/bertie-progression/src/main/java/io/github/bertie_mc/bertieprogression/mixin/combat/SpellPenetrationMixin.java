package io.github.bertie_mc.bertieprogression.mixin.combat;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.bertie_mc.bertieprogression.combat.DamageFamilies;
import io.github.bertie_mc.bertieprogression.combat.SpellDamageContext;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "io.redspace.ironsspellbooks.damage.DamageSources", remap = false)
public abstract class SpellPenetrationMixin {
    @Inject(method = "getResist", at = @At("HEAD"), cancellable = true, require = 1)
    private static void bertie$pureIgnoresSpellResistance(
            LivingEntity target, SchoolType school, CallbackInfoReturnable<Float> cir) {
        var source = SpellDamageContext.current();
        if (source != null && DamageFamilies.isPure(source)) cir.setReturnValue(1.0F);
    }

    @WrapMethod(method = "applyDamage")
    private static boolean bertie$spellContext(
            Entity target, float amount, DamageSource source, Operation<Boolean> original) {
        SpellDamageContext.begin(source);
        try {
            return original.call(target, amount, source);
        } finally {
            SpellDamageContext.end();
        }
    }

    @ModifyExpressionValue(
            method = "getResist",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/LivingEntity;getAttributeValue(Lnet/minecraft/core/Holder;)D"),
            require = 1)
    private static double bertie$penetrateResistance(double resistance) {
        return SpellDamageContext.penetrate(resistance);
    }
}
