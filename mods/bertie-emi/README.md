# Bertie EMI

Native EMI recipe-viewer plugins for third-party mods that only ship JEI plugins.
This is not a shared integration module for Bertie-owned mods: each owned mod ships any native EMI
integration from its own module.

- **Minecraft:** 1.21.1
- **Loader:** NeoForge
- **Mod ID:** `bertieemi`
- **Requires:** EMI

## Install
Releases use `bertie-emi/vX.Y.Z` tags on the
[Bertie release page](https://github.com/bertie-mc/bertie/releases). Put the JAR in your
`mods/` folder alongside NeoForge for Minecraft 1.21.1 and EMI.

## Credits / Integration
Adds native EMI support for mods such as Create, EnderIO, AnvilCraft, Magitech, Malum, Slag 'n' Embers, and various *Delight food mods, which otherwise only ship a JEI plugin. Each integration is guarded to only load if its target mod is present.

A few integrations instead reshape a plugin the target mod already ships: Pastel's single pedestal-crafting
category is replaced by one per pedestal tier, reusing Pastel's own recipe rendering.

## Building
`gradle :mods:bertie-emi:assemble` builds the JAR without running tests. Every integration dependency
resolves from public Maven repositories. The `jarJarCompileOnly` dependency convention makes
libraries bundled inside parent mods available to the compiler. Nothing third-party is committed
here; the first build requires network access.

`gradle :mods:bertie-emi:test` covers the shared machine-recipe descriptor. CI also launches EMI with Forbidden &
Arcanus and joins a world, failing unless the production plugin completes integration registration.

## License

Released into the public domain under **The Unlicense** — see [UNLICENSE](UNLICENSE). Third-party assets and dependencies are carved out in [NOTICE](NOTICE).
