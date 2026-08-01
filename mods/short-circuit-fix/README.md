# Short Circuit Fix

Registers *Short Circuit*'s circuit blocks on the translucent render layer - a step the NeoForge port omits (present in its Fabric build).

- **Minecraft:** 1.21.1
- **Loader:** NeoForge
- **Mod ID:** `shortcircuitfix`
- **Requires:** **Short Circuit** (NeoForge)

## Install

Download the latest JAR from the [Releases page](../../releases) and put it in your `mods/` folder. Requires NeoForge for Minecraft 1.21.1 plus the Short Circuit mod above.

## Building

`gradle build` — the built JAR is written to `build/libs/`. This is a runtime patch, so it must be run alongside Short Circuit to have any effect.

## Testing

The headless client suite loads the built release JAR with the real Short Circuit mod,
then verifies that both affected blocks use the translucent render layer. Its test-only
mod is produced by `gradle clientTestJar` in `build/test-libs/`; it is never included in
the release artifact. CI composes the same `bertie-ci` commands and Gradle task that can
be run locally on a Nix-enabled headless machine.

## License

Released into the public domain under **The Unlicense** — see [UNLICENSE](UNLICENSE). Third-party assets and dependencies are carved out in [NOTICE](NOTICE).
