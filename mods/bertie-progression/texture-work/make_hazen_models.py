"""Vanilla item models for five Hazen 'n Stuff items the mod ships geometry for but never registers.

Each one is a Geckolib model that the mod draws only for items it registered itself. Registering
them from this side gets the item but not that renderer, so the geometry is converted once, here,
into ordinary model elements the vanilla item renderer can draw.

Writes into bertie-progression's resources under assets/hazennstuff/, which wins because this mod
is ordered AFTER hazennstuff.
"""
import json
import os
import zipfile

import geo_to_item_model

NL = chr(10)

JAR_DIR = os.path.join(os.environ.get("APPDATA", ""), "PrismLauncher", "instances",
                       "bertie-no-worldgen", "minecraft", "mods")
RES = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources")
OUT = os.path.join(RES, "assets", "hazennstuff", "models", "item")
# Their art sits under textures/curios/ and textures/item/staves/, which the block atlas does not
# sweep, because Geckolib binds a texture directly and never needs it stitched. A vanilla model
# does, so each one is named as an atlas source. NeoForge merges these across mods.
ATLAS = os.path.join(RES, "assets", "minecraft", "atlases", "blocks.json")

# id -> (geometry, texture), both inside assets/hazennstuff/
ITEMS = {
    "chronicles_of_neptune": ("geo/curios/chronicles_of_neptune.geo.json",
                              "textures/curios/chronicles_of_neptune.png"),
    "ebony_scroll": ("geo/curios/ebony_scroll.geo.json",
                     "textures/curios/ebony_scroll.png"),
    "lunarnomicon": ("geo/curios/lunarnomicon.geo.json",
                     "textures/curios/lunarnomicon.png"),
    "radiant_crown_of_scrolls": ("geo/curios/radiant_crown_of_scrolls.geo.json",
                                 "textures/curios/radiant_crown_of_scrolls.png"),
    "grimoire_of_flight": ("geo/item/staves/grimoire_staff.geo.json",
                           "textures/item/staves/grimoire_staff.png"),
}


def hazen_jar():
    for name in sorted(os.listdir(JAR_DIR)):
        if name.startswith("hazennstuff-") and name.endswith(".jar"):
            return os.path.join(JAR_DIR, name)
    raise SystemExit("hazennstuff jar not found; the instance has to be present to read its models")


def main():
    os.makedirs(OUT, exist_ok=True)
    with zipfile.ZipFile(hazen_jar()) as jar:
        for name, (geo, tex) in ITEMS.items():
            model = geo_to_item_model.convert(
                json.loads(jar.read(f"assets/hazennstuff/{geo}")),
                "hazennstuff:" + tex[len("textures/"):-len(".png")],
                label=name)
            path = os.path.join(OUT, f"{name}.json")
            with open(path, "w", encoding="utf-8", newline="\n") as f:
                json.dump(model, f, indent=2)
                f.write("\n")
            print(f"  {name}.json  ({len(model['elements'])} elements)")
    os.makedirs(os.path.dirname(ATLAS), exist_ok=True)
    sources = [{"type": "minecraft:single",
                "resource": "hazennstuff:" + tex[len("textures/"):-len(".png")]}
               for _, tex in ITEMS.values()]
    with open(ATLAS, "w", encoding="utf-8", newline=NL) as f:
        json.dump({"sources": sources}, f, indent=2)
        f.write(NL)
    if geo_to_item_model.approximated:
        print("angles vanilla cannot hold exactly:")
        for line in geo_to_item_model.approximated:
            print(f"  {line}")
    print(f"Wrote {len(ITEMS)} models to {os.path.normpath(OUT)}")


if __name__ == "__main__":
    main()
