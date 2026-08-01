package com.berlord.endereyes;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.EnderManAngerEvent;

/**
 * Ender Eyes — a clean-room NeoForge reimplementation of the BandarHL/EnderEyes
 * Fabric mod's confirmed core behavior.
 *
 * <p>The mod adds a single rare treasure enchantment, {@code endereyes:ender_eyes},
 * that applies only to helmets (HEAD slot) at max level 1. While the player wears a
 * helmet enchanted with it, looking directly at an Enderman no longer angers it.
 *
 * <p>The enchantment itself is fully data-driven
 * ({@code data/endereyes/enchantment/ender_eyes.json}); it is made treasure-only by
 * deliberately leaving it out of the {@code in_enchanting_table}/{@code tradeable}
 * tags and out of {@code non_treasure}.
 *
 * <p>The enchantment has no built-in acquisition path. Modpacks may add it to loot,
 * trades, or another progression system as appropriate.
 *
 * <p>The original Fabric mod implemented the anger suppression with a mixin on
 * {@code EnderMan#isLookingAtMe}. On NeoForge the dedicated {@link EnderManAngerEvent}
 * replaces that mixin entirely, so this mod ships zero mixins and zero binary assets.
 */
@Mod(EnderEyes.MOD_ID)
public class EnderEyes {
    public static final String MOD_ID = "endereyes";

    /**
     * Resource key for the data-driven enchantment. The actual {@link Enchantment}
     * definition lives in {@code data/endereyes/enchantment/ender_eyes.json}; this key
     * is only used to look the registered holder back up at runtime.
     */
    public static final ResourceKey<Enchantment> ENDER_EYES_KEY = ResourceKey.create(
            Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "ender_eyes"));

    public EnderEyes(IEventBus modBus) {
        // Register the anger-suppression handler on the NeoForge game event bus.
        NeoForge.EVENT_BUS.register(this);
    }

    /**
     * Cancels Enderman anger when the staring player is wearing a helmet enchanted
     * with Ender Eyes. {@link EnderManAngerEvent} is posted on the NeoForge game bus
     * just before an Enderman would become angry at a staring player; cancelling it
     * suppresses the targeting/anger entirely (mirrors the original mixin nulling
     * target/angryAt/attacking and returning {@code false}).
     */
    @SubscribeEvent
    public void onEnderManAnger(EnderManAngerEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (protectsFromEnderman(player.registryAccess(), helmet)) {
            event.setCanceled(true);
        }
    }

    static boolean protectsFromEnderman(RegistryAccess registries, ItemStack helmet) {
        if (helmet.isEmpty()) {
            return false;
        }

        Holder<Enchantment> enchantment = registries
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(ENDER_EYES_KEY);
        return EnchantmentHelper.getItemEnchantmentLevel(enchantment, helmet) > 0;
    }
}
