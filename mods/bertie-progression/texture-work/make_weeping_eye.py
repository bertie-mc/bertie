#!/usr/bin/env python3
"""
bertieprogression texture generator — weeping_eye (16x16, animated).

The brief: the silhouette of an Eye of Ender, it blinks, and it cries.

    silhouette  the vanilla Eye of Ender outline, row for row. It is the one
                shape in the game that already reads as "thrown locator", and
                this item is a locator (Malum's Weeping Well), so the outline
                carries that meaning for free. Only the colouring is ours.
    shell       violet rather than ender green, lit from the upper left, with a
                fixed marble so the orb is not a flat disc. The marble is drawn
                from one seeded table shared by every frame — reroll it per
                frame and the shell boils under the animation.
    iris        a disc with a square pupil and a two-pixel glint. The pupil is
                what makes the blink legible: a lid crossing a marbled orb is a
                shape changing colour, a lid crossing a pupil is an eye closing.
    blink       lids from the top and the bottom, meeting at row 8. They are
                painted in the shell colours, so a shut eye is a plain orb with
                a crease across it, which is what a shut eye looks like. Closing
                takes two frames and opening three — shutting fast and opening
                slow is the half of a blink people actually read.
    tears       two beads on their own 20-frame cycle, half a cycle apart, one
                per side. Each swells on the rim, lets go, runs down the face
                and leaves the frame at a pixel a frame. They are not mirrored —
                the right one sits a row lower and hangs a frame longer —
                because a matched pair beats like a metronome. They are painted
                last, so a tear crosses a shut lid instead of vanishing under it.

                The cycle is also why no frame here can be held: park the
                animation on the open eye and a tear stops in mid-air. The rest
                between blinks is whole tear cycles instead, so the frame count
                has to stay a whole number of them or the loop jumps at the
                wrap. There is an assert on that.

Run:  python texture-work/make_weeping_eye.py [--ascii]
"""
import json
import os
import random
import sys

from PIL import Image

SIZE = 16
NAME = "weeping_eye"

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
TEX_ITEM = os.path.join(ROOT, "src", "main", "resources", "assets", "bertieprogression", "textures", "item")
DRAFTS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "drafts")

# The vanilla Eye of Ender silhouette, as inclusive column spans per row.
BLOB = {
    2: (7, 9),
    3: (5, 11),
    4: (4, 12),
    5: (3, 13),
    6: (3, 13),
    7: (2, 14),
    8: (2, 14),
    9: (2, 14),
    10: (3, 13),
    11: (3, 13),
    12: (4, 12),
    13: (5, 11),
    14: (7, 9),
}

CX = CY = 8  # the shape is symmetric about this pixel on both axes

# Palette. The shell is deliberately kept dark and half-drained of colour: the
# violet has to belong to the iris, or the whole orb is one purple mass and the
# eye inside it disappears.
RIM_LIT = (36, 28, 52)      # the rim where the light reaches it
RIM = (16, 12, 24)          # and where it does not — the darkest thing here
SHELL_HI = (72, 57, 96)
SHELL = (52, 40, 72)
SHELL_LO = (36, 28, 51)
IRIS_RIM = (60, 30, 88)
IRIS = (128, 74, 171)       # weeping_compass's violet, unchanged, so they pair
IRIS_HI = (172, 116, 216)
PUPIL = (16, 11, 22)
BLICK = (238, 234, 255)
CREASE = (20, 14, 30)
TEAR = (176, 202, 230)      # cold, so it never reads as shell breaking off
TEAR_HI = (240, 248, 255)
TEAR_LO = (110, 140, 178)

PUPIL_R = 1.45
IRIS_R = 2.65
IRIS_RIM_R = 3.35
GLINT = ((6, 6), (7, 6))

# Apertures for the blink, as the first and last rows still open. None is shut.
BLINK = [(6, 10), (8, 8), None, (8, 8), (6, 10), (4, 12)]
BLINK_AT = 36               # so the lid lifts onto the left bead welling at f40

TEAR_CYCLE = 20
FRAMES = 60                 # three tear cycles, one blink
# (column, rim row, frames spent welling before it lets go, phase in the cycle)
TEARS = ((5, 10, 4, 0), (11, 11, 5, 10))

MARBLE = {}
_rng = random.Random(20260802)
for _y in range(SIZE):
    for _x in range(SIZE):
        MARBLE[(_x, _y)] = 1.0 + (_rng.random() - 0.5) * 0.16


def shade(c, f):
    return tuple(max(0, min(255, int(round(v * f)))) for v in c)


def opaque(c):
    return (c[0], c[1], c[2], 255)


def blob_pixels():
    return {(x, y) for y, (x0, x1) in BLOB.items() for x in range(x0, x1 + 1)}


BODY = blob_pixels()


def rim_pixels():
    """Body pixels with a neighbour outside the shape — the outline."""
    out = set()
    for (x, y) in BODY:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if (x + dx, y + dy) not in BODY:
                out.add((x, y))
                break
    return out


EDGE = rim_pixels()


def shell_at(x, y):
    """Shell colour for one pixel: an upper-left light, then the fixed marble."""
    t = (x - CX) + (y - CY)
    base = SHELL_HI if t <= -4 else (SHELL if t <= 1 else SHELL_LO)
    return shade(base, MARBLE[(x, y)])


