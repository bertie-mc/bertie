# Bertie Creatures

Standalone Minecraft 1.21.1 / NeoForge mod containing selected Alex's Caves
creatures and Thornwood. Requires Citadel 2.7.6 or later. Alex's Caves is not a
dependency. The namespace is `bertiecreatures`.

## Content

- Deep One Mage, Hullbreaker, Vesper, Nucleeper, Luxtructosaurus, Tremorsaurus,
  Grottoceratops and Atlatitan, with their models, animations, sounds and combat
  projectiles.
- Sea Glass Shards, Vesper Wing and Stew, Fissile Core, Tectonic Shard, Heavy Bone,
  Dinosaur Chop, Cooked Dinosaur Chop, Dinosaur Nugget, Seething Stew and Dark Tatters.
- Tough Hide, retained because it is part of Grottoceratops' original drops.
- Thornwood logs, wood, stripped variants, branches, saplings, planks, stairs,
  slabs, fences, gates, signs, hanging signs, pressure plates, doors, trapdoors,
  buttons, boats and chest boats. Potted branches and saplings are supported.
- Dinosaur egg blocks, thin bones left by eaten chops, and primal magma used by
  Luxtructosaurus attacks.

Hullbreaker drops only 2–5 Sea Glass Shards. Vesper drops only 0–1 Vesper Wings.
Other selected mobs retain their original loot, including on-fire cooking and
Looting where applicable. Luxtructosaurus drops 7–11 Tectonic Shards before Looting.
Deep One Mage has no original death drops.

Seething Stew uses two Cooked Dinosaur Chops, a Heavy Bone and a bowl. Vesper
Stew uses a Vesper Wing, Thornwood Branch, brown mushroom and bowl.
Thornwood saplings use two branches and bone meal in place of the excluded fertilizer.

Wheat replaces Tree Stars for herbivore feeding. Golden carrots replace Serene
Salad for Tremorsaurus taming and Atlatitan riding. Tremorsaurus still needs the
Stunned effect before feeding; no Wooden Club is included. A pack-level way to
apply that effect remains to be chosen. Hay blocks replace fern thatch as egg bedding.

## Prototype scope

Mobs are available through spawn eggs and `/summon bertiecreatures:<mob>`.
Natural spawn placement and initial survival access to Thornwood and Dark Tatters
are left to the pack. No cave biomes, cave structures, Spelunkery Table, or cave
progression are registered. Thornwood's tree feature exists only for sapling growth.

Integrations with excluded content are removed: Deep One altars and bartering,
Vesper sacrifices, submarine detection, Raycat avoidance, Hazmat protection,
primordial cave eruptions and unrelated advancements. Luxtructosaurus uses a
standard boss bar. Existing `alexscaves:` saves are not automatically migrated.
The nuclear cloud and flash are retained; cave-specific sky darkening, global
sound muting and radiation post-processing are omitted.

Mount special ability defaults to **V**; the normal attack key controls a ridden
Tremorsaurus bite. Explosion/block-damage settings retain the upstream defaults
and are configurable in `bertiecreatures-common.toml`.

## Development

From the repository root with the documented Java 21/Gradle environment:

```sh
gradle :mods:bertie-creatures:build
gradle :mods:bertie-creatures:runGameTests
gradle :mods:bertie-creatures:runClientTests
gradle :mods:bertie-creatures:runClient
```

The server tests deliberately load Citadel and this mod without Alex's Caves.
They cover mob serialization and ticking, loot, stew recipes, Thornwood signs and
tree generation, boat materials, multipart damage, replacement taming food,
nuclear detonation, bubble air loss and effect cleanup. Resource tests check model
and particle dependencies. The isolated client test checks all eight mob renderers,
item models, multipart IDs, bubble overlays and particle providers, and captures
mob and animation previews in `build/test-diagnostics/clienttest`.

## License

GPL-3.0-only. See [LICENSE](LICENSE) and [NOTICE](NOTICE) for the exact upstream
revision, authors and credits. Distribute this component's corresponding source,
including the repository's shared build logic and dependency locks, with public
binaries or provide a GPL-compliant source download. No public-domain dedication
applies to this component or its assets.
