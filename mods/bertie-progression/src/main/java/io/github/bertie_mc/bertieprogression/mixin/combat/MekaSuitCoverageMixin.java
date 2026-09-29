package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.DamageFamilies;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "mekanism.common.item.gear.ItemMekaSuitArmor", remap = false)
public abstract class MekaSuitCoverageMixin {
    @Inject(method = "getDamageAbsorbed", at = @At("HEAD"), cancellable = true, require = 1)
    private static void bertie$pureSkipsPoweredArmor(
            Player player, DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        if (DamageFamilies.isPure(source)) cir.setReturnValue(0.0F);
    }

    @Redirect(
            method = "getDamageAbsorbed",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/damagesource/DamageSource;is(Lnet/minecraft/tags/TagKey;)Z"),
            require = 1)
    private static boolean bertie$shieldEligibility(DamageSource source, TagKey<DamageType> tag) {
        return !tag.equals(DamageTypeTags.BYPASSES_ARMOR) && source.is(tag);
    }
}
