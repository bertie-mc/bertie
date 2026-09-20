# Apple texture revision

The three item sprites follow Minecraft Java 1.21.1's vanilla apple outline,
including its stalk and top cleft, with no leaf. The zombie and breed apples keep
the exact opaque silhouette; the skeleton apple adds transparent interior gaps.

- Zombie: olive-green flesh, restrained brown bruises, pale upper-left highlights,
  and one connected upper-right patch using vanilla rotten flesh's rust, brown,
  and tan material colors
- Breed: four colored spiral arms curling outward, with darker perimeter colors
- Skeleton: ivory ribs joined to a central bone, with three pairs of curved open
  air gaps. The design is drawn on a 16x16 grid and enlarged to 32x32; only eight
  individual pixels soften rib bends. This keeps the coarse Minecraft appearance.
  Light comes from the upper left: pale upper faces, beige side planes, and connected
  shadows beneath the ribs and along the sternum's right side. Three bone tones
  replace mottled patches; no noise, corrosion speckles, or block-averaged shading.
  Dark contours define the outer bone edge and the inner air gaps.
  The stem matches the seven vanilla stem pixels exactly, each enlarged to a
  uniform 2x2 block

The skeleton concept uses the Bone Heart item from Born in Chaos by mongoose_artist
as a visual reference for the central bone and open ribs. The reference was inspected
from the user's installed 1.21.1 1.7.5 JAR at
`assets/born_in_chaos_v1/textures/item/boneheart.png`. Its pixels are not bundled.
Project: https://www.curseforge.com/minecraft/mc-mods/born-in-chaos

The silhouette reference was inspected from the Minecraft 1.21.1 client JAR at
`assets/minecraft/textures/item/apple.png`. The zombie patch's material reference
is `assets/minecraft/textures/item/rotten_flesh.png` from the same JAR. Neither
reference image is bundled. Minecraft artwork remains Mojang's
property; its referenced outline is outside this component's public-domain dedication.

Built-in ImageGen supplied revised concepts. `apple-prompts-v6.json` records the
current zombie/skeleton prompts; `apple-prompts-v2.json` records the breed prompt.
`build-apple-textures.ps1`
and `build-skeleton-apple.ps1` preserve the final pixels on their intended grids;
the shipped files are the PNGs under `assets/betterhorses/textures/item/`.
