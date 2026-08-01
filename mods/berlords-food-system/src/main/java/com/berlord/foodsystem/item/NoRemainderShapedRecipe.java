package com.berlord.foodsystem.item;

import com.berlord.foodsystem.BFS;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

/**
 * Shaped recipe that never leaves crafting remainders in the grid
 * (e.g. dragon's breath normally returns its glass bottle).
 */
public class NoRemainderShapedRecipe extends ShapedRecipe {
    // ShapedRecipe's own fields are package-private; keep copies for the codecs
    private final String groupCopy;
    private final CraftingBookCategory categoryCopy;
    private final ShapedRecipePattern patternCopy;
    private final ItemStack resultCopy;
    private final boolean showNotificationCopy;

    public NoRemainderShapedRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern,
                                   ItemStack result, boolean showNotification) {
        super(group, category, pattern, result, showNotification);
        this.groupCopy = group;
        this.categoryCopy = category;
        this.patternCopy = pattern;
        this.resultCopy = result;
        this.showNotificationCopy = showNotification;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        return NonNullList.withSize(input.size(), ItemStack.EMPTY);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return BFS.SHAPED_NO_REMAINDER_SERIALIZER.get();
    }

    public static class Serializer implements RecipeSerializer<NoRemainderShapedRecipe> {
        private static final MapCodec<NoRemainderShapedRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.groupCopy),
                CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC).forGetter(r -> r.categoryCopy),
                ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.patternCopy),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.resultCopy),
                Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(r -> r.showNotificationCopy)
        ).apply(inst, NoRemainderShapedRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, NoRemainderShapedRecipe> STREAM_CODEC = StreamCodec.of(
                (buf, r) -> {
                    buf.writeUtf(r.groupCopy);
                    buf.writeEnum(r.categoryCopy);
                    ShapedRecipePattern.STREAM_CODEC.encode(buf, r.patternCopy);
                    ItemStack.STREAM_CODEC.encode(buf, r.resultCopy);
                    buf.writeBoolean(r.showNotificationCopy);
                },
                buf -> new NoRemainderShapedRecipe(
                        buf.readUtf(),
                        buf.readEnum(CraftingBookCategory.class),
                        ShapedRecipePattern.STREAM_CODEC.decode(buf),
                        ItemStack.STREAM_CODEC.decode(buf),
                        buf.readBoolean()));

        @Override
        public MapCodec<NoRemainderShapedRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, NoRemainderShapedRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
