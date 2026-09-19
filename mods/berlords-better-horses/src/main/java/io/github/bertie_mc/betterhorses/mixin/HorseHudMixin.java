package io.github.bertie_mc.betterhorses.mixin;

import io.github.bertie_mc.betterhorses.HorseEquipment;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.animal.horse.Horse;
import net.neoforged.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Gui.class)
public abstract class HorseHudMixin {
    @Shadow
    private int getVehicleMaxHearts(LivingEntity vehicle) {
        throw new AssertionError();
    }

    @Redirect(
            method = {"maybeRenderJumpMeter", "maybeRenderExperienceBar", "isExperienceBarVisible"},
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/player/LocalPlayer;jumpableVehicle()Lnet/minecraft/world/entity/PlayerRideableJumping;"))
    private PlayerRideableJumping betterhorses$jumpBarWhileCharging(LocalPlayer player) {
        PlayerRideableJumping vehicle = player.jumpableVehicle();
        if (vehicle instanceof Horse horse
                && (((HorseEquipment) horse).betterhorses$traveller() || !player.input.jumping)) return null;
        return vehicle;
    }

    @Redirect(
            method = "renderFoodLevel",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/gui/Gui;getVehicleMaxHearts(Lnet/minecraft/world/entity/LivingEntity;)I"))
    private int betterhorses$keepHungerBelowHorseHealth(Gui gui, LivingEntity vehicle) {
        // Vanilla's food layer advances rightHeight, so the following mount hearts move up one row.
        // BFS owns this layout and already places its food slots above the unchanged mount hearts.
        if (vehicle instanceof Horse && !ModList.get().isLoaded("berlordsfoodsystem")) return 0;
        return getVehicleMaxHearts(vehicle);
    }
}
