# Bertie Progression (`bertieprogression`)

The progression mod for the **bertie** modpack. NeoForge **1.21.1** / Java **21**.

This mod contains the custom items, gated crafting, Hephaestus Forge shaping rules and
recipe changes used to connect the pack's exploration, technology and magic systems.

> **Scope.** This mod is written for the bertie modpack. It assumes the pack's mod list is
> present and is not intended as a standalone drop-in. It targets no other Minecraft version.

---

## What it adds

### Items

| Item | Role |
|---|---|
| `crafting_license` | the gate token — carrying it is what unlocks gated recipes |
| `descent_anchor` | descent gating |
| `weeping_eye` | locator for the weeping structures |
| `finder` / `locator` | structure-finding items |
| `pocket_watch` | consume once to permanently unlock Pocket Dimension's Open Portal key |
| `green_plushie_token` / `yellow_plushie_token` / `purple_plushie_token` | Doll Machine tokens for the green, yellow and purple gift boxes; four of one colour craft one of the next |

Plus the current material chain — dragonbone frames and braces, ignitium struts and lattices,
kinetic vanes and pattern plates, seals, resonances and attunements — each with its own
texture under `assets/bertieprogression/textures/item/`.

### Blocks

`pocket_dimension` uses Pocket Dimension's animated Pocket Block texture. It has beacon
mining properties and drops itself even when mined by hand. Stand on it and crouch to
enter the existing pocket dimension without consuming a watch. The original return
portal, cooldown and shared/separate plot configuration remain in use. Neither the block nor
the Open Portal key lets a player in for ten seconds after they take a hit; a refused attempt
shows "Combat Cooldown: X sec" in the middle of the screen.

Pocket Watch takes 1.6 seconds to consume, works at full hunger, and unlocks the original
Open Portal key for that player permanently, including after death and reconnecting.
Locked key presses do nothing silently. An already unlocked player cannot waste another
watch. The block and watch are in the Bertie Progression creative tab; recipes and
loot sources are not assigned yet.

The watch has a fixed clock hand over a clockwise purple swirl. Its animation uses
sixteen 16×16 frames, advancing every two ticks (a 1.6-second loop). The case and
hand share the same pixel grid, with nine flat colors and fully opaque or transparent pixels.

The Pocket Dimension integration targets version 2.9 and loads only when that mod is
present. Its block model is referenced at runtime; no third-party texture is bundled.

`eezo_ore` supplies the pack's Eezo material chain.

### Systems

- **Combat integration** (`combat/`) — shared Physical, Magic, Energy and Pure rules,
  armor and Protection formulas, resistance, recovery and ward compatibility.
  See [Combat integration](docs/combat.md) for formulas, coverage and retained exceptions.
- **Crafting gate** (`gate/CraftingGateHandler`) — recipes are withheld until the player
  holds the corresponding licence.
- **Catalyst recipes** (`recipe/CatalystShapedRecipe`) — a shaped recipe that requires a
  catalyst item present but does not consume it.
- **Hephaestus Forge integration** (`forge/`) — bed recipes, forge bed handling and
  pedestal formation rules integrated with Forbidden & Arcanus.
- **EMI integration** (`emi/`) — built-in Mallet Work and Ominous Fan recipes, plus pack-specific
  item visibility policy.
- **Doll Machine** (`doll/DollLottery`) — Kaleidoscope Doll's machine takes Plushie Tokens and
  deals only mob dolls; a yellow box holds a special doll 10% of the time and a purple box 50%.
  Each mob doll also drops from its own mob when a player kills it: 1%, or 5% for the Warden,
  Elder Guardian and Iron Golem and 10% for the Ender Dragon and Wither.
- **Baby wither skeletons** (`BabyWitherSkeletonSpeed`, `BabyWitherSkeletonOddsMixin`) — Tiny
  Skeletons' baby wither skeleton appears half as often and moves twice as fast.
- **Allay corruption** (`AllayCorruptionHandler`).
- **Removed items** (`RemovedItems`) — items withdrawn from the pack's progression.
- **No Mundane Potion** (`PotionBrewingBuilderMixin`) — no brewing mix produces it, and its
  potion, splash, lingering and tipped-arrow forms are hidden from creative tabs and EMI. Recipes
  and loot that named it are re-emitted without it: Timeless Slurry brews from a Thick Potion and
  colour film develops from the Awkward and Thick potions alone.
- **Vanity projectile sync** (`VanityEntityItemPacketMixin`) — a projectile that drops no item
  is sent to Vanity as a plain arrow, because Vanity's packet cannot encode an empty stack and
  the failure disconnected the player.

### Data

Removed potion families and effects use `removed_potions.json` and `removed_effects.json`,
alongside the existing item/tab removal lists. L2 Armor Corrosion cannot be brewed, applied,
forced onto an entity, or crafted into tipped arrows. Its normal, extended and strong forms
are hidden from creative tabs and EMI, with their recipes removed from the viewer. Loading
an older entity clears both the effect and its armor penalty. Registry entries remain for
save compatibility. L2's separate Corrosion trait and Ring of Corrosion are unaffected.

About 750 JSON files: the R01–R42 recipe ledger, darkstone stonecutting recipes,
35 Hephaestus Forge rituals (the four chunk loaders among them), and recipe additions or
overrides in the namespaces of the pack's other mods (Create kinetics, Malum spirit infusion,
Ice and Fire, Immersive Armors, Twilight Forest equipment, Avaritia, Cataclysm, Deeper Darker,
L2 Hostility loot modifiers and others). A Patchouli field guide documents the current progression in game.

---

## Building

```bash
gradle :mods:bertie-progression:assemble
gradle :mods:bertie-progression:test
gradle :mods:bertie-progression:runGameTests
```

Run these from the monorepo root inside `nix develop`. The JAR lands in
`mods/bertie-progression/build/libs/`.

`gradle :mods:bertie-progression:test` validates every JSON resource and local model texture reference, the current Brick
Forge bed table, and catalyst recipe remainder behavior.

GameTests exercise Pocket Dimension 2.9 with the new block, locked and unlocked key
travel, return travel, hand mining, watch consumption and unlock persistence. Their
flat-world preset explicitly includes a void pocket dimension because Minecraft's
GameTest server otherwise omits dimensions supplied outside the preset.

Toolchain is the shared Bertie harness: NeoForge `21.1.235`, ModDevGradle `2.0.134`,
Gradle `8.14.4` from Nixpkgs.

## Releasing

Bump `mod_version` in `mod.properties`, commit it, and create a namespaced tag:

```bash
git tag -s bertie-progression/v0.25.3 -m "Release bertie-progression v0.25.3"
git push origin bertie-progression/v0.25.3
```

The root release workflow builds the JAR and attaches it to a GitHub Release. Releases are
manual; confirm the current commit's relevant CI jobs are green before tagging.

---

## Licence

Released into the public domain under **The Unlicense** — see [UNLICENSE](UNLICENSE).

This covers the original code and assets here. It does not cover the mods this one
depends on or interoperates with; those keep their own licences and none of their
content is redistributed in this repository.
