package io.github.bertie_mc.bertieprogression.recipe;

import com.google.gson.JsonElement;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public final class CompactMachineRecipePolicy {
    private static final Set<String> STOCK = Set.of(
            "wall",
            "new_machine_tiny",
            "new_machine_small",
            "new_machine_normal",
            "new_machine_large",
            "new_machine_giant",
            "new_machine_colossal",
            "new_machine_soaryn",
            "new_machine_farming");

    public static Map<ResourceLocation, JsonElement> filter(Map<ResourceLocation, JsonElement> recipes) {
        var result = new HashMap<>(recipes);
        result.keySet().removeIf(id -> id.getNamespace().equals("compactmachines") && STOCK.contains(id.getPath()));
        return result;
    }

    private CompactMachineRecipePolicy() {}
}
