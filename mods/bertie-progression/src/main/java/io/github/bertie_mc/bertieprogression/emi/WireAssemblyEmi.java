package io.github.bertie_mc.bertieprogression.emi;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import io.github.bertie_mc.bertieprogression.BertieProgression;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/** Adds the guaranteed wire to the existing sequenced-assembly display. */
public final class WireAssemblyEmi {
    static void register(EmiRegistry registry) {
        registry.addRecipeDecorator(WireAssemblyEmi::decorate);
    }

    static void decorate(EmiRecipe recipe, WidgetHolder widgets) {
        var wire = wire(recipe.getId());
        if (wire != null) {
            // The native layout reserves an empty row above its two weighted results.
            widgets.addSlot(EmiStack.of(BuiltInRegistries.ITEM.get(wire)),
                            recipe.getDisplayWidth() - 38, 2)
                    .recipeContext(recipe);
        }
    }

    public static List<EmiStack> outputs(ResourceLocation display, List<EmiStack> weighted) {
        var wire = wire(display);
        if (wire == null) {
            return weighted;
        }
        var outputs = new ArrayList<EmiStack>();
        outputs.add(EmiStack.of(BuiltInRegistries.ITEM.get(wire)));
        outputs.addAll(weighted);
        return outputs;
    }

    private static ResourceLocation wire(ResourceLocation display) {
        if (display == null) {
            return null;
        }
        String prefix = "create_sequenced_assembly/" + BertieProgression.MODID + "/";
        String path = display.getPath();
        String assembly = "sequenced_assembly/";
        if (!path.startsWith(prefix + assembly) || !path.endsWith("_wire")) {
            return null;
        }
        var wire = ResourceLocation.fromNamespaceAndPath(
                BertieProgression.MODID, path.substring((prefix + assembly).length()));
        return BuiltInRegistries.ITEM.containsKey(wire) ? wire : null;
    }

    private WireAssemblyEmi() {}
}
