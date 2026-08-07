package io.github.bertie_mc.bertieprogression.forge;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The Brick Forge's ore smelts have a 1% chance of also yielding a storage block of what they made.
 *
 * <p>{@code slag:double_smelting} has no secondary-output field - its codec is exactly group,
 * ingredientA, ingredientB, result, experience, cookingTime - so the bonus cannot be data alone. The
 * table of which result yields which block IS data: {@code brick_forge_bonus.json} at the jar root,
 * written by {@code texture-work/gen_data.py} from the pack's own {@code c:ingots}, {@code c:gems}
 * and {@code c:storage_blocks} tags. Nothing here names an ore.
 *
 * <p>Read from the JAR ROOT rather than a datapack, for the same reason {@code removed_items.json}
 * is: a classpath resource is always there and needs no reload listener to stay in step.
 */
public final class BrickForgeBonus {

    /** One in a hundred. Deliberately small: this is a surprise, not a yield mechanic. */
    public static final float CHANCE = 0.01F;

    private static Map<Item, Item> table;

    private static Map<Item, Item> table() {
        if (table == null) {
            table = read();
        }
        return table;
    }

    private static Map<Item, Item> read() {
        Map<Item, Item> parsed = new HashMap<>();
        try (InputStream in = BrickForgeBonus.class.getResourceAsStream("/brick_forge_bonus.json")) {
            if (in == null) {
                return Collections.emptyMap();
            }
            Map<String, String> raw = new Gson()
                    .fromJson(
                            new InputStreamReader(in, StandardCharsets.UTF_8),
                            new TypeToken<Map<String, String>>() {}.getType());
            if (raw == null) {
                return Collections.emptyMap();
            }
            for (Map.Entry<String, String> e : raw.entrySet()) {
                Item from = item(e.getKey());
                Item to = item(e.getValue());
                // A pack without the mod that owns either side is not an error: skip the row.
                if (from != null && to != null) {
                    parsed.put(from, to);
                }
            }
        } catch (Exception e) {
            // A broken table must not take a smelt down with it - ship no bonus instead.
            return Collections.emptyMap();
        }
        return parsed;
    }

    private static Item item(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null || !BuiltInRegistries.ITEM.containsKey(rl)) {
            return null;
        }
        return BuiltInRegistries.ITEM.get(rl);
    }

    /**
     * The bonus a finished smelt owes, or an empty stack if this result has none or the roll failed.
     */
    public static ItemStack roll(ItemStack result, RandomSource random) {
        if (result.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Item block = table().get(result.getItem());
        if (block == null || random.nextFloat() >= CHANCE) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(block);
    }

    private BrickForgeBonus() {}
}
