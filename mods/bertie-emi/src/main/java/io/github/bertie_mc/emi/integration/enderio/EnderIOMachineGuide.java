package io.github.bertie_mc.emi.integration.enderio;

import dev.emi.emi.api.EmiRegistry;
import io.github.bertie_mc.emi.framework.InfoPages;
import java.util.List;

/** Operating instructions for machines whose work is not an item-to-item recipe. */
final class EnderIOMachineGuide {
    private EnderIOMachineGuide() {}

    static void register(EmiRegistry registry) {
        page(
                registry,
                "wired_charger",
                "Supply energy and place a chargeable item in the input slot. The charged item moves to the output.");
        page(
                registry,
                "wireless_charger",
                "Charges compatible items in nearby players' inventories using stored energy.",
                "Install a capacitor and configure the working area. An antenna above the charger extends its range.");
        page(
                registry,
                "stirling_generator",
                "Burns furnace fuels to generate energy. Its fuel tab lists the accepted fuels.",
                "A capacitor changes generation speed and fuel efficiency.");
        page(
                registry,
                "soul_engine",
                "First bind a compatible soul into the engine using a Soul Binder.",
                "Supply the fluid that soul accepts. The Soul Engine tab shows its fuel and energy yield.");
        page(
                registry,
                "crafter",
                "Set the crafting pattern, supply matching ingredients, then provide energy and a capacitor.",
                "It repeatedly performs the selected crafting-table recipe. Extract the results from its output.");
        page(
                registry,
                "impulse_hopper",
                "Set the item types and quantities in the ghost slots. Supply matching items to the input slots.",
                "It moves a complete batch to its output only when all configured ingredients are ready. Requires energy and a capacitor.");
        page(
                registry,
                "drain",
                "Place it above a fluid source and supply energy. It collects connected source blocks into its tank.",
                "Configure its working area and extract the collected fluid through a fluid output.");
        page(
                registry,
                "niard",
                "Supply fluid and energy to place source blocks into the world within its configured working area.");
        page(
                registry,
                "vacuum_chest",
                "Collects dropped items within its configured area. Use an item filter to control what it collects.",
                "Extract collected items from its inventory.");
        page(
                registry,
                "xp_vacuum",
                "Collects nearby experience orbs and stores them as liquid experience.",
                "Configure its collection area and pipe the liquid to an XP Obelisk or another experience tank.");
        page(
                registry,
                "xp_obelisk",
                "Stores experience as a liquid. Its buttons deposit levels from the player or return stored levels.",
                "It can exchange liquid experience with other machines through fluid connections.");
        page(
                registry,
                "farming_station",
                "Supply energy, a capacitor, seeds and the tools required by the crops or trees being farmed.",
                "It plants and harvests in its working area. Keep tools supplied and remove harvested items from the output.");
        page(
                registry,
                "powered_spawner",
                "Bind a supported mob soul into the spawner using a Soul Binder, then supply energy and a capacitor.",
                "Spawn mode creates that mob when the area has room. Capture mode fills Soul Vials from nearby matching mobs.");
        page(
                registry,
                "mind_killer",
                "Place this directly above a Powered Spawner to disable the AI of the mobs it spawns.");
        page(
                registry,
                "attractor_obelisk",
                "Uses energy to draw nearby mobs selected by a soul filter toward the obelisk.",
                "Install a capacitor, set the filter, and configure the working area.");
        page(
                registry,
                "aversion_obelisk",
                "Prevents mobs selected by its soul filter from spawning within the working area.",
                "Requires energy and a capacitor; existing mobs are not removed.");
        page(
                registry,
                "relocator_obelisk",
                "Moves newly spawning mobs selected by its soul filter to the area around the obelisk.",
                "Supply energy, install a capacitor, and configure the area in which spawns are intercepted.");
        page(
                registry,
                "inhibitor_obelisk",
                "Blocks entity teleportation into or out of its working area while powered.",
                "Requires a capacitor. It does not need a soul filter.");
        page(
                registry,
                "weather_obelisk",
                "Consumes weather-change fluids to select clear skies, rain or thunderstorms.",
                "The available fluids depend on the world's recipes; any loaded recipes appear in its Weather Obelisk tab.");
        page(
                registry,
                "block_detector",
                "Monitors the block in front of it and emits a redstone signal when its configured condition matches.",
                "Open its controls to choose the detection condition.");
        InfoPages.page(
                registry,
                "enderio/guide/travel_anchor",
                List.of("enderio:travel_anchor", "enderio:painted_travel_anchor"),
                "Travel Anchors are destinations for Ender IO's teleportation system.",
                "Stand on an anchor to travel to another visible anchor, or target an anchor with a Staff of Travelling.");
        InfoPages.page(
                registry,
                "enderio/guide/capacitor_bank",
                List.of(
                        "enderio:basic_capacitor_bank",
                        "enderio:advanced_capacitor_bank",
                        "enderio:vibrant_capacitor_bank"),
                "Stores energy for connected machines. Configure the faces that accept and supply energy.",
                "Adjacent banks of the same tier can combine into a larger energy store.");
        InfoPages.page(
                registry,
                "enderio/guide/solar_panel",
                List.of(
                        "enderio:energetic_photovoltaic_module",
                        "enderio:pulsating_photovoltaic_module",
                        "enderio:vibrant_photovoltaic_module"),
                "Generates energy from sunlight. Place it where the sky is unobstructed and connect an energy output.",
                "Some souls can be bound into the panel using a Soul Binder to modify its generation.");
    }

    private static void page(EmiRegistry registry, String machine, String... text) {
        InfoPages.page(registry, "enderio/guide/" + machine, List.of("enderio:" + machine), text);
    }
}
