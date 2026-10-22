package io.github.bertie_mc.emi.integration.enderio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enderio.enderio.content.enchanter.EnchanterRecipe;
import com.enderio.enderio.content.machines.obelisks.weather.WeatherChangeRecipe;
import com.enderio.enderio.content.storage.fluid_tank.TankRecipe;
import com.enderio.enderio.foundation.souldata.EngineSoul;
import dev.emi.emi.api.stack.EmiStack;
import io.github.bertie_mc.emi.framework.MachineDescriptor;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.junit.jupiter.api.Test;

class EnderIOExtraRecipesTest {
    @Test
    void fillingConsumesFluidInsteadOfProducingIt() {
        TankRecipe recipe = new TankRecipe(
                Ingredient.of(Items.GLASS_BOTTLE),
                new ItemStack(Items.POTION),
                SizedFluidIngredient.of(Fluids.WATER, 250),
                TankRecipe.Mode.FILL);

        MachineDescriptor display = EnderIOExtraRecipes.tank(recipe, FluidStack.EMPTY);

        assertEquals(1, display.fluidInputs.size());
        assertEquals(250, display.fluidInputs.getFirst().getAmount());
        assertTrue(display.fluidOutputs.isEmpty());
        assertEquals(
                Items.GLASS_BOTTLE,
                display.itemInputs
                        .getFirst()
                        .getEmiStacks()
                        .getFirst()
                        .getItemStack()
                        .getItem());
        assertEquals(Items.POTION, display.itemOutputs.getFirst().getItemStack().getItem());
    }

    @Test
    void emptyingProducesFluidAndReturnsTheContainer() {
        TankRecipe recipe = new TankRecipe(
                Ingredient.of(Items.WATER_BUCKET),
                new ItemStack(Items.BUCKET),
                SizedFluidIngredient.of(Fluids.WATER, 1_000),
                TankRecipe.Mode.EMPTY);

        MachineDescriptor display = EnderIOExtraRecipes.tank(recipe, new FluidStack(Fluids.WATER, 1_000));

        assertTrue(display.fluidInputs.isEmpty());
        assertEquals(1, display.fluidOutputs.size());
        assertEquals(1_000, display.fluidOutputs.getFirst().getAmount());
        assertEquals(Items.BUCKET, display.itemOutputs.getFirst().getItemStack().getItem());
    }

    @Test
    void enchantmentLevelScalesReagentsAndProducesTheMatchingBook() {
        Holder<Enchantment> enchantment = Holder.direct(Enchantment.enchantment(Enchantment.definition(
                        HolderSet.direct(Items.DIAMOND_SWORD.builtInRegistryHolder()),
                        1,
                        3,
                        Enchantment.dynamicCost(2, 5),
                        Enchantment.constantCost(30),
                        1,
                        EquipmentSlotGroup.MAINHAND))
                .build(ResourceLocation.withDefaultNamespace("emi_test")));
        EnchanterRecipe recipe = new EnchanterRecipe(enchantment, 1, SizedIngredient.of(Items.DIAMOND, 2));

        EmiStack lapis = EmiStack.of(Items.LAPIS_LAZULI);
        MachineDescriptor first = EnderIOExtraRecipes.enchanting(recipe, 1, lapis);
        MachineDescriptor third = EnderIOExtraRecipes.enchanting(recipe, 3, lapis);

        assertEquals(
                Items.WRITABLE_BOOK,
                first.itemInputs
                        .getFirst()
                        .getEmiStacks()
                        .getFirst()
                        .getItemStack()
                        .getItem());
        assertEquals(2, first.itemInputs.get(1).getAmount());
        assertEquals(6, third.itemInputs.get(1).getAmount());
        assertEquals(recipe.getLapisForLevel(3), third.itemInputs.get(2).getAmount());
        assertEquals(1, lapis.getAmount());
        assertEquals(
                1,
                first.itemOutputs
                        .getFirst()
                        .getItemStack()
                        .get(DataComponents.STORED_ENCHANTMENTS)
                        .getLevel(enchantment));
        assertEquals(
                3,
                third.itemOutputs
                        .getFirst()
                        .getItemStack()
                        .get(DataComponents.STORED_ENCHANTMENTS)
                        .getLevel(enchantment));
        assertTrue(third.info.getLast().getString().endsWith(" XP levels"));
    }

    @Test
    void engineRetainsItsBoundMachineAndReportsFuelYield() {
        EngineSoul.SoulData soul =
                new EngineSoul.SoulData(ResourceLocation.withDefaultNamespace("zombie"), "minecraft:water", 250, 4);
        MachineDescriptor display =
                EnderIOExtraRecipes.engine(soul, EmiStack.of(Fluids.WATER, 1_000), EmiStack.of(Items.STONE));

        assertEquals(1_000, display.fluidInputs.getFirst().getAmount());
        assertEquals(1, display.catalysts.size());
        assertTrue(display.itemInputs.isEmpty());
        assertEquals("250000 FE per bucket", display.info.get(1).getString());
        assertEquals("4 ticks per mB", display.info.getLast().getString());
    }

    @Test
    void weatherChangesConsumeTheSpecifiedFluidWithoutInventingAnItemResult() {
        WeatherChangeRecipe recipe =
                new WeatherChangeRecipe(new FluidStack(Fluids.WATER, 500), WeatherChangeRecipe.WeatherMode.RAIN);

        MachineDescriptor display = EnderIOExtraRecipes.weather(recipe);

        assertEquals(500, display.fluidInputs.getFirst().getAmount());
        assertTrue(display.itemOutputs.isEmpty());
        assertTrue(display.fluidOutputs.isEmpty());
        assertEquals("Weather: Rain", display.info.getFirst().getString());
    }
}
