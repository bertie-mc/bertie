package io.github.bertie_mc.emi.framework;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * EMI Information pages — the place to put what a recipe row cannot say. Two kinds of thing end up
 * here: mechanics that happen in the world rather than in a machine, and items that have no recipe
 * at all, where the useful answer is "you mine this" or "nothing in the mod makes it".
 */
public final class InfoPages {
    private InfoPages() {}

    private static final String NS = "bertieemi";

    /**
     * One page, attached to every one of the given items that this pack actually has. A page whose
     * subjects are all absent is skipped, so pages written against a newer version of a mod simply
     * do not appear rather than showing empty.
     */
    public static void page(EmiRegistry reg, String idPath, List<String> itemIds, String... lines) {
        List<EmiIngredient> stacks = new ArrayList<>();
        for (String id : itemIds) {
            EmiStack stack = Categories.stack(id);
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        if (stacks.isEmpty()) {
            return;
        }
        List<Component> text = Arrays.stream(lines)
                .map(Component::literal)
                .map(Component.class::cast)
                .toList();
        reg.addRecipe(new EmiInfoRecipe(stacks, text, ResourceLocation.fromNamespaceAndPath(NS, idPath)));
    }
}
