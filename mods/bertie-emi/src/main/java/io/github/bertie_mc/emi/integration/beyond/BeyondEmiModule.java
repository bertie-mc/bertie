package io.github.bertie_mc.emi.integration.beyond;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import io.github.bertie_mc.emi.framework.Categories;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/** Mechanics implemented by block and entity interactions rather than Minecraft recipes. */
public final class BeyondEmiModule {
    private BeyondEmiModule() {}

    public static void register(EmiRegistry registry) {
        Map<BeyondPages.Group, EmiRecipeCategory> categories = new EnumMap<>(BeyondPages.Group.class);
        for (BeyondPages.Group group : BeyondPages.Group.values()) {
            categories.put(
                    group,
                    Categories.machine(
                            registry,
                            "beyond_" + group.name().toLowerCase(java.util.Locale.ROOT),
                            "the_beyond:" + group.icon,
                            "The Beyond: " + group.title));
        }
        for (BeyondPages.Page page : BeyondPages.all()) {
            registry.addRecipe(new BeyondSchematicRecipe(
                    categories.get(page.group()),
                    ResourceLocation.fromNamespaceAndPath("bertieemi", "beyond/" + page.id()),
                    page));
        }
    }
}
