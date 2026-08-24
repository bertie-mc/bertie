package io.github.bertie_mc.bedrockfog;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client config for the fog, written to {@code config/bedrockfog-client.toml}.
 *
 * <p>Client rather than common: the fog is drawn from the camera and nothing about it reaches the
 * server, so a player may turn it off without desyncing from anyone.
 */
public final class BedrockFogConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> DIMENSIONS;
    public static final ModConfigSpec.IntValue FADE_DEPTH;
    public static final ModConfigSpec.IntValue FULL_DEPTH;
    public static final ModConfigSpec.DoubleValue FOG_START;
    public static final ModConfigSpec.DoubleValue FOG_END;
    public static final ModConfigSpec.DoubleValue DARKNESS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Where the fog appears.").push("depth");

        ENABLED = builder.comment("Whether the fog is drawn at all.").define("enabled", true);

        DIMENSIONS = builder.comment(
                        "Dimensions that get the fog, by ID. Anything not listed is left alone.",
                        "Depth is measured from each dimension's own floor, so one setting suits",
                        "dimensions with different build heights.")
                .defineListAllowEmpty(
                        "dimensions",
                        List.of("minecraft:overworld"),
                        () -> "minecraft:overworld",
                        value -> value instanceof String id && ResourceLocation.tryParse(id) != null);

        FADE_DEPTH = builder.comment(
                        "Blocks above the floor where the fog starts to show. The overworld floor is",
                        "y=-64, so 32 begins the fade at y=-32.")
                .defineInRange("fadeDepth", 32, 0, 512);

        FULL_DEPTH = builder.comment("Blocks above the floor where the fog is at full strength - bedrock level.")
                .defineInRange("fullDepth", 6, 0, 512);

        builder.pop();

        builder.comment("How the fog looks once it is at full strength.").push("appearance");

        FOG_START = builder.comment("Distance from the camera where the fog begins, in blocks.")
                .defineInRange("fogStart", 0.0, 0.0, 512.0);

        FOG_END = builder.comment("Distance where the fog is solid, in blocks. Lower is thicker.")
                .defineInRange("fogEnd", 16.0, 1.0, 512.0);

        DARKNESS = builder.comment("How far the fog colour is pulled towards black. 1.0 is pitch black.")
                .defineInRange("darkness", 0.9, 0.0, 1.0);

        builder.pop();

        SPEC = builder.build();
    }

    private BedrockFogConfig() {}
}
