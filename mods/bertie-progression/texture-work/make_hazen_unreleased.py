"""Inventory sprites for five Hazen 'n Stuff items the mod ships art for but never registers.

Each one is a Geckolib model: a `.geo.json` plus one UV atlas, drawn in the world by a renderer
that only runs for items the mod itself registered. Registering them from this side gets the item
but not that renderer, so the model is projected here, once, into an ordinary flat sprite.
"""
import io
import json
import os
import zipfile

from PIL import Image

import geo_render

JAR_DIR = os.path.join(os.environ.get("APPDATA", ""), "PrismLauncher", "instances",
                       "bertie-no-worldgen", "minecraft", "mods")
OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                   "assets", "bertieprogression", "textures", "item", "hazen")
SIZE = 64

# id -> (geometry, texture, yaw, pitch)
ITEMS = {
    "chronicles_of_neptune": ("geo/curios/chronicles_of_neptune.geo.json",
                              "textures/curios/chronicles_of_neptune.png", -35, 22),
    "ebony_scroll": ("geo/curios/ebony_scroll.geo.json",
                     "textures/curios/ebony_scroll.png", -35, 18),
    "lunarnomicon": ("geo/curios/lunarnomicon.geo.json",
                     "textures/curios/lunarnomicon.png", -35, 22),
    "radiant_crown_of_scrolls": ("geo/curios/radiant_crown_of_scrolls.geo.json",
                                 "textures/curios/radiant_crown_of_scrolls.png", -15, 8),
    "grimoire_of_flight": ("geo/item/staves/grimoire_staff.geo.json",
                           "textures/item/staves/grimoire_staff.png", -40, 18),
}


def hazen_jar():
    for name in sorted(os.listdir(JAR_DIR)):
        if name.startswith("hazennstuff-") and name.endswith(".jar"):
            return os.path.join(JAR_DIR, name)
    raise SystemExit("hazennstuff jar not found; the instance has to be present to read its art")


def main():
    os.makedirs(OUT, exist_ok=True)
    with zipfile.ZipFile(hazen_jar()) as jar:
        for name, (geo, tex, yaw, pitch) in ITEMS.items():
            model = json.loads(jar.read(f"assets/hazennstuff/{geo}"))
            atlas = Image.open(io.BytesIO(jar.read(f"assets/hazennstuff/{tex}"))).convert("RGBA")
            img = geo_render.render(model, atlas, size=SIZE, yaw=yaw, pitch=pitch)
            img.save(os.path.join(OUT, f"{name}.png"))
            print(f"  {name}.png")
    print(f"Wrote {len(ITEMS)} sprites to {os.path.normpath(OUT)}")


if __name__ == "__main__":
    main()
