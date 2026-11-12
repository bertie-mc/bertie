package io.github.bertie_mc.bertieprogression.gametest;

import io.github.bertie_mc.bertieprogression.anvil.OverchargedCrystalCrafting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("bertieprogression_crystal")
@PrefixGameTestTemplate(false)
public final class CrystalCraftingGameTests {
    @GameTest(template = "empty")
    public static void crystalStructureIsAtomicAndDropsWithoutOvercompressor(GameTestHelper helper) {
        var level = helper.getLevel();
        var table = helper.absolutePos(new BlockPos(3, 5, 3));
        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++)
                level.setBlockAndUpdate(table.offset(x, 0, z), Blocks.CRAFTING_TABLE.defaultBlockState());
        var corner = table.offset(-1, -3, -1);
        for (int y = 0; y < 3; y++)
            for (int z = 0; z < 3; z++)
                for (int x = 0; x < 3; x++) {
                    String id = x == 1 && y == 1 && z == 1
                            ? "cognition:cognitive_crystal_block"
                            : x != 1 && y != 1 && z != 1 ? "magitech:fluorite_block" : "oritech:superconductor";
                    level.setBlockAndUpdate(
                            corner.offset(x, y, z),
                            BuiltInRegistries.BLOCK
                                    .get(ResourceLocation.parse(id))
                                    .defaultBlockState());
                }
        level.setBlockAndUpdate(corner, Blocks.STONE.defaultBlockState());
        helper.assertTrue(
                !OverchargedCrystalCrafting.tryCraft(level, table), "A wrong corner must reject the whole structure");
        helper.assertTrue(!level.isEmptyBlock(corner.offset(1, 1, 1)), "Rejection must consume nothing");
        level.setBlockAndUpdate(
                corner,
                BuiltInRegistries.BLOCK
                        .get(ResourceLocation.parse("magitech:fluorite_block"))
                        .defaultBlockState());
        helper.assertTrue(
                OverchargedCrystalCrafting.tryCraft(level, table),
                "Regular Crafting Tables must support a direct item result");
        helper.assertTrue(
                !OverchargedCrystalCrafting.tryCraft(level, table), "The consumed structure must not yield twice");
        for (int y = 0; y < 3; y++)
            for (int z = 0; z < 3; z++)
                for (int x = 0; x < 3; x++)
                    helper.assertTrue(
                            level.isEmptyBlock(corner.offset(x, y, z)), "All 27 ingredients must be consumed");
        long crystals = level.getEntitiesOfClass(ItemEntity.class, new AABB(table).inflate(5)).stream()
                .filter(entity ->
                        entity.getItem().is(item("oritech:overcharged_crystal").getItem()))
                .mapToLong(entity -> entity.getItem().getCount())
                .sum();
        helper.assertTrue(crystals == 1, "The structure must drop one crystal item");
        helper.assertTrue(level.getBlockState(table).is(Blocks.CRAFTING_TABLE), "The crafting-table layer must remain");
        helper.succeed();
    }

    private static ItemStack item(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }
}
