# Withered Hearts

Client-side NeoForge mod that trims the vanilla "wither" dark heart bar so it only shows the hearts the Wither effect will actually drain before it expires.

- **Minecraft:** 1.21.1
- **Loader:** NeoForge
- **Mod ID:** `witheredhearts`

## Install

Download the latest JAR from the [Releases page](../../releases) and put it in your `mods/` folder. Requires NeoForge for Minecraft 1.21.1.

## Building

`gradle build` — the built JAR is written to `build/libs/`.

## Tests

`gradle test` covers Wither timing and per-heart consumption without Minecraft.
`gradle clientTestJar` builds a test-only mod used by the headless client suite to
verify both HUD wrappers are woven into `Gui`; test code is excluded from releases.

## License

Released into the public domain under **The Unlicense** — see [UNLICENSE](UNLICENSE). Third-party assets and dependencies are carved out in [NOTICE](NOTICE).
