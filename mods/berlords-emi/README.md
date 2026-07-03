# Berlord's EMI Integration

Native EMI recipe-viewer plugins (28 modules) for machine mods that only ship JEI plugins.

- **Minecraft:** 1.21.1
- **Loader:** NeoForge
- **Mod ID:** `berlords_emi`
- **Requires:** EMI

## Install
Download the latest JAR from the [Releases page](../../releases) and put it in your `mods/` folder. Requires NeoForge for Minecraft 1.21.1 plus EMI.

## Credits / Integration
Adds native EMI support for mods such as Create, EnderIO, AnvilCraft, Malum, Slag 'n' Embers, and various *Delight food mods, which otherwise only ship a JEI plugin. Each integration is guarded to only load if its target mod is present.

## Building
`./gradlew build` — the built JAR is written to `build/libs/`. Every integration dependency resolves from Modrinth's maven. A few libraries this mod compiles against (anvillib, l2core, l2serial, confluence_magic_lib) aren't published standalone — they ship JarJar-embedded inside their parent mods, so the `extractJarJarLibs` Gradle task pulls those parent mods from Modrinth and extracts the nested jars at build time. Nothing third-party is committed to this repo. Requires internet access on the first build.

## License

Released under the MIT License — see [LICENSE](LICENSE).
