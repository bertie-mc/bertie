package io.github.bertie_mc.bertieprogression.emi;

import io.github.bertie_mc.bertieprogression.AllayCorruptionHandler;
import io.github.bertie_mc.bertieprogression.BertieProgression;
import io.github.bertie_mc.bertieprogression.ModItems;
import io.github.bertie_mc.bertieprogression.forge.BedRecipes;
import io.github.bertie_mc.bertieprogression.recipe.ModRecipes;
import io.github.bertie_mc.bertieprogression.recipe.OminousFanRecipe;
import com.mojang.logging.LogUtils;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.neoforge.NeoForgeEmiIngredient;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Built-in EMI presentation for Bertie Progression's recipes and pack item policy. */
@EmiEntrypoint
public final class BertieProgressionEmiPlugin implements EmiPlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    /**
     * Preserves the Mallet Work category and recipe identifiers across the ownership move, and now
     * across the companion mod's rename to {@code bertieemi} as well. No mod carries this id any
     * more; it stays only so an existing EMI config keeps recognising the category. Categories added
     * since the move use {@link BertieProgression#MODID}.
     */
    private static final String LEGACY_NAMESPACE = "berlordsemi";

    @Override
    public void register(EmiRegistry registry) {
        registry.removeEmiStacks(BertieProgressionEmiPlugin::isReplacedSlagArmorPart);

        EmiStack brickForge = stackOf("slag:brick_forge", 1);
        InWorldEmiCategory category = new InWorldEmiCategory(
                ResourceLocation.fromNamespaceAndPath(LEGACY_NAMESPACE, "bertie_mallet"),
                brickForge, Component.literal("Mallet Work"));
        registry.addCategory(category);
        if (!brickForge.isEmpty()) {
            registry.addWorkstation(category, brickForge);
        }

        EmiStack mallet = EmiStack.of(ModItems.OPENING_MALLET.get());
        if (!mallet.isEmpty()) {
            registry.addWorkstation(category, mallet);
        }

        int recipeCount = 0;
        for (BedRecipes.BedRecipe recipe : BedRecipes.RECIPES) {
            List<EmiIngredient> inputs = new ArrayList<>();
            List<Component> info = new ArrayList<>();
            addInput(inputs, info, recipe.primary());
            addInput(inputs, info, recipe.secondary());
            addInput(inputs, info, recipe.tertiary());
            info.add(Component.literal((recipe.sneak() ? "SNEAK-strike" : "Strike")
                    + " the placed Brick Forge holding input 1; carry the rest"));

            List<EmiStack> outputs = new ArrayList<>();
            addStack(outputs, stackOf(recipe.resultId(), recipe.resultCount()));
            if (recipe.extraReturnId() != null) {
                addStack(outputs, stackOf(recipe.extraReturnId(), recipe.extraReturnCount()));
            }
            addRecipe(registry, category, "bed/" + recipe.id(), inputs,
                    ingredients(mallet), outputs, info);
            recipeCount++;
        }

        addRecipe(registry, category, "world/mud",
                ingredients(EmiStack.of(Items.DIRT)), ingredients(mallet),
                stacks(EmiStack.of(Items.MUD)),
                List.of(Component.literal("Strike placed Dirt that touches water")));
        recipeCount++;

        addRecipe(registry, category, "world/paper",
                ingredients(EmiStack.of(Items.SUGAR_CANE, 3)),
                ingredients(mallet, sized("berlordscarving:wood_slate", 2)),
                stacks(EmiStack.of(Items.PAPER, 3)),
                List.of(Component.literal("Strike a block touching water; the two slates are kept")));
        recipeCount++;

        addRecipe(registry, category, "world/brick_forge",
                ingredients(EmiStack.of(Items.MUD_BRICKS, 8), EmiStack.of(Items.CAMPFIRE),
                        tag("bertieprogression:stripped_logs", 4)),
                ingredients(mallet), stacks(stackOf("slag:brick_forge", 1)),
                List.of(Component.literal("Build: 3x3 Mud Brick ring around an UNLIT campfire, "
                        + "4 stripped logs above the corners - strike a ring brick")));
        recipeCount++;

        int ominousFanRecipeCount = registerOminousFan(registry);
        int allayRecipeCount = registerAllayCorruption(registry);
        LOGGER.info(
                "Bertie Progression EMI integration registered ({} Mallet Work recipes, "
                        + "{} Ominous Fan recipes, {} Allay Corruption recipes)",
                recipeCount, ominousFanRecipeCount, allayRecipeCount);
    }

    /**
     * The Allay trade from {@link AllayCorruptionHandler}: it is a tick handler rather than a recipe
     * type, so the single conversion it implements is synthesized here from the handler's own ids.
     * The Allay is drawn as its spawn egg and listed as an input, not a catalyst — it dies.
     */
    private static int registerAllayCorruption(EmiRegistry registry) {
        EmiStack input = stackOf(AllayCorruptionHandler.INPUT, 1);
        EmiStack output = stackOf(AllayCorruptionHandler.OUTPUT, 1);
        if (input.isEmpty() || output.isEmpty()) {
            return 0; // Forbidden Arcanus absent: neither side of the trade exists
        }

        EmiStack allay = EmiStack.of(Items.ALLAY_SPAWN_EGG);
        InWorldEmiCategory category = new InWorldEmiCategory(
                ResourceLocation.fromNamespaceAndPath(BertieProgression.MODID, "allay_corruption"),
                allay, Component.literal("Allay Corruption"));
        registry.addCategory(category);
        registry.addWorkstation(category, allay);

        registry.addRecipe(new InWorldEmiRecipe(category,
                ResourceLocation.fromNamespaceAndPath(
                        BertieProgression.MODID, "allay_corruption/arcane_crystal"),
                ingredients(input, allay), List.of(), stacks(output),
                List.of(Component.literal("Give an Allay the Arcane Crystal - it dies moments later, "
                        + "dropping the corrupted one"))));
        return 1;
    }

    private static int registerOminousFan(EmiRegistry registry) {
        var recipes = registry.getRecipeManager().getAllRecipesFor(ModRecipes.OMINOUS_FAN_TYPE.get());
        if (recipes.isEmpty()) {
            return 0;
        }

        EmiStack icon = stackOf("twilightforest:ominous_candle", 1);
        InWorldEmiCategory category = new InWorldEmiCategory(
                ResourceLocation.fromNamespaceAndPath(BertieProgression.MODID, "ominous_fan"),
                icon, Component.literal("Ominous Fan Blowing"));
        registry.addCategory(category);
        if (!icon.isEmpty()) {
            registry.addWorkstation(category, icon);
        }

        for (var holder : recipes) {
            OminousFanRecipe recipe = holder.value();
            registry.addRecipe(new OminousFanEmiRecipe(
                    category, holder.id(), EmiIngredient.of(recipe.input()), EmiStack.of(recipe.result())));
        }
        return recipes.size();
    }

    private static void addRecipe(EmiRegistry registry, InWorldEmiCategory category, String path,
                                  List<EmiIngredient> inputs, List<EmiIngredient> catalysts,
                                  List<EmiStack> outputs, List<Component> info) {
        registry.addRecipe(new InWorldEmiRecipe(category, displayId(path),
                inputs, catalysts, outputs, info));
    }

    private static void addInput(List<EmiIngredient> inputs, List<Component> info, BedRecipes.Input input) {
        if (input == null) {
            return;
        }
        String description = input.what().describe();
        if (description.startsWith("#")) {
            addIngredient(inputs, tag(description.substring(1), input.count()));
        } else if (description.contains("{")) {
            String[] materialAndPart = description.substring(
                    description.indexOf('{') + 1, description.indexOf('}')).split(",");
            addIngredient(inputs, sized("slag:dynamic_part", input.count()));
            info.add(Component.literal("Needs the exact Slag-cast "
                    + capitalize(materialAndPart[0].replace("slag:", "")) + " "
                    + capitalize(materialAndPart[1].replace("slag:", ""))));
        } else {
            addIngredient(inputs, sized(description, input.count()));
        }
    }

    private static List<EmiIngredient> ingredients(EmiIngredient... ingredients) {
        List<EmiIngredient> result = new ArrayList<>();
        Arrays.stream(ingredients).forEach(ingredient -> addIngredient(result, ingredient));
        return result;
    }

    private static List<EmiStack> stacks(EmiStack... stacks) {
        List<EmiStack> result = new ArrayList<>();
        Arrays.stream(stacks).forEach(stack -> addStack(result, stack));
        return result;
    }

    private static void addIngredient(List<EmiIngredient> ingredients, EmiIngredient ingredient) {
        if (ingredient != null && !ingredient.isEmpty()) {
            ingredients.add(ingredient);
        }
    }

    private static void addStack(List<EmiStack> stacks, EmiStack stack) {
        if (stack != null && !stack.isEmpty()) {
            stacks.add(stack);
        }
    }

    private static EmiIngredient sized(String itemId, int count) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
        return NeoForgeEmiIngredient.of(new SizedIngredient(Ingredient.of(item), count));
    }

    private static EmiIngredient tag(String tagId, int count) {
        TagKey<Item> key = TagKey.create(Registries.ITEM, ResourceLocation.parse(tagId));
        return NeoForgeEmiIngredient.of(new SizedIngredient(Ingredient.of(key), count));
    }

    private static EmiStack stackOf(String id, int count) {
        return stackOf(ResourceLocation.parse(id), count);
    }

    /** Empty when the id is not registered — {@code ITEM.get} answers Air for a mod that is absent. */
    private static EmiStack stackOf(ResourceLocation id, int count) {
        return EmiStack.of(BuiltInRegistries.ITEM.get(id), count);
    }

    private static ResourceLocation displayId(String path) {
        return ResourceLocation.fromNamespaceAndPath(LEGACY_NAMESPACE, "bertie_mallet/" + path);
    }

    private static String capitalize(String value) {
        return value == null || value.isEmpty()
                ? value
                : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static boolean isReplacedSlagArmorPart(EmiStack stack) {
        try {
            var itemStack = stack.getItemStack();
            if (itemStack.isEmpty() || !BuiltInRegistries.ITEM.getKey(itemStack.getItem())
                    .equals(ResourceLocation.parse("slag:dynamic_part"))) {
                return false;
            }
            String material = componentString(itemStack, "slag:material_type");
            if (!"slag:wooden".equals(material) && !"slag:bone".equals(material)) {
                return false;
            }
            String part = componentString(itemStack, "slag:part_type");
            return "slag:helmet".equals(part) || "slag:chestplate".equals(part)
                    || "slag:leggings".equals(part) || "slag:boots".equals(part);
        } catch (Throwable throwable) {
            return false;
        }
    }

    private static String componentString(net.minecraft.world.item.ItemStack stack, String typeId) {
        var type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.parse(typeId));
        if (type == null) {
            return null;
        }
        Object value = stack.get(type);
        return value == null ? null : String.valueOf(value);
    }
}
