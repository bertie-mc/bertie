# Apple texture revision

The three item sprites follow Minecraft Java 1.21.1's vanilla apple outline,
including its stalk and top cleft, with no leaf. The zombie and breed apples keep
the exact opaque silhouette; the skeleton apple adds transparent interior gaps.

- Zombie: olive-green flesh, restrained brown bruises, pale upper-left highlights
- Breed: four colored spiral arms curling outward, with darker perimeter colors
- Skeleton: ivory ribs joined to a central bone, with three pairs of curved open
  air gaps. The 32x32 grid allows finer curves while retaining broad color clusters.
  Light comes from the upper left: pale upper faces, beige side planes, and connected
  shadows beneath the ribs and along the sternum's right side. Three bone tones
  replace mottled patches; no noise, corrosion speckles, or block-averaged shading.
  Dark one-pixel contours define the outer bone edge and every inner air gap.
  The stem matches the seven vanilla stem pixels exactly, each enlarged to a
  uniform 2x2 block; finer details are reserved for the bone curves and contours

The skeleton concept uses the Bone Heart item from Born in Chaos by mongoose_artist
as a visual reference for the central bone and open ribs. The reference was inspected
from the user's installed 1.21.1 1.7.5 JAR at
`assets/born_in_chaos_v1/textures/item/boneheart.png`. Its pixels are not bundled.
Project: https://www.curseforge.com/minecraft/mc-mods/born-in-chaos

The silhouette reference was inspected from the Minecraft 1.21.1 client JAR at
`assets/minecraft/textures/item/apple.png`. Minecraft artwork remains Mojang's
property; its referenced outline is outside this component's public-domain dedication.

Built-in ImageGen supplied revised concepts. `apple-prompts-v2.json` and
`skeleton-apple-prompt-v5.json` record the current prompts. `build-apple-textures.ps1`
and `build-skeleton-apple.ps1` preserve the final pixels on their intended grids;
the shipped files are the PNGs under `assets/betterhorses/textures/item/`.
