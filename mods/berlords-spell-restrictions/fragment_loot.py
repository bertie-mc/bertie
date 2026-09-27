"""Write the knowledge-fragment loot: one loot table and one global loot modifier per source.

Each modifier is Iron's Spells' `append_loot`, which rolls one of this mod's tables into the loot a
matched chest or kill already produces. A source's school addon gates both files, so an absent
addon leaves no stray fragments or unknown items behind.

    python fragment_loot.py
"""
import json
import os

ROOT = os.path.dirname(os.path.abspath(__file__))
DATA = os.path.join(ROOT, "src", "main", "resources", "data")
ID = "berlordsspellrestrictions"

FRAGMENTS = {
    "occult": (f"{ID}:forbidden_knowledge_fragment", ["discerning_the_eldritch"]),
    "abyssal": (f"{ID}:depth_knowledge_fragment", ["cataclysm_spellbooks"]),
    "burning": ("cataclysm_spellbooks:burning_knowledge_fragment", ["cataclysm_spellbooks"]),
    "frozen": ("cataclysm_spellbooks:frozen_knowledge_fragment", ["cataclysm_spellbooks"]),
}


def chance(p, lo, hi=None):
    """One roll: p chance of lo..hi fragments."""
    return {"rolls": 1, "chance": p, "count": (lo, hi or lo)}


def rolls(n, p):
    """n rolls, each p chance of one fragment."""
    return {"rolls": n, "each": p}


def binomial(n, p):
    """One roll of a binomial count, as Discerning the Eldritch drops its fragments."""
    return {"rolls": 1, "binomial": (n, p)}


def kill(lo, hi):
    """Guaranteed lo..hi fragments from a boss the player killed."""
    return {"rolls": 1, "count": (lo, hi), "kill": True}


SUNKEN_CITY_CHESTS = ["minecraft:chests/shipwreck_map", "minecraft:chests/shipwreck_treasure",
                      "minecraft:chests/shipwreck_supply", "minecraft:chests/simple_dungeon"]

# school: [(source, [loot tables], drop, extra modifier conditions, extra mods)]
SOURCES = {
    "occult": [
        ("cult_of_azazel_rare", ["netherman:chests/mansion_rare_loot"], chance(0.8, 1, 3)),
        ("cult_of_azazel", ["netherman:chests/mansion_ordinary_loot"], rolls(3, 0.15)),
        ("nether_fortress", ["minecraft:chests/nether_bridge"], rolls(3, 0.25)),
        ("fortress_worship", ["betterfortresses:chests/worship"], chance(0.8, 1, 3)),
        ("blood_cultist_hut", ["discerning_the_eldritch:chests/blood_cultist_hut/bloody_chest"],
         chance(0.5, 1)),
        ("cultist_base", [f"discerning_the_eldritch:chests/cultist_base/{t}"
                          for t in ("library_loot", "lab_chest", "cultist_vault", "ascended_vault")],
         binomial(4, 0.25)),
        ("ascended_one", ["discerning_the_eldritch:entities/ascended_one"], kill(2, 4)),
    ],
    "abyssal": [
        ("elder_guardian", ["minecraft:entities/elder_guardian"], kill(1, 2)),
        ("ocean_monument", ["betteroceanmonuments:chests/upper_side_chamber"], chance(0.5, 1, 2)),
        ("sunken_city", SUNKEN_CITY_CHESTS, rolls(3, 0.25),
         [{"condition": "minecraft:location_check", "predicate": {"structures": "cataclysm:sunken_city"}}],
         ["cataclysm"]),
        ("leviathan", ["cataclysm:entities/the_leviathan"], kill(2, 4)),
        ("acropolis", ["cataclysm:chests/acropolis_treasure"], chance(0.8, 1, 3)),
        ("pirates", ["aquamirae:chests/ship_1", "aquamirae:chests/ship_2"], chance(0.2, 1, 2)),
        ("shipwrecks", ["minecraft:chests/shipwreck_map", "minecraft:chests/shipwreck_treasure",
                        "minecraft:chests/shipwreck_supply", "minecraft:chests/underwater_ruin_small",
                        "minecraft:chests/underwater_ruin_big"], chance(0.1, 1)),
    ],
    "burning": [
        ("ignis", ["cataclysm:entities/ignis"], kill(3, 5)),
        ("netherite_monstrosity", ["cataclysm:entities/netherite_monstrosity"], kill(1, 2)),
        ("citadel_vault", ["irons_spellbooks:chests/citadel/citadel_vault"], chance(0.5, 1, 2)),
        ("citadel", ["irons_spellbooks:chests/citadel/rampart_chest"], chance(0.15, 1)),
        ("pyromancer_tower", ["irons_spellbooks:chests/pyromancer_tower/burnt_chest",
                              "irons_spellbooks:chests/pyromancer_tower/pyromancer_basic_storage"],
         chance(0.2, 1)),
    ],
    "frozen": [
        ("frosted_prison", ["cataclysm:chests/frosted_prison_treasure"], chance(0.8, 1, 3)),
        ("maledictus", ["cataclysm:entities/maledictus"], kill(3, 5)),
        ("abandoned_treasure", ["cataclysm:chests/abandoned_treasure"], chance(0.5, 1, 2)),
        ("abandoned", ["cataclysm:chests/abandoned"], chance(0.1, 1)),
        ("ice_spider_den", ["irons_spellbooks:chests/ice_spider_den/tower",
                            "irons_spellbooks:chests/ice_spider_den/dungeon"], chance(0.2, 1)),
        ("impaled_icebreaker", ["irons_spellbooks:chests/impaled_icebreaker/captain_quarters"],
         chance(0.5, 1, 2)),
        ("mountain_tower", ["irons_spellbooks:chests/mountain_tower/mountain_tower"], chance(0.2, 1)),
    ],
}


