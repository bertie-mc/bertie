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

/**
 * Brews Better End's Potion of End Veil from Ender Pearl Dust.
 *
 * <p>Better End's own brew reads Ender Dust, which the pack has removed along with the anvils that
 * beat it out of an ender pearl. The effect is worth keeping, so the same potion is registered a
 * second time against EnderIO's Ender Pearl Dust. The mod's own mix stays registered and simply has
 * no reachable reagent; the redstone extension to the lasting variant is left alone and applies to
 * whichever of the two produced the potion.
 */
public final class EndVeilBrewingHandler {

    private EndVeilBrewingHandler() {}

    private static final ResourceKey<Potion> END_VEIL =
            ResourceKey.create(Registries.POTION, ResourceLocation.parse("betterend:end_veil"));

    private static final ResourceLocation ENDER_PEARL_DUST =
            ResourceLocation.parse("enderio:powdered_ender_pearl");

    @SubscribeEvent
    public static void onRegisterBrewingRecipes(RegisterBrewingRecipesEvent event) {
        // Both mods are optional here: without either one there is nothing to bridge.
        Optional<Holder.Reference<Potion>> veil = BuiltInRegistries.POTION.getHolder(END_VEIL);
        Optional<Holder.Reference<Item>> dust = BuiltInRegistries.ITEM.getHolder(
                ResourceKey.create(Registries.ITEM, ENDER_PEARL_DUST));
        if (veil.isEmpty() || dust.isEmpty()) {
            return;
        }
        event.getBuilder().addMix(Potions.AWKWARD, dust.get().value(), veil.get());
    }
}
