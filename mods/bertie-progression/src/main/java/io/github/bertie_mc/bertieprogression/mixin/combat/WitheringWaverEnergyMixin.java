package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.InnateEnergyResistance;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "io.github.bertie_mc.witheringwaver.entity.WitheringWaverEntity", remap = false)
public abstract class WitheringWaverEnergyMixin implements InnateEnergyResistance {
    @Shadow
    public abstract boolean isAssimilated();

    public double bertie$energyResistance() {
        return isAssimilated() ? 0.6 : 0.3;
    }

    @Redirect(
            method = "hurt",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/damagesource/DamageSource;is(Lnet/minecraft/tags/TagKey;)Z"),
            require = 1)
    private boolean bertie$centralResistance(DamageSource source, TagKey<DamageType> tag) {
        return !tag.equals(DamageTypeTags.WITCH_RESISTANT_TO) && source.is(tag);
    }
}