def write(path, obj):
    full = os.path.join(DATA, *path.split("/"))
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def gate(mods):
    return [{"type": "neoforge:mod_loaded", "modid": m} for m in sorted(set(mods))]


def table(item, drop, mods):
    entry = {"type": "minecraft:item", "name": item}
    pool = {"rolls": drop["rolls"], "bonus_rolls": 0.0, "entries": [entry]}
    if "chance" in drop:
        pool["conditions"] = [{"condition": "minecraft:random_chance", "chance": drop["chance"]}]
    if "each" in drop:
        entry["conditions"] = [{"condition": "minecraft:random_chance", "chance": drop["each"]}]
    if "count" in drop and drop["count"] != (1, 1):
        lo, hi = drop["count"]
        entry["functions"] = [{"function": "minecraft:set_count",
                               "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    if "binomial" in drop:
        n, p = drop["binomial"]
        entry["functions"] = [{"function": "minecraft:set_count",
                               "count": {"type": "minecraft:binomial", "n": n, "p": p}}]
    return {"neoforge:conditions": gate(mods),
            "type": "minecraft:entity" if drop.get("kill") else "minecraft:chest",
            "pools": [pool]}


def main():
    entries = []
    for school, sources in SOURCES.items():
        item, mods = FRAGMENTS[school]
        for source, tables, drop, *rest in sources:
            extra = rest[0] if rest else []
            more_mods = rest[1] if len(rest) > 1 else []
            name = f"fragments/{school}/{source}"
            write(f"{ID}/loot_table/{name}.json", table(item, drop, mods))
            conditions = [{"condition": "minecraft:any_of",
                           "terms": [{"condition": "neoforge:loot_table_id", "loot_table_id": t}
                                     for t in tables]}]
            if drop.get("kill"):
                conditions.append({"condition": "minecraft:killed_by_player"})
            write(f"{ID}/loot_modifiers/{name}.json", {
                "neoforge:conditions": gate(mods + more_mods),
                "type": "irons_spellbooks:append_loot",
                "conditions": conditions + extra,
                "key": f"{ID}:{name}",
            })
            entries.append(f"{ID}:{name}")
    write("neoforge/loot_modifiers/global_loot_modifiers.json", {"replace": False, "entries": entries})
    print(f"{len(entries)} fragment sources")


if __name__ == "__main__":
    main()
