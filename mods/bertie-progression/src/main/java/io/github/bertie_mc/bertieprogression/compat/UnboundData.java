package io.github.bertie_mc.bertieprogression.compat;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Apply pack material aliases to native classpath tables, including launch costs. */
public final class UnboundData {
    private static final JsonObject CONFIG = own("/unbound_material_aliases.json");
    private static final JsonObject SPECTRAL = own("/unbound_spectral_extension.json");

    private static JsonObject own(String path) {
        try (var stream = UnboundData.class.getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("Missing " + path);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    public static InputStream resource(ClassLoader loader, String path) {
        InputStream input = loader.getResourceAsStream(path);
        if (input == null || !path.startsWith("assets/unbound/") || !path.endsWith(".json")) return input;
        try (input) {
            JsonElement json = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            if (path.equals("assets/unbound/spectral.json")) extendSpectral(json.getAsJsonObject());
            json = aliases(json, path.contains("guide/recipes"));
            if (path.equals("assets/unbound/spectral.json"))
                for (var source : json.getAsJsonObject().getAsJsonArray("sources")) {
                    var items = source.getAsJsonObject().getAsJsonArray("items");
                    var unique = new JsonArray();
                    var seen = new LinkedHashSet<String>();
                    for (var item : items) if (seen.add(item.getAsString())) unique.add(item);
                    source.getAsJsonObject().add("items", unique);
                }
            return new ByteArrayInputStream(json.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private static JsonElement aliases(JsonElement value, boolean recipes) {
        var merges = CONFIG.getAsJsonObject("merges");
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            var replacement = merges.get(value.getAsString());
            return replacement == null ? value : replacement.deepCopy();
        }
        if (value.isJsonArray()) {
            var out = new JsonArray();
            for (var v : value.getAsJsonArray()) out.add(aliases(v, recipes));
            return out;
        }
        if (value.isJsonObject()) {
            var out = new JsonObject();
            for (var entry : value.getAsJsonObject().entrySet()) {
                if (recipes && retired(entry.getKey())) continue;
                var key =
                        merges.has(entry.getKey()) ? merges.get(entry.getKey()).getAsString() : entry.getKey();
                // Gear resolves these before other mods register items; resolve aliases when repairing.
                out.add(
                        key,
                        entry.getKey().equals("repair") && entry.getValue().isJsonPrimitive()
                                ? entry.getValue()
                                : aliases(entry.getValue(), recipes));
            }
            return out;
        }
        return value;
    }

    private static boolean retired(String id) {
        for (var item : CONFIG.getAsJsonArray("retired")) if (item.getAsString().equals(id)) return true;
        return false;
    }

    public static Item repairMaterial(Item item) {
        var alias = CONFIG.getAsJsonObject("merges")
                .get(BuiltInRegistries.ITEM.getKey(item).toString());
        return alias == null ? item : BuiltInRegistries.ITEM.get(ResourceLocation.parse(alias.getAsString()));
    }

    private static void extendSpectral(JsonObject root) {
        var refs = root.getAsJsonArray("refs");
        Set<String> existing = new HashSet<>();
        for (var r : refs) existing.add(r.getAsJsonObject().get("id").getAsString());
        for (var ref : SPECTRAL.getAsJsonArray("refs"))
            if (existing.add(ref.getAsJsonObject().get("id").getAsString())) refs.add(ref.deepCopy());
        var sources = root.getAsJsonArray("sources");
        Map<String, JsonObject> byId = new LinkedHashMap<>();
        for (var s : sources) byId.put(s.getAsJsonObject().get("id").getAsString(), s.getAsJsonObject());
        for (var element : SPECTRAL.getAsJsonArray("sources")) {
            var added = element.getAsJsonObject().deepCopy();
            var id = added.get("id").getAsString();
            if (byId.containsKey(id)) {
                var old = byId.get(id);
                old.add("refs", added.get("refs"));
                append(old.getAsJsonArray("items"), added.getAsJsonArray("items"));
            } else {
                sources.add(added);
                byId.put(id, added);
            }
        }
        for (var alias : SPECTRAL.getAsJsonObject("aliases").entrySet()) {
            var source = byId.get(alias.getKey());
            if (source == null) throw new IllegalStateException("Missing native spectral source " + alias.getKey());
            append(source.getAsJsonArray("items"), alias.getValue().getAsJsonArray());
        }
        var ores = root.getAsJsonObject("uses").getAsJsonObject("ores");
        for (var entry : SPECTRAL.getAsJsonObject("ores").entrySet())
            ores.add(entry.getKey(), entry.getValue().deepCopy());
    }

    private static void append(JsonArray target, JsonArray items) {
        Set<String> ids = new HashSet<>();
        for (var i : target) ids.add(i.getAsString());
        for (var i : items) if (ids.add(i.getAsString())) target.add(i.deepCopy());
    }

    /** Remove retired native journal nodes and flatten their prerequisites. */
    public static void journal(List<?> raw) {
        try {
            List<Object> nodes = new ArrayList<>(raw);
            Map<String, List<String>> removed = new HashMap<>();
            Set<String> unwanted = new HashSet<>();
            for (var id : CONFIG.getAsJsonArray("guide_removed")) unwanted.add(id.getAsString());
            for (Object node : nodes)
                if (unwanted.contains(call(node, "id")))
                    removed.put((String) call(node, "id"), (List<String>) call(node, "parents"));
            var result = new ArrayList<>();
            for (Object node : nodes) {
                String id = (String) call(node, "id");
                if (removed.containsKey(id)) continue;
                var parts = node.getClass().getRecordComponents();
                Object[] values = new Object[parts.length];
                Class<?>[] types = new Class<?>[parts.length];
                for (int i = 0; i < parts.length; i++) {
                    values[i] = parts[i].getAccessor().invoke(node);
                    types[i] = parts[i].getType();
                    if (parts[i].getName().equals("parents"))
                        values[i] = flatten((List<String>) values[i], removed, new HashSet<>());
                    if (CONFIG.getAsJsonObject("guide_merges").has(id)) {
                        String item =
                                CONFIG.getAsJsonObject("guide_merges").get(id).getAsString();
                        if (parts[i].getName().equals("item"))
                            values[i] = (Supplier<Item>) () -> BuiltInRegistries.ITEM.get(ResourceLocation.parse(item));
                        if (parts[i].getName().equals("goal"))
                            values[i] = Class.forName("me.moltea.unbound.progress.Goal$Holding")
                                    .getConstructor(Predicate.class)
                                    .newInstance((Predicate<ItemStack>) stack ->
                                            stack.is(BuiltInRegistries.ITEM.get(ResourceLocation.parse(item))));
                    }
                }
                result.add(node.getClass().getConstructor(types).newInstance(values));
            }
            ((List) raw).clear();
            ((List) raw).addAll(result);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Cannot update Unbound journal", failure);
        }
    }

    private static Object call(Object object, String method) throws ReflectiveOperationException {
        return object.getClass().getMethod(method).invoke(object);
    }

    private static List<String> flatten(List<String> parents, Map<String, List<String>> removed, Set<String> seen) {
        var result = new LinkedHashSet<String>();
        for (String parent : parents) {
            if (!removed.containsKey(parent)) result.add(parent);
            else if (seen.add(parent)) result.addAll(flatten(removed.get(parent), removed, seen));
        }
        return List.copyOf(result);
    }

    private UnboundData() {}
}
