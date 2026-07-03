"""Assemble the judged sharpest-but-clean picks into _final/ + a before/after sheet."""
import glob
import os
import shutil

from PIL import Image, ImageDraw

from lunchbox_downscale import SRC

BASE = os.path.join(SRC, "sharpened_16")
FINAL = os.path.join(BASE, "_final")
os.makedirs(FINAL, exist_ok=True)
UP = 160

PICKS = {
    "00000-3998877134": "v3_supersample",
    "00006-2940843621": "v3_supersample",
    "00016-1430971589": "v1_unsharp",
    "00055-1066272128": "v3_supersample",
    "00057-673318511": "v3_supersample",
    "00090-2288709805": "v1_unsharp",
    "00256-3371761933": "v1_unsharp",
    "00337-2525756876": "v3_supersample",
}


def checker(size, c1=(150, 150, 150), c2=(110, 110, 110), cell=8):
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            px[x, y] = (*(c1 if (x // cell + y // cell) % 2 == 0 else c2), 255)
    return img


def cell(img, label, src=False):
    c = Image.new("RGBA", (UP, UP + 20), (32, 32, 32, 255))
    if src:
        c.paste(img.convert("RGBA").resize((UP, UP), Image.LANCZOS), (0, 20))
    else:
        ck = checker(UP)
        ck.alpha_composite(img.resize((UP, UP), Image.NEAREST))
        c.paste(ck, (0, 20))
    ImageDraw.Draw(c).text((3, 5), label, fill=(235, 235, 235, 255))
    return c


rows = []
for name, pick in PICKS.items():
    src_im = Image.open(os.path.join(SRC, name + ".png"))
    soft = Image.open(os.path.join(BASE, name, "v0_soft.png"))
    new = Image.open(os.path.join(BASE, name, pick + ".png"))
    shutil.copy(os.path.join(BASE, name, pick + ".png"),
                os.path.join(FINAL, name + "_16.png"))
    new.resize((UP, UP), Image.NEAREST).save(os.path.join(FINAL, name + "_16x10.png"))

    cells = [cell(src_im, "source", src=True),
             cell(soft, "old (soft)"),
             cell(new, "NEW " + pick)]
    gap = 8
    row = Image.new("RGBA", (sum(c.width for c in cells) + gap * (len(cells) + 1),
                             UP + 20 + gap * 2), (24, 24, 24, 255))
    x = gap
    for c in cells:
        row.paste(c, (x, gap))
        x += c.width + gap
    rows.append((name, row))

W = max(r.width for _, r in rows)
H = sum(r.height for _, r in rows) + 26 * len(rows)
sheet = Image.new("RGBA", (W, H), (16, 16, 16, 255))
y = 0
d = ImageDraw.Draw(sheet)
for name, r in rows:
    d.text((6, y + 7), name + "  ->  " + PICKS[name], fill=(255, 220, 120, 255))
    sheet.paste(r, (0, y + 22))
    y += r.height + 26
sheet.save(os.path.join(FINAL, "_before_after.png"))
print("wrote", len(PICKS), "picks +_before_after.png ->", FINAL)
