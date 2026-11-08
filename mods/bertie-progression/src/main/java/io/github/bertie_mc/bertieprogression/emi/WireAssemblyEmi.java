package io.github.bertie_mc.bertieprogression.emi;

import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import io.github.bertie_mc.bertieprogression.BertieProgression;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Displays the guaranteed output separately from the mutually exclusive byproducts. */
final class WireAssemblyEmi {
    static void register(EmiRegistry registry) {
        var press = item("create:mechanical_press");
        var saw = item("create:mechanical_saw");
        var category = new InWorldEmiCategory(id("wire_assembly"), press, Component.literal("Wire Assembly"));
        registry.addCategory(category);
        registry.addWorkstation(category, press);
        registry.addWorkstation(category, saw);
        for (var holder : registry.getRecipeManager().getRecipes()) {
            if (!(holder.value() instanceof SequencedAssemblyRecipe recipe)
                    || !holder.id().getNamespace().equals(BertieProgression.MODID)
                    || !holder.id().getPath().startsWith("sequenced_assembly/")
                    || !holder.id().getPath().endsWith("_wire")) {
                continue;
            }
            String material = holder.id().getPath().substring("sequenced_assembly/".length());
            var outputs = new ArrayList<EmiStack>();
            outputs.add(item(BertieProgression.MODID + ":" + material));
            float weight = 0;
            for (var result : recipe.resultPool) {
                weight += result.getChance();
            }
            for (var result : recipe.resultPool) {
                outputs.add(EmiStack.of(result.getStack()).setChance(result.getChance() / weight));
            }
            registry.removeRecipes(display -> holder.id().equals(display.getId()));
            registry.addRecipe(new InWorldEmiRecipe(
                    category,
                    id("wire_assembly/" + material),
                    List.of(EmiIngredient.of(recipe.getIngredient())),
                    List.of(press, saw),
                    outputs,
                    List.of(
                            Component.literal("Press, press, saw, saw, press. One pass."),
                            Component.literal("First Wire is guaranteed."),
                            Component.literal("Extra result: 30% Wire or 70% Ingot."))));
        }
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(BertieProgression.MODID, name);
    }

    private static EmiStack item(String name) {
        return EmiStack.of(BuiltInRegistries.ITEM.get(ResourceLocation.parse(name)));
    }

    private WireAssemblyEmi() {}
}
