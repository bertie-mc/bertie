"""Metal lunchbox with a carry handle and two suitcase snap latches (STATIC 16x16).

Same geometry/authoring style as lunchbox_static() in make_textures.py.
Cool steel palette; closure = two brass toggle/draw latches (keeper tab on
the lid + hinged lever on the body, straddling the seam), like the snaps on a
hard suitcase. No padlock, no ribbon.
Writes out/lunchbox_metal.png + a big out/_preview_lunchbox_metal.png.
Does NOT copy into mod assets (kept as a preview until wired in).
Run:  python lunchbox_metal.py
"""
import os
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "out")
os.makedirs(OUT, exist_ok=True)

# ---- steel palette ------------------------------------------------------
OUTL = (34, 38, 46, 255)     # dark steel outline
BODY = (120, 132, 146, 255)  # steel body
HI   = (172, 184, 198, 255)  # left/top highlight
SH   = (78, 88, 102, 255)    # right/bottom shadow + seam
LID  = (138, 150, 164, 255)  # lid band (a touch lighter than body)
LIDH = (192, 202, 214, 255)  # lid top highlight row
MET  = (176, 182, 192, 255)  # handle light metal
METD = (90, 96, 108, 255)    # handle dark metal

# ---- brass toggle latch -------------------------------------------------
LAT  = (218, 186, 86, 255)   # latch base
LATH = (246, 222, 130, 255)  # latch highlight
LATD = (150, 116, 40, 255)   # latch shadow / hinge


def draw_latch(P, x0):
    """A suitcase draw-latch straddling the seam (y=9), 3 wide x 4 tall.
    cols x0..x0+2 ; keeper tab on the lid, lever + hinge foot on the body."""
    P[(x0 + 1, 8)] = LATH                                   # keeper tab on lid
    P[(x0, 9)] = LAT;  P[(x0 + 1, 9)] = LATH; P[(x0 + 2, 9)] = LATD   # lever top
    P[(x0, 10)] = LATH; P[(x0 + 1, 10)] = LAT; P[(x0 + 2, 10)] = LATD  # lever face
    P[(x0, 11)] = LATD; P[(x0 + 1, 11)] = LATD; P[(x0 + 2, 11)] = LATD  # hinge foot


def lunchbox_metal():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    P = {}
    left, right, top, bot = 2, 13, 5, 14

    # interior body fill
    for y in range(top + 1, bot):
        for x in range(left + 1, right):
            P[(x, y)] = BODY
    # lid band (upper portion)
    for y in range(top + 1, 9):
        for x in range(left + 1, right):
            P[(x, y)] = LID
    # lid top highlight row
    for x in range(left + 1, right):
        P[(x, top + 1)] = LIDH
    # left inner highlight, right inner shadow, bottom inner shadow
    for y in range(top + 1, bot):
        P[(left + 1, y)] = HI
        P[(right - 1, y)] = SH
    for x in range(left + 1, right):
        P[(x, bot - 1)] = SH
    # lid/base seam line
    for x in range(left + 1, right):
        P[(x, 9)] = SH

    # outline border (rounded corners)
    for x in range(left, right + 1):
        P[(x, top)] = OUTL
        P[(x, bot)] = OUTL
    for y in range(top, bot + 1):
        P[(left, y)] = OUTL
        P[(right, y)] = OUTL
    for c in [(left, top), (right, top), (left, bot), (right, bot)]:
        P[c] = (0, 0, 0, 0)

    # carry handle (arch) above the lid, feet landing on the lid top
    for x in (6, 7, 8, 9):
        P[(x, 2)] = METD
    P[(6, 2)] = MET
    P[(5, 3)] = METD; P[(10, 3)] = METD
    P[(5, 4)] = MET;  P[(10, 4)] = MET

    # two suitcase snap latches on the front
    draw_latch(P, 4)
    draw_latch(P, 9)

    for (x, y), c in P.items():
        if 0 <= x < 16 and 0 <= y < 16:
            img.putpixel((x, y), c)
    return img


if __name__ == "__main__":
    img = lunchbox_metal()
    img.save(os.path.join(OUT, "lunchbox_metal.png"))
    bg = Image.new("RGBA", (256, 256), (40, 40, 40, 255))
    bg.alpha_composite(img.resize((256, 256), Image.NEAREST))
    bg.save(os.path.join(OUT, "_preview_lunchbox_metal.png"))
    print("wrote out/lunchbox_metal.png + out/_preview_lunchbox_metal.png")
