"""
bertieprogression texture generator — desert_core (16x16), animated.

A sandstone pyramid with a gold capstone on a desert horizon, torn down by a
sandstorm and thrown back up brick by brick. It replaces the glass sphere full
of sand this item used to have — the same departure storm_core made from that
set — and **desert_core is no longer in make_cores.py**, which now covers the
other three. Do not add it back there or this art gets overwritten the next
time that script runs.

    form     Forbidden & Arcanus draws its prisms — sea, smelter, terrastomp,
             whirlwind — as a symmetric diamond: a pointed apex, straight edges
             widening to the widest course, then a steeper taper closing back
             under it. That is the shape berlord picked, measured off theirs.
             No pixel is copied and none of their colours are used.

             Every edge holds one constant step — a column a row on the way up,
             two columns a row on the way back in — and every course is
             symmetric about the sprite. Both are asserted. Three passes set
             the widths by eye and all three came out lumpy: at this size an
             edge that changes its step even once reads as a dent, not a slope.

             The two courses at the widest point share a span. That is the band
             the prisms have, it is the only repeat allowed, and it is what
             lets both edges keep their step while the widest course still
             stops one column short of the frame on each side. Run out to the
             full 16 and the pyramid's own outline sits where the ground's rim
             should be, so the border round the sprite breaks exactly where the
             pyramid is widest.

             It is drawn square on. Two passes at a three-quarter view — a real
             projection of a square base, then a hand-skewed one — were both
             wrong: the reference prisms are symmetric, and a 16-pixel sprite
             does not have the room to turn a pyramid and keep its edges
             regular.

    faces    the arris runs dead down the middle, at x=7. Left of it is the lit
             face, right of it the shaded one. Each face ramps away from the
             arris towards its own silhouette instead of sitting at one tone —
             that ramp is what gives the solid volume, and without it the
             sprite is the right outline filled flat.

    cap      gold, and it has to run hotter and more saturated than the stone
             rather than just brighter: sandstone is already a pale yellow, so
             a cap that differs only in value reads as a lit patch of the same
             rock.

    scene    painted sky down to one row below the apex, then ground to the
             bottom of the frame. The horizon is placed off the pyramid rather
             than off the frame, so the capstone always breaks the skyline by
             the same amount. The background is filled top to bottom; the only
             empty pixels are the four rounded corners, six each.

             The ground sits well under the pyramid's own tones throughout.
             Loose sand and cut sandstone are the same material: at matching
             values everything below the horizon dissolves into the ground and
             the sprite loses its silhouette exactly where it needs one.

             Nothing is drawn on the sand that the pyramid is not casting. An
             earlier pass had a hardcoded cast shadow here, written when the
             pyramid was much wider at the bottom and never re-derived — it
             ended up as a dark patch to the right of a base that was no longer
             under it, in every frame including the intact one.

    storm    streaks blown left to right, wrapping at the sprite edge. In open
             sky they are drawn; over the pyramid and the ground they are mixed
             into whatever is underneath, because a grain crossing the face is
             dust in front of stone, not a hole in it.

             Three tiers of streak, keyed off a storm level: the weak tier is
             always up, the other two cut in as the level rises. Intensity is
             carried by how many streaks are running and how long they are,
             never by changing a streak's speed — every streak holds one drift
             for the whole strip so the field is identical again after its own
             period and the loop closes. Faster air is a tier of its own with
             its own larger drifts. There is an assert on the frame count.

    collapse ends at nothing. It used to settle into a mound across the bottom
             two rows of the frame, which is neither where the base was nor
             anywhere a rebuilt pyramid covers, so a bar of stone sat under an
             empty desert for the whole pause. What the wind takes is carried
             downwind instead.

    rebuild  44 bricks, each lobbed in on a parabola launched from well outside
             the sprite, so it appears at an edge already travelling and drops
             onto its own slot. Eight frames in the air each, drawn dark — see
             FLIGHT_STONE. They arrive course by course, bottom first, and
             scattered within each course so it does not read as a wipe.

Frame 0 is the intact pyramid, so it stands on its own — that is what a client
with animation turned off falls back to.

Writes the shipped sprite plus two takes that nothing uses; see OUTPUTS.

Run:  python texture-work/make_desert_core.py [--ascii]
"""
import json
import os
import sys

from PIL import Image

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
TEX_ITEM = os.path.join(ROOT, "src", "main", "resources", "assets", "bertieprogression",
                        "textures", "item")
