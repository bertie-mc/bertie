package io.github.bertie_mc.toolcraft;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Collapses the pack's two competing tool-crafting systems onto one.
 *
 * <p>Slag n' Embers keeps its metallurgy — melter, crucible, brick forge, smeltery, molds, fluids
 * and the Rose Gold / Deep Alloy tiers — but loses its parts and its 299 baked tools. Magitech
 * keeps the part system, the assembly workbench and four tools; its weapons, hammer and wand are
 * switched off, and the wand's removal takes the spell system with it.
 *
 * <p>Nearly all of that is data: recipe overrides in {@code data/slag} and {@code data/magitech},
 * tag edits, and a lang overlay that renames the six surviving parts. Only two behaviours need
 * code, and both are mixins because Magitech hardcodes them — reopening enchanting, and narrowing
 * the scythe's area harvest to plants.
 */
@Mod(BertieToolcraft.MOD_ID)
public class BertieToolcraft {
    public static final String MOD_ID = "bertietoolcraft";
    private static final Logger LOGGER = LogUtils.getLogger();

    public BertieToolcraft() {
        boolean magitech = ModList.get().isLoaded("magitech");
        boolean slag = ModList.get().isLoaded("slag");
        if (!magitech && !slag) {
            LOGGER.info("Neither Magitech nor Slag-n-Embers is present - Bertie Toolcraft is inert.");
            return;
        }
        LOGGER.info(
                "Bertie Toolcraft active (magitech={}, slag={}): one part system, four tools.",
                magitech,
                slag);
    }
}
