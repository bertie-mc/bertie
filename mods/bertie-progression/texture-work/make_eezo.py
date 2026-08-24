#!/usr/bin/env python3
"""
bertieprogression texture generator - eezo_ore (block), raw_eezo, eezo_ingot (16x16).

    the ore       is vanilla bedrock with about a tenth of its pixels moved and a few dark
                  violet specks added. Generating a bedrock-ALIKE was tried and does not
                  work: match bedrock's palette and histogram exactly and the result still
                  reads as deepslate, because what the eye recognises in bedrock is the
                  particular pattern, not its statistics. So the pattern below is bedrock's
                  own, read out of the client jar, and the swaps keep the same palette and
                  the same colour counts - the block differs from bedrock in arrangement,
                  never in tone. See NOTICE: this one is a derivative asset.

    the swaps     26 of 256 pixels, chosen by a fixed seed, each replaced with a DIFFERENT
                  bedrock colour drawn from bedrock's own histogram. Ten per cent is enough
                  to break the eye's lock on a memorised pattern and not enough to read as a
                  different block.

    the items     hand-drawn, pixel by pixel. An earlier pass generated them from masks and
                  a lighting function; it produced a clean gradient and lost everything that
                  matters - the facets, the connected masses, the silhouette. Sprites this
                  small are drawn, not computed.

    item ramp     eight greys, near-white to near-black, the range vanilla raw ores use
                  (raw_iron runs #FEF4ED down to #3B3429). Bedrock's five greys cannot carry
                  a volume; these can.

    the violet    kept small and dark. It marks the ore, it is not the subject: about a
                  tenth of each mass, in proportion, so the big lump and the little one read
                  as the same material.

Run from anywhere. The bedrock source is found in the moddev artifacts under BERTIE_ROOT.
"""
import glob
import io
import os
import random
import zipfile
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve()
ASSETS = HERE.parent.parent / "src/main/resources/assets/bertieprogression"

# Vanilla bedrock's five greys, keyed by the letters used in the maps below.
BEDROCK = {
    "k": (0x22, 0x22, 0x22),
    "d": (0x33, 0x33, 0x33),
    "m": (0x57, 0x57, 0x57),
    "l": (0x63, 0x63, 0x63),
    "h": (0x97, 0x97, 0x97),
}

# The violets that mark the ore. Dark enough to sit inside bedrock's own brightness range;
# a brighter speck turns a block meant to be missed into a beacon.
ORE_VIOLET = {
    "v": (0x26, 0x22, 0x36),
    "V": (0x33, 0x2C, 0x4E),
}

# Eight tones for the items, plus three violets and an outline.
# Pulled down from the first attempt at these: eezo comes out of a bedrock seam, and a ramp
# topping out near white read as polished steel. The highlight is now a light grey, not a
# white, and the body sits in the lower half.
ITEM = {
    "1": (0xC4, 0xC4, 0xCC),
    "2": (0xA6, 0xA6, 0xAF),
    "3": (0x8B, 0x8B, 0x95),
    "4": (0x72, 0x72, 0x7B),
    "5": (0x5C, 0x5C, 0x65),
    "6": (0x48, 0x48, 0x50),
    "7": (0x35, 0x35, 0x3C),
    "8": (0x1F, 0x1F, 0x25),
    "p": (0x2C, 0x24, 0x4C),
    "P": (0x45, 0x37, 0x77),
    "Q": (0x6A, 0x57, 0xA8),
}

PALETTE = {}
PALETTE.update(BEDROCK)
PALETTE.update(ORE_VIOLET)
PALETTE.update(ITEM)

SWAP_SEED = 90210
SWAP_FRACTION = 0.10

# Dark violet specks, added after the swaps. Placed on the pattern, not scattered at random:
# three small marks, none of them touching a bright pixel, so nothing glints.
SPECKS = [
    (3, 2, "v"), (4, 2, "V"), (4, 3, "v"),
    (11, 7, "V"), (12, 7, "v"),
    (7, 12, "v"), (8, 12, "V"), (8, 13, "v"),
]


def inside(polygon, px, py):
    """Point-in-convex-polygon: every edge has to keep the point on the same side."""
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


def face(polygon, tone):
    return [
        [tone if inside(polygon, x + 0.5, y + 0.5) else "." for x in range(16)]
        for y in range(16)
    ]


def over(base, top):
    return [
        [top[y][x] if top[y][x] != "." else base[y][x] for x in range(16)]
        for y in range(16)
    ]


