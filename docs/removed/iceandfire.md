# Removed — iceandfire

Ice and Fire items removed from the pack. See [README.md](README.md) for what removal means and
which columns are yours.

Note the troll armour is named **variant-first** (`forest_troll_leather_boots`), not `troll_*`. A
`iceandfire:troll_*` glob matches only the leathers and the troll weapons — none of the armour.

**Not touched, deliberately:**

- `deathworm_chitin_{red,white,yellow}` — the materials. They are the contents of
  `#bertie_s1:deathworm_chitin`, which the Sirok's Nest Map recipe consumes.
- `troll_leather_{forest,frost,mountain}` — likewise the materials, not the armour.
- `deathworm_gauntlet_{red,white,yellow}` — a weapon, not part of the set. berlord's count of 24
  excludes them (they would make 27).

| Item | Id | Reason | Removed |
|---|---|---|---|
| Troll leather armour | `iceandfire:{forest,frost,mountain}_troll_leather_{helmet,chestplate,leggings,boots}` | useless, ugly | 2026-07-30 |
| Death worm chitin armour | `iceandfire:deathworm_{red,white,yellow}_{helmet,chestplate,leggings,boots}` | useless, ugly | 2026-07-30 |
| Sheep Disguise set | `iceandfire:sheep_{helmet,chestplate,leggings,boots}` | cut; the cyclops side quest it existed for was dropped | 2026-07-30 |

<!-- LEAKS: generated every build, do not edit by hand -->

## Leaks

Loot tables that still reference a removed id, i.e. ways it can still be obtained despite having no recipe.

_None._

<!-- /LEAKS -->
