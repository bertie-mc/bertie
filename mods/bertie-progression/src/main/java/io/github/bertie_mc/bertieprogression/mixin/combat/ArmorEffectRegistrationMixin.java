package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.ArmorDefenses;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEffect.class)
public abstract class ArmorEffectRegistrationMixin {
    @Inject(method = "addAttributeModifier", at = @At("HEAD"), cancellable = true, require = 1)
    private void bertie$rememberArmorPenalty(
            Holder<Attribute> attribute,
            ResourceLocation id,
            double amount,
            AttributeModifier.Operation operation,
            CallbackInfoReturnable<MobEffect> cir) {
        MobEffect effect = (MobEffect) (Object) this;
        String type = effect.getClass().getName();
        boolean armorOnly = type.equals("com.github.L_Ender.cataclysm.effects.EffectBlazing_Brand")
                || type.equals("net.hazen.hazennstuff.Registries.Effects.IchorEffect")
                || type.equals("auviotre.enigmatic.legacy.contents.effect.IchorCorrosion");
        if (armorOnly
                && amount < 0
                && (attribute.equals(Attributes.ARMOR_TOUGHNESS)
                        || type.equals("net.hazen.hazennstuff.Registries.Effects.IchorEffect")
                                && attribute.is(
                                        ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "spell_resist")))) {
            ArmorDefenses.retire(attribute, id);
            cir.setReturnValue(effect);
            return;
        }
        if (attribute.equals(Attributes.ARMOR) && amount < 0 && effect.getCategory() == MobEffectCategory.HARMFUL) {
            ArmorDefenses.registerPenalty(id);
        }
    }
}
