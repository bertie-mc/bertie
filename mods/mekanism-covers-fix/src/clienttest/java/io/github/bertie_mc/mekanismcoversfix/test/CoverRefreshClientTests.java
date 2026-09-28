package io.github.bertie_mc.mekanismcoversfix.test;

import dev.lucaargolo.mekanismcovers.MekanismCovers;
import dev.lucaargolo.mekanismcovers.mixed.TileEntityTransmitterMixed;
import io.github.bertie_mc.testing.client.ClientTest;
import io.github.bertie_mc.testing.client.context.ClientTestContext;
import io.github.bertie_mc.testing.client.context.IntegratedWorldContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;

public final class CoverRefreshClientTests {
    private CoverRefreshClientTests() {}

    @ClientTest
    public static void coverChangesRefreshCachedModelWithoutNeighborChanges(ClientTestContext context) {
        try (IntegratedWorldContext world = context.worldBuilder().create()) {
            world.connection().waitForChunksRender();
            BlockPos pos = world.server().computeOnServer(server -> {
                var player = world.connection().serverPlayer();
                BlockPos target = player.blockPosition().offset(2, 1, 0);
                var cable = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("mekanism:basic_universal_cable"));
                if (cable == Blocks.AIR) {
                    throw new AssertionError("Mekanism cable is absent");
                }
                world.connection().serverLevel().setBlockAndUpdate(target, cable.defaultBlockState());
                return target;
            });
            context.waitFor(
                    "client cable", client -> client.level.getBlockEntity(pos) instanceof TileEntityTransmitterMixed);
            world.connection().waitForChunksRender();

            applyCover(world, pos, Blocks.STONE);
            awaitCachedCover(context, pos, Blocks.STONE.defaultBlockState());
            applyCover(world, pos, Blocks.DIRT);
            awaitCachedCover(context, pos, Blocks.DIRT.defaultBlockState());
            removeCover(world, pos);
            awaitCachedCover(context, pos, null);

            applyCover(world, pos, Blocks.STONE);
            awaitCachedCover(context, pos, Blocks.STONE.defaultBlockState());
            // Simulate initial block-entity loading before a cover-removal packet arrives.
            context.runOnClient(client -> {
                BlockEntity old = client.level.getBlockEntity(pos);
                var registry = client.level.registryAccess();
                var tag = old.saveWithFullMetadata(registry);
                BlockEntity reloaded = BlockEntity.loadStatic(pos, old.getBlockState(), tag, registry);
                client.level.setBlockEntity(reloaded);
            });
            world.connection().waitForChunksRender();
            removeCover(world, pos);
            awaitCachedCover(context, pos, null);
        }
    }

    private static void applyCover(IntegratedWorldContext world, BlockPos pos, Block material) {
        world.server().runOnServer(server -> {
            var player = world.connection().serverPlayer();
            ItemStack cover = new ItemStack(MekanismCovers.COVER.get());
            cover.set(MekanismCovers.COVER_BLOCK, BuiltInRegistries.BLOCK.getKey(material));
            player.setItemInHand(InteractionHand.MAIN_HAND, cover);
            var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            var result = cover.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
            if (!result.consumesAction()) {
                throw new AssertionError("Cover application failed");
            }
        });
        world.connection().waitForClientboundPackets();
    }

    private static void removeCover(IntegratedWorldContext world, BlockPos pos) {
        world.server().runOnServer(server -> {
            var level = world.connection().serverLevel();
            BlockEntity tile = level.getBlockEntity(pos);
            MekanismCovers.removeCover(level, tile, tile.getBlockState(), pos, (TileEntityTransmitterMixed) tile, true);
        });
        world.connection().waitForClientboundPackets();
    }

    private static void awaitCachedCover(ClientTestContext context, BlockPos pos, BlockState expected) {
        context.waitFor("cached cover model " + expected, client -> {
            var tile = (TileEntityTransmitterMixed) client.level.getBlockEntity(pos);
            ModelData cached = client.level.getModelDataManager().getAt(pos);
            return tile != null
                    && tile.mekanism_covers$getCoverState() == expected
                    && cached != null
                    && cached.get(MekanismCovers.COVER_STATE) == expected;
        });
    }
}
