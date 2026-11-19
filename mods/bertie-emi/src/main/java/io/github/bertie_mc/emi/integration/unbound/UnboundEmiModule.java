package io.github.bertie_mc.emi.integration.unbound;

import com.google.gson.*;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.*;
import io.github.bertie_mc.emi.framework.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

public final class UnboundEmiModule {
    private static final Map<String, String> ALIASES = new HashMap<>();
    private static final Set<String> REMOVED = new HashSet<>();

    private static ResourceLocation id(String value) {
        return ResourceLocation.parse("bertieemi:unbound/" + value);
    }

    private static EmiStack stack(String value) {
        Set<String> seen = new HashSet<>();
        while (ALIASES.containsKey(value) && seen.add(value)) value = ALIASES.get(value);
        return REMOVED.contains(value) ? EmiStack.EMPTY : Categories.stack(value);
    }

    private static void policy() throws Exception {
        if (!ModList.get().isLoaded("bertieprogression")) return;
        Class<?> owner = Class.forName("io.github.bertie_mc.bertieprogression.RemovedItems");
        try (var stream = owner.getResourceAsStream("/merged_items.json")) {
            if (stream != null)
                JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                        .getAsJsonObject()
                        .entrySet()
                        .forEach(e -> ALIASES.put(e.getKey(), e.getValue().getAsString()));
        }
        try (var stream = owner.getResourceAsStream("/removed_items.json")) {
            if (stream != null)
                JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                        .getAsJsonArray()
                        .forEach(e -> REMOVED.add(e.getAsString()));
        }
    }

