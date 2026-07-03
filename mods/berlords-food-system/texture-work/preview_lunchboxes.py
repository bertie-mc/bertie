"""Rich preview sheets for the wood + metal lunchbox 16x16 textures.

Generates four focused, easy-to-read PNGs in out/:
  _zoom_lunchboxes.png        big clean 16x side-by-side (judge the art)
  _backgrounds_lunchboxes.png dark / light / checker contrast matrix
  _ingame_lunchboxes.png      vanilla inventory slots at 2x..6x + a hotbar
  _grid_lunchboxes.png        12x with a per-pixel grid (editing reference)

Reads the already-generated out/lunchbox_wood.png and out/lunchbox_metal.png.
Run:  python preview_lunchboxes.py
"""
import os
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "out")

BG = (38, 38, 42, 255)          # sheet background
PANEL = (50, 50, 56, 255)       # inset panel behind a zoom
BORDER = (96, 96, 104, 255)
TXT = (236, 236, 240, 255)
SUB = (176, 178, 188, 255)


def _font(size):
    for name in ("arialbd.ttf", "arial.ttf", "DejaVuSans.ttf"):
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            continue
    return ImageFont.load_default()


F = _font(15)
FS = _font(12)


def load16(name):
    return Image.open(os.path.join(OUT, name)).convert("RGBA")


def zoom(img, sc):
    return img.resize((img.width * sc, img.height * sc), Image.NEAREST)


