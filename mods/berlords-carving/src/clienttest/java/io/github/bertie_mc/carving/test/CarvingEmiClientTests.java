package io.github.bertie_mc.carving.test;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.EmiRecipeManager;
import io.github.bertie_mc.testing.client.ClientTest;
import io.github.bertie_mc.testing.client.ClientTestContext;
import io.github.bertie_mc.testing.client.IntegratedWorldContext;
import net.minecraft.resources.ResourceLocation;

/** Client integration coverage for Berlord's Carving's built-in EMI plugin. */
public final class CarvingEmiClientTests {
    private static final ResourceLocation CARVING_CATEGORY =
            ResourceLocation.fromNamespaceAndPath("berlordscarving", "carving");

    private CarvingEmiClientTests() {
    }

    @ClientTest
    public static void registersCarvingRecipesWithEmi(ClientTestContext context) {
        try (IntegratedWorldContext world = context.worldBuilder()
                .adjustSettings(settings -> settings.setName("berlords-carving"))
                .create()) {
            context.waitFor(
                    "Carving EMI category registration",
                    client -> findCategory() != null);

            context.runOnClient(client -> {
                EmiRecipeManager manager = EmiApi.getRecipeManager();
                EmiRecipeCategory category = findCategory();
                if (category == null) {
                    throw new AssertionError("Carving EMI category is absent");
                }
                if (manager.getWorkstations(category).isEmpty()) {
                    throw new AssertionError("Carving EMI category has no workstation");
                }
                if (manager.getRecipes(category).isEmpty()) {
                    throw new AssertionError("Carving EMI category has no recipes");
                }
            });
        }
    }

    private static EmiRecipeCategory findCategory() {
        EmiRecipeManager manager = EmiApi.getRecipeManager();
        if (manager == null) {
            return null;
        }
        return manager.getCategories().stream()
                .filter(category -> category.getId().equals(CARVING_CATEGORY))
                .findFirst()
                .orElse(null);
    }
}
