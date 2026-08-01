# ExplodeToMine

Locks certain ores so they must be exploded into a cracked, mineable twin block before they can be harvested - hand-mining the intact ore yields nothing.

- **Minecraft:** 1.21.1
- **Loader:** NeoForge
- **Mod ID:** `explodetomine`

## Install
Existing binaries remain on the
[legacy release page](https://github.com/bertie-mc/explode-to-mine/releases). New releases
use `explode-to-mine/vX.Y.Z` tags in the
[Bertie monorepo](https://github.com/bertie-mc/bertie/releases). Put the JAR in your
`mods/` folder alongside NeoForge for Minecraft 1.21.1.

## Building
`gradle :mods:explode-to-mine:assemble` builds the JAR without running the independent test suite.

## Testing

From the monorepo root, `bertie-ci unit-test --component explode-to-mine` boots NeoForge’s
test environment and verifies that
each locked vanilla ore resolves to the intended registered cracked block while unrelated
ores remain unchanged. CI runs this independently from artifact assembly.

## License

Released into the public domain under **The Unlicense** — see [UNLICENSE](UNLICENSE). Third-party assets and dependencies are carved out in [NOTICE](NOTICE).
