"""Generate ANIMATED item textures for Berlord's Food System from vanilla baselines.

Each item = 8 frames of 16x16, stacked into a 16x128 vertical strip + .png.mcmeta.
Run:  python make_textures.py
Outputs strips to out/ + copies into the mod's assets; also renders preview GIFs
and per-item contact sheets to out/.
"""
import json
import math
import os
import shutil

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
VANILLA = os.path.join(HERE, "vanilla")
OUT = os.path.join(HERE, "out")
DEST = os.path.join(HERE, "..", "src", "main", "resources", "assets",
                    "berlords_food_system", "textures", "item")
os.makedirs(OUT, exist_ok=True)

FRAMES = 8
FRAMETIME = 3  # ticks per frame


def load(name):
    return Image.open(os.path.join(VANILLA, name + ".png")).convert("RGBA")


def put(img, pixels):
    for (x, y), c in pixels.items():
        img.putpixel((x, y), c if len(c) == 4 else (*c, 255))


# ---------------------------------------------------------------- 1: emetic
# surface lumps drift left->right (swirl), stench wisps rise off the top
def emetic_frames():
    HI = (118, 148, 44)
    MAIN = (88, 114, 30)
    MID = (72, 94, 24)
    DARK = (53, 69, 15)
    EDGE = (62, 80, 20)
    STENCH_A = (138, 158, 96, 150)
    STENCH_B = (138, 158, 96, 90)

    base_rows = {
        6: {5: MID, 6: MAIN, 7: MAIN, 8: MID, 9: MID, 10: EDGE},
        7: {3: MID, 4: MAIN, 5: MAIN, 6: MAIN, 7: MAIN, 8: MAIN,
            9: MAIN, 10: MAIN, 11: MID, 12: EDGE},
        8: {5: EDGE, 6: MAIN, 7: MID, 8: MID, 9: MAIN, 10: EDGE},
    }
    OSC = [0, 1, 1, 0, 0, -1, -1, 0]   # ping-pong, seamless over 8 frames
    static_lumps = {(4, 7): HI, (9, 7): DARK}
    movers = [((6, 7), DARK, 0), ((7, 6), DARK, 2), ((8, 8), HI, 5)]
    frames = []
    for f in range(FRAMES):
        img = load("bowl")
        for y, cols in base_rows.items():
            for x, c in cols.items():
                put(img, {(x, y): c})
        put(img, static_lumps)
        # only central lumps oscillate +-1px, staggered phases
        for (x, y), c, ph in movers:
            put(img, {(x + OSC[(f + ph) % FRAMES], y): c})
        # stench wisps: alternate two columns, rising on a zigzag path
        # (dx, y) per rise step; tail trails the head's previous position
        path_a = [(0, 4), (1, 3), (0, 2), (-1, 1)]
        path_b = [(0, 4), (-1, 3), (0, 2), (1, 1)]
        col, path, ph = (6, path_a, f) if f < 4 else (9, path_b, f - 4)
        dx, y = path[ph]
        put(img, {(col + dx, y): STENCH_A})
        if ph > 0:
            tdx, ty = path[ph - 1]
            put(img, {(col + tdx, ty): STENCH_B})
        frames.append(img)
    return frames


