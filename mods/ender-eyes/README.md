# Ender Eyes

Adds the **Ender Eyes** helmet enchantment: while worn, looking directly at an Enderman will not anger it.

- **Minecraft:** 1.21.1
- **Loader:** NeoForge
- **Mod ID:** `endereyes`

## Install
Download the latest JAR from the [Releases page](../../releases) and put it in your `mods/` folder. Requires NeoForge for Minecraft 1.21.1.

## Credits / Integration

Clean-room NeoForge reimplementation of the Fabric mod *Ender Eyes*' behaviour; no upstream code was reused.

## Building

`gradle build` writes the JAR to `build/libs/`.

## Testing

`gradle test` boots NeoForge's test environment and checks the enchantment against the
real data-driven enchantment registry. The suite covers enchanted, unenchanted, and
empty helmet slots without launching a graphical client.

## License

Released into the public domain under **The Unlicense** — see [UNLICENSE](UNLICENSE). Third-party assets and dependencies are carved out in [NOTICE](NOTICE).