# Takes that are not in use but are being kept. Under texture-work rather than
# under resources, so they are versioned without being packaged into the jar —
# nothing in there ships.
VARIANTS = os.path.join(ROOT, "texture-work", "variants")

SIZE = 16

# --- palette -----------------------------------------------------------------

# One outline, dark, all the way round. A softer warm edge down the lit slope
# was tried and it loses the silhouette against a grey inventory slot.
OUTLINE = "#3A2A12"
UNDERSIDE = "#5C4A1C"      # where a course overhangs the one below it

# Each face ramps away from the arris towards its own silhouette rather than
# sitting at one tone. The gap between the arris and the face beside it has to
# be wide: on the prisms the centre line is the first thing you see, and
# matched any closer to the lit face it disappears into it and the fold goes
# with it.
ARRIS = "#F2E0AC"          # the near edge, after the prisms' bright centre
SAND_LIT = "#DCC176"       # the lit face, against the arris
SAND_LIT_EDGE = "#BE9C4E"  # and where it turns away at the silhouette
SAND_DARK = "#94742F"      # the shaded face, against the arris
SAND_DARK_EDGE = "#5C4A1C"  # and where it turns away
JOINT_LIT = 0.06           # how far a joint darkens the face it is cut into
JOINT_DARK = 0.08
# Everything below the widest course goes into shadow, deepening to the point.
# This is the other half of what the prisms do: a bright cone standing in a
# dark base, not one lit solid.
BASE_SHADE = 0.24

CAP_LIT = "#FFF2B4"       # the arris of the capstone
CAP_MID = "#F6CE44"       # its lit face
CAP_DARK = "#C89A24"      # its shaded face
CAP_EDGE = "#7A5510"      # and its edge

SKY_TOP = "#6E96BE"       # overhead
SKY_HORIZON = "#B7CEDE"   # hazing out where it meets the sand
SKY_EDGE = "#4E7BA8"      # and the rounded corners where it runs out

GROUND_FAR = "#CFB37B"    # at the horizon
GROUND_NEAR = "#A08249"   # at the bottom of the frame
GROUND_RIPPLE = "#8A6E3A"  # wind ripples cut into it
GROUND_EDGE = "#6E5628"   # and the rounded corners where it runs out

GRAIN = "#F3E0AC"         # a grain in open air
GRAIN_TAIL = "#C9AD71"    # what trails it
HAZE = "#EAD79E"          # what the storm mixes everything solid towards

# Broken stone, pitched under the ground it lies on. It started out in the same
# range as the sand and once the ground plane went in it vanished into it.
RUBBLE = "#8E7132"
RUBBLE_LIT = "#B99C5C"
RUBBLE_DARK = "#5F4C20"

# --- the pyramid --------------------------------------------------------------

PYRAMID = {
    3:  (7, 8),
    4:  (6, 9),
    5:  (5, 10),
    6:  (4, 11),
    7:  (3, 12),
    8:  (2, 13),
    9:  (1, 14),
    10: (1, 14),
    11: (3, 12),
    12: (5, 10),
    13: (7, 8),
}
WIDEST_ROW = 10
CAP_ROWS = (3, 4, 5)      # the gold capstone

# Dead centre on every row, and defined for rows the pyramid does not reach so
# the collapse profiles shade the same way.
ARRIS_X = {y: 7 for y in range(SIZE)}

# The horizon is placed off the pyramid, not off the frame: the last row of sky
# sits one below the apex, so the cap always breaks the skyline by the same
# amount whatever the silhouette does.
APEX_ROW = min(PYRAMID)
HORIZON = APEX_ROW + 2    # first row of ground

# Painted, not left transparent — the background is filled top to bottom and
# only the corners are empty, six pixels each, top and bottom matching.
SKY_ROWS = {
    0: [(3, 12)],
    1: [(2, 13)],
    2: [(1, 14)],
    3: [(0, 15)],
    4: [(0, 15)],
}
GROUND_ROWS = {
    5:  [(0, 15)],
    6:  [(0, 15)],
    7:  [(0, 15)],
    8:  [(0, 15)],
    9:  [(0, 15)],
    10: [(0, 15)],
    11: [(0, 15)],
    12: [(0, 15)],
    13: [(1, 14)],
    14: [(2, 13)],
    15: [(3, 12)],
}

