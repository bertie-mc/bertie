# Berlord's Spell Restrictions

A NeoForge 1.21.1 addon for Iron's Spells 'n Spellbooks 3.16.x.

## Progression

Two permanent, per-player gates control scroll production:

- Possessing a scroll anywhere in the normal inventory or offhand discovers its spell. Discovery remains after the scroll is used, transferred, or lost. A manuscript can grant the same knowledge.
- Crafting or upgrading a scroll requires its **output rarity** to be unlocked. Common starts unlocked. Using a crafting orb unlocks its rarity and all lower tiers.

Casting, inscription, and transferring an existing scroll into equipment are not rarity-gated. Found powerful scrolls remain usable under Iron's normal casting rules. Spell levels and minimum rarities stay unchanged; discovery does not create a lower-rarity version of a spell.

Eldritch, Occult (`discerning_the_eldritch:ritual`), and Abyssal (`cataclysm_spellbooks:abyssal`) use research knowledge. Each manuscript immediately teaches one uniformly random, enabled, unknown spell of its school, regardless of crafting tier. There is no research screen. The chat announcement colors only the spell name, using its school color. Complete schools do not consume manuscripts. The original Eldritch manuscript is adapted in place.

All unlocks survive death, dimension changes, and reconnection. Native Eldritch knowledge is retained. Inventory mutations and vanilla changed-slot notifications drive discovery, with a single bounded inventory pass at login/respawn; there is no added inventory polling loop.

## Items and recipes

The mod id is `berlordsspellrestrictions`.

| Item | 3×3 recipe |
| --- | --- |
| Occult Manuscript | Eight Forbidden Knowledge Fragments around `discerning_the_eldritch:shard_of_malice` |
| Abyssal Manuscript | Eight Depth Knowledge Fragments around `cataclysm:crystallized_coral` |
| Common Crafting Orb | Eight Common Inks around Arcane Essence |
| Uncommon / Rare / Epic / Legendary Crafting Orb | Eight matching-rarity inks around the previous rarity's orb |

Orb use is immediate, like a manuscript; it is not eating. Redundant or lower-tier orbs are not consumed. The Common orb is a crafting component under the default settings and becomes a usable unlock when Common is initially locked.

The new manuscript recipes load only when their ingredient mod is installed. Fragment loot, special-school scroll loot removal, and progression placement are intentionally supplied by the pack.

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
cloning. Client appearance and texture-atlas registration are checked in a client.

## Licence

The code is under the MIT licence in [LICENSE](LICENSE). The manuscript and fragment artwork
is excluded from it; see [NOTICE](NOTICE).
