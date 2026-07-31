# Frozen Registry Fix

Runtime patch for *Immersive Armors*: force-initializes its `Items` class before the armor-material registry freezes, fixing the "Registry is already frozen" crash that shows up in large modpacks.

- **Minecraft:** 1.21.1
- **Loader:** NeoForge
- **Mod ID:** `frozenregfix`
- **Requires:** Immersive Armors

## Install
Download the latest JAR from the [Releases page](../../releases) and put it in your `mods/` folder. Requires NeoForge for Minecraft 1.21.1 plus Immersive Armors.

## Credits / Integration
Patches a load-order bug in [Immersive Armors](https://www.curseforge.com/minecraft/mc-mods/immersive-armors) rather than modifying it directly. Hooks both `GameData.freezeData()` HEAD and the low-level `MappedRegistry.freeze()` of the armor_material registry, so the force-init still fires even when other coremods (e.g. railways-untold, Create) reroute the freeze around `GameData.freezeData`.

## Building
`./gradlew build` — the built JAR is written to `build/libs/`.

## License
Released into the public domain under **The Unlicense** — see [UNLICENSE](UNLICENSE). Third-party assets and dependencies are carved out in [NOTICE](NOTICE).
