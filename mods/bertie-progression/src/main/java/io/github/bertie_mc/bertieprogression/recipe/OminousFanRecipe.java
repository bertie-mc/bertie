package io.github.bertie_mc.bertieprogression.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * One item in, one item out, when an Encased Fan blows through Twilight Forest's Ominous Fire.
 *
 * <p>A real recipe rather than a hard-coded table, so it is data-driven and a recipe viewer can be
 * pointed at it. Deliberately minimal - no processing time, no chance outputs, no multi-output -
 * because it exists to prove the fifth fan-processing type works.
 */
public record OminousFanRecipe(Ingredient input, ItemStack result) implements Recipe<SingleRecipeInput> {

    @Override
    public boolean matches(SingleRecipeInput in, Level level) {
        return input.test(in.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput in, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(input);
        return list;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.OMINOUS_FAN.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.OMINOUS_FAN_TYPE.get();
    }

    public static final class Serializer implements RecipeSerializer<OminousFanRecipe> {

        private static final MapCodec<OminousFanRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                        Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(OminousFanRecipe::input),
                        ItemStack.CODEC.fieldOf("result").forGetter(OminousFanRecipe::result))
                .apply(i, OminousFanRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, OminousFanRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        Ingredient.CONTENTS_STREAM_CODEC,
                        OminousFanRecipe::input,
                        ItemStack.STREAM_CODEC,
                        OminousFanRecipe::result,
                        OminousFanRecipe::new);

        @Override
        public MapCodec<OminousFanRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, OminousFanRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
