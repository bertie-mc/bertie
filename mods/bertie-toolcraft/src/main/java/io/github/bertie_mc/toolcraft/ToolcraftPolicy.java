package io.github.bertie_mc.toolcraft;

/**
 * Dependency-free rules for the reduced Magitech tool set. Kept free of Minecraft and Magitech
 * types so it can be unit-tested headlessly; the mixins translate these answers into game
 * behaviour.
 *
 * <p>Magitech ships nine tools. Bertie keeps four — pickaxe, axe, shovel and scythe — each built
 * from head + handle + binding. The three weapons and the hammer are switched off in data; the
 * wand is switched off along with the spell system it gates.
 */
public final class ToolcraftPolicy {
    private ToolcraftPolicy() {}

    /** Magitech's {@code ToolType.getId()} values for the tools Bertie keeps. */
    public static final String PICKAXE = "pickaxe";
    public static final String AXE = "axe";
    public static final String SHOVEL = "shovel";
    public static final String SCYTHE = "scythe";

    /**
     * Enchantment value for every kept tool. Magitech has no per-material enchantability stat, and
     * berlord's ruling is that the vanilla "better enchantability rolls a better table offer"
     * mechanic is dead weight in this pack — so every tool gets one flat value rather than a
     * material-dependent one. 10 is vanilla diamond.
     */
    public static final int ENCHANTMENT_VALUE = 10;

    /** True when this Magitech tool id is one of the four Bertie keeps. */
    public static boolean isKeptTool(String toolTypeId) {
        return PICKAXE.equals(toolTypeId)
                || AXE.equals(toolTypeId)
                || SHOVEL.equals(toolTypeId)
                || SCYTHE.equals(toolTypeId);
    }

    /**
     * The vanilla tool whose enchantment set a kept tool should mirror. Returning the vanilla item
     * rather than listing enchantments by name means the answer stays right when another mod adds
     * an enchantment to {@code #minecraft:enchantable/*}.
     *
     * <p>The scythe mirrors a hoe, not a sword: it is a harvesting tool in Bertie, and Magitech
     * already routes it through {@code DEFAULT_HOE_ACTIONS}.
     *
     * @return a vanilla registry path, or null if the tool is not kept (and so takes nothing)
     */
    public static String vanillaEnchantmentProxy(String toolTypeId) {
        return switch (toolTypeId) {
            case PICKAXE -> "diamond_pickaxe";
            case AXE -> "diamond_axe";
            case SHOVEL -> "diamond_shovel";
            case SCYTHE -> "diamond_hoe";
            default -> null;
        };
    }

    /**
     * Whether a block should be swept up by the scythe's area harvest.
     *
     * <p>Magitech's stock scythe clears up to 20 blocks of whatever it hits, which turns it into a
     * general-purpose area miner. Bertie narrows that to blocks that break instantly — crops,
     * grass, flowers, saplings — so the scythe is a harvesting tool. Anything with real hardness
     * (nether wart block, shroomlight, sculk) still breaks one block at a time, exactly like a hoe.
     *
     * @param destroySpeed the block's hardness at that position, from {@code getDestroySpeed}
     */
    public static boolean sweepsInAreaHarvest(float destroySpeed) {
        return destroySpeed == 0.0F;
    }
}