def checker(w, h, sz=8, c1=(206, 206, 210, 255), c2=(168, 168, 174, 255)):
    im = Image.new("RGBA", (w, h), c1)
    d = ImageDraw.Draw(im)
    for y in range(0, h, sz):
        for x in range(0, w, sz):
            if ((x // sz) + (y // sz)) % 2:
                d.rectangle([x, y, x + sz - 1, y + sz - 1], fill=c2)
    return im


def text_img(s, font, fill=TXT, pad=4):
    tmp = ImageDraw.Draw(Image.new("RGBA", (4, 4)))
    b = tmp.textbbox((0, 0), s, font=font)
    w, h = b[2] - b[0], b[3] - b[1]
    im = Image.new("RGBA", (w + 2 * pad, h + 2 * pad), (0, 0, 0, 0))
    ImageDraw.Draw(im).text((pad - b[0], pad - b[1]), s, font=font, fill=fill)
    return im


def hconcat(imgs, gap, valign="top", bgc=(0, 0, 0, 0)):
    w = sum(i.width for i in imgs) + gap * (len(imgs) - 1)
    h = max(i.height for i in imgs)
    out = Image.new("RGBA", (w, h), bgc)
    x = 0
    for i in imgs:
        y = 0 if valign == "top" else (h - i.height if valign == "bottom" else (h - i.height) // 2)
        out.alpha_composite(i, (x, y))
        x += i.width + gap
    return out


def vconcat(imgs, gap, halign="center", bgc=(0, 0, 0, 0)):
    w = max(i.width for i in imgs)
    h = sum(i.height for i in imgs) + gap * (len(imgs) - 1)
    out = Image.new("RGBA", (w, h), bgc)
    y = 0
    for i in imgs:
        x = 0 if halign == "left" else (w - i.width) // 2
        out.alpha_composite(i, (x, y))
        y += i.height + gap
    return out


def framed(inner, bgc=PANEL, border=BORDER, m=10):
    w, h = inner.width + 2 * m, inner.height + 2 * m
    im = Image.new("RGBA", (w, h), bgc)
    im.alpha_composite(inner, (m, m))
    ImageDraw.Draw(im).rectangle([0, 0, w - 1, h - 1], outline=border)
    return im


def labeled(inner, label, sub=None):
    parts = [inner, text_img(label, F)]
    if sub:
        parts.append(text_img(sub, FS, fill=SUB))
    return vconcat(parts, gap=4)


def slot(item, sc, pad=1):
    S = 16 + 2 * pad
    ui = Image.new("RGBA", (S, S), (139, 139, 139, 255))
    d = ImageDraw.Draw(ui)
    d.line([(0, 0), (S - 1, 0)], fill=(85, 85, 85, 255))       # top shadow
    d.line([(0, 0), (0, S - 1)], fill=(85, 85, 85, 255))       # left shadow
    d.line([(0, S - 1), (S - 1, S - 1)], fill=(255, 255, 255, 255))  # bottom hi
    d.line([(S - 1, 0), (S - 1, S - 1)], fill=(255, 255, 255, 255))  # right hi
    ui.alpha_composite(item, (pad, pad))
    return ui.resize((S * sc, S * sc), Image.NEAREST)


def stack(blocks, pad=22, gap=22):
    w = max(b.width for b in blocks) + 2 * pad
    h = pad + sum(b.height for b in blocks) + gap * (len(blocks) - 1) + pad
    canvas = Image.new("RGBA", (w, h), BG)
    y = pad
    for b in blocks:
        canvas.alpha_composite(b, ((w - b.width) // 2, y))
        y += b.height + gap
    return canvas


def save(canvas, name):
    canvas.convert("RGBA").save(os.path.join(OUT, name))
    print("wrote out/" + name)


if __name__ == "__main__":
    wood = load16("lunchbox_wood.png")
    metal = load16("lunchbox_metal.png")
    items = [("Wooden", wood), ("Metal", metal)]

    # ---- 1) big clean zoom, side by side --------------------------------
    heroes = [labeled(framed(zoom(im, 16)), name, "16x16  ·  16x zoom")
              for name, im in items]
    save(stack([text_img("Lunchboxes — detail (16x)", F), hconcat(heroes, gap=28)]),
         "_zoom_lunchboxes.png")

    # ---- 2) background contrast matrix (dark / light / checker) ---------
    SC = 9
    z = 16 * SC
    backgrounds = [("on dark", (40, 40, 44, 255)),
                   ("on light", (224, 224, 226, 255)),
                   ("on checker", None)]
    header = hconcat([text_img(" ", F)] +
                     [vconcat([text_img(b[0], FS, fill=SUB)], gap=0) for b in backgrounds],
                     gap=18)
    rows = []
    for name, im in items:
        cells = [vconcat([text_img(name, F)], gap=0)]
        for bname, bcol in backgrounds:
            bg = checker(z + 20, z + 20) if bcol is None else Image.new("RGBA", (z + 20, z + 20), bcol)
            bg.alpha_composite(zoom(im, SC), (10, 10))
            ImageDraw.Draw(bg).rectangle([0, 0, bg.width - 1, bg.height - 1], outline=BORDER)
            cells.append(bg)
        rows.append(hconcat(cells, gap=18, valign="center"))
    save(stack([text_img("Lunchboxes — background contrast", F)] + rows),
         "_backgrounds_lunchboxes.png")

    # ---- 3) in-game: vanilla inventory slots + a hotbar -----------------
    slot_rows = []
    for name, im in items:
        chips = [vconcat([slot(im, s), text_img(f"{s}x", FS, fill=SUB)], gap=3)
                 for s in (2, 3, 4, 6)]
        slot_rows.append(labeled(hconcat(chips, gap=16, valign="bottom"), name))
    # mini hotbar (scale 3) with wood + metal placed among empty slots
    HB = 3
    empty = slot(Image.new("RGBA", (16, 16), (0, 0, 0, 0)), HB)
    seq = [empty, slot(wood, HB), empty, slot(metal, HB), empty]
    pw = sum(s.width for s in seq) + 6
    bar = Image.new("RGBA", (pw, seq[0].height + 6), (198, 198, 198, 255))
    x = 3
    for s in seq:
        bar.alpha_composite(s, (x, 3)); x += s.width
    ImageDraw.Draw(bar).rectangle([0, 0, pw - 1, bar.height - 1], outline=(70, 70, 70, 255))
    save(stack([text_img("Lunchboxes — inventory slot (true in-game scale)", F)]
               + slot_rows + [labeled(bar, "hotbar mock (3x)")]),
         "_ingame_lunchboxes.png")

    # ---- 4) pixel grid (editing reference) ------------------------------
    GSC = 12
    grids = []
    for name, im in items:
        g = Image.new("RGBA", (16 * GSC, 16 * GSC), (44, 44, 48, 255))
        g.alpha_composite(zoom(im, GSC), (0, 0))
        d = ImageDraw.Draw(g)
        for i in range(0, 16 * GSC + 1, GSC):
            d.line([(i, 0), (i, 16 * GSC)], fill=(255, 255, 255, 46))
            d.line([(0, i), (16 * GSC, i)], fill=(255, 255, 255, 46))
        ImageDraw.Draw(g).rectangle([0, 0, g.width - 1, g.height - 1], outline=BORDER)
        grids.append(labeled(g, name, "12x · pixel grid"))
    save(stack([text_img("Lunchboxes — pixel grid", F), hconcat(grids, gap=28)]),
         "_grid_lunchboxes.png")

    print("done")
