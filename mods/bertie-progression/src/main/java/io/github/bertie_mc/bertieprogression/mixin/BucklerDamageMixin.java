package io.github.bertie_mc.bertieprogression.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(targets = "tallestred.piglinproliferation.common.items.BucklerItem", remap = false)
public abstract class BucklerDamageMixin {
    @ModifyVariable(method = "bucklerBash", at = @At("STORE"), ordinal = 0, require = 1)
    private static float weaponDamage(float original, LivingEntity entity) {
        return (float) Math.max(0, entity.getAttributeValue(Attributes.ATTACK_DAMAGE));
    }
}
