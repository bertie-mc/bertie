"""Vibrant region-fill recolor of berlord's 3 box renders.

Uses the box's OWN dark lines (outline + lid/body seam + latch edges) as
boundaries: flood-fill the enclosed areas into parts, then paint each part a
vibrant colour, keeping the dark lines as separators. Lid vs body is decided by
the seam row; warm/bright compact parts become the latch accent. 3 colour
schemes per box -> 9 outputs + a contact sheet. PREVIEW ONLY.

DEBUG=1 also dumps a random-colour region map per source for checking the split.
"""
import os
import numpy as np
from collections import deque
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "out")
SRC_DIR = r"C:\Users\berlord\Documents\StabilityMatrix-win-x64\Data\Images\Text2Img\2026-06-14"
DEBUG = os.environ.get("DEBUG") == "1"

SOURCES = {
    "domed":   "00035-3606144610.png",
    "tab":     "00016-1430971589.png",
    "slotted": "00000-3998877134.png",
}

# 3 vibrant schemes; each maps the 3 part-slots to a (shadow, base, highlight) ramp
VARIANTS = {
    "classic": {
        "lid":   ((90, 20, 20), (200, 40, 40), (245, 130, 118)),    # red
        "body":  ((20, 40, 110), (40, 92, 205), (132, 178, 250)),   # blue
        "latch": ((70, 50, 16), (182, 142, 50), (246, 216, 122)),   # brass
    },
    "sunset": {
        "lid":   ((110, 46, 10), (236, 120, 24), (252, 192, 112)),  # orange
        "body":  ((12, 70, 72), (28, 152, 150), (138, 222, 214)),   # teal
        "latch": ((110, 96, 10), (226, 200, 30), (250, 238, 142)),  # yellow
    },
    "forest": {
        "lid":   ((20, 70, 24), (48, 150, 52), (152, 216, 122)),    # green
        "body":  ((96, 72, 20), (220, 180, 70), (248, 228, 150)),   # gold/cream
        "latch": ((90, 18, 18), (200, 40, 38), (244, 120, 110)),    # red
    },
}

LINE = (24, 22, 26)          # the dark separating lines, kept as guide
DARK_T = 0.28                # below this luminance = a dark line/boundary
MIN_AREA = 90                # smaller blobs fold back into the lines
WHITE_T = 240


def ramp(L, stops):
    pos = [s[0] for s in stops]
    out = np.empty(L.shape + (3,), np.float32)
    for c in range(3):
        out[..., c] = np.interp(L, pos, [s[1][c] for s in stops])
    return out


def flood_from_border(seed):
    """Mask of seed-pixels connected to the image border (4-conn)."""
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
    """4-connected component labels of mask; 0 = not labelled."""
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


def segment(src_path):
    """Return (L, lines mask, dict slot->mask, box mask)."""
    im = Image.open(src_path).convert("RGB")
    rgb = np.array(im).astype(np.float32)
    near_white = (rgb >= WHITE_T).all(axis=-1)
    box = ~flood_from_border(near_white)
    L = (0.299 * rgb[..., 0] + 0.587 * rgb[..., 1] + 0.114 * rgb[..., 2]) / 255.0

    lines = box & (L < DARK_T)
    fillable = box & ~lines
    lab, n = label_regions(fillable)

    # seam row (darkest row-avg in upper-middle) for lid/body split
    ys = np.where(box.any(axis=1))[0]
    y0, y1 = ys.min(), ys.max(); Hh = y1 - y0
    rowmean = np.array([L[y][box[y]].mean() if box[y].any() else 1.0 for y in range(L.shape[0])])
    seam = y0 + int(0.20 * Hh) + int(np.argmin(rowmean[y0 + int(0.20 * Hh): y0 + int(0.58 * Hh)]))

    yidx = np.arange(L.shape[0])[:, None] * np.ones((1, L.shape[1]))
    slots = {"lid": np.zeros_like(box), "body": np.zeros_like(box), "latch": np.zeros_like(box)}
    for r in range(1, n + 1):
        m = lab == r
        area = int(m.sum())
        if area < MIN_AREA:
            lines |= m                                   # tiny blob -> treat as line
            continue
        warm = (rgb[m][:, 0] - rgb[m][:, 2]).mean() > 16
        bright_small = area < 1600 and L[m].mean() > 0.70
        if warm or bright_small:
            slots["latch"] |= m
            continue
        above = m & (yidx < seam)
        below = m & (yidx >= seam)
        # region with no dark line across the seam -> cut it at the seam row
        if above.sum() > 250 and below.sum() > 250:
            slots["lid"] |= above
            slots["body"] |= below
        elif np.where(m)[0].mean() < seam:
            slots["lid"] |= m
        else:
            slots["body"] |= m
    return L, lines, slots, box, lab


def render(L, lines, slots, box, scheme):
    out = np.zeros((*L.shape, 3), np.float32)
    for slot, mask in slots.items():
        lo, mid, hi = scheme[slot]
        out[mask] = ramp(L, [(0.0, lo), (0.5, mid), (1.0, hi)])[mask]
    out[lines] = LINE
    rgba = np.dstack([np.clip(out, 0, 255).astype(np.uint8), (box * 255).astype(np.uint8)])
    return Image.fromarray(rgba, "RGBA")


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    rng = np.random.RandomState(7)
    srcs = list(SOURCES.items())
    vars_ = list(VARIANTS.items())

    CELL, GAP, LX, TY = 200, 12, 80, 26
    W = LX + len(vars_) * CELL + (len(vars_) - 1) * GAP + 10
    H = TY + len(srcs) * CELL + (len(srcs) - 1) * GAP + 10
    sheet = Image.new("RGBA", (W, H), (38, 38, 38, 255))
    draw = ImageDraw.Draw(sheet)
    for ci, (vname, _) in enumerate(vars_):
        draw.text((LX + ci * (CELL + GAP) + 4, 8), vname, fill=(235, 235, 235, 255))

    for ri, (sname, fn) in enumerate(srcs):
        L, lines, slots, box, lab = segment(os.path.join(SRC_DIR, fn))
        y = TY + ri * (CELL + GAP)
        draw.text((6, y + CELL // 2 - 4), sname, fill=(235, 235, 235, 255))
        if DEBUG:
            dbg = np.zeros((*L.shape, 4), np.uint8)
            for r in range(1, lab.max() + 1):
                m = lab == r
                if m.sum() >= MIN_AREA:
                    dbg[m] = (*rng.randint(40, 255, 3), 255)
            dbg[lines] = (12, 12, 12, 255)
            Image.fromarray(dbg, "RGBA").save(os.path.join(OUT, f"_dbgv_{sname}.png"))
        for ci, (vname, scheme) in enumerate(vars_):
            img = render(L, lines, slots, box, scheme)
            img.save(os.path.join(OUT, f"lbvib_{sname}_{vname}.png"))
            cell = Image.new("RGBA", (CELL, CELL), (38, 38, 38, 255))
            cell.alpha_composite(img.resize((CELL, CELL), Image.LANCZOS))
            sheet.paste(cell, (LX + ci * (CELL + GAP), y))
    sheet.save(os.path.join(OUT, "_lunchbox_vibrant.png"))
    print(f"wrote {len(srcs) * len(vars_)} vibrant boxes + _lunchbox_vibrant.png")
