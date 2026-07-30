package com.berlord.hephaestusarchitecture.structure;

import com.stal111.forbidden_arcanus.common.block.HephaestusForgeBlock;
import com.stal111.forbidden_arcanus.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A structure template normalized around its one Hephaestus Forge block.
 *
 * <p>Forge blocks and pedestals are semantic markers. Structure Void is ignored;
 * air and every other saved block state are exact requirements.</p>
 */
public record ForgeLayout(int tier,
                          List<Requirement> requirements,
                          List<BlockPos> pedestalOffsets) {

    public ForgeLayout {
        requirements = List.copyOf(requirements);
        pedestalOffsets = List.copyOf(pedestalOffsets);
    }

    public @Nullable Match match(net.minecraft.server.level.ServerLevel level, BlockPos forgePos) {
        for (Rotation rotation : Rotation.values()) {
            if (this.matches(level, forgePos, rotation)) {
                List<BlockPos> rotatedPedestals = this.pedestalOffsets.stream()
                        .map(offset -> rotate(offset, rotation))
                        .toList();
                return new Match(rotation, rotatedPedestals);
            }
        }
        return null;
    }

    private boolean matches(net.minecraft.server.level.ServerLevel level, BlockPos forgePos, Rotation rotation) {
        BlockState forgeState = level.getBlockState(forgePos);

        if (!(forgeState.getBlock() instanceof HephaestusForgeBlock forgeBlock)
                || forgeBlock.getLevel().getAsInt() != this.tier) {
            return false;
        }

        for (Requirement requirement : this.requirements) {
            BlockPos target = forgePos.offset(rotate(requirement.offset(), rotation));
            BlockState actual = level.getBlockState(target);

            if (requirement.kind() == RequirementKind.PEDESTAL) {
                if (!actual.is(ModTags.Blocks.PEDESTALS)) {
                    return false;
                }
            } else if (requirement.expected().isAir()) {
                if (!actual.isAir()) {
                    return false;
                }
            } else if (!actual.equals(requirement.expected().rotate(rotation))) {
                return false;
            }
        }
        return true;
    }

    static BlockPos rotate(BlockPos offset, Rotation rotation) {
        int turns = switch (rotation) {
            case CLOCKWISE_90 -> 1;
            case CLOCKWISE_180 -> 2;
            case COUNTERCLOCKWISE_90 -> 3;
            default -> 0;
        };
        RotationMath.Offset rotated = RotationMath.rotate(
                offset.getX(), offset.getY(), offset.getZ(), turns);
        return new BlockPos(rotated.x(), rotated.y(), rotated.z());
    }

    public enum RequirementKind {
        EXACT,
        PEDESTAL
    }

    public record Requirement(BlockPos offset, BlockState expected, RequirementKind kind) {

        public static Requirement exact(BlockPos offset, BlockState state) {
            return new Requirement(offset.immutable(), state, RequirementKind.EXACT);
        }

        public static Requirement pedestal(BlockPos offset) {
            return new Requirement(offset.immutable(), Blocks.AIR.defaultBlockState(), RequirementKind.PEDESTAL);
        }
    }

    public record Match(Rotation rotation, List<BlockPos> pedestalOffsets) {

        public Match {
            pedestalOffsets = List.copyOf(pedestalOffsets);
        }

        public List<BlockPos> pedestalPositions(BlockPos forgePos) {
            return this.pedestalOffsets.stream().map(forgePos::offset).toList();
        }
    }
}
