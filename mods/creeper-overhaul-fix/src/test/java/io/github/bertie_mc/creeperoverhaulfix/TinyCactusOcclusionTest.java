package io.github.bertie_mc.creeperoverhaulfix;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.MapCodec;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Creeper Overhaul's tiny cactus is a 4x4x4 nub built from bare {@code Properties.of()}, so unlike
 * every vanilla bush it keeps {@code canOcclude}. On its own that is harmless. Client Tweaks'
 * {@code creativeBreakingSupport} widens the shape of any block with an offset function to the full
 * footprint so it stays easy to click, and {@code getOcclusionShape} reads the same shape — so the
 * widened nub occludes a whole face and the ground below it stops drawing its top.
 */
class TinyCactusOcclusionTest {

    private static final BlockPos CACTUS_POS = new BlockPos(4, 65, -7);
    private static final BlockPos GROUND_POS = CACTUS_POS.below();

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        // Block construction claims an intrusive registry holder, which a frozen registry refuses.
        ((MappedRegistry<?>) BuiltInRegistries.BLOCK).unfreeze();
    }

    @Test
    void tinyCactusIsNoLongerAnOccluder() {
        assertFalse(
                new tech.thatgravyboat.creeperoverhaul.common.block.TinyCactusBlock()
                        .defaultBlockState()
                        .canOcclude(),
                "the fix must clear canOcclude on Creeper Overhaul's tiny cactus");
    }

    @Test
    void widenedOccludingCactusHidesTheGroundBelowIt() {
        assertFalse(
                groundDrawsItsTopFace(new WidenedCactus(upstreamProperties())),
                "an occluding cactus widened to the full footprint culls the top face below it");
    }

    @Test
    void widenedNonOccludingCactusLeavesTheGroundAlone() {
        assertTrue(
                groundDrawsItsTopFace(new WidenedCactus(upstreamProperties().noOcclusion())),
                "clearing canOcclude keeps the ground's top face whatever shape the block reports");
    }

    /** The property chain Creeper Overhaul 4.0.6 gives {@code creeperoverhaul:tiny_cactus}. */
    private static BlockBehaviour.Properties upstreamProperties() {
        return BlockBehaviour.Properties.of()
                .strength(0.4F)
                .sound(SoundType.WOOL)
                .dynamicShape()
                .offsetType(BlockBehaviour.OffsetType.XZ);
    }

    private static boolean groundDrawsItsTopFace(Block cactus) {
        cactus.getStateDefinition().getPossibleStates().forEach(BlockState::initCache);
        BlockState ground = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockGetter level = new TwoBlockLevel(ground, cactus.defaultBlockState());
        return Block.shouldRenderFace(ground, level, GROUND_POS, Direction.UP, CACTUS_POS);
    }

    /** The tiny cactus as Client Tweaks reports it to a creative-mode client. */
    private static final class WidenedCactus extends BushBlock {
        private static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 4, 10);
        private static final VoxelShape WIDENED = Shapes.create(SHAPE.bounds()
                .expandTowards(-1.0, 0.0, -1.0)
                .expandTowards(1.0, 0.0, 1.0)
                .intersect(new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)));

        private WidenedCactus(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return WIDENED;
        }

        @Override
        protected MapCodec<? extends BushBlock> codec() {
            return MapCodec.unit(() -> this);
        }
    }

    /** The ground at {@link #GROUND_POS}, the cactus above it, air everywhere else. */
    private record TwoBlockLevel(BlockState ground, BlockState cactus) implements BlockGetter {

        @Override
        public BlockState getBlockState(BlockPos pos) {
            if (pos.equals(GROUND_POS)) {
                return ground;
            }
            return pos.equals(CACTUS_POS) ? cactus : Blocks.AIR.defaultBlockState();
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return getBlockState(pos).getFluidState();
        }

        @Override
        public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public int getHeight() {
            return 384;
        }

        @Override
        public int getMinBuildHeight() {
            return -64;
        }
    }
}
