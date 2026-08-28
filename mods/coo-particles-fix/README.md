# CooParticles API Dedicated Server Fix

Runtime compatibility patch for *CooParticles API* 2.5.5.3.
It prevents CooParticles' common listener scanner from reflecting over its client-only
`TestBlockPlayerPathListener` on a dedicated server. Every gameplay listener remains
registered normally, including listeners added by consumers such as UsefulMagic.

- **Minecraft:** 1.21.1
- **Loader:** NeoForge
- **Mod ID:** `cooparticlesfix`
- **Requires:** CooParticles API 2.5.5.3

## Install

Releases use `coo-particles-fix/vX.Y.Z` tags on the
[Bertie release page](https://github.com/bertie-mc/bertie/releases). Put the JAR in your
`mods/` folder alongside NeoForge and CooParticles API.

## Building

`gradle :mods:coo-particles-fix:assemble` builds the JAR.

## Tests

`gradle :mods:coo-particles-fix:test` loads the exact CooParticles dependency graph and
verifies that its event bus received the compatibility hook. The pack's
dedicated GameTest and isolated client suites cover both physical distributions.

## License

Released into the public domain under **The Unlicense** — see [UNLICENSE](UNLICENSE).
Third-party dependencies are carved out in [NOTICE](NOTICE).
