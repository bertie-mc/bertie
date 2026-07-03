"""Downscale AI-generated lunchbox candidates to true 16x16 Minecraft item textures.

Source = StabilityMatrix Text2Img/2026-06-14/*.png (512x512, faux pixel-art on
a near-white background). For each:
  1. flood-fill the white background -> transparent (from the borders, so
     interior whites survive),
  2. crop to the subject + pad to square (keeps proportions, centers it),
  3. downscale to 16x16 with PREMULTIPLIED alpha (no white halo on the edges).

Writes <name>_16.png (the texture) + an 8x NEAREST preview, plus one contact
sheet comparing every original to its 16x16 result.
Run:  python lunchbox_downscale.py
"""
import glob
import os
from collections import deque

import numpy as np
from PIL import Image

SRC = r"C:\Users\berlord\Documents\StabilityMatrix-win-x64\Data\Images\Text2Img\2026-06-14"
OUT = os.path.join(SRC, "downscaled_16")
os.makedirs(OUT, exist_ok=True)

WHITE_THRESH = 238   # a pixel counts as background-white if min(R,G,B) >= this
PAD = 6              # px padding around the subject bbox (at source res) before squaring


def remove_white_bg(im):
    """Flood-fill near-white from the image border, set those pixels transparent."""
    a = np.array(im.convert("RGBA"))
    h, w = a.shape[:2]
    rgb = a[..., :3].astype(int)
    near_white = rgb.min(axis=2) >= WHITE_THRESH
    # also treat already-transparent pixels as background seeds
    near_white |= a[..., 3] < 8

    bg = np.zeros((h, w), dtype=bool)
    q = deque()
    for x in range(w):
        for y in (0, h - 1):
            if near_white[y, x] and not bg[y, x]:
                bg[y, x] = True
                q.append((y, x))
    for y in range(h):
        for x in (0, w - 1):
            if near_white[y, x] and not bg[y, x]:
                bg[y, x] = True
                q.append((y, x))
    while q:
        y, x = q.popleft()
        for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            ny, nx = y + dy, x + dx
            if 0 <= ny < h and 0 <= nx < w and near_white[ny, nx] and not bg[ny, nx]:
                bg[ny, nx] = True
                q.append((ny, nx))

    a[bg, 3] = 0
    return a


def crop_square(a):
    """Crop to non-transparent bbox, pad, then expand to a centered square."""
    ys, xs = np.where(a[..., 3] > 16)
    if len(xs) == 0:
        return Image.fromarray(a)
    x0, x1 = xs.min(), xs.max()
    y0, y1 = ys.min(), ys.max()
    x0, y0 = max(0, x0 - PAD), max(0, y0 - PAD)
    x1, y1 = min(a.shape[1] - 1, x1 + PAD), min(a.shape[0] - 1, y1 + PAD)
    sub = a[y0:y1 + 1, x0:x1 + 1]
    sh, sw = sub.shape[:2]
    side = max(sh, sw)
    canvas = np.zeros((side, side, 4), dtype=np.uint8)
    oy, ox = (side - sh) // 2, (side - sw) // 2
    canvas[oy:oy + sh, ox:ox + sw] = sub
    return Image.fromarray(canvas)


def downscale16(im):
    """16x16 downscale via premultiplied alpha so transparent RGB never bleeds in."""
    a = np.array(im.convert("RGBA")).astype(np.float64)
    alpha = a[..., 3:4] / 255.0
    a[..., :3] *= alpha                      # premultiply
    pre = Image.fromarray(a.astype(np.uint8))
    small = np.array(pre.resize((16, 16), Image.LANCZOS)).astype(np.float64)
    sa = small[..., 3:4] / 255.0
    safe = np.where(sa > 0, sa, 1.0)
    small[..., :3] = np.clip(small[..., :3] / safe, 0, 255)   # un-premultiply
    return Image.fromarray(small.astype(np.uint8))


def checker(size, c1=(150, 150, 150), c2=(110, 110, 110), cell=8):
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            px[x, y] = (*(c1 if (x // cell + y // cell) % 2 == 0 else c2), 255)
    return img


if __name__ == "__main__":
    files = sorted(glob.glob(os.path.join(SRC, "*.png")))
    results = []
    for f in files:
        name = os.path.splitext(os.path.basename(f))[0]
        im = Image.open(f)
        cleaned = crop_square(remove_white_bg(im))
        tex = downscale16(cleaned)
        tex.save(os.path.join(OUT, name + "_16.png"))
        tex.resize((128, 128), Image.NEAREST).save(os.path.join(OUT, name + "_16x8.png"))
        results.append((name, im, tex))
        print("wrote", name + "_16.png")

    # contact sheet: original (192) over 16x16 preview on checker (192), labelled
    COL, PADc, LABEL = 192, 12, 22
    sheet = Image.new("RGBA", (COL * len(results) + PADc * (len(results) + 1),
                               LABEL + 192 + 8 + 192 + PADc * 2), (32, 32, 32, 255))
    for i, (name, orig, tex) in enumerate(results):
        x = PADc + i * (COL + PADc)
        sheet.paste(orig.convert("RGBA").resize((COL, 192), Image.LANCZOS), (x, LABEL))
        ck = checker(192)
        ck.alpha_composite(tex.resize((192, 192), Image.NEAREST))
        sheet.paste(ck, (x, LABEL + 192 + 8))
    sheet.save(os.path.join(OUT, "_contact_sheet.png"))
    print("wrote _contact_sheet.png ->", OUT)
