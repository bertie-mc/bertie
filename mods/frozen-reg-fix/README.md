# Frozen Registry Fix

Runtime patch for *Immersive Armors*: initializes its lazy armor materials before a
writable registry freeze when possible, with a narrowly scoped late-registration fallback
for `immersive_armors` armor-material keys on NeoForge 21.1.233.

- **Minecraft:** 1.21.1
- **Loader:** NeoForge
- **Mod ID:** `frozenregfix`
- **Requires:** Immersive Armors

## Install
Releases use `frozen-reg-fix/vX.Y.Z` tags on the
[Bertie release page](https://github.com/bertie-mc/bertie/releases). Put the JAR in your
`mods/` folder alongside NeoForge for Minecraft 1.21.1 and Immersive Armors.

## Credits / Integration
Patches a load-order bug in [Immersive Armors](https://www.curseforge.com/minecraft/mc-mods/immersive-armors) rather than modifying it directly. The fallback bypasses only the frozen-state guard for `immersive_armors` keys in the armor-material registry; ordinary duplicate and value validation remains active.

## Building
`gradle :mods:frozen-reg-fix:assemble` builds the JAR without running client integration tests.

## Tests

`gradle :mods:frozen-reg-fix:clientTestJar` builds a test-only client mod under `build/test-libs/`. The
headless client suite loads the release JAR with Immersive Armors, reaches its post-load
lifecycle, and verifies all ten expected armor materials are registered. Test code and
resources are excluded from the release JAR.

## License
Released into the public domain under **The Unlicense** — see [UNLICENSE](UNLICENSE). Third-party assets and dependencies are carved out in [NOTICE](NOTICE).
