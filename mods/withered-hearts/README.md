# Withered Hearts

Client-side NeoForge mod that trims the vanilla "wither" dark heart bar so it only shows the hearts the Wither effect will actually drain before it expires.

- **Minecraft:** 1.21.1
- **Loader:** NeoForge
- **Mod ID:** `witheredhearts`

## Install

Releases use `withered-hearts/vX.Y.Z` tags on the
[Bertie release page](https://github.com/bertie-mc/bertie/releases). Put the JAR in your
`mods/` folder alongside NeoForge for Minecraft 1.21.1.

## Building

`gradle :mods:withered-hearts:assemble` builds the JAR without running the independent test suites.

## Tests

`gradle :mods:withered-hearts:test` covers Wither timing and per-heart consumption without Minecraft.

## License

Released into the public domain under **The Unlicense** — see [UNLICENSE](UNLICENSE). Third-party assets and dependencies are carved out in [NOTICE](NOTICE).
