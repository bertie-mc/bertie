package io.github.bertie_mc.creatures;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CreatureConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public final ModConfigSpec.IntValue nucleeperFuseTime =
            BUILDER.defineInRange("nucleeper_fuse_time", 300, 20, Integer.MAX_VALUE);
    public final ModConfigSpec.IntValue atlatitanMaxExplosionResistance =
            BUILDER.defineInRange("atlatitan_max_block_explosion_resistance", 10, 0, Integer.MAX_VALUE);
    public final ModConfigSpec.IntValue nukeMaxBlockExplosionResistance =
            BUILDER.defineInRange("nuke_max_block_explosion_resistance", 1000, 0, Integer.MAX_VALUE);
    public final ModConfigSpec.DoubleValue luxtructosaurusBlockDropChance =
            BUILDER.defineInRange("luxtructosaurus_block_drop_chance", 0.75, 0, 1);
    public final ModConfigSpec.BooleanValue nukesSpawnItemDrops = BUILDER.define("nuke_spawn_item_drops", true);
    public final ModConfigSpec.BooleanValue nuclearBombFlash = BUILDER.define("nuclear_bomb_flash", true);
    public final ModConfigSpec SPEC = BUILDER.build();
}
