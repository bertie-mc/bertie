# Explosive Enhancement (NeoForge)
A NeoForge 1.21.1 reimplementation of the **Explosive Enhancement** explosion-particle overhaul.

- **Minecraft:** 1.21.1
- **Loader:** NeoForge
- **Mod ID:** `explosiveenhancement`

## Install
Existing binaries remain on the
[legacy release page](https://github.com/bertie-mc/explosive-enhancement/releases). New
releases use `explosive-enhancement/vX.Y.Z` tags in the
[Bertie monorepo](https://github.com/bertie-mc/bertie/releases). Put the JAR in your
`mods/` folder alongside NeoForge for Minecraft 1.21.1.

## Credits / Integration

An independent NeoForge port of **[Explosive Enhancement](https://github.com/Superkat32/Explosive-Enhancement)** by **Superkat32** (originally a Fabric mod). The original is MIT-licensed.

## Building
`gradle :mods:explosive-enhancement:assemble` builds the JAR without running the independent test suites.

## Tests

`gradle :mods:explosive-enhancement:test` covers explosion sizing, placement, visibility, and particle-count policy.
`gradle :mods:explosive-enhancement:clientTestJar` builds a test-only mod that verifies the packet-handler mixin in a
headless client; it is excluded from releases.

## License

Released under the MIT License — see [LICENSE](LICENSE).
