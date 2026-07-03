"""Vanilla-material recolor of berlord's 3 box renders.

Two fixes over the flat-colour version:
  1. The lid/body boundary FOLLOWS the box's own dark seam: a per-column trace
     of the strongest brightness drop in the upper-middle band (smoothed),
     unioned with the dark outline -- no straight horizontal cut.
  2. Each part is filled with a TILED VANILLA TEXTURE (iron, leather/wool,
     wood, copper, gold...) modulated by the box's luminance so the original
     3-D lighting still reads. Dark lines kept as separators.

3 material schemes per box -> 9 outputs + a contact sheet. DEBUG=1 dumps the
region map with the traced seam drawn on top.
"""
import os
import numpy as np
from collections import deque
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "out")
MAT = os.path.join(HERE, "materials")
SRC_DIR = r"C:\Users\berlord\Documents\StabilityMatrix-win-x64\Data\Images\Text2Img\2026-06-14"
DEBUG = os.environ.get("DEBUG") == "1"

SOURCES = {
    "domed":   "00035-3606144610.png",
    "tab":     "00016-1430971589.png",
    "slotted": "00000-3998877134.png",
}

# each scheme assigns a vanilla texture to the 3 part-slots
VARIANTS = {
    "ironclad": {"body": "iron_block",   "lid": "netherite_block", "latch": "gold_block"},
    "rustic":   {"body": "copper_block", "lid": "spruce_planks",   "latch": "gold_block"},
    "leather":  {"body": "brown_wool",   "lid": "oak_planks",      "latch": "iron_block"},
}

LINE = (22, 20, 24)
DARK_T = 0.26
TILE_PX = 8            # each material texel -> 8x8 px in the 512 preview (chunky, readable)
MIN_AREA = 120
WHITE_T = 240


def flood_border(seed):
    h, w = seed.shape
    hit = np.zeros((h, w), bool)
    dq = deque()
    for x in range(w):
        for y in (0, h - 1):
            if seed[y, x]:
                hit[y, x] = True; dq.append((y, x))
    for y in range(h):
        for x in (0, w - 1):
            if seed[y, x]:
                hit[y, x] = True; dq.append((y, x))
    while dq:
        y, x = dq.popleft()
        for ny, nx in ((y-1, x), (y+1, x), (y, x-1), (y, x+1)):
            if 0 <= ny < h and 0 <= nx < w and seed[ny, nx] and not hit[ny, nx]:
                hit[ny, nx] = True; dq.append((ny, nx))
    return hit


def label_regions(mask):
    h, w = mask.shape
    lab = np.zeros((h, w), np.int32)
    cur = 0
    for sy in range(h):
        for sx in range(w):
            if mask[sy, sx] and lab[sy, sx] == 0:
                cur += 1
                lab[sy, sx] = cur
                dq = deque([(sy, sx)])
                while dq:
                    y, x = dq.popleft()
                    for ny, nx in ((y-1, x), (y+1, x), (y, x-1), (y, x+1)):
                        if 0 <= ny < h and 0 <= nx < w and mask[ny, nx] and lab[ny, nx] == 0:
                            lab[ny, nx] = cur; dq.append((ny, nx))
    return lab, cur


def blur3(a):
    p = np.pad(a, 1, mode="edge")
    return sum(p[i:i+a.shape[0], j:j+a.shape[1]]
               for i in range(3) for j in range(3)) / 9.0


def dilate(mask):
    p = np.pad(mask, 1, mode="constant")
    out = np.zeros_like(mask)
    for i in range(3):
        for j in range(3):
            out |= p[i:i+mask.shape[0], j:j+mask.shape[1]]
    return out


def border(box, n=2):
    """n-px outline of the box silhouette."""
    eroded = box.copy()
    for _ in range(n):
        p = np.pad(eroded, 1, mode="constant")
        nb = (p[:-2, 1:-1] & p[2:, 1:-1] & p[1:-1, :-2] & p[1:-1, 2:])
        eroded = eroded & nb
    return box & ~eroded


def trace_crease(L, box):
    """Per column, scan UP from deep in the bright body to the first real
    darkening (relative to that column's lit brightness) = the lid/body crease."""
    Lb = blur3(L)
    seam = np.full(L.shape[1], -1)
    for x in range(L.shape[1]):
        rows = np.where(box[:, x])[0]
        if len(rows) < 8:
            continue
        by0, by1 = rows.min(), rows.max(); h = by1 - by0
        lowmask = box[:, x] & (np.arange(L.shape[0]) >= by0 + 0.40 * h)
        if lowmask.sum() < 3:
            continue
        maxL = Lb[lowmask, x].max()
        thr = 0.60 * maxL                              # crease is much darker than the lit body
        anchor = int(by0 + 0.72 * h)
        crease, run, y = by0, 0, anchor
        while y > by0:
            if box[y, x] and Lb[y, x] < thr:
                run += 1
                if run >= 2:
                    crease = y + run                   # bottom of the dark run = top of body
                    break
            else:
                run = 0
            y -= 1
        seam[x] = min(crease, by1)
    valid = np.where(seam >= 0)[0]
    sm = seam.copy()
    for x in valid:                                    # median smooth so it follows a clean crease
        lo, hi = max(0, x - 11), min(L.shape[1], x + 12)
        win = seam[lo:hi]; win = win[win >= 0]
        sm[x] = int(np.median(win))
    return sm


