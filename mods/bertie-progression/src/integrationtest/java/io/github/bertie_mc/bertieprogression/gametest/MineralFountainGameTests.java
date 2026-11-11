package io.github.bertie_mc.bertieprogression.gametest;

import com.mojang.authlib.GameProfile;
import io.github.bertie_mc.bertieprogression.fountain.FountainOrePool;
import io.github.bertie_mc.bertieprogression.fountain.MineralFountainRules;
import io.github.bertie_mc.bertieprogression.fountain.SturdyDeepslateRules;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("bertieprogression")
@PrefixGameTestTemplate(false)
public final class MineralFountainGameTests {
    @GameTest(template = "empty", batch = "mineral_fountain", timeoutTicks = 100)
    public static void nativeFountainDefaultsToSturdyWithoutBoosters(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2)).atY(level.getMinBuildHeight() + 1);
        level.setBlockAndUpdate(
                pos,
                BuiltInRegistries.BLOCK
                        .get(ResourceLocation.parse("anvilcraft:mineral_fountain"))
                        .defaultBlockState());
        for (var direction : List.of(
                net.minecraft.core.Direction.NORTH,
                net.minecraft.core.Direction.SOUTH,
                net.minecraft.core.Direction.EAST,
                net.minecraft.core.Direction.WEST)) {
            level.setBlockAndUpdate(pos.relative(direction), Blocks.STONE.defaultBlockState());
        }
        level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
        var entity = level.getBlockEntity(pos);
        helper.assertTrue(entity != null, "native Fountain block entity must register");
        entity.getClass().getMethod("resetTickCount").invoke(entity);
        helper.runAtTickTime(25, () -> {
            helper.assertTrue(
                    level.getBlockState(pos.above()).is(BuiltInRegistries.BLOCK.get(SturdyDeepslateRules.ID)),
                    "native Fountain tick must produce Sturdy Deepslate instead of Cinerite");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", batch = "mineral_fountain")
    public static void deepslateCascadeWorksWithoutFourRawStorageBlocks(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2)).atY(level.getMinBuildHeight() + 1);
        var neighbors = List.of(
                Blocks.STONE.defaultBlockState(),
                Blocks.STONE.defaultBlockState(),
                Blocks.STONE.defaultBlockState(),
                Blocks.STONE.defaultBlockState());
        helper.assertTrue(
                FountainOrePool.build(level).size() > 0, "Overworld pool must discover registered ore features");
        boolean foundOre = false;
        for (int seed = 0; seed < 50; seed++) {
            level.setBlockAndUpdate(pos.above(), Blocks.DEEPSLATE.defaultBlockState());
            level.random.setSeed(seed);
            helper.assertTrue(
                    MineralFountainRules.process(level, pos, neighbors), "ordinary Deepslate must be accepted");
            var result = level.getBlockState(pos.above());
            helper.assertTrue(!result.is(Blocks.DEEPSLATE), "every operation must resolve a result");
            if (result.is(Tags.Blocks.ORES)
                    && !BuiltInRegistries.BLOCK
                            .getKey(result.getBlock())
                            .getPath()
                            .contains("earth_core")) {
                foundOre = true;
                break;
            }
        }
        helper.assertTrue(foundOre, "the ordinary ore branch must work without matching storage-block neighbours");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "mineral_fountain")
    public static void sturdyHarvestRequiresNetheriteAndDropsTheBlock(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var sturdy = BuiltInRegistries.BLOCK.get(SturdyDeepslateRules.ID);
        var player = FakePlayerFactory.get(
                level, new GameProfile(UUID.fromString("684d3db8-81dd-4de3-b38b-57fa538f2c4d"), "fountain_miner"));
        player.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
        player.setOnGround(true);
        player.removeAllEffects();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_PICKAXE));
        float unbuffed = sturdy.defaultBlockState().getDestroyProgress(player, level, pos);
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 100, 1));
        float reference = Blocks.REINFORCED_DEEPSLATE.defaultBlockState().getDestroyProgress(player, level, pos);
        // Optional mods can tag Reinforced Deepslate as pickaxe-mineable; retain the native speed-one reference.
        reference /= player.getMainHandItem().getDestroySpeed(Blocks.REINFORCED_DEEPSLATE.defaultBlockState());
        helper.assertTrue(
                Math.abs(unbuffed - reference) < 0.00000001,
                "unbuffed Netherite mining must match the native Haste II reference: " + unbuffed + " vs " + reference);
        player.removeAllEffects();
        for (var tool : List.of(Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE)) {
            level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3)).forEach(ItemEntity::discard);
            level.setBlockAndUpdate(pos, sturdy.defaultBlockState());
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(tool));
            helper.assertTrue(player.gameMode.destroyBlock(pos), "the survival mining path must break the block");
            boolean dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3)).stream()
                    .anyMatch(item -> item.getItem().is(sturdy.asItem()));
            helper.assertTrue(
                    dropped == (tool == Items.NETHERITE_PICKAXE), "only Netherite must yield Sturdy Deepslate");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "mineral_fountain")
    public static void nativeSpecialChancesAndFlatBlockBonusesArePreserved(GameTestHelper helper) {
        var stone = Blocks.STONE.defaultBlockState();
        var base = MineralFountainRules.probabilities(
                helper.getLevel(), Blocks.DEEPSLATE.defaultBlockState(), List.of(stone, stone, stone, stone));
        helper.assertTrue(
                Math.abs(base.core() - 0.01) < 0.000001 && Math.abs(base.voidChance() - 0.01) < 0.000001,
                "the effective native Overworld chance recipes must supply both 1% baselines");
        var core = MineralFountainRules.block(ResourceLocation.parse("anvilcraft:earth_core_shard_block"));
        var voidBlock = MineralFountainRules.block(ResourceLocation.parse("anvilcraft:void_matter_block"));
        var boosted = MineralFountainRules.probabilities(
                helper.getLevel(), Blocks.DEEPSLATE.defaultBlockState(), List.of(core, core, voidBlock, stone));
        helper.assertTrue(
                Math.abs(boosted.core() - 0.07) < 0.000001 && Math.abs(boosted.voidChance() - 0.04) < 0.000001,
                "storage boosters must add three percentage points each");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "mineral_fountain")
    public static void dimensionPoolsDoNotMixOverworldAndNetherOres(GameTestHelper helper) {
        var overworld = FountainOrePool.build(helper.getLevel()).outputs();
        var nether = FountainOrePool.build(
                        helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER))
                .outputs();
        helper.assertTrue(
                overworld.contains(ResourceLocation.parse("minecraft:deepslate_diamond_ore")),
                "Overworld pool must include Diamond");
        helper.assertTrue(
                !overworld.contains(ResourceLocation.parse("minecraft:ancient_debris")),
                "Overworld pool must not include Nether-only Ancient Debris");
        helper.assertTrue(
                nether.contains(ResourceLocation.parse("minecraft:ancient_debris")),
                "Nether pool must include Ancient Debris");
        helper.assertTrue(
                !nether.contains(ResourceLocation.parse("minecraft:deepslate_diamond_ore")),
                "Nether pool must not include Overworld-only Diamond");
        helper.assertTrue(
                !overworld.contains(ResourceLocation.parse("anvilcraft:earth_core_shard_ore")),
                "rare outputs must not leak into ordinary ore pool");
        helper.succeed();
    }

    private MineralFountainGameTests() {}
}
