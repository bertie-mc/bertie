package com.berlord.filters;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bertie Filters - FTB Filter System custom-filter handlers for the Bertie modpack.
 *
 * <p>Registers a single FFS "custom" filter, eventId {@code bertie:wooden}, that matches WOODEN
 * Slag-n-Embers modular tools / armor. Slag buries the per-part material inside nested part NBT
 * ({@code slag:dynamic_parts}), which FFS's normal component filter cannot reach - so an FTB Quests
 * task can use the custom filter string {@code bertie:wooden} with the slag modular type as extra
 * data (e.g. {@code pickaxe}, {@code helmet}, {@code chestplate}, {@code leggings}, {@code boots}).
 *
 * <p>The actual matching logic references Slag classes, so it lives in {@link WoodenSlagFilter} and is
 * only wired up when {@code slag} is loaded - keeping this mod inert (no crash) if Slag is absent.
 */
@Mod(BertieFilters.MODID)
public class BertieFilters {
    public static final String MODID = "bertie_filters";
    public static final Logger LOGGER = LoggerFactory.getLogger("bertie_filters");

    public BertieFilters(IEventBus modEventBus) {
        // The FFS event bus is a static architectury Event - registered eagerly at construct time,
        // not via the mod event bus. Only register the slag-touching handler if Slag is present.
        if (ModList.get().isLoaded("slag")) {
            WoodenSlagFilter.register();
            LOGGER.info("Registered FTB Filter System custom filter 'bertie:wooden' (Slag-n-Embers present).");
        } else {
            LOGGER.info("Slag-n-Embers not loaded - 'bertie:wooden' custom filter not registered.");
        }
    }
}
