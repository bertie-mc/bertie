package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.CombatTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "org.confluence.terra_curio.util.TCUtils", remap = false)
public abstract class TerraWorldFireMixin {
    @Inject(method = "isFire", at = @At("HEAD"), cancellable = true, require = 1)
    private static void bertie$worldFire(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(source.is(DamageTypeTags.IS_FIRE)
                && source.typeHolder().is(CombatTags.CONTACT_FIRE)
                && !source.is(DamageTypes.LAVA));
    }
}
