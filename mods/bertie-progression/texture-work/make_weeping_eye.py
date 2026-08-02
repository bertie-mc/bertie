#!/usr/bin/env python3
"""
bertieprogression texture generator — weeping_eye (16x16, animated).

The brief: the silhouette of an Eye of Ender, pastel pink, a core that moves,
and a blink that is not a shutter. Drawn after four eyes already in the pack —
endrem's witch_eye, Cataclysm's cursed_eye and mech_eye, l2complements'
guardian_eye — each of which settles one question this texture was getting
wrong:

    witch_eye     is two tones and a bright ring, nothing else, and still reads
                  as an eye at a glance. It is the argument for keeping the
                  palette short.
    cursed_eye    shuts by having the face close over the iris in fragments,
                  never along a row. Its blink is the reference for this one.
    mech_eye      never holds still — the core pulses and flares even while the
                  shell does nothing. A still core reads as a bead, not an eye.
    guardian_eye  is pale, low-contrast and soft, and survives it by keeping one
                  hard dark rim. That rim is why a pastel item still has a
                  silhouette on a bright inventory slot.

    silhouette    the vanilla Eye of Ender outline, row for row. It is the one
                  shape in the game that already reads as "thrown locator", and
                  this item is a locator (Malum's Weeping Well), so the outline
                  carries that meaning for free. Only the colouring is ours.
    shell         pastel pink, lit from the upper left, over a fixed marble so
                  the orb is not a flat disc. The marble comes from one seeded
                  table shared by every frame — reroll it per frame and the
                  shell boils under the animation.
    iris          a deeper rose disc with a soft-cornered pupil and a two-pixel
                  glint. Everything else here is pale, so the iris is the only
                  saturated thing in the sprite and the eye reads from across
                  the hotbar.
    core          the iris drifts a pixel at a time and holds — a gaze, not a
                  wobble — and the pupil widens and narrows on its own slower
                  cycle. The glint travels with it, because the glint is a
                  corneal reflection and the cornea is the part that turns.
                  Pinning it in place was tried first and it spends a third of
                  the loop stranded on pale shell, where a white pixel does not
                  exist. The drift still reads as looking rather than as the
                  sprite sliding, because the shell, rim, lash and tears all
                  stay put. One re-fixation is hidden inside the blink, which is
                  where eyes actually do it.
    blink         the opening is an ellipse that shrinks, so both lid edges are
                  arcs and the aperture narrows to points at the corners. The
                  centre of that ellipse sinks as it shuts, because the upper
                  lid does most of the travelling; that asymmetry is most of
                  what separates a blink from a shutter. The shut frame is not
                  drawn as a row either — it is the last sliver of the same
                  ellipse, so the seam carries the same curve the lids had.
    tears         two beads on their own 20-frame cycle, half a cycle apart, one
                  per side. Each swells on the rim, lets go, runs down the face
                  and leaves the frame at a pixel a frame. They are not mirrored
                  — the right one sits a row lower and hangs a frame longer —
                  because a matched pair beats like a metronome. They are
                  painted last, so a tear crosses a shut lid instead of
                  vanishing under it.

                  The cycle is also why no frame here can be held: park the
                  animation on the open eye and a tear stops in mid-air. The
                  rest between blinks is whole tear cycles instead, so the frame
                  count has to stay a whole number of them or the loop jumps at
                  the wrap. There is an assert on that.

Run:  python texture-work/make_weeping_eye.py [--ascii]
"""
import json
import math
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

# Palette: pastel pink throughout, and the whole range is spent on one hue.
# The shell takes the pale end and the iris the deep end — a pastel eye has no
# contrast to spare, so none of it goes on a second colour.
RIM_LIT = (198, 142, 170)   # the rim where the light reaches it
RIM = (126, 70, 100)        # and where it does not — the silhouette lives here
SHELL_HI = (252, 226, 236)
SHELL = (243, 203, 219)
SHELL_LO = (222, 172, 195)
IRIS_HI = (234, 148, 182)   # the lower iris, lit
IRIS = (216, 124, 162)
IRIS_DK = (188, 98, 138)    # the upper iris, in the lid's shadow
IRIS_RIM = (142, 72, 108)
PUPIL = (72, 32, 56)
BLICK = (255, 252, 254)
CREASE = (140, 80, 114)
TEAR = (247, 235, 246)      # a shade cooler than the shell, or it disappears
TEAR_HI = (255, 253, 255)
TEAR_LO = (212, 186, 210)

IRIS_R = 2.65
IRIS_RIM_R = 3.35
PUPIL_R = 1.5               # a fixed three-by-three footprint
# The pupil widens by its corners darkening, not by its footprint growing. There
# is no size between a five-pixel plus and a three-by-three block at this scale,
# and the plus reads as a cross rather than as a pupil.
PUPIL_CORNER_TIGHT = 0.70   # constricted: the corners are nearly iris
PUPIL_CORNER_WIDE = 0.46    # dilated: dark, but never as dark as the pupil, or
PUPIL_CYCLE = 30            # the whole thing is a solid square again
GLINT = ((6, 6), (7, 6))

# Where the eye is looking, as (last frame of the hold, iris offset). The change
# at 38 lands while the lid is shut, so it opens looking somewhere else.
GAZE = ((13, (0, 0)), (24, (-1, 0)), (32, (-1, 1)), (38, (0, 1)),
        (49, (1, 0)), (56, (1, -1)), (60, (0, 0)))

