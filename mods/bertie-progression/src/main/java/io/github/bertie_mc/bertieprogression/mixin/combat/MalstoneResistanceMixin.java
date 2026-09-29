package io.github.bertie_mc.bertieprogression.mixin.combat;

import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.ytgld.malstone.event.MyEvent", remap = false)
public abstract class MalstoneResistanceMixin {
    @Inject(method = "damageAndMagic", at = @At("HEAD"), cancellable = true, require = 1)
    private void bertie$sharedResistance(LivingDamageEvent.Pre event, CallbackInfo ci) {
        ci.cancel();
    }
}
