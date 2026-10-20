package io.github.bertie_mc.bertieprogression.gametest;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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

    @GameTest(template = "empty")
    public static void lavaVisionBrewsExtendsAndSplashes(GameTestHelper h) {
        var brewing = h.getLevel().potionBrewing();
        var normal = BuiltInRegistries.POTION.getHolderOrThrow(
                ResourceKey.create(Registries.POTION, ResourceLocation.parse("alexsmobs:lava_vision")));
        var extended = BuiltInRegistries.POTION.getHolderOrThrow(
                ResourceKey.create(Registries.POTION, ResourceLocation.parse("alexsmobs:long_lava_vision")));
        ItemStack bottle = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("alexsmobs:lava_bottle")));
        ItemStack tooth =
                new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("alexsmobs:bone_serpent_tooth")));
        h.assertTrue(brewing.hasMix(bottle, tooth), "the lava bottle must accept Bone Serpent Tooth");
        ItemStack potion = brewing.mix(tooth, bottle);
        h.assertTrue(
                potion.is(Items.POTION)
                        && potion.get(DataComponents.POTION_CONTENTS)
                                .potion()
                                .orElseThrow()
                                .is(normal),
                "tooth must produce Lava Vision");
        h.assertTrue(
                normal.value().getEffects().getFirst().getDuration() == 3600, "normal duration must be three minutes");
        ItemStack longer = brewing.mix(new ItemStack(Items.REDSTONE), potion);
        h.assertTrue(
                longer.get(DataComponents.POTION_CONTENTS)
                        .potion()
                        .orElseThrow()
                        .is(extended),
                "redstone must extend Lava Vision");
        h.assertTrue(
                extended.value().getEffects().getFirst().getDuration() == 9600, "long duration must be eight minutes");
        for (ItemStack input : new ItemStack[] {potion, longer}) {
            h.assertTrue(brewing.hasMix(input, new ItemStack(Items.GUNPOWDER)), "both durations must accept gunpowder");
            ItemStack splash = brewing.mix(new ItemStack(Items.GUNPOWDER), input);
            h.assertTrue(splash.is(Items.SPLASH_POTION), "gunpowder must produce a splash potion");
            h.assertTrue(
                    splash.get(DataComponents.POTION_CONTENTS).equals(input.get(DataComponents.POTION_CONTENTS)),
                    "splash conversion must retain potion duration");
        }
        h.succeed();
    }
}
