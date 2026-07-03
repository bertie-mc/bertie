# Berlord's EMI Integration

Native EMI recipe-viewer plugins (28 modules) for machine mods that only ship JEI plugins.

- **Minecraft:** 1.21.1
- **Loader:** NeoForge
- **Mod ID:** `berlords_emi`
- **Dependency:** Requires EMI. Adds native EMI support for mods such as Create, EnderIO, AnvilCraft, Malum, Slag 'n' Embers, and various *Delight food mods.

## Building

This mod is **built locally only** — it has no cloud CI. It integrates with ~30 other mods and compiles against their APIs, and a few of those (`anvillib 1.4.0+build.188`, `l2serial`, `confluence_magic_lib`) are **not published on any public maven**, so an ephemeral CI runner can't resolve them. To build, place the required mod jars in `libs/` (git-ignored, not redistributed here) and run `./gradlew build`.

## License

Released under the MIT License — see [LICENSE](LICENSE).
