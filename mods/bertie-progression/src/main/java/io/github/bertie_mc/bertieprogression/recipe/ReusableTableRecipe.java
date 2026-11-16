package io.github.bertie_mc.bertieprogression.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import committee.nova.mods.avaritia.api.common.crafting.ShapedRecipePatternCodecs;
import committee.nova.mods.avaritia.api.common.crafting.TierInput;
import committee.nova.mods.avaritia.common.crafting.recipe.ShapedTableCraftingRecipe;
import io.github.bertie_mc.bertieprogression.BertieProgression;
import java.util.function.Supplier;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** An Avaritia table craft with a recipe-local, component-preserving catalyst. */
public final class ReusableTableRecipe extends ShapedTableCraftingRecipe {
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, BertieProgression.MODID);
    private static final Supplier<Serializer> SERIALIZER = SERIALIZERS.register("reusable_table", Serializer::new);
    private final Ingredient catalyst;

    public static void register(IEventBus bus) {
        SERIALIZERS.register(bus);
    }

    public ReusableTableRecipe(ShapedRecipePattern pattern, ItemStack result, int tier, Ingredient catalyst) {
        super(pattern, result, tier, false);
        this.catalyst = catalyst;
    }

    public boolean isCatalyst(ItemStack stack) {
        return catalyst.test(stack);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(TierInput input) {
        NonNullList<ItemStack> remaining = super.getRemainingItems(input);
        for (int slot = 0; slot < input.size(); slot++) {
            if (catalyst.test(input.getItem(slot))) {
                remaining.set(slot, input.getItem(slot).copyWithCount(1));
            }
        }
        return remaining;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER.get();
    }

    public static final class Serializer implements RecipeSerializer<ReusableTableRecipe> {
        private static final MapCodec<ReusableTableRecipe> CODEC =
                RecordCodecBuilder.mapCodec(instance -> instance.group(
                                ShapedRecipePatternCodecs.MAP_CODEC.forGetter(recipe -> recipe.pattern),
                                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
                                Codec.INT.fieldOf("tier").forGetter(recipe -> recipe.tier),
                                Ingredient.CODEC_NONEMPTY.fieldOf("catalyst").forGetter(recipe -> recipe.catalyst))
                        .apply(instance, ReusableTableRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, ReusableTableRecipe> STREAM = StreamCodec.composite(
                ShapedRecipePattern.STREAM_CODEC,
                recipe -> recipe.pattern,
                ItemStack.STREAM_CODEC,
                recipe -> recipe.result,
                ByteBufCodecs.VAR_INT,
                recipe -> recipe.tier,
                Ingredient.CONTENTS_STREAM_CODEC,
                recipe -> recipe.catalyst,
                ReusableTableRecipe::new);

        @Override
        public MapCodec<ReusableTableRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ReusableTableRecipe> streamCodec() {
            return STREAM;
        }
    }
}
