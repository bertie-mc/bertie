package io.github.bertie_mc.bertieprogression.gametest;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("bertieprogression")
@PrefixGameTestTemplate(false)
public final class UnboundProgressionGameTests {
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void selectedUnboundRecipesAndNativeTablesLoad(GameTestHelper helper) throws Exception {
        helper.assertTrue(!ModList.get().isLoaded("pocket_dimension"), "Pocket Dimension is still loaded");
        var level = helper.getLevel();
        var recipes = level.getRecipeManager();
        for (String name : List.of(
                "valve",
                "air_filter",
                "beam_prism",
                "tripod",
                "mount_head",
                "optical_tube",
                "eyepiece",
                "tuner",
                "empty_canister",
                "hydroponics"))
            helper.assertTrue(recipes.byKey(id("unbound:" + name)).isPresent(), "Missing selected recipe " + name);
        for (String name : List.of("glass_blend", "lens_blank")) {
            var recipe = recipes.byKey(id("bertieprogression:unbound/mixing/" + name))
                    .orElseThrow()
                    .value();
            helper.assertTrue(
                    recipe.getIngredients().size() == (name.equals("lens_blank") ? 24 : 15),
                    "Mixing ingredient quantities lost");
        }
        helper.assertTrue(
                recipes.byKey(id("bertieprogression:unbound/copper_gear")).isPresent(), "Gear assembly failed to load");
        var raw = new ItemStack(BuiltInRegistries.ITEM.get(id("unbound:raw_ilmenite")));
        boolean found = false;
        for (var holder : recipes.getRecipes())
            if (holder.value() instanceof AbstractCookingRecipe cooking
                    && cooking.matches(new SingleRecipeInput(raw), level)) {
                helper.assertTrue(
                        cooking.getResultItem(level.registryAccess()).is(Items.IRON_INGOT),
                        "Ilmenite still smelts into Titanium: " + holder.id());
                found = true;
            }
        helper.assertTrue(found, "Raw Ilmenite has no Iron smelting path");
        var adapter = BuiltInRegistries.BLOCK.get(id("bertieprogression:mechanical_adapter"));
        try (var stream = io.github.bertie_mc.bertieprogression.BertieProgression.class.getResourceAsStream(
                "/assets/bertieprogression/blockstates/mechanical_adapter.json")) {
            var variants = JsonParser.parseReader(new InputStreamReader(stream))
                    .getAsJsonObject()
                    .getAsJsonObject("variants");
            for (var entry : variants.entrySet())
                for (String property : entry.getKey().split(","))
                    helper.assertTrue(
                            adapter.getStateDefinition().getProperty(property.split("=")[0]) != null,
                            "Placed model uses unknown property " + property);
        }
        Class<?> spectra = Class.forName("me.moltea.unbound.spectral.Spectra");
        helper.assertTrue(
                level.registryAccess()
                                .registryOrThrow(net.minecraft.core.registries.Registries.DIMENSION_TYPE)
                                .containsKey(id("pocket_dimension:pocket_dimension"))
                        && level.registryAccess()
                                .registryOrThrow(net.minecraft.core.registries.Registries.BIOME)
                                .containsKey(id("pocket_dimension:endless_void_biom")),
                "Existing saves lost retired dimension registry references");
        helper.assertTrue(
                ((Collection<?>) spectra.getMethod("refs").invoke(null)).size() >= 46,
                "New spectral references not installed");
        var byItem = spectra.getDeclaredField("BY_ITEM");
        byItem.setAccessible(true);
        Map<?, ?> samples = (Map<?, ?>) byItem.get(null);
        try (var stream = io.github.bertie_mc.bertieprogression.BertieProgression.class.getResourceAsStream(
                "/unbound_spectral_bindings.json")) {
            var binding = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();
            for (var family : binding.getAsJsonArray("families"))
                for (var item : family.getAsJsonObject().getAsJsonArray("sample_items"))
                    helper.assertTrue(
                            samples.containsKey(id(item.getAsString())), "Spectral specimen not recognised: " + item);
        }
        Class<?> uses = Class.forName("me.moltea.unbound.spectral.Uses");
        Object debris = uses.getMethod("ore", net.minecraft.world.level.block.Block.class)
                .invoke(null, Blocks.ANCIENT_DEBRIS);
        helper.assertTrue(
                debris != null
                        && !((List<?>) debris.getClass().getMethod("refs").invoke(debris)).contains("au"),
                "Refined gold leaked into Ancient Debris scanner targets");
        Class<?> guide = Class.forName("me.moltea.unbound.progress.Guide");
        var nodes = (List<?>) guide.getField("NODES").get(null);
        var ids = new HashSet<String>();
        for (Object node : nodes)
            ids.add((String) node.getClass().getMethod("id").invoke(node));
        for (String removed :
                List.of("electric_furnace", "crusher", "mail_station", "storage_terminal", "assembler", "storage_cell"))
            helper.assertTrue(!ids.contains(removed), "Retired journal node remains " + removed);
        for (Object node : nodes)
            for (Object parent : (List<?>) node.getClass().getMethod("parents").invoke(node))
                helper.assertTrue(ids.contains(parent), "Dangling native journal dependency " + parent);
        helper.succeed();
    }

    private static ResourceLocation id(String value) {
        return ResourceLocation.parse(value);
    }

    private UnboundProgressionGameTests() {}
}