# How far shut the eye is, frame by frame: one half-lidded frame, shut, then a
# slower lift. There is deliberately no step above 0.5 on the way down — the
# aperture there is a two-row slit, the iris breaks into loose dark pixels
# inside it, and the frame reads as a grimace.
BLINK = (0.5, 1.0, 1.0, 0.72, 0.42, 0.18)
BLINK_AT = 36
LENS_W = 6.8                # wide enough that a wide-open lens hides nothing
LENS_H = 6.4
LENS_PINCH = 0.35           # the corners come in as it shuts, so it stays almond
LID_DROP = 1.8              # the lids meet below centre, not across the middle
SHUT_SEAM = 0.94            # the shut seam is this sliver of the same ellipse

TEAR_CYCLE = 20
FRAMES = 60                 # three tear cycles, one blink
# (column, rim row, frames spent welling before it lets go, phase in the cycle)
TEARS = ((5, 10, 4, 0), (11, 11, 5, 10))

MARBLE = {}
_rng = random.Random(20260802)
for _y in range(SIZE):
    for _x in range(SIZE):
        MARBLE[(_x, _y)] = 1.0 + (_rng.random() - 0.5) * 0.13


def shade(c, f):
    return tuple(max(0, min(255, int(round(v * f)))) for v in c)


def opaque(c):
    return (c[0], c[1], c[2], 255)


BODY = {(x, y) for y, (x0, x1) in BLOB.items() for x in range(x0, x1 + 1)}


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


def ring(region):
    """Body pixels just outside a region, eight-connected so a stepped arc comes
    back as an unbroken line instead of a dotted one."""
    out = set()
    for (x, y) in region:
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                p = (x + dx, y + dy)
                if p in BODY and p not in region:
                    out.add(p)
    return out


def shell_at(x, y):
    """Shell colour for one pixel: an upper-left light, then the fixed marble."""
    t = (x - CX) + (y - CY)
    base = SHELL_HI if t <= -4 else (SHELL if t <= 1 else SHELL_LO)
    return shade(base, MARBLE[(x, y)])


def lens(a):
    """The open region at blink amount a, as an ellipse clipped to the body.

    An ellipse rather than a pair of rows is the whole point: its edges are arcs,
    so the aperture tapers to points at the corners the way an eye does. The
    centre sinks with a because the upper lid does most of the travelling.
    """
    if a >= 1.0:
        return set()
    cy = CY + LID_DROP * a
    h = LENS_H * (1.0 - a)
    w = LENS_W * (1.0 - LENS_PINCH * a)
    return {(x, y) for (x, y) in BODY
            if ((x - CX) / w) ** 2 + ((y - cy) / h) ** 2 <= 1.0}


def blink_amount(frame):
    i = frame - BLINK_AT
    return BLINK[i] if 0 <= i < len(BLINK) else 0.0


def gaze(frame):
    for last, offset in GAZE:
        if frame < last:
            return offset
    return GAZE[-1][1]


def pupil_corner(frame):
    """The corner tone of the pupil this frame — how wide it is standing open."""
    wide = 0.5 + 0.5 * math.cos(2 * math.pi * frame / PUPIL_CYCLE)
    return shade(IRIS, PUPIL_CORNER_TIGHT + (PUPIL_CORNER_WIDE - PUPIL_CORNER_TIGHT) * wide)


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


def paint(frame):
    grid = {}

    # Shell first, everywhere. The lids are the same material as the orb, so
    # painting the whole body and then creasing it is the lid.
    for (x, y) in BODY:
        grid[(x, y)] = shell_at(x, y)
    for (x, y) in EDGE:
        grid[(x, y)] = RIM_LIT if (x - CX) + (y - CY) <= -3 else RIM

    a = blink_amount(frame)
    open_px = lens(a)
    ox, oy = gaze(frame)
    ix, iy = CX + ox, CY + oy
    corner = pupil_corner(frame)

    for (x, y) in open_px:
        d = ((x - ix) ** 2 + (y - iy) ** 2) ** 0.5
        if d <= PUPIL_R:
            grid[(x, y)] = corner if d > 1.1 else PUPIL
        elif d <= IRIS_R:
            # Shadowed under the upper lid, brightest at the bottom, the way any
            # iris sitting under a brow is. Flat, it reads as a sticker.
            tone = IRIS_HI if y > iy else (IRIS_DK if y < iy - 1 else IRIS)
            grid[(x, y)] = shade(tone, MARBLE[(x, y)])
        elif d <= IRIS_RIM_R:
            grid[(x, y)] = shade(IRIS_RIM, MARBLE[(x, y)])

    # The glint rides the gaze. It is a corneal reflection and the cornea is the
    # part that turns, so it travels — and pinned in place it spends a third of
    # the loop sitting on pale shell, which is the same as not being drawn.
    for gx, gy in GLINT:
        p = (gx + ox, gy + oy)
        if p in open_px:
            grid[p] = BLICK

    # The lash line follows whatever curve the lens left behind. Shut, there is
    # no lens to follow, so the seam is the last sliver of one — same arc, one
    # pixel thick.
    crease = ring(open_px) if open_px else lens(SHUT_SEAM)
    for p in crease - EDGE:
        grid[p] = CREASE

    # One lifted row above the upper lid and nothing under the lower one. Ring
    # the whole crease instead and a narrow aperture comes out as four stacked
    # bands, which reads as a mouth rather than an eye.
    top = {}
    for (x, y) in crease:
        if y < top.get(x, SIZE):
            top[x] = y
    for x, y in top.items():
        p = (x, y - 1)
        if p in BODY and p not in EDGE and p not in open_px and p not in crease:
            grid[p] = shade(shell_at(*p), 1.06)

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
            r, g, b, alpha = px[x, y]
            row += "." if alpha == 0 else " #+=-o*"[min(int((r + g + b) / 3 / 255 * 6) + 1, 6)]
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
    assert lens(0.0) == BODY, "a wide-open eye must not have a lid on it"
    assert GAZE[-1][0] == FRAMES, "the gaze schedule must cover the whole loop"

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
