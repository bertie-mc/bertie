# Mekanism Covers Refresh Fix

Refreshes a Mekanism cable or pipe's cached model data when its cover is applied,
replaced, or removed. Covers appear without changing neighboring cables or reloading
the world. The patch also covers Mekanism Extras transmitters through their shared
Mekanism base class.

- Minecraft 1.21.1 / NeoForge
- Mekanism 10.7.19.85
- Mekanism Covers 1.3-BETA+1.21
- Mod ID: `mekanismcoversfix`

The client refreshes only when the synchronized `CoverState` changes. Initial block
entity loading seeds the remembered state so removing a cover after reconnecting also
refreshes the model. No polling, cable connection changes, or rendering settings are
required. The JAR loads safely on dedicated servers; the refresh runs only on clients.

## Build and verification

`gradle :mods:mekanism-covers-fix:build` builds the JAR and checks that the exact
Mekanism and Mekanism Covers dependencies receive the mixin.

`gradle :mods:mekanism-covers-fix:runClientTests` exercises the real cover item and
removal path on an isolated cable. It checks the cached render model after applying,
replacing, removing, and reloading a cover, without any neighboring block updates.

## License

Original patch code is dedicated to the public domain under [The Unlicense](UNLICENSE).
Dependencies retain their own licenses; see [NOTICE](NOTICE).
