# Bertie Forge Ink

NeoForge 1.21.1 addon for **bertie**: the Hephaestus Forge (Forbidden Arcanus 2.6.1) runs on
**Iron's Spells 'n Spellbooks ink** instead of experience.

## Behavior

| Forge tier | Accepted ink | Essence |
|-----------:|--------------------|--------:|
| I | Common Ink | 40 |
| II | Uncommon Ink | 80 |
| III | Rare Ink | 160 |
| IV | Epic Ink | 320 |
| V | Legendary Ink | 640 |

Each tier doubles the previous (`40 × 2^(tier−1)`).

- Only the ink matching the forge's tier is consumed (wrong-tier inks sit inert, like any
  junk item in stock FA — `InputSlot` doesn't filter placement; shift-click won't route them).
- **Removed XP sources:** vanilla XP bottles (essence component stripped), Xpetrified Orb
  (blocked at `EssenceDataInput`; its component is kept so right-click XP redeem still works),
  disenchanting (`ExtractEnchantmentsInput` disabled — also on Essence Utrem Jars),
  stored-essence items pouring EXPERIENCE (`EssenceStorageInput` blocked for that type).
- UI: essence renamed **Ink** (`essence.forbidden_arcanus.experience`), XP slot's glass-bottle
  ghost icon redrawn as the ISS ink pot (no feather) in FA's ghost style. Delivered via a
  force-enabled built-in resource pack (`AddPackFindersEvent`, `Pack.Position.TOP`) so the
  override can't lose the mod-resource load-order lottery.

## Known quirk

Essence Utrem Jars have no tier, so an EXPERIENCE jar can be filled with **any** ink
(right-click) and a Quantum Injector will push that essence into any forge — automation
bypasses the tier gate by design of FA's jar system. The forge slot itself is always gated.

## How it works

- Inks get FA's `forbidden_arcanus:essence_data` component (`EssenceValue(EXPERIENCE, 40·2^(tier−1))`)
  via `ModifyDefaultComponentsEvent` — FA's own `EssenceDataInput` then accepts/consumes them.
- 5 tiny mixins: tier gate in `HephaestusForgeBlockEntity#getInput` +
  `HephaestusForgeMenu#canInput`, whitelists/kills in the three stock
  `HephaestusForgeInput` implementations.
- Forbidden & Arcanus and Valhelsia Core are resolved from the Modrinth maven for
  `compileOnly`; mods.toml orders us `AFTER` forbidden_arcanus so our component patch wins.

## Build

```
gradlew build
```

Jar lands in `build/libs/forgeink-<version>.jar`.

## Licence

The code is released into the public domain under **The Unlicense** — see
[UNLICENSE](UNLICENSE).

**One exception.** The bundled resourcepack override contains a modified version of
Forbidden & Arcanus' Hephaestus Forge GUI texture, by **stal111**, re-labelled so the
gauge reads as ink rather than experience. That file is a derivative of stal111's
artwork, is credited to them, and is not covered by the public domain dedication.
See [NOTICE](NOTICE) for the details.