# The rhomboid the base stands on. Only the debris strip draws anything here.
# It stops at row 13 on purpose: debris on rows 14 and 15 is at the bottom of
# the frame rather than where the pyramid was, and nothing a rebuilt pyramid
# does ever covers it again.
DEBRIS_FIELD = {
    10: [(2, 13)],
    11: [(2, 13)],
    12: [(3, 12)],
    13: [(5, 10)],
}
DEBRIS_DENSITY = 0.34     # how much of the rhomboid carries a chip of stone

# A row narrower than this gets no outline. Without the guard a two-pixel brick
# left standing mid-erosion comes out entirely outline colour and reads as a
# hole rather than as the last of the stone.
OUTLINE_MIN_RUN = 4

assert set(SKY_ROWS) == set(range(HORIZON)), "sky has to run from the top to the horizon"
assert set(GROUND_ROWS) == set(range(HORIZON, SIZE)), "ground has to run from the horizon down"
assert PYRAMID[WIDEST_ROW] == (1, SIZE - 2), "the widest course stops one short of the frame"
for _y, _span in PYRAMID.items():
    assert _span[0] <= ARRIS_X[_y] <= _span[1], "arris off the stone on row %d" % _y
    assert _span[0] + _span[1] == SIZE - 1, (
        "row %d is not symmetric — the reference prisms are, and the angled "
        "view they replaced is gone" % _y)
# No two courses may share a span apart from the pair at the widest point.
# Anywhere else a repeat is a flat spot in a slope, which is what berlord kept
# seeing as a lumpy outline.
for _y in sorted(PYRAMID)[1:]:
    if _y == WIDEST_ROW:
        continue
    assert PYRAMID[_y] != PYRAMID[_y - 1], (
        "rows %d and %d have the same span" % (_y - 1, _y))


def hexcol(s):
    return tuple(int(s[i:i + 2], 16) for i in (1, 3, 5)) + (255,)


def mix(a, b, t):
    """Blend colour `a` a fraction `t` of the way towards `b`."""
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3)) + (255,)


def spans(table):
    out = set()
    for y, runs in table.items():
        for x0, x1 in (runs if isinstance(runs, list) else [runs]):
            out.update((x, y) for x in range(x0, x1 + 1))
    return out


def scatter(x, y):
    """A fixed pseudo-random number in [0,1). Not random at run time: the strip
    has to come out the same on every run."""
    return (((x + 1) * 73856093) ^ ((y + 1) * 19349663)) % 997 / 997.0


# --- bricks -------------------------------------------------------------------
#
# Two columns by one row, one column where a course runs out odd.


def bricks_of(table):
    out = []
    for y in sorted(table):
        runs = table[y]
        for x0, x1 in (runs if isinstance(runs, list) else [runs]):
            x = x0
            while x <= x1:
                out.append((y, x, min(2, x1 - x + 1)))
                x += 2
    return out


def brick_pixels(bricks):
    return {(x + i, y) for (y, x, w) in bricks for i in range(w)}


BRICKS = bricks_of(PYRAMID)
BASE_COURSES = 3          # rows 11..13, everything below the widest course


def row_mid(y):
    x0, x1 = PYRAMID[y]
    return (x0 + x1) / 2.0


# Erosion takes the top course first and, within a course, the bricks furthest
# from that course's own middle — wind gets at a corner before it gets at a
# face. The jitter stops each frame leaving a tidy centred remnant, which reads
# as something built that way rather than something being eaten.
ERODE_ORDER = sorted(
    BRICKS,
    key=lambda b: (b[0], -(abs(b[1] - row_mid(b[0])) + scatter(b[1], b[0]) * 2.5)))

# Which bricks a brick rests on: everything in the course below that its own
# columns sit over.
BRICK_AT = {}
for _b in BRICKS:
    for _i in range(_b[2]):
        BRICK_AT[(_b[1] + _i, _b[0])] = _b
SUPPORT = {b: {BRICK_AT[(b[1] + i, b[0] + 1)]
               for i in range(b[2]) if (b[1] + i, b[0] + 1) in BRICK_AT}
           for b in BRICKS}


