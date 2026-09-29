package io.github.bertie_mc.fletching;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.CustomData;
import org.junit.jupiter.api.Test;

class PotionTankTest {
    @Test
    void tooltipShowsItemEffectAndPotionTogether() {
        PotionTank tank = new PotionTank();
        tank.fill(poison(Items.POTION));
        ItemStack arrows = ArrowRecipe.craft(
                new ItemStack(Items.FEATHER),
                new ItemStack(Items.STICK),
                new ItemStack(Items.FLINT),
                new ItemStack(Items.TNT),
                tank);
        java.util.List<net.minecraft.network.chat.Component> lines = new java.util.ArrayList<>();
        arrows.getItem()
                .appendHoverText(arrows, Item.TooltipContext.EMPTY, lines, net.minecraft.world.item.TooltipFlag.NORMAL);
        String text = lines.toString().toLowerCase(java.util.Locale.ROOT);
        assertTrue(text.contains("tnt"), text);
        assertTrue(text.contains("poison"), text);
    }

    static ItemStack poison(Item item) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.POISON));
        return stack;
    }

    @Test
    void formsShareLiquidButDifferentStrengthsDoNotMix() {
        PotionTank tank = new PotionTank();
        assertTrue(tank.fill(poison(Items.POTION)));
        assertTrue(tank.fill(poison(Items.SPLASH_POTION)));
        assertTrue(tank.fill(poison(Items.LINGERING_POTION)));
        assertEquals(3, tank.batches());
        ItemStack strong = poison(Items.POTION);
        strong.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.STRONG_POISON));
        assertFalse(tank.fill(strong));
        assertEquals(3, tank.batches());
        assertFalse(tank.fill(new ItemStack(Items.DIAMOND)));
    }

    @Test
    void capacityNeverWastesPartOfABottle() {
        PotionTank tank = new PotionTank();
        for (int i = 0; i < 8; i++) assertTrue(tank.fill(poison(Items.POTION)));
        assertFalse(tank.fill(poison(Items.POTION)));
        tank.consume();
        assertTrue(tank.fill(poison(Items.POTION)));
        for (int i = 0; i < 8; i++) tank.consume();
        assertTrue(tank.displayStack().isEmpty());
        assertEquals(0, tank.batches());
        ItemStack other = poison(Items.POTION);
        other.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.SWIFTNESS));
        assertTrue(tank.fill(other));
    }

    @Test
    void combinedArrowsKeepBothPropertiesAndAlwaysProduceEight() {
        PotionTank tank = new PotionTank();
        tank.fill(poison(Items.POTION));
        for (var part : PartCatalog.inSlot(3)) {
            if (part.stack().isEmpty()) continue;
            Item effect = part.stack().getItem();
            ItemStack arrows = ArrowRecipe.craft(
                    new ItemStack(Items.FEATHER),
                    new ItemStack(Items.STICK),
                    new ItemStack(Items.FLINT),
                    new ItemStack(effect),
                    tank);
            assertEquals(8, arrows.getCount());
            var data = arrows.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                    .copyTag();
            assertFalse(data.getString("effect").isEmpty());
            assertTrue(data.getBoolean("isPotionEffect"));
            assertTrue(data.contains("bertiePotionContents"));
            assertEquals(
                    "minecraft:poison",
                    data.getList("potionEffects", 10).getCompound(0).getString("id"));
            if (effect == Items.TNT) assertTrue(ArrowProfile.read(data).extra("tnt"));
        }
        for (ItemStack effect :
                new ItemStack[] {ItemStack.EMPTY, new ItemStack(Items.HONEYCOMB), new ItemStack(Items.TNT)}) {
            assertEquals(
                    8,
                    ArrowRecipe.craft(
                                    new ItemStack(Items.FEATHER),
                                    new ItemStack(Items.STICK),
                                    new ItemStack(Items.FLINT),
                                    effect,
                                    new PotionTank())
                            .getCount());
        }
    }
}
