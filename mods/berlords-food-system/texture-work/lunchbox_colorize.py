"""Colorize berlord's 3 source box renders into lunchbox colorways.

Takes the actual Stability-Matrix images (grayscale metal boxes on a white
background), keys out the white backdrop, and recolors each as a believable
MULTI-MATERIAL item (not a flat single-hue gradient map):
  - painted BODY  -> the variant's colour, applied only to the mid-tones
  - dark steel    -> shadows / lid / deep recesses stay neutral metal
  - bright steel  -> specular shine on the handle & clasp stays metallic
  - brass         -> any warm/red original pixels (the tab, latch) go gold
12 outputs + a contact sheet. PREVIEW ONLY; full-res (512).
"""
import os
from collections import deque
import numpy as np
from PIL import Image, ImageOps, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "out")
SRC_DIR = r"C:\Users\berlord\Documents\StabilityMatrix-win-x64\Data\Images\Text2Img\2026-06-14"

SOURCES = {                       # label -> source render
    "domed":   "00035-3606144610.png",   # dark domed lid + front keyhole
    "tab":     "00016-1430971589.png",   # steel box + red top tab
    "slotted": "00000-3998877134.png",   # dark slotted/recessed lid
}

# painted-body ramp per variant: shadow -> midtone -> highlight (only the body)
COLORWAYS = {
    "blue":  ((22, 40, 78),  (58, 104, 176), (140, 186, 238)),
    "red":   ((70, 20, 18),  (176, 52, 46),  (232, 120, 100)),
    "green": ((24, 56, 28),  (66, 134, 62),  (150, 206, 130)),
    "amber": ((78, 52, 16),  (196, 150, 60), (244, 210, 130)),
}

# shared material ramps (same for every variant) -------------------------------
NEUTRAL = [(0.0, (28, 30, 36)), (0.40, (92, 96, 106)), (0.70, (150, 156, 166)),
           (0.88, (198, 204, 212)), (1.0, (238, 242, 248))]   # charcoal -> steel -> shine
BRASS = [(0.0, (54, 40, 14)), (0.5, (150, 116, 40)), (1.0, (240, 208, 112))]
# how "painted" each luminance is: shadows & speculars stay metal, body gets colour
WEIGHT = [(0.0, 0.0), (0.22, 0.0), (0.42, 1.0), (0.72, 1.0), (0.90, 0.18), (1.0, 0.0)]

WHITE_T = 240                     # background-white threshold (box light-grays sit below this)


def ramp(L, stops):
    """Per-channel linear interpolation of L (0..1 array) through colour stops."""
    pos = [s[0] for s in stops]
    out = np.empty(L.shape + (3,), np.float32)
    for c in range(3):
        out[..., c] = np.interp(L, pos, [s[1][c] for s in stops])
    return out


def box_mask(rgb):
    """True where the box is. Flood-fill near-white from the borders = backdrop."""
    near_white = (rgb >= WHITE_T).all(axis=-1)
    h, w = near_white.shape
    bg = np.zeros((h, w), bool)
    dq = deque()
    for x in range(w):
        for y in (0, h - 1):
            if near_white[y, x] and not bg[y, x]:
                bg[y, x] = True; dq.append((y, x))
    for y in range(h):
        for x in (0, w - 1):
            if near_white[y, x] and not bg[y, x]:
                bg[y, x] = True; dq.append((y, x))
    while dq:
        y, x = dq.popleft()
        for ny, nx in ((y-1, x), (y+1, x), (y, x-1), (y, x+1)):
            if 0 <= ny < h and 0 <= nx < w and near_white[ny, nx] and not bg[ny, nx]:
                bg[ny, nx] = True; dq.append((ny, nx))
    return ~bg                    # box = everything not reached from the border


def colorize(src_path, body_ramp):
    im = Image.open(src_path).convert("RGBA")
    rgb = np.array(im)[..., :3].astype(np.float32)
    mask = box_mask(rgb.astype(np.uint8))

    L = (0.299 * rgb[..., 0] + 0.587 * rgb[..., 1] + 0.114 * rgb[..., 2]) / 255.0
    lo, mid, hi = body_ramp
    body = ramp(L, [(0.0, lo), (0.5, mid), (1.0, hi)])
    neutral = ramp(L, NEUTRAL)
    w = np.interp(L, [s[0] for s in WEIGHT], [s[1] for s in WEIGHT])[..., None]
    out = neutral * (1 - w) + body * w                       # paint only the mid-tones

    # warm original pixels (the red tab / brass latch) -> brass, regardless of band
    warm = (rgb[..., 0] - rgb[..., 2] > 25) & (rgb[..., 0] > 110)
    out[warm] = ramp(L, BRASS)[warm]

    out = np.clip(out, 0, 255).astype(np.uint8)
    rgba = np.dstack([out, (mask * 255).astype(np.uint8)])
    return Image.fromarray(rgba, "RGBA")


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    srcs = list(SOURCES.items())
    cols = list(COLORWAYS.items())

    CELL, GAP, LX, TY = 150, 10, 84, 26
    W = LX + len(cols) * CELL + (len(cols) - 1) * GAP + 10
    H = TY + len(srcs) * CELL + (len(srcs) - 1) * GAP + 10
    sheet = Image.new("RGBA", (W, H), (38, 38, 38, 255))
    draw = ImageDraw.Draw(sheet)
    for ci, (cname, _) in enumerate(cols):
        draw.text((LX + ci * (CELL + GAP) + 4, 8), cname, fill=(235, 235, 235, 255))

    for ri, (sname, fn) in enumerate(srcs):
        path = os.path.join(SRC_DIR, fn)
        y = TY + ri * (CELL + GAP)
        draw.text((6, y + CELL // 2 - 4), sname, fill=(235, 235, 235, 255))
        for ci, (cname, body_ramp) in enumerate(cols):
            img = colorize(path, body_ramp)
            img.save(os.path.join(OUT, f"lbcolor_{sname}_{cname}.png"))
            cell = Image.new("RGBA", (CELL, CELL), (38, 38, 38, 255))
            cell.alpha_composite(img.resize((CELL, CELL), Image.LANCZOS))
            sheet.paste(cell, (LX + ci * (CELL + GAP), y))

    sheet.save(os.path.join(OUT, "_lunchbox_colorized.png"))
    print(f"wrote {len(srcs) * len(cols)} colorized boxes + _lunchbox_colorized.png")
