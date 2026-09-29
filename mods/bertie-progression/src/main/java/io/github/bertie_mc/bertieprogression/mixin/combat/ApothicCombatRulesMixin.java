package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.CombatMath;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.shadowsoffire.apothic_attributes.api.ALCombatRules", remap = false)
public abstract class ApothicCombatRulesMixin {
    @Inject(method = "getAValue", at = @At("HEAD"), cancellable = true, require = 1)
    private static void bertie$pressure(float damage, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue((float) CombatMath.armorPressure(damage));
    }

    @Inject(method = "getArmorDamageReduction", at = @At("HEAD"), cancellable = true, require = 1)
    private static void bertie$armor(float damage, float armor, float toughness, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue((float) CombatMath.armorRemaining(damage, armor));
    }

    @Inject(method = "getProtDamageReduction", at = @At("HEAD"), cancellable = true, require = 1)
    private static void bertie$protection(float points, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue((float) CombatMath.protectionRemaining(points));
    }

    @Redirect(
            method = "getDamageAfterArmor",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;modifyArmorEffectiveness(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;F)F"),
            require = 1)
    private static float bertie$counterBreach(
            ServerLevel level, ItemStack weapon, Entity target, DamageSource source, float effectiveness) {
        float changed = EnchantmentHelper.modifyArmorEffectiveness(level, weapon, target, source, effectiveness);
        if (changed >= effectiveness) {
            return changed;
        }
        double penalty = effectiveness - changed;
        double toughness =
                target instanceof LivingEntity living ? living.getAttributeValue(Attributes.ARMOR_TOUGHNESS) : 0;
        return effectiveness - (float) CombatMath.counterArmorPenalty(penalty, toughness);
    }
}
