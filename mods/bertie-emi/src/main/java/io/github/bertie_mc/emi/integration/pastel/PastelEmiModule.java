package io.github.bertie_mc.emi.integration.pastel;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import earth.terrarium.pastel.compat.emi.PastelEmiRecipeCategories;
import earth.terrarium.pastel.compat.emi.recipes.PedestalCraftingEmiRecipeGated;
import earth.terrarium.pastel.recipe.pedestal.PedestalRecipe;
import earth.terrarium.pastel.recipe.pedestal.PedestalTier;
import io.github.bertie_mc.emi.framework.Categories;
import io.github.bertie_mc.emi.framework.GenericEmiCategory;
import io.github.bertie_mc.emi.framework.Recipes;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Pastel pigment pedestals. Pastel ships one {@code pastel:pedestal_crafting} category holding every
 * pedestal recipe, with all six pedestal blocks as its workstations — so the Topaz pedestal advertised
 * Moonstone recipes it will never run. This splits it into one category per {@link PedestalTier},
 * each workstationed by only the pedestals of that tier. A better pedestal still crafts everything
 * below it in game; the tabs just stop repeating the lower tiers.
 *
 * <p>The entries are Pastel's own {@link PedestalCraftingEmiRecipeGated} — subclassed purely to
 * answer a different category — so the grid, powder slots, crafting time and the "recipe not
 * unlocked" gating render exactly as Pastel draws them, and each keeps its real recipe id.
 * Pastel's own category is then emptied with {@link EmiRegistry#removeRecipes}, which EMI applies at
 * bake time regardless of plugin order; an empty category is dropped from the recipe screen.
 */
public final class PastelEmiModule {

    private PastelEmiModule() {}

    public static void register(EmiRegistry reg) {
        // Resolve Pastel's category first: if a future Pastel moves or renames it this throws before
        // anything is registered, and the module is skipped with Pastel's own single tab left intact.
        EmiRecipeCategory pastelCategory = PastelEmiRecipeCategories.PEDESTAL_CRAFTING;

        Map<PedestalTier, EmiRecipeCategory> byTier = new EnumMap<>(PedestalTier.class);
        byTier.put(
                PedestalTier.BASIC,
                category(
                        reg,
                        "pastel_pedestal_basic",
                        "Pedestal Crafting (Topaz/Amethyst/Citrine)",
                        "pastel:pedestal_basic_amethyst",
                        "pastel:pedestal_basic_topaz",
                        "pastel:pedestal_basic_citrine"));
        byTier.put(
                PedestalTier.SIMPLE,
                category(reg, "pastel_pedestal_cmy", "Pedestal Crafting (CMY)", "pastel:pedestal_all_basic"));
        byTier.put(
                PedestalTier.ADVANCED,
                category(reg, "pastel_pedestal_onyx", "Pedestal Crafting (Onyx)", "pastel:pedestal_onyx"));
        byTier.put(
                PedestalTier.COMPLEX,
                category(
                        reg,
                        "pastel_pedestal_moonstone",
                        "Pedestal Crafting (Moonstone)",
                        "pastel:pedestal_moonstone"));

        reg.removeRecipes(recipe -> recipe.getCategory() == pastelCategory);

        Recipes.forEach(reg.getRecipeManager(), PedestalRecipe.class, (id, recipe) -> {
            EmiRecipeCategory tier = byTier.get(recipe.getTier());
            if (tier != null) {
                reg.addRecipe(new TieredPedestalRecipe(recipe, tier));
            }
        });
    }

    /** The first workstation id is also the category icon; every one of them opens the category. */
    private static EmiRecipeCategory category(EmiRegistry reg, String key, String name, String... workstationItemIds) {
        GenericEmiCategory cat = new GenericEmiCategory(
                ResourceLocation.fromNamespaceAndPath("bertieemi", key),
                Categories.stack(workstationItemIds[0]),
                Component.literal(name));
        reg.addCategory(cat);
        for (String itemId : workstationItemIds) {
            EmiStack workstation = Categories.stack(itemId);
            if (!workstation.isEmpty()) {
                reg.addWorkstation(cat, workstation);
            }
        }
        return cat;
    }

    /** Pastel's pedestal entry, reported under the category of the tier that recipe actually needs. */
    private static final class TieredPedestalRecipe extends PedestalCraftingEmiRecipeGated {
        private final EmiRecipeCategory tierCategory;

        private TieredPedestalRecipe(PedestalRecipe recipe, EmiRecipeCategory tierCategory) {
            super(recipe);
            this.tierCategory = tierCategory;
        }

        @Override
        public EmiRecipeCategory getCategory() {
            return tierCategory;
        }
    }
}
