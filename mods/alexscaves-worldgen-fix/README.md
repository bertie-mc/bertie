# Alex's Caves Worldgen Fix

Runtime patches for *Alex's Caves* on NeoForge 21.1 — a crash that kills world generation, a crash
that kills the client, and two cave biomes that generate half-vanilla.

- **Minecraft:** 1.21.1
- **Loader:** NeoForge
- **Mod ID:** `alexscavesworldgenfix`
- **Targets:** **Alex's Caves (Unofficial Port)** 2.0.9 and later — harmless without it

> The name predates the later fixes. It still only patches Alex's Caves.

Nothing here repairs terrain that already generated. The fixes apply as chunks are created, so
seeing them work means new terrain.

## Bug 1 — worldgen dies during biome decoration

Alex's Caves redirects *every* `List.get(int)` inside `ChunkGenerator#applyBiomeDecoration` through
a clamp that is meant to survive a stale feature index:

```java
if (index < 0 || index >= list.size()) {
    int safeIndex = Math.max(0, Math.min(index, list.size() - 1));
    return list.get(safeIndex);
}
```

On an **empty** list that is `max(0, min(index, -1))` → `0`, and `get(0)` throws
`IndexOutOfBoundsException: Index 0 out of bounds for length 0` — the exact crash the clamp was
added in 2.0.9 to prevent. Vanilla wraps the decoration loop in `catch (Exception)` and rethrows a
`ReportedException`, which takes down the server's worldgen thread. On an integrated server the
symptom is a world that simply stops generating: loaded chunks keep rendering and terrain ends
against empty sky.