def ring(rows):
    """
    Rings a hand-drawn shape with the outline tone.

    The maps below carry SHADING only. Hand-placing outline pixels as well is what went
    wrong the last time these were drawn: a stray 8 inside the body reads as a hole, and
    two lumps that meet each get their own edge, which splits them down the middle. Here
    the boundary is computed, so it is always exactly the silhouette and never inside it.
    """
    out = [row[:] for row in rows]
    for y in range(16):
        for x in range(16):
            if rows[y][x] == ".":
                continue
            if any(
                not (0 <= x + dx < 16 and 0 <= y + dy < 16) or rows[y + dy][x + dx] == "."
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))
            ):
                out[y][x] = "8"
    return out


def parse(rows):
    if len(rows) != 16 or any(len(row) != 16 for row in rows):
        bad = [i for i, row in enumerate(rows) if len(row) != 16]
        raise ValueError(f"maps must be 16x16; rows {bad} are wrong, got {len(rows)} rows")
    return [list(row) for row in rows]


def to_image(rows):
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    pixels = image.load()
    for y, row in enumerate(rows):
        for x, cell in enumerate(row):
            if cell == ".":
                pixels[x, y] = (0, 0, 0, 0)
            else:
                r, g, b = PALETTE[cell]
                pixels[x, y] = (r, g, b, 255)
    return image


# ----------------------------------------------------------------------------- the ore

def bedrock_pattern():
    """Reads vanilla bedrock out of the client jar and returns it as palette letters."""
    root = os.environ.get("BERTIE_ROOT") or str(HERE.parents[3])
    jars = sorted(glob.glob(os.path.join(root, "mods", "*", "build", "moddev", "artifacts", "*client-extra*.jar")))
    if not jars:
        raise SystemExit(
            "no client-extra jar under " + root + " - build any mod once so moddev unpacks it"
        )
    with zipfile.ZipFile(jars[0]) as jar:
        image = Image.open(io.BytesIO(jar.read("assets/minecraft/textures/block/bedrock.png"))).convert("RGB")
    lookup = {value: key for key, value in BEDROCK.items()}
    return [[lookup[image.getpixel((x, y))] for x in range(16)] for y in range(16)]


def ore_rows():
    grid = bedrock_pattern()

    # Bedrock's own colour counts, so a swap changes the pattern and not the tone balance.
    counts = {}
    for row in grid:
        for cell in row:
            counts[cell] = counts.get(cell, 0) + 1
    bag = [colour for colour, count in sorted(counts.items()) for _ in range(count)]

    rng = random.Random(SWAP_SEED)
    positions = [(x, y) for y in range(16) for x in range(16)]
    rng.shuffle(positions)
    for x, y in positions[: round(256 * SWAP_FRACTION)]:
        choices = [colour for colour in bag if colour != grid[y][x]]
        grid[y][x] = rng.choice(choices)

    for x, y, colour in SPECKS:
        grid[y][x] = colour
    return grid


# --------------------------------------------------------------------------- the items
#
# Lit from the upper left. 1 is the highlight, 8 the outline; p/P/Q are the violet.

# Fat, the way vanilla raw ores are: a big mass with a smaller lobe growing out of its
# lower right, overlapping rather than sitting beside it. A thin shape is all boundary once
# it is ringed, and what is left reads as wire.
# Fat, the way vanilla raw ores are: a big mass with a smaller lobe growing out of its lower
# right, overlapping rather than sitting beside it. A thin shape is all boundary once it is
# ringed, and what is left reads as wire.
#
# The dark end of the ramp lives INSIDE the silhouette, not along its edge. Shading down to
# 6 at the rim and letting ring() overwrite it leaves a body of nothing but tones 1-3, which
# is precisely how the last pass ended up with no gradient at all.
# Dented and patchy on purpose. A raw ore is a piece broken out of rock: vanilla's all have
# notched outlines and cavities, and a smooth blob with one clean left-to-right gradient
# reads as a pebble. The edges step in and out, and the shading has light spots inside the
# dark half and hollows inside the lit half rather than one uniform sweep.
# The gradient that worked, with noise and dents added to it rather than a redraw. Two things
# a raw ore needs that a clean blob does not: an outline that steps in and out instead of
# running in long unbroken lines, and shading that is patchy - a hollow inside the lit half, a
# catch of light inside the shadow - rather than one even sweep across the piece.
#
# Dents are cheap to overdo. Every notch turns its neighbours into boundary, and boundary
# becomes outline, so a shape this size takes about four before the interior starts to
# disappear under its own edge.
RAW_EEZO = ring(parse([
    "................",
    "...1112222......",
    "..112122334.....",
    ".11132223344....",
    ".11P12243455....",
    ".1PQ12233455....",
    ".11PQ2354456....",
    ".1132334556.....",
    ".1223345667.....",
    "..23445611223...",
    "...456674PQ23...",
    ".....66573455...",
    "......774456....",
    ".......5667.....",
    "................",
    "................",
]))


