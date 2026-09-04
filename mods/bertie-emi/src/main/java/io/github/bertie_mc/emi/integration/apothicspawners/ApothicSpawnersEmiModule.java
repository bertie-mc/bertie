package io.github.bertie_mc.emi.integration.apothicspawners;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.shadowsoffire.apothic_spawners.modifiers.SpawnerModifier;
import dev.shadowsoffire.apothic_spawners.modifiers.StatModifier;
import io.github.bertie_mc.emi.framework.Categories;
import io.github.bertie_mc.emi.framework.GenericEmiRecipe;
import io.github.bertie_mc.emi.framework.MachineDescriptor;
import io.github.bertie_mc.emi.framework.Recipes;
import net.minecraft.network.chat.Component;

/**
 * Apothic Spawners' spawner modifiers: hold an item in each hand and right-click a mob spawner to
 * change one of its stats. There is no output item, so the spawner is both the workstation and the
 * thing shown as the result, and each stat the modifier touches becomes an info line.
 *
 * <p>The offhand item is a catalyst when the modifier does not consume it, which is the difference
 * between a reusable tool and a material spent per use.
 */
public final class ApothicSpawnersEmiModule {

    private ApothicSpawnersEmiModule() {}

    public static void register(EmiRegistry reg) {
        EmiRecipeCategory spawners =
                Categories.machine(reg, "apothic_spawner_modifiers", "minecraft:spawner", "Spawner Modifiers");
        Recipes.forEach(reg.getRecipeManager(), SpawnerModifier.class, (id, modifier) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(EmiIngredient.of(modifier.getMainhandInput()));
            EmiIngredient offhand = EmiIngredient.of(modifier.getOffhandInput());
            if (!offhand.isEmpty()) {
                if (modifier.consumesOffhand()) {
                    d.itemIn(offhand);
                } else {
                    d.catalyst(offhand);
                }
            }
            d.itemOut(Categories.stack("minecraft:spawner"));
            for (StatModifier<?> stat : modifier.getStatModifiers()) {
                d.info(describe(stat));
            }
            reg.addRecipe(new GenericEmiRecipe(spawners, id, d));
        });
    }

    /**
     * {@code ADD} adjusts a stat by its value and {@code SET} replaces it, which is the difference
     * between "+2 Spawn Count" and "Spawn Count: 2". Booleans read as a switch either way.
     */
    private static Component describe(StatModifier<?> stat) {
        String value = stat.getFormattedValue();
        String name = stat.stat().name().getString();
        if (stat.mode() == StatModifier.Mode.SET) {
            return Component.literal(name + ": " + value);
        }
        if ("true".equals(value)) {
            return Component.literal("Enables " + name);
        }
        if ("false".equals(value)) {
            return Component.literal("Disables " + name);
        }
        boolean signed = value.startsWith("-") || value.startsWith("+");
        return Component.literal((signed ? value : "+" + value) + " " + name);
    }
}
