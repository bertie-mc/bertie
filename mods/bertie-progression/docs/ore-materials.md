# Ore intermediates

`ore_materials.json` lists the canonical forms for 20 metals, the missing item
registrations, slurry registrations, texture hashes and processing recipe IDs.
Existing items retain their IDs. This includes Oritech's six native metal Gems.
Plutonium is excluded from the added family.

The family fills missing Dust, Dirty Dust, Clump, Gem, Shard, Crystal, dirty and
clean Slurry forms. Each material also has a Spec, Dense Charge and animated
Activated Crystal. Colliders use the consistent Python artwork family. Dense
metal Charges and Activated Crystals have no production or consumption recipes yet.
The separate Dense Coal Charge is made in the Combiner from 32 Coal Dust and one
HDPE Sheet, then centrifuged into one Carbon Fibre Strand in 50 ticks.

Ordinary raw-metal routes enrich one raw item into two Dust, purify two raw items
into five Clumps, and inject one raw item into four Shards. Two raw storage blocks
produce 45 Clumps; one produces 36 Shards. Dissolution retains the native
three-Raw-to-2000-Slurry ratio. Intermediate
conversions retain their native 1:1 quantities; crystallization uses 200 Slurry.
Ore-block recipes, Fortune and mining drops are unchanged.

Each of the 20 metal Centrifuge routes consumes one matching Dirty Dust and
100 mB Sheol Fire, producing one matching Gem and three secondary Specs. The
Fluid Addon is required. Water/Acid Clump-to-Gem alternatives are disabled.
The current byproducts are listed in `ore_materials.json`; no self-material
placeholders remain. Uranium, Elvenite and Solerium intentionally have no secondary source;
every other remaining material has one. Armageddon materials can only be secondary
outputs of higher tiers in their own progression.

Eight matching Specs surrounding one matching Dirty Dust craft two Dirty Dust.
The centre is consumed normally.

Dirty Mithril Scrap is a separate special-material route: one Raw Mithril makes
three Dirty Mithril Scrap in the Mekanism Crusher, and each enriches into one
native Mithril Scrap. Like the existing Ancient Debris → Dirty Netherite Scrap
route, dirty-scrap production is Crusher-only. Clibano's Netherite residue chance
is 15%, retaining its existing fire eligibility and one-Scrap primary result.

All 20 metal Gems have a 1:1 Mekanism Crusher route into matching Dust.
Existing Atomic Forge routes remain; Gem alloying, cooking, heated mixing
and thermal compatibility finishing are disabled. Crystal chemical processing
and component uses such as Uranium Pellet assembly are separate and retained.

Raw Voiderite and Raw Bloody use the supplied provisional
artwork. They have ordinary smelting/blasting links, but mining drops are not
redirected to them. Their existence does not select a world-generation design.

Stellaris and its dedicated addons are withdrawn. Desh, Neptunium and Tharsite
lose their 27 custom items and six slurry registrations. Ostrum's earlier
withdrawal remains. Existing Uranium, Plutonium fuel/dust and Aquaculture's distinct
Neptunium remain.

Solerium, Corronium and the cube-shaped Plutonium family are hosted under
Bertie Progression with their original artwork. Twelve native item forms,
including three ores and the Plutonium Block, are retained; Corronium and Solerium
also regain eighteen processing forms and four slurries. Former Stellaris item
and block IDs alias to the new entries. Native ore drops, smelting and Plutonium
small-unit/block conversions remain. No new ore-generation locations are assigned.
Corronium produces Eezo Specs, contracting its former Tharsite → Desh → Eezo
byproduct chain; Solerium still produces Corronium Specs. Original licensed assets
and namespace adaptations are credited in NOTICE and the provenance manifest.

Resonant Coil retains the `platinum_coil` item ID and uses Platinum Wire with
Malignant Pewter at the top/bottom and Cognitive Alloy in its centre column.
Electric Coil uses Electrum Wire, Cognitive Alloy top/bottom and Royal Steel
in the centre. Ordinary Nether Table crafts produce one; replacing the exact
centre with Bound Soul Ingot produces two Resonant or six Electric Coils.
Existing Magnetic Coil and Electric Coil Assembler routes remain available.

Assembler also makes ordinary Resonant Coils from Platinum Wire, Malignant
Pewter and Electric Coil. Bound Soul variants produce six Electric or two
Resonant Coils in 48 ticks.

The Enderic Compound fluid Centrifuge recipe consumes Warp Flux and 500 mB of
native liquid Timeless Slurry for eight Compounds in 400 ticks. The earlier
potion-component matching compatibility is no longer needed.
