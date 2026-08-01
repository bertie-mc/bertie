# Bush Tweaks

A small mixin mod that makes *Berries & Cherries* bushes behave like vanilla sweet-berry bushes: crouch-safe, correct slowdown, and proper damage gating.

- **Minecraft:** 1.21.1
- **Loader:** NeoForge
- **Mod ID:** `bushtweaks`
- **Requires:** Berries & Cherries (has no effect without it)

## Install
Existing binaries remain on the [legacy release page](https://github.com/bertie-mc/bush-tweaks/releases).
New releases use `bush-tweaks/vX.Y.Z` tags in the
[Bertie monorepo](https://github.com/bertie-mc/bertie/releases). Put the JAR in your
`mods/` folder alongside NeoForge for Minecraft 1.21.1 and Berries & Cherries.

## Credits / Integration
This mod contains only original mixin code. It references the target mod's classes by name to patch its bush behavior — no code from *Berries & Cherries* is included.

## Building
`gradle :mods:bush-tweaks:assemble` builds the JAR without running the independent test suite.

## Tests

`gradle :mods:bush-tweaks:test` starts NeoForge's in-process test environment with the pack's Berries &
Cherries version. It verifies that all 18 target blocks have vanilla movement speed and
that the patched damage procedures distinguish moving, stationary, and crouching entities.

## License

Released into the public domain under **The Unlicense** — see [UNLICENSE](UNLICENSE). Third-party assets and dependencies are carved out in [NOTICE](NOTICE).
