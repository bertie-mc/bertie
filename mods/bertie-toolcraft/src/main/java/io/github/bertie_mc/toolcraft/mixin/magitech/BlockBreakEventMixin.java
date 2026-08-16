package io.github.bertie_mc.toolcraft.mixin.magitech;

import io.github.bertie_mc.toolcraft.ToolcraftPolicy;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.stln.magitech.item.tool.trait.BlockBreakEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Narrows the scythe's area harvest to plants.
 *
 * <p>Magitech's stock scythe sweeps up to 20 blocks of whatever block it hits, which makes it a
 * general-purpose area miner. Bertie wants a hoe that happens to harvest fields: the sweep only
 * fires on blocks that break instantly (crops, grass, flowers, saplings). Anything with real
 * hardness that a hoe is still the right tool for — nether wart block, shroomlight, sculk — breaks
 * one block at a time.
 *
 * <p>Checking the origin block alone is sufficient because Magitech only ever collects blocks of
 * the same {@link Block} as the origin, so the whole sweep shares its hardness.
 */
@Mixin(BlockBreakEvent.class)
public abstract class BlockBreakEventMixin {

    @Inject(method = "addScytheMine", at = @At("HEAD"), cancellable = true)
    private static void bertietoolcraft$onlySweepInstantBlocks(
            Player player, ItemStack stack, BlockPos pos, Set<BlockPos> collected, Block block, CallbackInfo ci) {
        if (player == null) {
            return;
        }
        Level level = player.level();
        BlockState state = level.getBlockState(pos);
        if (!ToolcraftPolicy.sweepsInAreaHarvest(state.getDestroySpeed(level, pos))) {
            ci.cancel(); // hardness-bearing block: behave like an ordinary hoe, one block at a time
        }
    }
}
