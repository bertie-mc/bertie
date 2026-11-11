# Deep resources and machine progression

Sturdy Deepslate drops itself when harvested at Netherite mining level. Its
ordinary Netherite mining time matches the former untagged block mined with
Haste II; tool improvements and status effects retain their normal scaling.
The native Wither/Dragon immunity and other unrelated properties remain.
Foundry combines one Steel Ingot and one Sturdy Deepslate into four Deep Alloy
Ingots in 80 ticks.

## Mineral Fountain

The ordinary Cinerite fallback is Sturdy Deepslate. With Deepslate above the
Fountain, each operation resolves the first successful stage:

1. Earth Core chance → Earth Core Shard Ore.
2. Void chance → Void Stone.
3. A 50% roll → a weighted ore from the current dimension.
4. Otherwise → Sturdy Deepslate.

Native special chances come from the effective AnvilCraft recipes: Overworld
starts at 1% each, Nether at 10% Earth Core, and End at 10% Void. Each adjacent
Earth Core Shard Block or Void Matter Block adds three percentage points to
its corresponding conditional roll. The rare outcomes are excluded from the
ordinary ore pool. Existing Lava/heater behavior and height restrictions remain.

The four horizontal neighbours are the booster area. Ore blocks add 75% of
their base ore weight each: `weight * (1 + 0.75 * count)`. Raw storage blocks
do not count and boosters are not consumed. The ore stage remains 50%; bonuses
change which ore it chooses. Neighbour snapshots update at most once every
200 ticks, so changed boosters can take ten seconds to affect generation.

Ore pools use effective generation settings for the dimension's biomes.
Weights follow vein size, placement count, rarity filter and biome occurrence
linearly. Stone/deepslate variants share a material pool entry. The owned
Tungsten and Solar Core features supply their native mean vein sizes. Retired
duplicates are normalized to canonical ores; off-dimension ores are not added
by booster placement. Pools rebuild on data reload or level lifecycle changes.

## Crafting and processing

The selected Foundry, Fuel Generator, Enderic Laser, Osmium Compressor,
Combiner, Configurator, Transmission Pole and Bottle of Ruin use Nether Table
recipes. Fluid Addon, Target Designator and Energy Tablet retain ordinary 3×3
crafting. The Foundry requires two Enderic Lenses. The Transmission Pole's
four-row recipe sits at the bottom of its five-row EMI grid.

Energy Cubes and factory upgrades use Osmium, Refined Glowstone, Electrum,
Refined Obsidian, Energite, Adamant and Duratium side pairs from Basic through
Cosmic. Factory alloy corners, Control Circuits, centres and stored-data transfer
remain. Basic Cube corners use Cognitive Alloy. Infinite tiers, Bins and tanks
remain unchanged.

Metal Gems finish through 1:1 Crusher conversion to matching Dust or retained
Atomic Forge recipes. Alloying, cooking, heated mixing and thermal compatibility
bypasses are disabled. The separate Crystal/Shard chain remains available.

Frost and Ember Time Warp use the selected infused-material boosters. Radiant
Steel alchemy consumes 1000 mB Timeless Slurry for two Ingots; Nether Star
Brilliance retains its three Crystals and 2000 mB Lava for four outputs.

The focused `runIntegrationGameTests` suite exercises native Fountain ticks,
dimension pools, effective chance recipes, booster units, mining speed and the
survival drop path with AnvilCraft and Bertie Tiers. Its fixture substitutes
Stone for an unrelated optional Pastel worldgen block so that the focused
server can load without the wider combat/magic fixture.

## Warden materials and the Otherside

Warden loot retains the Sculk Catalyst and gives one to Looting level plus one
Carapaces through Minecraft's ordinary Looting roll. Heart of the Deep and
Primordial Soulstone no longer drop from Wardens. Warden Tendril tells players
to shear the Warden.

One Carapace yields one Bone Shard on Crushing Wheels, or two on the Pulveriser
or Mekanism Crusher. Crushing Wheels alone have a 5% Echo Shard bonus.
Reinforced Echo Shard and Warden Stick use their Nether Table patterns.
Any elemental Dragonforge makes one Sculkium from one Duratium and one
Reinforced Echo Shard. The Shard occupies the forge's second ingredient slot;
the former Foundry route is removed.
Heart of the Deep is crafted on the Nether Table with the selected Echo/Core/
Bone Shards, Creaking Heart, Dragon Hearts and Heart of the Sea.

