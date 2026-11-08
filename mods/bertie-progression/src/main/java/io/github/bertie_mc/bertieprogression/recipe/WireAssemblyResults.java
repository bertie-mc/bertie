package io.github.bertie_mc.bertieprogression.recipe;

import io.github.bertie_mc.bertieprogression.BertieProgression;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Adds the guaranteed wire alongside Create's weighted assembly result. */
public final class WireAssemblyResults {
    private static final String PREFIX = "sequenced_assembly/";
    private static final String MARKER = "bertie_guaranteed_wire";

    public static void mark(ResourceLocation recipe, ItemStack output, boolean complete) {
        if (!complete
                || output.isEmpty()
                || !recipe.getNamespace().equals(BertieProgression.MODID)
                || !recipe.getPath().startsWith(PREFIX)
                || !recipe.getPath().endsWith("_wire")) {
            return;
        }
        var wire = ResourceLocation.fromNamespaceAndPath(
                BertieProgression.MODID, recipe.getPath().substring(PREFIX.length()));
        if (!BuiltInRegistries.ITEM.containsKey(wire)) {
            return;
        }
        var data = output.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag();
        data.putString(MARKER, wire.toString());
        output.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }

    public static List<ItemStack> expand(List<ItemStack> results) {
        var expanded = new ArrayList<ItemStack>();
        for (var result : results) {
            var data = result.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                    .copyTag();
            if (!data.contains(MARKER)) {
                expanded.add(result);
                continue;
            }
            var wire = ResourceLocation.tryParse(data.getString(MARKER));
            data.remove(MARKER);
            if (data.isEmpty()) {
                result.remove(DataComponents.CUSTOM_DATA);
            } else {
                result.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
            }
            if (wire != null && BuiltInRegistries.ITEM.containsKey(wire)) {
                expanded.add(new ItemStack(BuiltInRegistries.ITEM.get(wire)));
            }
            expanded.add(result);
        }
        return expanded;
    }

    private WireAssemblyResults() {}
}
