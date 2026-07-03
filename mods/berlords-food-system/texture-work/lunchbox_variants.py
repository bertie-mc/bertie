"""Lunchbox texture candidates — 3 box silhouettes x 4 colorways = 12 options.

PREVIEW ONLY. Nothing here is copied into the mod assets; berlord picks a
design+colorway by number, then we promote the winner into make_textures.py's
lunchbox_static() and rebuild.

The three silhouettes are recreations (16x16 pixel art) of berlord's three
reference box renders:
  domed   - tall rounded dark lid + small front clasp (the "stone chest" one)
  handle  - flat lid + tall metal carry-arch + latch (the "red-tab steel box")
  slotted - flat lid with a dark recessed slot/vent band + handle + latch
Each is authored as a grid of ZONE CODES; a colorway maps zone -> RGB, so one
template recolors cleanly into all four palettes.
"""
import os
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "out")

# ----------------------------------------------------------------- zone codes
# .  transparent          o  outline
# h  body highlight (L)   b  body mid        s  body shadow (R/bottom/seam)
# H  lid  highlight       l  lid  mid        k  lid shadow      L  lid dark/recess
# m  handle metal light   n  handle metal dark
# a  accent bright        d  accent dark

L_, R_, T_, B_ = 2, 13, 5, 14          # box outline extents (shared frame)


def base_box():
    """Common front-facing metal box body shared by every design."""
    P = {}
    # body interior
    for y in range(T_ + 1, B_):
        for x in range(L_ + 1, R_):
            P[(x, y)] = "b"
    # lid band (rows 6-8) sits on the upper body
    for y in range(T_ + 1, 9):
        for x in range(L_ + 1, R_):
            P[(x, y)] = "l"
    # lid top + left highlight, lid right shadow
    for x in range(L_ + 1, R_):
        P[(x, T_ + 1)] = "H"
    for y in range(T_ + 1, 9):
        P[(L_ + 1, y)] = "H"
        P[(R_ - 1, y)] = "k"
    # body left highlight, right + bottom shadow
    for y in range(9, B_):
        P[(L_ + 1, y)] = "h"
        P[(R_ - 1, y)] = "s"
    for x in range(L_ + 1, R_):
        P[(x, B_ - 1)] = "s"
    # lid/base seam
    for x in range(L_ + 1, R_):
        P[(x, 9)] = "k"
    # outline border with clipped corners
    for x in range(L_, R_ + 1):
        P[(x, T_)] = "o"
        P[(x, B_)] = "o"
    for y in range(T_, B_ + 1):
        P[(L_, y)] = "o"
        P[(R_, y)] = "o"
    for c in [(L_, T_), (R_, T_), (L_, B_), (R_, B_)]:
        P.pop(c, None)
    return P


def handle_arch(P):
    """Tall steel carry handle arching above the lid."""
    for x in (6, 7, 8, 9):
        P[(x, 2)] = "n"
    P[(6, 2)] = "m"
    P[(5, 3)] = "n"; P[(10, 3)] = "n"
    P[(5, 4)] = "m"; P[(10, 4)] = "m"


def latch(P):
    """Accent clasp straddling the lid/base seam, front center."""
    P[(7, 8)] = "a"; P[(8, 8)] = "a"
    P[(7, 9)] = "a"; P[(8, 9)] = "a"
    P[(7, 10)] = "d"; P[(8, 10)] = "d"


# ---- design 1: domed dark lid + small front clasp, low knob (no tall arch) ----
def design_domed():
    P = base_box()
    # recolor the whole lid band dark (domed shell)
    for y in range(T_ + 1, 9):
        for x in range(L_ + 1, R_):
            P[(x, y)] = "L"
    for x in range(L_ + 1, R_):
        P[(x, T_ + 1)] = "k"          # subtle top sheen on the dark dome
    for y in range(T_ + 1, 9):
        P[(L_ + 1, y)] = "k"
    # round the upper corners so the lid reads as a dome
    P[(L_ + 1, T_ + 1)] = "o"; P[(R_ - 1, T_ + 1)] = "o"
    # low knob centered on the dome
    P[(7, T_)] = "n"; P[(8, T_)] = "n"
    P[(7, 4)] = "m"
    # round front clasp/keyhole on the body, lower-center
    P[(8, 11)] = "a"; P[(8, 12)] = "d"
    P[(7, 11)] = "d"
    return P