Deep Foundations extends Earth Core Shard through the Warden materials and
Heart to entering the Otherside. The separate Ruin branch unlocks Bedrock Dust
and entering Imbrifer. The Beacon branches also introduce the Giant Anvil and
rings, and the Ember Smithing Template and Anvil.

## Dragonforge and Forge III

Only Imbrifer naturally generates Stage 5 dragons. Cave dragons and skeletons
generated elsewhere are at most Stage 4; existing dragons and ordinary growth
are unchanged. Stage 4 corpse harvesting retains Hearts but cannot yield Eggs.
The native cave structures accept Imbrifer biomes for all three elements;
ordinary roost placement and cave spacing remain unchanged.

Dragonforge Bricks, Cores and Apertures use the three elemental Nether-table
patterns. Bricks combine Scale Blocks, Enriched Gloomslate Bricks and Bedrock
Dust Blocks. Cores use Resonarium Plates, Mithril Weave, Dragon Bone Blocks
and the matching Heart. Apertures use Soundproof Glass, Earth Core Shard Blocks
and Carbon Fibre Strands. Blood production consumes Dragon's Breath with the
matching Scale, retaining the 750-tick duration.

Resonarium Plates also assemble from two Resonarium, Turtle Scute and Armadillo
Scute. A Plate and Refined Glowstone yield two Lite in the Foundry. Eight
Gloomslate Bricks around Lite craft eight Enriched Bricks, which drop themselves.

Forge III upgrades a Tier II forge using Corrupted Arcane Crystal as its main
ingredient and one each of the three Dragonsteel Ingots, Radiant Steel, Heart
of the Deep, Evil Spine, Evil Stone and Giant Anvil on its eight pedestals.
It consumes 3000 Aureal, 15000 Blood, 50 Souls and 1350 Ink.

The Refined Storage Controller requires Forge III, one each of the six Infused
Netherite Ingots and two Silicon Wafers. One Wafer is the main ingredient and
the other seven items occupy pedestals. This ritual has no extra essence cost.
Its original crafting recipe is disabled; recoloring still requires a Controller.

Deep Foundations branches from Imbrifer to Dragon Egg, and from the Otherside
through Resonarium, Plate, Lite, Enriched Bricks, Dragonforge Bricks and a full
set of parts. Forge III follows the parts and Forge II; the Controller follows
Forge III. The parts task requires 17 Bricks, eight Dragon Bone Blocks, one
Core and one Aperture, accepting elemental alternatives.

Ender Metal uses three Pearls, two Platinum, two Refined Obsidian and Hollow
Crystal in 500 mB Water for four Ingots. Impact Pile uses seven Netherite,
three Cursed Gold and three Large Obsidiansteel Slates on the Nether Table.
Raw Ironwood has its revised raw-metal craft and 16-item Colossal Iron bulk
craft; alloying one with Biosteel yields two Ironwood in the Foundry.

AnvilCraft's raw-metal Superheating multipliers and equipment-crushing recovery
recipes are disabled. Ordinary smelting, material heating and non-equipment
crushing retain their roles. Mekanism's casing retains the name Steel Casing.

## Crystal alchemy and collision tiers

Seven Magitech Crystals consume 2000 mB of their matching Potion. The six
elemental crystals retain their base inputs and add Infused Netherite, two
matching materials and one block. Hollow uses its seven selected solid inputs;
the Crucible has eight slots. Each corresponding Potion converts Mana Potion
1:1 and consumes its own selected items without requiring a Crystal.

The Spirit Altar produces Grand Lava Sponge from eight Nether Depths Lava
Sponges, Aerial/Infernal/Aqueous Spirits, Magma Blocks, Magma Cream, Buckets and
Solar Cores. The existing Pastel dry and wet variants retain their identities.

Anvil Collision uses vanilla/Royal at T1, Ember at T2 and Transcendence at T3,
with higher tiers accepted by lower minimums. Recipes that consume a specific
anvil retain that material requirement. Other anvil systems retain their tags.
