# Saddle material atlas

`textures/entity/saddle_materials.png` is the shared 128x128 atlas used by
`HorseSaddleModel`. It was generated with Codex's built-in image generation on
2026-09-19 and resized by sampling the center of each destination pixel, with no
blurring. Geometry uses Minecraft model units and stays within each material cell.
The existing 16x16 inventory icons and imported armor textures are independent.

Each cell is 64x32 pixels:

| Row (V) | Left (U=0) | Right (U=64) |
| --- | --- | --- |
| 0 | Dark leather | Caramel leather |
| 32 | Cyan quilted cloth | Burgundy quilted cloth |
| 64 | Green canvas | Beige bedroll linen |
| 96 | Iron | Brass |

Generation prompt:

> Create ONE flat Minecraft pixel-art MATERIAL TEXTURE ATLAS, not a screenshot or
> 3D render. Exact composition is a 128 by 128 logical pixel image, enlarged
> uniformly to 1024x1024 with nearest-neighbor square pixel blocks. No perspective,
> lighting gradients, labels, margins or grid lines. Fully opaque. Eight rectangular
> material swatches in exactly TWO columns and FOUR rows, each 64x32 logical pixels.
> Row 1: dark chocolate horse-saddle leather / warm caramel horse-saddle leather.
> Row 2: rich muted cyan quilted saddle blanket cloth with small pale-gold stitch
> details / burgundy red quilted cloth with small pale-gold stitch details.
> Row 3: forest-green canvas with small tan stitch details / beige coarse bedroll
> linen. Row 4: grey iron with cool highlights / warm brass buckle metal with gold
> highlights. Vanilla Minecraft texture style: crisp chunky pixel clusters, low
> noise, 4–6 colors per swatch, detailed but readable at native size. Leather has
> angular scuffs, intermittent seams and tiny stitch pairs. Cloth has 4-pixel quilt
> or weave clusters and sparse stitch pairs. Metals have irregular short highlight
> bands, no rust. Cover each entire cell with no separating border. No objects,
> horse or saddle drawings, shadows, watermarks or text. Strong but restrained
> contrast so small 3D components have visible material texture.