# ---- design 2: flat lid + tall carry handle + latch (classic lunchbox) ----
def design_handle():
    P = base_box()
    handle_arch(P)
    latch(P)
    return P


# ---- design 3: flat lid with a dark recessed slot band + handle + latch ----
def design_slotted():
    P = base_box()
    handle_arch(P)
    # dark recessed slot across the lid
    for x in range(L_ + 2, R_ - 1):
        P[(x, 7)] = "L"
        P[(x, 8)] = "k"
    P[(L_ + 1, 7)] = "k"; P[(R_ - 1, 7)] = "L"
    latch(P)
    return P


DESIGNS = {
    "domed": design_domed(),
    "handle": design_handle(),
    "slotted": design_slotted(),
}

# ------------------------------------------------------------------ colorways
STEEL = {"m": (170, 174, 182), "n": (86, 90, 100)}   # handle metal, all palettes

COLORWAYS = {
    "blue": {  # matches the current shipped BFS lunchbox
        "o": (30, 36, 52),
        "h": (116, 166, 220), "b": (76, 122, 178), "s": (48, 80, 122),
        "H": (134, 180, 228), "l": (98, 144, 200), "k": (60, 96, 150), "L": (40, 64, 104),
        "a": (220, 188, 78), "d": (156, 124, 44),
    },
    "red": {
        "o": (56, 22, 24),
        "h": (214, 96, 84), "b": (176, 58, 52), "s": (120, 36, 36),
        "H": (224, 120, 108), "l": (198, 78, 70), "k": (150, 48, 44), "L": (104, 30, 30),
        "a": (236, 206, 128), "d": (176, 142, 70),
    },
    "green": {
        "o": (26, 46, 26),
        "h": (120, 182, 96), "b": (78, 140, 64), "s": (46, 96, 42),
        "H": (140, 196, 114), "l": (98, 160, 80), "k": (58, 112, 50), "L": (36, 78, 34),
        "a": (224, 190, 84), "d": (158, 126, 46),
    },
    "amber": {
        "o": (54, 40, 20),
        "h": (214, 176, 108), "b": (180, 140, 78), "s": (128, 96, 48),
        "H": (224, 190, 128), "l": (198, 158, 92), "k": (150, 112, 56), "L": (104, 76, 36),
        "a": (120, 150, 210), "d": (70, 96, 150),   # steel-blue clasp for contrast
    },
}


def render(zonemap, colorway):
    pal = {**STEEL, **colorway}
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for (x, y), z in zonemap.items():
        if 0 <= x < 16 and 0 <= y < 16 and z in pal:
            img.putpixel((x, y), (*pal[z], 255))
    return img


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    designs = list(DESIGNS.items())
    colors = list(COLORWAYS.items())

    CELL, GAP, LX, TY = 96, 12, 78, 26
    W = LX + len(colors) * CELL + (len(colors) - 1) * GAP + 10
    H = TY + len(designs) * CELL + (len(designs) - 1) * GAP + 10
    sheet = Image.new("RGBA", (W, H), (40, 40, 40, 255))
    draw = ImageDraw.Draw(sheet)

    # column headers (colorway names)
    for ci, (cname, _) in enumerate(colors):
        x = LX + ci * (CELL + GAP)
        draw.text((x + 4, 8), cname, fill=(230, 230, 230, 255))
    # row headers (design names) + cells
    for ri, (dname, zmap) in enumerate(designs):
        y = TY + ri * (CELL + GAP)
        draw.text((6, y + CELL // 2 - 4), dname, fill=(230, 230, 230, 255))
        for ci, (cname, cw) in enumerate(colors):
            x = LX + ci * (CELL + GAP)
            img = render(zmap, cw)
            img.save(os.path.join(OUT, f"lunchbox_{dname}_{cname}.png"))
            sheet.paste(img.resize((CELL, CELL), Image.NEAREST), (x, y))

    sheet.save(os.path.join(OUT, "_lunchbox_variants.png"))
    print(f"wrote {len(designs) * len(colors)} variants + _lunchbox_variants.png")
