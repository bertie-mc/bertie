package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.MagicOrigins;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "auviotre.enigmatic.legacy.contents.attribute.ProjectileDeflectAttribute$Events", remap = false)
public abstract class EnigmaticDeflectEligibilityMixin {
    @Inject(method = "onProjectileImpact", at = @At("HEAD"), cancellable = true, require = 1)
    private static void bertie$ordinaryProjectilesOnly(ProjectileImpactEvent event, CallbackInfo ci) {
        if (MagicOrigins.excludesProjectileDefense(event.getEntity())) ci.cancel();
    }
}
