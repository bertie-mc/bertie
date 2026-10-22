package io.github.bertie_mc.bertieprogression;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;

/** Brews Better End's Potion of End Veil from an Ender Pearl Fragment. */
public final class EndVeilBrewingHandler {

    private EndVeilBrewingHandler() {}

    private static final ResourceKey<Potion> END_VEIL =
            ResourceKey.create(Registries.POTION, ResourceLocation.parse("betterend:end_veil"));

    private static final ResourceLocation ENDER_PEARL_FRAGMENT =
            ResourceLocation.parse("forbidden_arcanus:ender_pearl_fragment");

    @SubscribeEvent
    public static void onRegisterBrewingRecipes(RegisterBrewingRecipesEvent event) {
        // Both mods are optional here: without either one there is nothing to bridge.
        Optional<Holder.Reference<Potion>> veil = BuiltInRegistries.POTION.getHolder(END_VEIL);
        Optional<Holder.Reference<Item>> fragment =
                BuiltInRegistries.ITEM.getHolder(ResourceKey.create(Registries.ITEM, ENDER_PEARL_FRAGMENT));
        if (veil.isEmpty() || fragment.isEmpty()) {
            return;
        }
        event.getBuilder().addMix(Potions.AWKWARD, fragment.get().value(), veil.get());
    }
}
