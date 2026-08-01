# Berlord's EMI Integration

Native EMI recipe-viewer plugins (28 modules) for machine mods that only ship JEI plugins.

- **Minecraft:** 1.21.1
- **Loader:** NeoForge
- **Mod ID:** `berlords_emi`
- **Requires:** EMI

## Install
Existing binaries remain on the [legacy release page](https://github.com/bertie-mc/berlords-emi/releases).
New releases use `berlords-emi/vX.Y.Z` tags in the
[Bertie monorepo](https://github.com/bertie-mc/bertie/releases). Put the JAR in your
`mods/` folder alongside NeoForge for Minecraft 1.21.1 and EMI.

## Credits / Integration
Adds native EMI support for mods such as Create, EnderIO, AnvilCraft, Malum, Slag 'n' Embers, and various *Delight food mods, which otherwise only ship a JEI plugin. Each integration is guarded to only load if its target mod is present.

## Building
`gradle :mods:berlords-emi:assemble` builds the JAR without running tests. Every integration dependency
resolves from public Maven repositories. A few libraries this mod compiles against
(anvillib, l2core, l2serial, and confluence_magic_lib) are published only inside their
parent mods, so the shared `extractNestedJars` task extracts them into the ignored build
directory. Nothing third-party is committed here; the first build requires network access.

`gradle :mods:berlords-emi:test` covers the shared machine-recipe descriptor. CI also launches EMI with Forbidden &
Arcanus and joins a world, failing unless the production plugin completes integration registration.

## License

Released into the public domain under **The Unlicense** — see [UNLICENSE](UNLICENSE). Third-party assets and dependencies are carved out in [NOTICE](NOTICE).
