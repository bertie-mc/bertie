package io.github.bertie_mc.emi.integration.farmersdelight;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;
import vectorwing.farmersdelight.integration.emi.FDRecipeCategories;

/**
 * Farmer's Delight gained a native EMI plugin in 1.3, and Extra Mod Integrations still ships the
 * module it wrote before that. Both register a Cooking Pot, a Cutting Board and a Decomposition
 * category, and both use the same category ids, so EMI cannot tell them apart: it keys categories by
 * object identity, and two objects that merely agree on their id both survive. The result is the
 * same recipes listed under two identical-looking tabs.
 *
 * <p>Farmer's Delight's own plugin is the one that stays — it ships with the recipes it describes,
 * and the Copper Pot workstation hangs off its Cooking category. The other copy is emptied, which
 * takes its tab with it.
 *
 * <p>Nothing here refers to Extra Mod Integrations. The test is "same id, different object", so this
 * keeps working if that mod is removed, renamed, or replaced by a third plugin making the same
 * mistake, and it can never empty Farmer's Delight's own categories.
 */
public final class FarmersDelightDuplicateEmiModule {

    private FarmersDelightDuplicateEmiModule() {}

    public static void register(EmiRegistry reg) {
        Map<ResourceLocation, EmiRecipeCategory> owned =
                List.of(FDRecipeCategories.COOKING, FDRecipeCategories.CUTTING, FDRecipeCategories.DECOMPOSITION)
                        .stream()
                        .collect(Collectors.toMap(EmiRecipeCategory::getId, category -> category));

        reg.removeRecipes(recipe -> {
            EmiRecipeCategory category = recipe.getCategory();
            EmiRecipeCategory mine = owned.get(category.getId());
            return mine != null && mine != category;
        });
    }
}
