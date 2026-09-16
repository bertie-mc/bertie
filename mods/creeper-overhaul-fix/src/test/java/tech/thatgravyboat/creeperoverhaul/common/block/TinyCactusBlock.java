package tech.thatgravyboat.creeperoverhaul.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Stands in for Creeper Overhaul's block of the same name so the mixin, which matches on the class
 * name, applies in tests without putting a third-party jar on the classpath. Only the parts the fix
 * depends on are reproduced: the property chain and the shape as of Creeper Overhaul 4.0.6.
 */
public class TinyCactusBlock extends BushBlock {

    private static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 4, 10);

    public TinyCactusBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(0.4F)
                .sound(SoundType.WOOL)
                .dynamicShape()
                .offsetType(BlockBehaviour.OffsetType.XZ));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Vec3 offset = state.getOffset(level, pos);
        return SHAPE.move(offset.x, offset.y, offset.z);
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return MapCodec.unit(TinyCactusBlock::new);
    }
}
