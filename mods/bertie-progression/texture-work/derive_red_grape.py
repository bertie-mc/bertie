#!/usr/bin/env python3
"""
Red grape sprite: Berries And Cherries' green grapes, hue-shifted to crimson.

The pack keeps one grape agriculture - Youkai's Feasts' - but wears Berries And Cherries'
sprites, because those read better at 16 pixels. That works for two of the three colours: the
dark bunch becomes the black grape and the green bunch becomes the white grape, both copied
verbatim. There is no red bunch to copy, and Cocktails Delight's is All Rights Reserved, so the
red one is DERIVED from the green instead. Berries And Cherries is AFL-3.0, which permits a
derivative as long as the attribution in NOTICE travels with it.

Only the berry hue moves. The sprite is a single green family around 81 degrees plus one brown
stem pixel; rotating everything would drag the stem to pink, so the shift is applied to the green
band alone and saturation is lifted slightly - a straight rotation lands on a red that reads muddy
next to the purple of the other two bunches.

Run:  python texture-work/derive_red_grape.py
"""
import colorsys
import os

from PIL import Image

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
CROPS = os.path.join(ROOT, "src", "main", "resources", "assets", "youkaisfeasts",
                     "textures", "item", "crops")
SOURCE = os.path.join(CROPS, "white_grape.png")   # the green bunch, already copied in
OUT = os.path.join(CROPS, "red_grape.png")

# The green berry band, in degrees. Everything outside it - the single brown stem pixel - is left
# exactly as it is.
BAND = (60.0, 110.0)
TARGET_HUE = 355.0 / 360.0
# Red at the source saturation reads washed out against the dark and purple bunches, so lift it.
SATURATION_GAIN = 1.25


def main():
    src = Image.open(SOURCE).convert("RGBA")
    out = Image.new("RGBA", src.size)
    moved = 0
    for x in range(src.width):
        for y in range(src.height):
            r, g, b, a = src.getpixel((x, y))
            if a == 0:
                out.putpixel((x, y), (r, g, b, a))
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if BAND[0] <= h * 360.0 <= BAND[1]:
                s = min(1.0, s * SATURATION_GAIN)
                nr, ng, nb = colorsys.hsv_to_rgb(TARGET_HUE, s, v)
                out.putpixel((x, y), (round(nr * 255), round(ng * 255), round(nb * 255), a))
                moved += 1
            else:
                out.putpixel((x, y), (r, g, b, a))
    out.save(OUT)
    print(f"wrote {os.path.relpath(OUT, ROOT)} ({moved} berry pixels recoloured)")


if __name__ == "__main__":
    main()
