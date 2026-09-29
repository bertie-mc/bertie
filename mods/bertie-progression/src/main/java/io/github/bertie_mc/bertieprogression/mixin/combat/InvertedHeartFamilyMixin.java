package io.github.bertie_mc.bertieprogression.mixin.combat;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.bertie_mc.bertieprogression.combat.DamageRedirectContext;
import io.github.bertie_mc.bertieprogression.combat.DamageRoutes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "com.sammy.malum.common.geas.authority.InvertedHeartAuthority", remap = false)
public abstract class InvertedHeartFamilyMixin {
    @WrapMethod(method = {"finalizedIncomingDamageEvent", "finalizedOutgoingDamageEvent"})
    private void bertie$retainFamily(
            LivingDamageEvent.Post event,
            LivingEntity attacker,
            LivingEntity target,
            ItemStack stack,
            Operation<Void> original) {
        if (DamageRoutes.redirected(event.getSource())) return;
        DamageRedirectContext.begin(event.getSource());
        try {
            original.call(event, attacker, target, stack);
        } finally {
            DamageRedirectContext.end();
        }
    }
}
