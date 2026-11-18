package io.github.bertie_mc.bertieprogression.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import io.github.bertie_mc.bertieprogression.BertieProgression;
import io.github.bertie_mc.bertieprogression.cybernetics.MechanicalAdapterBlockEntity;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public final class MechanicalAdapterRenderer extends KineticBlockEntityRenderer<MechanicalAdapterBlockEntity> {
    private static final PartialModel LOWER = partial("lower");
    private static final PartialModel UPPER = partial("upper");

    public static void init() {}

    private static PartialModel partial(String half) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(
                BertieProgression.MODID, "block/mechanical_adapter_shaft_" + half));
    }

    public MechanicalAdapterRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(
            MechanicalAdapterBlockEntity entity,
            float partialTicks,
            PoseStack poses,
            MultiBufferSource buffers,
            int light,
            int overlay) {
        var state = entity.getBlockState();
        var part = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER ? LOWER : UPPER;
        var facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        var model = CachedBuffers.partial(part, state)
                .rotateCentered((facing.toYRot() + 180) * Mth.DEG_TO_RAD, Direction.UP);
        standardKineticRotationTransform(model, entity, light).renderInto(poses, buffers.getBuffer(RenderType.solid()));
    }
}