def build_order():
    """Course by course, bottom first — a course is finished before the next
    one starts — and scattered within each course so it does not read as a
    wipe. Filtered against SUPPORT so nothing arrives before what holds it up;
    that is belt and braces at strict course order, but it keeps the guarantee
    if the ordering is ever loosened again."""
    want = sorted(BRICKS, key=lambda b: (-b[0], scatter(b[1], b[0])))
    placed, out = set(), []
    while want:
        for i, b in enumerate(want):
            if SUPPORT[b] <= placed:
                out.append(b)
                placed.add(b)
                del want[i]
                break
        else:
            raise AssertionError("no brick can be placed — support graph has a cycle")
    return out


BUILD_ORDER = build_order()
_seen = set()
for _b in BUILD_ORDER:
    assert SUPPORT[_b] <= _seen, "brick %r lands before something under it" % (_b,)
    _seen.add(_b)

# --- storm --------------------------------------------------------------------
#
# (start x, row, length, columns per frame). Each streak wraps at the sprite
# edge, so it repeats after SIZE/drift frames; every drift here divides SIZE,
# which is what lets a single frame count close the loop for all of them.
#
# Rows are deliberately not laddered. Evenly spaced streaks read as a comb.
STORM_WEAK = (
    (0,  2, 2, 1),
    (5,  0, 1, 2),
    (15, 6, 2, 1),
    (1,  11, 1, 2),
    (0,  12, 2, 1),
    (8,  15, 2, 2),
    (2,  4, 1, 1),
)
STORM_MID = (
    (7,  1, 4, 2),
    (1,  5, 3, 2),
    (13, 8, 4, 2),
    (4,  11, 5, 4),
    (9,  14, 3, 2),
)
# Long and fast rather than many. An earlier pass ran twenty short ones and the
# strip frames came out as sand-coloured static with no pyramid findable.
STORM_HARD = (
    (3,  3, 7, 4),
    (10, 7, 8, 4),
    (15, 10, 6, 4),
    (2,  13, 7, 4),
)
STORM_MID_AT = 0.35
STORM_HARD_AT = 0.70
STORM_PERIOD = SIZE
# How far a full storm washes the stone towards HAZE. Low: the wash and the
# grains are the same colour, so anything near a third takes the faces to the
# tone of the sand crossing them and the whole sprite flattens out.
HAZE_MAX = 0.16


def silhouette_edge(shape):
    rows = {}
    for (x, y) in shape:
        rows.setdefault(y, []).append(x)
    out = set()
    for y, xs in rows.items():
        if len(xs) >= OUTLINE_MIN_RUN:
            out |= {(min(xs), y), (max(xs), y)}
    return out


# Frame 0 is the fallback for a client with animation off, so no weak streak
# may sit on the silhouette there: at rest it reads as a chipped edge rather
# than as weather, and it never moves off.
_EDGE = silhouette_edge(spans(PYRAMID))
for _s in STORM_WEAK:
    _hit = [((_s[0] - i) % SIZE, _s[1]) for i in range(_s[2])
            if ((_s[0] - i) % SIZE, _s[1]) in _EDGE]
    assert not _hit, "weak streak %r sits on the silhouette at %r on frame 0" % (_s, _hit)


# --- painting -----------------------------------------------------------------


def draw_sky(grid):
    for y, runs in SKY_ROWS.items():
        for x0, x1 in runs:
            col = mix(hexcol(SKY_TOP), hexcol(SKY_HORIZON), y / max(1, HORIZON - 1))
            for x in range(x0, x1 + 1):
                grid[(x, y)] = col
            grid[(x0, y)] = hexcol(SKY_EDGE)
            grid[(x1, y)] = hexcol(SKY_EDGE)


def draw_ground(grid):
    """Sand from the horizon to the bottom of the frame, shaded by distance
    rather than by a pattern — at this size a per-pixel checker is static, not
    texture."""
    depth = SIZE - 1 - HORIZON
    for y, runs in GROUND_ROWS.items():
        for x0, x1 in runs:
            for x in range(x0, x1 + 1):
                col = mix(hexcol(GROUND_FAR), hexcol(GROUND_NEAR),
                          (y - HORIZON) / depth)
                # Ripples, as short horizontal runs stepped along per row so
                # they cross rather than stack into columns.
                if y > HORIZON and (x + y * 5) % 11 < 3:
                    col = mix(col, hexcol(GROUND_RIPPLE), 0.55)
                grid[(x, y)] = col
            grid[(x0, y)] = hexcol(GROUND_EDGE)
            grid[(x1, y)] = hexcol(GROUND_EDGE)


