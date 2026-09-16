# Creeper Overhaul Fix

Stops *Creeper Overhaul*'s tiny cactus from opening a hole in the ground it is planted on.

- **Minecraft:** 1.21.1
- **Loader:** NeoForge
- **Mod ID:** `creeperoverhaulfix`
- **Targets:** **Creeper Overhaul** 4.0.6 — harmless without it

## The bug

`creeperoverhaul:tiny_cactus` is a 4x4x4 nub built from a bare `BlockBehaviour.Properties.of()`,
so it keeps `canOcclude`. Every vanilla bush clears that flag through `noCollission()`, and a block
that claims to occlude is allowed to hide the faces its own shape covers.

On its own the nub covers too little to hide anything. Client Tweaks' `creativeBreakingSupport`
changes that: for a creative-mode client it widens the shape of any block with an offset function to
the whole footprint, so randomly offset plants stay easy to click.

```java
bounds().expandTowards(-1, 0, -1).expandTowards(1, 0, 1).intersect(new AABB(0, 0, 0, 1, 1, 1))
```

`BlockBehaviour#getOcclusionShape` reads that same shape. The widened nub is `0,0,0 -> 1,0.25,1`,
its down face covers a full square, and the block below stops drawing its top — a hole straight
through the terrain, in creative mode only. Vanilla plants are immune because they never claim to
occlude in the first place.

## The fix

The block's properties are cleared of `canOcclude` while they are still mutable: `Block`'s
constructor builds the state definition after `BlockBehaviour`'s constructor returns, and that is
where each state copies the flag. `Block#shouldRenderFace` then short-circuits before it consults
any shape, so the ground keeps its face whatever a third party reports for the cactus.

Selection, collision and the widened creative hitbox are left alone.

## Install

Releases use `creeper-overhaul-fix/vX.Y.Z` tags on the
[Bertie release page](https://github.com/bertie-mc/bertie/releases). Put the JAR in your `mods/`
folder alongside NeoForge for Minecraft 1.21.1 and Creeper Overhaul.

## Building

`gradle :mods:creeper-overhaul-fix:assemble` builds the JAR without running tests.

## Tests

`gradle :mods:creeper-overhaul-fix:test` pins the interaction: a widened occluding cactus culls the
face below it, clearing the flag restores it, and the mixin reaches the class Creeper Overhaul
registers.

## Credits / Integration

This mod contains only original code. It names the target mod's block class to clear one property —
no code from *Creeper Overhaul* is included.

## License

Released into the public domain under **The Unlicense** — see [UNLICENSE](UNLICENSE).
Third-party dependencies are carved out in [NOTICE](NOTICE).
