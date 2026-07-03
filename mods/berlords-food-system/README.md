# Berlord's Food System

A Valheim-style food system: 1-5 stomach slots, per-food buffs and abilities, a Stomach Extension potion, and native replace-oldest behaviour.

- **Minecraft:** 1.21.1
- **Loader:** NeoForge
- **Mod ID:** `berlords_food_system`

## Install

Download the latest JAR from the [Releases page](../../releases) and put it in your `mods/` folder. Requires NeoForge for Minecraft 1.21.1.

## Credits / Integration

Design inspired by *Spice of Life: Valheim Reforged* by robinfrt (used with permission). All code here is original.

Optional integration with Sophisticated Backpacks (slot-aware feeding upgrades).

## Building

`./gradlew build` — the built JAR is written to `build/libs/`. `sophisticated-core` (needed for the optional Sophisticated Backpacks integration) resolves from Modrinth's maven at build time.

## License

Released under the MIT License — see [LICENSE](LICENSE).
