# Bedrock Fog

The void fog Minecraft drew near the world floor before 1.8, back for 1.21.1 / NeoForge, and
switchable per dimension.

Client-side only. Nothing about the fog reaches the server, so it is safe on a vanilla-ish
multiplayer connection and a player may turn it off without desyncing.

## What it does

Below a configurable depth the terrain fog closes in and its colour is pulled towards black — the
same read as the Darkness effect, but tied to how deep you are rather than to a mob. The effect
ramps: nothing at the fade depth, full at bedrock level.

Depth is measured from the dimension's **own floor**, not from y=0, so a single setting behaves the
same in the overworld (floor -64) and in a dimension whose floor is 0.

## Config

`config/bedrockfog-client.toml`.

| Key | Default | Meaning |
| --- | --- | --- |
| `depth.enabled` | `true` | draw the fog at all |
| `depth.dimensions` | `["minecraft:overworld"]` | dimensions that get the fog; anything unlisted is untouched |
| `depth.fadeDepth` | `32` | blocks above the floor where the fog starts to show (overworld y=-32) |
| `depth.fullDepth` | `6` | blocks above the floor where it is at full strength (overworld y=-58) |
| `appearance.fogStart` | `0.0` | distance from the camera where the fog begins |
| `appearance.fogEnd` | `16.0` | distance where the fog is solid; lower is thicker |
| `appearance.darkness` | `0.9` | how far the fog colour is pulled to black; 1.0 is pitch black |

Head underwater, in lava or in powder snow keeps that fluid's own fog — the effect only touches the
open-air case.
