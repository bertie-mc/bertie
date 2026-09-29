package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.DamageFamilies;
import io.github.bertie_mc.bertieprogression.combat.DamageFamily;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.sammy.malum.common.geas.pact.eldritch.ArcanaphageGeas", remap = false)
public abstract class ArcanaphageFamilyMixin {
    @Inject(method = "incomingDamageEvent", at = @At("HEAD"), cancellable = true, require = 1)
    private void bertie$onlyConvertPhysical(
            LivingIncomingDamageEvent event,
            LivingEntity attacker,
            LivingEntity target,
            ItemStack stack,
            CallbackInfo ci) {
        if (DamageFamilies.of(event.getSource()) != DamageFamily.PHYSICAL) ci.cancel();
    }
}
