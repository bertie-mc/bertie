package io.github.bertie_mc.betterhorses.mixin;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.animal.horse.Horse;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RandomStrollGoal.class)
public abstract class HorseWanderingMixin {
    @Shadow
    @Final
    protected PathfinderMob mob;

    @Inject(
            method = {"canUse", "canContinueToUse"},
            at = @At("HEAD"),
            cancellable = true)
    private void betterhorses$stayParked(CallbackInfoReturnable<Boolean> cir) {
        if (mob instanceof Horse horse && horse.isSaddled()) cir.setReturnValue(false);
    }
}
