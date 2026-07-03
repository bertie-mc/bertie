"""Clean + sharpen lunchbox 4/5/7 textures: kill the semi-transparent fringe.

For each: snap alpha to binary (no partial pixels left), denoise stray/lone
pixels + fill 1px holes, recolour washed-out edge pixels by bleeding the
confident (high-alpha) colours outward, then a mild pop + light unsharp.
The genuine background stays fully empty so they remain usable item sprites.

Backs up the originals, writes the cleaned versions, and renders an OLD|NEW
comparison sheet (3 rows = lunchbox 4,5,7).
Run:  python lunchbox_clean.py
"""
import os
import shutil

import numpy as np
from PIL import Image, ImageFilter, ImageEnhance, ImageDraw

MOD = (r"C:\Users\berlord\Documents\claude_base\berlords-texture-test"
       r"\src\main\resources\assets\berlords_texture_test\textures\item")
BACKUP = r"C:\Users\berlord\Documents\claude_base\berlords-texture-test\texture-backup"
WORK = os.path.join(os.path.dirname(os.path.abspath(__file__)), "clean_work")
os.makedirs(BACKUP, exist_ok=True)
os.makedirs(WORK, exist_ok=True)

ITEMS = [4, 5, 7]
THRESH = {4: 110, 5: 110, 7: 110}   # alpha cutoff for opaque
UP = 160


def n4_opaque(mask):
    p = np.pad(mask.astype(int), 1)
    return p[:-2, 1:-1] + p[2:, 1:-1] + p[1:-1, :-2] + p[1:-1, 2:]


def edge_bleed(rgb, known, iters=6):
    """Fill !known pixels by averaging known 8-neighbours, iteratively."""
    rgb = rgb.copy()
    known = known.copy()
    for _ in range(iters):
        if known.all():
            break
        pr = np.pad(rgb, ((1, 1), (1, 1), (0, 0)))
        pk = np.pad(known.astype(float), 1)
        ksum = (pk[:-2, 1:-1] + pk[2:, 1:-1] + pk[1:-1, :-2] + pk[1:-1, 2:] +
                pk[:-2, :-2] + pk[:-2, 2:] + pk[2:, :-2] + pk[2:, 2:])
        rsum = (pr[:-2, 1:-1] + pr[2:, 1:-1] + pr[1:-1, :-2] + pr[1:-1, 2:] +
                pr[:-2, :-2] + pr[:-2, 2:] + pr[2:, :-2] + pr[2:, 2:])
        fill = (~known) & (ksum > 0)
        nz = np.maximum(ksum, 1)
        avg = rsum / nz[..., None]
        rgb[fill] = avg[fill]
        known = known | fill
    return rgb


def edge_despeckle(rgb, opaque, dist=72):
    """Recolour ONLY thin boundary colour-outliers (specks) to neighbour mean.
    Interior pixels (>=5 opaque neighbours) are left untouched so real
    high-contrast detail — bands, seams, gems — is preserved."""
    pr = np.pad(rgb, ((1, 1), (1, 1), (0, 0)))
    po = np.pad(opaque.astype(float), 1)
    prm = pr * po[..., None]

    def s2(p):
        return (p[:-2, :-2] + p[:-2, 1:-1] + p[:-2, 2:] + p[1:-1, :-2] +
                p[1:-1, 2:] + p[2:, :-2] + p[2:, 1:-1] + p[2:, 2:])
    osum = s2(po)
    mean = s2(prm) / np.maximum(osum, 1)[..., None]
    d = np.sqrt(((rgb - mean) ** 2).sum(axis=2))
    repl = opaque & (osum >= 2) & (osum <= 4) & (d > dist)   # thin boundary only
    out = rgb.copy()
    out[repl] = mean[repl]
    return out


def process(img, t):
    a = np.array(img.convert("RGBA"))
    al = a[..., 3]
    rgb = a[..., :3].astype(float)

    opaque = al >= t
    cnt = n4_opaque(opaque)
    opaque = opaque & ~(cnt <= 1)        # drop lone pixels + 1px spurs
    opaque = opaque | ((~opaque) & (cnt >= 3))   # fill pinholes

    # bleed ONLY into the transparent ring (seed = opaque) so opaque colours
    # are preserved exactly; this just gives the unsharp a halo-free border
    bled = edge_bleed(rgb, opaque, iters=2)
    bled = edge_despeckle(bled, opaque)  # kill stray edge specks, keep detail

    im = Image.fromarray(np.clip(bled, 0, 255).astype(np.uint8), "RGB")
    im = ImageEnhance.Color(im).enhance(1.05)
    im = ImageEnhance.Contrast(im).enhance(1.08)
    im = im.filter(ImageFilter.UnsharpMask(0.6, 70, 1))

    out = np.dstack([np.array(im), np.where(opaque, 255, 0).astype(np.uint8)])
    return Image.fromarray(out, "RGBA")


def checker(size, c1=(150, 150, 150), c2=(110, 110, 110), cell=10):
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            px[x, y] = (*(c1 if (x // cell + y // cell) % 2 == 0 else c2), 255)
    return img


def tile(tex, label=None):
    h = UP + (20 if label else 0)
    c = Image.new("RGBA", (UP, h), (32, 32, 32, 255))
    ck = checker(UP)
    ck.alpha_composite(tex.resize((UP, UP), Image.NEAREST))
    c.paste(ck, (0, h - UP))
    if label:
        ImageDraw.Draw(c).text((4, 4), label, fill=(235, 235, 235, 255))
    return c


if __name__ == "__main__":
    pairs = []
    for n in ITEMS:
        src = os.path.join(MOD, "lunchbox_%d.png" % n)
        old = Image.open(src).convert("RGBA")
        shutil.copy(src, os.path.join(BACKUP, "lunchbox_%d.png" % n))   # save original
        new = process(old, THRESH[n])
        new.save(os.path.join(WORK, "lunchbox_%d_new.png" % n))
        old.save(os.path.join(WORK, "lunchbox_%d_old.png" % n))
        a = np.array(new)[..., 3]
        part = ((a > 0) & (a < 255)).sum()
        print("lunchbox_%d: partial-alpha now=%d (was many)" % (n, part))
        pairs.append((n, old, new))

    gap, hdr = 10, 24
    W = gap + UP + gap + UP + gap
    H = hdr + len(pairs) * (UP + 20 + gap) + gap
    sheet = Image.new("RGBA", (W, H), (24, 24, 24, 255))
    d = ImageDraw.Draw(sheet)
    d.text((gap + UP // 2 - 12, 6), "OLD", fill=(255, 220, 120, 255))
    d.text((gap + UP + gap + UP // 2 - 12, 6), "NEW", fill=(255, 220, 120, 255))
    y = hdr
    for n, old, new in pairs:
        sheet.paste(tile(old, "lunchbox_%d" % n), (gap, y))
        sheet.paste(tile(new, "lunchbox_%d" % n), (gap + UP + gap, y))
        y += UP + 20 + gap
    out = os.path.join(WORK, "_old_vs_new.png")
    sheet.save(out)
    print("wrote", out)
