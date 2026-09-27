package io.github.bertie_mc.spellrestrictions.mixin;

import io.redspace.ironsspellbooks.network.spells.LearnSpellPacket;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LearnSpellPacket.class, remap = false)
public abstract class LearnSpellPacketMixin {
    // The obsolete selection packet must not allow a client to choose its reward.
    @Inject(method = "handle", at = @At("HEAD"), cancellable = true)
    private static void rejectChosenResearch(LearnSpellPacket packet, IPayloadContext context, CallbackInfo ci) {
        ci.cancel();
    }
}
