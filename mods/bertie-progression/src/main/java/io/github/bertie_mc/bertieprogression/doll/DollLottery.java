package io.github.bertie_mc.bertieprogression.doll;

import io.github.bertie_mc.bertieprogression.BertieProgression;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * What a Doll Machine gift box holds. The machine's own shuffled pools contain only mob dolls
 * ({@code #bertieprogression:mob_dolls}); a yellow or purple box swaps its draw for a special doll
 * part of the time.
 */
public final class DollLottery {
    /** Special dolls that are ordinary registered items. */
    public static final TagKey<Item> SPECIAL_DOLLS =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath(BertieProgression.MODID, "special_dolls"));

    private static final ResourceLocation CUSTOM_DOLL =
            ResourceLocation.fromNamespaceAndPath("kaleidoscope_doll", "custom_doll");
    private static final ResourceLocation CUSTOM_DOLL_ID =
            ResourceLocation.fromNamespaceAndPath("kaleidoscope_doll", "custom_doll_id");

    /**
     * Custom-model dolls are one item told apart by a model-id component, so no tag can name them.
     * The empty id is the Unknown Doll, a custom doll with no model set.
     */
    private static final List<String> CUSTOM_MODELS =
            List.of("", "geometry.little_winefox", "geometry.normal_winefox", "geometry.zhiban", "geometry.hailuo");

    /** Chance that a box holds a special doll, by machine tier: green, yellow, purple. */
    private static final float[] SPECIAL_CHANCE = {0.0F, 0.10F, 0.50F};

    public static ItemStack draw(ItemStack mobDoll, int tier, RandomSource random) {
        if (mobDoll.isEmpty()
                || tier < 0
                || tier >= SPECIAL_CHANCE.length
                || random.nextFloat() >= SPECIAL_CHANCE[tier]) {
            return mobDoll;
        }
        List<Item> tagged = new ArrayList<>();
        BuiltInRegistries.ITEM.getTagOrEmpty(SPECIAL_DOLLS).forEach(holder -> tagged.add(holder.value()));
        int pick = random.nextInt(tagged.size() + CUSTOM_MODELS.size());
        if (pick < tagged.size()) {
            return new ItemStack(tagged.get(pick));
        }
        ItemStack custom = customDoll(CUSTOM_MODELS.get(pick - tagged.size()));
        return custom.isEmpty() ? mobDoll : custom;
    }

    private static ItemStack customDoll(String model) {
        Item item = BuiltInRegistries.ITEM.get(CUSTOM_DOLL);
        if (item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item);
        DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(CUSTOM_DOLL_ID);
        if (!model.isEmpty() && type != null) {
            setComponent(stack, type, model);
        }
        return stack;
    }

    @SuppressWarnings("unchecked")
    private static <T> void setComponent(ItemStack stack, DataComponentType<T> type, Object value) {
        stack.set(type, (T) value);
    }

    private DollLottery() {}
}
