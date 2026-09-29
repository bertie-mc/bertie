package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.InnateEnergyResistance;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.monster.Witch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Witch.class)
public abstract class WitchEnergyMixin implements InnateEnergyResistance {
    public double bertie$energyResistance() {
        return 0.85;
    }

    @Redirect(
            method = "getDamageAfterMagicAbsorb",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/damagesource/DamageSource;is(Lnet/minecraft/tags/TagKey;)Z"),
            require = 1)
    private boolean bertie$centralResistance(DamageSource source, TagKey<DamageType> tag) {
        return !tag.equals(DamageTypeTags.WITCH_RESISTANT_TO) && source.is(tag);
    }
}
