#!/usr/bin/env python3
"""
bertieprogression texture generator — eezo_ore (block), raw_eezo, eezo_ingot (16x16).

The brief: an ore that reads as bedrock at a glance, a raw drop and an ingot in
the same palette. It is meant to be walked past, so the base has to be bedrock,
not a bedrock-ish grey.

    palette   the five greys are vanilla bedrock's own — #222222 #333333 #575757
              #636363 #979797 — read off the texture with a histogram. Nothing
              is copied: the arrangement, both silhouettes and all the shading
              are drawn here. Matching the palette is the whole point; a
              hand-picked set of greys reads as "some dark stone" instead.

    the base  bedrock is not square noise, it is short HORIZONTAL runs, and that
              streak is what the eye recognises. So the generator scatters
              2-4 x 1 rectangles (occasionally x2), wrapping at the edges so the
              block tiles, and the colour weights and rectangle count are tuned
              until the result's histogram matches bedrock's own
              (d 88 / m 59 / h 49 / l 42 / k 18 out of 256).

    the seed  chosen, not arbitrary. Random seeds band: a run of dark rows at
              the top or bottom of the tile becomes a visible seam once the
              block repeats. 243 is the seed whose per-row and per-column mean
              luminance vary least, so the 2x2 tiling has no stripe in it.

    the eezo  five small violet veins, hand-placed rather than scattered, each
              built dark -> mid -> bright so it reads as something embedded and
              lit rather than as dirt. Four to five specks is the most the base
              takes before the block stops reading as bedrock.

    the items two angular lumps for the raw drop, the way vanilla raw ores are a
              big mass and a small one rather than a ball; a standard ingot bar
              for the ingot, but with the vein split across its top face, which
              is the "weird" in the brief and also the only thing that says at a
              glance which ingot this is.

Run from anywhere; the output path is resolved from this file.
"""
import random
from pathlib import Path
from PIL import Image

ASSETS = Path(__file__).resolve().parent.parent / "src/main/resources/assets/bertieprogression"

PALETTE = {
    "k": (0x22, 0x22, 0x22, 255),
    "d": (0x33, 0x33, 0x33, 255),
    "m": (0x57, 0x57, 0x57, 255),
    "l": (0x63, 0x63, 0x63, 255),
    "h": (0x97, 0x97, 0x97, 255),
    "v": (0x2E, 0x24, 0x4A, 255),
    "V": (0x4A, 0x37, 0x84, 255),
    "G": (0x7A, 0x5F, 0xC8, 255),
    ".": (0, 0, 0, 0),
}

BEDROCK_SEED = 243
RECTANGLES = 140
WEIGHTS = (("d", 24), ("m", 27), ("h", 22), ("l", 19), ("k", 8))

# Five veins in the block, dark -> mid -> bright. (x, y, colour)
VEINS = [
    (2, 2, "v"), (3, 2, "V"), (4, 2, "G"), (2, 3, "v"), (3, 3, "V"), (4, 3, "v"),
    (11, 4, "v"), (12, 4, "V"), (13, 4, "v"), (11, 5, "v"), (12, 5, "G"), (13, 5, "V"),
    (6, 9, "v"), (7, 9, "V"), (8, 9, "v"), (6, 10, "v"), (7, 10, "G"), (8, 10, "V"), (7, 11, "v"),
    (13, 12, "v"), (12, 13, "v"), (13, 13, "V"),
    (0, 6, "v"), (0, 7, "v"), (1, 7, "V"),
]

RAW_EEZO = [
    "................",
    "..kkkkk.........",
    ".khhhhlkk.......",
    ".khhhllhhk......",
    ".khhVGVhllk.....",
    ".khhVGVhlllk....",
    ".klhVGhlmmlk....",
    ".kllhVllmmmkk...",
    ".kllmmmmmmkhhk..",
    ".kdlmmmmmkkhhhk.",
    "..kdmmmmkkhVGhk.",
    "..kddmmmk.kVGmk.",
    "...kddmk..kmmmk.",
    "....kkk...kmmdk.",
    "..........kkddk.",
    "...........kkk..",
]

EEZO_INGOT = [
    "................",
    "................",
    "................",
    ".........kkkkk..",
    "......kkkhhhVGk.",
    "...kkkhhhhhVGhk.",
    "..khhhhhhVGVhlk.",
    "..khhhhVGVhlllk.",
    "..kllVGVlllmmdk.",
    "..kdlVvmmmmmddk.",
    "..kddmmmmmdddk..",
    "...kdddddddkk...",
    "....kkkkkkk.....",
    "................",
    "................",
    "................",
]


def from_grid(rows):
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    pixels = image.load()
    for y, row in enumerate(rows):
        if len(row) != 16:
            raise ValueError(f"row {y} is {len(row)} wide, not 16")
        for x, char in enumerate(row):
            pixels[x, y] = PALETTE[char]
    return image


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


def main():
    grid = bedrock_base(BEDROCK_SEED)
    for x, y, colour in VEINS:
        grid[y][x] = colour

    (ASSETS / "textures/block").mkdir(parents=True, exist_ok=True)
    (ASSETS / "textures/item").mkdir(parents=True, exist_ok=True)
    from_grid(["".join(row) for row in grid]).save(ASSETS / "textures/block/eezo_ore.png")
    from_grid(RAW_EEZO).save(ASSETS / "textures/item/raw_eezo.png")
    from_grid(EEZO_INGOT).save(ASSETS / "textures/item/eezo_ingot.png")
    print(f"wrote eezo_ore, raw_eezo and eezo_ingot under {ASSETS}")


if __name__ == "__main__":
    main()
