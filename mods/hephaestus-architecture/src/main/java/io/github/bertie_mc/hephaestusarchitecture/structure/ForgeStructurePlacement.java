package io.github.bertie_mc.hephaestusarchitecture.structure;

import com.stal111.forbidden_arcanus.common.block.HephaestusForgeBlock;
import com.stal111.forbidden_arcanus.common.block.entity.forge.HephaestusForgeLevel;
import com.stal111.forbidden_arcanus.core.init.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Builds a complete, immediately valid Hephaestus Forge layout for one tier.
 *
 * <p>Placement mirrors validation: a tier with a loaded structure template gets that template's
 * exact requirements (pedestal markers become plain Darkstone Pedestals), every other tier gets
 * Forbidden &amp; Arcanus' native 9x9 base with the eight native pedestal positions. The layout's
 * bounding box is cleared first, so placing over an older tier leaves no requirement-breaking
 * leftovers inside the new footprint.</p>
 */
public final class ForgeStructurePlacement {

    /**
     * The native base plate one block below the forge, transcribed from F&amp;A's
     * {@code ModBlockPatterns.BASE_HEPHAESTUS_PATTERN}. P = Polished Darkstone, A = Gilded
     * Chiseled Polished Darkstone, C = Chiseled Arcane Polished Darkstone, * = any (left alone).
     */
    static final String[] NATIVE_BASE = {
        "***PPP***",
        "*PPPAPPP*",
        "*PAPPPAP*",
        "PPPPCPPPP",
        "PAPCACPAP",
        "PPPPCPPPP",
        "*PAPPPAP*",
        "*PPPAPPP*",
        "***PPP***"
    };

    private ForgeStructurePlacement() {}

    /**
     * Places the tier's full layout with its lowest layer at {@code origin} and returns the
     * position of the placed forge block.
     */
    public static BlockPos place(ServerLevel level, BlockPos origin, int tier) {
        List<Placement> placements = placements(level, tier);

        int minY = 0;
        for (Placement placement : placements) {
            minY = Math.min(minY, placement.offset().getY());
        }
        BlockPos forgePos = origin.above(-minY);

        clear(level, forgePos, placements);
        for (Placement placement : placements) {
            if (placement.state().isAir()) {
                continue;
            }
            level.setBlock(forgePos.offset(placement.offset()), placement.state(), Block.UPDATE_ALL);
        }

        level.setBlock(forgePos, forgeBlock(tier).defaultBlockState(), Block.UPDATE_ALL);
        BlockState forgeState = level.getBlockState(forgePos);
        if (forgeState.getBlock() instanceof HephaestusForgeBlock forgeBlock) {
            forgeBlock.updateState(forgeState, level, forgePos);
        }
        return forgePos;
    }

    private static List<Placement> placements(ServerLevel level, int tier) {
        BlockState pedestal = ModBlocks.DARKSTONE_PEDESTAL.get().defaultBlockState();
        List<Placement> placements = new ArrayList<>();

        Optional<ForgeLayout> custom = ForgeLayouts.custom(level, tier);
        if (custom.isPresent()) {
            for (ForgeLayout.Requirement requirement : custom.get().requirements()) {
                placements.add(new Placement(
                        requirement.offset(),
                        requirement.kind() == ForgeLayout.RequirementKind.PEDESTAL
                                ? pedestal
                                : requirement.expected()));
            }
            return placements;
        }

        for (int z = 0; z < NATIVE_BASE.length; z++) {
            for (int x = 0; x < NATIVE_BASE[z].length(); x++) {
                char key = NATIVE_BASE[z].charAt(x);
                if (key == '*') {
                    continue;
                }
                placements.add(new Placement(new BlockPos(x - 4, -1, z - 4), nativeBaseState(key)));
            }
        }
        for (BlockPos offset : ForgeLayouts.nativePedestals()) {
            placements.add(new Placement(offset, pedestal));
        }
        return placements;
    }

    private static void clear(ServerLevel level, BlockPos forgePos, List<Placement> placements) {
        BlockPos min = BlockPos.ZERO;
        BlockPos max = BlockPos.ZERO;
        for (Placement placement : placements) {
            BlockPos offset = placement.offset();
            min = new BlockPos(
                    Math.min(min.getX(), offset.getX()),
                    Math.min(min.getY(), offset.getY()),
                    Math.min(min.getZ(), offset.getZ()));
            max = new BlockPos(
                    Math.max(max.getX(), offset.getX()),
                    Math.max(max.getY(), offset.getY()),
                    Math.max(max.getZ(), offset.getZ()));
        }
        for (BlockPos pos : BlockPos.betweenClosed(forgePos.offset(min), forgePos.offset(max))) {
            if (!level.getBlockState(pos).isAir()) {
                level.setBlock(pos.immutable(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    private static Block forgeBlock(int tier) {
        for (HephaestusForgeLevel forgeLevel : HephaestusForgeLevel.values()) {
            if (forgeLevel.getAsInt() == tier) {
                return forgeLevel.getBlock();
            }
        }
        throw new IllegalArgumentException("No Hephaestus Forge tier " + tier);
    }

    private static BlockState nativeBaseState(char key) {
        return switch (key) {
            case 'P' -> ModBlocks.POLISHED_DARKSTONE.get().defaultBlockState();
            case 'A' -> ModBlocks.GILDED_CHISELED_POLISHED_DARKSTONE.get().defaultBlockState();
            case 'C' -> ModBlocks.CHISELED_ARCANE_POLISHED_DARKSTONE.get().defaultBlockState();
            default -> throw new IllegalStateException("Unmapped native base key '" + key + "'");
        };
    }

    private record Placement(BlockPos offset, BlockState state) {}
}
