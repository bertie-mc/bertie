package io.github.bertie_mc.betterhorses.mixin;

import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(Player.class)
public abstract class HorseMiningMixin {
    @Redirect(
            method = "getDigSpeed",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;onGround()Z"))
    private boolean betterhorses$mountedMining(Player player) {
        return player.onGround() || player.getVehicle() instanceof Horse;
    }
}
