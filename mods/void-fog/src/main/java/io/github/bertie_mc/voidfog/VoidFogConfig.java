package io.github.bertie_mc.voidfog;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client config for the fog, written to {@code config/voidfog-client.toml}.
 *
 * <p>Client rather than common: the fog is drawn from the camera and nothing about it reaches the
 * server, so a player may turn it off without desyncing from anyone.
 */
public final class VoidFogConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> DIMENSIONS;
    public static final ModConfigSpec.IntValue FADE_DEPTH;
    public static final ModConfigSpec.IntValue FULL_DEPTH;

    public static final ModConfigSpec.IntValue SKY_RADIUS;
    public static final ModConfigSpec.IntValue SKY_INTERVAL;

    public static final ModConfigSpec.DoubleValue FOG_START;
    public static final ModConfigSpec.DoubleValue FALLOFF;
    public static final ModConfigSpec.DoubleValue FOG_END;
    public static final ModConfigSpec.DoubleValue DARKNESS;

    public static final ModConfigSpec.BooleanValue PARTICLES;
    public static final ModConfigSpec.IntValue PARTICLE_COUNT;
    public static final ModConfigSpec.IntValue PARTICLE_RADIUS;

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
                        "y=-64, so 10 begins the fade at y=-54 - a little above the highest bedrock.")
                .defineInRange("fadeDepth", 10, 0, 512);

        FULL_DEPTH = builder.comment(
                        "Blocks above the floor where the fog is at full strength. Measured to the",
                        "player's EYE, so this has to be a height someone can actually stand at: the",
                        "lowest floor is the y=-64 bedrock layer, which puts an eye at about 2.6, so 3.",
                        "Setting this to 0 means full strength is only reached inside solid rock.")
                .defineInRange("fullDepth", 5, 0, 512);

        builder.pop();

        builder.comment(
                        "The fog gives way near a column that is open to the sky, and comes back as you",
                        "walk away from it. The easing is quadratic, so stepping sideways under a shaft",
                        "does not flip it.")
                .push("sky");

        SKY_RADIUS = builder.comment(
                        "How far an opening to the sky holds the fog off, in blocks. 0 disables the check",
                        "and the fog ignores daylight entirely.")
                .defineInRange("radius", 24, 0, 96);

        SKY_INTERVAL = builder.comment(
                        "Ticks between sky scans. The result is eased between scans, so raising this",
                        "costs smoothness, not correctness.")
                .defineInRange("scanInterval", 10, 1, 200);

        builder.pop();

        builder.comment("How the fog looks once it is at full strength.").push("appearance");

        FOG_START = builder.comment("Distance from the camera where the fog begins, in blocks.")
                .defineInRange("fogStart", 0.0, 0.0, 512.0);

        FALLOFF = builder.comment(
                        "How sharply the fog arrives as you descend through the band. Everything the",
                        "effect does - how near the view closes in and how black it goes - is driven",
                        "by this one curve, so the two always arrive together. Higher holds the fog",
                        "off until you are deeper, then brings it in faster.")
                .defineInRange("falloff", 3.5, 1.0, 8.0);

        FOG_END = builder.comment("Distance where the fog is solid, in blocks. Lower is thicker.")
                .defineInRange("fogEnd", 10.0, 1.0, 512.0);

        DARKNESS = builder.comment(
                        "How far the fog colour is pulled towards black. 1.0 is pitch black, which is",
                        "what makes a torch further off than fogEnd invisible rather than dim.")
                .defineInRange("darkness", 1.0, 0.0, 1.0);

        builder.pop();

        builder.comment("The motes drifting in the fog.").push("particles");

        PARTICLES = builder.comment("Whether the fog carries particles.").define("enabled", true);

        PARTICLE_COUNT = builder.comment("Motes spawned per tick at full strength. Scales down with the fog.")
                .defineInRange("perTick", 6, 0, 64);

        PARTICLE_RADIUS = builder.comment("How far from the camera motes may spawn, in blocks.")
                .defineInRange("radius", 12, 1, 48);

        builder.pop();

        SPEC = builder.build();
    }

    private VoidFogConfig() {}
}
