package io.github.bertie_mc.creatures.server.entity.ai;

import java.util.EnumSet;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

public class LookForwardsGoal extends Goal {

    private Mob mob;

    public LookForwardsGoal(Mob mob) {
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));
        this.mob = mob;
    }

    @Override
    public boolean canUse() {
        return true;
    }

    public void tick() {
        mob.setYHeadRot(mob.getYRot());
    }
}
