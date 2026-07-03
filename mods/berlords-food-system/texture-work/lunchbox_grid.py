"""27 lunchbox options across 3 axes, from berlord's 3 reference boxes.

  shape   = the 3 references (domed / tab / slotted)         -- keep/var the SHAPE
  texture = metal(iron) / wood(oak) / leather(brown_wool)    -- keep/var the TEXTURE
  color   = natural / warm / cool                            -- keep/var the COLOR

3 x 3 x 3 = 27. Reuses the crease-following segmentation from lunchbox_textured.
- natural: the material's own colours (lid darkened for a two-tone), box-lit.
- warm/cool: the material's GRAIN kept, recoloured through a palette ramp.
Latch is always a gold clasp. Outputs 27 PNGs + a contact sheet.
"""
import os
import numpy as np
from PIL import Image, ImageDraw
from lunchbox_textured import segment, tiled, SRC_DIR, SOURCES

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "out")

TEXTURES = {"metal": "iron_block", "wood": "oak_planks", "leather": "brown_wool"}
COLORS = ["natural", "warm", "cool"]
PALETTE = {
    "warm": {"lid": ((70, 16, 16), (180, 40, 36), (238, 120, 104)),
             "body": ((86, 52, 14), (206, 150, 52), (246, 212, 140))},
    "cool": {"lid": ((16, 26, 60), (36, 72, 150), (120, 160, 232)),
             "body": ((12, 64, 66), (30, 140, 138), (140, 216, 210))},
}
LINE = (22, 20, 24)


def ramp(L, stops):
    out = np.empty(L.shape + (3,), np.float32)
    for c in range(3):
        out[..., c] = np.interp(L, [s[0] for s in stops], [s[1][c] for s in stops])
    return out


def mat_value(name, shape):
    """Material grain as a mean-centred 0..1 value map (colour discarded)."""
    t = tiled(name, shape)
    v = (0.299 * t[..., 0] + 0.587 * t[..., 1] + 0.114 * t[..., 2]) / 255.0
    return np.clip(0.5 + (v - v.mean()) * 1.4, 0, 1)


def render(L, lines, slots, box, texture, color):
    lightf = np.clip(0.45 + 1.15 * L, 0.32, 1.62)[..., None]
    out = np.zeros((*L.shape, 3), np.float32)
    gold = tiled("gold_block", L.shape)

    if color == "natural":
        col = tiled(TEXTURES[texture], L.shape)
        out[slots["body"]] = np.clip(col * lightf, 0, 255)[slots["body"]]
        out[slots["lid"]] = np.clip(col * 0.55 * lightf, 0, 255)[slots["lid"]]
    else:
        V = mat_value(TEXTURES[texture], L.shape)
        grain = (0.80 + 0.42 * V)[..., None]
        pal = PALETTE[color]
        for slot in ("lid", "body"):
            lo, mid, hi = pal[slot]
            painted = ramp(L, [(0.0, lo), (0.5, mid), (1.0, hi)]) * grain
            out[slots[slot]] = np.clip(painted, 0, 255)[slots[slot]]

    out[slots["latch"]] = np.clip(gold * lightf, 0, 255)[slots["latch"]]
    out[lines] = LINE
    rgba = np.dstack([out.astype(np.uint8), (box * 255).astype(np.uint8)])
    return Image.fromarray(rgba, "RGBA")


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    shapes = list(SOURCES.items())
    texkeys = list(TEXTURES.keys())                           # rows: metal/wood/leather
    n = 0
    rendered = {}                                             # (shape,tex,color) -> img
    for sname, fn in shapes:
        L, lines, slots, box, _, seam = segment(os.path.join(SRC_DIR, fn))
        for t in texkeys:
            for c in COLORS:
                img = render(L, lines, slots, box, t, c)
                img.save(os.path.join(OUT, f"lbgen_{sname}_{t}_{c}.png"))
                rendered[(sname, t, c)] = img
                n += 1

    def grid(keys_rc, cols, rows, title_cols, title_rows, fname, cell=200):
        GAP, LX, TY = 10, 78, 30
        W = LX + len(cols) * cell + (len(cols) - 1) * GAP + 10
        H = TY + len(rows) * cell + (len(rows) - 1) * GAP + 10
        sh = Image.new("RGBA", (W, H), (36, 36, 36, 255))
        d = ImageDraw.Draw(sh)
        for ci, t in enumerate(title_cols):
            d.text((LX + ci * (cell + GAP) + 6, 10), t, fill=(235, 235, 235, 255))
        for ri, t in enumerate(title_rows):
            d.text((6, TY + ri * (cell + GAP) + cell // 2 - 4), t, fill=(235, 235, 235, 255))
        for ri in range(len(rows)):
            for ci in range(len(cols)):
                img = rendered[keys_rc(ri, ci)]
                cimg = Image.new("RGBA", (cell, cell), (36, 36, 36, 255))
                cimg.alpha_composite(img.resize((cell, cell), Image.LANCZOS))
                sh.paste(cimg, (LX + ci * (cell + GAP), TY + ri * (cell + GAP)))
        sh.save(os.path.join(OUT, fname))

    # one 3x3 sheet per shape (rows = texture, cols = color)
    for sname, _ in shapes:
        grid(lambda ri, ci, s=sname: (s, texkeys[ri], COLORS[ci]),
             COLORS, texkeys, COLORS, texkeys, f"_grid_{sname}.png")
    # master: rows = shape, cols = the 9 texture/color combos
    combos = [(t, c) for t in texkeys for c in COLORS]
    grid(lambda ri, ci: (shapes[ri][0], combos[ci][0], combos[ci][1]),
         combos, [s for s, _ in shapes], [f"{t}/{c}" for t, c in combos],
         [s for s, _ in shapes], "_lunchbox_grid.png", cell=150)
    print(f"wrote {n} options + _grid_<shape>.png (3) + _lunchbox_grid.png")
