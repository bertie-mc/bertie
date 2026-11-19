package io.github.bertie_mc.bertieprogression.mixin;

import io.github.bertie_mc.bertieprogression.compat.UnboundData;
import java.util.List;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "me.moltea.unbound.progress.Guide", remap = false)
public abstract class UnboundJournalMixin {
    @Shadow
    @Final
    public static List<?> NODES;

    @Inject(method = "<clinit>", at = @At("TAIL"), require = 1)
    private static void bertie$journal(CallbackInfo ci) {
        UnboundData.journal(NODES);
    }
}
