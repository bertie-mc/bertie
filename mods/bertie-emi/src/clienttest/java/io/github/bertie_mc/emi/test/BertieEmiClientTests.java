package io.github.bertie_mc.emi.test;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.EmiRecipeManager;
import io.github.bertie_mc.testing.client.ClientTest;
import io.github.bertie_mc.testing.client.context.ClientTestContext;
import io.github.bertie_mc.testing.client.context.IntegratedWorldContext;
import net.minecraft.resources.ResourceLocation;

/** Client integration coverage for the Forbidden & Arcanus EMI integration. */
public final class BertieEmiClientTests {
    private static final ResourceLocation CLIBANO_CATEGORY =
            ResourceLocation.fromNamespaceAndPath("bertieemi", "fa_clibano_combustion");
    private static final ResourceLocation HEPHAESTUS_CATEGORY =
            ResourceLocation.fromNamespaceAndPath("bertieemi", "fa_hephaestus_ritual_tier_1");

    private BertieEmiClientTests() {}

    @ClientTest
    public static void registersForbiddenArcanusIntegration(ClientTestContext context) {
        try (IntegratedWorldContext world = context.worldBuilder()
                .adjustSettings(settings -> settings.setName("bertie-emi"))
                .create()) {
            context.waitFor("Forbidden & Arcanus EMI category registration", client -> categoriesReady());
            context.runOnClient(client -> assertCategories());
        }
    }

    private static boolean categoriesReady() {
        return findCategory(CLIBANO_CATEGORY) != null && findCategory(HEPHAESTUS_CATEGORY) != null;
    }

    private static void assertCategories() {
        assertPopulatedCategory(CLIBANO_CATEGORY, "Clibano");
        assertPopulatedCategory(HEPHAESTUS_CATEGORY, "Hephaestus Forge");
    }

    private static void assertPopulatedCategory(ResourceLocation id, String name) {
        EmiRecipeManager manager = EmiApi.getRecipeManager();
        EmiRecipeCategory category = findCategory(id);
        if (category == null) {
            throw new AssertionError(name + " EMI category is absent");
        }
        if (manager.getWorkstations(category).isEmpty()) {
            throw new AssertionError(name + " EMI category has no workstation");
        }
        if (manager.getRecipes(category).isEmpty()) {
            throw new AssertionError(name + " EMI category has no recipes");
        }
    }

    private static EmiRecipeCategory findCategory(ResourceLocation id) {
        EmiRecipeManager manager = EmiApi.getRecipeManager();
        if (manager == null) {
            return null;
        }
        return manager.getCategories().stream()
                .filter(category -> category.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
}
