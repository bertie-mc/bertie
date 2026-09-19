package io.github.bertie_mc.betterhorses.gametest;

import io.github.bertie_mc.betterhorses.*;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(BetterHorses.ID)
@PrefixGameTestTemplate(false)
public final class HorseGameTests {
    private HorseGameTests() {}

    private static Horse horse(GameTestHelper helper) {
        Horse horse = helper.spawn(EntityType.HORSE, new BlockPos(1, 2, 1));
        horse.setTamed(true);
        horse.setNoAi(true);
        horse.setAge(0);
        horse.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40);
        horse.setHealth(40);
        return horse;
    }

    @GameTest(template = "empty")
    public static void shoesSaveAndRemoveWithoutStacking(GameTestHelper helper) {
        Horse horse = horse(helper);
        HorseEquipment eq = (HorseEquipment) horse;
        double speed = horse.getAttributeValue(Attributes.MOVEMENT_SPEED),
                step = horse.getAttributeValue(Attributes.STEP_HEIGHT),
                fall = horse.getAttributeValue(Attributes.SAFE_FALL_DISTANCE);
        for (int i = 0; i < 3; i++) eq.betterhorses$shoes().setItem(0, BetterHorses.NETHERITE.toStack());
        helper.assertTrue(
                Math.abs(horse.getAttributeValue(Attributes.MOVEMENT_SPEED)
                                - speed
                                - 4 / HorsePhysics.BLOCKS_PER_SECOND_PER_ATTRIBUTE)
                        < 1e-8,
                "speed modifier must not stack");
        helper.assertTrue(horse.getAttributeValue(Attributes.STEP_HEIGHT) == step + 2, "step bonus");
        helper.assertTrue(horse.getAttributeValue(Attributes.SAFE_FALL_DISTANCE) == fall + 16, "safe fall bonus");
        eq.betterhorses$saddleInventory().setItem(0, BetterHorses.PASSENGER.toStack());
        horse.setBodyArmorItem(BetterHorses.ARMOR.toStack());
        CompoundTag saved = new CompoundTag();
        horse.save(saved);
        Horse loaded = EntityType.HORSE.create(helper.getLevel());
        loaded.load(saved);
        helper.assertTrue(((HorseEquipment) loaded).betterhorses$tier() == ShoeTier.NETHERITE, "shoe must survive NBT");
        helper.assertTrue(
                ((HorseEquipment) loaded).betterhorses$syncedSaddle().is(BetterHorses.PASSENGER.get())
                        && loaded.isSaddled(),
                "custom saddle must survive NBT");
        helper.assertTrue(loaded.getBodyArmorItem().is(BetterHorses.ARMOR.get()), "armor must survive NBT");
        eq.betterhorses$shoes().setItem(0, ItemStack.EMPTY);
        helper.assertTrue(
                horse.getAttributeValue(Attributes.MOVEMENT_SPEED) == speed
                        && horse.getAttributeValue(Attributes.STEP_HEIGHT) == step
                        && horse.getAttributeValue(Attributes.SAFE_FALL_DISTANCE) == fall,
                "removal must restore original attributes");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void effigyRoundTripPreservesHorseAndEquipment(GameTestHelper helper) {
        Horse horse = horse(helper);
        UUID id = horse.getUUID(), owner = UUID.randomUUID();
        horse.setOwnerUUID(owner);
        horse.setCustomName(Component.literal("Bucephalus"));
        horse.setVariant(Variant.BLACK);
        horse.setHealth(23);
        ((HorseEquipment) horse).betterhorses$shoes().setItem(0, BetterHorses.DIAMOND.toStack());
        ((HorseEquipment) horse).betterhorses$saddleInventory().setItem(0, BetterHorses.WANDERER.toStack());
        ((HorseEquipment) horse).betterhorses$storage().setItem(14, new ItemStack(Items.DIAMOND, 7));
        horse.setBodyArmorItem(BetterHorses.ARMOR.toStack());
        ItemStack effigy = BetterHorses.EFFIGY.toStack();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(
                BetterHorses.EFFIGY
                        .get()
                        .interactLivingEntity(effigy, player, horse, InteractionHand.MAIN_HAND)
                        .consumesAction(),
                "capture succeeds");
        helper.assertTrue(
                horse.isRemoved() && HorseEffigyItem.occupied(effigy), "capture atomically stores and removes horse");
        helper.runAfterDelay(2, () -> {
            BlockPos target = helper.absolutePos(new BlockPos(2, 3, 2));
            helper.assertTrue(
                    HorseEffigyItem.release(effigy, helper.getLevel(), Vec3.atBottomCenterOf(target), 90),
                    "release succeeds");
            Horse restored = (Horse) helper.getLevel().getEntity(id);
            helper.assertTrue(
                    restored != null
                            && restored.getOwnerUUID().equals(owner)
                            && restored.getHealth() == 23
                            && restored.getVariant() == Variant.BLACK
                            && restored.getName().getString().equals("Bucephalus"),
                    "identity and stats preserved");
            helper.assertTrue(
                    ((HorseEquipment) restored).betterhorses$tier() == ShoeTier.DIAMOND
                            && ((HorseEquipment) restored)
                                    .betterhorses$syncedSaddle()
                                    .is(BetterHorses.WANDERER.get())
                            && restored.getBodyArmorItem().is(BetterHorses.ARMOR.get()),
                    "all equipment preserved");
            helper.assertTrue(
                    ((HorseEquipment) restored)
                                            .betterhorses$storage()
                                            .getItem(14)
                                            .getCount()
                                    == 7
                            && ((HorseEquipment) restored)
                                    .betterhorses$storage()
                                    .getItem(0)
                                    .isEmpty(),
                    "effigy preserves cargo and its exact slot");
            helper.assertFalse(HorseEffigyItem.occupied(effigy), "effigy empties after spawn");
            helper.assertFalse(
                    HorseEffigyItem.release(effigy, helper.getLevel(), Vec3.atBottomCenterOf(target), 90),
                    "second release cannot duplicate horse");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void blockedReleaseAndOccupiedCaptureKeepHorseSafe(GameTestHelper helper) {
        Horse horse = horse(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack effigy = BetterHorses.EFFIGY.toStack();
        player.startRiding(horse);
        helper.assertTrue(
                BetterHorses.EFFIGY.get().interactLivingEntity(effigy, player, horse, InteractionHand.MAIN_HAND)
                        == InteractionResult.FAIL,
                "cannot capture riders");
        player.stopRiding();
        BetterHorses.EFFIGY.get().interactLivingEntity(effigy, player, horse, InteractionHand.MAIN_HAND);
        helper.runAfterDelay(2, () -> {
            BlockPos target = helper.absolutePos(new BlockPos(1, 2, 1));
            helper.getLevel().setBlockAndUpdate(target, Blocks.STONE.defaultBlockState());
            helper.assertFalse(
                    HorseEffigyItem.release(effigy, helper.getLevel(), Vec3.atLowerCornerOf(target), 0),
                    "cannot release into solid block");
            helper.assertTrue(HorseEffigyItem.occupied(effigy), "failed release retains horse");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void passengerSaddleSeatsTwoAndMenuProtectsIt(GameTestHelper helper) {
        Horse horse = horse(helper);
        HorseEquipment eq = (HorseEquipment) horse;
        eq.betterhorses$saddleInventory().setItem(0, BetterHorses.PASSENGER.toStack());
        Player first = helper.makeMockPlayer(GameType.SURVIVAL),
                second = helper.makeMockPlayer(GameType.SURVIVAL),
                third = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(first.startRiding(horse) && second.startRiding(horse), "both riders can mount");
        helper.assertFalse(third.startRiding(horse), "third rider rejected");
        helper.assertTrue(horse.getControllingPassenger() == first, "front rider steers");
        helper.assertTrue(
                horse.getPassengerRidingPosition(first).distanceTo(horse.getPassengerRidingPosition(second)) > 0.7,
                "seats must not overlap");
        BetterHorseMenu menu = new BetterHorseMenu(1, first.getInventory(), horse);
        helper.assertTrue(
                menu.slots.size() == 54
                        && !menu.getSlot(BetterHorseMenu.STORAGE_START).isActive(),
                "storage slots are inactive for the passenger saddle");
        helper.assertFalse(menu.getSlot(0).mayPickup(first), "cannot remove occupied double saddle");
        helper.assertFalse(menu.getSlot(2).mayPlace(new ItemStack(Items.DIRT)), "shoe slot rejects other items");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void quickMoveShoesEquipsAndUnequips(GameTestHelper helper) {
        Horse horse = horse(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(9, BetterHorses.GOLD.toStack());
        BetterHorseMenu menu = new BetterHorseMenu(1, player.getInventory(), horse);
        menu.quickMoveStack(player, 3);
        helper.assertTrue(
                ((HorseEquipment) horse).betterhorses$tier() == ShoeTier.GOLD
                        && player.getInventory().getItem(9).isEmpty(),
                "shift click equips shoes");
        menu.quickMoveStack(player, 2);
        helper.assertTrue(
                ((HorseEquipment) horse).betterhorses$tier() == ShoeTier.NONE, "shift click removes shoe effects");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void netheriteProtectsContactHeatButNotFire(GameTestHelper helper) {
        Horse horse = horse(helper);
        ((HorseEquipment) horse).betterhorses$shoes().setItem(0, BetterHorses.NETHERITE.toStack());
        helper.assertFalse(horse.hurt(horse.damageSources().hotFloor(), 2), "magma contact protected");
        helper.assertFalse(horse.hurt(horse.damageSources().campfire(), 2), "campfire contact protected");
        helper.assertTrue(horse.hurt(horse.damageSources().inFire(), 2), "actual fire still hurts");
        horse.invulnerableTime = 0;
        helper.assertTrue(horse.hurt(horse.damageSources().onFire(), 2), "burning still hurts");
        helper.assertTrue(
                horse.canStandOnFluid(Fluids.WATER.defaultFluidState())
                        && horse.canStandOnFluid(Fluids.LAVA.defaultFluidState()),
                "netherite supports both fluids");
        ((HorseEquipment) horse).betterhorses$shoes().setItem(0, BetterHorses.DIAMOND.toStack());
        helper.assertTrue(
                horse.canStandOnFluid(Fluids.WATER.defaultFluidState())
                        && !horse.canStandOnFluid(Fluids.LAVA.defaultFluidState()),
                "diamond only supports water");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void warriorTransfersDamageAndUnriddenHorseTakesAll(GameTestHelper helper) {
        Horse horse = horse(helper);
        ((HorseEquipment) horse).betterhorses$saddleInventory().setItem(0, BetterHorses.WARRIOR.toStack());
        Player rider = helper.makeMockPlayer(GameType.SURVIVAL);
        rider.startRiding(horse);
        float before = rider.getHealth();
        horse.hurt(horse.damageSources().generic(), 10);
        helper.assertTrue(
                Math.abs(horse.getHealth() - 38) < 0.01 && Math.abs(rider.getHealth() - (before - 8)) < 0.01,
                "80 percent transferred");
        rider.stopRiding();
        horse.invulnerableTime = 0;
        horse.hurt(horse.damageSources().generic(), 10);
        helper.assertTrue(Math.abs(horse.getHealth() - 28) < 0.01, "unridden horse takes full damage");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void diamondHorseLandsOnWaterSurface(GameTestHelper helper) {
        surfaceTest(helper, false);
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void netheriteHorseLandsOnLavaWithoutBurning(GameTestHelper helper) {
        surfaceTest(helper, true);
    }

    private static void surfaceTest(GameTestHelper helper, boolean lava) {
        for (int x = 3; x < 9; x++)
            for (int z = 3; z < 9; z++) helper.setBlock(new BlockPos(x, 1, z), lava ? Blocks.LAVA : Blocks.WATER);
        Horse horse = horse(helper);
        horse.setNoAi(false);
        horse.setNoGravity(false);
        ((HorseEquipment) horse)
                .betterhorses$shoes()
                .setItem(0, lava ? BetterHorses.NETHERITE.toStack() : BetterHorses.DIAMOND.toStack());
        BlockPos center = helper.absolutePos(new BlockPos(5, 4, 5));
        horse.setPos(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
        helper.runAfterDelay(40, () -> {
            double surface = helper.absolutePos(new BlockPos(5, 1, 5)).getY() + 8.0 / 9;
            helper.assertTrue(
                    horse.onGround() && Math.abs(horse.getY() - surface) < 0.05,
                    "horse must stand on the fluid surface: y=" + horse.getY() + ", expected=" + surface + ", grounded="
                            + horse.onGround() + ", velocity=" + horse.getDeltaMovement() + ", health="
                            + horse.getHealth());
            helper.assertTrue(
                    horse.getHealth() == 40 && !horse.isOnFire(), "surface walking must not damage or ignite horse");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void realRiddenJumpGetsExtraHeight(GameTestHelper helper) {
        JumpHorse horse = new JumpHorse(helper.getLevel());
        double power = horse.getAttributeValue(Attributes.JUMP_STRENGTH);
        ((HorseEquipment) horse).betterhorses$shoes().setItem(0, BetterHorses.NETHERITE.toStack());
        horse.jump();
        double height = HorsePhysics.apex(horse.getDeltaMovement().y, 0.08);
        helper.assertTrue(
                Math.abs(height - HorsePhysics.apex(power, 0.08) - 3) < 0.01,
                "actual horse jump receives three blocks, not three velocity units");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void travellerMenuMovesCargoAndProtectsSaddle(GameTestHelper helper) {
        Horse horse = horse(helper);
        HorseEquipment eq = (HorseEquipment) horse;
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BetterHorseMenu menu = new BetterHorseMenu(1, player.getInventory(), horse);
        helper.assertFalse(menu.hasStorage(), "ordinary horse has no active storage");
        player.getInventory().setItem(9, BetterHorses.WANDERER.toStack());
        menu.quickMoveStack(player, 3);
        helper.assertTrue(menu.hasStorage(), "equipping traveller enables storage without reopening");
        for (int i = BetterHorseMenu.STORAGE_START; i < BetterHorseMenu.STORAGE_END; i++)
            helper.assertTrue(menu.getSlot(i).isActive(), "all fifteen slots enabled");
        player.getInventory().setItem(9, new ItemStack(Items.APPLE, 32));
        menu.quickMoveStack(player, 3);
        player.getInventory().setItem(9, new ItemStack(Items.APPLE, 50));
        menu.quickMoveStack(player, 3);
        helper.assertTrue(
                eq.betterhorses$storage().getItem(0).getCount() == 64
                        && eq.betterhorses$storage().getItem(1).getCount() == 18,
                "cargo merges and overflows correctly");
        helper.assertFalse(menu.getSlot(0).mayPickup(player), "loaded saddle cannot be removed");
        helper.assertFalse(
                menu.getSlot(0).mayPlace(BetterHorses.WARRIOR.toStack()), "loaded saddle cannot be replaced");
        menu.clicked(0, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().isEmpty() && menu.hasStorage(), "normal click cannot bypass cargo lock");
        helper.assertTrue(menu.quickMoveStack(player, 0).isEmpty(), "shift click cannot bypass cargo lock");
        menu.quickMoveStack(player, BetterHorseMenu.STORAGE_START);
        menu.quickMoveStack(player, BetterHorseMenu.STORAGE_START + 1);
        helper.assertTrue(
                eq.betterhorses$storage().isEmpty() && player.getInventory().countItem(Items.APPLE) == 82,
                "taking cargo conserves every item");
        menu.quickMoveStack(player, 0);
        helper.assertFalse(menu.hasStorage(), "empty saddle can be removed");
        helper.assertFalse(
                menu.getSlot(BetterHorseMenu.STORAGE_START).mayPlace(new ItemStack(Items.APPLE)),
                "inactive slots reject items on the server");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void travellerCargoSurvivesReloadAndDropsOnDeath(GameTestHelper helper) {
        Horse horse = horse(helper);
        HorseEquipment eq = (HorseEquipment) horse;
        eq.betterhorses$saddleInventory().setItem(0, BetterHorses.WANDERER.toStack());
        ItemStack cargo = new ItemStack(Items.DIAMOND, 7);
        cargo.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("Travel fund"));
        eq.betterhorses$storage().setItem(14, cargo);
        // Commands or another mod can bypass menu restrictions; cargo must survive that too.
        eq.betterhorses$saddleInventory().setItem(0, ItemStack.EMPTY);
        CompoundTag saved = new CompoundTag();
        horse.save(saved);
        Horse loaded = EntityType.HORSE.create(helper.getLevel());
        loaded.load(saved);
        HorseEquipment restored = (HorseEquipment) loaded;
        helper.assertTrue(
                restored.betterhorses$storage().getItem(0).isEmpty()
                        && ItemStack.matches(
                                cargo, restored.betterhorses$storage().getItem(14)),
                "slot, count and components survive reload even with a forcibly removed saddle");
        eq.betterhorses$saddleInventory().setItem(0, BetterHorses.WANDERER.toStack());
        horse.hurt(horse.damageSources().genericKill(), Float.MAX_VALUE);
        helper.runAfterDelay(2, () -> {
            int diamonds = helper
                    .getLevel()
                    .getEntitiesOfClass(
                            net.minecraft.world.entity.item.ItemEntity.class,
                            horse.getBoundingBox().inflate(3))
                    .stream()
                    .filter(item -> item.getItem().is(Items.DIAMOND))
                    .mapToInt(item -> item.getItem().getCount())
                    .sum();
            helper.assertTrue(diamonds == 7 && eq.betterhorses$storage().isEmpty(), "death drops cargo exactly once");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void diamondWalksOnPowderSnow(GameTestHelper helper) {
        powderSnowTest(helper, BetterHorses.DIAMOND.toStack());
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void netheriteWalksOnPowderSnow(GameTestHelper helper) {
        powderSnowTest(helper, BetterHorses.NETHERITE.toStack());
    }

    private static void powderSnowTest(GameTestHelper helper, ItemStack shoes) {
        for (int x = 3; x < 9; x++)
            for (int z = 3; z < 9; z++) helper.setBlock(new BlockPos(x, 1, z), Blocks.POWDER_SNOW);
        Horse horse = horse(helper);
        horse.setNoAi(false);
        HorseEquipment eq = (HorseEquipment) horse;
        eq.betterhorses$shoes().setItem(0, shoes);
        BlockPos center = helper.absolutePos(new BlockPos(5, 6, 5));
        horse.setPos(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
        helper.assertTrue(
                net.minecraft.world.level.block.PowderSnowBlock.canEntityWalkOnPowderSnow(horse),
                "waterwalking tier supports powder snow");
        helper.runAfterDelay(40, () -> {
            double surface = helper.absolutePos(new BlockPos(5, 2, 5)).getY();
            helper.assertTrue(
                    horse.onGround() && Math.abs(horse.getY() - surface) < 0.05,
                    "horse lands on the full snow surface after a long fall: " + horse.getY());
            helper.assertTrue(horse.getTicksFrozen() == 0, "standing on snow does not freeze horse");
            eq.betterhorses$shoes().setItem(0, BetterHorses.GOLD.toStack());
            helper.assertFalse(
                    net.minecraft.world.level.block.PowderSnowBlock.canEntityWalkOnPowderSnow(horse),
                    "gold does not grant powder snow walking");
            helper.runAfterDelay(15, () -> {
                helper.assertTrue(horse.getY() < surface - 0.2, "removing waterwalking restores sinking");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty")
    public static void travellerAlwaysUsesFullJumpWithShoeBonus(GameTestHelper helper) {
        JumpHorse horse = new JumpHorse(helper.getLevel());
        HorseEquipment eq = (HorseEquipment) horse;
        eq.betterhorses$saddleInventory().setItem(0, new ItemStack(Items.SADDLE));
        horse.chargedJump(0);
        double weak = horse.getDeltaMovement().y;
        horse.chargedJump(100);
        double full = horse.getDeltaMovement().y;
        helper.assertTrue(weak < full, "ordinary saddle keeps vanilla charge behavior");
        eq.betterhorses$saddleInventory().setItem(0, BetterHorses.WANDERER.toStack());
        horse.chargedJump(0);
        helper.assertTrue(Math.abs(horse.getDeltaMovement().y - full) < 1e-8, "traveller tap gives full power");
        eq.betterhorses$shoes().setItem(0, BetterHorses.DIAMOND.toStack());
        horse.chargedJump(0);
        helper.assertTrue(
                Math.abs(HorsePhysics.apex(horse.getDeltaMovement().y, 0.08) - HorsePhysics.apex(full, 0.08) - 2)
                        < 0.01,
                "full jump includes the horseshoe bonus");
        helper.succeed();
    }

    private static class JumpHorse extends Horse {
        JumpHorse(net.minecraft.world.level.Level level) {
            super(EntityType.HORSE, level);
        }

        void jump() {
            executeRidersJump(1, Vec3.ZERO);
        }

        void chargedJump(int charge) {
            onPlayerJump(charge);
            executeRidersJump(playerJumpPendingScale, Vec3.ZERO);
        }
    }
}
