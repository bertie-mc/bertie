package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.DamageFamilies;
import io.github.bertie_mc.bertieprogression.combat.EnergyLayers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.sammy.malum.common.geas.pact.aqueous.PatienceRepaidGeas", remap = false)
public abstract class PatienceRepaidDebtMixin {
    @Shadow
    private float bufferedDamage;

    @Inject(method = "incomingDamageEvent", at = @At("HEAD"), cancellable = true, require = 1)
    private void bertie$deferAfterDefenses(
            LivingIncomingDamageEvent event,
            LivingEntity attacker,
            LivingEntity target,
            ItemStack stack,
            CallbackInfo ci) {
        if (!event.isCanceled() && !DamageFamilies.isPure(event.getSource())) {
            var container = event.getContainer();
            EnergyLayers.state(container).deferDebt(() -> {
                float half = container.getNewDamage() * 0.5F;
                container.setNewDamage(half);
                bufferedDamage += half * 1.2F;
            });
        }
        ci.cancel();
    }
}
