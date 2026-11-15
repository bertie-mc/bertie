# Cybernetics and refining

The Augmentation Center and basic Research Station use 5×5 Nether Table recipes.
The twelve selected research nodes retain native energy, timing, requirements,
effects and application costs; only their item research costs change. The exact
cost arrays are authored in `texture-work/augmentation_research_costs.json`.

Empty Reinforced Carbon Sheet uses the 7×7 End Table. Refinery combines one Empty
Sheet and 3,000 mB Light Naphtha into one Reinforced Carbon Sheet. Plating crafts
produce eight blocks, Carbon Plating uses Fibre in the centre, and the four
Assembler alternatives are disabled.

Refinery converts one clean ore-material Dust and 50 mB Sulfuric Acid into
100 mB Mineral Slurry. Eligibility is based on registered ore block forms and
matching clean Dust forms, including untagged planet/Armageddon ores. Worldgen
configuration is not inspected. Alloy/refined dusts without an ore form are
excluded. The selected IDs are in `ore_dust_families.json` and the
`bertieprogression:ore_material_dusts` item tag.

The Pearl Fragment Lava route gives 10 mB Strange Matter; Levitation Powder Block
replaces End Dust and gives 200 mB. Steam, Sheol Fire and times remain unchanged.
All stock Clump-input Centrifuge compatibility routes are disabled; the selected
Dirty Dust + Sheol Fire → Gem + three Specs routes remain.

Extras' Absolute, Supreme, Cosmic and Infinite Fluid/Chemical Tanks and Energy
Cubes are withdrawn. Native Basic through Ultimate storage remains. Mekanism's
native Installer blacklist stops Ultimate storage entering the retired tiers and
stops existing retired storage upgrading further; factory Installer uses remain.
Naquadah Reactor Controller uses an Ultimate Chemical Tank. Dynamic Tank uses
four Steel Sheets and a Bucket to produce four blocks.

Schrödinger's Safe uses the captured 7×7 End Table recipe. Confined Energy Anvilon
is an accepted high-capacity contained core, added to Oritech's native high tag.

Dragonsteel collision cycles **Fire → Ice → Lightning → Fire**. The falling Tier-I
anvil hits the source Dragonsteel block above the target element's block: Frost
Metal for Ice, Lightning-Infused Netherite for Lightning, Ember Metal for Fire.
Both blocks are consumed for eight target ingots; the anvil survives at 32 m/tick.
Bertie EMI draws this vertical order explicitly.
