package io.github.bertie_mc.fletching.gametest;

import com.fletchery.mod.entity.CustomArrowEntity;
import com.fletchery.mod.registry.ModRegistries;
import io.github.bertie_mc.fletching.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(BertieFletching.ID)
@PrefixGameTestTemplate(false)
public final class RedesignGameTests {
    @GameTest(template = "empty", batch = "refine_consistency")
    public static void repeatedDrawsAndDayNightFletchingsMatch(GameTestHelper h) {
        Pig target = pig(h, 2, 3);
        for (int i = 0; i < 20; i++) {
            target.setHealth(100);
            target.invulnerableTime = 0;
            TestArrow shot = arrow(h, "feather", "stick", "copper", "");
            shot.setCritArrow(true);
            shot.hit(target);
            near(h, target.getHealth(), 83.5, "identical full draws have identical damage including the tip");
        }
        long time = h.getLevel().getDayTime();
        try {
            double health = 0;
            for (String feather : new String[] {"raven", "roadrunner"}) {
                h.getLevel().setDayTime(feather.equals("raven") ? 18000 : 6000);
                target.setHealth(100);
                target.invulnerableTime = 0;
                TestArrow shot = arrow(h, feather, "stick", "flint", "");
                shot.setCritArrow(true);
                ArrowFlight.beforeTick(shot);
                shot.hit(target);
                if (health == 0) health = target.getHealth();
                else near(h, target.getHealth(), health, "raven night and roadrunner day deal equal damage");
            }
        } finally {
            h.getLevel().setDayTime(time);
        }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "refine_launch")
    public static void SpreadChangesAimWithoutRandomSpeed(GameTestHelper h) {
        for (int i = 0; i < 40; i++) {
            TestArrow shot = arrow(h, "raven", "stick", "flint", "");
            shot.shoot(0, 0, 1, 3, 1);
            near(h, shot.getDeltaMovement().length(), 3, "inaccuracy changes direction only");
        }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "refine_crit")
    public static void SunAndSonicReuseAttributeCriticalAndProjectileBonus(GameTestHelper h) {
        var owner = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        attribute(owner, "apothic_attributes:crit_chance", 1);
        attribute(owner, "apothic_attributes:crit_damage", 2);
        attribute(owner, "apothic_attributes:projectile_damage", 1.4);
        Pig first = pig(h, 2, 3), second = pig(h, 5, 3);
        TestArrow sun = arrow(h, "sun", "stick", "flint", "");
        sun.setOwner(owner);
        sun.setCritArrow(true);
        sun.hit(first);
        near(h, first.getHealth(), 16, "physical and fire share both critical multipliers: (10 + 10) * 1.5 * 2 * 1.4");
        for (boolean critical : new boolean[] {false, true}) {
            first.setHealth(100);
            second.setHealth(100);
            first.invulnerableTime = second.invulnerableTime = 0;
            first.clearFire();
            second.clearFire();
            TestArrow echo = arrow(h, "feather", "stick", "reinforced_echo", "");
            echo.setOwner(owner);
            echo.setCritArrow(critical);
            echo.hit(first);
            near(h, second.getHealth(), first.getHealth(), "sonic damage matches raw primary at both draw strengths");
        }
        h.succeed();
    }

    private static void attribute(LivingEntity entity, String id, double value) {
        var key = net.minecraft.resources.ResourceLocation.parse(id);
        var holder = net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE
                .getHolder(key)
                .orElseThrow();
        entity.getAttribute(holder).setBaseValue(value);
    }

