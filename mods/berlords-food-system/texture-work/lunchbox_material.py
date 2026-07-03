"""Multi-material recolor of berlord's 3 source box renders.

Each box is SEGMENTED into parts (trim / lid / body / brass latch / steel
hardware) and every part gets its OWN colour+material ramp, so one image shows
several different colours and materials -- a believable two-tone lunchbox, not a
single-hue gradient map.

Run with DEBUG=1 to also dump a flat region map per source for checking the
lid/body seam split.
"""
import os
import numpy as np
from collections import deque
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "out")
SRC_DIR = r"C:\Users\berlord\Documents\StabilityMatrix-win-x64\Data\Images\Text2Img\2026-06-14"
DEBUG = os.environ.get("DEBUG") == "1"

# label -> (source render, material scheme). Each box gets ONE multi-material look.
SCHEMES = {
    "domed": {                                   # red lid + tan body + brass keyhole
        "src": "00035-3606144610.png",
        "lid":  ((58, 16, 16), (168, 44, 40), (228, 104, 92)),     # red enamel
        "body": ((84, 62, 32), (190, 156, 96), (238, 214, 162)),   # warm tan
    },
    "tab": {                                      # teal body + leather lid + brass tab
        "src": "00016-1430971589.png",
        "lid":  ((46, 30, 16), (120, 82, 44), (182, 142, 92)),     # brown leather
        "body": ((18, 58, 58), (42, 132, 128), (150, 212, 206)),   # teal enamel
    },
    "slotted": {                                  # green lid + cream body + brass latch
        "src": "00000-3998877134.png",
        "lid":  ((18, 44, 22), (52, 120, 50), (140, 196, 120)),    # forest green
        "body": ((86, 70, 38), (198, 168, 104), (240, 222, 168)),  # cream
    },
}

NEUTRAL = [(0.0, (40, 42, 48)), (0.45, (110, 114, 124)), (0.75, (164, 170, 180)),
           (1.0, (236, 240, 246))]                                  # steel hardware/shine
BRASS = [(0.0, (52, 38, 14)), (0.5, (152, 116, 40)), (1.0, (242, 210, 116))]
TRIM = (26, 26, 30)                                                 # dark edge trim
WHITE_T = 240


def ramp(L, stops):
    pos = [s[0] for s in stops]
    out = np.empty(L.shape + (3,), np.float32)
    for c in range(3):
        out[..., c] = np.interp(L, pos, [s[1][c] for s in stops])
    return out


def box_mask(rgb):
    near_white = (rgb >= WHITE_T).all(axis=-1)
    h, w = near_white.shape
    bg = np.zeros((h, w), bool)
    dq = deque()
    for x in range(w):
        for y in (0, h - 1):
            if near_white[y, x]:
                bg[y, x] = True; dq.append((y, x))
    for y in range(h):
        for x in (0, w - 1):
            if near_white[y, x]:
                bg[y, x] = True; dq.append((y, x))
    while dq:
        y, x = dq.popleft()
        for ny, nx in ((y-1, x), (y+1, x), (y, x-1), (y, x+1)):
            if 0 <= ny < h and 0 <= nx < w and near_white[ny, nx] and not bg[ny, nx]:
                bg[ny, nx] = True; dq.append((ny, nx))
    return ~bg


def find_seam(L, mask):
    """Row of the lid/body seam = darkest row-average in the upper-middle band."""
    ys = np.where(mask.any(axis=1))[0]
    y0, y1 = ys.min(), ys.max()
    H = y1 - y0
    lo, hi = y0 + int(0.20 * H), y0 + int(0.58 * H)
    rowmean = np.array([L[y][mask[y]].mean() if mask[y].any() else 1.0
                        for y in range(L.shape[0])])
    seam = lo + int(np.argmin(rowmean[lo:hi]))
    return y0, y1, seam


def render(scheme):
    im = Image.open(os.path.join(SRC_DIR, scheme["src"])).convert("RGBA")
    rgb = np.array(im)[..., :3].astype(np.float32)
    mask = box_mask(rgb.astype(np.uint8))
    L = (0.299 * rgb[..., 0] + 0.587 * rgb[..., 1] + 0.114 * rgb[..., 2]) / 255.0
    y0, y1, seam = find_seam(L, mask)

    yy = np.arange(L.shape[0])[:, None] * np.ones((1, L.shape[1]))
    is_lid = mask & (yy < seam)
    is_body = mask & (yy >= seam)
    warm = mask & (rgb[..., 0] - rgb[..., 2] > 22) & (rgb[..., 0] > 100)   # tab/latch -> brass
    bright_clasp = is_body & (L > 0.80)                                    # keyhole shine -> steel
    trim = mask & (L < 0.14)                                              # outline -> dark trim

    out = ramp(L, NEUTRAL)
    out[is_lid] = ramp(L, [(0, scheme["lid"][0]), (0.5, scheme["lid"][1]), (1, scheme["lid"][2])])[is_lid]
    out[is_body] = ramp(L, [(0, scheme["body"][0]), (0.5, scheme["body"][1]), (1, scheme["body"][2])])[is_body]
    out[bright_clasp] = ramp(L, NEUTRAL)[bright_clasp]
    out[warm] = ramp(L, BRASS)[warm]
    out[trim] = TRIM

    out = np.clip(out, 0, 255).astype(np.uint8)
    rgba = np.dstack([out, (mask * 255).astype(np.uint8)])
    img = Image.fromarray(rgba, "RGBA")

    if DEBUG:
        dbg = np.zeros((*L.shape, 4), np.uint8)
        dbg[is_lid] = (220, 60, 60, 255)
        dbg[is_body] = (60, 120, 220, 255)
        dbg[warm] = (240, 200, 40, 255)
        dbg[trim] = (20, 20, 20, 255)
        Image.fromarray(dbg, "RGBA").save(os.path.join(OUT, f"_dbg_{name}.png"))
    return img


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    items = list(SCHEMES.items())
    CELL, GAP, TY = 240, 14, 26
    W = len(items) * CELL + (len(items) - 1) * GAP + 20
    sheet = Image.new("RGBA", (W, TY + CELL + 14), (38, 38, 38, 255))
    draw = ImageDraw.Draw(sheet)
    for i, (name, scheme) in enumerate(items):
        img = render(scheme)
        img.save(os.path.join(OUT, f"lbmat_{name}.png"))
        x = 10 + i * (CELL + GAP)
        draw.text((x + 4, 8), name, fill=(235, 235, 235, 255))
        cell = Image.new("RGBA", (CELL, CELL), (38, 38, 38, 255))
        cell.alpha_composite(img.resize((CELL, CELL), Image.LANCZOS))
        sheet.paste(cell, (x, TY))
    sheet.save(os.path.join(OUT, "_lunchbox_material.png"))
    print("wrote", len(items), "multi-material boxes + _lunchbox_material.png")