    public static void register(EmiRegistry registry) throws Exception {
        policy();
        JsonObject data;
        try (var stream = UnboundEmiModule.class.getResourceAsStream("/unbound_sources.json")) {
            data = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
        var mining =
                Categories.machineNoStation(registry, "unbound_mining", "unbound:ilmenite_ore", "Unbound Ore Drops");
        var worlds =
                Categories.machine(registry, "unbound_distribution", "unbound:ore_scanner", "Unbound Ore Distribution");
        int index = 0;
        for (var value : data.getAsJsonArray("ores")) {
            var row = value.getAsJsonObject();
            var ore = stack(row.get("ore").getAsString());
            if (ore.isEmpty()) continue;
            var d = new MachineDescriptor()
                    .itemOut(ore.copy())
                    .info(Component.literal(title(row.get("world").getAsString())));
            if (row.has("height")) {
                var h = row.getAsJsonObject("height");
                d.info(Component.literal("Y: " + anchor(h.getAsJsonObject("min_inclusive")) + " to "
                        + anchor(h.getAsJsonObject("max_inclusive"))));
            }
            if (row.has("attempts")) d.info(Component.literal("Vein attempts per chunk: " + row.get("attempts")));
            registry.addRecipe(new GenericEmiRecipe(worlds, id("distribution/" + index), d));
            if (row.has("drop")) {
                d = new MachineDescriptor()
                        .itemIn(ore.copy())
                        .itemOut(stack(row.get("drop").getAsString())
                                .setAmount(Math.max(1, row.get("min").getAsInt())));
                d.info(Component.literal("Without Silk Touch"));
                if (row.get("min").getAsInt() != row.get("max").getAsInt())
                    d.info(Component.literal("Base amount: " + row.get("min") + "–" + row.get("max")));
                if (row.get("fortune").getAsBoolean()) d.info(Component.literal("Fortune increases the yield"));
                registry.addRecipe(new GenericEmiRecipe(mining, id("mining/" + index), d));
            }
            index++;
        }
        var mobs = Categories.machineNoStation(
                registry, "unbound_mob_drops", "unbound:selenite_colossus_spawn_egg", "Unbound Mob Drops");
        index = 0;
        for (var value : data.getAsJsonArray("drops")) {
            var row = value.getAsJsonObject();
            var output = stack(row.get("item").getAsString());
            if (output.isEmpty()) continue;
            String entity = row.get("entity").getAsString();
            var d = new MachineDescriptor()
                    .catalyst(stack(entity + "_spawn_egg"))
                    .itemOut(output.setAmount(Math.max(1, row.get("min").getAsInt()))
                            .setChance(row.get("chance").getAsFloat()));
            d.info(Component.translatable("entity." + entity.replace(':', '.')));
            if (row.get("min").getAsInt() != row.get("max").getAsInt())
                d.info(Component.literal("Base amount: " + row.get("min") + "–" + row.get("max")));
            if (row.get("looting").getAsBoolean()) d.info(Component.literal("Looting increases the yield"));
            registry.addRecipe(new GenericEmiRecipe(mobs, id("drops/" + index++), d));
        }
        processing(registry);
        if (ModList.get().isLoaded("create")) {
            var assembly = Categories.machineNoStation(
                    registry, "unbound_assembly", "unbound:lunar_rocket", "Unbound Multiblock Assembly");
            for (var guide : UnboundPonders.guides()) {
                var d = new MachineDescriptor();
                for (var material : guide.materials()) d.itemInMerged(EmiStack.of(material));
                d.itemOut(stack(guide.subject().toString()));
                d.info(Component.literal("Assemble in the world"));
                if (guide.id().equals("emitter"))
                    d.info(Component.literal("Activate the Heart with a Station Seed and its launch materials"));
                registry.addRecipe(
                        new AssemblyRecipe(assembly, id("assembly/" + guide.id()), d, guide.subject(), guide.id()));
            }
        }
        Class.forName("me.moltea.unbound.compat.RecipeViewer")
                .getMethod("bind", BiConsumer.class)
                .invoke(null, (BiConsumer<ItemStack, Boolean>) (item, uses) -> {
                    var replacement =
                            stack(BuiltInRegistries.ITEM.getKey(item.getItem()).toString());
                    if (uses) EmiApi.displayUses(replacement);
                    else EmiApi.displayRecipes(replacement);
                });
    }

    private static void processing(EmiRegistry registry) throws Exception {
        var fuel = Categories.machine(registry, "unbound_fuel", "unbound:refinery_mk1", "Unbound Fuel Plant");
        Class<?> feed = Class.forName("me.moltea.unbound.block.machine.Feedstock");
        Class<?> kind = Class.forName("me.moltea.unbound.rocket.Fuel");
        var of = feed.getMethod("of", ItemStack.class);
        var per = feed.getMethod("perCanister", kind);
        int index = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            var input = new ItemStack(item);
            Object record = of.invoke(null, input);
            if (record == null) continue;
            Object type = feed.getMethod("fuel").invoke(record);
            if (!(boolean) kind.getMethod("canned").invoke(type)) continue;
            int units = (int) feed.getMethod("units").invoke(record), capacity = (int) per.invoke(null, type);
            if (units <= 0 || capacity <= 0) continue;
            int divisor = gcd(units, capacity), outputs = units / divisor, inputs = capacity / divisor;
            var product = (Item) kind.getMethod("canister").invoke(type);
            var d = new MachineDescriptor()
                    .itemIn(stack(BuiltInRegistries.ITEM.getKey(item).toString())
                            .setAmount(inputs))
                    .itemIn(stack("unbound:empty_canister").setAmount(outputs))
                    .itemOut(EmiStack.of(product).setAmount(outputs));
            registry.addRecipe(new GenericEmiRecipe(fuel, id("fuel/" + index++), d));
        }
        var charging = Categories.machine(
                registry, "unbound_charging", "unbound:charging_station", "Unbound Battery Charging");
        var d = new MachineDescriptor()
                .itemIn(stack("unbound:battery"))
                .itemOut(stack("unbound:charged_battery"))
                .info(Component.literal("Open sky in daylight"));
        registry.addRecipe(new GenericEmiRecipe(charging, id("charging/daylight"), d));
        var analysis = Categories.machine(
                registry, "unbound_analysis", "unbound:spectral_lab_mk1", "Unbound Spectral Analysis");
        Class<?> spectra = Class.forName("me.moltea.unbound.spectral.Spectra");
        index = 0;
        var shownSamples = new HashSet<String>();
        for (Object source : (Collection<?>) spectra.getMethod("sources").invoke(null))
            for (Object item : (List<?>) source.getClass().getMethod("items").invoke(source)) {
                var sample = stack(item.toString());
                if (sample.isEmpty()
                        || !shownSamples.add(BuiltInRegistries.ITEM
                                .getKey(sample.getItemStack().getItem())
                                .toString())) continue;
                d = new MachineDescriptor()
                        .catalyst(sample)
                        .info(Component.literal("Analyse this specimen at a Spectral Laboratory"))
                        .info(Component.literal(
                                "Identify its spectrum to learn references and calibrate the Ore Scanner"));
                registry.addRecipe(new GenericEmiRecipe(analysis, id("analysis/" + index++), d));
            }
        var forge = Categories.machineNoStation(
                registry, "unbound_zero_g_forging", "unbound:star_journal", "Unbound Zero-G Forging");
        var client = Minecraft.getInstance();
        if (client.level != null)
            for (var holder : registry.getRecipeManager().getRecipes()) {
                var recipe = holder.value();
                var type = BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
                if (type == null || !type.toString().equals("unbound:zero_g_forging")) continue;
                d = new MachineDescriptor();
                for (Object input :
                        (List<?>) recipe.getClass().getMethod("ingredients").invoke(recipe)) {
                    var ingredient = (net.minecraft.world.item.crafting.Ingredient)
                            input.getClass().getMethod("ingredient").invoke(input);
                    long amount = ((Number) input.getClass().getMethod("count").invoke(input)).longValue();
                    d.itemInMerged(EmiIngredient.of(ingredient).setAmount(amount));
                }
                d.itemOut(EmiStack.of(
                        (ItemStack) recipe.getClass().getMethod("output").invoke(recipe)));
                d.info(Component.literal("Base time: "
                        + Categories.seconds(
                                (int) recipe.getClass().getMethod("time").invoke(recipe))));
                registry.addRecipe(
                        new GenericEmiRecipe(forge, id("zero_g/" + holder.id().getPath()), d));
            }
    }

