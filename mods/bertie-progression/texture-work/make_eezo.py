#!/usr/bin/env python3
"""
bertieprogression texture generator - eezo_ore (block), raw_eezo, eezo_ingot (16x16).

The brief: an ore that reads as bedrock at a glance, a raw drop and an ingot in the
same family. The ore is meant to be walked past, so its base has to be bedrock, not
a bedrock-ish grey; the two items are held in the hand and inspected, so they get a
wider ramp than bedrock's five greys could ever give.

    ore palette   the five greys are vanilla bedrock's own - #222222 #333333 #575757
                  #636363 #979797 - read off that texture with a histogram. Nothing is
                  copied: the arrangement is generated here. Matching the palette is the
                  whole point; hand-picked greys read as "some dark stone" instead.

    the base      bedrock is not square noise, it is short HORIZONTAL runs, and that
                  streak is what the eye recognises. So the generator scatters 2-4 x 1
                  rectangles (occasionally x2), wrapping at the edges so the block tiles,
                  with weights and a count tuned until the histogram matches bedrock's
                  own (d 88 / m 59 / h 49 / l 42 / k 18 out of 256).

    the seed      chosen, not arbitrary. Random seeds band: a run of dark rows at the top
                  or bottom becomes a visible seam once the block repeats. 243 is the seed
                  whose per-row and per-column mean luminance vary least.

    the eezo      EIGHT pixels, in three clusters, in violets dark enough to sit inside
                  bedrock's own brightness range rather than on top of it. This is the
                  whole design: the ore has to be genuinely missable, so it differs from
                  bedrock in about four per cent of its pixels and never in brightness.

    item ramp     vanilla raw ores run eight tones from near-white to near-black
                  (raw_iron: #FEF4ED down to #3B3429). Five greys cannot carry that, and a
                  raw drop shaded in five reads flat however good the silhouette is - which
                  is exactly what happened to the first pass. So the items get their own
                  eight-step grey and a four-step violet.

    the shading   the two items are drawn as MASKS, not as pixel maps: an outline of '#'
                  is shaded by where each pixel sits along the light direction, so the
                  gradient is continuous rather than banded, and the silhouette stays easy
                  to redraw. Edge pixels take the darkest tone, which is what lets a 16x16
                  item read against a bright inventory slot.

    the items     the raw drop is a big mass and a small one, the way vanilla raw ores are,
                  each veined in proportion. The ingot is a triangular prism angled like a
                  normal ingot, its triangular end towards the camera, and the violet on
                  that end is the core rod in section.

Run from anywhere; the output path is resolved from this file.
"""
import random
from pathlib import Path

from PIL import Image

ASSETS = Path(__file__).resolve().parent.parent / "src/main/resources/assets/bertieprogression"

# --- the ore: bedrock's own five greys, plus violets that hide inside their range ---
ORE_PALETTE = {
    "k": (0x22, 0x22, 0x22),
    "d": (0x33, 0x33, 0x33),
    "m": (0x57, 0x57, 0x57),
    "l": (0x63, 0x63, 0x63),
    "h": (0x97, 0x97, 0x97),
    "v": (0x26, 0x23, 0x33),
    "V": (0x33, 0x2D, 0x4A),
    "G": (0x42, 0x39, 0x5E),
}

BEDROCK_SEED = 243
RECTANGLES = 140
WEIGHTS = (("d", 24), ("m", 27), ("h", 22), ("l", 19), ("k", 8))

# Eight pixels, three clusters. Any more, or any brighter, and the block stops reading as
# bedrock - which is the one thing this texture must not do.
VEINS = [
    (3, 2, "v"), (4, 2, "V"), (4, 3, "G"),
    (11, 8, "V"), (12, 8, "G"), (12, 9, "v"),
    (6, 12, "v"), (7, 12, "V"),
]

# --- the items: eight greys and four violets, lit from the upper left ---
GREY = [
    (0xF0, 0xF0, 0xF4),
    (0xCF, 0xCF, 0xD6),
    (0xAB, 0xAB, 0xB3),
    (0x8A, 0x8A, 0x92),
    (0x6C, 0x6C, 0x74),
    (0x52, 0x52, 0x5A),
    (0x3B, 0x3B, 0x43),
    (0x24, 0x24, 0x2B),
]
VIOLET = [
    (0x7B, 0x66, 0xC0),
    (0x54, 0x42, 0x92),
    (0x38, 0x2B, 0x63),
    (0x23, 0x1B, 0x40),
]

TRANSPARENT = (0, 0, 0, 0)


