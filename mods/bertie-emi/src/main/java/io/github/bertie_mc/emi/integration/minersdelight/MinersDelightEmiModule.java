package io.github.bertie_mc.emi.integration.minersdelight;

import com.sammy.minersdelight.content.data.CupConversionDataMap;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import io.github.bertie_mc.emi.framework.Categories;
import io.github.bertie_mc.emi.framework.GenericEmiRecipe;
import io.github.bertie_mc.emi.framework.InfoPages;
import io.github.bertie_mc.emi.framework.MachineDescriptor;
import io.github.bertie_mc.emi.framework.Recipes;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;
import vectorwing.farmersdelight.integration.emi.FDRecipeCategories;

/**
 * Miner's Delight. Two separate reasons its food went unexplained, neither of them a recipe.
 *
 * <p>The cup meals: the Copper Pot cooks ordinary Farmer's Delight recipes, and where the result has
 * a {@code minersdelight:cup_variant} data map entry the pot serves the cup instead of the bowl. No
 * recipe says so, so two dozen cup meals had no source at all. Each is listed here against the real
 * ingredients of the recipe it comes from, which also makes the pot itself a workstation for every
 * cooking recipe it can run.
 *
 * <p>The scavenged foods: bat wings, spider legs, tentacles and the rest come from loot modifiers
 * with conditions no viewer reads — most want a Farmer's Delight knife in hand, and each has a
 * cooked form you get by killing the mob while it is on fire. Those conditions are the recipe.
 */
public final class MinersDelightEmiModule {

    private static final String MOD = "minersdelight";

    private MinersDelightEmiModule() {}

    public static void register(EmiRegistry reg) {
        copperPot(reg);
        scavenging(reg);
    }

    /**
     * The pot runs Farmer's Delight cooking recipes, so it belongs on that category as a workstation.
     * Recipes whose result converts to a cup get a second entry under the pot's own category, because
     * the cup is a different item and nothing else in the game points at it.
     */
    private static void copperPot(EmiRegistry reg) {
        EmiStack pot = Categories.stack(MOD + ":copper_pot");
        if (pot.isEmpty()) {
            return;
        }
        reg.addWorkstation(FDRecipeCategories.COOKING, pot);

        EmiRecipeCategory cups = Categories.machine(reg, "minersdelight_copper_pot", MOD + ":copper_pot", "Copper Pot");
        Recipes.forEach(reg.getRecipeManager(), CookingPotRecipe.class, (id, recipe) -> {
            ItemStack bowl = recipe.getResultItem(Minecraft.getInstance().level.registryAccess());
            ItemStack cup = CupConversionDataMap.getCupVariant(bowl).orElse(ItemStack.EMPTY);
            if (cup.isEmpty()) {
                return;
            }
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ingredient : recipe.getIngredients()) {
                d.itemInMerged(EmiIngredient.of(ingredient));
            }
            d.itemOut(EmiStack.of(cup));
            d.info(Component.literal("Served in a cup, not a bowl, because the Copper Pot cooked it"));
            if (recipe.getCookTime() > 0) {
                d.info(Component.literal(Categories.seconds(recipe.getCookTime())));
            }
            reg.addRecipe(new GenericEmiRecipe(cups, cupId(id), d));
        });
    }

    /** Distinct from the cooking recipe's own id, which already names an entry in EMI's Cooking tab. */
    private static ResourceLocation cupId(ResourceLocation cooking) {
        return ResourceLocation.fromNamespaceAndPath(
                "bertieemi", "minersdelight/copper_pot/" + cooking.getNamespace() + "/" + cooking.getPath());
    }

    /**
     * The scavenged foods are butchering drops and now live on their mobs' own pages in Advanced
     * Loot Info, which renders the knife and the fire the way it renders every other condition. Only
     * the wild crop is left here: it is placed by worldgen rather than dropped, so no mob page could
     * ever carry it.
     */
    private static void scavenging(EmiRegistry reg) {
        InfoPages.page(
                reg,
                MOD + "/wild_cave_carrots",
                List.of(MOD + ":wild_cave_carrots"),
                "Generates underground in patches, alongside cave air.",
                "Break it for Cave Carrots.");
    }
}
