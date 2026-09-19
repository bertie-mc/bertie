package io.github.bertie_mc.betterhorses.mixin;

import io.github.bertie_mc.betterhorses.HorseEquipment;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.animal.horse.Horse;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(LocalPlayer.class)
public abstract class TravellerJumpMixin {
    @Shadow
    private float jumpRidingScale;

    @Shadow
    private int jumpRidingTicks;

    @Shadow
    protected abstract void sendRidingJump();

    @Unique
    private boolean betterhorses$jumpHeld;

    @Redirect(
            method = "aiStep",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/player/LocalPlayer;jumpableVehicle()Lnet/minecraft/world/entity/PlayerRideableJumping;"))
    private PlayerRideableJumping betterhorses$pressToJump(LocalPlayer player) {
        PlayerRideableJumping vehicle = player.jumpableVehicle();
        boolean pressed = player.input.jumping;
        boolean justPressed = pressed && !betterhorses$jumpHeld;
        betterhorses$jumpHeld = pressed;
        if (vehicle instanceof Horse horse && ((HorseEquipment) horse).betterhorses$traveller()) {
            if (justPressed && horse.onGround() && vehicle.getJumpCooldown() == 0) {
                jumpRidingScale = 1.0F;
                jumpRidingTicks = -10;
                vehicle.onPlayerJump(100);
                sendRidingJump();
            }
            // Suppress vanilla charging and its second jump on key release.
            return null;
        }
        return vehicle;
    }
}
