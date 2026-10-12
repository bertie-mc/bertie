package io.github.bertie_mc.bertieprogression.recipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import net.mehvahdjukaar.moonlight.api.set.wood.WoodTypeRegistry;
import net.mehvahdjukaar.sawmill.WoodcuttingRecipe;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.StonecutterRecipe;

/** Separates wooden cutting after Sawmill has produced its automatic recipes. */
public final class CuttingRecipeRules {
    private CuttingRecipeRules() {}

    public static List<RecipeHolder<?>> separate(
            Collection<RecipeHolder<?>> recipes, HolderLookup.Provider registries) {
        List<RecipeHolder<?>> result = new ArrayList<>();
        List<WoodcuttingRecipe> wood = recipes.stream()
                .map(RecipeHolder::value)
                .filter(WoodcuttingRecipe.class::isInstance)
                .map(WoodcuttingRecipe.class::cast)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        for (RecipeHolder<?> holder : recipes) {
            if (!(holder.value() instanceof StonecutterRecipe stone)) {
                result.add(holder);
                continue;
            }
            ItemStack[] inputs = stone.getIngredients().getFirst().getItems();
            List<ItemStack> wooden =
                    Arrays.stream(inputs).filter(CuttingRecipeRules::isWood).toList();
            if (wooden.isEmpty()) {
                result.add(holder);
                continue;
            }
            ItemStack output = stone.getResultItem(registries);
            List<ItemStack> remainingStone =
                    Arrays.stream(inputs).filter(input -> !isWood(input)).toList();
            if (!remainingStone.isEmpty()) {
                result.add(new RecipeHolder<>(
                        holder.id(),
                        new StonecutterRecipe(
                                stone.getGroup(), Ingredient.of(remainingStone.stream()), output.copy())));
            }
            List<ItemStack> uncovered = wooden.stream()
                    .filter(input -> wood.stream()
                            .noneMatch(existing ->
                                    existing.getIngredients().getFirst().test(input)
                                            && ItemStack.isSameItemSameComponents(
                                                    existing.getResultItem(registries), output)))
                    .toList();
            boolean covered = uncovered.isEmpty();
            if (!covered) {
                Ingredient ingredient = uncovered.size() == inputs.length
                        ? stone.getIngredients().getFirst()
                        : Ingredient.of(uncovered.stream());
                WoodcuttingRecipe moved = new WoodcuttingRecipe(stone.getGroup(), ingredient, output.copy(), 1);
                wood.add(moved);
                ResourceLocation id = remainingStone.isEmpty()
                        ? holder.id()
                        : ResourceLocation.fromNamespaceAndPath(
                                "bertieprogression",
                                "woodcutting/" + holder.id().getNamespace() + "/"
                                        + holder.id().getPath());
                result.add(new RecipeHolder<>(id, moved));
            }
        }
        return result;
    }

    private static boolean isWood(ItemStack stack) {
        return stack.is(ItemTags.PLANKS)
                || stack.is(ItemTags.LOGS)
                || WoodTypeRegistry.INSTANCE.getBlockTypeOf(stack.getItem()) != null;
    }
}
