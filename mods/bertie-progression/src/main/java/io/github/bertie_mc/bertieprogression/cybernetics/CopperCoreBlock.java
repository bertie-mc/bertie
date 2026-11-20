package io.github.bertie_mc.bertieprogression.cybernetics;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.block.blocks.processing.MachineCoreBlock;
import rearth.oritech.block.entity.MachineCoreEntity;

/** Legacy casing substitutes remain loadable and retire to native Machine Cores. */
public final class CopperCoreBlock extends MachineCoreBlock {
    public CopperCoreBlock(Properties properties) {
        super(properties, 1);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CoreEntity(pos, state);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!level.isClientSide && state.getValue(USED)) {
            if (!next.is(this)
                    && level.getBlockEntity(pos) instanceof MachineCoreEntity core
                    && core.getCachedController() != null) {
                core.getCachedController().onCoreBroken(pos);
            } else if (next.is(this) && !next.getValue(USED)) level.scheduleTick(pos, this, 1);
        }
        super.onRemove(state, level, pos, next, moving);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(USED))
            level.setBlockAndUpdate(pos, AugmentationCores.basic().defaultBlockState());
    }

    public static final class CoreEntity extends MachineCoreEntity {
        public CoreEntity(BlockPos pos, BlockState state) {
            super(pos, state);
        }

        @Override
        public BlockEntityType<?> getType() {
            return MechanicalAdapterRegistry.COPPER_CORE_ENTITY.get();
        }
    }
}
