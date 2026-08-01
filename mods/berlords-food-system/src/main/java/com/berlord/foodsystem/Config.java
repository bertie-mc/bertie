package com.berlord.foodsystem;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {

    public static final ModConfigSpec COMMON_SPEC;
    public static final ModConfigSpec CLIENT_SPEC;

    // ---- common ----
    public static ModConfigSpec.IntValue DEFAULT_SLOTS;
    public static ModConfigSpec.IntValue MAX_SLOTS;
    public static ModConfigSpec.DoubleValue BASE_HEALTH;
    public static ModConfigSpec.DoubleValue TOTAL_HEALTH;
    public static ModConfigSpec.DoubleValue BASE_REGEN_RATE;
    public static ModConfigSpec.DoubleValue REGEN_COOLDOWN;
    public static ModConfigSpec.DoubleValue FOOD_DURATION;
    public static ModConfigSpec.BooleanValue BONUS_DURATION;
    public static ModConfigSpec.BooleanValue SLEEP_DEPLETION;
    public static ModConfigSpec.IntValue EAT_COOLDOWN_TICKS;
    public static ModConfigSpec.BooleanValue DISABLE_REGENERATION;
    public static ModConfigSpec.BooleanValue ALLOW_SATURATION;
    public static ModConfigSpec.BooleanValue REPLACE_OLDEST;
    public static ModConfigSpec.BooleanValue DISABLE_SPRINT_WHEN_EMPTY;
    public static ModConfigSpec.BooleanValue SB_FEEDING;
    public static ModConfigSpec.IntValue SB_FEEDING_SCAN_TICKS;
    public static ModConfigSpec.IntValue SAME_FOOD_REFRESH_TICKS;
    public static ModConfigSpec.BooleanValue DISABLE_NATURAL_REGEN_RULE;
    public static ModConfigSpec.BooleanValue SYNERGY_VARIETY;
    public static ModConfigSpec.BooleanValue SYNERGY_MONO;

    // ---- client ----
    public static ModConfigSpec.ConfigValue<String> HUD_PRESET;
    public static ModConfigSpec.DoubleValue HUD_X;
    public static ModConfigSpec.DoubleValue HUD_Y;
    public static ModConfigSpec.DoubleValue HUD_SCALE;

    static {
        ModConfigSpec.Builder common = new ModConfigSpec.Builder();

        common.push("stomach");
        DEFAULT_SLOTS = common.comment("Number of stomach slots every player starts with")
                .defineInRange("default_slots", 3, 1, 5);
        MAX_SLOTS = common.comment("Maximum number of stomach slots reachable (min = default_slots)")
                .defineInRange("max_slots", 5, 1, 5);
        REPLACE_OLDEST = common.comment("Eating with a full stomach replaces the food with the least remaining time")
                .define("replace_oldest_when_full", true);
        DISABLE_SPRINT_WHEN_EMPTY = common.comment("Disable sprinting while the stomach is completely empty (vanilla-style: an empty stomach can't start or sustain a sprint)")
                .define("disable_sprint_when_empty", true);
        EAT_COOLDOWN_TICKS = common.comment("Item cooldown after eating a food. To avoid accidental waste. Foods tagged always_edible have no cooldown.")
                .defineInRange("eat_cooldown_ticks", 100, 0, 1200);
        SAME_FOOD_REFRESH_TICKS = common.comment("Sophisticated Backpacks feeding upgrades only re-feed a food already in a slot once its remaining time drops below this many ticks (prevents auto-feeders from burning food)")
                .defineInRange("same_food_refresh_ticks", 600, 0, 72000);
        common.pop();

        common.push("health");
        BASE_HEALTH = common.comment("Health the player has with an empty stomach (2 = 1 heart)")
                .defineInRange("base_health", 6.0, 2.0, 20.0);
        TOTAL_HEALTH = common.comment("Maximum health reachable through food")
                .defineInRange("total_health", 40.0, 20.0, 800.0);
        common.pop();

        common.push("regen");
        BASE_REGEN_RATE = common.comment("Base ticks per 1 health of passive regeneration with an empty stomach (lowered by food regen values, floor 10). Default 200 = 1 HP / 10s.")
                .defineInRange("base_regen_rate", 200.0, 20.0, 600.0);
        REGEN_COOLDOWN = common.comment("Ticks after taking damage before regeneration resumes (0 = no cooldown). Default 160 = 8s.")
                .defineInRange("regen_cooldown", 160.0, 0.0, 600.0);
        DISABLE_REGENERATION = common.comment("Disable passive regeneration entirely")
                .define("disable_regeneration", false);
        DISABLE_NATURAL_REGEN_RULE = common.comment("Force the naturalRegeneration gamerule off on world load (vanilla regen would bypass the food system)")
                .define("disable_natural_regeneration_gamerule", true);
        common.pop();

        common.push("food");
        FOOD_DURATION = common.comment("Seconds of duration per saturation point (Reforged formula)")
                .defineInRange("food_duration", 180.0, 60.0, 600.0);
        BONUS_DURATION = common.comment("Foods that don't stack to 64 get bonus duration")
                .define("unstackable_food_bonus_duration", true);
        SLEEP_DEPLETION = common.comment("Sleeping consumes half the remaining duration of eaten foods")
                .define("food_depletion_during_sleep", false);
        ALLOW_SATURATION = common.comment("Allow food to apply saturation and food-related effects like Farmer's Delight nourishment")
                .define("allow_saturation", false);
        common.pop();

        common.push("compat");
        SB_FEEDING = common.comment("Sophisticated Backpacks feeding upgrades feed only when a stomach slot is actually free (prevents wasted food); false blocks them entirely")
                .define("sophisticated_backpacks_feeding", true);
        SB_FEEDING_SCAN_TICKS = common.comment("Fallback scan interval (ticks) when a feeding upgrade has nothing to do — stomach full, OR empty slots but nothing eligible to feed. The stomach pins hunger, so SB would otherwise rescan every 2-5s forever. The feeder is woken immediately when the backpack contents change or a stomach slot frees up, so this is just the backstop; 580 still guarantees a refresh inside the 600-tick re-feed window = 100% buff uptime.")
                .defineInRange("sophisticated_backpacks_scan_ticks", 580, 20, 6000);
        common.pop();

        common.push("synergy");
        SYNERGY_VARIETY = common.comment("Variety synergy: 3+ DIFFERENT food categories (meat, fish, vegetable, fruit, grain, sweet, meal, special) in the stomach grant faster regen, then bonus max health (4) and Speed (5). A cursed food (e.g. rotten flesh) suppresses it. Needs 3+ stomach slots.")
                .define("variety_synergy", true);
        SYNERGY_MONO = common.comment("Mono-diet synergy: filling EVERY stomach slot with one category grants a specialization with a downside (all meat = Carnivore: Strength + attack speed, slower mining; all fish = Mariner: Luck + water breathing, slower; all vegetable = Forager; all fruit = Pathfinder; all sweet = Sugar Rush). Needs 3+ stomach slots.")
                .define("mono_synergy", true);
        common.pop();

        COMMON_SPEC = common.build();

        ModConfigSpec.Builder client = new ModConfigSpec.Builder();
        client.push("hud");
        HUD_PRESET = client.comment("Position of the stomach slots on the HUD (default, bottom_right, bottom_left, custom)")
                .define("preset", "default");
        HUD_X = client.comment("Custom preset: X offset from screen center of the LEFT slot when 3 slots are unlocked (extra slots grow further left)")
                .defineInRange("x", 47.0, -10000.0, 10000.0);
        HUD_Y = client.comment("Custom preset: Y offset from screen bottom")
                .defineInRange("y", -39.0, -10000.0, 10000.0);
        HUD_SCALE = client.comment("HUD scale")
                .defineInRange("scale", 0.9, 0.1, 5.0);
        client.pop();
        CLIENT_SPEC = client.build();
    }

    public static int defaultSlots() {
        return DEFAULT_SLOTS.get();
    }

    public static int maxSlots() {
        return Math.max(MAX_SLOTS.get(), defaultSlots());
    }

    private Config() {}
}
