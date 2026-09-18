package io.github.bertie_mc.carving;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class CarvingContractTest {

    private static Set<CarvingMaterial> union(Set<CarvingMaterial> a, Set<CarvingMaterial> b) {
        EnumSet<CarvingMaterial> all = EnumSet.copyOf(a);
        all.addAll(b);
        return all;
    }

    @Test
    void networkOrdinalsRemainStableAndInvalidIndicesFallBack() {
        assertEquals(
                List.of(
                        "wood",
                        "stone",
                        "flint",
                        "bone",
                        "diamond",
                        "leather",
                        "copper",
                        "iron",
                        "golden",
                        "emerald",
                        "amethyst",
                        "lapis",
                        "quartz",
                        "obsidian",
                        "echo",
                        "deep_alloy",
                        "rose_gold",
                        "netherite",
                        "redstone",
                        "prismarine",
                        "heart_of_the_sea",
                        "turtle_scute",
                        "armadillo_scute",
                        "resin_brick",
                        "ancient_metal",
                        "black_steel",
                        "cursium",
                        "ignitium",
                        "witherite"),
                Arrays.stream(CarvingMaterial.values())
                        .map(material -> material.id)
                        .toList());
        assertSame(CarvingMaterial.WOOD, CarvingMaterial.byIndex(-1));
        assertSame(CarvingMaterial.WOOD, CarvingMaterial.byIndex(100));
        assertSame(ToolKind.PICKAXE, ToolKind.byIndex(-1));
        assertSame(ArmorKind.HELMET, ArmorKind.byIndex(100));
    }

    @Test
    void standaloneRegistrationExposesOnlyUsableSlates() {
        // Leather is here without a vanilla tool behind it: its small slate feeds the big one, so it
        // is usable on its own terms where flint and bone still need Slag to mean anything. The
        // slate-only materials are here too, minus the ones whose source item comes from a mod.
        Set<CarvingMaterial> vanillaSlateOnly = Set.of(
                CarvingMaterial.NETHERITE,
                CarvingMaterial.REDSTONE,
                CarvingMaterial.PRISMARINE,
                CarvingMaterial.HEART_OF_THE_SEA,
                CarvingMaterial.TURTLE_SCUTE,
                CarvingMaterial.ARMADILLO_SCUTE);
        assertEquals(
                union(
                        Set.of(
                                CarvingMaterial.WOOD,
                                CarvingMaterial.STONE,
                                CarvingMaterial.DIAMOND,
                                CarvingMaterial.LEATHER,
                                CarvingMaterial.IRON,
                                CarvingMaterial.GOLDEN),
                        vanillaSlateOnly),
                Carving.SMALL_SLATES.keySet());
        assertEquals(
                union(
                        Set.of(
                                CarvingMaterial.DIAMOND,
                                CarvingMaterial.LEATHER,
                                CarvingMaterial.IRON,
                                CarvingMaterial.GOLDEN),
                        vanillaSlateOnly),
                Carving.BIG_SLATES.keySet());
        assertFalse(Carving.usesSlag(CarvingMaterial.IRON));
    }

    @Test
    void vanillaResultsUseTheRequestedKindAndDurabilityPenalty() {
        ItemStack axe = Carving.resultStack(CarvingMaterial.WOOD, false, ToolKind.AXE.ordinal(), 2, 0);
        assertSame(Items.WOODEN_AXE, axe.getItem());
        assertEquals(Math.round(axe.getMaxDamage() * 0.50F), axe.getDamageValue());

        ItemStack chestplate = Carving.resultStack(CarvingMaterial.DIAMOND, true, ArmorKind.CHESTPLATE.ordinal(), 0, 1);
        assertSame(Items.DIAMOND_CHESTPLATE, chestplate.getItem());
        assertEquals(Math.round(chestplate.getMaxDamage() * 0.30F), chestplate.getDamageValue());
    }

    @Test
    void shapeKeysAlwaysSelectPartSilhouettes() {
        assertEquals("slag/pickaxe_head", Carving.shapeKey(CarvingMaterial.WOOD, false, ToolKind.PICKAXE.ordinal()));
        assertEquals("slag/boots", Carving.shapeKey(CarvingMaterial.DIAMOND, true, ArmorKind.BOOTS.ordinal()));
    }
}
