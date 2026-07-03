"""Generate SHARPER 16x16 variants of the lunchbox candidates.

Re-downscales from the 512px sources (sharpening the already-soft 16px output
is strictly worse) using several techniques, so each candidate gets a row of
options to compare. All spatial sharpening happens in the PREMULTIPLIED-alpha
domain so silhouette edges never grow a dark/white halo.

Variants per image:
  v0_soft          - plain LANCZOS premult (the previous result, as reference)
  v1_unsharp       - + UnsharpMask(1.0, 120%, 2)            (clean crisp)
  v2_unsharp_strong- + UnsharpMask(1.6, 210%, 1)            (aggressive)
  v3_supersample   - source->48 unsharp ->16 unsharp        (detail-preserving)
  v4_pop           - v1 + contrast x1.15 + saturation x1.30 (Minecraft punch)
  v5_quant         - v1 quantized to 22 colors              (hand-pixel block look)

Outputs to <SRC>/sharpened_16/:
  <name>/<variant>.png         16x16 texture
  <name>/<variant>_8x.png      ~144px NEAREST preview
  <name>_compare.png           labelled side-by-side row for that candidate
  _master_compare.png          all 8 rows stacked
Run:  python lunchbox_sharpen.py
"""
import glob
import os

import numpy as np
from PIL import Image, ImageFilter, ImageEnhance, ImageDraw

from lunchbox_downscale import remove_white_bg, crop_square, SRC

OUT = os.path.join(SRC, "sharpened_16")
os.makedirs(OUT, exist_ok=True)

UP = 144  # preview upscale size


# ---- premultiplied-alpha resampling (halo-free) --------------------------
def to_premult(im):
    a = np.asarray(im.convert("RGBA"), dtype=np.float64)
    a[..., :3] *= a[..., 3:4] / 255.0
    return a


def premult_resize(arr, size):
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8)).resize(
        (size, size), Image.LANCZOS)


def unpremult(im):
    a = np.asarray(im, dtype=np.float64)
    al = a[..., 3:4] / 255.0
    a[..., :3] = np.clip(a[..., :3] / np.where(al > 0, al, 1.0), 0, 255)
    return Image.fromarray(a.astype(np.uint8))


def split_alpha(im):
    a = np.asarray(im)
    return Image.fromarray(a[..., :3], "RGB"), a[..., 3]


def join_alpha(rgb, alpha):
    return Image.fromarray(np.dstack([np.asarray(rgb.convert("RGB")), alpha]), "RGBA")


# ---- variant builders (input: premultiplied float array of cleaned source)
def v_soft(pre):
    return unpremult(premult_resize(pre, 16))


def v_unsharp(pre, r=1.0, pct=120, th=2):
    im = premult_resize(pre, 16).filter(ImageFilter.UnsharpMask(r, pct, th))
    return unpremult(im)


def v_supersample(pre):
    im = premult_resize(pre, 48).filter(ImageFilter.UnsharpMask(1.4, 150, 2))
    im = np.asarray(im, dtype=np.float64)
    im = premult_resize(im, 16).filter(ImageFilter.UnsharpMask(0.8, 90, 1))
    return unpremult(im)


def v_pop(pre):
    base = v_unsharp(pre)
    rgb, alpha = split_alpha(base)
    rgb = ImageEnhance.Color(rgb).enhance(1.30)
    rgb = ImageEnhance.Contrast(rgb).enhance(1.15)
    return join_alpha(rgb, alpha)


def v_quant(pre, n=22):
    base = v_unsharp(pre)
    rgb, alpha = split_alpha(base)
    q = rgb.quantize(colors=n, method=Image.MEDIANCUT, dither=Image.NONE).convert("RGB")
    return join_alpha(q, alpha)


VARIANTS = [
    ("v0_soft", v_soft),
    ("v1_unsharp", lambda p: v_unsharp(p, 1.0, 120, 2)),
    ("v2_unsharp_strong", lambda p: v_unsharp(p, 1.6, 210, 1)),
    ("v3_supersample", v_supersample),
    ("v4_pop", v_pop),
    ("v5_quant", v_quant),
]


def checker(size, c1=(150, 150, 150), c2=(110, 110, 110), cell=8):
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            px[x, y] = (*(c1 if (x // cell + y // cell) % 2 == 0 else c2), 255)
    return img


def labelled(tex_or_img, label, src=False):
    cell = UP
    canvas = Image.new("RGBA", (cell, cell + 18), (32, 32, 32, 255))
    if src:
        thumb = tex_or_img.convert("RGBA").resize((cell, cell), Image.LANCZOS)
        canvas.paste(thumb, (0, 18))
    else:
        ck = checker(cell)
        ck.alpha_composite(tex_or_img.resize((cell, cell), Image.NEAREST))
        canvas.paste(ck, (0, 18))
    d = ImageDraw.Draw(canvas)
    d.text((3, 4), label, fill=(235, 235, 235, 255))
    return canvas


if __name__ == "__main__":
    files = sorted(glob.glob(os.path.join(SRC, "*.png")))
    rows = []
    for f in files:
        name = os.path.splitext(os.path.basename(f))[0]
        d = os.path.join(OUT, name)
        os.makedirs(d, exist_ok=True)
        src_im = Image.open(f)
        cleaned = crop_square(remove_white_bg(src_im))   # full-res, bg removed, squared
        pre = to_premult(cleaned)

        cells = [labelled(src_im, "source", src=True)]
        for vname, fn in VARIANTS:
            tex = fn(pre)
            tex.save(os.path.join(d, vname + ".png"))
            tex.resize((UP, UP), Image.NEAREST).save(os.path.join(d, vname + "_8x.png"))
            cells.append(labelled(tex, vname))

        gap, top = 8, 18
        row = Image.new("RGBA", (sum(c.width for c in cells) + gap * (len(cells) + 1),
                                 UP + top + gap * 2), (24, 24, 24, 255))
        x = gap
        for c in cells:
            row.paste(c, (x, gap))
            x += c.width + gap
        row.save(os.path.join(OUT, name + "_compare.png"))
        rows.append((name, row))
        print("wrote", name + "_compare.png")

    W = max(r.width for _, r in rows)
    H = sum(r.height for _, r in rows) + 30 * len(rows)
    master = Image.new("RGBA", (W, H), (16, 16, 16, 255))
    y = 0
    md = ImageDraw.Draw(master)
    for name, r in rows:
        md.text((6, y + 8), name, fill=(255, 220, 120, 255))
        master.paste(r, (0, y + 26))
        y += r.height + 30
    master.save(os.path.join(OUT, "_master_compare.png"))
    print("wrote _master_compare.png ->", OUT)
