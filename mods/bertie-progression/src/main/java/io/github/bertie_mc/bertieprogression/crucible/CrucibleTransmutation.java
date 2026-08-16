package io.github.bertie_mc.bertieprogression.crucible;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * The Zardius Crucible is not mixed INTO a basin - the basin becomes it.
 *
 * <p>Magitech's own recipe is a bench craft. This is the second route: a heated basin under a
 * mixer, charged with arcane spirits, fluorite, mundabitur dust and a bucket of lava. The mixer
 * spins it for the ordinary length of time, and then the basin is gone and a crucible stands in its
 * place, with the mixer knocked off the top of it.
 *
 * <p>It has to be a real Create recipe, because nothing else will make a mixer turn: the recipe is
 * what Create matches, times and drives. Only its LAST step is ours. Rather than let Create push the
 * crucible into the basin as an item, this intercepts the moment the craft lands and rewrites the
 * world instead - which is also why the recipe's declared result is the crucible itself. If this
 * hook ever fails to bind, the recipe still resolves to a crucible in the basin and the player is
 * inconvenienced rather than stuck.
 *
 * <p>Magitech is an optional runtime dependency, so the block is resolved by id and everything here
 * turns into a no-op when it is absent.
 */
public final class CrucibleTransmutation {

    private static final ResourceLocation CRUCIBLE =
            ResourceLocation.fromNamespaceAndPath("magitech", "zardius_crucible");

    private CrucibleTransmutation() {}

    /**
     * Ours iff a processing recipe rolls a crucible. No other recipe in the pack produces one, and
     * matching on the result rather than a recipe id keeps this working if the file is ever renamed.
     */
    public static boolean isCrucibleRecipe(Recipe<?> recipe) {
        if (!(recipe instanceof ProcessingRecipe<?, ?> processing)) {
            return false;
        }
        Block crucible = crucible();
        if (crucible == null) {
            return false;
        }
        for (ItemStack stack : processing.getRollableResultsAsItemStacks()) {
            if (stack.is(crucible.asItem())) {
                return true;
            }
        }
        return false;
    }

    /** Empties the basin, knocks the machine off it and leaves a crucible behind. */
    public static void transform(Level level, BasinBlockEntity basin) {
        Block crucible = crucible();
        if (crucible == null) {
            return;
        }
        BlockPos pos = basin.getBlockPos();

        // Emptied first: Create drops a basin's contents when the block goes, and the ingredients
        // have been spent. The lava went with them.
        clear(basin);

        // Create drives a basin from one or two blocks above it. Whichever machine was turning is
        // dropped rather than deleted - it is the same pop-off the player already knows from an
        // overstressed contraption, so nothing is lost to a craft that succeeded.
        for (int up = 1; up <= 2; up++) {
            BlockPos above = pos.above(up);
            BlockEntity be = level.getBlockEntity(above);
            if (be instanceof BasinOperatingBlockEntity) {
                level.destroyBlock(above, true);
                break;
            }
        }

        level.setBlock(pos, crucible.defaultBlockState(), Block.UPDATE_ALL);
    }

    private static void clear(BasinBlockEntity basin) {
        for (var inventory : new com.simibubi.create.foundation.item.SmartInventory[] {
            basin.getInputInventory(), basin.getOutputInventory()
        }) {
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                inventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
        basin.getTanks()
                .forEach(behaviour ->
                        behaviour.getCapability().drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE));
    }

    private static Block crucible() {
        return BuiltInRegistries.BLOCK.getOptional(CRUCIBLE).orElse(null);
    }
}
