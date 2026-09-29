package io.github.bertie_mc.bertieprogression.mixin.combat;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.bertie_mc.bertieprogression.combat.DamageFamilies;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "org.confluence.terra_curio.util.TCUtils", remap = false)
public abstract class TerraDefenseMixin {
    @ModifyExpressionValue(
            method = "applyBrainOfConfusion",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextFloat()F", ordinal = 1),
            require = 1)
    private static float bertie$oneDodgeRoll(float original) {
        return Float.POSITIVE_INFINITY;
    }

    @Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true, require = 1)
    private static void bertie$pureSkipsImmunity(
            Entity target, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (DamageFamilies.isPure(source)) cir.setReturnValue(false);
    }

    @Inject(
            method = {"applyFrozenTurtleShell", "applyInjuryFree"},
            at = @At("HEAD"),
            cancellable = true,
            require = 1)
    private static void bertie$pureSkipsReduction(
            LivingEntity target, float amount, CallbackInfoReturnable<Float> cir) {
        var stack = ((DamageStackAccessor) target).bertie$damageContainers();
        if (stack != null
                && !stack.isEmpty()
                && DamageFamilies.isPure(stack.peek().getSource())) cir.setReturnValue(amount);
    }

    @Inject(method = "applyLavaHurtReduce", at = @At("HEAD"), cancellable = true, require = 1)
    private static void bertie$pureSkipsLavaReduction(
            LivingEntity target, DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        if (DamageFamilies.isPure(source)) cir.setReturnValue(amount);
    }
}
