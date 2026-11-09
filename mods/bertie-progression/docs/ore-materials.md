# Ore intermediates

`ore_materials.json` lists the canonical forms for 24 metals, the missing item
registrations, slurry registrations, texture hashes and processing recipe IDs.
Existing items retain their IDs. This includes Oritech's six native metal Gems.
Plutonium is excluded from the added family.

The family fills missing Dust, Dirty Dust, Clump, Gem, Shard, Crystal, dirty and
clean Slurry forms. Each material also has a Spec, Dense Charge and animated
Activated Crystal. Colliders use the consistent Python artwork family. Dense
Charges and Activated Crystals have no production or consumption recipes yet.

Missing raw-metal routes follow the existing two-Dust enrichment and three-Clump
purification baseline. Higher chemical processing inherits Mekanism's existing
three-Raw-to-eight-Shards and three-Raw-to-2000-Slurry ratios. Intermediate
conversions retain their native 1:1 quantities; crystallization uses 200 Slurry.
Ore-block recipes, Fortune, mining drops and fluid Centrifuge variants are unchanged.

Dry Centrifuge recipes consume one matching Dirty Dust and produce one Gem plus
four Specs. Iron gives Nickel, Copper gives Gold, Gold gives Copper, Nickel and
Osmium give Platinum, Tin gives Lead and Lead gives Tin. All other materials
currently give their own Specs as placeholders. Platinum's former Fluxite and
Uranium's Plutonium associations are unresolved in this family. The seeded Spec
recycling craft remains deferred.

Raw Voiderite, Raw Bloody, Raw Ostrum and Ostrum Ingot use the supplied provisional
artwork. They have ordinary smelting/blasting links, but mining drops are not
redirected to them. Their existence does not select a world-generation design.

Platinum Coil uses platinum windings and a Malignant Pewter top core. Its recipe
is not assigned. Existing Electric Coil is unchanged.

The Enderic Compound fluid Centrifuge recipe consumes Warp Flux and 500 mB of
ordinary Mana Potion for eight Compounds. The `bertieprogression:mana_potion`
fluid tag delegates fluid identity to Create; the optional Oritech mixin also
checks the potion contents and decorates the recipe-viewer fluid preview.
