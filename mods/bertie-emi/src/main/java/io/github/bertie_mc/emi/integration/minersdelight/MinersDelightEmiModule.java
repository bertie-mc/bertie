package io.github.bertie_mc.emi.integration.minersdelight;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import io.github.bertie_mc.emi.framework.Categories;
import vectorwing.farmersdelight.integration.emi.FDRecipeCategories;

/**
 * Miner's Delight — the Copper Pot. Its recipes are Farmer's Delight cooking recipes, so EMI already
 * lists them, but nothing tells EMI the Copper Pot is a place they can be made: the mod ships a JEI
 * catalyst and no EMI workstation, which left the pot looking like a block with nothing to cook.
 * Registering it on Farmer's Delight's own Cooking category means it opens the same recipes the
 * Cooking Pot does, which is what it actually does in game.
 */
public final class MinersDelightEmiModule {

    private MinersDelightEmiModule() {}

    public static void register(EmiRegistry reg) {
        EmiStack copperPot = Categories.stack("minersdelight:copper_pot");
        if (!copperPot.isEmpty()) {
            reg.addWorkstation(FDRecipeCategories.COOKING, copperPot);
        }
    }
}
