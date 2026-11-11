package io.github.bertie_mc.bertieprogression.gametest;

import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("bertieprogression")
@PrefixGameTestTemplate(false)
public final class RefinedStorageGameTests {
    @GameTest(template = "empty", batch = "paired_collision")
    public static void nativeCollisionEventsConsumeOnlyTheExactPairAndKeepTheAnvil(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        String[] elements = {"fire", "ice", "lightning"};
        String[] bases = {
            "anvilcraft:ember_metal_block",
            "anvilcraft:frost_metal_block",
            "elemental_metals:lightning_infused_netherite_block"
        };
        for (int i = 0; i < elements.length; i++) {
            var target = block("iceandfire:dragonsteel_" + elements[i] + "_block");
            var base = block(bases[i]);
            for (var anvilBlock : List.of(Blocks.ANVIL, block("anvilcraft:royal_anvil"))) {
                clearDrops(level, pos);
                level.setBlockAndUpdate(pos, target.defaultBlockState());
                level.setBlockAndUpdate(pos.below(), base.defaultBlockState());
                level.setBlockAndUpdate(pos.east(2), target.defaultBlockState());
                level.setBlockAndUpdate(pos.east(2).below(), base.defaultBlockState());
                var anvil = FallingBlockEntity.fall(level, pos.above(3), anvilBlock.defaultBlockState());
                var collision = new AnvilEvent.CollisionBlock(level, pos, anvil, 32);
                NeoForge.EVENT_BUS.post(collision);
                helper.assertTrue(
                        collision.isCanceled() && level.isEmptyBlock(pos) && level.isEmptyBlock(pos.below()),
                        "The exact two blocks must be consumed");
                helper.assertTrue(
                        anvil.isAlive() && anvil.getBlockState().is(anvilBlock), "The original anvil must remain");
                helper.assertTrue(
                        level.getBlockState(pos.east(2)).is(target)
                                && level.getBlockState(pos.east(2).below()).is(base),
                        "Nearby pairs must remain untouched");
                helper.assertTrue(
                        drops(level, pos, "iceandfire:dragonsteel_" + elements[i] + "_ingot") == 8,
                        "Each collision must yield exactly eight Ingots");
                anvil.discard();
            }
        }
        clearDrops(level, pos);
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "paired_collision")
    public static void collisionRejectsWrongSupportsAndLowSpeed(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var target = block("iceandfire:dragonsteel_fire_block");
        var base = block("anvilcraft:ember_metal_block");
        for (boolean wrongBase : List.of(true, false)) {
            clearDrops(level, pos);
            level.setBlockAndUpdate(pos, target.defaultBlockState());
            level.setBlockAndUpdate(
                    pos.below(), wrongBase ? Blocks.STONE.defaultBlockState() : base.defaultBlockState());
            var anvil = FallingBlockEntity.fall(level, pos.above(3), Blocks.ANVIL.defaultBlockState());
            NeoForge.EVENT_BUS.post(new AnvilEvent.CollisionBlock(level, pos, anvil, wrongBase ? 32 : 31.99));
            helper.assertTrue(
                    level.getBlockState(pos).is(target) && !level.isEmptyBlock(pos.below()) && anvil.isAlive(),
                    "An invalid pair or speed must consume nothing");
            helper.assertTrue(
                    drops(level, pos, "iceandfire:dragonsteel_fire_ingot") == 0,
                    "Invalid collisions must not produce Ingots");
            anvil.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "paired_collision")
    public static void horizontalAnvilMovementTriggersThePairedRecipe(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(3, 2, 2));
        clearDrops(level, pos);
        level.setBlockAndUpdate(pos, block("iceandfire:dragonsteel_fire_block").defaultBlockState());
        level.setBlockAndUpdate(
                pos.below(), block("anvilcraft:ember_metal_block").defaultBlockState());
        var anvil = FallingBlockEntity.fall(level, pos.west(2), Blocks.ANVIL.defaultBlockState());
        anvil.setDeltaMovement(new Vec3(32, 0, 0));
        anvil.move(MoverType.SELF, new Vec3(32, 0, 0));
        helper.assertTrue(
                level.isEmptyBlock(pos) && level.isEmptyBlock(pos.below()) && anvil.isAlive(),
                "The native moving-anvil event must consume the pair and retain the anvil");
        helper.assertTrue(
                drops(level, pos, "iceandfire:dragonsteel_fire_ingot") == 8,
                "The native movement path must output eight Ingots");
        anvil.discard();
        clearDrops(level, pos);
        helper.succeed();
    }

    private static net.minecraft.world.level.block.Block block(String id) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
    }

    private static ItemStack stack(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    private static long drops(ServerLevel level, BlockPos pos, String item) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3)).stream()
                .filter(entity -> entity.getItem().is(stack(item).getItem()))
                .mapToLong(entity -> entity.getItem().getCount())
                .sum();
    }

    private static void clearDrops(ServerLevel level, BlockPos pos) {
        level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3)).forEach(ItemEntity::discard);
    }

    private RefinedStorageGameTests() {}
}
