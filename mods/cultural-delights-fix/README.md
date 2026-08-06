# Cultural Delights Recipe Fix

A resource-only NeoForge compatibility mod for Cultural Delights 0.17.8 on Minecraft 1.21.1.

Cultural Delights 0.17.8 suppresses four vanilla soup and stew recipes by shipping zero-byte JSON
files. Minecraft rejects those files and aborts datapack loading. This mod overrides the same
resource locations with valid recipes guarded by `neoforge:false`, preserving the intended recipe
removal without breaking server startup.

- **Loader:** `lowcodefml`
- **Mod ID:** `culturaldelightsfix`
- **Requires:** Cultural Delights 0.17.8

The JAR contains only mod metadata and recipe data; it has no Java entrypoint.

## Building

`gradle :mods:cultural-delights-fix:assemble` builds the JAR.

## License

Released into the public domain under **The Unlicense** — see [UNLICENSE](UNLICENSE). Cultural
Delights retains its own licence and is not redistributed here.
