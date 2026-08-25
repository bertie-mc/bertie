# Void Fog

The fog Minecraft drew near the bottom of the world until 1.8 took it out, back for 1.21.1 /
NeoForge, and switchable per dimension.

Client-side only. Nothing about it reaches the server, so it is safe on a vanilla-ish multiplayer
connection and a player may turn it off without desyncing.

## What it does

Below a fade depth the fog closes in - terrain and sky alike, so a cave mouth does not cut a
bright edge through it - its colour is pulled towards black and pale motes drift through it. The
effect eases in rather than ramping: nothing at y=-54, half at the top of the bedrock layer, full on
the floor.

Two things soften it, both gradually:

- **Daylight nearby.** A column open to the sky within `sky.radius` holds the fog off, completely
  underneath and less the further in you walk. The falloff is quadratic in distance and the result
  is eased over about a second, so crossing under a shaft mouth is a fade, not a switch.
- **Suppression.** Anything registered through `VoidFogApi` can clear the fog for one player - see
  below.

Depth is measured from the dimension's **own floor**, not from y=0, so a single setting behaves the
same in the overworld (floor -64) and in a dimension whose floor is 0.

## Turning it off for a player

For an armour set, a trinket or an enchanted tool. Two ways in, and neither needs this mod as a
compile dependency:

- **`voidfog:suppresses_void_fog`** - an item tag. Wear or hold anything in it and the fog is gone.
  Pure data, no code.
- **`VoidFogApi.addSuppressor(player -> …)`** - returns 0 (no effect) to 1 (no fog), so a partial
  effect is possible. The strongest source wins; they do not stack.

The tag is item data and this mod is installed on clients only, so it resolves in single player but
will be empty against a dedicated server that does not have the mod. Registered suppressors run
either way.

## Config

`config/voidfog-client.toml`.

| Key | Default | Meaning |
| --- | --- | --- |
| `depth.enabled` | `true` | draw the fog at all |
| `depth.dimensions` | `["minecraft:overworld"]` | dimensions that get it; anything unlisted is untouched |
| `depth.fadeDepth` | `10` | blocks above the floor where it starts to show (overworld y=-54) |
| `depth.fullDepth` | `5` | blocks above the floor where it is at full strength, measured to the eye |
| `sky.radius` | `24` | how far an opening to the sky holds it off; `0` ignores daylight |
| `sky.scanInterval` | `10` | ticks between sky scans; the result is eased between them |
| `appearance.fogStart` | `0.0` | distance from the camera where the fog begins |
| `appearance.falloff` | `3.0` | how sharply the fog arrives; drives distance and darkness together |
| `appearance.fogEnd` | `8.0` | distance where it is solid; lower is thicker |
| `appearance.darkness` | `1.0` | how far the colour is pulled to black; 1.0 is pitch black |
| `particles.enabled` | `true` | draw the motes |
| `particles.perTick` | `6` | motes per tick at full strength, scaling down with the fog |
| `particles.radius` | `12` | how far from the camera motes may spawn |

Head underwater, in lava or in powder snow keeps that fluid's own fog - the effect only touches the
open-air case.
