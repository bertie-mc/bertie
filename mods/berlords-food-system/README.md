# Berlord's Food System

A Valheim-style food system: 1-5 stomach slots, per-food buffs and abilities, a Stomach Extension potion, and native replace-oldest behaviour.

- **Minecraft:** 1.21.1
- **Loader:** NeoForge
- **Mod ID:** `berlordsfoodsystem`

## Install

Releases use `berlords-food-system/vX.Y.Z` tags on the
[Bertie release page](https://github.com/bertie-mc/bertie/releases). Put the JAR in your
`mods/` folder alongside NeoForge for Minecraft 1.21.1.

## Credits / Integration

Design inspired by *Spice of Life: Valheim Reforged* by robinfrt (used with permission). All code here is original.

Optional integration with Sophisticated Backpacks (slot-aware feeding upgrades).

## Building

`gradle :mods:berlords-food-system:assemble` builds the JAR without running tests. `sophisticated-core`, used by the
optional Sophisticated Backpacks integration, resolves from Modrinth's Maven repository.

`gradle :mods:berlords-food-system:test` covers stomach state and buff configuration. CI also loads the real Sophisticated
Backpacks integration in a headless client, verifies every mixin target, and joins a world.

## License

Released into the public domain under **The Unlicense** — see [UNLICENSE](UNLICENSE). Third-party assets and dependencies are carved out in [NOTICE](NOTICE).
