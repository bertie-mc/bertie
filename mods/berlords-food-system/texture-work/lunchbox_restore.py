"""Natural colorization of berlord's 3 grayscale box renders.

The sources are effectively black-and-white (measured mean chroma ~2-4/255;
the only real colour is the red tab on the steel box). This treats each as a
B&W photo and grades it back to a believable METAL look -- cool gunmetal in the
shadows, neutral->warm steel through the mid-tones and highlights, brass on the
warm latch/tab. No invented two-tone paint; just "what this metal object would
look like in colour." One natural image per source + a combined sheet.
"""
import os
import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "out")
SRC_DIR = r"C:\Users\berlord\Documents\StabilityMatrix-win-x64\Data\Images\Text2Img\2026-06-14"

SOURCES = {
    "domed":   "00035-3606144610.png",
    "tab":     "00016-1430971589.png",
    "slotted": "00000-3998877134.png",
}

# metal split-tone: cool dark gunmetal -> steel -> warm specular
METAL = [(0.00, (36, 42, 54)), (0.30, (92, 100, 112)), (0.55, (150, 152, 150)),
         (0.78, (208, 204, 192)), (1.00, (250, 247, 238))]
BRASS = [(0.00, (60, 44, 16)), (0.50, (156, 120, 44)), (1.00, (244, 212, 120))]


def ramp(L, stops):
    pos = [s[0] for s in stops]
    out = np.empty(L.shape + (3,), np.float32)
    for c in range(3):
        out[..., c] = np.interp(L, pos, [s[1][c] for s in stops])
    return out


def restore(src_path):
    im = Image.open(src_path).convert("RGB")
    rgb = np.array(im).astype(np.float32)
    L = (0.299 * rgb[..., 0] + 0.587 * rgb[..., 1] + 0.114 * rgb[..., 2]) / 255.0
    out = ramp(L, METAL)
    warm = (rgb[..., 0] - rgb[..., 2] > 18) & (rgb[..., 0] > 95)      # the red tab -> brass
    out[warm] = ramp(L, BRASS)[warm]
    return Image.fromarray(np.clip(out, 0, 255).astype(np.uint8), "RGB")


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    items = list(SOURCES.items())
    CELL, GAP, TY = 240, 14, 26
    W = len(items) * CELL + (len(items) - 1) * GAP + 20
    sheet = Image.new("RGB", (W, TY + CELL + 14), (245, 245, 245))
    draw = ImageDraw.Draw(sheet)
    for i, (name, fn) in enumerate(items):
        img = restore(os.path.join(SRC_DIR, fn))
        img.save(os.path.join(OUT, f"lbrestore_{name}.png"))
        x = 10 + i * (CELL + GAP)
        draw.text((x + 4, 8), name, fill=(30, 30, 30))
        sheet.paste(img.resize((CELL, CELL), Image.LANCZOS), (x, TY))
    sheet.save(os.path.join(OUT, "_lunchbox_restore.png"))
    print("wrote", len(items), "restored boxes + _lunchbox_restore.png")
