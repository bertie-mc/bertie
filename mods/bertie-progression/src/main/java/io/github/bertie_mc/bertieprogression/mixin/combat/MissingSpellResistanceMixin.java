package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.SpellResistance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class MissingSpellResistanceMixin {
    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, require = 1)
    private float bertie$spellResistanceOnce(float amount, DamageSource source, float originalAmount) {
        return ModList.get().isLoaded("irons_spellbooks")
                ? SpellResistance.applyMissing((LivingEntity) (Object) this, source, amount)
                : amount;
    }
}
