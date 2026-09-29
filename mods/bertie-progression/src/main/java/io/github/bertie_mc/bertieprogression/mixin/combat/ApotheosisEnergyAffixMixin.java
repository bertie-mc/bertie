package io.github.bertie_mc.bertieprogression.mixin.combat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.affix.effect.DamageReductionAffix;
import io.github.bertie_mc.bertieprogression.combat.EnergyLayers;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "dev.shadowsoffire.apotheosis.affix.effect.DamageReductionAffix", remap = false)
public abstract class ApotheosisEnergyAffixMixin {
    @Shadow
    @Final
    protected DamageReductionAffix.DamageType type;

    @ModifyReturnValue(method = "onHurt", at = @At("RETURN"), require = 1)
    private float bertie$energyContribution(
            float reduced, AffixInstance affix, DamageSource source, LivingEntity target, float amount) {
        return type == DamageReductionAffix.DamageType.MAGIC
                ? EnergyLayers.capture(target, source, amount, reduced)
                : reduced;
    }
}
