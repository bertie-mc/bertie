package io.github.bertie_mc.bertieprogression.anvil;

import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockInput;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockRecipe;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.Tags;

public final class OverchargedCrystalCrafting {
    public static final ResourceLocation RECIPE =
            ResourceLocation.parse("bertieprogression:multiblock/overcharged_crystal");
    private static final ResourceLocation COMPRESSOR = ResourceLocation.parse("anvilcraft:space_overcompressor");

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void landed(AnvilEvent.GiantOnLand event) {
        if (event.getLevel() instanceof ServerLevel level
                && tryCraft(level, event.getPos().below(2))) {
            event.setAnvilDamage(false);
        }
    }

    public static boolean tryCraft(ServerLevel level, BlockPos tableCenter) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockState state = level.getBlockState(tableCenter.offset(x, 0, z));
                if (!state.is(Tags.Blocks.PLAYER_WORKSTATIONS_CRAFTING_TABLES)
                        && !(x == 0 && z == 0 && COMPRESSOR.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock())))) {
                    return false;
                }
            }
        }
        var holder = level.getRecipeManager().byKey(RECIPE);
        if (holder.isEmpty() || !(holder.get().value() instanceof MultiblockRecipe recipe)) return false;
        BlockPos corner = tableCenter.offset(-1, -3, -1);
        List<List<List<BlockState>>> layers = new ArrayList<>();
        for (int y = 0; y < 3; y++) {
            List<List<BlockState>> rows = new ArrayList<>();
            for (int z = 0; z < 3; z++) {
                List<BlockState> row = new ArrayList<>();
                for (int x = 0; x < 3; x++) row.add(level.getBlockState(corner.offset(x, y, z)));
                rows.add(row);
            }
            layers.add(rows);
        }
        if (!recipe.matches(new MultiblockInput(layers, 3), level)) return false;
        var result = recipe.getResult().copy();
        if (result.isEmpty()) return false;
        for (int y = 0; y < 3; y++) {
            for (int z = 0; z < 3; z++) {
                for (int x = 0; x < 3; x++) {
                    level.setBlockAndUpdate(corner.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
        Containers.dropItemStack(
                level, tableCenter.getX() + 0.5, tableCenter.getY() - 1.5, tableCenter.getZ() + 0.5, result);
        return true;
    }

    private OverchargedCrystalCrafting() {}
}
