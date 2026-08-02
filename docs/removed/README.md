# Removed items

Items berlord has decided should not exist in the finished pack — primarily crafted clutter, above
all the weapon and armour lines that mods ship by the dozen. Removing them unclogs EMI and makes
progression design possible without reading past junk.

**One file per mod, named for its modid.** `slag.md` holds `slag:` items, `minecraft.md` holds vanilla.
The filename *is* the namespace, and the generator rejects any id that does not match it.

---

## What "removed" means here

Two mechanisms, both driven from these files:

| | how |
|---|---|
| **Uncraftable** | every recipe in the pack whose result is a listed id gets overridden with a `neoforge:false` condition. All recipe types, all namespaces — the generator searches by *result*, so you never name a recipe file. |
| **Gone from creative, and from EMI** | a `BuildCreativeModeTabContentsEvent` handler in `bertie_s1` drops the listed ids from every tab. EMI's `index-source` is `creative`, so removing an item from the tabs removes it from EMI too. One mechanism, no second list to keep in sync. |

## Standing rule: the God of War walls (berlord, 2026-07-30)

**Removing armour or a weapon means removing it from the God of War tab too.** That chapter's
equipment walls showcase every armour piece and every weapon in the pack, generated from
`s1-build/godofwar_equipment.json` by `_wall_quests` / `_wall_armor_sets`. A removed item left in
there is a quest asking the player to obtain something that no longer has a recipe.

**berlord ruled 2026-07-30: drop them from the tab one by one, as each item is removed** — not
batched up and done once. So a removal is only finished when the item is gone from the pack *and*
from the walls.

The cost he accepted: a quest's id is its index in the chapter list, so every wall entry dropped
renumbers the ones after it and orphans their save progress. That lands almost entirely on showcase
wall quests rather than the hand-authored God of War tree, which is why doing it incrementally is
cheap here and would not be in a progression chapter.

## What it deliberately does not do

- **World-found items are not touched.** No loot tables are edited. If a removed item still drops
  from a chest or a mob, it is still obtainable — that is what the leaks block below each table is
  for. This is a reporting gap by choice, not an oversight: the assumption is that these lists hold
  craft-only items.
- **It is not retroactive.** Anything already sitting in an existing world's chests or inventories
  stays there. Removal governs fresh progression.
- **The item still exists in the registry.** It has to — you cannot unregister an item without
  breaking every save that contains one. It is unobtainable and invisible, not absent.

## The table

Four hand-written columns. Add a row to remove an item; delete the row to bring it back.

| Item | Id | Reason | Removed |
|---|---|---|---|
| Display name, for reading | exact registry id, or a pattern | why it is gone | date you removed it |

The Id column takes an exact id, a `*` glob, or brace expansion — `foo_{helmet,boots}` becomes two
ids. Braces exist because a bare material glob is usually **wrong**: `l2complements:eternium_*` is
12 items, not the 4 armour pieces, because it swallows the tools, ingot, nugget and block. Spell the
slots out. A pattern that matches nothing, or a literal id that is not a registered item in this
pack, is a **build error** — never a silent no-op. Every expansion prints its match count.

**Below each table the generator writes a `LEAKS` block between HTML-comment markers.** It is
regenerated on every run and lists any loot table in the pack that still references a removed id —
i.e. the ways it can still be obtained despite having no recipe. **Do not edit inside those markers;
everything above them is yours and the tooling never touches it.**

## Running it

`python texture-work/gen_data.py` from the mod directory does everything: parses these files, emits
the recipe disables, writes the id list the Java handler reads, and refreshes every LEAKS block.
It needs the `s1 demo` instance present to scan jars; without it, it warns and skips rather than
silently emitting nothing.

A removal is only live once the jar is rebuilt and synced — recipe overrides and the id list both
ship inside `bertie_s1`.
