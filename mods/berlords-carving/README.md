# Berlord's Carving

Carve early-game tool heads and armor from material slates: place the head inside a block of material and drag to carve it away. Includes optional Slag 'n' Embers and EMI integration.

- **Minecraft:** 1.21.1
- **Loader:** NeoForge
- **Mod ID:** `berlordscarving`

## Install
Releases use `carving/vX.Y.Z` tags on the
[Bertie release page](https://github.com/bertie-mc/bertie/releases). Put the JAR in your
`mods/` folder alongside NeoForge for Minecraft 1.21.1. Slag 'n' Embers and EMI are
optional—install them for extra integration, or run standalone.

## Integration & credits

Works standalone with its own textures. When **Slag 'n' Embers** (by LopyLuna) is installed, Carving integrates with it and uses Slag's parts and art **at runtime**, loaded from your installed copy of Slag — this repository does not contain or redistribute any Slag assets. Slag 'n' Embers is All Rights Reserved.

## Building
From the monorepo root, enter the pinned environment and run the module tasks:

```bash
nix develop
gradle :mods:berlords-carving:assemble
gradle :mods:berlords-carving:test
gradle :mods:berlords-carving:runGameTests
gradle :mods:berlords-carving:runClientTests
```

The JAR is written to `mods/berlords-carving/build/libs/`. Optional dependencies resolve from
Modrinth, so no local JARs are required.

The JVM suite checks the material and network-index contracts, armor overrides, packaged
JSON, models, textures, shapes, and Slag recipe replacements. GameTests exercise the
waterlogged carving station and its inventory. CI additionally launches a client with EMI
and Slag, verifies all 148 integration recipes register, and joins a world.

## License

Released into the public domain under **The Unlicense** — see [UNLICENSE](UNLICENSE). Third-party assets and dependencies are carved out in [NOTICE](NOTICE).
