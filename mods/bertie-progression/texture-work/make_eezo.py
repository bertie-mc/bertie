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
ITEM = {
    "1": (0xEE, 0xEE, 0xF2),
    "2": (0xCB, 0xCB, 0xD3),
    "3": (0xA8, 0xA8, 0xB1),
    "4": (0x88, 0x88, 0x91),
    "5": (0x6B, 0x6B, 0x74),
    "6": (0x51, 0x51, 0x59),
    "7": (0x3A, 0x3A, 0x42),
    "8": (0x22, 0x22, 0x29),
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
RAW_EEZO = ring(parse([
    "................",
    "...1112222......",
    "..111122233.....",
    ".11112223344....",
    ".11P12233445....",
    ".1PQ12233455....",
    ".11PQ2334556....",
    ".1122334556.....",
    ".1223345667.....",
    "..23445611223...",
    "...456671PQ23...",
    ".....66773455...",
    "......774456....",
    ".......5667.....",
    "................",
    "................",
]))

# A triangular prism at an ingot's angle, its triangular end towards the camera. The left end
# is tall and the body recedes up and to the right, so the near cap reads as a face rather
# than as the end of a bar. Three bands - bright top, mid cap, dark right - because it is the
# STEP between facets that reads as volume. The violet is the core rod in section.
EEZO_INGOT = ring(parse([
    "................",
    "................",
    "................",
    ".........22222..",
    ".......11222222.",
    ".....1111222222.",
    "...33112222667..",
    "..34411222667...",
    ".3PQ44226677....",
    ".3PQ4466577.....",
    ".344466577......",
    "..4466577.......",
    "...66577........",
    "................",
    "................",
    "................",
]))


def main():
    ASSETS.joinpath("textures/block").mkdir(parents=True, exist_ok=True)
    ASSETS.joinpath("textures/item").mkdir(parents=True, exist_ok=True)
    to_image(ore_rows()).save(ASSETS / "textures/block/eezo_ore.png")
    to_image(RAW_EEZO).save(ASSETS / "textures/item/raw_eezo.png")
    to_image(EEZO_INGOT).save(ASSETS / "textures/item/eezo_ingot.png")
    print("wrote eezo_ore, raw_eezo and eezo_ingot under " + str(ASSETS))


if __name__ == "__main__":
    main()
