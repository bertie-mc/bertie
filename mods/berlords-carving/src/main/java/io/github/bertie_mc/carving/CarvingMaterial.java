package io.github.bertie_mc.carving;

/**
 * A material a slate can be made of. Ordinal is the network index, so existing entries keep their
 * position (WOOD..DIAMOND unchanged); new materials are appended.
 *
 * <p>TIER 1 materials are carved in-hand ({@link io.github.bertie_mc.carving.client.CarvingScreen}); TIER 2
 * materials are worked only at the carving station's water-jet ({@link
 * io.github.bertie_mc.carving.block.CarvingStationBlock}). Every material has armor; only {@link #LEATHER}
 * lacks tools - it still carves a small slate, which is what {@code hasSmallSlate} is for.
 * {@code slagId == null} means the material has no Slag equivalent (leather is always
 * vanilla). {@code vanillaTool}/{@code vanillaArmor} name the vanilla item prefix used when Slag is
 * absent (null = that form is Slag-only and is hidden without Slag).
 *
 * <p>{@code slateOnly} materials have neither a Slag part nor a vanilla equivalent, so the two
 * slates are the product and carving them yields nothing - unless the pack names an armor output
 * for the material in {@link ArmorOverrides}, which is how prismarine carves into armor. They
 * register only when {@code requiredMod} - the mod supplying the item they are made from - is
 * present.
 */
public enum CarvingMaterial {
    WOOD("wood", "wooden", 1, true, "wooden", null),
    STONE("stone", "stone", 1, true, "stone", null),
    FLINT("flint", "flint", 1, true, null, null),
    BONE("bone", "bone", 1, true, null, null),
    DIAMOND("diamond", "diamond", 2, true, "diamond", "diamond"),
    LEATHER("leather", null, 1, false, null, "leather"),
    COPPER("copper", "copper", 2, true, null, null),
    IRON("iron", "iron", 2, true, "iron", "iron"),
    GOLDEN("golden", "golden", 2, true, "golden", "golden"),
    EMERALD("emerald", "emerald", 2, true, null, null),
    AMETHYST("amethyst", "amethyst", 2, true, null, null),
    LAPIS("lapis", "lapis", 2, true, null, null),
    QUARTZ("quartz", "quartz", 2, true, null, null),
    OBSIDIAN("obsidian", "obsidian", 2, true, null, null),
    ECHO("echo", "echo", 2, true, null, null),
    DEEP_ALLOY("deep_alloy", "deep_alloy", 2, true, null, null),
    ROSE_GOLD("rose_gold", "rose_gold", 2, true, null, null),
    NETHERITE("netherite", null),
    REDSTONE("redstone", null),
    PRISMARINE("prismarine", null, 1, false, null, null, true, null),
    HEART_OF_THE_SEA("heart_of_the_sea", null),
    TURTLE_SCUTE("turtle_scute", null),
    ARMADILLO_SCUTE("armadillo_scute", null),
    RESIN_BRICK("resin_brick", "vanillabackport"),
    ANCIENT_METAL("ancient_metal", "cataclysm"),
    BLACK_STEEL("black_steel", "cataclysm"),
    CURSIUM("cursium", "cataclysm"),
    IGNITIUM("ignitium", "cataclysm"),
    WITHERITE("witherite", "cataclysm");

    public final String id;
    /** Slag material_type id (without the slag: namespace), or null if there's no Slag equivalent. */
    public final String slagId;

    public final int tier;
    public final boolean hasTools;
    public final String vanillaTool;
    public final String vanillaArmor;

    /** The material is a pair of slates and nothing else: no tools, no armor, no Slag part. */
    public final boolean slateOnly;
    /** Modid supplying the item a slate-only material is carved from, or null when it is vanilla. */
    public final String requiredMod;

    CarvingMaterial(String id, String slagId, int tier, boolean hasTools, String vanillaTool, String vanillaArmor) {
        this(id, slagId, tier, hasTools, vanillaTool, vanillaArmor, false, null);
    }

    /** Slate-only at tier 2: without an armor override the slates are inert crafting stock. */
    CarvingMaterial(String id, String requiredMod) {
        this(id, null, 2, false, null, null, true, requiredMod);
    }

    CarvingMaterial(
            String id,
            String slagId,
            int tier,
            boolean hasTools,
            String vanillaTool,
            String vanillaArmor,
            boolean slateOnly,
            String requiredMod) {
        this.id = id;
        this.slagId = slagId;
        this.tier = tier;
        this.hasTools = hasTools;
        this.vanillaTool = vanillaTool;
        this.vanillaArmor = vanillaArmor;
        this.slateOnly = slateOnly;
        this.requiredMod = requiredMod;
    }

    /** Whether a small slate exists for this material. Leather has one without having tools. */
    public boolean hasSmallSlate() {
        return hasTools || slateOnly || this == LEATHER;
    }

    /** Worked only at the carving station (water-jet), never the in-hand screen. */
    public boolean isStationOnly() {
        return tier == 2;
    }

    public static CarvingMaterial byIndex(int i) {
        CarvingMaterial[] v = values();
        return (i >= 0 && i < v.length) ? v[i] : WOOD;
    }
}
