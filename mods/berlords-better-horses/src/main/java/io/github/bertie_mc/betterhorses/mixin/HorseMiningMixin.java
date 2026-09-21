package io.github.bertie_mc.betterhorses.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Lets a rider mine at full speed from horseback.
 *
 * <p>{@code getDigSpeed} divides by five while the player is off the ground, which a mounted
 * player always is. Undoing that at the return rather than redirecting {@code onGround()} is
 * deliberate: only one {@code @Redirect} may own an instruction, and Forbidden Arcanus already
 * redirects that same call for its Sea Prism modifier under a mixin config that requires the
 * injection to land. Two redirects there is not a warning, it is a startup failure.
 */
@Mixin(Player.class)
public abstract class HorseMiningMixin {

    private static final float AIRBORNE_PENALTY = 5.0F;

    @ModifyReturnValue(method = "getDigSpeed", at = @At("RETURN"))
    private float betterhorses$mountedMining(float speed) {
        Player player = (Player) (Object) this;
        if (!player.onGround() && player.getVehicle() instanceof Horse) {
            return speed * AIRBORNE_PENALTY;
        }
        return speed;
    }
}