def tear_frames(x, y0, swell):
    """One bead's whole life, frame by frame, as {pixel: colour}.

    It grows in place on the rim, then the head steps down a row a frame with
    the tail trailing it, until both are off the bottom of the sprite.
    """
    out = [{(x, y0): TEAR_LO}, {(x, y0): TEAR}]
    for _ in range(swell - 2):
        out.append({(x, y0): TEAR_HI, (x, y0 + 1): TEAR})
    for head in range(y0, SIZE + 1):
        frame = {(x, head): TEAR_HI, (x, head + 1): TEAR}
        frame = {p: c for p, c in frame.items() if p[1] < SIZE}
        if frame:
            out.append(frame)
    assert len(out) <= TEAR_CYCLE, "tear at x=%d outlives its cycle" % x
    return out + [{}] * (TEAR_CYCLE - len(out))


TEAR_LIFE = {x: tear_frames(x, y0, swell) for x, y0, swell, _ in TEARS}


def aperture(frame):
    """The open rows this frame, or None while the eye is shut."""
    i = frame - BLINK_AT
    if 0 <= i < len(BLINK):
        return BLINK[i]
    return (min(BLOB), max(BLOB))


def paint(frame):
    grid = {}

    # Shell first, everywhere. The lids are the same material as the orb, so
    # painting the whole body and then creasing it is the lid.
    for (x, y) in BODY:
        grid[(x, y)] = shell_at(x, y)
    for (x, y) in EDGE:
        grid[(x, y)] = RIM_LIT if (x - CX) + (y - CY) <= -3 else RIM

    open_rows = aperture(frame)
    if open_rows is not None:
        top, bot = open_rows
        for (x, y) in BODY:
            if not top <= y <= bot:
                continue
            d = ((x - CX) ** 2 + (y - CY) ** 2) ** 0.5
            if d <= PUPIL_R:
                # The corners of the three-by-three go to a near-black violet
                # rather than to the pupil proper, which is the whole of what
                # makes a square pupil read as a round one at this size.
                grid[(x, y)] = shade(IRIS, 0.34) if d > 1.1 else PUPIL
            elif d <= IRIS_R:
                # Shadowed under the upper lid, brightest at the bottom, the way
                # any iris sitting under a brow is. Flat, it reads as a sticker.
                tone = IRIS_HI if y > CY else (shade(IRIS, 0.76) if y < CY - 1 else IRIS)
                grid[(x, y)] = shade(tone, MARBLE[(x, y)])
            elif d <= IRIS_RIM_R:
                grid[(x, y)] = shade(IRIS_RIM, MARBLE[(x, y)])
        for p in GLINT:
            if top <= p[1] <= bot:
                grid[p] = BLICK
        # The lash line, and one lifted row behind it so the lid has a body.
        for edge, back in ((top - 1, top - 2), (bot + 1, bot + 2)):
            for (x, y) in BODY:
                if y == edge and (x, y) not in EDGE:
                    grid[(x, y)] = CREASE
                elif y == back and (x, y) not in EDGE:
                    grid[(x, y)] = shade(shell_at(x, y), 1.18)
    else:
        for (x, y) in BODY:
            if y == CY and (x, y) not in EDGE:
                grid[(x, y)] = CREASE
            elif y == CY - 1 and (x, y) not in EDGE:
                grid[(x, y)] = shade(shell_at(x, y), 1.18)

    # Tears last: one running over a shut lid is right, one disappearing under
    # it is not.
    for x, _, _, phase in TEARS:
        for p, c in TEAR_LIFE[x][(frame + phase) % TEAR_CYCLE].items():
            grid[p] = c

    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()
    for p, c in grid.items():
        px[p] = opaque(c)
    return img


def dump(img):
    """Rough ASCII of one frame, for checking the silhouette by eye."""
    px = img.load()
    for y in range(SIZE):
        row = ""
        for x in range(SIZE):
            r, g, b, a = px[x, y]
            row += "." if a == 0 else " #+=-o*"[min(int((r + g + b) / 3 / 255 * 6) + 1, 6)]
        print(row)


def build_strip(frames):
    strip = Image.new("RGBA", (SIZE, SIZE * len(frames)), (0, 0, 0, 0))
    for i, frame in enumerate(frames):
        strip.paste(frame, (0, i * SIZE))
    return strip


def build_preview(frames, scale=8):
    """An inventory-slot-grey GIF at 100ms a frame — frametime 2, as it runs."""
    slot = (139, 139, 139, 255)
    out = []
    for frame in frames:
        flat = Image.new("RGBA", (SIZE, SIZE), slot)
        flat.alpha_composite(frame)
        out.append(flat.convert("RGB").resize((SIZE * scale, SIZE * scale), Image.NEAREST))
    return out


if __name__ == "__main__":
    assert FRAMES % TEAR_CYCLE == 0, "frame count must be whole tear cycles"
    assert BLINK_AT + len(BLINK) <= FRAMES, "the blink runs off the end of the loop"

    frames = [paint(i) for i in range(FRAMES)]

    os.makedirs(TEX_ITEM, exist_ok=True)
    build_strip(frames).save(os.path.join(TEX_ITEM, NAME + ".png"))
    with open(os.path.join(TEX_ITEM, NAME + ".png.mcmeta"), "w", newline="\n") as fh:
        json.dump({"animation": {"frametime": 2}}, fh, indent=2)
        fh.write("\n")
    print("wrote item/%s.png (%d frames) and its .mcmeta" % (NAME, len(frames)))

    os.makedirs(DRAFTS, exist_ok=True)
    preview = build_preview(frames)
    preview[0].save(os.path.join(DRAFTS, NAME + "_preview.gif"), save_all=True,
                    append_images=preview[1:], duration=100, loop=0, optimize=False)
    print("wrote drafts/%s_preview.gif" % NAME)

    if "--ascii" in sys.argv:
        dump(frames[0])
