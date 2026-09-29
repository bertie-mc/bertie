package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.DamageFamilies;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.craisinlord.antarchy.neoforge.registry.AntarchyNeoForgeEvents", remap = false)
public abstract class BloodglassPureMixin {
    @Inject(method = "handleBloodglassShield", at = @At("HEAD"), cancellable = true, require = 1)
    private static void bertie$pureSkipsBloodglass(LivingIncomingDamageEvent event, CallbackInfo ci) {
        if (DamageFamilies.isPure(event.getSource())) ci.cancel();
    }
}