Upstream: [Raguto/AlexsCaves-1.21.1#172](https://github.com/Raguto/AlexsCaves-1.21.1/issues/172),
open against 2.0.10 — the newest build — with no fix.

### Why the list is empty, and what it costs

`STRONGHOLDS` is a decoration step no ordinary biome populates — strongholds are a structure, not a
placed feature — so its entry in the globally sorted feature list is empty. Alex's Caves' cave
biomes are the only ones that declare features there:

| biome | features at `STRONGHOLDS` |
|---|---|
| Primordial Caves | `caveman_house` |
| Magnetic Caves | `galena_hexagon_floor`, `galena_hexagon_ceiling` |
| Forlorn Hollows | `thornwood_tree_with_branches` |
| Candy Cavity | four |
| Toxic Caves, Abyssal Chasm | none |

`STRONGHOLDS` is step 5 of 11, and the throw aborted the whole method, so every later step was lost
with it: `UNDERGROUND_ORES`, `UNDERGROUND_DECORATION`, `FLUID_SPRINGS`, `VEGETAL_DECORATION`,
`TOP_LAYER_MODIFICATION`. That is every tree in a Primordial Cave, the ore pass and crystals in
Magnetic Caves, the block palette and `forlorn_ruins` in Forlorn Hollows. The two biomes with
nothing at `STRONGHOLDS` were the two that looked fine.

## What this does

Wraps the call that hands the decoration loop its feature list and returns a view that answers an
out-of-range index with a placement-free no-op feature instead of throwing. Alex's Caves' clamp is
left alone and now clamps against that view, so the unidentifiable feature alone is skipped and
every later step still runs.

It cannot be fixed at the `get` itself: Alex's Caves already owns that instruction with a
`@Redirect`, and one injector owns an instruction. The list's `size()` still reports the truth, so
on lists that are not empty the clamp picks exactly what it picked before — this mod does not get to
redirect worldgen.

Wrapping the whole of `applyBiomeDecoration` is kept as a backstop, in case a future Alex's Caves
build clamps somewhere this does not cover. It should now stay silent. When it does fire the filter
is narrow: the throwable must be an `IndexOutOfBoundsException` **and** some frame of its own stack
must be Alex's Caves' clamp handler, so an unrelated worldgen bug still crashes rather than becoming
silent missing terrain.

Both skipped features and absorbed failures are logged — the first three, then every hundredth.

## Bug 2 — pick block crashes the client

`ACItemRegistry.getSpawnEggFor` walks Alex's Caves' own spawn eggs looking for one whose entity type
matches, and identifies each by calling `egg.getType(null)`. Older NeoForge tolerated a null stack;
21.1.233's `SpawnEggItem#getType` goes straight to `stack.getOrDefault(...)`:

```
NullPointerException: Cannot invoke "ItemStack.getOrDefault(...)" because "p_330335_" is null
  at SpawnEggItem.getType(SpawnEggItem.java:150)
  at ACItemRegistry.getSpawnEggFor(ACItemRegistry.java:389)
```

It throws on the *first* egg it examines, so the lookup is broken for every entity, not just the one
that asked. Middle-clicking a Gum Worm segment routes through it and takes the client down.

Fixed by substituting a real stack for the null one. A spawn egg with no `entity_data` component
reports its own default type, so the search does what it was written to do and pick block yields the
Gum Worm egg. Callers that already pass a stack are untouched, so a future Alex's Caves build that
fixes this itself keeps working.

## Bug 3 — Magnetic Caves and Forlorn Hollows generate half-vanilla

A cave carved by Alex's Caves is a big hollow area whose roof and upper walls are its own blocks
while the floor and lower walls are vanilla stone, decorated by vanilla features, with a flat
horizontal seam between the two.

Six cave types inherit `replaceBiomes` from `AbstractCaveGenerationStructurePiece`, which walks down
from a point below sea level and overwrites each chunk section's biome container so the cave keeps
its own biome. Four call it at the end of `postProcess`:

| piece | cave | calls `replaceBiomes` |
|---|---|---|
| `DinoBowlStructurePiece` | Primordial Caves | yes, offset 32 |
| `CakeCaveStructurePiece` | Candy Cavity | yes, offset 32 |
| `AcidPitStructurePiece` | Toxic Caves | yes, offset 20 |
| `OceanTrenchStructurePiece` | Abyssal Chasm | yes, offset 16 |
| `FerrocaveStructurePiece` | Magnetic Caves | **never** |
| `ForlornCanyonStructurePiece` | Forlorn Hollows | **never** |

Without that call the cave's biome is only ever whatever the noise biome source sampled, and Alex's
Caves places its biomes on a climate window that includes `depth` (see
`config/alexscaves_biome_generation/`). Where the sampled depth leaves the window the biome reverts
to the vanilla underground one — vanilla blocks, vanilla features — while everything above stays
Alex's Caves. The four caves that pin their biome overwrite that boundary before it can be seen.
These two never did, which is why the seam appears in exactly this pair.

The `depth` parameter comes from the overworld noise router, so any mod that reshapes it moves the
seam. That is why this reads as an incompatibility rather than a plain bug — on unmodified noise the
crossing can sit below the cave floor and never show.

### What this does

Calls `replaceBiomes` at the tail of `postProcess` for both pieces, where the other four already
call it. The offset is derived from the piece's own bounding box rather than a constant, so the walk
always starts a section above the cave's roof wherever the generator put it, instead of assuming a
fixed depth below sea level.

Running it twice is safe — it only assigns section biome containers — so a future Alex's Caves build
that adds the call itself costs nothing. If that build also *reshapes* the pieces, the injectors are
declared `require = 1` and fail loudly rather than silently doing nothing.

`replaceBiomes` also carries a process-wide `replaceBiomesError` latch: one exception anywhere
disables biome replacement for every cave for the rest of the session, and it is never reset. It is
not tripping today. If it ever does, the other four caves start showing this same seam.

## Install

Releases use `alexscaves-worldgen-fix/vX.Y.Z` tags on the
[Bertie release page](https://github.com/bertie-mc/bertie/releases). Put the JAR in your `mods/`
folder alongside NeoForge for Minecraft 1.21.1.

Remove it once upstream fixes the clamp.

## Building

`gradle :mods:alexscaves-worldgen-fix:assemble` builds the JAR without running the test suite.

## Tests

`gradle :mods:alexscaves-worldgen-fix:test` covers the recognition logic without launching a game,
including a transcription of the upstream clamp that proves the empty-list case throws.

## License

Released into the public domain under **The Unlicense** — see [UNLICENSE](UNLICENSE). Third-party
assets and dependencies are carved out in [NOTICE](NOTICE).
