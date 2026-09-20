# Berlord's Better Horses

NeoForge 1.21.1 mod, installed on the client and server. Adds equipment for ordinary
horses; donkey, mule, llama, skeleton horse, and zombie horse inventories are unchanged.

## Equipment

Sneak-use a tamed horse, or open its inventory while riding, to access saddle,
armor, and horseshoe slots. Shift-click equips and removes the matching item.
One horseshoe item equips all four hooves. Equipment saves with the horse and
synchronizes to other players.

The horse preview keeps the same size and position with every saddle or no saddle.
The reserved area on the right stays blank unless the traveller saddle is equipped.

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
snow. Iron and gold shoes do not prevent sinking. Hold Shift
over the item to expand the Waterwalking explanation.

Each material colors the hooves' bottom pixel row. The two middle pixels on the
rear face remain the horse's original grey, leaving the horseshoe open at the back.

Netherite horse armor uses vanilla netherite armor attributes and JerryLu086's
MIT-licensed Simple Netherite Horse Armor textures. The 16x16 item has a two-pixel
muzzle correction; the 64x64 horse atlas is unchanged and uses Minecraft's existing horse armor model.
It does not make the horse fireproof. See [NOTICE](NOTICE) for attribution and license.

## Saddles

- **Passenger's Saddle:** two players can ride together; the first rider steers.
  The second player mounts by using the occupied horse. Dismounting the front
  rider transfers control to the remaining rider. The saddle cannot be removed
  through its menu while both seats are occupied.
- **Warrior's Saddle:** redirects 80% of damage after the horse's armor reductions
  to the controlling rider. The rider's normal defenses apply. If the rider rejects
  the damage (for example during invulnerability), the horse retains that damage.
  Without a rider, the horse takes normal damage.
- **Traveller's Saddle:** adds fifteen storage slots in a 5x3 grid to the right of
  the horse preview. Cargo saves with the horse, survives effigy capture, and
  drops on death. Empty the storage before removing or replacing the saddle.
  Equipping it shows the slots immediately in an already-open horse inventory.
  Its model has green canvas, leather saddlebags with clasps, and a strapped bedroll.
  Pressing the bound jump key immediately triggers the horse's maximum jump,
  including any horseshoe bonus. Holding the key does not repeat the jump;
  release and press again after landing. Other saddles retain vanilla charging.
  The item description is "Extra Space, Perfect Jump". Its existing
  `betterhorses:wanderer_saddle` registry ID remains unchanged for saved worlds.

Server setting `warriorDamageTransfer` accepts 0.0–1.0 in the world's
`serverconfig/betterhorses-server.toml`. The default is `0.8`.

Shoes and saddles use a grey "When Equipped:" heading and Minecraft-blue stats.
Saddle descriptions are "2 Seats", "Lifelink", and "Extra Space, Perfect Jump".
Waterwalking and Lavawalking are gold. Holding Shift expands those abilities and
all three saddle descriptions, independently of the crouch binding. Each sentence
uses its own line, without a trailing period. Lifelink's explanation uses the configured damage-transfer percentage.

Saddle models share a textured leather/cloth/metal atlas, with separate raised seats,
straps, buckles, and hollow metal stirrups. The passenger model has two seats and two
stirrups per side; warrior and traveller models have one per side. A continuous
blanket replaces the overlapping passenger sections. The vanilla body saddle is
hidden for custom saddles, while its bridle and reins remain visible.

## Riding improvements

Ridden horses swim at 50% of their normal dry-ground movement speed, including
horseshoe and movement-attribute bonuses. They float without holding jump and keep
their riders in deep water. Waterwalking shoes still allow full-speed surface
walking. Swimming does not grant underwater breathing or lava protection.

A saddle stops idle wandering, including an already-started stroll. Horses can
still follow a lead or tempting food, breed, and flee danger. Removing the saddle
restores wandering. While ridden, tamed saddled horses no longer rear up and
interrupt steering. Vanilla 1.21.1 already limits actual rider-ejecting bucking to
untamed horses; taming and jumping behavior remain intact.

In first person, your horse and its equipment smoothly fade from 100% visible at
15 degrees down to 10% at 60 degrees, using the pitch shown in F3. Looking farther
down keeps 10% visibility. Third-person views, other horses, and inventory previews remain opaque.

While mounted, the XP bar appears normally and changes to the jump bar only while
holding jump. The traveller saddle always shows XP. Vanilla hunger remains in its
normal row with horse hearts above it. With Berlord's Food System installed, that
mod retains its own food-slot and mount-health layout.

Mining while riding a horse uses the same speed as standing on the ground. Tool,
enchantment, potion, and underwater mining modifiers still apply; the usual
airborne penalty returns when dismounted.

## Appearance apples

Feed a horse a **Zombie Apple** or **Skeleton Apple** to give it the corresponding
vanilla undead appearance. This is cosmetic: the entity stays an ordinary horse,
with its usual sounds, behavior, breeding, health, movement, owner, riders, lead,
equipment, and storage. Coat markings are hidden while the undead appearance is
active. The appearance saves with the horse, synchronizes to clients, and survives
effigy capture. Repeating the same appearance does not consume another apple.

