# Rustic Engineer Fix
Runtime patch that fixes *Rustic Engineer*'s airship and dragonfly flight — choppy turning and the pitch-dive bug.

- **Minecraft:** 1.21.1
- **Loader:** NeoForge
- **Mod ID:** `rusticengineerfix`
- **Requires:** the **Rustic Engineer** mod

## Install
Existing binaries remain on the
[legacy release page](https://github.com/bertie-mc/rustic-engineer-fix/releases). New
releases use `rustic-engineer-fix/vX.Y.Z` tags in the
[Bertie monorepo](https://github.com/bertie-mc/bertie/releases). Put the JAR in your
`mods/` folder alongside NeoForge for Minecraft 1.21.1 and Rustic Engineer.

## Building
`gradle :mods:rustic-engineer-fix:assemble` builds the JAR without running the independent test suite.

## Tests

`gradle :mods:rustic-engineer-fix:test` loads the exact Rustic Engineer version in NeoForge's in-process test
environment, verifies both foreign procedure classes received the expected injections,
and covers the yaw and vertical-composition decisions without launching a client.

## License

Released into the public domain under **The Unlicense** — see [UNLICENSE](UNLICENSE). Third-party assets and dependencies are carved out in [NOTICE](NOTICE).
