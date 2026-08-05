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

`abyssal_core_hunter.png` is the exception: **it is a hand export and will not
track its generator.** Its generator lives outside this repository in the
workspace's gitignored `custom-textures/` directory. Nothing here regenerates
the file, so if the abyssal core is redrawn, re-export it or drop it. The same
gap applies to the *shipped* `abyssal_core.png`, which unlike storm, desert and
weeping_eye has no generator in `texture-work/` at all.

| File | Generator | What it is |
| --- | --- | --- |
| `storm_core_v1.png` | `../make_storm_core.py` | The storm core with the alternate flat-fill cloud: mid-grey body, lighter blocks scattered through the upper half, darker underside, one edge colour. Shares its silhouette, bolt, rain and animation with the shipped v2 — only the grey differs. 24 frames, one tick each. |
| `desert_core_still.png` | `../make_desert_core.py` | The desert core standing, weather only: the pyramid on its horizon in a weak sandstorm, nothing else happening. 16 frames, two ticks each. Same scene, silhouette and storm as the shipped strip; it is that strip's first phase on its own. |
| `desert_core_debris.png` | `../make_desert_core.py` | The full destroy-and-rebuild strip with broken stone left scattered over the rhomboid the base stands on — it fades up as the pyramid comes apart and thins back out as the courses go down. Identical to the shipped strip in every other respect; one flag apart (`build_cycle(field=True)`). 144 frames, one tick each. |
| `abyssal_core_hunter.png` | none — see the note above | The abyssal core that stays inside its own slot: the same whirlpool, drain, splatter and hunting tentacle as the shipped sprite, at 16x16 with no model transform. The shipped one is 48x48 and overflows into the neighbouring inventory slots via a `gui` display scale of 3; this is the fallback if that turns out to be a nuisance in a full inventory, and it needs no model beyond the ordinary generated item. 32 frames, two ticks each. |

## Using one

Copy the `.png` and its `.mcmeta` into
`src/main/resources/assets/bertieprogression/textures/item/` under the item's
name, then register the item as usual. Better still, point its generator at
`TEX_ITEM` in `OUTPUTS` so it keeps being regenerated in place.
