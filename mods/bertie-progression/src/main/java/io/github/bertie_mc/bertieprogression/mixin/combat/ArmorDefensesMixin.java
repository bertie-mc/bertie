package io.github.bertie_mc.bertieprogression.mixin.combat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.bertie_mc.bertieprogression.combat.ArmorDefenses;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class ArmorDefensesMixin {
    @ModifyReturnValue(method = "getArmorValue", at = @At("RETURN"), require = 1)
    private int bertie$counterEffects(int original) {
        return Mth.floor(ArmorDefenses.effectiveArmor((LivingEntity) (Object) this));
    }
}
