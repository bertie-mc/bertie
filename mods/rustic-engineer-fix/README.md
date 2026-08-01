# Rustic Engineer Fix
Runtime patch that fixes *Rustic Engineer*'s airship and dragonfly flight — choppy turning and the pitch-dive bug.

- **Minecraft:** 1.21.1
- **Loader:** NeoForge
- **Mod ID:** `rusticengineerfix`
- **Requires:** the **Rustic Engineer** mod

## Install
Download the latest JAR from the [Releases page](../../releases) and put it in your `mods/` folder. Requires NeoForge for Minecraft 1.21.1 plus Rustic Engineer.

## Building
`gradle build` — the built JAR is written to `build/libs/`.

## Tests

`gradle test` loads the exact Rustic Engineer version in NeoForge's in-process test
environment, verifies both foreign procedure classes received the expected injections,
and covers the yaw and vertical-composition decisions without launching a client.

## License

Released into the public domain under **The Unlicense** — see [UNLICENSE](UNLICENSE). Third-party assets and dependencies are carved out in [NOTICE](NOTICE).
