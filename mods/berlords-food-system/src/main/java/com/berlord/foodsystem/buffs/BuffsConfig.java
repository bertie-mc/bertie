package com.berlord.foodsystem.buffs;

import com.berlord.foodsystem.BFS;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-food buff definitions: which effects/attributes/abilities a food grants while it
 * occupies a stomach slot. config/berlords-food-buffs.json, synced to clients as raw JSON.
 */
public final class BuffsConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String FILE_NAME = "berlords-food-buffs.json";

    public record EffectDef(Holder<MobEffect> effect, ResourceLocation effectId, int amplifier) {}

    public record AttributeDef(Holder<Attribute> attribute, AttributeModifier modifier) {}

    public static final class Abilities {
        public boolean flight = false;
        public boolean climbing = false;
        public boolean endermanCalm = false;
        public double magnetRadius = 0.0;
        public double xpBoost = 1.0;
        public double durabilitySaver = 0.0;
    }

    public static final class FoodBuff {
        public final List<EffectDef> effects = new ArrayList<>();
        public final List<AttributeDef> attributes = new ArrayList<>();
        public final Abilities abilities = new Abilities();
        /** optional inline synergy-category override (meat/fish/vegetable/...); null = auto-resolve */
        public String category = null;
    }

    public static final class Runtime {
        public final Map<Item, FoodBuff> foods;
        public final Map<ResourceLocation, Holder<MobEffect>> knownEffects;
        public final List<AttributeDef> knownModifiers;
        public final String rawJson;

        Runtime(Map<Item, FoodBuff> foods, Map<ResourceLocation, Holder<MobEffect>> knownEffects,
                List<AttributeDef> knownModifiers, String rawJson) {
            this.foods = foods;
            this.knownEffects = knownEffects;
            this.knownModifiers = knownModifiers;
            this.rawJson = rawJson;
        }
    }

    private static volatile Runtime CURRENT = new Runtime(Map.of(), Map.of(), List.of(), "{}");

    public static Runtime get() {
        return CURRENT;
    }

    public static void loadFromDisk(Path configDir) {
        Path file = configDir.resolve(FILE_NAME);
        try {
            if (!Files.exists(file)) {
                // migrate from the solvrbuffs era if present
                Path legacy = configDir.resolve("solvrbuffs-foods.json");
                if (Files.exists(legacy)) {
                    Files.copy(legacy, file);
                    LOGGER.info("[bfs] migrated food buffs config from {}", legacy.getFileName());
                } else {
                    Files.writeString(file, DEFAULT_JSON, StandardCharsets.UTF_8);
                    LOGGER.info("[bfs] wrote default food buffs config to {}", file);
                }
            }
            applyJson(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOGGER.error("[bfs] failed to read {}", file, e);
        }
    }

    public static void applyJson(String json) {
        try {
            CURRENT = parse(json);
            SynergyEngine.invalidate(); // inline category overrides may have changed
            LOGGER.info("[bfs] food buffs applied: {} foods configured", CURRENT.foods.size());
        } catch (Exception e) {
            LOGGER.error("[bfs] invalid food buffs json, keeping previous", e);
        }
    }

    private static Runtime parse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        Map<Item, FoodBuff> foods = new HashMap<>();
        Map<ResourceLocation, Holder<MobEffect>> knownEffects = new HashMap<>();
        List<AttributeDef> knownModifiers = new ArrayList<>();

        if (root.has("foods")) {
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("foods").entrySet()) {
                if (entry.getKey().startsWith("_")) continue;
                ResourceLocation itemId = ResourceLocation.tryParse(entry.getKey());
                if (itemId == null) {
                    LOGGER.warn("[bfs] bad item id '{}', skipped", entry.getKey());
                    continue;
                }
                var itemOpt = BuiltInRegistries.ITEM.getOptional(itemId);
                if (itemOpt.isEmpty()) {
                    LOGGER.warn("[bfs] unknown item '{}' (mod not installed?), skipped", itemId);
                    continue;
                }
                foods.put(itemOpt.get(), parseFood(itemId, entry.getValue().getAsJsonObject(), knownEffects, knownModifiers));
            }
        }
        // every effect/attribute a synergy may emit must be in the known sets so BuffEngine's
        // stale-sweep can strip a tier the instant it stops applying
        SynergyEngine.registerKnown(knownEffects, knownModifiers);
        return new Runtime(foods, knownEffects, knownModifiers, json);
    }

    private static FoodBuff parseFood(ResourceLocation itemId, JsonObject obj,
                                      Map<ResourceLocation, Holder<MobEffect>> knownEffects, List<AttributeDef> knownModifiers) {
        FoodBuff buff = new FoodBuff();

        if (obj.has("category") && obj.get("category").isJsonPrimitive()) {
            buff.category = obj.get("category").getAsString();
        }

        if (obj.has("effects")) {
            for (JsonElement el : obj.getAsJsonArray("effects")) {
                JsonObject e = el.getAsJsonObject();
                ResourceLocation id = ResourceLocation.tryParse(e.get("id").getAsString());
                if (id == null) continue;
                var holder = BuiltInRegistries.MOB_EFFECT.getHolder(id);
                if (holder.isEmpty()) {
                    LOGGER.warn("[bfs] unknown effect '{}' on {}, skipped", id, itemId);
                    continue;
                }
                int amp = e.has("amplifier") ? e.get("amplifier").getAsInt() : 0;
                buff.effects.add(new EffectDef(holder.get(), id, amp));
                knownEffects.put(id, holder.get());
            }
        }

        if (obj.has("attributes")) {
            int i = 0;
            for (JsonElement el : obj.getAsJsonArray("attributes")) {
                JsonObject a = el.getAsJsonObject();
                ResourceLocation id = ResourceLocation.tryParse(a.get("id").getAsString());
                if (id == null) continue;
                var holder = BuiltInRegistries.ATTRIBUTE.getHolder(id);
                if (holder.isEmpty()) {
                    LOGGER.warn("[bfs] unknown attribute '{}' on {}, skipped", id, itemId);
                    continue;
                }
                double amount = a.get("amount").getAsDouble();
                AttributeModifier.Operation op = switch (a.has("operation") ? a.get("operation").getAsString() : "add_value") {
                    case "add_multiplied_base" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
                    case "add_multiplied_total" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
                    default -> AttributeModifier.Operation.ADD_VALUE;
                };
                ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath(BFS.MODID,
                        (itemId.getNamespace() + "_" + itemId.getPath()).replaceAll("[^a-z0-9_]", "_") + "_a" + i);
                AttributeDef def = new AttributeDef(holder.get(), new AttributeModifier(modifierId, amount, op));
                buff.attributes.add(def);
                knownModifiers.add(def);
                i++;
            }
        }

        if (obj.has("abilities")) {
            JsonObject ab = obj.getAsJsonObject("abilities");
            if (ab.has("flight")) buff.abilities.flight = ab.get("flight").getAsBoolean();
            if (ab.has("climbing")) buff.abilities.climbing = ab.get("climbing").getAsBoolean();
            if (ab.has("enderman_calm")) buff.abilities.endermanCalm = ab.get("enderman_calm").getAsBoolean();
            if (ab.has("magnet")) buff.abilities.magnetRadius = ab.get("magnet").getAsDouble();
            if (ab.has("xp_boost")) buff.abilities.xpBoost = ab.get("xp_boost").getAsDouble();
            if (ab.has("durability_saver")) buff.abilities.durabilitySaver = ab.get("durability_saver").getAsDouble();
        }
        return buff;
    }

    static final String DEFAULT_JSON = """
            {
              "_doc": [
                "================================ HOW TO GIVE A FOOD BUFFS ================================",
                "Each entry under 'foods' is keyed by the item id and can have three sections:",
                "",
                "--- 1. effects (potion effects, active while the food is in a slot, no particles) ---",
                "VANILLA:  \\"minecraft:cooked_beef\\": { \\"effects\\": [ { \\"id\\": \\"minecraft:strength\\", \\"amplifier\\": 0 } ] }",
                "          amplifier 0 = level I, 1 = level II, and so on.",
                "MODDED:   same format, just the mod's namespace: { \\"id\\": \\"farmersdelight:nourishment\\" },",
                "          { \\"id\\": \\"alexscaves:sugar_rush\\", \\"amplifier\\": 1 } - any effect registered by any installed mod works.",
                "NEGATIVE effects work too: { \\"id\\": \\"minecraft:blindness\\" } makes a cursed food.",
                "",
                "--- 2. attributes (stat modifiers, active while the food is in a slot) ---",
                "operation: add_value (flat), add_multiplied_base (% of base), add_multiplied_total (% of final).",
                "SIZE:            { \\"id\\": \\"minecraft:generic.scale\\", \\"amount\\": -0.25, \\"operation\\": \\"add_multiplied_base\\" }  (25% smaller)",
                "ARMOR TOUGHNESS: { \\"id\\": \\"minecraft:generic.armor_toughness\\", \\"amount\\": 4, \\"operation\\": \\"add_value\\" }",
                "SPEED (flat):    { \\"id\\": \\"minecraft:generic.movement_speed\\", \\"amount\\": 0.02, \\"operation\\": \\"add_value\\" }",
                "SPEED (percent): { \\"id\\": \\"minecraft:generic.movement_speed\\", \\"amount\\": 0.15, \\"operation\\": \\"add_multiplied_total\\" }  (+15%)",
                "Other useful ones: generic.attack_damage, generic.attack_speed, generic.armor, generic.knockback_resistance,",
                "generic.step_height, generic.safe_fall_distance, generic.block_interaction_range, generic.entity_interaction_range,",
                "generic.luck, generic.gravity, generic.jump_strength, player.block_break_speed, player.mining_efficiency,",
                "player.sneaking_speed, player.submerged_mining_speed, player.sweeping_damage_ratio, neoforge:swim_speed,",
                "neoforge:creative_flight, neoforge:nametag_distance. Modded attributes work the same way (modid:attribute_name).",
                "",
                "--- 3. abilities (custom mechanics, not potion effects) ---",
                "\\"abilities\\": { \\"flight\\": true,            creative-style flight",
                "                \\"climbing\\": true,          spider wall-climb",
                "                \\"enderman_calm\\": true,     endermen don't aggro from your gaze (like wearing a pumpkin)",
                "                \\"magnet\\": 8.0,             pulls items & xp orbs within this radius (blocks)",
                "                \\"xp_boost\\": 1.5,           XP multiplier (1.0 = off)",
                "                \\"durability_saver\\": 0.3 }  30% chance held tools take no durability damage",
                "",
                "--- 4. category (optional) — overrides this food's auto-detected synergy category ---",
                "\\"category\\": \\"meat\\"   one of: meat, fish, vegetable, fruit, grain, sweet, meal, special, cursed.",
                "Categories drive FOOD SYNERGIES (needs 3+ stomach slots, no config entry required):",
                "  VARIETY - 3+ DIFFERENT categories in the stomach: faster regen; 4 = +max health; 5 = more regen, more health, Speed.",
                "  MONO    - EVERY slot the SAME category: a specialization with a perk and a downside",
                "            (all meat = Strength + attack speed, slower mining; all fish = Luck + water breathing, slower; etc.).",
                "Categories are auto-detected from c:foods/* tags + item names; set \\"category\\" only to correct a mis-detect.",
                "",
                "All sections can be combined on one food. Reload in game with /bfs reload.",
                "Run /bfs dumpids to write EVERY effect & attribute id available in THIS instance (incl. modded)",
                "to config/berlords-food-system-ids.txt.",
                "",
                "--- vanilla 1.21 effects, for reference (prefix with minecraft:) ---",
                "speed, slowness, haste, mining_fatigue, strength, instant_health, instant_damage, jump_boost,",
                "nausea, regeneration, resistance, fire_resistance, water_breathing, invisibility, blindness,",
                "night_vision, hunger, weakness, poison, wither, health_boost, absorption, saturation, glowing,",
                "levitation, luck, unluck, slow_falling, conduit_power, dolphins_grace, bad_omen, hero_of_the_village,",
                "darkness, trial_omen, raid_omen, wind_charged, weaving, oozing, infested",
                "",
                "System settings (slot counts, replace-oldest, durations, regen, cooldowns...) live in",
                "config/berlords_food_system-common.toml."
              ],
              "foods": {
                "minecraft:golden_carrot": { "effects": [ { "id": "minecraft:night_vision", "amplifier": 0 } ] },
                "minecraft:cooked_salmon": { "attributes": [ { "id": "neoforge:swim_speed", "amount": 0.15, "operation": "add_multiplied_total" } ] },
                "minecraft:rabbit_stew": { "effects": [ { "id": "minecraft:jump_boost", "amplifier": 0 } ] },
                "minecraft:glow_berries": { "effects": [ { "id": "minecraft:glowing", "amplifier": 0 } ] },
                "minecraft:cookie": { "attributes": [ { "id": "minecraft:generic.scale", "amount": -0.5, "operation": "add_multiplied_base" } ] },
                "minecraft:pumpkin_pie": { "attributes": [ { "id": "minecraft:generic.scale", "amount": 0.3, "operation": "add_multiplied_base" } ] },
                "minecraft:chorus_fruit": { "abilities": { "enderman_calm": true } }
              }
            }
            """;

    private BuffsConfig() {}
}
