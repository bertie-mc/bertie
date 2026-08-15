package io.github.bertie_mc.witheringwaver.gametest;

import io.github.bertie_mc.witheringwaver.WwEntities;
import io.github.bertie_mc.witheringwaver.entity.OrbitingSkullEntity;
import io.github.bertie_mc.witheringwaver.entity.WitheringWaverEntity;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("witheringwaver")
@PrefixGameTestTemplate(false)
public final class WitheringWaverGameTests {
    private static final String EMPTY = "empty";

    private WitheringWaverGameTests() {}

    private static WitheringWaverEntity spawnWaver(GameTestHelper helper) {
        return helper.spawn(WwEntities.WITHERING_WAVER.get(), new Vec3(1.0, 1.0, 1.0));
    }

    private static AABB around(WitheringWaverEntity waver, double radius) {
        return waver.getBoundingBox().inflate(radius);
    }

    @GameTest(template = EMPTY, batch = "t4_stats")
    public static void statsMatchSpec(GameTestHelper helper) {
        WitheringWaverEntity waver = spawnWaver(helper);
        helper.assertTrue(waver.getMaxHealth() == 120.0F, "max health should be 120");
        helper.assertTrue(waver.getAttributeValue(Attributes.ARMOR) == 10.0, "armor should be 10");
        helper.assertTrue(waver.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == 6.0, "toughness should be 6");

        waver.hurt(helper.getLevel().damageSources().magic(), 10.0F);
        float expected = 120.0F - 10.0F * 0.7F;
        helper.assertTrue(
                Math.abs(waver.getHealth() - expected) < 0.01F,
                "magic damage should be reduced 30%, health was " + waver.getHealth());
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = "t2_reap")
    public static void reapKillsAndOrbitsSkulls(GameTestHelper helper) {
        WitheringWaverEntity waver = spawnWaver(helper);
        WitherSkeleton a = helper.spawn(EntityType.WITHER_SKELETON, new Vec3(2.0, 1.0, 1.0));
        WitherSkeleton b = helper.spawn(EntityType.WITHER_SKELETON, new Vec3(1.0, 1.0, 3.0));
        WitherSkeleton c = helper.spawn(EntityType.WITHER_SKELETON, new Vec3(3.0, 1.0, 2.0));
        a.setNoAi(true);
        b.setNoAi(true);
        c.setNoAi(true);

        waver.reapNow();

        helper.runAfterDelay(5, () -> {
            helper.assertTrue(
                    !a.isAlive() && !b.isAlive() && !c.isAlive(), "reaped wither skeletons should die immediately");
            List<OrbitingSkullEntity> skulls =
                    helper.getLevel().getEntitiesOfClass(OrbitingSkullEntity.class, around(waver, 8.0));
            helper.assertTrue(skulls.size() == 3, "expected 3 orbiting skulls, found " + skulls.size());
            List<ItemEntity> skullItems = helper.getLevel()
                    .getEntitiesOfClass(
                            ItemEntity.class,
                            around(waver, 6.0),
                            item -> item.getItem().is(Items.WITHER_SKELETON_SKULL));
            helper.assertTrue(skullItems.isEmpty(), "reaped skeletons must never drop skulls");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, batch = "t1_assimilate")
    public static void assimilateBuffsAndCrumbles(GameTestHelper helper) {
        WitheringWaverEntity waver = spawnWaver(helper);
        Skeleton skeleton = helper.spawn(EntityType.SKELETON, new Vec3(2.0, 1.0, 2.0));
        Stray stray = helper.spawn(EntityType.STRAY, new Vec3(1.0, 1.0, 3.0));
        skeleton.setNoAi(true);
        stray.setNoAi(true);

        waver.assimilateNow();

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(!skeleton.isAlive() && !stray.isAlive(), "assimilated skeletons should die immediately");
            helper.assertTrue(waver.isAssimilated(), "waver should be armored after assimilating");
            helper.assertTrue(waver.getMaxHealth() == 144.0F, "expected 144 max health, was " + waver.getMaxHealth());
            helper.assertTrue(waver.getHealth() == 144.0F, "assimilation should fully heal");
            helper.assertTrue(
                    waver.getAttributeValue(Attributes.ARMOR) == 16.0,
                    "expected 16 armor, was " + waver.getAttributeValue(Attributes.ARMOR));
            double toughness = waver.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
            helper.assertTrue(Math.abs(toughness - 9.2) < 0.001, "expected 9.2 toughness, was " + toughness);

            // recast while armored: percentages come from the live unbuffed stats,
            // so a third skeleton stacks +12 HP / +3 armor / +1.6 toughness on top
            Skeleton third = helper.spawn(EntityType.SKELETON, new Vec3(2.0, 1.0, 3.0));
            third.setNoAi(true);
            waver.setHealth(130.0F);
            waver.assimilateNow();
            helper.assertTrue(!third.isAlive(), "recast should consume the third skeleton");
            helper.assertTrue(
                    waver.getMaxHealth() == 156.0F,
                    "expected 156 max health after stacking, was " + waver.getMaxHealth());
            helper.assertTrue(waver.getHealth() == 156.0F, "recast should fully heal again");
            helper.assertTrue(
                    waver.getAttributeValue(Attributes.ARMOR) == 19.0,
                    "expected 19 armor after stacking, was " + waver.getAttributeValue(Attributes.ARMOR));
        });
        // Past the one-tick post-cast heal window, deplete the bonus band.
        helper.runAfterDelay(4, () -> waver.setHealth(119.0F));
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(!waver.isAssimilated(), "armor should crumble once bonus health is depleted");
            helper.assertTrue(
                    waver.getMaxHealth() == 120.0F,
                    "bonus stats should be gone, max health was " + waver.getMaxHealth());
            helper.assertTrue(waver.getAttributeValue(Attributes.ARMOR) == 10.0, "armor should reset to 10");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, batch = "t3_friendly")
    public static void skeletonFamilyIsFriendly(GameTestHelper helper) {
        WitheringWaverEntity waver = spawnWaver(helper);
        WitherSkeleton skeleton = helper.spawn(EntityType.WITHER_SKELETON, new Vec3(3.0, 1.0, 3.0));
        skeleton.setNoAi(true);
        float skeletonHealth = skeleton.getHealth();

        waver.hurt(helper.getLevel().damageSources().mobAttack(skeleton), 10.0F);
        helper.assertTrue(waver.getHealth() == waver.getMaxHealth(), "skeleton attacks must not hurt the waver");
        waver.hurt(helper.getLevel().damageSources().mobAttack(waver), 10.0F);
        helper.assertTrue(waver.getHealth() == waver.getMaxHealth(), "the waver must not hurt itself");
        waver.hurt(helper.getLevel().damageSources().explosion(waver, waver), 10000.0F);
        helper.assertTrue(
                waver.getHealth() == waver.getMaxHealth(),
                "the waver's own explosions must never hurt him, whatever their size");
        helper.getLevel()
                .explode(
                        waver,
                        waver.getX(),
                        waver.getY(),
                        waver.getZ(),
                        3.0F,
                        net.minecraft.world.level.Level.ExplosionInteraction.NONE);
        helper.assertTrue(
                waver.getDeltaMovement().length() < 0.05,
                "explosions must not shove the waver, movement was " + waver.getDeltaMovement());
        skeleton.hurt(helper.getLevel().damageSources().mobAttack(waver), 10.0F);
        helper.assertTrue(
                skeleton.getHealth() == skeletonHealth, "the waver must not hurt wither skeletons outside reaping");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = "t8_spawn", timeoutTicks = 400)
    public static void naturalWitherSkeletonSpawnsReplaceOneInThirty(GameTestHelper helper) {
        net.minecraft.core.BlockPos at = helper.absolutePos(new net.minecraft.core.BlockPos(2, 2, 2));
        int total = 300;
        for (int i = 0; i < total; i++) {
            EntityType.WITHER_SKELETON.spawn(helper.getLevel(), at, net.minecraft.world.entity.MobSpawnType.NATURAL);
        }
        helper.runAfterDelay(5, () -> {
            AABB box = new AABB(at).inflate(8.0);
            int wavers = helper.getLevel()
                    .getEntitiesOfClass(WitheringWaverEntity.class, box)
                    .size();
            int skeletons = helper.getLevel()
                    .getEntitiesOfClass(WitherSkeleton.class, box)
                    .size();
            helper.getLevel()
                    .getEntitiesOfClass(WitheringWaverEntity.class, box)
                    .forEach(WitheringWaverEntity::discard);
            helper.getLevel().getEntitiesOfClass(WitherSkeleton.class, box).forEach(WitherSkeleton::discard);
            helper.assertTrue(
                    wavers >= 1 && wavers <= 60,
                    "expected roughly 1-in-30 replacement over " + total + " natural spawns, got " + wavers);
            helper.assertTrue(
                    wavers + skeletons == total,
                    "every replaced skeleton must vanish: " + wavers + " wavers + " + skeletons + " skeletons != "
                            + total);
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, batch = "t6_shrapnel")
    public static void shrapnelTierUpgradesPastSixtyPercent(GameTestHelper helper) {
        WitheringWaverEntity waver = spawnWaver(helper);
        net.minecraft.world.entity.monster.Zombie zombie = helper.spawn(EntityType.ZOMBIE, new Vec3(3.0, 1.0, 3.0));
        zombie.setNoAi(true);
        for (int i = 0; i < 4; i++) {
            waver.recordShrapnelHit(99, 8, zombie);
        }
        var four = zombie.getEffect(net.minecraft.world.effect.MobEffects.WITHER);
        helper.assertTrue(four != null && four.getAmplifier() == 1, "4 of 8 hits should be Wither II");
        waver.recordShrapnelHit(99, 8, zombie);
        var five = zombie.getEffect(net.minecraft.world.effect.MobEffects.WITHER);
        helper.assertTrue(five != null && five.getAmplifier() == 2, "5 of 8 hits should upgrade to Wither III");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = "t7_los", timeoutTicks = 400)
    public static void volleyStopsWithoutLineOfSight(GameTestHelper helper) {
        WitheringWaverEntity waver = spawnWaver(helper);
        net.minecraft.world.entity.animal.IronGolem golem =
                helper.spawn(EntityType.IRON_GOLEM, new Vec3(3.0, 1.0, 3.0));
        golem.setNoAi(true);
        golem.getAttribute(Attributes.MAX_HEALTH).setBaseValue(300.0);
        golem.setHealth(300.0F);
        for (int i = 0; i < 3; i++) {
            WitherSkeleton food = helper.spawn(EntityType.WITHER_SKELETON, new Vec3(2.0, 1.0, 1.0 + i));
            food.setNoAi(true);
        }
        waver.setTarget(golem);
        waver.reapNow();

        // Entomb the golem mid-acceleration: no angle keeps line of sight, so the
        // volley must abort and keep its skulls.
        helper.runAfterDelay(45, () -> {
            for (int x = 2; x <= 4; x++) {
                for (int z = 2; z <= 4; z++) {
                    for (int y = 1; y <= 4; y++) {
                        if (x == 3 && z == 3 && y < 4) {
                            continue;
                        }
                        helper.setBlock(
                                new net.minecraft.core.BlockPos(x, y, z),
                                net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                    }
                }
            }
        });
        helper.runAfterDelay(160, () -> {
            helper.assertTrue(
                    waver.getAbilityState() == WitheringWaverEntity.STATE_IDLE,
                    "volley should abort within 1s of losing line of sight, state was " + waver.getAbilityState());
            helper.assertTrue(waver.hasOrbitingSkulls(), "aborted volley should keep its orbiting skulls");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, batch = "t5_summon")
    public static void summonedSkeletonsDropNothing(GameTestHelper helper) {
        WitheringWaverEntity waver = spawnWaver(helper);

        waver.summonNow();

        helper.runAfterDelay(2, () -> {
            List<WitherSkeleton> summoned = helper.getLevel()
                    .getEntitiesOfClass(
                            WitherSkeleton.class,
                            around(waver, 8.0),
                            s -> s.getTags().contains(WitheringWaverEntity.TAG_SUMMONED));
            helper.assertTrue(summoned.size() == 3, "expected 3 summoned wither skeletons, found " + summoned.size());
            helper.assertTrue(waver.isReapLocked(), "reaping should be locked after summoning");
            summoned.forEach(s -> s.hurt(helper.getLevel().damageSources().generic(), 10000.0F));
        });
        helper.runAfterDelay(8, () -> {
            List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, around(waver, 10.0));
            helper.assertTrue(drops.isEmpty(), "summoned skeletons must drop nothing, found " + drops.size());
            List<ExperienceOrb> orbs = helper.getLevel().getEntitiesOfClass(ExperienceOrb.class, around(waver, 10.0));
            helper.assertTrue(orbs.isEmpty(), "summoned skeletons must drop no experience");
            helper.succeed();
        });
    }
}
