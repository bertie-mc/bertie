package io.github.bertie_mc.bertieprogression.gate;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Flint against the side of a plank block splits it in two: the lower half stays as a bottom slab
 * and the upper half drops as an item. One try in ten works, so a stack of planks is not a
 * shortcut past the sawmill — it is the early-game way to a slab before any saw exists.
 *
 * <p>Only the four side faces count. Striking the top or the bottom is the gesture for placing or
 * mining, and taking it over would make planks unusable as a building block.
 *
 * <p>The slab is resolved by registry name rather than a hand-written table, so every wood set in
 * the pack works without listing it: {@code <ns>:<name>_planks} pairs with {@code <ns>:<name>_slab}.
 * The lookup is cached because the miss case - any non-plank block - is the common one.
 */
public final class PlankSplitHandler {

    private static final float CHANCE = 0.1F;
    private static final Map<Block, Block> SLAB_OF = new HashMap<>();

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        ItemStack held = event.getItemStack();
        if (!held.is(Items.FLINT)) return;

        BlockHitResult hit = event.getHitVec();
        if (hit == null || hit.getDirection().getAxis() == Direction.Axis.Y) return;

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        Block slab = slabFor(state.getBlock());
        if (slab == null) return;
        // A plank block with a block entity is somebody else's block wearing a plank model.
        if (level.getBlockEntity(pos) != null) return;

        Player player = event.getEntity();
        if (player.isSpectator()) return;

        // Claim the interaction either way: a failed try must not also place whatever is in the
        // off hand, or the gesture becomes unusable next to a wall.
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide));
        player.swing(InteractionHand.MAIN_HAND, true);

        if (level.isClientSide) return;

        SoundType sound = state.getSoundType();
        level.playSound(
                null,
                pos,
                sound.getBreakSound(),
                SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F,
                sound.getPitch() * 0.8F);

        if (level.getRandom().nextFloat() >= CHANCE) return;

        BlockState bottom = slab.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM);
        level.setBlockAndUpdate(pos, bottom);
        Block.popResourceFromFace(level, pos, hit.getDirection(), new ItemStack(slab));
        if (level instanceof ServerLevel server) {
            server.levelEvent(2001, pos, Block.getId(state));
        }
    }

    /** The matching slab, or null when this block is not a plank that has one. */
    private static Block slabFor(Block block) {
        Block slab = SLAB_OF.computeIfAbsent(block, b -> {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(b);
            if (!id.getPath().endsWith("_planks")) return Blocks.AIR;
            String base = id.getPath().substring(0, id.getPath().length() - "_planks".length());
            Block candidate = BuiltInRegistries.BLOCK
                    .getOptional(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), base + "_slab"))
                    .orElse(Blocks.AIR);
            return candidate instanceof SlabBlock ? candidate : Blocks.AIR;
        });
        return slab == Blocks.AIR ? null : slab;
    }

    private PlankSplitHandler() {}
}
