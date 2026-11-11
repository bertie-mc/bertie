package io.github.bertie_mc.bertieprogression.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

/** Matches the required book level while allowing unrelated enchantments on the same book. */
public record EnchantedBookIngredient(Holder<Enchantment> enchantment, int level) implements ICustomIngredient {
    public static final MapCodec<EnchantedBookIngredient> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                            RegistryFixedCodec.create(Registries.ENCHANTMENT)
                                    .fieldOf("enchantment")
                                    .forGetter(EnchantedBookIngredient::enchantment),
                            Codec.intRange(1, 255).fieldOf("level").forGetter(EnchantedBookIngredient::level))
                    .apply(instance, EnchantedBookIngredient::new));

    @Override
    public boolean test(ItemStack stack) {
        return stack.is(Items.ENCHANTED_BOOK)
                && stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY)
                                .getLevel(enchantment)
                        == level;
    }

    @Override
    public Stream<ItemStack> getItems() {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        var enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        enchantments.set(enchantment, level);
        book.set(DataComponents.STORED_ENCHANTMENTS, enchantments.toImmutable());
        return Stream.of(book);
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return ModRecipes.ENCHANTED_BOOK.get();
    }
}