def draw_debris_field(grid, density):
    """Broken stone lying over the rhomboid the base stood on. Scattered off
    the hash rather than a modulo, so it does not come out as a dotted line,
    and thinned by `density` so it clears as the pyramid goes back up."""
    if density <= 0:
        return
    for (x, y) in spans(DEBRIS_FIELD):
        if (x, y) not in grid:
            continue
        r = scatter(x * 3, y * 7)
        if r < density:
            grid[(x, y)] = hexcol(RUBBLE_DARK if r < density / 2 else RUBBLE)


def draw_rubble(grid, table):
    """Stone coming apart. Shaded by depth under the top of its own column,
    with chips placed off the hash — at a modulo of the coordinates the chips
    landed on every fourth column on every row, which is a dotted line across
    the sprite and reads as a rendering fault rather than as broken stone."""
    pile = spans(table)
    tops = {}
    for (x, y) in pile:
        tops[x] = min(y, tops.get(x, SIZE))
    for (x, y) in pile:
        depth = y - tops[x]
        col = RUBBLE_LIT if depth == 0 else RUBBLE if depth == 1 else RUBBLE_DARK
        r = scatter(x, y)
        if r < 0.26:
            col = RUBBLE_DARK
        elif r > 0.86:
            col = RUBBLE_LIT
        grid[(x, y)] = hexcol(col)


def shade_pyramid(grid, shape):
    """Colour a set of pyramid pixels. Works on the whole thing and on whatever
    is left of it mid-collapse, so erosion and rebuild get the same stone."""
    if not shape:
        return
    rows = {}
    for (x, y) in shape:
        rows.setdefault(y, []).append(x)

    # Each face ramps from the arris out to its own silhouette. `lo` and `hi`
    # come off whatever is present on the row rather than off PYRAMID, so a
    # half-eaten course shades across what is left of it.
    for y, xs in rows.items():
        a = ARRIS_X[y]
        lo, hi = min(xs), max(xs)
        for x in xs:
            if y in CAP_ROWS:
                grid[(x, y)] = hexcol(CAP_LIT if x == a else CAP_MID if x < a else CAP_DARK)
            elif x == a:
                grid[(x, y)] = hexcol(ARRIS)
            elif x < a:
                grid[(x, y)] = mix(hexcol(SAND_LIT), hexcol(SAND_LIT_EDGE),
                                   (a - x) / max(1, a - lo))
            else:
                grid[(x, y)] = mix(hexcol(SAND_DARK), hexcol(SAND_DARK_EDGE),
                                   (x - a) / max(1, hi - a))

    deepest = max(PYRAMID) - WIDEST_ROW
    for (x, y) in shape:
        if y > WIDEST_ROW:
            grid[(x, y)] = mix(grid[(x, y)], hexcol(SAND_DARK_EDGE),
                               BASE_SHADE * (y - WIDEST_ROW) / deepest)

    # Joints, on opposite diagonals either side of the arris, because two
    # surfaces at an angle to each other do not share a grain direction. Cut
    # into the ramp rather than painted as their own colour, or they flatten
    # it. The shaded face carries them at two thirds the density of the lit
    # one — it is narrower and has the dark outline down its edge, so at
    # matching density it stopped being a surface and became gravel.
    for (x, y) in shape:
        if y in CAP_ROWS or x == ARRIS_X[y]:
            continue
        if x < ARRIS_X[y]:
            if (x + 2 * y) % 4 == 0:
                grid[(x, y)] = mix(grid[(x, y)], (0, 0, 0, 255), JOINT_LIT)
        elif (x - 2 * y) % 6 == 0:
            grid[(x, y)] = mix(grid[(x, y)], (0, 0, 0, 255), JOINT_DARK)

    # The capstone takes its edge on the shaded side only — rimmed both sides
    # it is three short courses with most of them turned to edge colour, and
    # the metal stops being visible at all.
    #
    # Below the cap the outline follows the whole boundary, sides and
    # undersides, but never the top edge. The undersides matter: the base taper
    # steps two columns a row, so outlining the end of each course alone leaves
    # the corners unjoined and the bottom of the pyramid comes out as a scatter
    # of dark dots on the sand instead of a stepped edge.
    for y, xs in rows.items():
        if len(xs) < OUTLINE_MIN_RUN:
            continue
        if y in CAP_ROWS:
            grid[(max(xs), y)] = hexcol(CAP_EDGE)
            continue
        for x in xs:
            if (x - 1, y) not in shape or (x + 1, y) not in shape:
                grid[(x, y)] = hexcol(OUTLINE)
            elif (x, y + 1) not in shape:
                # A tread of the staircase, not a silhouette. Full ink here and
                # the base taper comes out as a heavy dark chevron laid over
                # the sand; this is stone meeting the ground, so it wants a
                # contact shadow instead.
                grid[(x, y)] = hexcol(UNDERSIDE)


