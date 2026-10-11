"""Normalize large slate silhouettes without changing existing visible artwork.

Each 16x16 sprite has a 14x14 opaque face with one pixel clipped from each corner.
Missing pixels reuse the closest existing colour; cross-seam gaps prefer darker ties.
Run with --check to validate without writing.
"""
import argparse
from pathlib import Path
from PIL import Image

TEXTURES = Path(__file__).resolve().parent.parent / 'src/main/resources/assets/berlordscarving/textures/item'


def inside(x, y):
    return 1 <= x <= 14 and 1 <= y <= 14 and (x, y) not in ((1, 1), (14, 1), (1, 14), (14, 14))


def normalize(image):
    if image.size != (16, 16):
        raise ValueError(f'Expected a 16x16 sprite, got {image.size}')
    original = image.convert('RGBA')
    output = original.copy()
    colours = [(x, y, original.getpixel((x, y))) for y in range(16) for x in range(16)
               if original.getpixel((x, y))[3] and inside(x, y)]
    if not colours:
        raise ValueError('Slate has no visible face pixels')
    added = removed = 0
    for y in range(16):
        for x in range(16):
            pixel = original.getpixel((x, y))
            if not inside(x, y):
                if pixel[3]:
                    removed += 1
                output.putpixel((x, y), (0, 0, 0, 0))
            elif not pixel[3]:
                distance = min(abs(px - x) + abs(py - y) for px, py, _ in colours)
                candidates = [(px, py, colour) for px, py, colour in colours
                              if abs(px - x) + abs(py - y) == distance]
                if x in (7, 8) or y in (7, 8):
                    candidates.sort(key=lambda p: (sum(p[2][:3]), p[1], p[0]))
                else:
                    candidates.sort(key=lambda p: (abs(p[1] - y), p[1], p[0]))
                output.putpixel((x, y), candidates[0][2])
                added += 1
    return output, added, removed


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    changed = 0
    for path in sorted(TEXTURES.glob('*_big_slate.png')):
        with Image.open(path) as image:
            original = image.convert('RGBA')
        normalized, added, removed = normalize(original)
        if original.tobytes() != normalized.tobytes():
            changed += 1
            print(f'{path.name}: fill {added}, remove {removed}')
            if not args.check:
                normalized.save(path)
        for y in range(16):
            for x in range(16):
                if inside(x, y) and original.getpixel((x, y))[3]:
                    assert original.getpixel((x, y)) == normalized.getpixel((x, y))
    if args.check and changed:
        raise SystemExit(f'{changed} large slate silhouettes need normalization')
    print(f'{changed} sprites corrected' if changed else 'All large slate silhouettes are normalized')


if __name__ == '__main__':
    main()
