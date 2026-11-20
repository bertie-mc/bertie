# Cybernetics and refining

The Augmentation Center and basic Research Station use 5×5 Nether Table recipes.

The NeoForge version required by Unbound needs compatibility hooks for older
owo and Forgified Fabric API packet registration. Packet codecs are resolved
using their registered connection phase and direction, including Do a Barrel
Roll's configuration synchronization. Existing resolved codecs are preserved.
The twelve selected research nodes retain native energy, timing and application
costs. Living Fortress and Flight exchange their tree positions and dependency
roles, retaining their identities, effects, station requirements and costs.
Auto-Feeder becomes Adaptive Immune System: its saved identity and costs remain,
while its effect protects against Unbound's vacuum instead of consuming food.
Disabled or removed immunity restores native vacuum behavior. The exact
cost arrays are authored in `texture-work/augmentation_research_costs.json`.

The Mechanical Adapter powers only the Augmentation Center. Assemble the center
with its native Machine Cores. Existing linked Copper substitutes migrate to
basic Machine Cores. Place one adapter controller against the center's rear core
wall, add one Machine Core above, and right-click the controller. Right-clicking
with a held core also places that core and assembles it in one action. An
assembled upper section returns the original core tier when broken. The item
and unassembled controller occupy one block; the core completes the upper section.
Rear corner mounting follows the adjacent core's controller link.
The two shaft inserts rotate with their individual Create inputs; the housing
stays still.
The two rear shaft sockets must both be driven in the same direction; equal RPM
is unnecessary. Each input applies eight stress units per RPM. Charging is
32 RF per tick per RPM of the slower input, without a minimum speed threshold.
These defaults do not change existing research energy costs. External RF input
and extraction remain closed through both controller and multiblock ports.
Stopping or removing the adapter retains stored energy and research progress.
Research-station construction and controller/research/application recipes remain
unchanged. The combined entry quest retains its current main-quest position and Precision Mechanism prerequisite.

Untreated Reinforced Carbon Sheet uses the 7×7 End Table. Refinery combines one Untreated
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
four Steel Sheets and a Bucket to produce one block.

Schrödinger's Safe uses the captured 7×7 End Table recipe. Confined Energy Anvilon
is an accepted high-capacity contained core, added to Oritech's native high tag.

Dragonsteel collision cycles **Fire → Ice → Lightning → Fire**. The falling Tier-I
anvil hits the source Dragonsteel block above the target element's block: Frost
Metal for Ice, Lightning-Infused Netherite for Lightning, Ember Metal for Fire.
Both blocks are consumed for eight target ingots; the anvil survives at 32 m/tick.
Bertie EMI draws this vertical order explicitly.
