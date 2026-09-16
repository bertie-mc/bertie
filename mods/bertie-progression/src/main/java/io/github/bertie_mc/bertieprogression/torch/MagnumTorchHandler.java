package io.github.bertie_mc.bertieprogression.torch;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * A Magnum Torch is built rather than crafted. Stack two Edelwood Logs, set the gem block on top of
 * them and light a campfire above that, then touch the gem block with Mundabitur Dust: the pillar
 * collapses into the matching torch, standing where the lower log was.
 *
 * <p>Which gem block gives which torch is the mod's own pairing - Diamond, Emerald, Amethyst - and
 * the torch that a missing Magnum Torch install would resolve to is simply not offered, so the
 * ritual disables itself rather than eating the build.
 */
public final class MagnumTorchHandler {

    private static final ResourceLocation DUST = ResourceLocation.parse("forbidden_arcanus:mundabitur_dust");
    private static final ResourceLocation EDELWOOD = ResourceLocation.parse("forbidden_arcanus:edelwood_log");

    /** Gem block -> the torch it yields. Resolved once, on first use, after every mod has loaded. */
    private static final Map<String, String> PAIRS = new LinkedHashMap<>(Map.of(
            "minecraft:diamond_block", "magnumtorch:diamond_magnum_torch",
            "minecraft:emerald_block", "magnumtorch:emerald_magnum_torch",
            "minecraft:amethyst_block", "magnumtorch:amethyst_magnum_torch"));

    private static Map<Block, Block> torches;

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        ItemStack held = event.getItemStack();
        if (!BuiltInRegistries.ITEM.getKey(held.getItem()).equals(DUST)) return;

        Level level = event.getLevel();
        BlockPos gem = event.getPos();
        Block torch = torchFor(level.getBlockState(gem).getBlock());
        if (torch == null) return;

        // Bottom to top: log, log, gem block, lit campfire. The dust goes on the gem block, which
        // is the only part of the pillar a player can reach without breaking it.
        BlockPos upper = gem.below();
        BlockPos lower = upper.below();
        if (!isEdelwood(level.getBlockState(upper)) || !isEdelwood(level.getBlockState(lower))) return;
        if (!isLitCampfire(level.getBlockState(gem.above()))) return;

        Player player = event.getEntity();
        if (player.isSpectator()) return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        player.swing(InteractionHand.MAIN_HAND, true);
        if (level.isClientSide) return;

        level.removeBlock(gem.above(), false);
        level.removeBlock(gem, false);
        level.removeBlock(upper, false);
        level.setBlockAndUpdate(lower, torch.defaultBlockState());
        held.consume(1, player);

        level.playSound(null, lower, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.2F);
        if (level instanceof ServerLevel server) {
            server.sendParticles(
                    ParticleTypes.END_ROD,
                    lower.getX() + 0.5,
                    lower.getY() + 1.5,
                    lower.getZ() + 0.5,
                    60,
                    0.4,
                    1.2,
                    0.4,
                    0.02);
        }
    }

    private static boolean isEdelwood(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(EDELWOOD);
    }

    private static boolean isLitCampfire(BlockState state) {
        return state.getBlock() instanceof CampfireBlock && state.getValue(CampfireBlock.LIT);
    }

    /** The torch this gem block yields, or null when it is not one of the three. */
    private static Block torchFor(Block block) {
        if (torches == null) {
            Map<Block, Block> resolved = new LinkedHashMap<>();
            PAIRS.forEach((gemId, torchId) -> {
                Block gem = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(gemId));
                Block torch = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(torchId));
                if (gem != Blocks.AIR && torch != Blocks.AIR) resolved.put(gem, torch);
            });
            torches = resolved;
        }
        return torches.get(block);
    }

    private MagnumTorchHandler() {}
}
