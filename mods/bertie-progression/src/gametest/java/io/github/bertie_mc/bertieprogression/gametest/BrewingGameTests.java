package io.github.bertie_mc.bertieprogression.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("bertieprogression")
@PrefixGameTestTemplate(false)
public final class BrewingGameTests {
    @GameTest(template = "empty")
    public static void nothingBrewsMundanePotion(GameTestHelper helper) {
        var brewing = helper.getLevel().potionBrewing();
        ItemStack water = PotionContents.createItemStack(Items.POTION, Potions.WATER);
        ItemStack awkward = PotionContents.createItemStack(Items.POTION, Potions.AWKWARD);
        helper.assertTrue(
                !brewing.hasMix(water, new ItemStack(Items.REDSTONE)), "redstone must not turn water mundane");
        helper.assertTrue(
                !brewing.hasMix(water, new ItemStack(Items.SUGAR)), "a starting reagent must not turn water mundane");
        helper.assertTrue(
                brewing.hasMix(water, new ItemStack(Items.NETHER_WART)), "water must still brew into awkward");
        helper.assertTrue(
                brewing.hasMix(awkward, new ItemStack(Items.SUGAR)), "starting reagents must still work on awkward");
        helper.succeed();
    }

    private BrewingGameTests() {}
}
