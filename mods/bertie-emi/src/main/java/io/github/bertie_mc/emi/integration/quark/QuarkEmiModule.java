package io.github.bertie_mc.emi.integration.quark;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiWorldInteractionRecipe;
import dev.emi.emi.api.stack.EmiStack;
import io.github.bertie_mc.emi.framework.Categories;
import net.minecraft.resources.ResourceLocation;

/**
 * Quark — Cloud in a Bottle. Filled by right-clicking a Glass Bottle while standing inside the cloud
 * layer, which is a right-click handler rather than a recipe, so the item had no source in any viewer.
 * It goes in EMI's World Interaction category, where the altitude is the whole of the requirement.
 */
public final class QuarkEmiModule {

    /** {@code BottledCloudModule} fills the bottle only between these two heights. */
    private static final int CLOUD_BOTTOM = 191;

    private static final int CLOUD_TOP = 196;

    private QuarkEmiModule() {}

    public static void register(EmiRegistry reg) {
        EmiStack bottle = Categories.stack("minecraft:glass_bottle");
        EmiStack cloud = Categories.stack("quark:bottled_cloud");
        if (bottle.isEmpty() || cloud.isEmpty()) {
            return;
        }
        reg.addRecipe(EmiWorldInteractionRecipe.builder()
                .id(ResourceLocation.fromNamespaceAndPath("bertieemi", "quark/bottled_cloud"))
                .leftInput(bottle)
                .rightInput(Categories.stack("quark:cloud"), true)
                .output(cloud)
                .build());
        io.github.bertie_mc.emi.framework.InfoPages.page(
                reg,
                "quark/bottled_cloud",
                java.util.List.of("quark:bottled_cloud"),
                "Right-click a Glass Bottle while inside the cloud layer,",
                "between Y=" + CLOUD_BOTTOM + " and Y=" + CLOUD_TOP + ".");
    }
}
