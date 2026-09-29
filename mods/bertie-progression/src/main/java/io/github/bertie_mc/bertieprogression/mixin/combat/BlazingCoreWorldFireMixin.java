package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.CombatTags;
import io.github.bertie_mc.bertieprogression.combat.DamageFamilies;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "auviotre.enigmatic.legacy.contents.item.spellstones.BlazingCore$Events", remap = false)
public abstract class BlazingCoreWorldFireMixin {
    @Redirect(
            method = "onAttack",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/damagesource/DamageSource;is(Lnet/minecraft/tags/TagKey;)Z"),
            require = 1)
    private static boolean bertie$contactFire(DamageSource source, TagKey<DamageType> ignored) {
        return DamageFamilies.isEnergy(source) && source.typeHolder().is(CombatTags.CONTACT_FIRE);
    }
}
