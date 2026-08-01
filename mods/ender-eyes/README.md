# Ender Eyes

Adds the **Ender Eyes** helmet enchantment: while worn, looking directly at an Enderman will not anger it.

- **Minecraft:** 1.21.1
- **Loader:** NeoForge
- **Mod ID:** `endereyes`

## Install
Existing binaries remain on the [legacy release page](https://github.com/bertie-mc/ender-eyes/releases).
New releases use `ender-eyes/vX.Y.Z` tags in the
[Bertie monorepo](https://github.com/bertie-mc/bertie/releases). Put the JAR in your
`mods/` folder alongside NeoForge for Minecraft 1.21.1.

## Credits / Integration

Clean-room NeoForge reimplementation of the Fabric mod *Ender Eyes*' behaviour; no upstream code was reused.

## Building

`gradle :mods:ender-eyes:assemble` builds the JAR without running the independent test suite.

## Testing

`gradle :mods:ender-eyes:test` boots NeoForge's test environment and checks the enchantment against the
real data-driven enchantment registry. The suite covers enchanted, unenchanted, and
empty helmet slots without launching a graphical client.

## License

Released into the public domain under **The Unlicense** — see [UNLICENSE](UNLICENSE). Third-party assets and dependencies are carved out in [NOTICE](NOTICE).