def boundary(mask, x, y):
    return any(
        not (0 <= x + dx < 16 and 0 <= y + dy < 16) or mask[y + dy][x + dx] != "#"
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))
    )


def blank():
    return [[None] * 16 for _ in range(16)]


def to_image(grid):
    image = Image.new("RGBA", (16, 16), TRANSPARENT)
    pixels = image.load()
    for y in range(16):
        for x in range(16):
            cell = grid[y][x]
            pixels[x, y] = TRANSPARENT if cell is None else (cell[0], cell[1], cell[2], 255)
    return image


def parse(rows):
    if len(rows) != 16 or any(len(row) != 16 for row in rows):
        raise ValueError("every map must be 16 rows of 16")
    return [list(row) for row in rows]


# ----------------------------------------------------------------------------- the ore

def bedrock_base(seed):
    """Short horizontal runs, wrapped at the edges so the block tiles."""
    rng = random.Random(seed)
    grid = [["d"] * 16 for _ in range(16)]
    bag = [colour for colour, weight in WEIGHTS for _ in range(weight)]
    for _ in range(RECTANGLES):
        colour = rng.choice(bag)
        width = rng.randint(2, 4)
        height = 2 if rng.random() < 0.18 else 1
        x0, y0 = rng.randrange(16), rng.randrange(16)
        for dy in range(height):
            for dx in range(width):
                grid[(y0 + dy) % 16][(x0 + dx) % 16] = colour
    return grid


def ore_image():
    grid = bedrock_base(BEDROCK_SEED)
    for x, y, colour in VEINS:
        grid[y][x] = colour
    return to_image([[ORE_PALETTE[cell] for cell in row] for row in grid])


# --------------------------------------------------------------------------- the items

def shade(mask, ramp, light=(-0.72, -0.69), spread=1.0, base=0.0, seed=0, weight=0.72, rim=None):
    """
    Shades a '#' mask along the light direction.

    Every solid pixel is placed on the ramp by how far it sits up-light from the shape's
    centre, so the gradient is continuous rather than banded; a little seeded jitter keeps
    it from looking airbrushed. Keep that jitter small - past about a tenth it stops reading
    as grain and starts reading as noise, and a 16x16 sprite has no room to absorb it.

    A {@code rim} colour outlines the shape all the way round, the way every vanilla item
    sprite is outlined - that ring is the only reason a 16x16 item reads against the grey of
    an inventory slot. It is passed separately from the ramp so the BODY never reaches it:
    letting the gradient run down to the darkest tone is what turned the shadow half of the
    first raw drop into a hole. Faces of a solid pass no rim, because their shared edges are
    creases rather than silhouette, and get outlined together afterwards.
    """
    solid = [(x, y) for y in range(16) for x in range(16) if mask[y][x] == "#"]
    if not solid:
        return blank()
    cx = sum(x for x, _ in solid) / len(solid)
    cy = sum(y for _, y in solid) / len(solid)
    reach = max(
        max(abs(x - cx) for x, _ in solid),
        max(abs(y - cy) for _, y in solid),
    ) or 1.0

    rng = random.Random(seed)
    jitter = {point: rng.uniform(-0.11, 0.11) for point in solid}
    last = len(ramp) - 1
    edge = rim
    grid = blank()
    for x, y in solid:
        # lit is +1 fully up-light and -1 fully away; map it onto the ramp.
        lit = ((x - cx) * light[0] + (y - cy) * light[1]) / reach
        t = base + (0.5 - lit * 0.5 * spread) + jitter[(x, y)] * 0.5
        # A power under 1 pushes the whole shape down the ramp. Without it the lit half lands
        # on the top two tones and the item reads as a white blob; vanilla spends most of a
        # raw ore in the middle and only a dozen pixels on the highlight.
        t = max(0.0, min(1.0, t)) ** weight
        index = max(0, min(last, round(t * last)))

        if edge is not None and boundary(mask, x, y):
            grid[y][x] = edge
            continue
        grid[y][x] = ramp[index]
    return grid


def overlay_veins(grid, mask, veins, seed=0):
    """Replaces pixels with the violet ramp, shaded alike so the veins sit IN the metal."""
    violet = shade(mask, VIOLET[:3], base=0.15, weight=0.8, seed=seed)
    for y in range(16):
        for x in range(16):
            if veins[y][x] == "#" and grid[y][x] is not None and violet[y][x] is not None:
                grid[y][x] = violet[y][x]
    return grid




