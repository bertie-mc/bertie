# Berlord's Spell Restrictions

A NeoForge 1.21.1 addon for Iron's Spells 'n Spellbooks 3.16.x.

## Progression

Two permanent, per-player gates control scroll production:

- Possessing a scroll anywhere in the normal inventory or offhand discovers its spell. Discovery remains after the scroll is used, transferred, or lost. A manuscript can grant the same knowledge.
- Crafting or upgrading a scroll requires its **output rarity** to be unlocked. Common starts unlocked. Using a crafting orb unlocks its rarity and all lower tiers.

Casting, inscription, and transferring an existing scroll into equipment are not rarity-gated. Found powerful scrolls remain usable under Iron's normal casting rules. Spell levels and minimum rarities stay unchanged; discovery does not create a lower-rarity version of a spell.

Eldritch, Occult (`discerning_the_eldritch:ritual`), and Abyssal (`cataclysm_spellbooks:abyssal`) use research knowledge. Each manuscript immediately teaches one uniformly random, enabled, unknown spell of its school, regardless of crafting tier. There is no research screen. The chat announcement colors only the spell name, using its school color. Complete schools do not consume manuscripts. The original Eldritch manuscript is adapted in place. The Occult and Abyssal manuscripts are named in their school's color and describe themselves the way the Eldritch one does.

Cataclysm: Spellbooks' Ignis and Maledictus spells need learning too. Its Burning Manuscript and Frozen Tablet teach one random unknown spell from their own lists per use, announced the same way; spells on those lists that anyone can already cast are skipped. The Burning and Frozen Knowledge Fragments teach nothing and serve only as crafting material.

All unlocks survive death, dimension changes, and reconnection. Native Eldritch knowledge is retained. Inventory mutations and vanilla changed-slot notifications drive discovery, with a single bounded inventory pass at login/respawn; there is no added inventory polling loop.

## Items and recipes

The mod id is `berlordsspellrestrictions`.

| Item | 3×3 recipe |
| --- | --- |
| Occult Manuscript | Eight Forbidden Knowledge Fragments around `discerning_the_eldritch:shard_of_malice` |
| Abyssal Manuscript | Eight Depth Knowledge Fragments around `cataclysm:crystallized_coral` |
| Frozen Tablet (Cataclysm: Spellbooks) | Eight Frozen Knowledge Fragments around `cataclysm:cursium_ingot` |
| Common Crafting Orb | Eight Common Inks around Arcane Essence |
| Uncommon / Rare / Epic / Legendary Crafting Orb | Eight matching-rarity inks around the previous rarity's orb |

Orb use is immediate, like a manuscript; it is not eating. An orb's tooltip reads like an ink's: gray text with the rarity in its own color. Redundant or lower-tier orbs are not consumed. The Common orb is a crafting component under the default settings and becomes a usable unlock when Common is initially locked.

The new manuscript recipes load only when their ingredient mod is installed. Special-school scroll loot removal and progression placement are intentionally supplied by the pack.

## Knowledge fragment sources

Fragments are added to the loot of the structures and bosses below through Iron's Spells' `append_loot` modifier. A source applies only while its school's addon is installed; sources from other mods simply never match when that mod is absent. `fragment_loot.py` writes the tables and modifiers; edit the sources there and rerun it.

| Fragment | Source | Drop |
| --- | --- | --- |
| Forbidden (Occult) | Cult of Azazel mansion, rare chests / ordinary chests | 80% for 1-3 / 3 rolls at 15% |
| | Nether fortress chests; YUNG's worship room | 3 rolls at 25%; 80% for 1-3 |
| | Blood Cultist Hut bloody chest | 50% for 1 |
| | Cultist Base library, lab and vault chests | 0-4 (binomial, 4 at 25%) |
| | Ascended One, killed by a player | 2-4 |
| Depth (Abyssal) | Elder Guardian, killed by a player; YUNG's ocean monument side chambers | 1-2; 50% for 1-2 |
| | Sunken City chests; the Leviathan, killed by a player | 3 rolls at 25%; 2-4 |
| | Acropolis treasure | 80% for 1-3 |
| | Aquamirae pirate ships and shelters | 20% for 1-2 |
| | Shipwrecks and underwater ruins | 10% for 1 |
| Burning (Cataclysm: Spellbooks) | Ignis, the Burning Arena's boss, killed by a player | 3-5 |
| | Netherite Monstrosity, killed by a player | 1-2 |
| | Citadel vault / rampart chests | 50% for 1-2 / 15% for 1 |
| | Pyromancer Tower chests | 20% for 1 |
| Frozen (Cataclysm: Spellbooks) | Frosted Prison treasure; Maledictus, killed by a player | 80% for 1-3; 3-5 |
| | Cataclysm's abandoned snow structures, treasure / other chests | 50% for 1-2 / 10% for 1 |
| | Ice Spider Den and Mountain Tower chests | 20% for 1 |
| | Impaled Icebreaker captain's quarters | 50% for 1-2 |

The Sunken City's chests share the vanilla shipwreck and dungeon tables, so that source also checks that the chest stands inside the city.

All five orbs share Iron's original empty-orb geometry and orbit motion. Their orbiting lights use rarity colors. The neutral base pulses toward the rarity color over 2.4 seconds. Client atlas animation handles the effect without gameplay tick code. Upstream orb pixels are read from the installed Iron's resources at texture reload; they are not bundled in this JAR.

## Configuration

World server config: `serverconfig/berlordsspellrestrictions-server.toml`.

| Setting | Default | Meaning |
| --- | --- | --- |
| `startingTier` | `COMMON` | Initial tier for new player data: `NONE`, `COMMON`, `UNCOMMON`, `RARE`, `EPIC`, or `LEGENDARY` |
| `requireSpellDiscovery` | `true` | Require discovery to manufacture or upgrade ordinary scrolls |
| `requireCraftingRarity` | `true` | Enforce the crafting rarity ceiling |

Changing the starting tier does not rewrite permanent progress of existing players. Native special-school learning remains required even when the ordinary discovery gate is disabled.

## Installation

Install on both client and server with Iron's Spells and its dependencies. Cataclysm: Spellbooks and Discerning the Eldritch are optional and supply their own spell schools.

Remove **Iron's Spells 'n Spellbooks Restrictions** (`irons_restrictions`); it implements incompatible casting and research restrictions. The loader reports a clear incompatibility if both are present.

## Building

```bash
gradle :mods:berlords-spell-restrictions:assemble
gradle :mods:berlords-spell-restrictions:test
gradle :mods:berlords-spell-restrictions:runGameTests
```

Run these from the monorepo root. The GameTest runtime adds Cataclysm: Spellbooks and
Discerning the Eldritch so the Occult and Abyssal manuscripts have their schools.

The unit suite covers independent gates, configurable starting lock, tier skipping, duplicate
consumption policy, permanent discovery, and orb palette/alpha preservation. Headless GameTests
exercise inventory hooks, real Scroll Forge and Arcane Anvil menus, shared-result pickup,
found-scroll casting, each manuscript, all seven recipes, persistent serialization, and death
cloning, as well as one-spell learning from Cataclysm: Spellbooks' items, the Frozen Tablet recipe,
fragment drops from real chest tables, and school-colored manuscript names. Client appearance and texture-atlas registration are checked in a client.

## Licence

The code is under the MIT licence in [LICENSE](LICENSE). The manuscript and fragment artwork
is excluded from it; see [NOTICE](NOTICE).
