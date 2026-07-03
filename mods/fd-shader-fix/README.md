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

`./gradlew build` — the built JAR is written to `build/libs/`.

## License

Released under the MIT License — see [LICENSE](LICENSE).
