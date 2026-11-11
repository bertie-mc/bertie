package io.github.bertie_mc.bertieprogression.mixin;

import io.github.bertie_mc.bertieprogression.ender.EyeAcquisitions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.raid.Raid;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Raid.class)
public abstract class EyeRaidMixin {
    @Shadow
    @Final
    private ServerLevel level;

    @Unique
    private boolean bertie$wasVictory;

    @Inject(method = "tick", at = @At("HEAD"))
    private void bertie$beforeRaidTick(CallbackInfo ci) {
        bertie$wasVictory = ((Raid) (Object) this).isVictory();
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void bertie$raidReward(CallbackInfo ci) {
        if (!bertie$wasVictory && ((Raid) (Object) this).isVictory()) {
            EyeAcquisitions.raidWon(level, ((Raid) (Object) this).getCenter());
        }
    }
}
