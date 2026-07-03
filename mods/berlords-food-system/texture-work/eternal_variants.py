"""Eternal Seasoning rework candidates — preview only, no deploy.

Palette derived from the tooltip's ETERNAL_COLOR 0xA8A0D8 (desaturated cold lavender).
"""
import os
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
VANILLA = os.path.join(HERE, "vanilla")
OUT = os.path.join(HERE, "out")

# lavender ramp around #A8A0D8
W = (242, 240, 252)    # near-white core
L = (206, 200, 238)    # light
P = (168, 160, 216)    # 0xA8A0D8 exact
M = (126, 117, 178)    # mid-dark
D = (88, 79, 136)      # dark
O = (56, 48, 92)       # outline
G = (255, 216, 74)     # bright gold glitter
g = (212, 164, 40)     # dark gold glitter


def blank():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def put(img, pixels):
    for (x, y), c in pixels.items():
        img.putpixel((x, y), (*c, 255))


# ---- A: custom flame wisp (blaze-powder dynamic, own silhouette) ----
def variant_a():
    img = blank()
    body = {
        # floating sparks
        (9, 1): L, (11, 3): G, (2, 5): D, (12, 5): M, (13, 8): D,
        # curling tip
        (5, 3): M,
        (5, 4): P, (6, 4): M,
        (4, 5): M, (5, 5): L, (6, 5): P,
        # upper body, leaning left
        (4, 6): P, (5, 6): L, (6, 6): P, (7, 6): M,
        (4, 7): P, (5, 7): W, (6, 7): L, (7, 7): P,
        (3, 8): M, (4, 8): P, (5, 8): W, (6, 8): L, (7, 8): P, (8, 8): M,
        # right-side lick sweeping out
        (10, 7): D, (10, 8): M,
        # lower body widening right
        (3, 9): M, (4, 9): L, (5, 9): W, (6, 9): L, (7, 9): P, (8, 9): P, (9, 9): M, (10, 9): D,
        (3, 10): D, (4, 10): P, (5, 10): W, (6, 10): W, (7, 10): L, (8, 10): P, (9, 10): M, (10, 10): M, (11, 10): D,
        (3, 11): D, (4, 11): M, (5, 11): L, (6, 11): W, (7, 11): L, (8, 11): P, (9, 11): P, (10, 11): M, (11, 11): O,
        (4, 12): O, (5, 12): M, (6, 12): P, (7, 12): P, (8, 12): M, (9, 12): M, (10, 12): O,
        (5, 13): O, (6, 13): D, (7, 13): D, (8, 13): D, (9, 13): O,
    }
    put(img, body)
    # gold glitter on the body
    put(img, {(8, 10): G, (7, 12): g, (4, 8): g})
    return img


# ---- B: vanilla blaze_powder luminance-remapped into the lavender ramp ----
def variant_b():
    src = Image.open(os.path.join(VANILLA, "blaze_powder.png")).convert("RGBA")
    img = blank()
    for y in range(16):
        for x in range(16):
            p = src.getpixel((x, y))
            if p[3] == 0:
                continue
            lum = (p[0] + p[1] + p[2]) // 3
            if lum >= 220:
                c = W
            elif lum >= 160:
                c = L
            elif lum >= 125:
                c = P
            elif lum >= 95:
                c = M
            elif lum >= 70:
                c = D
            else:
                c = O
            img.putpixel((x, y), (*c, 255))
    put(img, {(4, 10): G, (7, 11): g, (10, 10): G, (9, 12): g})
    return img


if __name__ == "__main__":
    variants = {"A_custom_wisp": variant_a(), "B_blaze_remap": variant_b()}
    sheet = Image.new("RGBA", (128 * 2 + 16, 128), (40, 40, 40, 255))
    for i, (name, img) in enumerate(variants.items()):
        img.save(os.path.join(OUT, f"eternal_{name}.png"))
        sheet.paste(img.resize((128, 128), Image.NEAREST), (i * 144, 0))
    sheet.save(os.path.join(OUT, "_eternal_variants.png"))
    print("ok")
