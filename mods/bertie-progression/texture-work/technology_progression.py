"""Utility replacements and predecessor-based machine crafting."""

import copy
import json
from pathlib import Path


REPLACEMENTS = {
    "mekanismgenerators:heat_generator": "oritech:basic_generator_block",
    "mekanismgenerators:advanced_solar_generator": "oritech:big_solar_panel_block",
    "oritech:energy_pipe": "mekanism:basic_universal_cable",
    "oritech:fluid_pipe": "mekanism:basic_mechanical_pipe",
    "oritech:item_pipe": "mekanism:basic_logistical_transporter",
    "oritech:transparent_item_pipe": "mekanism:basic_logistical_transporter",
    "oritech:small_tank_block": "mekanism:basic_fluid_tank",
    "oritech:small_storage_block": "mekanism:basic_energy_cube",
    "oritech:large_storage_block": "mekanism:basic_induction_cell",
}

PREDECESSORS = {
    "data/mekanism/recipe/factory/basic/smelting.json": "mekanism:energized_smelter",
    "data/mekanism/recipe/electric_pump.json": "oritech:pump_block",
    "data/mekanism/recipe/fluid_tank/basic.json": "create:fluid_tank",
    "data/pastel/recipe/pedestal/tier3/block_breaker.json": "create:mechanical_drill",
    "data/pastel/recipe/pedestal/tier1/block_placer.json": "create:deployer",
    "data/anvilcraft/recipe/block_devourer.json": "pastel:block_breaker",
    "data/anvilcraft/recipe/block_placer.json": "pastel:block_placer",
    "data/oritech/recipe/crafting/destroyer.json": "anvilcraft:block_devourer",
    "data/oritech/recipe/crafting/placer.json": "anvilcraft:block_placer",
}


def _strings(value):
    if isinstance(value, str):
        yield value
    elif isinstance(value, dict):
        for child in value.values():
            yield from _strings(child)
    elif isinstance(value, list):
        for child in value:
            yield from _strings(child)


def _replace(value):
    if isinstance(value, str):
        return REPLACEMENTS.get(value, value)
    if isinstance(value, dict):
        return {key: _replace(child) for key, child in value.items()}
    if isinstance(value, list):
        return [_replace(child) for child in value]
    return value


def _require(recipe, *items):
    conditions = recipe.setdefault("neoforge:conditions", [])
    present = {c.get("modid") for c in conditions if c.get("type") == "neoforge:mod_loaded"}
    for namespace in sorted({item.split(":", 1)[0] for item in items} - {"minecraft"} - present):
        conditions.append({"type": "neoforge:mod_loaded", "modid": namespace})


def with_predecessor(recipe, predecessor):
    result = copy.deepcopy(recipe)
    if predecessor in set(_strings(result.get("key", {}))):
        return result
    rows = [list(row.ljust(3)) for row in result["pattern"]]
    if len(rows) != 3 or any(len(row) != 3 for row in rows):
        raise ValueError("Predecessor upgrades require a three-by-three recipe")
    symbol = next(ch for ch in "UPQZ" if ch not in result["key"])
    old = rows[1][1]
    # Spare cells preserve the original centre's material cost alongside the predecessor.
    if old != " ":
        spare = next(((y, x) for y in range(3) for x in range(3) if rows[y][x] == " "), None)
        if spare is not None:
            rows[spare[0]][spare[1]] = old
    rows[1][1] = symbol
    result["pattern"] = ["".join(row) for row in rows]
    result["key"][symbol] = {"item": predecessor}
    used = set("".join(result["pattern"]))
    result["key"] = {key: value for key, value in result["key"].items() if key in used}
    _require(result, predecessor)
    return result


def apply_recipes(write, recipes, resources, removed):
    from recipe_outputs import item_outputs

    resources = Path(resources)

    def effective(path):
        local = resources / path
        return json.loads(local.read_text(encoding="utf-8")) if local.is_file() else copy.deepcopy(recipes[path])

    for path in sorted(recipes):
        recipe = effective(path)
        if {"type": "neoforge:false"} in recipe.get("neoforge:conditions", []):
            continue
        if item_outputs(recipe, set()) & removed:
            continue
        retiring = set(_strings(recipe)) & REPLACEMENTS.keys()
        if retiring:
            fixed = _replace(recipe)
            _require(fixed, *(REPLACEMENTS[item] for item in retiring))
            write(path, fixed)

    for path, predecessor in PREDECESSORS.items():
        source = effective(path) if recipes[path].get("type") == "pastel:pedestal" else _replace(recipes[path])
        write(path, with_predecessor(source, predecessor))

    write("data/anvilcraft/tags/block/block_devourer_probability_dropping.json", {
        "replace": True, "values": []})
