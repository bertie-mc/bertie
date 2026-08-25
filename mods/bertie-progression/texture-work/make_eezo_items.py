"""Draw the ship-ready 16x16 Eezo item textures.

The palette deliberately follows eezo_ore.png: coarse bedrock grays with small,
dark-purple Eezo inclusions.  Every coordinate is authored at native resolution;
there is no resampling or antialiasing.
"""

from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
ITEMS = ROOT / "src/main/resources/assets/bertieprogression/textures/item"

TRANSPARENT = (0, 0, 0, 0)
PALETTE = {
    ".": TRANSPARENT,
    "K": (34, 34, 34, 255),      # outline / deepest ore shadow
    "D": (51, 51, 51, 255),      # dark bedrock from eezo_ore
    "S": (69, 69, 69, 255),      # shadow transition
    "M": (87, 87, 87, 255),      # bedrock midtone from eezo_ore
    "G": (105, 105, 105, 255),   # light midtone
    "L": (124, 124, 124, 255),   # light transition
    "H": (151, 151, 151, 255),   # highlight from eezo_ore
    "W": (178, 178, 178, 255),   # raw-mineral specular highlight
    "X": (198, 198, 198, 255),   # polished edge highlight
    "P": (38, 34, 54, 255),      # deep Eezo purple from eezo_ore
    "Q": (51, 44, 78, 255),      # lit Eezo purple from eezo_ore
    "V": (70, 58, 101, 255),     # purple highlight
}


RAW_EEZO = (
    "................",
    "..DDDDD.........",
    ".DGHHLGDDD......",
    ".DHWHWHLHGDDD...",
    "DLWWWHWHWHHLGD..",
    "DLGLWVQHLHLGLGD.",
    "KLMMLQPMSSMGMGD.",
    "KGSMGMMMSDDSMMD.",
    "KGSSGMMSDDDDDSK.",
    "KMSSSDDDDHVLMMK.",
    "KMMSSDDDLLQPGMSK",
    ".KMSSSKKMLLGSDDK",
    "..KKKK..KMMMSDDK",
    ".........KMMDDK.",
    "..........KKKK..",
    "................",
)


# Exact silhouette and per-pixel value structure of AnvilCraft's Titanium Ingot.
# Only its palette is translated; no Eezo shading geometry is improvised.
TITANIUM_STRUCTURE = (
    "................",
    "................",
    "..........011...",
    "......00002001..",
    "..100063321114..",
    "..157722001114..",
    ".17832220000114.",
    ".19783222200014.",
    "1559833322220014",
    ".159833333220204",
    "..19783333332444",
    "...1953666444...",
    "....156444......",
    ".....44.........",
    "................",
    "................",
)


TITANIUM_TO_EEZO = {
    ".": ".",
    "0": "M", "1": "S", "2": "G", "3": "L", "4": "D",
    "5": "W", "6": "H", "7": "W", "8": "X", "9": "W",
}


# One-pixel Eezo seam following the long axis of the Titanium right face.
EEZO_ROD = {
    (11, 6): "P", (12, 6): "P", (13, 6): "P",
    (8, 7): "V", (9, 7): "Q", (10, 7): "P",
    (5, 8): "Q", (6, 8): "Q", (7, 8): "P",
}


EEZO_ROD_THICK_MIDDLE = {
    **EEZO_ROD,
    (8, 8): "Q", (9, 8): "V", (9, 6): "Q", (10, 6): "Q",
}


def draw_raw_eezo() -> Image.Image:
    """Two overlapping raw-ore lobes with one shared silhouette."""
    image = Image.new("RGBA", (16, 16), TRANSPARENT)
    pixels = image.load()
    for y, row in enumerate(RAW_EEZO):
        if len(row) != 16:
            raise ValueError(f"raw Eezo row {y} has {len(row)} pixels")
        for x, symbol in enumerate(row):
            pixels[x, y] = PALETTE[symbol]
    return image


def draw_eezo_ingot(rod_pixels=EEZO_ROD_THICK_MIDDLE) -> Image.Image:
    """AnvilCraft Titanium Ingot geometry with an Eezo material palette."""
    image = Image.new("RGBA", (16, 16), TRANSPARENT)
    pixels = image.load()
    for y, row in enumerate(TITANIUM_STRUCTURE):
        if len(row) != 16:
            raise ValueError(f"Titanium Ingot row {y} has {len(row)} pixels")
        for x, source_symbol in enumerate(row):
            pixels[x, y] = PALETTE[TITANIUM_TO_EEZO[source_symbol]]

    for coordinate, symbol in rod_pixels.items():
        pixels[coordinate] = PALETTE[symbol]

    return image


def main() -> None:
    ITEMS.mkdir(parents=True, exist_ok=True)
    draw_raw_eezo().save(ITEMS / "raw_eezo.png", optimize=False)
    # The thick-middle rod is the one that shipped; the thinner EEZO_ROD is kept above
    # because it is the same seam one pixel narrower and worth re-comparing, but it is no
    # longer written out - an unreferenced second sprite in the assets folder is just a
    # thing for someone to wire up by mistake.
    draw_eezo_ingot().save(ITEMS / "eezo_ingot.png", optimize=False)


if __name__ == "__main__":
    main()
