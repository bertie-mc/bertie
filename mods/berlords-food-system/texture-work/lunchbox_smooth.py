"""Smooth + palette-reduce noisy lunchbox 4 & 5 faces. Iterate from ORIGINALS.

Step 0: make every pixel fully opaque (alpha=1). Former transparent/partial
        pixels are filled by bleeding the nearest solid colours outward (no black).
Step 1: build a ladder of reductions so we can pick how aggressive to go:
        quantize to N colours, optionally with a Mode-filter denoise pass that
        flattens single-pixel speckle on the faces.

Renders one comparison sheet (rows = lunchbox 4,5; columns = the ladder), each
tile labelled with its final unique-colour count. Nothing is deployed yet.
Run:  python lunchbox_smooth.py
"""
import os

import numpy as np
from PIL import Image, ImageFilter, ImageDraw

BACKUP = r"C:\Users\berlord\Documents\claude_base\berlords-texture-test\texture-backup"
WORK = os.path.join(os.path.dirname(os.path.abspath(__file__)), "smooth_work")
os.makedirs(WORK, exist_ok=True)
ITEMS = [4, 5]
UP = 150


def edge_bleed(rgb, known, iters=40):
    rgb = rgb.copy()
    known = known.copy()
    for _ in range(iters):
        if known.all():
            break
        pr = np.pad(rgb, ((1, 1), (1, 1), (0, 0)))
        pk = np.pad(known.astype(float), 1)
        ks = (pk[:-2, 1:-1] + pk[2:, 1:-1] + pk[1:-1, :-2] + pk[1:-1, 2:] +
              pk[:-2, :-2] + pk[:-2, 2:] + pk[2:, :-2] + pk[2:, 2:])
        rs = (pr[:-2, 1:-1] + pr[2:, 1:-1] + pr[1:-1, :-2] + pr[1:-1, 2:] +
              pr[:-2, :-2] + pr[:-2, 2:] + pr[2:, :-2] + pr[2:, 2:])
        fill = (~known) & (ks > 0)
        avg = rs / np.maximum(ks, 1)[..., None]
        rgb[fill] = avg[fill]
        known = known | fill
    return rgb


def fill_opaque(img):
    """alpha -> 1 for every pixel; bleed real colours into the former holes."""
    a = np.array(img.convert("RGBA"))
    al = a[..., 3]
    known = al >= 200
    if not known.any():
        known = al > 0
    filled = edge_bleed(a[..., :3].astype(float), known)
    out = np.dstack([np.clip(filled, 0, 255).astype(np.uint8),
                     np.full(al.shape, 255, np.uint8)])
    return Image.fromarray(out, "RGBA")


def quant(img_rgb, n):
    return img_rgb.quantize(colors=n, method=Image.MEDIANCUT, dither=Image.NONE).convert("RGB")


def despeckle_lone(img):
    """Replace only pixels whose colour appears NOWHERE in their 3x3 neighbourhood
    with the local majority colour. Kills lone speckle; keeps any >=2px feature."""
    a = np.array(img.convert("RGB"))
    h, w, _ = a.shape
    a64 = a.astype(np.int64)
    code = a64[..., 0] * 65536 + a64[..., 1] * 256 + a64[..., 2]
    pad = np.pad(code, 1, constant_values=-1)
    out = a.copy()
    for y in range(h):
        for x in range(w):
            win = pad[y:y + 3, x:x + 3].ravel()
            if (win == code[y, x]).sum() == 1:           # lone pixel
                neigh = win[win != -1]
                vals, counts = np.unique(neigh, return_counts=True)
                m = int(vals[counts.argmax()])
                out[y, x] = [(m >> 16) & 255, (m >> 8) & 255, m & 255]
    return Image.fromarray(out, "RGB")


def ncolors(img):
    a = np.array(img.convert("RGB"))
    return len({tuple(p) for row in a for p in row})


# (label, builder taking an opaque RGB image)
LADDER = [
    ("original\nopaque", lambda im: im),
    ("quant 12", lambda im: quant(im, 12)),
    ("quant 10", lambda im: quant(im, 10)),
    ("quant 8", lambda im: quant(im, 8)),
    ("q8 +\ndespeckle", lambda im: despeckle_lone(quant(im, 8))),
    ("q6 +\ndespeckle", lambda im: despeckle_lone(quant(im, 6))),
]


def tile(img, label):
    c = Image.new("RGBA", (UP, UP + 34), (32, 32, 32, 255))
    c.paste(img.convert("RGBA").resize((UP, UP), Image.NEAREST), (0, 34))
    d = ImageDraw.Draw(c)
    for i, line in enumerate(label.split("\n")):
        d.text((4, 3 + i * 13), line, fill=(235, 235, 235, 255))
    return c


if __name__ == "__main__":
    gap = 8
    cols = len(LADDER)
    rowh = UP + 34 + gap
    sheet = Image.new("RGBA", (gap + cols * (UP + gap), 22 + len(ITEMS) * rowh + gap),
                      (22, 22, 22, 255))
    d = ImageDraw.Draw(sheet)
    y = 22
    for n in ITEMS:
        base = fill_opaque(Image.open(os.path.join(BACKUP, "lunchbox_%d.png" % n)))
        base_rgb = base.convert("RGB")
        d.text((6, y - 16), "lunchbox_%d" % n, fill=(255, 220, 120, 255))
        for ci, (label, fn) in enumerate(LADDER):
            out = fn(base_rgb)
            cnt = ncolors(out)
            out.save(os.path.join(WORK, "lunchbox_%d_%s.png"
                                  % (n, label.split("\n")[0].replace(" ", ""))))
            sheet.paste(tile(out, "%s\n%d cols" % (label.replace("\n", " "), cnt)),
                        (gap + ci * (UP + gap), y))
        y += rowh
    out_path = os.path.join(WORK, "_smooth_ladder.png")
    sheet.save(out_path)
    print("wrote", out_path)
