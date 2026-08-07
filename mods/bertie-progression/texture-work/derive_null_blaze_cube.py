#!/usr/bin/env python3
"""
Null Blaze Cube sprite: Re-Avaritia's Blaze Cube, desaturated.

This is the one texture in this mod that is NOT original art, which is why it lives here
rather than in make_textures.py - that script's promise is that no third-party art is copied,
and it must stay true. Re-Avaritia is MIT (see the mod's NOTICE for the attribution this
requires), so the derivative is allowed as long as the notice travels with it.

The source is a 16x32 two-frame animation with an interpolated 32-tick cycle. Both frames are
desaturated by luminance with alpha untouched, and the .mcmeta is carried across, so the inert
cube keeps the live one's slow pulse in grey.

Run:  python texture-work/derive_null_blaze_cube.py
"""
import os
import zipfile

from PIL import Image

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
TEX_ITEM = os.path.join(ROOT, "src", "main", "resources", "assets", "bertieprogression",
                        "textures", "item")
INSTANCE_MODS = os.path.join(os.environ.get("APPDATA", ""), "PrismLauncher", "instances",
                             "s1 demo", ".minecraft", "mods")
SOURCE = "assets/avaritia/textures/item/resource/blaze/blaze_cube.png"
OUT = "null_blaze_cube"

# Rec. 709 luma. Plain channel averaging turns the cube's orange to a muddy mid-grey and loses
# the shading that makes it read as a cube at 16 pixels.
LUMA = (0.2126, 0.7152, 0.0722)
# Luma alone is not enough: the live cube is a bright yellow, and yellow's luma is near white, so a
# straight desaturation gives a WHITE cube rather than a grey one. Dimming afterwards is what makes
# it read as greyed out. Raise for a paler cube, lower for a darker one; below about 0.6 the
# shading collapses into mud.
DIM = 0.72


def source_jar():
    if not os.path.isdir(INSTANCE_MODS):
        raise SystemExit(f"no synced instance at {INSTANCE_MODS}")
    for name in sorted(os.listdir(INSTANCE_MODS)):
        if name.lower().startswith("re-avaritia") and name.endswith(".jar"):
            return os.path.join(INSTANCE_MODS, name)
    raise SystemExit(f"no Re-Avaritia jar in {INSTANCE_MODS}")


def main():
    jar = source_jar()
    with zipfile.ZipFile(jar) as zf:
        with zf.open(SOURCE) as f:
            src = Image.open(f).convert("RGBA")
        meta = zf.read(SOURCE + ".mcmeta")

    out = Image.new("RGBA", src.size, (0, 0, 0, 0))
    sp, op = src.load(), out.load()
    for y in range(src.height):
        for x in range(src.width):
            r, g, b, a = sp[x, y]
            if a == 0:
                continue
            v = min(255, int(round((r * LUMA[0] + g * LUMA[1] + b * LUMA[2]) * DIM)))
            op[x, y] = (v, v, v, a)

    os.makedirs(TEX_ITEM, exist_ok=True)
    out.save(os.path.join(TEX_ITEM, OUT + ".png"))
    with open(os.path.join(TEX_ITEM, OUT + ".png.mcmeta"), "wb") as f:
        f.write(meta)
    print(f"{OUT}.png {out.size[0]}x{out.size[1]} from {os.path.basename(jar)}")


if __name__ == "__main__":
    main()
