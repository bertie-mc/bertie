# ExplodeToMine

Locks certain ores so they must be exploded into a cracked, mineable twin block before they can be harvested - hand-mining the intact ore yields nothing.

- **Minecraft:** 1.21.1
- **Loader:** NeoForge
- **Mod ID:** `explodetomine`

## Install
Download the latest JAR from the [Releases page](../../releases) and put it in your `mods/` folder. Requires NeoForge for Minecraft 1.21.1.

## Building
`gradle build` — the built JAR is written to `build/libs/`.

## Testing

`bertie-ci unit-test --project .` boots NeoForge’s test environment and verifies that
each locked vanilla ore resolves to the intended registered cracked block while unrelated
ores remain unchanged. CI runs this independently from artifact assembly.

## License

Released into the public domain under **The Unlicense** — see [UNLICENSE](UNLICENSE). Third-party assets and dependencies are carved out in [NOTICE](NOTICE).
