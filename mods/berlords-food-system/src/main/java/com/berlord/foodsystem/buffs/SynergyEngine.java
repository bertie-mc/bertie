package com.berlord.foodsystem.buffs;

import com.berlord.foodsystem.BFS;
import com.berlord.foodsystem.Config;
import com.berlord.foodsystem.stomach.Stomach;
import com.berlord.foodsystem.stomach.StomachData;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Food synergy: each tick the categories of the active stomach foods are resolved and ONE
 * synthetic {@link BuffsConfig.FoodBuff} is appended to {@link ActiveFoods#activeBuffs}, so it
 * inherits {@link BuffEngine}'s existing effect/attribute merge and stale-sweep for free.
 *
 * Two layers, both gated to 3+ unlocked slots and mutually exclusive by construction:
 *  - VARIETY (3+ distinct categories): faster passive regen, then bonus max health and Speed.
 *  - MONO (a FULL stomach of ONE category): a specialization with a perk and an attribute downside.
 *
 * The variety passive-regen tier is NOT expressible as a FoodBuff (it tunes the mod's own
 * {@code regenFactor}, not the Regeneration effect), so it rides a separate hook in
 * {@link Stomach#regenFactor}; see {@link #varietyRegenBonus}.
 */
public final class SynergyEngine {

    // counting categories (variety distinctness, mono specialization)
    public static final String MEAT = "meat", FISH = "fish", VEGETABLE = "vegetable", FRUIT = "fruit",
            GRAIN = "grain", SWEET = "sweet", MEAL = "meal", SPECIAL = "special";
    // non-counting flag: suppresses variety entirely and breaks any mono diet
    public static final String CURSED = "cursed";

    private static final Set<String> COUNTING = Set.of(MEAT, FISH, VEGETABLE, FRUIT, GRAIN, SWEET, MEAL, SPECIAL);

    /** added to the food regen sum (lowers ticks-per-heal) — see {@link Stomach#regenFactor} */
    private static final double REGEN_BONUS_3 = 40.0;
    private static final double REGEN_BONUS_5 = 80.0;

    // ---- convention (c:) item tags, NeoForge 21.1.x singular spelling ----
    private static final TagKey<Item> RAW_MEAT = ctag("foods/raw_meat");
    private static final TagKey<Item> COOKED_MEAT = ctag("foods/cooked_meat");
    private static final TagKey<Item> RAW_FISH = ctag("foods/raw_fish");
    private static final TagKey<Item> COOKED_FISH = ctag("foods/cooked_fish");
    private static final TagKey<Item> VEGETABLE_TAG = ctag("foods/vegetable");
    private static final TagKey<Item> FRUIT_TAG = ctag("foods/fruit");
    private static final TagKey<Item> BERRY = ctag("foods/berry");
    private static final TagKey<Item> BREAD = ctag("foods/bread");
    private static final TagKey<Item> PIE = ctag("foods/pie");
    private static final TagKey<Item> COOKIE = ctag("foods/cookie");
    private static final TagKey<Item> CANDY = ctag("foods/candy");
    private static final TagKey<Item> SOUP = ctag("foods/soup");
    private static final TagKey<Item> GOLDEN = ctag("foods/golden");
    private static final TagKey<Item> FOOD_POISONING = ctag("foods/food_poisoning");

    private static TagKey<Item> ctag(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }

    // ---- category cache (invalidated on config reload + datapack reload) ----
    private static final String NONE = ""; // ConcurrentHashMap can't hold null values
    private static final Map<Item, String> CACHE = new ConcurrentHashMap<>();

    public static void invalidate() {
        CACHE.clear();
    }

    /** resolved category for an item, or null when uncategorized (no synergy contribution) */
    static String categoryOf(Item item) {
        String c = CACHE.computeIfAbsent(item, SynergyEngine::resolve);
        return c.isEmpty() ? null : c;
    }

    private static String resolve(Item item) {
        // 1. explicit inline override from the buffs config
        BuffsConfig.FoodBuff fb = BuffsConfig.get().foods.get(item);
        if (fb != null && fb.category != null && !fb.category.isBlank()) return fb.category;

        Holder.Reference<Item> h = item.builtInRegistryHolder();
        // 2. cursed wins over any counting category (raw chicken etc. are also "raw_meat")
        if (h.is(FOOD_POISONING)) return CURSED;
        // 3. convention tag map
        if (h.is(COOKED_MEAT) || h.is(RAW_MEAT)) return MEAT;
        if (h.is(COOKED_FISH) || h.is(RAW_FISH)) return FISH;
        if (h.is(VEGETABLE_TAG)) return VEGETABLE;
        if (h.is(FRUIT_TAG) || h.is(BERRY)) return FRUIT;
        if (h.is(BREAD) || h.is(PIE)) return GRAIN;
        if (h.is(COOKIE) || h.is(CANDY)) return SWEET;
        if (h.is(SOUP)) return MEAL;
        if (h.is(GOLDEN)) return SPECIAL;
        // 4. honey is a drink by tag but plays as sweet until a pack enables the drink category
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id.equals(ResourceLocation.withDefaultNamespace("honey_bottle"))) return SWEET;
        // 5. name heuristic for modded foods that ship no convention tags
        String byName = heuristic(id.getPath());
        return byName != null ? byName : NONE;
    }

    private static String heuristic(String path) {
        if (containsAny(path, "beef", "pork", "chicken", "mutton", "rabbit", "bacon", "steak", "ham", "meat", "venison")) return MEAT;
        if (containsAny(path, "fish", "cod", "salmon", "tuna", "sushi", "calamari", "shrimp", "crab", "clam")) return FISH;
        if (containsAny(path, "potato", "carrot", "beet", "cabbage", "tomato", "onion", "lettuce", "corn", "mushroom", "kale", "vegetable")) return VEGETABLE;
        if (containsAny(path, "apple", "berry", "melon", "grape", "peach", "cherry", "banana", "orange", "mango", "fruit")) return FRUIT;
        if (containsAny(path, "bread", "wheat", "rice", "dough", "toast", "bun", "bagel", "cracker", "pasta", "noodle")) return GRAIN;
        if (containsAny(path, "cookie", "cake", "candy", "chocolate", "sugar", "pie", "donut", "pudding", "muffin", "jam", "sweet")) return SWEET;
        if (containsAny(path, "soup", "stew", "salad", "sandwich", "burger", "pizza", "curry", "_bowl", "meal")) return MEAL;
        return null;
    }

    private static boolean containsAny(String s, String... needles) {
        for (String n : needles) if (s.contains(n)) return true;
        return false;
    }

    // ---- synthetic synergy buffs, built once registries are available ----
    private static volatile boolean built = false;
    private static BuffsConfig.FoodBuff VAR4, VAR5;
    private static final Map<String, BuffsConfig.FoodBuff> MONO = new java.util.HashMap<>();
    private static final List<BuffsConfig.EffectDef> ALL_EFFECTS = new ArrayList<>();
    private static final List<BuffsConfig.AttributeDef> ALL_ATTRS = new ArrayList<>();

    private static synchronized void ensureDefs() {
        if (built) return;

        // VARIETY tiers (regen tier 3 is handled in Stomach.regenFactor, not here)
        VAR4 = buff(null, new BuffsConfig.AttributeDef[]{
                attr("synergy_variety_health4", "minecraft:generic.max_health", 0.10, Operation.ADD_MULTIPLIED_TOTAL)});
        VAR5 = buff(new BuffsConfig.EffectDef[]{eff("minecraft:speed", 0)}, new BuffsConfig.AttributeDef[]{
                attr("synergy_variety_health5", "minecraft:generic.max_health", 0.15, Operation.ADD_MULTIPLIED_TOTAL)});

        // MONO specializations: perk(s) + a negative-attribute downside
        MONO.put(MEAT, buff(new BuffsConfig.EffectDef[]{eff("minecraft:strength", 0)}, new BuffsConfig.AttributeDef[]{
                attr("synergy_mono_meat_attack_speed", "minecraft:generic.attack_speed", 0.20, Operation.ADD_MULTIPLIED_TOTAL),
                attr("synergy_mono_meat_block_break", "minecraft:player.block_break_speed", -0.20, Operation.ADD_MULTIPLIED_TOTAL)}));
        MONO.put(FISH, buff(new BuffsConfig.EffectDef[]{eff("minecraft:luck", 0), eff("minecraft:water_breathing", 0)},
                new BuffsConfig.AttributeDef[]{
                        attr("synergy_mono_fish_speed", "minecraft:generic.movement_speed", -0.10, Operation.ADD_MULTIPLIED_TOTAL)}));
        MONO.put(VEGETABLE, buff(new BuffsConfig.EffectDef[]{eff("minecraft:haste", 0)}, new BuffsConfig.AttributeDef[]{
                attr("synergy_mono_veg_step_height", "minecraft:generic.step_height", 1.0, Operation.ADD_VALUE),
                attr("synergy_mono_veg_attack_damage", "minecraft:generic.attack_damage", -0.20, Operation.ADD_MULTIPLIED_TOTAL)}));
        MONO.put(FRUIT, buff(new BuffsConfig.EffectDef[]{eff("minecraft:jump_boost", 0)}, new BuffsConfig.AttributeDef[]{
                attr("synergy_mono_fruit_speed", "minecraft:generic.movement_speed", 0.15, Operation.ADD_MULTIPLIED_TOTAL),
                attr("synergy_mono_fruit_health", "minecraft:generic.max_health", -0.10, Operation.ADD_MULTIPLIED_TOTAL)}));
        MONO.put(SWEET, buff(new BuffsConfig.EffectDef[]{eff("minecraft:speed", 0), eff("minecraft:haste", 0),
                eff("minecraft:weakness", 0)}, null));

        built = true;
    }

    private static BuffsConfig.FoodBuff buff(BuffsConfig.EffectDef[] effs, BuffsConfig.AttributeDef[] attrs) {
        BuffsConfig.FoodBuff b = new BuffsConfig.FoodBuff();
        if (effs != null) for (BuffsConfig.EffectDef e : effs) b.effects.add(e);
        if (attrs != null) for (BuffsConfig.AttributeDef a : attrs) b.attributes.add(a);
        return b;
    }

    private static BuffsConfig.EffectDef eff(String id, int amp) {
        ResourceLocation rl = ResourceLocation.parse(id);
        Holder<MobEffect> h = BuiltInRegistries.MOB_EFFECT.getHolder(rl)
                .orElseThrow(() -> new IllegalStateException("[bfs] synergy effect missing: " + id));
        BuffsConfig.EffectDef def = new BuffsConfig.EffectDef(h, rl, amp);
        ALL_EFFECTS.add(def);
        return def;
    }

    private static BuffsConfig.AttributeDef attr(String modPath, String attrId, double amount, Operation op) {
        Holder<Attribute> h = BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(attrId))
                .orElseThrow(() -> new IllegalStateException("[bfs] synergy attribute missing: " + attrId));
        ResourceLocation modId = ResourceLocation.fromNamespaceAndPath(BFS.MODID, modPath);
        BuffsConfig.AttributeDef def = new BuffsConfig.AttributeDef(h, new AttributeModifier(modId, amount, op));
        ALL_ATTRS.add(def);
        return def;
    }

    /**
     * Every effect/attribute id a synergy CAN emit, registered into the runtime's known sets so
     * {@link BuffEngine}'s stale-sweep removes a tier the moment it stops being desired. Called
     * once per config (re)load. The ~10-line engine touch the whole design hinges on.
     */
    public static void registerKnown(Map<ResourceLocation, Holder<MobEffect>> knownEffects,
                                     List<BuffsConfig.AttributeDef> knownModifiers) {
        ensureDefs();
        for (BuffsConfig.EffectDef e : ALL_EFFECTS) knownEffects.put(e.effectId(), e.effect());
        knownModifiers.addAll(ALL_ATTRS);
    }

    /** the synthetic buff for this tick's stomach contents, or null when no synergy applies */
    public static BuffsConfig.FoodBuff computeSynergy(Player player) {
        boolean variety = Config.SYNERGY_VARIETY.get();
        boolean mono = Config.SYNERGY_MONO.get();
        if (!variety && !mono) return null;

        int unlocked = Stomach.unlockedSlots(player);
        if (unlocked < 3) return null; // global gate

        Summary s = summarize(player, unlocked);
        ensureDefs();

        if (mono && s.full && !s.cursed && !s.monoBroken && s.monoCategory != null) {
            BuffsConfig.FoodBuff b = MONO.get(s.monoCategory);
            if (b != null) return b;
        }
        if (variety && !s.cursed) {
            if (s.distinct >= 5) return VAR5;
            if (s.distinct >= 4) return VAR4;
            // distinct == 3 grants regen only (varietyRegenBonus), no effects/attributes
        }
        return null;
    }

    /** ticks added to the food regen sum from the variety tier (faster passive regen) */
    public static double varietyRegenBonus(Player player) {
        if (!Config.SYNERGY_VARIETY.get()) return 0.0;
        int unlocked = Stomach.unlockedSlots(player);
        if (unlocked < 3) return 0.0;
        Summary s = summarize(player, unlocked);
        if (s.cursed) return 0.0;
        if (s.distinct >= 5) return REGEN_BONUS_5;
        if (s.distinct >= 3) return REGEN_BONUS_3;
        return 0.0;
    }

    private record Summary(int distinct, boolean full, boolean cursed, String monoCategory, boolean monoBroken) {}

    private static Summary summarize(Player player, int unlocked) {
        StomachData data = Stomach.get(player);
        boolean full = true, cursed = false, monoBroken = false;
        String monoCategory = null;
        Set<String> distinct = new HashSet<>();
        for (int i = 0; i < unlocked; i++) {
            StomachData.FoodSlot slot = data.slots[i];
            if (!slot.isActive()) {
                full = false;
                monoBroken = true; // an empty/expiring slot is not a mono diet
                continue;
            }
            String cat = categoryOf(slot.stack.getItem());
            if (CURSED.equals(cat)) {
                cursed = true;
                monoBroken = true;
                continue;
            }
            if (cat == null || !COUNTING.contains(cat)) {
                monoBroken = true; // uncategorized / non-counting (e.g. drink) counts for neither
                continue;
            }
            distinct.add(cat);
            if (monoCategory == null) monoCategory = cat;
            else if (!monoCategory.equals(cat)) monoBroken = true;
        }
        return new Summary(distinct.size(), full, cursed, monoCategory, monoBroken);
    }

    private SynergyEngine() {}
}
