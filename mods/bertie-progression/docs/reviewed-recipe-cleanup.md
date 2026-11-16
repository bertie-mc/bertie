# Reviewed recipe policy

`texture-work/reviewed_progression_recipes.py` applies the selected policy after
the earlier generation passes. It also runs independently against an installed
mod set. The selected materials and exact layouts are in
`recipe_cleanup_selections.json`; the bounded duplicate/compatibility worklist is
in `recipe_cleanup_audit.json`.

## Crafting and building forms

Slab recombination keeps its original cost and arrangement. Generated Mekanism
Additions duplicates and Create returns already covered by native shapeless
recipes are disabled. Other duplicate candidates are removed only when their
effective definitions match and the removed ID has no active data/code reference.

Chiseled blocks use a five-brick plus for five blocks, with the selected matching
cut/full-block fallbacks. Mosaics use eight planks in a square with one empty corner for eight; pillars use
three matching full blocks in a vertical column for three. Material slates retain
their crafts. Competing block crafts, Iron Bars, trapdoors and Lunchbox use the
reviewed layouts. Raw Amethyst Blocks smelt into Polished Amethyst, without a
direct crafting reversal.

Handcrafted retains the wood-trim grid crafts. Quark Vertical Planks remain
available through explicit Sawmill recipes, independently of automatic recipe
generation. Pastel retains the Calcite building family. Runic Malum walls use
stonecutting, and the three Pastel detectors have distinct centre materials.

## Retained storage forms

AnvilCraft Gunpowder Block retains its native behavior and references Crate
Delight's bag model at runtime. Sugar, Rotten Flesh, Sweet/Glow Berry and Cocoa
storage use their selected bags/crates; Charcoal uses Mekanism's block. Recipes,
loot and tag memberships name the retained forms. Native behavior of withdrawn
block classes is not transferred to a different block by the merge.

Both Nether Wart forms remain. Plutonium's forms remain separate; its specifically
withdrawn Time Warp block-to-raw route does not consolidate the material family.

## Acquisition and retirement

The selected Sculk Table shortcuts and End Table Record Fragment recipe are
disabled. Refined Coal moves to the Sculk Table. Crushing a Music Disc produces
one Record Fragment, with an independent 50% chance of a second.

Crafting Tablet, Spawner Controller/Cage, Fragment Forge, AnvilCraft Oil and all
five Oritech Small Clumps are withdrawn. Full Clumps remain in the material chain,
including Nickel and Platinum's Purification/Crushing intermediates. Withdrawal
does not unregister content or erase existing inventories/world blocks.

Dynamic Tank and Thermal Evaporation Block crafts each produce one. Bio Generator
uses seven Biosteel Ingots. The Charger makes Magnet Ingots from Nickel. Flower
Time Warp has a 1% Wither Rose chance, and the skull conversion consumes four
Wither Roses. Metal-block-to-raw Time Warp routes are disabled while raw-block
packing/unpacking remains.

## Compatibility boundaries

Schema repairs retain quantities, chances and fluid volumes. Optional compatibility
recipes require their providers and registered outputs. Known wizard-base armor
tag ingredients use tags, and the Azure Seastone wall/Warlock leggings endpoints
name the correct family members. Empty deliberate recipe overrides use a valid
false condition.

Missing-material choices not selected in the review remain outside this policy;
they are not resolved by inventing new dependencies or replacing retained inputs.
