package io.github.bertie_mc.emi.integration.l2complements;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.xkmc.l2complements.content.recipe.BurntRecipe;
import dev.xkmc.l2complements.content.recipe.DiffusionRecipe;
import io.github.bertie_mc.emi.framework.Categories;
import io.github.bertie_mc.emi.framework.GenericEmiRecipe;
import io.github.bertie_mc.emi.framework.MachineDescriptor;
import io.github.bertie_mc.emi.framework.Recipes;
import java.text.NumberFormat;
import java.util.Locale;
import net.minecraft.network.chat.Component;

/**
 * L2 Complements — Burning (item -> essence) and Diffusion (block + base block -> block, via the
 * Diffusion Wand). Recipe base = l2core BaseRecipe (public fields). Diffusion fields are Blocks.
 *
 * <p>Burning covers every {@code burnt} recipe in the pack, Curse of Pandora's and L2 Hostility's
 * included, since they share L2 Complements' recipe type. The chance carries the whole meaning of
 * these: an item entity killed by fire yields the result once per {@code chance} items consumed, and
 * that ranges from 1 in 64 to 1 in 27,648. Shown as odds rather than through
 * {@code EmiStack.setChance}, whose "0.##" percentage renders the long ones as a flat 0%.
 */
public final class L2ComplementsEmiModule {

    /** Grouped, because a bare 27648 is unreadable at a glance. */
    private static final NumberFormat ODDS = NumberFormat.getIntegerInstance(Locale.ROOT);

    private L2ComplementsEmiModule() {}

    public static void register(EmiRegistry reg) {
        EmiRecipeCategory burnt =
                Categories.machineNoStation(reg, "l2complements_burnt", "minecraft:lava_bucket", "Burning");
        Recipes.forEach(reg.getRecipeManager(), BurntRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(EmiIngredient.of(r.ingredient));
            d.itemOut(EmiStack.of(r.result));
            if (r.chance > 1) {
                d.info(Component.literal("1 in " + ODDS.format(r.chance) + " per item burned"));
            }
            reg.addRecipe(new GenericEmiRecipe(burnt, id, d));
        });

        EmiRecipeCategory diff =
                Categories.machine(reg, "l2complements_diffusion", "l2complements:diffusion_wand", "Diffusion");
        Recipes.forEach(reg.getRecipeManager(), DiffusionRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(EmiStack.of(r.ingredient)); // Block is an ItemLike
            d.catalyst(EmiStack.of(r.base));
            d.itemOut(EmiStack.of(r.result));
            reg.addRecipe(new GenericEmiRecipe(diff, id, d));
        });
    }
}
