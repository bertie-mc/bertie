package io.github.bertie_mc.bertieprogression.emi;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import io.github.bertie_mc.bertieprogression.recipe.ElectricSmelting;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.BlastingRecipe;

final class ElectricSmeltingEmi {
    static void register(EmiRegistry registry) {
        registry.getRecipeManager().byKey(ElectricSmelting.SOURCE).ifPresent(holder -> {
            if (!(holder.value() instanceof BlastingRecipe recipe)) {
                return;
            }
            var furnace =
                    EmiStack.of(BuiltInRegistries.ITEM.get(ResourceLocation.parse("oritech:powered_furnace_block")));
            var category = new InWorldEmiCategory(
                    ResourceLocation.parse("bertieprogression:electric_smelting"),
                    furnace,
                    Component.literal("Electric Smelting"));
            registry.addCategory(category);
            registry.addWorkstation(category, furnace);
            registry.addRecipe(new InWorldEmiRecipe(
                    category,
                    ResourceLocation.parse("bertieprogression:electric_smelting/oritech_charge_mix"),
                    List.of(EmiIngredient.of(recipe.getIngredients().getFirst())),
                    List.of(),
                    List.of(EmiStack.of(
                            recipe.getResultItem(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)))),
                    List.of()));
        });
    }

    private ElectricSmeltingEmi() {}
}