# -------------------------------------------------------- 2: demonic gruel
# tentacles extend/retract like slug eye stalks, staggered phases
def demonic_frames():
    BLACK = (26, 6, 6)
    DARK = (61, 10, 10)
    MID = (122, 18, 18)
    HI = (176, 32, 32)
    BRIGHT = (214, 58, 42)
    OUTLINE = (31, 21, 2)
    HALF = (58, 39, 6)

    # rim row 5 stays vanilla bowl wood — contents only inside
    contents = {
        (5, 6): DARK, (6, 6): HI, (7, 6): MID, (8, 6): BLACK, (9, 6): DARK, (10, 6): BLACK,
        (3, 7): DARK, (4, 7): MID, (5, 7): BLACK, (6, 7): DARK, (7, 7): HI, (8, 7): MID,
        (9, 7): BLACK, (10, 7): DARK, (11, 7): BLACK, (12, 7): DARK,
        (5, 8): BLACK, (6, 8): DARK, (7, 8): MID, (8, 8): BLACK, (9, 8): DARK, (10, 8): BLACK,
    }
    crack = {(7, 9): OUTLINE, (7, 10): OUTLINE, (8, 10): HALF, (8, 11): OUTLINE}

    def tentacle(base_x, bend_x, ext):
        # ext 0 = retracted, 3 = fully out; tip is always BRIGHT
        if ext == 0:
            return {}
        if ext == 1:
            return {(base_x, 4): BRIGHT}
        if ext == 2:
            return {(base_x, 4): DARK, (bend_x, 3): BRIGHT}
        return {(base_x, 4): DARK, (bend_x, 3): MID, (bend_x, 2): BRIGHT}

    LEFT = [3, 3, 2, 1, 0, 0, 1, 2]
    RIGHT = [2, 3, 3, 2, 1, 0, 0, 1]
    NUB = [1, 1, 1, 0, 0, 0, 1, 1]

    # tiny internal stirring: three highlight pairs swap places on slow,
    # staggered half-cycles (1px of motion inside the gruel)
    swaps = [((7, 7), (8, 7), HI, MID, 0),
             ((6, 6), (7, 6), HI, MID, 2),
             ((7, 8), (6, 8), MID, DARK, 4)]

    frames = []
    for f in range(FRAMES):
        img = load("bowl")
        put(img, contents)
        for a, b, ca, cb, ph in swaps:
            if ((f + ph) // 4) % 2 == 0:
                put(img, {a: ca, b: cb})
            else:
                put(img, {a: cb, b: ca})
        put(img, crack)
        put(img, tentacle(5, 4, LEFT[f]))
        put(img, tentacle(10, 11, RIGHT[f]))
        if NUB[f]:
            put(img, {(8, 4): DARK})
        frames.append(img)
    return frames


# --------------------------------------------------- 3: stomach extension
# liquid pinwheel rotates counterclockwise; glass layer static on top
def stomach_frames():
    bottle = load("glass_bottle")
    overlay = load("potion_overlay")
    SWIRL = [(255, 80, 220), (255, 225, 80), (70, 225, 255), (150, 90, 255)]
    cx, cy = 8.0, 10.5
    frames = []
    for f in range(FRAMES):
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        for y in range(16):
            for x in range(16):
                if overlay.getpixel((x, y))[3] == 0:
                    continue
                dx, dy = x - cx, y - cy
                r = math.hypot(dx, dy)
                a = math.atan2(dy, dx)
                # +f/FRAMES phase per frame = counterclockwise rotation;
                # negative r term mirrors the spiral so arms lead head-first
                t = (a / (2 * math.pi) - r * 0.22 + f / FRAMES) % 1.0
                # ragged band edges: fixed per-pixel jitter on the band phase
                t = (t + ((x * 7 + y * 13) % 5 - 2) * 0.035) % 1.0
                c = SWIRL[int(t * len(SWIRL)) % len(SWIRL)]
                # color noise: fixed sparkle/murk masks over the moving fluid
                h = (x * 5 + y * 3) % 11
                if h == 0:
                    c = tuple(min(255, v + 60) for v in c)
                elif h == 5:
                    c = tuple(int(v * 0.78) for v in c)
                put(img, {(x, y): c})
        put(img, {(8, 10): (255, 255, 255)})
        img.alpha_composite(bottle)
        frames.append(img)
    return frames


# --------------------------------------------------- 4: eternal seasoning
# approved wisp: sparks bob up/down, tip rows shiver left/right with delay
def eternal_frames():
    W = (242, 240, 252)
    L = (206, 200, 238)
    P = (168, 160, 216)
    M = (126, 117, 178)
    D = (88, 79, 136)
    O = (56, 48, 92)
    G = (255, 216, 74)
    g = (212, 164, 40)

    body = {
        # curling tip
        (5, 3): M,
        (5, 4): P, (6, 4): M,
        (4, 5): M, (5, 5): L, (6, 5): P,
        # body
        (4, 6): P, (5, 6): L, (6, 6): P, (7, 6): M,
        (4, 7): P, (5, 7): W, (6, 7): L, (7, 7): P,
        (3, 8): M, (4, 8): P, (5, 8): W, (6, 8): L, (7, 8): P, (8, 8): M,
        (10, 7): D, (10, 8): M,
        (3, 9): M, (4, 9): L, (5, 9): W, (6, 9): L, (7, 9): P, (8, 9): P, (9, 9): M, (10, 9): D,
        (3, 10): D, (4, 10): g, (5, 10): W, (6, 10): W, (7, 10): L, (8, 10): G, (9, 10): M, (10, 10): M, (11, 10): D,
        (3, 11): D, (4, 11): M, (5, 11): L, (6, 11): W, (7, 11): L, (8, 11): P, (9, 11): P, (10, 11): M, (11, 11): O,
        (4, 12): O, (5, 12): M, (6, 12): P, (7, 12): g, (8, 12): M, (9, 12): M, (10, 12): O,
        (5, 13): O, (6, 13): D, (7, 13): D, (8, 13): D, (9, 13): O,
    }
    NO_SWAY = [0] * 8
    SWAY_R = [0, 0, 0, 1, 1, 0, 0, 0]
    SWAY_L = [0, 0, 0, -1, -1, 0, 0, 0]
    sparks = [((9, 1), L, 0, NO_SWAY), ((11, 3), G, 2, SWAY_R),
              ((2, 5), D, 4, SWAY_L), ((12, 5), M, 6, NO_SWAY),
              ((13, 8), D, 1, SWAY_L)]
    BOB = [0, 0, -1, -1, 0, 0, 1, 1]

    # column tops: the flame surface rises/falls per column like fluid
    col_top = {}
    for x, y in body:
        col_top[x] = min(y, col_top.get(x, 99))

    # burst particles: (column, launch frame, dx per age) -> pixel detaches
    # above the surface, zigzags upward fading L -> M -> D, then disappears
    BURSTS = [(6, 0, (0, 1, 0)), (10, 3, (0, -1, 0)), (4, 6, (0, 0, -1))]
    FADE = [L, M, D]

    frames = []
    for f in range(FRAMES):
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        put(img, body)
        # gentler wave traveling left->right; upward bias grows toward the
        # right so right-side columns never dip below rest height
        for x, y0 in col_top.items():
            s = round(0.6 * math.sin(2 * math.pi * (f - x) / FRAMES)
                      + (x - 3) * 0.06)
            if s > 0:
                put(img, {(x, y0 - 1): body[(x, y0)]})
            elif s < 0:
                img.putpixel((x, y0), (0, 0, 0, 0))
        for bx, bf, zig in BURSTS:
            age = (f - bf) % FRAMES
            if age < 3:
                put(img, {(bx + zig[age], col_top[bx] - 2 - age): FADE[age]})
        for (x, y), c, ph, sway in sparks:
            k = (f + ph) % FRAMES
            put(img, {(x + sway[k], y + BOB[k]): c})
        frames.append(img)
    return frames


# --------------------------------------------------------- 5: lunchbox (STATIC)
# a blue metal lunchbox with a carry handle and a brass latch. No animation.
def lunchbox_static():
    OUTL = (30, 36, 52, 255)    # dark navy outline
    BODY = (76, 122, 178, 255)  # blue body
    HI = (116, 166, 220, 255)   # left/top highlight
    SH = (48, 80, 122, 255)     # right/bottom shadow + seam
    LID = (98, 144, 200, 255)   # lid band
    LIDH = (134, 180, 228, 255) # lid top highlight
    MET = (170, 174, 182, 255)  # handle light metal
    METD = (86, 90, 100, 255)   # handle dark metal
    BRA = (220, 188, 78, 255)   # brass latch
    BRAD = (156, 124, 44, 255)  # brass latch shadow

    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    P = {}
    left, right, top, bot = 2, 13, 5, 14

    # interior body fill
    for y in range(top + 1, bot):
        for x in range(left + 1, right):
            P[(x, y)] = BODY
    # lid band (upper portion)
    for y in range(top + 1, 9):
        for x in range(left + 1, right):
            P[(x, y)] = LID
    # lid top highlight row
    for x in range(left + 1, right):
        P[(x, top + 1)] = LIDH
    # left inner highlight, right inner shadow, bottom inner shadow
    for y in range(top + 1, bot):
        P[(left + 1, y)] = HI
        P[(right - 1, y)] = SH
    for x in range(left + 1, right):
        P[(x, bot - 1)] = SH
    # lid/base seam line
    for x in range(left + 1, right):
        P[(x, 9)] = SH

    # outline border (rounded corners)
    for x in range(left, right + 1):
        P[(x, top)] = OUTL
        P[(x, bot)] = OUTL
    for y in range(top, bot + 1):
        P[(left, y)] = OUTL
        P[(right, y)] = OUTL
    for c in [(left, top), (right, top), (left, bot), (right, bot)]:
        P[c] = (0, 0, 0, 0)

    # carry handle (arch) above the lid, feet landing on the lid top
    for x in (6, 7, 8, 9):
        P[(x, 2)] = METD
    P[(6, 2)] = MET
    P[(5, 3)] = METD; P[(10, 3)] = METD
    P[(5, 4)] = MET;  P[(10, 4)] = MET

    # brass latch straddling the seam, front center
    P[(7, 8)] = BRA;  P[(8, 8)] = BRA
    P[(7, 9)] = BRA;  P[(8, 9)] = BRA
    P[(7, 10)] = BRAD; P[(8, 10)] = BRAD

    for (x, y), c in P.items():
        if 0 <= x < 16 and 0 <= y < 16:
            img.putpixel((x, y), c)
    return img


TEXTURES = {
    "emetic": emetic_frames,
    "demonic_gruel": demonic_frames,
    "stomach_extension": stomach_frames,
    "eternal_seasoning": eternal_frames,
}

# static (non-animated) single-frame textures: written as plain 16x16 PNG, NO mcmeta
STATIC_TEXTURES = {
    "lunchbox": lunchbox_static,
}

if __name__ == "__main__":
    for name, fn in TEXTURES.items():
        frames = fn()
        # vertical strip + mcmeta -> mod assets
        strip = Image.new("RGBA", (16, 16 * FRAMES), (0, 0, 0, 0))
        for i, fr in enumerate(frames):
            strip.paste(fr, (0, 16 * i))
        strip.save(os.path.join(OUT, name + ".png"))
        with open(os.path.join(OUT, name + ".png.mcmeta"), "w") as fp:
            json.dump({"animation": {"frametime": FRAMETIME}}, fp)
        shutil.copy(os.path.join(OUT, name + ".png"), os.path.join(DEST, name + ".png"))
        shutil.copy(os.path.join(OUT, name + ".png.mcmeta"),
                    os.path.join(DEST, name + ".png.mcmeta"))
        # preview gif (8x, on dark bg) + contact sheet (frames in a row)
        big = []
        for fr in frames:
            bg = Image.new("RGBA", (128, 128), (40, 40, 40, 255))
            bg.alpha_composite(fr.resize((128, 128), Image.NEAREST))
            big.append(bg.convert("P"))
        big[0].save(os.path.join(OUT, name + ".gif"), save_all=True,
                    append_images=big[1:], duration=FRAMETIME * 50, loop=0)
        sheet = Image.new("RGBA", (96 * FRAMES + 8 * (FRAMES - 1), 96), (40, 40, 40, 255))
        for i, fr in enumerate(frames):
            sheet.paste(fr.resize((96, 96), Image.NEAREST), (i * 104, 0))
        sheet.save(os.path.join(OUT, "_sheet_" + name + ".png"))
        print("wrote", name)

    for name, fn in STATIC_TEXTURES.items():
        img = fn()
        img.save(os.path.join(OUT, name + ".png"))
        shutil.copy(os.path.join(OUT, name + ".png"), os.path.join(DEST, name + ".png"))
        # static texture: ensure no stale animation mcmeta lingers
        stale = os.path.join(DEST, name + ".png.mcmeta")
        if os.path.exists(stale):
            os.remove(stale)
        # large preview on dark bg for eyeballing
        bg = Image.new("RGBA", (256, 256), (40, 40, 40, 255))
        bg.alpha_composite(img.resize((256, 256), Image.NEAREST))
        bg.save(os.path.join(OUT, "_preview_" + name + ".png"))
        print("wrote static", name)
    print("done")