# Two masses, the way vanilla raw ores are composed - and shaded SEPARATELY. One light
# falloff across both puts the small mass entirely on the shadow side of the shared centre,
# which turns it into a black smudge; vanilla gives each lump its own highlight.
RAW_BIG = parse([
    "................",
    "...#####........",
    "..########......",
    ".##########.....",
    ".###########....",
    ".###########....",
    ".##########.....",
    ".#########......",
    "..#######.......",
    "..######........",
    "...####.........",
    "................",
    "................",
    "................",
    "................",
    "................",
])

RAW_SMALL = parse([
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "..........##....",
    ".........#####..",
    "........######..",
    "........######..",
    "........#####...",
    ".........###....",
    "................",
    "................",
])

RAW_BIG_VEINS = parse([
    "................",
    "................",
    "................",
    "...###..........",
    "...####.........",
    "....###.........",
    "....##..........",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
])

RAW_SMALL_VEINS = parse([
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "..........##....",
    "..........##....",
    "..........#.....",
    "................",
    "................",
    "................",
])

CORE_ROD = parse([
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    ".....##.........",
    ".....##.........",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
])


def inside(polygon, px, py):
    sign = None
    for index in range(len(polygon)):
        ax, ay = polygon[index]
        bx, by = polygon[(index + 1) % len(polygon)]
        cross = (bx - ax) * (py - ay) - (by - ay) * (px - ax)
        if abs(cross) < 1e-9:
            continue
        if sign is None:
            sign = cross > 0
        elif (cross > 0) != sign:
            return False
    return True


def fill(polygon):
    return [
        ["#" if inside(polygon, x + 0.5, y + 0.5) else "." for x in range(16)]
        for y in range(16)
    ]


def union(*masks):
    return [
        ["#" if any(m[y][x] == "#" for m in masks) else "." for x in range(16)]
        for y in range(16)
    ]


def compose(*layers):
    grid = blank()
    for layer in layers:
        for y in range(16):
            for x in range(16):
                if layer[y][x] is not None:
                    grid[y][x] = layer[y][x]
    return grid


def outline(grid, mask, ramp):
    """Rings the whole silhouette once the faces inside it are drawn."""
    for y in range(16):
        for x in range(16):
            if mask[y][x] == "#" and boundary(mask, x, y):
                grid[y][x] = ramp[-1]
    return grid


def raw_eezo():
    big = overlay_veins(
        shade(RAW_BIG, GREY[:5], spread=1.1, base=-0.06, weight=0.9, seed=7, rim=GREY[7]),
        RAW_BIG,
        RAW_BIG_VEINS,
        seed=7,
    )
    small = overlay_veins(
        shade(RAW_SMALL, GREY[:5], spread=1.1, base=0.04, weight=0.9, seed=11, rim=GREY[7]),
        RAW_SMALL,
        RAW_SMALL_VEINS,
        seed=11,
    )
    return to_image(compose(big, small))


def triangular_prism():
    """
    A prism lying the way an ingot lies - long axis low-left to high-right - turned so the
    triangular end faces the camera.

    Two triangles, the near cap and the far one offset up and to the right, plus the two long
    faces joining them.
    """
    near = ((5, 4), (1, 12), (11, 12))
    offset = (4, -3)
    far = tuple((x + offset[0], y + offset[1]) for x, y in near)
    cap = fill(near)
    top = fill((near[0], far[0], far[1], near[1]))
    right = fill((near[0], far[0], far[2], near[2]))
    return cap, top, right


def eezo_ingot():
    """
    Each face is nearly ONE tone: it is the step between faces that says which way each one
    points, and running a full ramp across every face sweeps those steps away until the prism
    reads as a lump. The outline goes on afterwards, around the whole silhouette, so the
    creases between faces survive instead of every face being outlined into darkness.
    """
    cap, top, right = triangular_prism()
    faces = (
        (right, 3, 5, 3),
        (top, 0, 2, 4),
        (cap, 1, 3, 5),
    )
    grid = compose(
        *(shade(mask, GREY[lo:hi], spread=0.3, weight=1.0, seed=seed) for mask, lo, hi, seed in faces)
    )
    grid = overlay_veins(grid, cap, CORE_ROD, seed=6)
    return to_image(outline(grid, union(cap, top, right), GREY))


def main():
    (ASSETS / "textures/block").mkdir(parents=True, exist_ok=True)
    (ASSETS / "textures/item").mkdir(parents=True, exist_ok=True)
    ore_image().save(ASSETS / "textures/block/eezo_ore.png")
    raw_eezo().save(ASSETS / "textures/item/raw_eezo.png")
    eezo_ingot().save(ASSETS / "textures/item/eezo_ingot.png")
    print("wrote eezo_ore, raw_eezo and eezo_ingot under " + str(ASSETS))


if __name__ == "__main__":
    main()
