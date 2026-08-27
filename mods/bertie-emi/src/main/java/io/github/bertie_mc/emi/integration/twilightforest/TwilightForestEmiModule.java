package io.github.bertie_mc.emi.integration.twilightforest;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiIngredient;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import twilightforest.compat.emi.TFEmiCategories;
import twilightforest.compat.emi.recipes.EmiDryingRecipe;
import twilightforest.item.recipe.DryingRecipe;

/**
 * Twilight Forest ships its own EMI plugin, so this only patches the two gaps in the Drying Rack's
 * half of it. Nothing else in the mod is touched.
 *
 * <p>First, that plugin registers the Drying category and its recipes but - alone among its five
 * categories - never calls {@code addWorkstation} for it. The tab therefore exists while no drying
 * rack opens it, which is how a rack in hand looks like a block with nothing to make.
 *
 * <p>Second, it skips the one drying recipe that produces Stale Bread. Nothing else in the pack makes
 * Stale Bread, so hiding its only source leaves the item with no source at all. It is reinstated here
 * through the mod's own recipe widget, and the original is removed first so that exactly one entry
 * shows however a future Twilight Forest decides to treat it.
 */
public final class TwilightForestEmiModule {

    /** Every rack variant the mod recognises, so one workstation slot cycles through all of them. */
    private static final TagKey<Item> DRYING_RACKS =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("twilightforest", "drying_racks"));

    private static final ResourceLocation STALE_BREAD =
            ResourceLocation.fromNamespaceAndPath("twilightforest", "stale_bread");

    private TwilightForestEmiModule() {}

    public static void register(EmiRegistry reg) {
        reg.addWorkstation(TFEmiCategories.DRYING, EmiIngredient.of(DRYING_RACKS));
        reinstateStaleBread(reg);
    }

    private static void reinstateStaleBread(EmiRegistry reg) {
        Item staleBread = BuiltInRegistries.ITEM.get(STALE_BREAD);
        if (staleBread == Items.AIR) {
            return;
        }
        for (RecipeHolder<?> holder : reg.getRecipeManager().getRecipes()) {
            if (!(holder.value() instanceof DryingRecipe drying)
                    || !drying.getResult().is(staleBread)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            RecipeHolder<DryingRecipe> typed = (RecipeHolder<DryingRecipe>) holder;
            ResourceLocation original = holder.id();
            reg.removeRecipes(
                    recipe -> recipe.getCategory() == TFEmiCategories.DRYING && original.equals(recipe.getId()));
            reg.addRecipe(new ReinstatedDrying(typed));
            return;
        }
    }

    /**
     * The mod's own drying widget under an id of ours. It has to differ from the recipe id: the
     * removal above is applied to the whole baked recipe list, so an entry keeping the original id
     * would be taken out again along with the one it replaces.
     */
    private static final class ReinstatedDrying extends EmiDryingRecipe {
        private final ResourceLocation id;

        private ReinstatedDrying(RecipeHolder<DryingRecipe> holder) {
            super(holder);
            this.id = ResourceLocation.fromNamespaceAndPath(
                    "bertieemi", "twilightforest/drying/" + holder.id().getPath());
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }
    }
}
