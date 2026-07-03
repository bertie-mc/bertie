# Berlord's EMI Integration

Native EMI recipe-viewer plugins (28 modules) for machine mods that only ship JEI plugins.

- **Minecraft:** 1.21.1
- **Loader:** NeoForge
- **Mod ID:** `berlords_emi`
- **Dependency:** Requires EMI. Adds native EMI support for mods such as Create, EnderIO, AnvilCraft, Malum, Slag 'n' Embers, and various *Delight food mods.

## Building

`./gradlew build`. Every integration dependency resolves from Modrinth's maven. A few libraries emi compiles against (anvillib, l2core, l2serial, confluence_magic_lib) aren't published standalone — they ship JarJar-embedded inside their parent mods, so the `extractJarJarLibs` Gradle task pulls those parent mods from Modrinth and extracts the nested jars at build time. Nothing third-party is committed to this repo. Requires internet access on the first build.

## License

Released under the MIT License — see [LICENSE](LICENSE).
