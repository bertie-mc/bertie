# Short Circuit Fix

Registers *Short Circuit*'s circuit blocks on the translucent render layer - a step the NeoForge port omits (present in its Fabric build).

- **Minecraft:** 1.21.1
- **Loader:** NeoForge
- **Mod ID:** `shortcircuitfix`
- **Requires:** **Short Circuit** (NeoForge)

## Install

Releases use `short-circuit-fix/vX.Y.Z` tags on the
[Bertie release page](https://github.com/bertie-mc/bertie/releases). Put the JAR in your
`mods/` folder alongside NeoForge for Minecraft 1.21.1 and Short Circuit.

## Building

`gradle :mods:short-circuit-fix:assemble` builds the JAR without running tests. This runtime patch must be run
alongside Short Circuit to have any effect.

## Testing

`gradle :mods:short-circuit-fix:runClientTests` loads the mod with the real Short Circuit
runtime in a graphical Minecraft client, then verifies that both affected blocks use the
translucent render layer. Client-test code is never included in the release artifact.

## License

Released into the public domain under **The Unlicense** — see [UNLICENSE](UNLICENSE). Third-party assets and dependencies are carved out in [NOTICE](NOTICE).
