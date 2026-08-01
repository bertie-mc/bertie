# Kept texture variants

Finished sprites that nothing currently uses, held here because they may be
wanted later.

These are real Minecraft textures — vertical frame strips with their `.mcmeta`
beside them, drop-in ready. They sit under `texture-work/` rather than under
`src/main/resources/`, so they are versioned but **not packaged into the jar**.
Nothing in this folder ships.

Everything here is written by its generator on every run, not exported by hand.
That matters: a variant usually shares its geometry with the sprite that *is*
in use, so a one-off export would quietly go stale the next time that geometry
changed, and you would not find out until you tried to use it.

| File | Generator | What it is |
| --- | --- | --- |
| `storm_core_v1.png` | `../make_storm_core.py` | The storm core with the flat-fill cloud: mid-grey body, lighter blocks scattered through the upper half, darker underside, one edge colour. Closest to berlord's reference. Shares its silhouette, bolt, rain and animation with the shipped v2 — only the grey differs. 24 frames, one tick each. |

## Using one

Copy the `.png` and its `.mcmeta` into
`src/main/resources/assets/bertie_progression/textures/item/` under the item's
name, then register the item as usual. Better still, point its generator at
`TEX_ITEM` in `OUTPUTS` so it keeps being regenerated in place.
