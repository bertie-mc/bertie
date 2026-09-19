# Berlord's Better Horses

NeoForge 1.21.1 mod, installed on the client and server. Adds equipment for ordinary
horses; donkey, mule, llama, skeleton horse, and zombie horse inventories are unchanged.

## Equipment

Sneak-use a tamed horse, or open its inventory while riding, to access saddle,
armor, and horseshoe slots. Shift-click equips and removes the matching item.
One horseshoe item equips all four hooves. Equipment saves with the horse and
synchronizes to other players.

| Horseshoes | Speed (blocks/s) | Extra safe fall | Extra jump height | Extra step height | Surface walking |
| --- | ---: | ---: | ---: | ---: | --- |
| Iron | 1 | 4 | 0 | 0 | — |
| Gold | 2 | 8 | 1 | 0 | — |
| Diamond | 3 | 12 | 2 | 1 | Water |
| Netherite | 4 | 16 | 3 | 2 | Water and lava |

Bonuses are additive. Speed is calibrated for full-forward riding on ordinary dry
ground; terrain, potion effects, and other modifiers still affect movement. Jump
bonuses add height through Minecraft's gravity/drag trajectory rather than adding
raw velocity. Fall and step bonuses use the corresponding entity attributes.

Netherite shoes protect against magma blocks and campfires. They do **not** grant
fire resistance: fire blocks, burning, and being submerged in lava still hurt.
Lava protection applies at the walkable surface. Surface walking does not raise a
horse that is already submerged.

Waterwalking also lets diamond- and netherite-shod horses stand and land on powder
snow. Iron and gold shoes do not prevent sinking. The item tooltip keeps this
behavior implicit in waterwalking.

Each material colors the hooves' bottom pixel row. The two middle pixels on the
rear face remain the horse's original grey, leaving the horseshoe open at the back.

Netherite horse armor uses vanilla netherite armor attributes and JerryLu086's
MIT-licensed Simple Netherite Horse Armor textures. The imported 16x16 item and
64x64 horse atlas are unchanged and use Minecraft's existing horse armor model.
It does not make the horse fireproof. See [NOTICE](NOTICE) for attribution and license.

## Saddles

- **Passenger saddle:** two players can ride together; the first rider steers.
  The second player mounts by using the occupied horse. Dismounting the front
  rider transfers control to the remaining rider. The saddle cannot be removed
  through its menu while both seats are occupied.
- **Warrior saddle:** redirects 80% of damage after the horse's armor reductions
  to the controlling rider. The rider's normal defenses apply. If the rider rejects
  the damage (for example during invulnerability), the horse retains that damage.
  Without a rider, the horse takes normal damage.
- **Traveller saddle:** adds fifteen storage slots in a 5x3 grid to the right of
  the horse preview. Cargo saves with the horse, survives effigy capture, and
  drops on death. Empty the storage before removing or replacing the saddle.
  Equipping it shows the slots immediately in an already-open horse inventory.
  Its green cloth, saddlebags, and bedroll are unchanged.
  Pressing the bound jump key immediately triggers the horse's maximum jump,
  including any horseshoe bonus. Holding the key does not repeat the jump;
  release and press again after landing. Other saddles retain vanilla charging.
  The item description is "Extra space, perfect jump". Its existing
  `betterhorses:wanderer_saddle` registry ID remains unchanged for saved worlds.

Server setting `warriorDamageTransfer` accepts 0.0–1.0 in the world's
`serverconfig/betterhorses-server.toml`. The default is `0.8`.

## Horse effigy

Use an empty effigy on a living tamed horse to store it. A filled effigy glints;
use it on the top of a block with enough open space to release the horse. The
effigy becomes empty and can be reused. Capture and release operate on the server
and preserve the horse's UUID, owner, name, variant, health, attributes, and all
equipment. Creative mode also moves the horse rather than leaving a copy behind.

Occupied, leashed, and riding horses cannot be stored. A blocked release leaves
the horse safely in the effigy. A matching UUID already loaded in any dimension
also prevents release.

## Crafting

Each slash separates a crafting-table row; `.` means an empty slot. Each recipe
produces one item.

| Item | Top / middle / bottom | Ingredients |
| --- | --- | --- |
| Horse effigy | `PP. / .P. / PPP` | `P`: any planks, including mixed wood types |
| Iron, gold, diamond horseshoes | `M.M / M.M / .M.` | `M`: iron ingot, gold ingot, or diamond, respectively |
| Passenger (double) saddle | `S.S / CCC / H.H` | `S`: saddle; `C`: cyan wool; `H`: tripwire hook |
| Warrior saddle | `DSW / RRR / HAH` | `D`: shield; `S`: saddle; `W`: iron sword; `R`: red wool; `H`: tripwire hook; `A`: iron horse armor |
| Traveller saddle | `.SC / GGG / H.H` | `S`: saddle; `C`: chest; `G`: green wool; `H`: tripwire hook |

Netherite horseshoes and horse armor use the smithing table: netherite upgrade
template + diamond version + netherite ingot. The effigy recipe unlocks when the
player obtains planks.

All items also appear in the Tools & Utilities creative tab. Item IDs use the
`betterhorses` namespace.

## Development

From the repository's development environment (or native Windows Gradle 8/JDK 21):

```text
gradle :mods:berlords-better-horses:build
gradle :mods:berlords-better-horses:runGameTests
gradle :mods:berlords-better-horses:runClientTests
```

Item sprites are cleaned up on a native 16x16 grid. All horseshoes share one
shape with five-pixel arms and single-pixel nail holes. The saddle sprites use
solid leather, cloth, and metal clusters with no downscaling or partial alpha.
`tools/build-item-textures.ps1` regenerates these original item sprites;
`tools/build-entity-textures.ps1` regenerates the original hoof and saddle atlases.
Neither script changes the imported armor or the horse effigy.

Unit tests cover jump-height conversion and texture dimensions/hoof gaps.
GameTests cover persistence, effigy safety, inventory transfers, seating, damage,
fluid and powder-snow landings, cargo persistence/drop/transfer, and full jump power.
Client tests exercise synchronized storage and key-press/release jump behavior,
check item models, and capture the inventory and all three rendered saddles. Screenshots are in
`build/test-diagnostics/clienttest`.

Mixins target AbstractHorse equipment persistence, menus, rider positioning and
jump velocity, surface-only lava contact, and powder-snow collision. A client-only
input mixin replaces charging with an immediate jump for the traveller saddle.
Client rendering adds a layer to
the existing horse renderer. Other mods that replace horse menus/renderers or
change rider physics should be checked together with this mod.