PRISM_YAW = -55.0
PRISM_PITCH = -20.0
PRISM_LENGTH = 2.4
PRISM_SCALE = 8.5


def eezo_ingot_rows():
    """
    A triangular prism, projected rather than drawn.

    Four hand-drawn attempts at this came out as a fang, because a prism at an angle is not a
    silhouette anyone eyeballs correctly on a 16x16 grid - the ridge and the end cap have to
    agree with each other or the eye refuses to read it as a solid. So the real thing is built
    in three dimensions, turned to face the camera down its axis, and orthographically
    projected. Back-faces drop out on their own, which leaves exactly what should be visible:
    the triangular end and the roof running back from it.

    The two faces are then given tones by hand rather than by the light angle. Lambert put
    them within one step of each other and the crease disappeared; a solid needs the STEP.
    """
    import math

    apex, left, right = (0.5, 0.95), (0.0, 0.0), (1.0, 0.0)
    near = [(0.0, y, z) for y, z in (apex, left, right)]
    far = [(PRISM_LENGTH, y, z) for y, z in (apex, left, right)]
    points = near + far
    centre = [sum(p[i] for p in points) / 6.0 for i in range(3)]
    points = [tuple(p[i] - centre[i] for i in range(3)) for p in points]

    yaw, pitch = math.radians(PRISM_YAW), math.radians(PRISM_PITCH)

    def turn(p):
        x, y, z = p
        x, y = x * math.cos(yaw) - y * math.sin(yaw), x * math.sin(yaw) + y * math.cos(yaw)
        y, z = y * math.cos(pitch) - z * math.sin(pitch), y * math.sin(pitch) + z * math.cos(pitch)
        return x, y, z

    turned = [turn(p) for p in points]

    def flat(p):
        return 8.0 + p[0] * PRISM_SCALE, 8.6 - p[2] * PRISM_SCALE

    # (vertices, tone). The camera looks along +y, so a face whose normal has a negative y
    # component is pointing at it; everything else is the far side and never drawn.
    plan = (([0, 1, 2], "2"), ([3, 5, 4], "2"), ([0, 3, 4, 1], "4"), ([0, 2, 5, 3], "5"), ([1, 4, 5, 2], "6"))
    drawn = []
    for indices, tone in plan:
        a, b, c = (turned[i] for i in indices[:3])
        u = [b[i] - a[i] for i in range(3)]
        v = [c[i] - a[i] for i in range(3)]
        normal_y = u[2] * v[0] - u[0] * v[2]
        if normal_y >= 0.0:
            continue
        depth = sum(turned[i][1] for i in indices) / len(indices)
        drawn.append((depth, [flat(turned[i]) for i in indices], tone, tuple(indices)))
    drawn.sort(key=lambda item: -item[0])

    rows = [["." for _ in range(16)] for _ in range(16)]
    for _, polygon, tone, _ in drawn:
        for y in range(16):
            for x in range(16):
                if inside(polygon, x + 0.5, y + 0.5):
                    rows[y][x] = tone

    # The core rod, in section, on the TRIANGULAR END - the whole point of showing that face.
    # Taking the nearest polygon instead put it on the roof, where it reads as a stain.
    cap = next((poly for _, poly, _, idx in drawn if idx == (0, 1, 2)), None)
    if cap:
        cx = sum(p[0] for p in cap) / len(cap)
        cy = sum(p[1] for p in cap) / len(cap)
        for dx, dy, tone in ((0, 0, "Q"), (1, 0, "P"), (0, 1, "P"), (1, 1, "p")):
            x, y = int(cx) + dx, int(cy) + dy
            if 0 <= x < 16 and 0 <= y < 16 and rows[y][x] == "2":
                rows[y][x] = tone
    return rows


EEZO_INGOT = ring(eezo_ingot_rows())


def main():
    ASSETS.joinpath("textures/block").mkdir(parents=True, exist_ok=True)
    ASSETS.joinpath("textures/item").mkdir(parents=True, exist_ok=True)
    to_image(ore_rows()).save(ASSETS / "textures/block/eezo_ore.png")
    to_image(RAW_EEZO).save(ASSETS / "textures/item/raw_eezo.png")
    to_image(EEZO_INGOT).save(ASSETS / "textures/item/eezo_ingot.png")
    print("wrote eezo_ore, raw_eezo and eezo_ingot under " + str(ASSETS))


if __name__ == "__main__":
    main()