An **enchanted golden apple** restores the normal appearance and retained coat and
markings. It also keeps vanilla healing, growth, temper, and breeding effects.

A **Breed Apple** chooses one of the other six vanilla coat colors uniformly.
It leaves markings and stats intact. On a cosmetic undead horse, the chosen coat
is retained underneath and becomes visible when the appearance is restored.
Creative-mode feeding does not consume these apples.

Apple descriptions use Minecraft's gray tooltip color. Breed Apple says
"Changes horse to a random breed"; Zombie and Skeleton Apples say "Zombify horse"
and "Bonify horse", each followed by "Notch Apple to cure" on a separate line.

## Horse effigy

Use an empty effigy on a living tamed horse to store it. A filled effigy glints;
use it on the top of a block with enough open space to release the horse. The
effigy becomes empty and can be reused. Capture and release operate on the server
and preserve the horse's UUID, owner, name, variant, health, attributes, and all
equipment. Creative mode also moves the horse rather than leaving a copy behind.

Leashed horses can be stored: capture detaches the leash and drops one lead at the
horse. Release does not reconnect it to the previous player or fence. Occupied and
riding horses cannot be stored. A blocked release leaves
the horse safely in the effigy. A matching UUID already loaded in any dimension
also prevents release.

## Crafting

Each slash separates a crafting-table row; `.` means an empty slot. Each recipe
produces one item.

| Item | Top / middle / bottom | Ingredients |
| --- | --- | --- |
| Horse effigy | `PP. / .P. / PPP` | `P`: any planks, including mixed wood types |
| Zombie Apple | `FFF / FAF / FFF` | `A`: apple; `F`: rotten flesh |
| Skeleton Apple | `BBB / BAB / BBB` | `A`: apple; `B`: bone |
| Breed Apple | `DDD / DAD / DDD` | `A`: apple; `D`: any dye, including mixed colors |
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

Most item sprites use a 16x16 grid. All horseshoes share one
shape with five-pixel arms and single-pixel nail holes. The saddle sprites use
solid leather, cloth, and metal clusters with no downscaling or partial alpha.
`tools/build-item-textures.ps1` regenerates these original item sprites;
`tools/build-entity-textures.ps1` regenerates the original hoof and empty-slot textures.
`tools/build-apple-textures.ps1` regenerates the three apple icons from their native
pixel layouts. Zombie and breed apples use 16x16 grids. The skeleton apple uses
32x32, built from a coarse 16x16 design with only eight finer contour pixels.
Its ribs have transparent air gaps. The zombie apple has an upper-right rotten
patch colored like vanilla rotten flesh.
Its upper-left lighting follows each rib and the central bone with connected highlight
and shadow bands, with darker contours around the outside and the rib gaps.
Its stalk uses the exact vanilla stem pixels as uniform 2x2 blocks.
All use vanilla's apple contour and stalk; the
breed apple has four outward-curling colored bands. Built-in ImageGen prompts are
in `tools/apple-prompts-v2.json` and `tools/apple-prompts-v6.json`;
`tools/apple-style-revision.md` records the vanilla and Born in Chaos Bone Heart references.
The new 128x128 shared saddle atlas was made with built-in image generation and
sampled onto its native grid; its material regions and prompt are documented in
`tools/saddle-materials.md`. Neither script changes the saddle atlas, imported armor,
or horse effigy. The empty horseshoe slot has a flat outline and four nail pixels
in the same grey as vanilla's empty saddle and armor icons.

Unit tests cover jump-height conversion and texture dimensions/hoof gaps.
GameTests cover persistence, effigy safety, inventory transfers, seating, damage,
fluid and powder-snow landings, cargo persistence/drop/transfer, full jump power,
saddled wandering/rearing, mounted mining with potion modifiers, and player/fence
lead capture with a delayed check against lead duplication.
Client tests exercise synchronized storage and key-press/release jump behavior,
measure swimming against dry-ground speed and ride out onto shore, check first-person
fading and item models, verify tooltip colors and Shift expansion with rebound crouch, and
capture the inventory and both sides of all three rendered saddles. Screenshots are in
`build/test-diagnostics/clienttest`.

The focused `*.appleRecipesAndAppearancePersistence` client check also exercises
server-side recipes (including mixed dyes), feeding, creative consumption, a different
coat on every use, cosmetic appearance synchronization, normal enchanted-apple feeding,
preserved equipment/identity, NBT, and effigy storage. It captures the actual
zombie/skeleton horse appearances. For texture-only iteration, the separate
`*.appleItemPreview` check renders the three apple icons without a test world.

For UI-only iteration, set `BERTIE_CLIENT_TEST_FILTER` to
`*.equipmentSyncMenuAndAppearance,*.equipmentTooltipsUseShift,*.mountedHudAndFade`.
This excludes swimming and other unrelated client tests.

Mixins target AbstractHorse equipment persistence, menus, rider positioning and
jump velocity, surface-only lava contact, and powder-snow collision. A client-only
input mixin replaces charging with an immediate jump for the traveller saddle.
Additional hooks cover mounted water movement, idle strolling, rearing, the mining
airborne check, and client render buffers for horse/equipment transparency.
Client rendering adds a layer to
the existing horse renderer. Other mods that replace horse menus/renderers or
change rider physics should be checked together with this mod.
