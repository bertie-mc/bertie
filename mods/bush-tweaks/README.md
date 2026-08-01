# Bush Tweaks

A small mixin mod that makes *Berries & Cherries* bushes behave like vanilla sweet-berry bushes: crouch-safe, correct slowdown, and proper damage gating.

- **Minecraft:** 1.21.1
- **Loader:** NeoForge
- **Mod ID:** `bushtweaks`
- **Requires:** Berries & Cherries (has no effect without it)

## Install
Download the latest JAR from the [Releases page](../../releases) and put it in your `mods/` folder. Requires NeoForge for Minecraft 1.21.1 plus Berries & Cherries.

## Credits / Integration
This mod contains only original mixin code. It references the target mod's classes by name to patch its bush behavior — no code from *Berries & Cherries* is included.

## Building
`gradle build` — the built JAR is written to `build/libs/`.

## Tests

`gradle test` starts NeoForge's in-process test environment with the pack's Berries &
Cherries version. It verifies that all 18 target blocks have vanilla movement speed and
that the patched damage procedures distinguish moving, stationary, and crouching entities.

## License

Released into the public domain under **The Unlicense** — see [UNLICENSE](UNLICENSE). Third-party assets and dependencies are carved out in [NOTICE](NOTICE).
