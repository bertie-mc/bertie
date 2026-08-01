# FdLib Post-Shader Fix

Wraps *fdlib*'s post-shader initialization in a try/catch so an intermittent shader-load race is logged and skipped instead of crashing the client.

- **Minecraft:** 1.21.1
- **Loader:** NeoForge
- **Mod ID:** `fdshaderfix`
- **Requires:** fdlib

## Install

Download the latest JAR from the [Releases page](../../releases) and put it in your `mods/` folder. Requires NeoForge for Minecraft 1.21.1 plus fdlib.

## Credits / Integration

This is a runtime patch for *fdlib*. It wraps fdlib's post-shader init in a try/catch to stop an intermittent boot crash, helping mods built on fdlib — such as Qliphoth Awakening, Cinematic Cataclysm, and AnvilCraft — boot reliably.

## Building

`gradle build` — the built JAR is written to `build/libs/`.

## Tests

`gradle test` verifies that successful shader initialization is preserved and failures
are reported without escaping. `gradle clientTestJar` builds a test-only mod that checks
the wrapper is applied to fdlib in a headless client; it is excluded from releases.

## License

Released into the public domain under **The Unlicense** — see [UNLICENSE](UNLICENSE). Third-party assets and dependencies are carved out in [NOTICE](NOTICE).
