package com.berlord.foodsystem.mixin;

import com.berlord.foodsystem.buffs.ActiveFoods;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** climbing ability: walls behave like ladders while a climbing food is active. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityClimbMixin {

    @Inject(method = "onClimbable", at = @At("RETURN"), cancellable = true)
    private void bfs$climbWalls(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) return;
        if (!((Object) this instanceof Player player)) return;
        if (player.isSpectator() || player.getAbilities().flying) return;
        if (player.horizontalCollision && ActiveFoods.hasClimbing(player)) {
            cir.setReturnValue(true);
        }
    }
}
