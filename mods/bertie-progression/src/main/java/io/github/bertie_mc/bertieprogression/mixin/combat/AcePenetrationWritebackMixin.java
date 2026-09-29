package io.github.bertie_mc.bertieprogression.mixin.combat;

import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.acetheeldritchking.aces_spell_utils.events.AcesSpellUtilsServerEvents", remap = false)
public abstract class AcePenetrationWritebackMixin {
    @Inject(method = "spellResPenetrationEvent", at = @At("HEAD"), cancellable = true, require = 1)
    private static void bertie$noDamageReconstruction(LivingDamageEvent.Pre event, CallbackInfo ci) {
        ci.cancel();
    }
}
