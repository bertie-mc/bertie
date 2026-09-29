package io.github.bertie_mc.bertieprogression.gametest;

import io.github.bertie_mc.bertieprogression.RemovedItems;
import io.github.bertie_mc.bertieprogression.combat.CombatRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.TippedArrowRecipe;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("bertieprogression")
@PrefixGameTestTemplate(false)
public final class RemovedPotionGameTests {
    @GameTest(template = "empty", batch = "combat")
    public static void corrosionBottleAndArrowVariantsAreHiddenAndUnbrewable(GameTestHelper helper) {
        var brewing = helper.getLevel().potionBrewing();
        for (String id : List.of("armor_corrosion", "long_armor_corrosion", "strong_armor_corrosion")) {
            var potion = BuiltInRegistries.POTION
                    .getHolder(ResourceLocation.fromNamespaceAndPath("l2complements", id))
                    .orElseThrow();
            for (var item : List.of(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION, Items.TIPPED_ARROW)) {
                ItemStack stack = PotionContents.createItemStack(item, potion);
                helper.assertTrue(RemovedItems.isRemovedPotion(stack), "all removed potion forms must be hidden");
                for (var reagent :
                        List.of(Items.REDSTONE, Items.GLOWSTONE_DUST, Items.GUNPOWDER, Items.DRAGON_BREATH)) {
                    helper.assertTrue(
                            !brewing.hasMix(stack, new ItemStack(reagent)),
                            "saved corrosion potions cannot be upgraded or converted");
                }
            }
        }
        helper.assertTrue(
                !brewing.hasMix(
                        PotionContents.createItemStack(Items.POTION, Potions.WEAKNESS),
                        new ItemStack(Items.MAGMA_CREAM)),
                "weakness recipe removed");
        helper.assertTrue(
                !brewing.hasMix(
                        PotionContents.createItemStack(Items.POTION, Potions.FIRE_RESISTANCE),
                        new ItemStack(Items.FERMENTED_SPIDER_EYE)),
                "fire resistance recipe removed");
        helper.assertTrue(
                !RemovedItems.isRemovedPotion(PotionContents.createItemStack(Items.TIPPED_ARROW, Potions.POISON)),
                "ordinary poison arrows remain available");
        var custom = new ItemStack(Items.SPLASH_POTION);
        custom.set(
                DataComponents.POTION_CONTENTS,
                new PotionContents(
                        Optional.empty(),
                        Optional.empty(),
                        List.of(new MobEffectInstance(
                                CombatRegistry.effect("l2complements:armor_corrosion")
                                        .orElseThrow(),
                                600))));
        helper.assertTrue(RemovedItems.isRemovedPotion(custom), "custom copies of the removed effect are hidden too");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "combat")
    public static void corrosionCannotBeAppliedOrRestoredFromASave(GameTestHelper helper) {
        var target = helper.spawn(EntityType.ZOMBIE, 1, 2, 1);
        target.setNoAi(true);
        target.getAttribute(Attributes.ARMOR).setBaseValue(20);
        var effect = CombatRegistry.effect("l2complements:armor_corrosion").orElseThrow();
        helper.assertTrue(!target.addEffect(new MobEffectInstance(effect, 600)), "ordinary application is blocked");
        target.forceAddEffect(new MobEffectInstance(effect, 600), null);
        helper.assertTrue(!target.hasEffect(effect), "forced application is blocked");
        // Recreate the serialized state of a world saved before removal.
        target.getActiveEffectsMap().put(effect, new MobEffectInstance(effect, 600));
        effect.value().addAttributeModifiers(target.getAttributes(), 0);
        CompoundTag tag = new CompoundTag();
        target.saveWithoutId(tag);
        var loaded = EntityType.ZOMBIE.create(helper.getLevel());
        loaded.load(tag);
        helper.assertTrue(!loaded.hasEffect(effect), "saved effect is cleared");
        helper.assertTrue(loaded.getArmorValue() == 20, "saved armor penalty is cleared");
        tag.remove("active_effects");
        loaded.load(tag);
        helper.assertTrue(loaded.getArmorValue() == 20, "orphaned saved armor penalty is cleared too");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "combat")
    public static void corrosionCannotBeCraftedIntoTippedArrows(GameTestHelper helper) {
        var potion = BuiltInRegistries.POTION
                .getHolder(ResourceLocation.parse("l2complements:armor_corrosion"))
                .orElseThrow();
        var slots = new ArrayList<ItemStack>();
        for (int i = 0; i < 9; i++) slots.add(new ItemStack(Items.ARROW));
        slots.set(4, PotionContents.createItemStack(Items.LINGERING_POTION, potion));
        var recipe = new TippedArrowRecipe(CraftingBookCategory.MISC);
        helper.assertTrue(
                !recipe.matches(CraftingInput.of(3, 3, slots), helper.getLevel()),
                "corrosion arrow recipe must not match");
        slots.set(4, PotionContents.createItemStack(Items.LINGERING_POTION, Potions.POISON));
        helper.assertTrue(
                recipe.matches(CraftingInput.of(3, 3, slots), helper.getLevel()),
                "other tipped arrows remain craftable");
        helper.succeed();
    }

    private RemovedPotionGameTests() {}
}
