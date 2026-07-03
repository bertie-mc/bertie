"""NEW hand-authored 16x16 lunchbox item textures.

Not a recolor of berlord's renders -- these are drawn from scratch as Minecraft
item pixel art, using his three boxes only as visual reference:
  domed   - tall rounded dark lid + front keyhole clasp
  tab     - flat lid + small top tab/handle
  slotted - flat lid with a dark recessed slot
A cabinet-projection 3/4 box (top + front + right faces) with flat per-face
shading, dark separating lines, a two-tone lid/body and an accent clasp.
3 designs x 3 colourways = 9 textures, rendered at 16x16 (previewed scaled).
"""
import os
import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "out")

DEPTH = 3
FX0, FX1, FYT, FYB = 2, 10, 6, 13          # front face extents
OUTL = (24, 20, 28)

COLORWAYS = {                               # (lid, body, latch)
    "red":   ((188, 46, 44), (210, 176, 120), (232, 196, 82)),
    "blue":  ((54, 98, 182), (172, 180, 192), (232, 196, 82)),
    "green": ((58, 150, 60), (202, 172, 112), (206, 64, 52)),
}
FACE_SHADE = {"top": 1.18, "front": 1.0, "right": 0.70}


def build_box():
    front = {(x, y) for x in range(FX0, FX1 + 1) for y in range(FYT, FYB + 1)}
    top = {(x + d, FYT - d) for x in range(FX0, FX1 + 1) for d in range(0, DEPTH + 1)}
    right = {(FX1 + d, y - d) for y in range(FYT, FYB + 1) for d in range(0, DEPTH + 1)}
    face = {}
    for p in front:  face[p] = "front"
    for p in right:  face[p] = "right"
    for p in top:    face[p] = "top"
    box = set(face)
    # per-column top y, for wrapping the lid over the silhouette
    tY = {}
    for (x, y) in box:
        tY[x] = min(y, tY.get(x, 99))
    lid = set(top)
    for (x, y) in (front | right):
        if y <= tY[x] + 1:
            lid.add((x, y))
    body = (front | right) - lid
    return face, box, lid, body, tY


PRI = {"top": 2, "right": 1, "front": 0}


def silhouette_and_seams(face, box):
    edges = set()
    for (x, y) in box:
        f = face.get((x, y), "top")
        for nx, ny in ((x+1, y), (x-1, y), (x, y+1), (x, y-1)):
            if (nx, ny) not in box:                       # full silhouette outline
                edges.add((x, y)); break
    for (x, y) in box:                                    # single-thickness face seam
        f = face.get((x, y), "top")
        for nx, ny in ((x+1, y), (x-1, y), (x, y+1), (x, y-1)):
            if (nx, ny) in box and PRI.get(face.get((nx, ny)), 0) > PRI.get(f, 0):
                edges.add((x, y)); break
    return edges


def draw(design, colorway):
    face, box, lid, body, tY = build_box()
    lid_c, body_c, latch_c = COLORWAYS[colorway]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()

    def put(p, rgb):
        if 0 <= p[0] < 16 and 0 <= p[1] < 16:
            px[p[0], p[1]] = (int(rgb[0]), int(rgb[1]), int(rgb[2]), 255)

    seams = silhouette_and_seams(face, box)
    latch = set()
    lid_extra = set()

    if design == "domed":
        # darken the lid, round the two top-back corners, add a front keyhole
        for p in [(2, 6), (13, 3)]:
            box.discard(p); lid.discard(p); face.pop(p, None)
        latch = {(6, 10), (6, 11)}                        # keyhole clasp on the front
    elif design == "tab":
        lid_extra = {(7, 2), (8, 2), (7, 3), (8, 3)}      # tab/handle above the lid
        box |= lid_extra
    elif design == "slotted":
        latch = {(5, 9), (6, 9), (7, 9)}                  # dark recessed slot reads via accent line

    for p in box:
        if p in lid_extra:
            put(p, np.array(latch_c) * 0.9); continue
        f = face.get(p, "top")
        base = np.array(lid_c if p in lid else body_c, float)
        if design == "domed" and p in lid:
            base = base * 0.62 + np.array((30, 30, 38)) * 0.38   # dark dome
        put(p, np.clip(base * FACE_SHADE[f], 0, 255))
    for p in latch:
        if design == "slotted":
            put(p, OUTL)                                  # the slot is a dark line
        else:
            put(p, latch_c)
    for p in seams:
        if p in box:
            put(p, OUTL)
    return img


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    designs = ["domed", "tab", "slotted"]
    colors = list(COLORWAYS.keys())
    S = 192
    CELL, GAP, LX, TY = S, 12, 78, 26
    W = LX + len(colors) * CELL + (len(colors) - 1) * GAP + 10
    H = TY + len(designs) * CELL + (len(designs) - 1) * GAP + 10
    sheet = Image.new("RGBA", (W, H), (40, 40, 40, 255))
    d = ImageDraw.Draw(sheet)
    for ci, c in enumerate(colors):
        d.text((LX + ci * (CELL + GAP) + 4, 8), c, fill=(235, 235, 235, 255))
    for ri, design in enumerate(designs):
        d.text((6, TY + ri * (CELL + GAP) + CELL // 2 - 4), design, fill=(235, 235, 235, 255))
        for ci, c in enumerate(colors):
            img = draw(design, c)
            img.save(os.path.join(OUT, f"lbitem_{design}_{c}.png"))            # real 16x16
            cell = Image.new("RGBA", (CELL, CELL), (40, 40, 40, 255))
            cell.alpha_composite(img.resize((CELL, CELL), Image.NEAREST))
            sheet.paste(cell, (LX + ci * (CELL + GAP), TY + ri * (CELL + GAP)))
    sheet.save(os.path.join(OUT, "_lunchbox_items.png"))
    print("wrote 9 item textures + _lunchbox_items.png")
