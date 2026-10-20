package io.github.bertie_mc.bertieprogression;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.brewing.BrewingRecipe;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;

public final class LavaVisionBrewingHandler {
    private LavaVisionBrewingHandler() {}

    @SubscribeEvent
    public static void register(RegisterBrewingRecipesEvent event) {
        var normal = BuiltInRegistries.POTION.getHolder(
                ResourceKey.create(Registries.POTION, ResourceLocation.parse("alexsmobs:lava_vision")));
        var extended = BuiltInRegistries.POTION.getHolder(
                ResourceKey.create(Registries.POTION, ResourceLocation.parse("alexsmobs:long_lava_vision")));
        Item bottle = BuiltInRegistries.ITEM.get(ResourceLocation.parse("alexsmobs:lava_bottle"));
        Item tooth = BuiltInRegistries.ITEM.get(ResourceLocation.parse("alexsmobs:bone_serpent_tooth"));
        if (normal.isEmpty() || extended.isEmpty() || bottle == Items.AIR || tooth == Items.AIR) {
            return;
        }
        ItemStack output = new ItemStack(Items.POTION);
        output.set(DataComponents.POTION_CONTENTS, new PotionContents(normal.get()));
        event.getBuilder().addRecipe(new BrewingRecipe(Ingredient.of(bottle), Ingredient.of(tooth), output));
        event.getBuilder().addMix(normal.get(), Items.REDSTONE, extended.get());
        // Vanilla's container mix applies gunpowder to both registered potion durations.
    }
}
