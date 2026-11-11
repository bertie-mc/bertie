package io.github.bertie_mc.bertieprogression.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

final class CombatFixture {
    static <T extends Mob> T spawn(GameTestHelper helper, EntityType<T> type, BlockPos relative) {
        T entity = type.create(helper.getLevel());
        if (entity == null) throw new AssertionError("Missing test entity " + type);
        // Spawn modifiers must see the controlled fixture before they initialize random traits.
        entity.setNoAi(true);
        entity.setPersistenceRequired();
        Vec3 pos = helper.absoluteVec(Vec3.atBottomCenterOf(relative));
        entity.moveTo(pos.x, pos.y, pos.z, entity.getYRot(), entity.getXRot());
        helper.getLevel().addFreshEntity(entity);
        return entity;
    }

    private CombatFixture() {}
}
