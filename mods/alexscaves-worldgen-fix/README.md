# Alex's Caves Worldgen Fix

Runtime patches for two *Alex's Caves* crashes on NeoForge 21.1 — one that kills world generation,
one that kills the client.

- **Minecraft:** 1.21.1
- **Loader:** NeoForge
- **Mod ID:** `alexscavesworldgenfix`
- **Targets:** **Alex's Caves (Unofficial Port)** 2.0.9 and later — harmless without it

> The name predates the second fix. It still only patches Alex's Caves.

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

## What this does

Wraps `applyBiomeDecoration` and absorbs that one failure. The chunk keeps the decoration that ran
before the fault and loses the rest, so it may be short some ores or trees; every other chunk is
untouched and the server stays alive.

The filter is narrow on purpose: the throwable must be an `IndexOutOfBoundsException` **and** some
frame of its own stack must be Alex's Caves' clamp handler. Any other worldgen failure is rethrown
unchanged, so this cannot quietly turn an unrelated bug into missing terrain.

Absorbed failures are logged — the first three, then every hundredth — with the chunk position.

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
