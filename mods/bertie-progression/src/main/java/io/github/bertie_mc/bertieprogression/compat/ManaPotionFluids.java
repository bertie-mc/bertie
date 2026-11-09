package io.github.bertie_mc.bertieprogression.compat;

import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;

public final class ManaPotionFluids {
    public static final ResourceLocation TAG = ResourceLocation.parse("bertieprogression:mana_potion");
    public static final ResourceKey<Potion> POTION = ResourceKey.create(
            Registries.POTION, ResourceLocation.parse("irons_spellbooks:instant_mana_one"));

    public static boolean matches(DataComponentHolder stack) {
        var contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents != null && contents.potion().map(potion -> potion.is(POTION)).orElse(false);
    }

    public static void decoratePreview(Object stack) {
        var potion = BuiltInRegistries.POTION.getHolder(POTION);
        if (potion.isEmpty()) {
            return;
        }
        try {
            // Architectury's optional fluid API exposes the same component setter as ItemStack.
            stack.getClass()
                    .getMethod("set", DataComponentType.class, Object.class)
                    .invoke(stack, DataComponents.POTION_CONTENTS, new PotionContents(potion.get()));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot apply Mana Potion contents to the fluid preview", e);
        }
    }

    private ManaPotionFluids() {}
}