    @GameTest(template = "empty", batch = "refine_homing")
    public static void HomingIgnoresBehindAndPreservesCloseHitscanHit(GameTestHelper h) {
        Pig behind = pig(h, 2, 0), front = pig(h, 2, 5);
        TestArrow shot = arrow(h, "feather", "dielectric", "flint", "");
        ArrowFlight.beforeTick(shot);
        h.assertTrue(
                ArrowFlight.homingTarget(shot).orElseThrow() == front, "forward target wins over closer rear target");
        Pig side = pig(h, 3, 2);
        TestArrow scan = arrow(h, "resonant", "ender", "flint", "");
        scan.tick();
        h.assertTrue(front.getHealth() < 100, "hitscan retains an easy direct hit despite nearby off-axis targets");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "refine_secondary_range")
    public static void RadialChildrenRetainOneRangeBudgetAcrossRicochets(GameTestHelper h) {
        Pig target = pig(h, 2, 3);
        TestArrow shot = arrow(h, "stymphalian", "ender", "flint", "");
        shot.hit(target);
        var children = h.getLevel()
                .getEntitiesOfClass(
                        CustomArrowEntity.class, target.getBoundingBox().inflate(3));
        h.assertTrue(children.size() == 8, "eight children");
        for (var child : children) {
            var state = ((ArrowRuntime) child).bertie$flight();
            h.assertTrue(state.remainingRange > 0 && state.remainingRange <= 24, "range begins at split impact");
            child.setNoGravity(true);
            for (int i = 0; i < 150 && !child.isRemoved(); i++) child.tick();
            h.assertTrue(child.isRemoved(), "missed secondary arrow expires within its range");
            h.assertTrue(
                    child.position().distanceTo(target.position()) <= 25,
                    "secondary cannot reach a target fifty blocks away");
        }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "redesign_close_ricochet")
    public static void ricochetCannotSkipCloseTargetsBetweenTicks(GameTestHelper h) {
        Pig first = pig(h, 2, 3), second = pig(h, 4, 3);
        TestArrow arrow = arrow(h, "sun", "end", "flint", "");
        arrow.setDeltaMovement(0, 0, 3);
        for (int tick = 0; tick < 3 && !arrow.isRemoved(); tick++) arrow.tick();
        h.assertTrue(
                first.getHealth() < 100 && second.getHealth() < 100,
                "both close-range ricochet impacts must be detected");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "redesign_loot")
    public static void magnetInsertionPreservesOverflowEvenInCreative(GameTestHelper h) {
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
        for (int i = 0; i < 36; i++)
            player.getInventory()
                    .setItem(i, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COBBLESTONE, 64));
        var loot = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND, 8);
        ArrowCombatEvents.insertLoot(player.getInventory(), loot);
        h.assertTrue(loot.getCount() == 8, "full inventory cannot delete loot");
        player.getInventory()
                .setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND, 60));
        ArrowCombatEvents.insertLoot(player.getInventory(), loot);
        h.assertTrue(
                loot.getCount() == 4 && player.getInventory().getItem(0).getCount() == 64,
                "partial stack keeps the remainder for dropping at shooter");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "redesign_draw")
    public static void gargoyleSlowsBowAndCrossbowDraw(GameTestHelper h) {
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(h.absolutePos(new BlockPos(2, 20, 2)).getCenter());
        var bow = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOW);
        var crossbow = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CROSSBOW);
        player.getInventory().clearContent();
        player.getInventory()
                .setItem(
                        9,
                        ArrowRecipe.fromData(
                                data("feather", "stick", "flint", ""),
                                1,
                                h.getLevel().registryAccess()));
        int baseline = net.minecraft.world.item.CrossbowItem.getChargeDuration(crossbow, player);
        player.getInventory()
                .setItem(
                        9,
                        ArrowRecipe.fromData(
                                data("feather", "stick", "gargoyle", ""),
                                1,
                                h.getLevel().registryAccess()));
        h.assertTrue(
                net.minecraft.world.item.CrossbowItem.getChargeDuration(crossbow, player)
                        == (int) Math.ceil(baseline / .8),
                "crossbow uses 80% draw speed");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, bow);
        ((net.minecraft.world.item.BowItem) bow.getItem())
                .releaseUsing(bow, h.getLevel(), player, bow.getUseDuration(player) - 20);
        var arrows = h.getLevel()
                .getEntitiesOfClass(
                        CustomArrowEntity.class, player.getBoundingBox().inflate(3));
        h.assertTrue(
                arrows.size() == 1 && arrows.getFirst().getDeltaMovement().length() < 2.7,
                "20-tick gargoyle shot has not reached normal full draw");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "redesign_fright")
    public static void frightenReversesIntendedMovement(GameTestHelper h) {
        Pig target = pig(h, 2, 3);
        target.setNoAi(false);
        target.setSpeed(.3f);
        target.setYRot(0);
        target.setDeltaMovement(Vec3.ZERO);
        target.travel(new Vec3(0, 0, 1));
        double normal = target.getDeltaMovement().z;
        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(ArrowStatus.FRIGHTENED, 60));
        target.setDeltaMovement(Vec3.ZERO);
        target.travel(new Vec3(0, 0, 1));
        h.assertTrue(
                normal > 0 && target.getDeltaMovement().z < 0,
                "frightened entity moves opposite to the requested direction: " + normal + " -> "
                        + target.getDeltaMovement().z);
        h.succeed();
    }

    @GameTest(template = "empty", batch = "redesign_first_link", timeoutTicks = 240)
    public static void darkLinkBeginsAfterImpactAndExpires(GameTestHelper h) {
        Pig a = pig(h, 2, 3), b = pig(h, 4, 3);
        arrow(h, "feather", "dark", "flint", "").hit(a);
        near(h, a.getHealth(), 90, "initial impact damages only the struck target");
        near(h, b.getHealth(), 100, "new link does not replay first strike");
        a.invulnerableTime = 0;
        a.hurt(h.getLevel().damageSources().mobAttack(b), 20);
        near(h, b.getHealth(), 90, "later damage is shared");
        h.runAfterDelay(202, () -> {
            a.hurt(h.getLevel().damageSources().mobAttack(b), 10);
            near(h, b.getHealth(), 90, "expired link does not share");
            h.succeed();
        });
    }

    @GameTest(template = "empty", batch = "redesign_sponge")
    public static void spongeRemovesSourcesOnlyWithinTwoBlocks(GameTestHelper h) {
        h.setBlock(new BlockPos(2, 4, 2), Blocks.WATER);
        h.setBlock(new BlockPos(3, 4, 2), Blocks.WATER);
        h.setBlock(new BlockPos(6, 4, 2), Blocks.WATER);
        TestArrow arrow = arrow(h, "sun", "stick", "flint", "sponge");
        arrow.setDeltaMovement(0, 0, 2);
        arrow.tick();
        h.assertBlockPresent(Blocks.AIR, new BlockPos(2, 4, 2));
        h.assertBlockPresent(Blocks.AIR, new BlockPos(3, 4, 2));
        h.assertBlockPresent(Blocks.WATER, new BlockPos(6, 4, 2));
        h.succeed();
    }

    @GameTest(template = "empty", batch = "redesign_conditional")
    public static void timeBonusesGravityAndShieldBypassMatchTheCatalogue(GameTestHelper h) {
        long time = h.getLevel().getDayTime();
        try {
            h.getLevel().setDayTime(6000);
            near(h, ArrowProfile.read(data("raven", "stick", "flint", "")).speed(h.getLevel()), 1, "raven daytime");
            near(
                    h,
                    ArrowProfile.read(data("roadrunner", "stick", "flint", "")).speed(h.getLevel()),
                    1.6,
                    "roadrunner daytime");
            h.getLevel().setDayTime(18000);
            near(h, ArrowProfile.read(data("raven", "stick", "flint", "")).speed(h.getLevel()), 1.6, "raven night");
            near(
                    h,
                    ArrowProfile.read(data("roadrunner", "stick", "flint", "")).speed(h.getLevel()),
                    1,
                    "roadrunner night");
            near(h, ArrowProfile.read(data("wheat", "end", "flint", "")).gravity(), 1.2, "wheat gravity");
            near(h, ArrowProfile.read(data("emu", "stick", "flint", "")).gravity(), .8, "emu gravity");
            near(
                    h,
                    ArrowProfile.read(data("neutronium", "stick", "flint", "")).speed(h.getLevel()),
                    11,
                    "+1000% is eleven times base speed");
            h.assertTrue(
                    ArrowDamage.source(h.getLevel(), "arrow", null, null)
                            .is(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD),
                    "ender projectile source bypasses shields");
        } finally {
            h.getLevel().setDayTime(time);
        }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "redesign_aoe")
    public static void eldritchDamageReachesOnlyTheSixBlockPathRadius(GameTestHelper h) {
        Pig inside = pig(h, 7, 3), outside = pig(h, 10, 3);
        TestArrow arrow = arrow(h, "sun", "infinity", "eldritch", "");
        arrow.setDeltaMovement(0, 0, 3);
        arrow.tick();
        h.assertTrue(inside.getHealth() < 100, "target five blocks from path receives AoE");
        near(h, outside.getHealth(), 100, "target eight blocks from path is outside the radius");
        h.succeed();
    }

    private RedesignGameTests() {}

    private static CompoundTag data(String feather, String shaft, String tip, String extra) {
        CompoundTag data = new CompoundTag();
        data.putInt("bertieVersion", 2);
        data.putString("feather", PartCatalog.key(0, feather).itemId());
        data.putString("shaft", PartCatalog.key(1, shaft).itemId());
        data.putString("tip", PartCatalog.key(2, tip).itemId());
        if (!extra.isEmpty()) data.putString("effect", PartCatalog.key(3, extra).itemId());
        return data;
    }

    private static TestArrow arrow(GameTestHelper helper, String feather, String shaft, String tip, String extra) {
        TestArrow arrow = new TestArrow(helper);
        arrow.setCustomProperties(data(feather, shaft, tip, extra));
        arrow.setPos(helper.absolutePos(new BlockPos(2, 4, 1)).getCenter());
        arrow.setDeltaMovement(0, 0, 1);
        arrow.setBaseDamage(10);
        return arrow;
    }

    private static Pig pig(GameTestHelper helper, int x, int z) {
        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(x, 4, z));
        pig.setNoAi(true);
        pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        pig.setHealth(100);
        return pig;
    }

    private static void near(GameTestHelper h, double value, double expected, String message) {
        h.assertTrue(Math.abs(value - expected) < .001, message + ": got " + value + ", expected " + expected);
    }

    @GameTest(template = "empty", batch = "redesign_damage")
    public static void flatDamageAndFractionalArmorPiercingUseOneDamagePipeline(GameTestHelper h) {
        Pig plain = pig(h, 2, 3);
        arrow(h, "feather", "stick", "diamond", "").hit(plain);
        near(h, plain.getHealth(), 86, "+4 is added to ten raw damage");
        Pig armored = pig(h, 4, 3);
        armored.getAttribute(Attributes.ARMOR).setBaseValue(20);
        arrow(h, "feather", "stick", "diamond", "").hit(armored);
        float vanilla = net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(
                armored, 14, h.getLevel().damageSources().generic(), 20, 0);
        near(
                h,
                armored.getHealth(),
                100 - (14 - (14 - vanilla) * .8),
                "20% armor piercing reduces armor absorption, not all mitigation");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "redesign_links")
    public static void darkLinksShareRawHalfWithoutChainingBack(GameTestHelper h) {
        Pig a = pig(h, 1, 2), b = pig(h, 3, 2), c = pig(h, 5, 2);
        a.getAttribute(Attributes.ARMOR).setBaseValue(20);
        ArrowCombatEvents.link(a, b);
        ArrowCombatEvents.link(b, c);
        a.hurt(h.getLevel().damageSources().mobAttack(c), 20);
        near(h, b.getHealth(), 90, "linked target receives half of raw damage");
        near(h, c.getHealth(), 100, "shared hit never forwards down the next link");
        near(
                h,
                a.getHealth(),
                100
                        - net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(
                                a, 20, h.getLevel().damageSources().generic(), 20, 0),
                "no reflected hit returns to first target");
        Pig d = pig(h, 2, 5), e = pig(h, 4, 5), f = pig(h, 6, 5);
        ArrowCombatEvents.link(a, d);
        ArrowCombatEvents.link(a, e);
        ArrowCombatEvents.link(a, f);
        h.assertTrue(a.getPersistentData().getList("BertieArrowLinks", 10).size() == 3, "link cap remains three");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "redesign_splits")
    public static void radialSplitRequiresDamageAndChildrenCannotSplitAgain(GameTestHelper h) {
        Pig target = pig(h, 2, 3);
        TestArrow arrow = arrow(h, "stymphalian", "stick", "flint", "");
        arrow.hit(target);
        var children = h.getLevel()
                .getEntitiesOfClass(
                        CustomArrowEntity.class, target.getBoundingBox().inflate(3));
        h.assertTrue(children.size() == 8, "damaging target hit emits eight arrows");
        for (var child : children) {
            near(
                    h,
                    child.getCustomProperties().getDouble("bertieFixedDamage"),
                    5,
                    "each child stores half the pre-mitigation hit");
            near(
                    h,
                    child.getDeltaMovement().normalize().y,
                    -Math.sin(Math.PI / 18),
                    "children depart ten degrees below horizontal");
        }
        Pig immune = pig(h, 5, 3);
        immune.setInvulnerable(true);
        arrow(h, "stymphalian", "stick", "flint", "").hit(immune);
        h.assertTrue(
                h.getLevel()
                                .getEntitiesOfClass(
                                        CustomArrowEntity.class,
                                        target.getBoundingBox().inflate(8))
                                .size()
                        == 8,
                "immune target never creates a split");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "redesign_scan")
    public static void hitscanPiercesImmediatelyWithFiniteSpeedDamage(GameTestHelper h) {
        Pig a = pig(h, 2, 3), b = pig(h, 2, 5);
        TestArrow arrow = arrow(h, "resonant", "stick", "flint", "");
        arrow.tick();
        near(h, a.getHealth(), 80, "2x speed yields twenty raw damage");
        near(h, b.getHealth(), 80, "second target is hit on the same tick");
        h.assertTrue(arrow.isRemoved(), "hitscan projectile finishes immediately");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "redesign_phase")
    public static void earthDrillStopsAtOreUnlessPhasing(GameTestHelper h) {
        for (int x : new int[] {2, 5}) {
            h.setBlock(new BlockPos(x, 4, 2), Blocks.STONE);
            h.setBlock(new BlockPos(x, 4, 3), Blocks.DIAMOND_ORE);
            h.setBlock(new BlockPos(x, 4, 4), Blocks.STONE);
        }
        TestArrow regular = arrow(h, "sun", "stick", "flint", "earth");
        regular.setDeltaMovement(0, 0, 3);
        regular.tick();
        h.assertBlockPresent(Blocks.AIR, new BlockPos(2, 4, 2));
        h.assertBlockPresent(Blocks.DIAMOND_ORE, new BlockPos(2, 4, 3));
        h.assertBlockPresent(Blocks.STONE, new BlockPos(2, 4, 4));
        TestArrow phase = arrow(h, "sun", "infinity", "flint", "earth");
        phase.setPos(h.absolutePos(new BlockPos(5, 4, 1)).getCenter());
        phase.setDeltaMovement(0, 0, 3);
        phase.tick();
        h.assertBlockPresent(Blocks.DIAMOND_ORE, new BlockPos(5, 4, 3));
        h.assertBlockPresent(Blocks.AIR, new BlockPos(5, 4, 4));
        h.assertTrue(phase.getZ() > h.absolutePos(new BlockPos(5, 4, 4)).getZ(), "phasing drills past untouched ore");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "redesign_delayed", timeoutTicks = 100)
    public static void mnemonicWaitsTwoSecondsAndAddsHalfRawDamage(GameTestHelper h) {
        Pig target = pig(h, 2, 3);
        arrow(h, "feather", "stick", "mnemonic", "").hit(target);
        near(h, target.getHealth(), 90, "initial hit");
        h.runAfterDelay(39, () -> near(h, target.getHealth(), 90, "delayed hit has not fired early"));
        h.runAfterDelay(42, () -> {
            near(h, target.getHealth(), 85, "delayed half-damage follows at two seconds");
            h.succeed();
        });
    }

    @GameTest(template = "empty", batch = "redesign_ricochet")
    public static void endRodRicochetsEvenFromAnImmuneTarget(GameTestHelper h) {
        Pig immune = pig(h, 2, 3), other = pig(h, 5, 3);
        immune.setInvulnerable(true);
        TestArrow arrow = arrow(h, "feather", "end", "flint", "");
        arrow.hit(immune);
        h.assertTrue(!arrow.isRemoved(), "arrow survives immune impact for ricochet");
        near(h, ((ArrowRuntime) (Object) arrow).bertie$flight().ricochets, 1, "one ricochet consumed");
        h.assertTrue(
                arrow.getDeltaMovement()
                                .normalize()
                                .dot(other.getBoundingBox()
                                        .getCenter()
                                        .subtract(arrow.position())
                                        .normalize())
                        > .99,
                "ricochet aims at nearest other target");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "redesign_xp")
    public static void experienceBonusesMultiplyMobExperience(GameTestHelper h) {
        Pig target = pig(h, 2, 3);
        target.setHealth(5);
        TestArrow arrow = arrow(h, "feather", "experience", "flint", "brilliance");
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        arrow.setOwner(player);
        arrow.hit(target);
        var event = new LivingExperienceDropEvent(target, player, 10);
        NeoForge.EVENT_BUS.post(event);
        h.assertTrue(event.getDroppedExperience() == 30, "experience rod and refined brilliance yield 3x dropped XP");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "redesign_freeze", timeoutTicks = 80)
    public static void permafrostHoldsPositionThenExpires(GameTestHelper h) {
        Pig target = pig(h, 2, 3);
        arrow(h, "feather", "stick", "permafrost", "").hit(target);
        h.assertTrue(target.hasEffect(ArrowStatus.FROZEN), "freeze applied");
        h.runAfterDelay(3, () -> {
            Vec3 position = target.position();
            target.setDeltaMovement(1, 1, 1);
            h.runAfterDelay(
                    2, () -> near(h, target.position().distanceTo(position), 0, "frozen target remains anchored"));
        });
        h.runAfterDelay(44, () -> {
            h.assertTrue(!target.hasEffect(ArrowStatus.FROZEN), "freeze expires after two seconds");
            h.succeed();
        });
    }

    private static final class TestArrow extends CustomArrowEntity {
        TestArrow(GameTestHelper helper) {
            super(ModRegistries.CUSTOM_ARROW_ENTITY.get(), helper.getLevel());
        }

        void hit(LivingEntity target) {
            onHitEntity(new EntityHitResult(target, target.getBoundingBox().getCenter()));
        }
    }
}
