package com.berlord.endereyes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(EphemeralTestServerProvider.class)
class EnderEyesTest {
    @Test
    void enchantedHelmetSuppressesAnger(MinecraftServer server) {
        assertTrue(EnderEyes.protectsFromEnderman(
                server.registryAccess(), enchantedHelmet(server)));
    }

    @Test
    void unenchantedHelmetDoesNotSuppressAnger(MinecraftServer server) {
        assertFalse(EnderEyes.protectsFromEnderman(
                server.registryAccess(), new ItemStack(Items.DIAMOND_HELMET)));
    }

    @Test
    void emptyHeadSlotDoesNotSuppressAnger(MinecraftServer server) {
        assertFalse(EnderEyes.protectsFromEnderman(
                server.registryAccess(), ItemStack.EMPTY));
    }

    private static ItemStack enchantedHelmet(MinecraftServer server) {
        Holder<Enchantment> enchantment = server.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(EnderEyes.ENDER_EYES_KEY);
        ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
        helmet.enchant(enchantment, 1);
        return helmet;
    }
}
