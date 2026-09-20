package io.github.bertie_mc.creatures.server.entity.ai;

import io.github.bertie_mc.creatures.server.entity.living.DeepOneBaseEntity;
import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

public class DeepOneAttackGoal extends Goal {

    private DeepOneBaseEntity deepOne;

    public DeepOneAttackGoal(DeepOneBaseEntity deepOne) {
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        this.deepOne = deepOne;
    }

    @Override
    public boolean canUse() {
        return deepOne.getTarget() != null && deepOne.getTarget().isAlive() && !deepOne.isTradingLocked();
    }

    @Override
    public void stop() {
        super.stop();
        deepOne.setSoundsAngry(false);
    }

    public void tick() {
        LivingEntity target = deepOne.getTarget();
        if (target != null) {
            deepOne.getLookControl()
                    .setLookAt(target.getX(), target.getEyeY(), target.getZ(), 20.0F, (float) deepOne.getMaxHeadXRot());
            deepOne.startAttackBehavior(target);
            deepOne.setSoundsAngry(true);
            if (deepOne.distanceTo(target) <= 16) {}
        }
    }
}
