package io.github.bertie_mc.bertieprogression.recipe;

import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;

/** Reuses the Charge Mix blasting recipe only in electric-furnace lookups. */
public final class ElectricSmelting {
    public static final ResourceLocation SOURCE =
            ResourceLocation.parse("bertieprogression:silicon_from_blasting_charge_mix");

    public static Optional<RecipeHolder<SmeltingRecipe>> find(
            RecipeManager recipes, SingleRecipeInput input, Level level) {
        return recipes.byKey(SOURCE)
                .filter(holder -> holder.value() instanceof BlastingRecipe recipe && recipe.matches(input, level))
                .map(holder -> convert((BlastingRecipe) holder.value(), level.registryAccess()));
    }

    public static RecipeHolder<SmeltingRecipe> convert(BlastingRecipe source, HolderLookup.Provider registries) {
        return new RecipeHolder<>(
                SOURCE,
                new SmeltingRecipe(
                        source.getGroup(),
                        CookingBookCategory.MISC,
                        source.getIngredients().getFirst(),
                        source.getResultItem(registries).copy(),
                        source.getExperience(),
                        source.getCookingTime()));
    }

    private ElectricSmelting() {}
}