def storm_streaks(level):
    out = list(STORM_WEAK)
    if level >= STORM_MID_AT:
        out += list(STORM_MID)
    if level >= STORM_HARD_AT:
        out += list(STORM_HARD)
    return out


def draw_storm(grid, frame, level):
    """Grains over the sprite. Solid where there is nothing behind them, mixed
    into whatever is there where there is — a grain crossing the pyramid is
    dust in front of it, and drawn solid it punches a hole in the face."""
    for x0, y, length, drift in storm_streaks(level):
        head = (x0 + frame * drift) % SIZE
        for i in range(length):
            p = ((head - i) % SIZE, y)
            if p in grid:
                grid[p] = mix(grid[p], hexcol(GRAIN), 0.55 if i == 0 else 0.30)
            else:
                grid[p] = hexcol(GRAIN if i == 0 else GRAIN_TAIL)


def apply_haze(grid, level):
    if level <= 0:
        return
    t = HAZE_MAX * level
    for p in grid:
        grid[p] = mix(grid[p], hexcol(HAZE), t)


def draw_debris(grid, bricks, step):
    """Stone torn off the pyramid, carried right and up by the wind. `step` is
    how many frames ago it came away.

    The scatter is not decoration. Bricks off the same course all shift by the
    same amount, so carried off on a shared offset they stay shoulder to
    shoulder and land as one long brown bar across the sky."""
    for (y, x, w) in bricks:
        dx = 3 * step + x % 3
        dy = step + (x // 2) % 2
        for i in range(w):
            p = (x + i + dx, y - dy)
            if 0 <= p[0] < SIZE and 0 <= p[1] < SIZE:
                grid[p] = hexcol(RUBBLE_LIT if step == 1 else RUBBLE)


# --- bricks in the air ---------------------------------------------------------
#
# Every brick is thrown onto the pyramid from off-sprite. The ground it comes
# off is below the frame and never in it, so a brick is invisible until it
# crosses an edge and it arrives already travelling.
#
# FLIGHT_FROM is well below row 15 on purpose. Launched just off the bottom
# edge the first drawn position is already halfway up the sprite, and the throw
# reads as starting in mid-air.

FLIGHT = 8
FLIGHT_FROM = 26.0
FLIGHT_SIDE = 9.0
# High enough that the lob spends most of its flight in open sky. A flat arc
# keeps the stone down among the ground it is being thrown onto, where a
# sand-coloured chip on sand is not visible at all.
FLIGHT_ARC = 12.0
# And dark, for the same reason. Every other loose thing in the sprite — the
# grains, the haze — is pale warm sand; stone in the air has to be the one
# thing that is not, or it reads as more weather.
FLIGHT_STONE = "#8A6D2D"
FLIGHT_STONE_LIT = "#B08D45"


def flight_pos(brick, t):
    """Where a brick is a fraction `t` through its flight. The arc is a plain
    parabola: without it a brick slides up a straight line and reads as being
    winched rather than thrown."""
    y, x, _w = brick
    side = -1 if scatter(x, y) < 0.5 else 1
    lx = x + side * FLIGHT_SIDE
    px = lx + (x - lx) * t
    py = FLIGHT_FROM + (y - FLIGHT_FROM) * t - FLIGHT_ARC * 4 * t * (1 - t)
    return px, py


def draw_flight(grid, brick, t):
    px, py = flight_pos(brick, t)
    x, y = int(round(px)), int(round(py))
    for i in range(brick[2]):
        p = (x + i, y)
        if 0 <= p[0] < SIZE and 0 <= p[1] < SIZE:
            grid[p] = hexcol(FLIGHT_STONE_LIT if i == 0 else FLIGHT_STONE)


def render(shape, rubble=None, level=0.0, frame=0, debris=(), flying=(), field=0.0):
    grid = {}
    draw_sky(grid)
    draw_ground(grid)
    draw_debris_field(grid, field)
    if rubble:
        draw_rubble(grid, rubble)
    shade_pyramid(grid, shape)
    apply_haze(grid, level)
    for bricks, step in debris:
        draw_debris(grid, bricks, step)
    for brick, t in flying:
        draw_flight(grid, brick, t)
    draw_storm(grid, frame, level)

    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()
    for (x, y), col in grid.items():
        px[x, y] = col
    return img


# --- take one: the pyramid standing -------------------------------------------
#
# Weather and nothing else. Two ticks a frame — a weak storm that steps a
# column every tick is not weak, it is a fan.

STILL_FRAMES = 16
STILL_FRAMETIME = 2

assert STILL_FRAMES % STORM_PERIOD == 0


def build_still():
    full = spans(PYRAMID)
    return [render(full, level=0.0, frame=i) for i in range(STILL_FRAMES)]


# --- take two: struck down and thrown back up ----------------------------------

STAND_N = 44      # it stands, weather only
RISE_N = 8        # the storm gets up
STRIP_N = 8       # the cap goes, then course after course
FALL_N = 6        # what is left comes apart
PAUSE_N = 24      # bare desert, storm dying off it
BUILD_N = 54      # 44 bricks thrown back on, then it stands again

CYCLE_FRAMES = STAND_N + RISE_N + STRIP_N + FALL_N + PAUSE_N + BUILD_N
CYCLE_FRAMETIME = 1

assert BUILD_N >= len(BRICKS), "build phase is shorter than the brick count"
assert CYCLE_FRAMES % STORM_PERIOD == 0, (
    "cycle: %d frames is not a whole number of %d-frame storm cycles"
    % (CYCLE_FRAMES, STORM_PERIOD))

# Bricks gone by the end of each strip frame, stopping at every course above
# the base. Landing on a course boundary is deliberate: the collapse profiles
# are hand-drawn and have to start from a shape the strip actually leaves.
STRIP_REMOVED = (5, 9, 13, 18, 22, 27, 31, 35)
assert STRIP_REMOVED[-1] == sum(
    1 for y, _, _ in BRICKS if y <= max(PYRAMID) - BASE_COURSES)

# The collapse, hand-authored per frame. It comes apart inside the footprint it
# stood on and ends at nothing.
#
# It used to settle into a mound spread across the bottom two rows of the
# frame, which is neither where the base was nor anywhere a rebuilt pyramid
# covers, so a bar of stone sat under an empty desert for the whole pause. What
# the wind takes is carried downwind by draw_debris; what is left behind, if
# anything, is DEBRIS_FIELD.
FALL_PROFILE = (
    {11: [(3, 12)], 12: [(5, 10)], 13: [(7, 8)]},
    {11: [(3, 12)], 12: [(5, 10)]},
    {11: [(4, 11)], 12: [(6, 9)]},
    {11: [(5, 10)]},
    {11: [(6, 9)]},
    {},
)


def cycle_level(phase, k):
    if phase == "stand":
        return 0.0
    if phase == "rise":
        return 0.12 + 0.88 * (k + 1) / RISE_N
    if phase == "strip":
        return 1.0
    if phase == "fall":
        return 1.0 - 0.06 * k
    if phase == "pause":
        return max(0.0, 0.45 - 0.03 * k)
    return 0.0


def cycle_frame(i):
    for phase, n in (("stand", STAND_N), ("rise", RISE_N), ("strip", STRIP_N),
                     ("fall", FALL_N), ("pause", PAUSE_N), ("build", BUILD_N)):
        if i < n:
            return phase, i
        i -= n
    raise AssertionError("frame past the end of the strip")


def bricks_in_air(bk):
    """Whatever is mid-flight at build-relative frame `bk`. `bk` goes negative
    on purpose: the first bricks are already up during the last few frames of
    the pause, so the rebuild announces itself before the first one lands."""
    out = []
    for i, brick in enumerate(BUILD_ORDER):
        gap = i - bk
        if 0 < gap <= FLIGHT:
            out.append((brick, 1.0 - gap / FLIGHT))
    return out


def field_density(phase, k, on):
    """Debris fades up as the pyramid comes apart, holds through the pause, and
    thins back out as the courses go down — gone by the time the last brick
    lands, so the strip loops onto a clean frame 0."""
    if not on:
        return 0.0
    if phase == "fall":
        return DEBRIS_DENSITY * (k + 1) / FALL_N
    if phase == "pause":
        return DEBRIS_DENSITY
    if phase == "build":
        return DEBRIS_DENSITY * max(0.0, 1.0 - k / len(BRICKS))
    return 0.0


def build_cycle(field=False):
    full = spans(PYRAMID)
    frames = []
    for i in range(CYCLE_FRAMES):
        phase, k = cycle_frame(i)
        level = cycle_level(phase, k)
        shape, rubble, debris, flying = full, None, [], []

        if phase == "strip":
            gone = STRIP_REMOVED[k]
            shape = brick_pixels(BRICKS) - brick_pixels(ERODE_ORDER[:gone])
            prev = STRIP_REMOVED[k - 1] if k else 0
            debris = [(ERODE_ORDER[prev:gone], 1)]
            if k:
                start = STRIP_REMOVED[k - 2] if k > 1 else 0
                debris.append((ERODE_ORDER[start:prev], 2))
        elif phase == "fall":
            # Only the first frame is still standing masonry — it is the shape
            # the strip left, and turning it to rubble on the phase boundary
            # pops. After that it is coming apart, and it has to be drawn as
            # rubble: the profiles are one and two courses tall, so run through
            # the stone shader every pixel in them has open air underneath, the
            # underside rule fires on all of it, and each profile lands as a
            # solid dark bar across the sprite.
            if k == 0:
                shape = spans(FALL_PROFILE[0])
            else:
                shape = set()
                rubble = FALL_PROFILE[k] or None
                lost = spans(FALL_PROFILE[k - 1]) - spans(FALL_PROFILE[k])
                debris = [([(y, x, 1) for (x, y) in lost], 1)]
        elif phase == "pause":
            shape = set()
            flying = bricks_in_air(k - PAUSE_N)
        elif phase == "build":
            shape = brick_pixels(BUILD_ORDER[:min(k, len(BRICKS))])
            flying = bricks_in_air(k)

        frames.append(render(shape, rubble=rubble, level=level, frame=i,
                             debris=debris, flying=flying,
                             field=field_density(phase, k, field)))
    return frames


# --- output -------------------------------------------------------------------
#
# (directory, texture name, take). Everything here is written on every run.
#
# The collapse is the live item texture. The other two are kept because berlord
# may want them later: they land in texture-work/variants as real strips with
# their .mcmeta beside them, ready to drop into an assets folder, but outside
# resources so they are not packaged. Writing them every run rather than
# checking in a one-off export is what stops them drifting out of step with the
# silhouette, the scene and the storm, which all three takes share.
OUTPUTS = (
    (TEX_ITEM, "desert_core", "collapse"),
    (VARIANTS, "desert_core_still", "still"),
    (VARIANTS, "desert_core_debris", "debris"),
)

TAKES = {
    "still": (build_still, STILL_FRAMETIME),
    "collapse": (lambda: build_cycle(field=False), CYCLE_FRAMETIME),
    "debris": (lambda: build_cycle(field=True), CYCLE_FRAMETIME),
}


def strip_image(frames):
    """The frames stacked top to bottom, which is the layout Minecraft wants."""
    strip = Image.new("RGBA", (SIZE, SIZE * len(frames)), (0, 0, 0, 0))
    for i, frame in enumerate(frames):
        strip.paste(frame, (0, i * SIZE))
    return strip


def dump(img):
    """Rough ASCII of one frame, for checking the silhouette by eye."""
    px = img.load()
    for y in range(SIZE):
        row = ""
        for x in range(SIZE):
            r, g, b, a = px[x, y]
            lum = (r + g + b) / 3
            row += "." if a == 0 else " #+=-o*"[min(int(lum / 255 * 6) + 1, 6)]
        print(row)


if __name__ == "__main__":
    for _, _, _take in OUTPUTS:
        assert _take in TAKES, "unknown take %r" % _take

    first = None
    for folder, name, take in OUTPUTS:
        build, frametime = TAKES[take]
        frames = build()
        os.makedirs(folder, exist_ok=True)
        strip_image(frames).save(os.path.join(folder, name + ".png"))
        with open(os.path.join(folder, name + ".png.mcmeta"), "w", newline="\n") as fh:
            json.dump({"animation": {"frametime": frametime}}, fh, indent=2)
            fh.write("\n")
        print("wrote %s/%s.png (%s, %d frames) and its .mcmeta"
              % (os.path.basename(folder), name, take, len(frames)))
        first = first or frames[0]
    if "--ascii" in sys.argv:
        dump(first)
