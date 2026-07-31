# Frozen Registry Fix

Runtime patch for *Immersive Armors*: initializes its lazy armor materials before a
writable registry freeze when possible, with a narrowly scoped late-registration fallback
for `immersive_armors` armor-material keys on NeoForge 21.1.233.

- **Minecraft:** 1.21.1
- **Loader:** NeoForge
- **Mod ID:** `frozenregfix`
- **Requires:** Immersive Armors

## Install
Download the latest JAR from the [Releases page](../../releases) and put it in your `mods/` folder. Requires NeoForge for Minecraft 1.21.1 plus Immersive Armors.

## Credits / Integration
Patches a load-order bug in [Immersive Armors](https://www.curseforge.com/minecraft/mc-mods/immersive-armors) rather than modifying it directly. The fallback bypasses only the frozen-state guard for `immersive_armors` keys in the armor-material registry; ordinary duplicate and value validation remains active.

## Building
`./gradlew build` — the built JAR is written to `build/libs/`.

## License
Released into the public domain under **The Unlicense** — see [UNLICENSE](UNLICENSE). Third-party assets and dependencies are carved out in [NOTICE](NOTICE).
