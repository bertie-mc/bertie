package com.berlord.foodsystem.item;

import com.berlord.foodsystem.BFS;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Shapeless: one Eternal Seasoning + one (non-eternal) food item -> the same food,
 * tagged with the eternal component. Eaten eternal food never leaves the stomach.
 */
public class EternalSeasoningRecipe extends CustomRecipe {

    public EternalSeasoningRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack seasoning = ItemStack.EMPTY;
        ItemStack food = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.is(BFS.ETERNAL_SEASONING.get())) {
                if (!seasoning.isEmpty()) return false;
                seasoning = stack;
            } else if (stack.has(DataComponents.FOOD) && !stack.has(BFS.ETERNAL.get())) {
                if (!food.isEmpty()) return false;
                food = stack;
            } else {
                return false;
            }
        }
        return !seasoning.isEmpty() && !food.isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider provider) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty() && stack.has(DataComponents.FOOD) && !stack.is(BFS.ETERNAL_SEASONING.get())) {
                ItemStack out = stack.copyWithCount(1);
                out.set(BFS.ETERNAL.get(), Unit.INSTANCE);
                return out;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return BFS.ETERNAL_SEASONING_SERIALIZER.get();
    }
}
