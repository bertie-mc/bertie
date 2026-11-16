package io.github.bertie_mc.bertieprogression.emi;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiStack;
import java.io.InputStreamReader;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.junit.jupiter.api.Test;

class CompactMiniEmiTest {
    @Test
    void indexesEverySelectedMiniOutputWithItsComponentsAndFullWallCount() throws Exception {
        var access = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var ops = RegistryOps.create(JsonOps.INSTANCE, access);
        Object manager = null;
        for (var constructor : RecipeManager.class.getConstructors()) {
            if (constructor.getParameterCount() == 0) manager = constructor.newInstance();
            else if (constructor.getParameterCount() == 1 && constructor.getParameterTypes()[0].isInstance(access))
                manager = constructor.newInstance(access);
        }
        assertNotNull(manager, Arrays.toString(RecipeManager.class.getConstructors()));
        var clazz = Class.forName("dev.compactmods.crafting.recipes.MiniaturizationRecipe");
        var codec = (MapCodec<?>) clazz.getField("CODEC").get(null);
        var views = JsonParser.parseReader(
                        new InputStreamReader(getClass().getResourceAsStream("/compact_mini_recipes.json")))
                .getAsJsonArray();
        List<RecipeHolder<?>> holders = new ArrayList<>();
        for (var element : views) {
            var row = element.getAsJsonObject();
            if (row.has("machine_layout")) continue;
            var id = ResourceLocation.parse(row.get("recipe").getAsString());
            var data = JsonParser.parseReader(new InputStreamReader(getClass()
                            .getResourceAsStream("/data/" + id.getNamespace() + "/recipe/" + id.getPath() + ".json")))
                    .getAsJsonObject();
            if (data.getAsJsonObject("components").has("core"))
                data.getAsJsonObject("components").getAsJsonObject("core").addProperty("block", "minecraft:iron_block");
            data.getAsJsonObject("catalyst").addProperty("id", "minecraft:stick");
            holders.add(new RecipeHolder<>(
                    id, (Recipe<?>) codec.codec().parse(ops, data).getOrThrow()));
        }
        var replace = Arrays.stream(RecipeManager.class.getMethods())
                .filter(m -> m.getName().equals("replaceRecipes"))
                .findFirst()
                .orElseThrow();
        replace.invoke(manager, holders);
        List<EmiRecipe> displayed = new ArrayList<>();
        Object recipeManager = manager;
        var registry = (EmiRegistry) Proxy.newProxyInstance(
                EmiRegistry.class.getClassLoader(), new Class<?>[] {EmiRegistry.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getRecipeManager")) return recipeManager;
                    if (method.getName().equals("addRecipe")) displayed.add((EmiRecipe) args[0]);
                    return null;
                });
        var giant = new InWorldEmiCategory(
                ResourceLocation.parse("bertieprogression:giant_anvil"),
                EmiStack.EMPTY,
                Component.literal("Giant Anvil"));
        CompactMiniEmi.register(registry, giant);
        assertEquals(8, displayed.size());
        var soaryn = displayed.stream()
                .filter(r -> r.getId().getPath().endsWith("compact_machine_soaryn"))
                .findFirst()
                .orElseThrow();
        assertEquals(386, soaryn.getInputs().getFirst().getAmount());
        var template =
                BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.parse("compactmachines:room_template"));
        assertEquals(
                ResourceLocation.parse("compactmachines:soaryn"),
                soaryn.getOutputs().getFirst().getItemStack().get(template));
    }
}
