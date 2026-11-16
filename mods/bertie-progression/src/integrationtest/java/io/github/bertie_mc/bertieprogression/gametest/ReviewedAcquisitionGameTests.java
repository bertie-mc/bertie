package io.github.bertie_mc.bertieprogression.gametest;

import committee.nova.mods.avaritia.api.common.crafting.TierInput;
import io.github.bertie_mc.bertieprogression.recipe.ReusableTableRecipe;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("bertieprogression")
@PrefixGameTestTemplate(false)
public final class ReviewedAcquisitionGameTests {
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void repairedThirdPartySchemasDecodeWithInstalledSerializers(GameTestHelper helper) throws Exception {
        var ops = net.minecraft.resources.RegistryOps.create(
                com.mojang.serialization.JsonOps.INSTANCE, helper.getLevel().registryAccess());
        for (String file : List.of(
                "cocktailsdelight/recipe/emptying/blue_curacao",
                "cocktailsdelight/recipe/filling/blue_curacao",
                "ends_delight/recipe/food/bubble_tea",
                "ends_delight/recipe/food/ender_congee",
                "endersdelight/recipe/chorus_pie_slice",
                "malum/recipe/create/milling/grim_talc",
                "bertieprogression/recipe/woodcutting/vertical_oak_planks")) {
            try (var input = io.github.bertie_mc.bertieprogression.BertieProgression.class.getResourceAsStream(
                    "/data/" + file + ".json")) {
                helper.assertTrue(input != null, "Reviewed override exists: " + file);
                var data = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(input))
                        .getAsJsonObject();
                registeredFixtureValues(data);
                var decoded = net.minecraft.world.item.crafting.Recipe.CODEC.parse(ops, data);
                helper.assertTrue(decoded.error().isEmpty(), file + ": " + decoded.error());
            }
        }
        helper.succeed();
    }

    private static void registeredFixtureValues(com.google.gson.JsonElement node) {
        if (node.isJsonArray()) {
            for (var value : node.getAsJsonArray()) registeredFixtureValues(value);
            return;
        }
        if (!node.isJsonObject()) return;
        var object = node.getAsJsonObject();
        for (var entry : object.entrySet()) {
            var value = entry.getValue();
            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                String id = value.getAsString();
                if (entry.getKey().equals("fluid") || entry.getKey().equals("id") && object.has("amount")) {
                    if (!BuiltInRegistries.FLUID.containsKey(ResourceLocation.parse(id)))
                        entry.setValue(new com.google.gson.JsonPrimitive("minecraft:water"));
                } else if (entry.getKey().equals("item") || entry.getKey().equals("id")) {
                    if (!BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(id)))
                        entry.setValue(new com.google.gson.JsonPrimitive("minecraft:stone"));
                }
            } else registeredFixtureValues(value);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void endTableReturnsTheSameCoreAcrossRepeatedCrafts(GameTestHelper helper) {
        var holder = helper.getLevel()
                .getRecipeManager()
                .byKey(ResourceLocation.parse("bertieprogression:enderite_ingot_from_ferrojelly"))
                .orElseThrow();
        helper.assertTrue(holder.value() instanceof ReusableTableRecipe, "End Table catalyst serializer must load");
        var recipe = (ReusableTableRecipe) holder.value();
        String[] rows = {"       ", "JJJJJJJ", "JEEEEEJ", "JEJVJEJ", "JEEEEEJ", "JJJJJJJ", "       "};
        var items = new ArrayList<ItemStack>();
        for (String row : rows)
            for (char symbol : row.toCharArray()) {
                String id =
                        switch (symbol) {
                            case 'J' -> "the_beyond:ferrojelly";
                            case 'E' -> "magitech:ender_metal_ingot";
                            case 'V' -> "cataclysm:void_core";
                            default -> "minecraft:air";
                        };
                var stack =
                        new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)), symbol == 'V' ? 1 : 4);
                if (symbol == 'V') stack.set(DataComponents.CUSTOM_NAME, Component.literal("Named reusable core"));
                items.add(stack);
            }
        for (int craft = 0; craft < 3; craft++) {
            var input = TierInput.of(7, 7, items, 3);
            helper.assertTrue(recipe.matches(input, helper.getLevel()), "7×7 End Table craft must match repeatedly");
            var result = recipe.assemble(input, helper.getLevel().registryAccess());
            helper.assertTrue(result.getCount() == 3, "Each operation produces three Enderite Ingots");
            var remaining = recipe.getRemainingItems(input);
            int cores = 0;
            for (int slot = 0; slot < input.size(); slot++) {
                var original = input.getItem(slot);
                var remainder = remaining.get(slot);
                if (BuiltInRegistries.ITEM.getKey(original.getItem()).toString().equals("cataclysm:void_core")) {
                    helper.assertTrue(
                            remainder.getCount() == 1 && ItemStack.isSameItemSameComponents(original, remainder),
                            "Core identity, custom name and components must survive");
                    cores++;
                }
            }
            helper.assertTrue(cores == 1, "Exactly one reusable core");
            for (var stack : items)
                if (!stack.isEmpty()
                        && !BuiltInRegistries.ITEM
                                .getKey(stack.getItem())
                                .toString()
                                .equals("cataclysm:void_core")) stack.shrink(1);
        }
        helper.assertTrue(
                !recipe.matches(TierInput.of(7, 7, items, 2), helper.getLevel()),
                "Lower-tier tables must not bypass End crafting");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void allFiveMiniRecipesLoadAndKeepConnectionVariantsMatchable(GameTestHelper helper)
            throws Exception {
        Class<?> nativeMini = Class.forName("dev.compactmods.crafting.recipes.MiniaturizationRecipe");
        Class<?> nativeBlock = Class.forName("dev.compactmods.crafting.recipes.components.BlockComponent");
        var connected = BuiltInRegistries.BLOCK
                .get(ResourceLocation.parse("oritech:superconductor_connection"))
                .defaultBlockState();
        int cableComponents = 0;
        for (String name : List.of(
                "large_heat_generator",
                "large_gas_burning_generator",
                "large_pigment_mixer",
                "large_chemical_infuser",
                "large_electrolytic_separator")) {
            var holder = helper.getLevel()
                    .getRecipeManager()
                    .byKey(ResourceLocation.parse("bertieprogression:miniaturization/" + name))
                    .orElseThrow();
            helper.assertTrue(
                    nativeMini.isInstance(holder.value()), "Native Miniaturization recipe must decode: " + name);
            Object recipe = holder.value();
            var outputs = (ItemStack[]) nativeMini.getMethod("getOutputs").invoke(recipe);
            helper.assertTrue(outputs.length == 1 && outputs[0].getCount() == 1, "One machine per recipe");
            var dimensions = (net.minecraft.world.phys.AABB)
                    nativeMini.getMethod("getDimensions").invoke(recipe);
            helper.assertTrue(
                    dimensions.getXsize() == 3 && dimensions.getYsize() == 3 && dimensions.getZsize() == 3,
                    "Captured 3³ footprint must survive native decoding");
            var catalyst = (ItemStack) nativeMini.getMethod("catalyst").invoke(recipe);
            helper.assertTrue(
                    !BuiltInRegistries.ITEM
                            .getKey(catalyst.getItem())
                            .toString()
                            .equals("mekanism:steel_casing"),
                    "Every captured machine has its selected catalyst");
            Object components = nativeMini.getMethod("getComponents").invoke(recipe);
            var blocks = (java.util.Map<?, ?>)
                    components.getClass().getMethod("getBlockComponents").invoke(components);
            for (var component : blocks.values()) {
                if (nativeBlock.isInstance(component)
                        && BuiltInRegistries.BLOCK
                                .getKey((net.minecraft.world.level.block.Block)
                                        nativeBlock.getMethod("getBlock").invoke(component))
                                .toString()
                                .equals("oritech:superconductor")) {
                    helper.assertTrue(
                            (boolean) nativeBlock
                                    .getMethod("matches", net.minecraft.world.level.block.state.BlockState.class)
                                    .invoke(component, connected),
                            "Cable network updates must not break Miniaturization matching");
                    cableComponents++;
                }
            }
            try (var input = io.github.bertie_mc.bertieprogression.BertieProgression.class.getResourceAsStream(
                    "/spatial_guides.json")) {
                var guides = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(input))
                        .getAsJsonArray();
                var cells = new java.util.HashMap<
                        net.minecraft.core.BlockPos, net.minecraft.world.level.block.state.BlockState>();
                for (var element : guides) {
                    var guide = element.getAsJsonObject();
                    if (!guide.get("recipe").getAsString().equals("bertieprogression:miniaturization/" + name))
                        continue;
                    for (var entry : guide.getAsJsonArray("blocks")) {
                        var cell = entry.getAsJsonObject();
                        var pos = cell.getAsJsonArray("pos");
                        cells.put(
                                new net.minecraft.core.BlockPos(
                                        pos.get(0).getAsInt(),
                                        pos.get(1).getAsInt(),
                                        pos.get(2).getAsInt()),
                                BuiltInRegistries.BLOCK
                                        .get(ResourceLocation.parse(cell.getAsJsonObject("state")
                                                .get("Name")
                                                .getAsString()))
                                        .defaultBlockState());
                    }
                }
                net.minecraft.world.level.BlockGetter getter = new net.minecraft.world.level.BlockGetter() {
                    public net.minecraft.world.level.block.entity.BlockEntity getBlockEntity(
                            net.minecraft.core.BlockPos pos) {
                        return null;
                    }

                    public net.minecraft.world.level.block.state.BlockState getBlockState(
                            net.minecraft.core.BlockPos pos) {
                        return cells.getOrDefault(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                    }

                    public net.minecraft.world.level.material.FluidState getFluidState(
                            net.minecraft.core.BlockPos pos) {
                        return getBlockState(pos).getFluidState();
                    }

                    public int getHeight() {
                        return 3;
                    }

                    public int getMinBuildHeight() {
                        return 0;
                    }
                };
                var nativeComponents = Class.forName("dev.compactmods.crafting.api.components.IRecipeComponents");
                var nativeBlocks = Class.forName("dev.compactmods.crafting.recipes.blocks.RecipeBlocks");
                var nativeInput = Class.forName("dev.compactmods.crafting.api.recipe.layers.IRecipeBlocks");
                var create = nativeBlocks.getMethod(
                        "create",
                        net.minecraft.world.level.BlockGetter.class,
                        nativeComponents,
                        net.minecraft.world.phys.AABB.class);
                var captured =
                        create.invoke(null, getter, components, new net.minecraft.world.phys.AABB(0, 0, 0, 3, 3, 3));
                helper.assertTrue(
                        (boolean) nativeMini.getMethod("matches", nativeInput).invoke(recipe, captured),
                        "Exact saved-world geometry must match: " + name + "; identified="
                                + nativeBlocks.getMethod("allIdentified").invoke(captured) + "; totals="
                                + nativeBlocks
                                        .getMethod("getKnownComponentTotals")
                                        .invoke(captured) + "; filled="
                                + nativeBlocks.getMethod("getFilledBounds").invoke(captured));
                var broken = cells.keySet().iterator().next();
                cells.remove(broken);
                captured = create.invoke(null, getter, components, new net.minecraft.world.phys.AABB(0, 0, 0, 3, 3, 3));
                helper.assertTrue(
                        !(boolean) nativeMini.getMethod("matches", nativeInput).invoke(recipe, captured),
                        "An incomplete structure must fail: " + name);
            }
        }
        helper.assertTrue(cableComponents == 3, "Mixer, Chemical Infuser and Separator use canonical cable components");
        helper.succeed();
    }
}
