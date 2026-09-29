package io.github.bertie_mc.bertieprogression.mixin.combat;

import dev.xkmc.l2complements.content.feature.EntityFeature;
import dev.xkmc.l2complements.init.data.LCConfig;
import io.github.bertie_mc.bertieprogression.combat.DamageFamilies;
import net.minecraft.tags.DamageTypeTags;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.xkmc.l2complements.events.MagicEventHandler", remap = false)
public abstract class L2OwnerlessImmunityMixin {
    @Inject(method = "onLivingAttack", at = @At("TAIL"), require = 1)
    private static void bertie$retainOwnerlessException(LivingIncomingDamageEvent event, CallbackInfo ci) {
        var source = event.getSource();
        if (LCConfig.SERVER.enableImmunityEnchantments.get()
                && DamageFamilies.isPure(source)
                && source.getEntity() == null
                && !source.typeHolder().is(DamageTypeTags.BYPASSES_EFFECTS)
                && !source.typeHolder().is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                && EntityFeature.ENVIRONMENTAL_REJECT.test(event.getEntity())) {
            event.setCanceled(true);
        }
    }
}
