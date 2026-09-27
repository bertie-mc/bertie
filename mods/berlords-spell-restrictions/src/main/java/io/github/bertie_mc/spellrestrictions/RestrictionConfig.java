package io.github.bertie_mc.spellrestrictions;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class RestrictionConfig {
    public enum Tier {
        NONE,
        COMMON,
        UNCOMMON,
        RARE,
        EPIC,
        LEGENDARY;

        public int rank() {
            return ordinal() - 1;
        }
    }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.EnumValue<Tier> STARTING_TIER;
    public static final ModConfigSpec.BooleanValue REQUIRE_DISCOVERY;
    public static final ModConfigSpec.BooleanValue REQUIRE_RARITY;

    static {
        var builder = new ModConfigSpec.Builder();
        STARTING_TIER = builder.comment(
                        "Initial crafting tier for a new player. NONE also locks Common. Existing permanent unlocks are preserved.")
                .defineEnum("startingTier", Tier.COMMON);
        REQUIRE_DISCOVERY = builder.comment(
                        "Require prior scroll possession or manuscript research before crafting/upgrading a spell.")
                .define("requireSpellDiscovery", true);
        REQUIRE_RARITY = builder.comment("Require the output scroll's rarity to be unlocked before crafting/upgrading.")
                .define("requireCraftingRarity", true);
        SPEC = builder.build();
    }

    private RestrictionConfig() {}
}