    private static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    private static String title(String text) {
        return Arrays.stream(text.split("_"))
                .map(Categories::capitalize)
                .collect(java.util.stream.Collectors.joining(" "));
    }

    private static String anchor(JsonObject value) {
        if (value.has("absolute")) return value.get("absolute").getAsString();
        if (value.has("above_bottom")) return "bottom + " + value.get("above_bottom");
        return "top − " + value.get("below_top");
    }

    private static final class AssemblyRecipe extends GenericEmiRecipe {
        private final ResourceLocation subject;
        private final String guide;

        AssemblyRecipe(
                EmiRecipeCategory category,
                ResourceLocation id,
                MachineDescriptor data,
                ResourceLocation subject,
                String guide) {
            super(category, id, data);
            this.subject = subject;
            this.guide = guide;
        }

        @Override
        public int getDisplayHeight() {
            return super.getDisplayHeight() + 22;
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            super.addWidgets(widgets);
            widgets.add(new PonderButton(2, super.getDisplayHeight() + 2, subject, guide));
        }
    }

    private static final class PonderButton extends Widget {
        private final Bounds bounds;
        private final ResourceLocation subject;
        private final String guide;

        PonderButton(int x, int y, ResourceLocation subject, String guide) {
            this.bounds = new Bounds(x, y, 58, 18);
            this.subject = subject;
            this.guide = guide;
        }

        @Override
        public Bounds getBounds() {
            return bounds;
        }

        @Override
        public void render(GuiGraphics g, int x, int y, float delta) {
            g.blitSprite(
                    ResourceLocation.withDefaultNamespace(
                            bounds.contains(x, y) ? "widget/button_highlighted" : "widget/button"),
                    bounds.x(),
                    bounds.y(),
                    58,
                    18);
            g.drawCenteredString(
                    Minecraft.getInstance().font,
                    Component.literal("Ponder"),
                    bounds.x() + 29,
                    bounds.y() + 5,
                    0xFFFFFFFF);
        }

        @Override
        public boolean mouseClicked(int x, int y, int button) {
            if (button != 0 || !bounds.contains(x, y)) return false;
            UnboundPonders.open(subject, guide);
            return true;
        }
    }

    private UnboundEmiModule() {}
}
