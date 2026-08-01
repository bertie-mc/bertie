# Short Circuit Fix

Registers *Short Circuit*'s circuit blocks on the translucent render layer - a step the NeoForge port omits (present in its Fabric build).

- **Minecraft:** 1.21.1
- **Loader:** NeoForge
- **Mod ID:** `shortcircuitfix`
- **Requires:** **Short Circuit** (NeoForge)

## Install

Existing binaries remain on the
[legacy release page](https://github.com/bertie-mc/short-circuit-fix/releases). New
releases use `short-circuit-fix/vX.Y.Z` tags in the
[Bertie monorepo](https://github.com/bertie-mc/bertie/releases). Put the JAR in your
`mods/` folder alongside NeoForge for Minecraft 1.21.1 and Short Circuit.

## Building

`gradle :mods:short-circuit-fix:assemble` builds the JAR without running tests. This runtime patch must be run
alongside Short Circuit to have any effect.

## Testing

The headless client suite loads the built release JAR with the real Short Circuit mod,
then verifies that both affected blocks use the translucent render layer. Its test-only
mod is produced by `gradle :mods:short-circuit-fix:clientTestJar` in `build/test-libs/`; it is never included in
the release artifact. CI composes the same `bertie-ci` commands and Gradle task that can
be run locally on a Nix-enabled headless machine.

## License

Released into the public domain under **The Unlicense** — see [UNLICENSE](UNLICENSE). Third-party assets and dependencies are carved out in [NOTICE](NOTICE).
