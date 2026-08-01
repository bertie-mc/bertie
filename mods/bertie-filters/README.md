# Bertie Filters

Adds an FTB Filter System custom filter (`bertie:wooden`) that matches wooden-material Slag 'n' Embers modular tools and armor by reading nested part NBT the default component filter cannot reach.

- **Minecraft:** 1.21.1
- **Loader:** NeoForge
- **Mod ID:** `bertiefilters`
- **Requires:** FTB Filter System, Slag 'n' Embers

## Install
Releases use `bertie-filters/vX.Y.Z` tags on the
[Bertie release page](https://github.com/bertie-mc/bertie/releases). Put the JAR in your
`mods/` folder alongside NeoForge for Minecraft 1.21.1 and the dependencies above.

## Building
`gradle :mods:bertie-filters:assemble` builds the JAR without running tests. Dependencies (FTB Filter System,
Architectury, Slag 'n' Embers) resolve from Modrinth, Architectury, and FTB Maven
repositories.

## Tests

`gradle :mods:bertie-filters:test` runs the fast filter-policy suite. It covers exact event routing, requested
modular types, mixed-material tools, and rejection of missing or non-wooden parts without
launching Minecraft.

## License

Released into the public domain under **The Unlicense** — see [UNLICENSE](UNLICENSE). Third-party assets and dependencies are carved out in [NOTICE](NOTICE).
