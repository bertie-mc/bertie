package io.github.bertie_mc.betterhorses.mixin;

import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Horse.class)
public abstract class HorseMobilityMixin extends AbstractHorse {
    protected HorseMobilityMixin(EntityType<? extends AbstractHorse> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean dismountsUnderwater() {
        return getPassengers().stream().noneMatch(Player.class::isInstance) && super.dismountsUnderwater();
    }

    @Override
    public void setStanding(boolean standing) {
        if (standing && isTamed() && isSaddled() && getControllingPassenger() instanceof Player) return;
        super.setStanding(standing);
    }

    @Override
    protected void tickRidden(Player player, Vec3 input) {
        if (isTamed() && isSaddled()) setStanding(false);
        super.tickRidden(player, input);
    }

    @Override
    public void travel(Vec3 input) {
        if (!isInWater() || getPassengers().stream().noneMatch(Player.class::isInstance)) {
            super.travel(input);
            return;
        }
        if (!isControlledByLocalInstance()) return;

        // Match half the steady-state dry-ground speed, including the client's extra 0.98 drag.
        double passiveDrag = isEffectiveAi() ? 1 : 0.98;
        double groundDrag = 0.6 * 0.91 * passiveDrag;
        double swimDrag = 0.8 * passiveDrag;
        float acceleration = (float) (getSpeed() * 0.5 * (1 - swimDrag) / (1 - groundDrag));
        moveRelative(acceleration, input);
        Vec3 motion = getDeltaMovement();
        // Keep the horse's head and both riders above the surface without requiring jump input.
        double rise = Mth.clamp((getFluidHeight(FluidTags.WATER) - 0.8) * 0.12, -0.05, 0.12);
        if (horizontalCollision && isFree(motion.x, 0.6, motion.z)) rise = 0.3;
        setDeltaMovement(motion.x, rise, motion.z);
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(0.8));
        resetFallDistance();
        calculateEntityAnimation(false);
    }
}
