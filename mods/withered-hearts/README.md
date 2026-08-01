# Withered Hearts

Client-side NeoForge mod that trims the vanilla "wither" dark heart bar so it only shows the hearts the Wither effect will actually drain before it expires.

- **Minecraft:** 1.21.1
- **Loader:** NeoForge
- **Mod ID:** `witheredhearts`

## Install

Existing binaries remain on the [legacy release page](https://github.com/bertie-mc/withered-hearts/releases).
New releases use `withered-hearts/vX.Y.Z` tags in the
[Bertie monorepo](https://github.com/bertie-mc/bertie/releases). Put the JAR in your
`mods/` folder alongside NeoForge for Minecraft 1.21.1.

## Building

`gradle :mods:withered-hearts:assemble` builds the JAR without running the independent test suites.

## Tests

`gradle :mods:withered-hearts:test` covers Wither timing and per-heart consumption without Minecraft.
`gradle :mods:withered-hearts:clientTestJar` builds a test-only mod used by the headless client suite to
verify both HUD wrappers are woven into `Gui`; test code is excluded from releases.

## License

Released into the public domain under **The Unlicense** — see [UNLICENSE](UNLICENSE). Third-party assets and dependencies are carved out in [NOTICE](NOTICE).
