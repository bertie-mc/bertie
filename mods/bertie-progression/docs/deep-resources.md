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

One Carapace yields one Bone Shard on Crushing Wheels or the Pulveriser, or
two on the Mekanism Crusher. Crushing Wheels alone have a 5% Echo Shard bonus.
Reinforced Echo Shard and Warden Stick use their Nether Table patterns.
Foundry makes two Sculkium from one Reinforced Echo Shard and one Duratium.
Heart of the Deep is crafted on the Nether Table with the selected Echo/Core/
Bone Shards, Creaking Heart, Dragon Hearts and Heart of the Sea.

Deep Foundations extends Earth Core Shard through the Warden materials and
Heart to entering the Otherside. The separate Ruin branch unlocks Bedrock Dust
and entering Imbrifer. The Beacon branches also introduce the Giant Anvil and
rings, and the Ember Smithing Template and Anvil.

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
