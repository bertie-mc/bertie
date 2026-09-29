package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.DamageFamilies;
import io.github.bertie_mc.bertieprogression.combat.DamageFamily;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.neoforged.neoforge.common.Tags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "dev.xkmc.l2hostility.content.traits.legendary.DementorTrait", remap = false)
public abstract class DementorPhysicalMixin {
    @Redirect(
            method = "onDamaged",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/damagesource/DamageSource;is(Lnet/minecraft/tags/TagKey;)Z"),
            require = 1)
    private boolean bertie$physicalOnly(DamageSource source, TagKey<DamageType> tag) {
        return tag.equals(Tags.DamageTypes.IS_MAGIC)
                ? DamageFamilies.of(source) != DamageFamily.PHYSICAL
                : source.is(tag);
    }
}
