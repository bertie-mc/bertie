# Inventory icon correction

Built-in OpenAI ImageGen supplied the 2026-09-20 edit concept. Final cleanup stays
on the native 16x16 grid so no resampling changes the existing armor colors.

Prompt: preserve the netherite horse armor sprite except move the two lowest
muzzle pixels at (13,8) and (14,8), using zero-based coordinates, one pixel up,
replacing (13,7) and (14,7). Clear their previous positions. Match the vanilla
horse armor silhouette. For the empty horseshoe slot, use the iron horseshoe's
U shape in neutral grays, with five-pixel arms and small dark nail holes, on
transparent background. No additional decoration or smoothing.

The slot was subsequently simplified to a one-pixel outline and four nail pixels,
all in vanilla's empty equipment icon color, `#7c7c7c`, with transparent interiors.
`build-entity-textures.ps1` records its final pixels. The armor edit copies the two original pixel colors without
altering any other position. See NOTICE for the upstream armor attribution.

Final assets:

- [Empty horseshoe slot](../src/main/resources/assets/betterhorses/textures/gui/horseshoe_slot.png)
- [Netherite horse armor item](../src/main/resources/assets/betterhorses/textures/item/netherite_horse_armor.png)
