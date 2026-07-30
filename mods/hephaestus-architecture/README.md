# Bertie Hephaestus Architecture

NeoForge 1.21.1 addon for Forbidden & Arcanus 2.6.1. It makes the
Hephaestus Forge multiblock and its usable pedestal positions depend on the
forge tier.

## Behavior

- Tier 1 keeps Forbidden & Arcanus' native 9×9 base and eight pedestal slots.
- Tiers 2–5 load Minecraft structure templates:
  - `hephaestusarchitecture:hephaestus_forge/tier_2`
  - `hephaestusarchitecture:hephaestus_forge/tier_3`
  - `hephaestusarchitecture:hephaestus_forge/tier_4`
  - `hephaestusarchitecture:hephaestus_forge/tier_5`
- A higher-tier forge is active only while its tier template matches.
- The one Hephaestus Forge block saved in the template is the anchor.
- Every F&A pedestal saved in the template becomes a legal ritual input
  position. Pedestals elsewhere cannot feed that forge.
- All four horizontal rotations work. Mirrored layouts are not accepted.
- Structure Void is ignored. Saved air must remain air. Every other block state
  must match, including directional properties.
- If a higher-tier template is absent or invalid, that tier logs a warning and
  falls back to the native F&A layout. This keeps development builds playable
  while the final schematics are still being authored.
- Ritual datapacks may use up to 64 distinct `inputs` groups instead of F&A's
  stock limit of eight. An input group's `amount` may occupy multiple
  pedestals.

## Supplying a tier schematic

The most reliable workflow is to convert each schematic to a vanilla structure
template in a temporary creative world:

1. Build the complete tier layout.
2. Put any F&A Hephaestus Forge tier block at its intended center/anchor.
3. Put F&A pedestals at every position that should accept a ritual item.
4. Use Structure Void for positions that must be ignored by validation.
5. Save it with a structure block under the name
   `hephaestusarchitecture:hephaestus_forge/tier_2` (or tier 3–5).
6. Copy the generated NBT into:
   `src/main/resources/data/hephaestusarchitecture/structure/hephaestus_forge/tier_2.nbt`.

The generated world file is normally under
`<world>/generated/hephaestusarchitecture/structures/hephaestus_forge/`.
Minecraft's generated folder uses `structures` (plural), while the 1.21.1
datapack resource folder in this project is `structure` (singular).

## Build

The wrapper delegates to the pinned wrapper in the sibling `forge-ink` project:

```powershell
.\gradlew.bat build
```

Output:
`build/libs/hephaestusarchitecture-0.1.0.jar`

## Known integration note

Bertie's native EMI category handles variable-size ritual input groups. F&A's
optional built-in JEI screen still draws only eight pedestal slots, so rituals
with more than eight total displayed items should be viewed through EMI unless
that JEI layout is patched separately.
