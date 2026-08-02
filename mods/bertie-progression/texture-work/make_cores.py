#!/usr/bin/env python3
"""
bertieprogression texture generator — cursed_core (16x16), hand-placed.

The cores are the 7x7 mechanical-crafter walls that feed the Hephaestus Forge
tier 2 ritual, one per Cataclysm boss domain. They were a set of four glass
spheres, identical glass with a different essence trapped in each; cursed_core
is the last one still drawn that way. The glass and the essence bands are kept
here as they were, so the sphere is unchanged if it is ever wanted again:

    glass   a 14px sphere: a dark tinted silhouette ring, a shell ring that
            catches the light at the upper left and rim-lights at the lower
            right, and a three-pixel specular glint painted last, over the
            contents, because it sits on the front of the sphere
    essence cursed — a skull with lit sockets adrift in green miasma, filling
            the sphere on four depth bands, with a few off-palette flecks of
            crimson so a 10px interior still looks alive instead of like a
            flat tinted fill

storm_core left the set first. It is now a dark grey storm cloud pierced by a
bolt, drawn by texture-work/make_storm_core.py, and deliberately does not share
the glass — do not add it back here or that art gets overwritten the next time
this script runs.

desert_core has gone the same way and for the same reason: it is now an
animated pyramid on a desert horizon, a 144-frame strip with a .png.mcmeta
beside it, drawn by texture-work/make_desert_core.py. Nothing this script can
express. **Do not add it back.**

abyssal_core has gone the same way, and for the same reason. It is now an
animated whirlpool with a tentacle hunting out of it, and it is a 32x32 strip of
32 frames with a .png.mcmeta beside it — nothing this script can express. It was
still listed here after that art landed, which meant one run of this script
would have quietly replaced an animated sprite with a static glass sphere and
left the .mcmeta pointing at frames that no longer existed. Do not add it back.
Its generator lives outside the monorepo, in the workspace's custom-textures/.

No third-party art is copied; Cataclysm, Deepwaters, Malum and Slag ingredient
items were looked at for each domain's palette only. Nothing here needs a
NOTICE carve-out.

Run:  python texture-work/make_cores.py
"""
import os

from PIL import Image

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
TEX_ITEM = os.path.join(ROOT, "src", "main", "resources", "assets", "bertieprogression", "textures", "item")

SIZE = 16
C = 8.0          # sphere centre, in pixel-corner coordinates
R_OUT = 7.0      # silhouette          -> 14px sphere
R_GLASS = 6.0    # inside the outline  -> 1px silhouette ring
R_IN = 5.0       # the essence         -> 1px shell ring, 10px interior

# Light from the upper left, as a unit vector in screen space.
LIGHT = (-0.6, -0.8)

# The glint on the front of the sphere. Painted after the essence.
SPECULAR = [(4, 4), (5, 4), (4, 5)]


def hexcol(s):
    return tuple(int(s[i:i + 2], 16) for i in (1, 3, 5)) + (255,)


def disc(radius):
    """Pixels whose centre falls inside a circle of `radius` about C."""
    return {(x, y) for y in range(SIZE) for x in range(SIZE)
            if (x + 0.5 - C) ** 2 + (y + 0.5 - C) ** 2 <= radius * radius}


OUT = disc(R_OUT)
GLASS = disc(R_GLASS)
IN = disc(R_IN)
RING_EDGE = OUT - GLASS
RING_SHELL = GLASS - IN


def facing(x, y):
    """How squarely a pixel faces the light: +1 upper left, -1 lower right."""
    return ((x + 0.5 - C) * LIGHT[0] + (y + 0.5 - C) * LIGHT[1]) / R_OUT


# Per-core palettes. `edge`/`mid`/`hi`/`rim` are the glass itself; 0..3 is the
# essence ramp, darkest to brightest; the rest are that essence's own details.
CORES = {
    "cursed_core": {
        "edge": "#071B14", "mid": "#14513A", "hi": "#A8F7C6", "rim": "#2E9A64",
        "0": "#062218", "1": "#0C4128", "2": "#146B3E", "3": "#2FA55E",
        "B": "#E6DEC0", "H": "#FFFAE4", "D": "#A89B78",   # bone, lit and shaded
        "E": "#05130C",   # eye socket
        "G": "#5CFF9E",   # what is looking out of it
        "N": "#06170E",   # nose and mouth
        "m": "#A6FFB8",   # motes drifting in the miasma
        "v": "#C1264A",   # cursed flesh
    },
}

# The essence, as (row, first column, run) over the 10px interior. Painted in
# order, so the flecks at the end of each list land on top of the fill.
ESSENCE = {
    # A skull adrift in miasma, something still lit behind the sockets.
    "cursed_core": [
        (3,  6, "0000"),
        (4,  4, "01111110"),
        (5,  4, "11222211"),
        (6,  3, "1122222211"),
        (7,  3, "1122222211"),
        (8,  3, "1122222211"),
        (9,  3, "1122222211"),
        (10, 4, "11222211"),
        (11, 4, "01111110"),
        (12, 6, "0000"),
        # the skull
        (4,  6, "HHBB"),
        (5,  5, "HBBBBD"),
        (6,  4, "HBBBBBBD"),
        (7,  4, "BEEBBEEB"),
        (8,  4, "BEGBBGEB"),
        (9,  4, "BBBNNBBD"),
        (10, 5, "DBBBBD"),
        (11, 6, "BNNB"),
        # miasma wisps, motes, and flesh that did not burn off
        (6,  3, "3"),
        (9, 12, "3"),
        (3,  7, "m"),
        (11, 4, "m"),
        (5, 11, "m"),
        (7,  3, "v"),
        (8, 12, "v"),
    ],
}


def paint(name):
    pal = CORES[name]
    grid = {}

    for (x, y) in RING_EDGE:
        grid[(x, y)] = pal["rim"] if facing(x, y) < -0.72 else pal["edge"]

    # The shell stays a flat tint apart from the lower-right rim light. Giving
    # it a lit arc at the upper left too merges it with the specular and the
    # whole top of the sphere goes to one bright mass.
    for (x, y) in RING_SHELL:
        grid[(x, y)] = pal["rim"] if facing(x, y) < -0.45 else pal["mid"]

    for row, x0, run in ESSENCE[name]:
        for i, ch in enumerate(run):
            p = (x0 + i, row)
            if p in IN:
                grid[p] = pal[ch]

    for p in SPECULAR:
        grid[p] = pal["hi"]

    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()
    for (x, y), col in grid.items():
        px[x, y] = hexcol(col)
    return img


if __name__ == "__main__":
    os.makedirs(TEX_ITEM, exist_ok=True)
    for name in CORES:
        paint(name).save(os.path.join(TEX_ITEM, name + ".png"))
        print("wrote " + name + ".png")