def segment(src_path):
    im = Image.open(src_path).convert("RGB")
    rgb = np.array(im).astype(np.float32)
    box = ~flood_border((rgb >= WHITE_T).all(axis=-1))
    L = (0.299 * rgb[..., 0] + 0.587 * rgb[..., 1] + 0.114 * rgb[..., 2]) / 255.0

    seam = trace_crease(L, box)
    # the separating LINES are only thin structure: the silhouette outline + the
    # traced crease (drawn 2 px). NOT the big dark lid fill -> that stays lid.
    lines = border(box, 2)
    for x in range(L.shape[1]):
        if seam[x] >= 0:
            for dy in (-1, 0):
                y = seam[x] + dy
                if 0 <= y < L.shape[0] and box[y, x]:
                    lines[y, x] = True

    yidx = np.arange(L.shape[0])[:, None] * np.ones((1, L.shape[1]))
    seam_row = np.where(seam >= 0, seam, 10 ** 6)[None, :]    # per-column crease row
    above = yidx < seam_row                                   # lid side of the crease
    # latch: warm pixels (the tab) + the single best compact bright blob (keyhole)
    warm = box & (rgb[..., 0] - rgb[..., 2] > 16) & (rgb[..., 0] > 95)
    latch = warm.copy()
    labb, nb = label_regions(box & ~lines & (L > 0.70))
    best, best_score = None, 0
    for r in range(1, nb + 1):
        m = labb == r
        yy, xx = np.where(m)
        a = len(yy)
        if not (200 <= a <= 1400):
            continue
        w, h = np.ptp(xx) + 1, np.ptp(yy) + 1
        if not (0.5 <= w / h <= 2.0):                        # compact, not a thin streak
            continue
        score = L[m].mean() * a
        if score > best_score:
            best, best_score = m, score
    if best is not None:
        latch |= best

    slots = {
        "lid":  box & above & ~lines & ~latch,
        "body": box & ~above & ~lines & ~latch,
        "latch": latch & ~lines,
    }
    return L, lines, slots, box, None, seam


def tiled(mat_name, shape):
    t = Image.open(os.path.join(MAT, mat_name + ".png")).convert("RGB")
    t = t.resize((t.width * TILE_PX, t.height * TILE_PX), Image.NEAREST)
    ta = np.array(t, np.float32)
    reps = (shape[0] // ta.shape[0] + 2, shape[1] // ta.shape[1] + 2, 1)
    return np.tile(ta, reps)[:shape[0], :shape[1]]


def render(L, lines, slots, box, scheme):
    # shade factor from box luminance: keeps the 3-D lighting on the material
    shade = np.clip(0.45 + 1.15 * L, 0.30, 1.65)[..., None]
    out = np.zeros((*L.shape, 3), np.float32)
    for slot, mask in slots.items():
        tex = tiled(scheme[slot], L.shape)
        painted = np.clip(tex * shade, 0, 255)
        out[mask] = painted[mask]
    out[lines] = LINE
    rgba = np.dstack([out.astype(np.uint8), (box * 255).astype(np.uint8)])
    return Image.fromarray(rgba, "RGBA")


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    rng = np.random.RandomState(7)
    srcs = list(SOURCES.items())
    vars_ = list(VARIANTS.items())
    CELL, GAP, LX, TY = 200, 12, 84, 26
    W = LX + len(vars_) * CELL + (len(vars_) - 1) * GAP + 10
    H = TY + len(srcs) * CELL + (len(srcs) - 1) * GAP + 10
    sheet = Image.new("RGBA", (W, H), (38, 38, 38, 255))
    draw = ImageDraw.Draw(sheet)
    for ci, (vname, _) in enumerate(vars_):
        draw.text((LX + ci * (CELL + GAP) + 4, 8), vname, fill=(235, 235, 235, 255))

    for ri, (sname, fn) in enumerate(srcs):
        L, lines, slots, box, lab, seam = segment(os.path.join(SRC_DIR, fn))
        y = TY + ri * (CELL + GAP)
        draw.text((6, y + CELL // 2 - 4), sname, fill=(235, 235, 235, 255))
        if DEBUG:
            dbg = np.zeros((*L.shape, 4), np.uint8)
            dbg[slots["lid"]] = (90, 140, 235, 255)
            dbg[slots["body"]] = (235, 120, 90, 255)
            dbg[slots["latch"]] = (240, 215, 60, 255)
            dbg[lines] = (15, 15, 15, 255)
            for x in range(L.shape[1]):
                if seam[x] >= 0 and box[seam[x], x]:
                    dbg[seam[x], x] = (0, 255, 0, 255)
            Image.fromarray(dbg, "RGBA").save(os.path.join(OUT, f"_dbgt_{sname}.png"))
        for ci, (vname, scheme) in enumerate(vars_):
            img = render(L, lines, slots, box, scheme)
            img.save(os.path.join(OUT, f"lbtex_{sname}_{vname}.png"))
            cell = Image.new("RGBA", (CELL, CELL), (38, 38, 38, 255))
            cell.alpha_composite(img.resize((CELL, CELL), Image.LANCZOS))
            sheet.paste(cell, (LX + ci * (CELL + GAP), y))
    sheet.save(os.path.join(OUT, "_lunchbox_textured.png"))
    print(f"wrote {len(srcs) * len(vars_)} textured boxes + _lunchbox_textured.png")
