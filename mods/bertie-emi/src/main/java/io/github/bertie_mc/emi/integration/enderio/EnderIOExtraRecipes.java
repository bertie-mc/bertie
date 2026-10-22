package io.github.bertie_mc.emi.integration.enderio;

import com.enderio.enderio.api.soul.Soul;
import com.enderio.enderio.content.enchanter.EnchanterRecipe;
import com.enderio.enderio.content.machines.obelisks.weather.WeatherChangeRecipe;
import com.enderio.enderio.content.storage.fluid_tank.TankRecipe;
import com.enderio.enderio.foundation.souldata.EngineSoul;
import com.enderio.enderio.init.EIODataComponents;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.neoforge.NeoForgeEmiIngredient;
import dev.emi.emi.api.neoforge.NeoForgeEmiStack;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import io.github.bertie_mc.emi.framework.Categories;
import io.github.bertie_mc.emi.framework.GenericEmiRecipe;
import io.github.bertie_mc.emi.framework.MachineDescriptor;
import io.github.bertie_mc.emi.framework.Recipes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidStack;

/** Processes whose displays depend on fluid direction, enchantment level, or synced soul data. */
final class EnderIOExtraRecipes {
    private EnderIOExtraRecipes() {}

    static void register(EmiRegistry reg) {
        RecipeManager recipes = reg.getRecipeManager();
        EmiRecipeCategory tank = Categories.machine(reg, "enderio_tank", "enderio:fluid_tank", "Fluid Tank");
        reg.addWorkstation(tank, Categories.stack("enderio:pressurized_fluid_tank"));
        Recipes.forEach(recipes, TankRecipe.class, (id, recipe) -> {
            if (recipe.mode() == TankRecipe.Mode.FILL) {
                reg.addRecipe(new GenericEmiRecipe(tank, id, tank(recipe, FluidStack.EMPTY)));
            } else {
                // Alternatives are separate results, never several fluids produced by one operation.
                int index = 0;
                for (FluidStack fluid : recipe.fluid().getFluids()) {
                    reg.addRecipe(new GenericEmiRecipe(tank, variant(id, "fluid_" + index++), tank(recipe, fluid)));
                }
            }
        });

        EmiRecipeCategory enchanter = Categories.machine(reg, "enderio_enchanting", "enderio:enchanter", "Enchanter");
        EmiIngredient lapis = EmiIngredient.of(Tags.Items.GEMS_LAPIS);
        Recipes.forEach(recipes, EnchanterRecipe.class, (id, recipe) -> {
            for (int level = 1; level <= recipe.enchantment().value().getMaxLevel(); level++) {
                reg.addRecipe(new GenericEmiRecipe(
                        enchanter, variant(id, "level_" + level), enchanting(recipe, level, lapis)));
            }
        });

        EmiRecipeCategory weather =
                Categories.machine(reg, "enderio_weather_change", "enderio:weather_obelisk", "Weather Obelisk");
        Recipes.forEach(
                recipes,
                WeatherChangeRecipe.class,
                (id, recipe) -> reg.addRecipe(new GenericEmiRecipe(weather, id, weather(recipe))));

        EmiRecipeCategory engine = Categories.machine(reg, "enderio_soul_engine", "enderio:soul_engine", "Soul Engine");
        for (EngineSoul.SoulData data : EngineSoul.ENGINE.map.values()) {
            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(data.entitytype())) {
                continue;
            }
            EmiIngredient fuel = engineFuel(data.fluid());
            if (fuel.isEmpty()) {
                continue;
            }
            ItemStack boundEngine =
                    Categories.stack("enderio:soul_engine").getItemStack().copy();
            boundEngine.set(EIODataComponents.SOUL, Soul.of(data.entitytype()));
            MachineDescriptor descriptor = engine(data, fuel, EmiStack.of(boundEngine));
            reg.addRecipe(new GenericEmiRecipe(engine, variant(data.entitytype(), "soul_engine"), descriptor));
        }

        reg.addWorkstation(VanillaEmiRecipeCategories.CRAFTING, Categories.stack("enderio:crafter"));
        reg.addWorkstation(VanillaEmiRecipeCategories.SMELTING, Categories.stack("enderio:alloy_smelter"));
        reg.addWorkstation(VanillaEmiRecipeCategories.FUEL, Categories.stack("enderio:stirling_generator"));
    }

    static MachineDescriptor tank(TankRecipe recipe, FluidStack outputFluid) {
        MachineDescriptor descriptor =
                new MachineDescriptor().itemIn(EmiIngredient.of(recipe.input())).itemOut(EmiStack.of(recipe.output()));
        if (recipe.mode() == TankRecipe.Mode.FILL) {
            descriptor.fluidIn(NeoForgeEmiIngredient.of(recipe.fluid()));
        } else {
            descriptor.fluidOut(NeoForgeEmiStack.of(outputFluid));
        }
        return descriptor;
    }

    static MachineDescriptor enchanting(EnchanterRecipe recipe, int level, EmiIngredient lapis) {
        EmiIngredient input = NeoForgeEmiIngredient.of(recipe.input());
        input.setAmount((long) recipe.input().count() * level);
        return new MachineDescriptor()
                .itemIn(EmiStack.of(Items.WRITABLE_BOOK))
                .itemIn(input)
                .itemIn(lapis.copy().setAmount(recipe.getLapisForLevel(level)))
                .itemOut(EmiStack.of(recipe.getBookForLevel(level)))
                .info(Enchantment.getFullname(recipe.enchantment(), level))
                .info(Component.literal(recipe.getXPCostForLevel(level) + " XP levels"));
    }

    static MachineDescriptor engine(EngineSoul.SoulData data, EmiIngredient fuel, EmiStack boundEngine) {
        return new MachineDescriptor()
                .fluidIn(fuel)
                .catalyst(boundEngine)
                .info(Component.literal("Bind this soul into the engine using a Soul Binder."))
                .info(Component.literal((long) data.powerpermb() * 1_000 + " FE per bucket"))
                .info(Component.literal(data.tickpermb() + " ticks per mB"));
    }

    static MachineDescriptor weather(WeatherChangeRecipe recipe) {
        return new MachineDescriptor()
                .fluidIn(NeoForgeEmiStack.of(recipe.fluid()))
                .info(Component.literal(
                        "Weather: " + Categories.capitalize(recipe.mode().getSerializedName())));
    }

    private static EmiIngredient engineFuel(String id) {
        List<EmiStack> fluids = new ArrayList<>();
        if (id.startsWith("#")) {
            TagKey<Fluid> tag = TagKey.create(Registries.FLUID, ResourceLocation.parse(id.substring(1)));
            BuiltInRegistries.FLUID
                    .getTag(tag)
                    .ifPresent(entries -> entries.forEach(
                            entry -> fluids.add(NeoForgeEmiStack.of(new FluidStack(entry.value(), 1_000)))));
        } else {
            ResourceLocation fluid = ResourceLocation.parse(id);
            if (BuiltInRegistries.FLUID.containsKey(fluid)) {
                fluids.add(NeoForgeEmiStack.of(new FluidStack(BuiltInRegistries.FLUID.get(fluid), 1_000)));
            }
        }
        return EmiIngredient.of(fluids);
    }

    private static ResourceLocation variant(ResourceLocation source, String suffix) {
        return ResourceLocation.fromNamespaceAndPath(
                "bertieemi", "enderio/" + source.getNamespace() + "/" + source.getPath() + "/" + suffix);
    }
}
