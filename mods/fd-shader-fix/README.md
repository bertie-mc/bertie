# FdLib Post-Shader Fix

Wraps *fdlib*'s post-shader initialization in a try/catch so an intermittent shader-load race is logged and skipped instead of crashing the client.

- **Minecraft:** 1.21.1
- **Loader:** NeoForge
- **Mod ID:** `fdshaderfix`
- **Requires:** fdlib

## Install

Releases use `fd-shader-fix/vX.Y.Z` tags on the
[Bertie release page](https://github.com/bertie-mc/bertie/releases). Put the JAR in your
`mods/` folder alongside NeoForge for Minecraft 1.21.1 and fdlib.

## Credits / Integration

This is a runtime patch for *fdlib*. It wraps fdlib's post-shader init in a try/catch to stop an intermittent boot crash, helping mods built on fdlib — such as Qliphoth Awakening, Cinematic Cataclysm, and AnvilCraft — boot reliably.

## Building

`gradle :mods:fd-shader-fix:assemble` builds the JAR without running the independent test suites.

## Tests

`gradle :mods:fd-shader-fix:test` verifies that successful shader initialization is preserved and failures
are reported without escaping.

## License

Released into the public domain under **The Unlicense** — see [UNLICENSE](UNLICENSE). Third-party assets and dependencies are carved out in [NOTICE](NOTICE).
