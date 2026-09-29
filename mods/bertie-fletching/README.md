# Bertie Fletching

Extends Fletchery Expanded 1.0.5 on NeoForge 1.21.1 with an ordered catalogue of 12 fletching materials, 12 shafts, 22 tips and 18 extras. See [Components](COMPONENTS.md) for ingredient IDs and descriptions.

Included in Bertie's standard client and server packs. Stable releases and compatibility notes are listed in the [changelog](CHANGELOG.md).

## Crafting

Every craft produces eight arrows, consuming one feather, shaft, tip and optional extra. The potion tank accepts regular, splash and lingering potions, holds enough liquid for 64 arrows, and coats eight arrows per bottle. Click with a bottle to return its empty bottle on the cursor; shift-click to return it to its inventory position. Different potion contents cannot mix. Shift-crafting stops when the coating runs out, and requires space for complete batches.

Empty slots show monochrome item outlines. The tank keeps its fill marks and contents tooltip. Potion strength is preserved; timed effects use one eighth of the potion duration. Potion pixels are drawn over the completed component arrow in inventory, bows, crossbows and the projectile preview. Custom material layers use palettes sampled from the ingredient textures, including resource-pack overrides, instead of multiplying two unrelated colors.

Tags are `fletchery:feather`, `fletchery:shaft`, `fletchery:tip`, `fletchery:extra` and `fletchery:potion`. The aggregate `fletchery:fletchry` tag contains all five groups and can be searched as `#fletchry`. `fletchery:all` is an alias.

## Flight and damage

New arrows store `bertieVersion=2`. The redesigned projectile path retains bow damage and enchantment handling, uses a fixed 1.5x full-draw critical multiplier, and applies component bonuses before mitigation. Armor piercing reduces armor absorption by the specified fraction. Ender shafts bypass shields. Existing arrows without this version marker retain legacy behavior; craft new arrows to use the redesign.

- Wheat uses 80% speed and 120% gravity; Emu uses 120% arrow speed and 80% gravity. Day/night bonuses use the world's day cycle. End rods no longer reset feather speed.
- Stymphalian hits that deal damage emit eight arrows at 10 degrees below horizontal, each with half the original raw hit damage. Child arrows cannot split recursively. Split arrows and ricochet chains share a 24-block secondary travel budget; every bounce consumes the remaining range instead of restarting it. Bone fires eight shrapnel traces in a 60-degree cone, four blocks behind the victim, at 10% raw damage each.
- Dark links last ten seconds, allow three links per target, and transfer half of raw incoming damage. Each receiver applies its own mitigation. Damage sharing cannot recurse or reflect, and the link starts after the triggering hit. The link with the least remaining time is replaced at the cap.
- Homing searches within 24 blocks and 30 degrees of the original direction. Turning is limited to three degrees per block of travel. Guidance preserves speed and cannot restore upward velocity lost to gravity. Hitscan keeps a direct hit already in its path; otherwise it chooses its initial aim once within the same cone. Ricochets search within 24 blocks. End rods ricochet once; Ender rods three times. Immune entity hits and ground impacts can ricochet. Infinity and Ender rods phase through blocks.
- Resonant feathers trace an instantaneous straight ray up to 128 blocks with unlimited entity piercing. Damage uses twice the original arrow speed, not infinite speed. Solid blocks stop rays unless the shaft phases. Resonance tips jump to up to eight distinct targets within 12 blocks, retaining 60% damage per hop.
- Full-draw critical damage is deterministic, and launch spread changes direction without randomizing speed. Sun fire and Reinforced Echo sonic damage inherit the attack's critical roll and projectile bonuses; the secondary victim still applies its own defenses. Sonic particles cover the complete ray, including its endpoint.
- Mnemonic hits repeat half the raw damage after two seconds. Reinforced Echo adds a sonic hit to the closest other target within 24 blocks. Heavy Core adds uncapped damage equal to the arrow's descent from its peak to the victim.
- Permafrost freezes in place for two seconds. Frighten reverses movement input for three seconds. Blaze shafts apply 30% projectile vulnerability for ten seconds after their initial hit. Sun Glowing lasts twenty seconds. Resplendent applies Pastel's native Primordial Fire for ten seconds and launches ten blocks upward.
- Wind Charge affects a seven-block radius and launches targets seven blocks upward under normal gravity. TNT uses explosion power four. Earth drills up to twelve blocks, stops at `c:ores`, and skips ores when paired with phasing. Sponge removes source water within two blocks of the path. Torch trail lights expire after ten seconds.
- Divine and Eldritch execute below 30% health and transfer loot to the shooter. Eldritch hits each entity once within six blocks of its path. Inventory overflow appears at the shooter instead of being deleted. Experience Rod and Refined Brilliance multiply mob XP drops; together they yield three times XP.
- Breeze shafts expire after fifteen seconds of flight. Other missed projectiles expire after sixty seconds of active flight.

Old tank contents are converted to eight-arrow batches; a remaining half-batch rounds up during migration. Existing table ingredients are preserved, including removed components that can be taken back out.

## Combat integration

Primordial Fire uses Pastel's native attachment API when Pastel is installed. Blood Vial and Fiery Blood add one `simplymore:bleed` stack and refresh at least ten seconds of duration.

The combat-unification integration uses `simplymore:bleed` and classifies its ticks as Physical. Dark links use the shared transfer marker when available, preserving damage family and preventing further pack transfers or reflections. Fletching damage types contribute to the physical, energy and pure damage-family tags.

**Follow-up:** verify bleeding stacking, attribution and duration during full-pack playtesting after combat-unification; background fletching tests do not load the full combat stack.

## Validation

`gradle :mods:bertie-fletching:assemble` builds the mod. `test` covers catalogue order, tags, texture references, coating colors and storage. `runGameTests` includes Apothic Attributes and covers actual damage, shared critical rolls, launch consistency, homing, secondary range, mitigation, links, splitting, hitscan, delayed damage, drilling, freezing, XP and crafting transactions. `compileClienttestJava` checks the interactive regression harness without launching a client; `runClientTests` launches that harness when desktop testing is available.

Original code is released under Unlicense. Adapted rendering and screen code and coating masks retain PriestDiO's MIT licence; see [NOTICE](NOTICE).
