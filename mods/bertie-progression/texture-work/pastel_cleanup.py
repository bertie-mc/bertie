"""Temporary crafting routes and ingredient replacements for retained Pastel content."""

import copy
import json
from pathlib import Path


REPLACEMENTS = {
    "pastel:shimmerstone_gem": "minecraft:glowstone",
    "pastel:stratine_gem": "minecraft:magma_cream",
    "pastel:stratine_fragments": "minecraft:magma_cream",
    "pastel:onyx_powder": "pastel:onyx_shard",
}
CRAFTING_OUTPUTS = {
    "anvilcraft:anvil_hammer", "pastel:bottle_of_fading", "pastel:bottle_of_failing",
    "pastel:bottle_of_ruin", "pastel:enchanter", "pastel:phantom_frame",
    "pastel:glow_phantom_frame", "irons_spellbooks:cinderous_soulcaller",
    "cataclysm:flame_eye", "hazennstuff:rose_quartz",
    "pastel:amethyst_glass", "pastel:amethyst_glass_pane",
    "pastel:onyx_glass", "pastel:onyx_glass_pane",
    "pastel:moonstone_glass", "pastel:moonstone_glass_pane",
}
UNCRAFTABLE_ARMOUR = {
    "pastel:bedrock_helmet", "pastel:bedrock_chestplate", "pastel:bedrock_leggings",
}
REMOVED_PIGMENTS = {"pastel:pink_pigment", "pastel:orange_pigment"}


def replace_inputs(value):
    if isinstance(value, str):
        return REPLACEMENTS.get(value, value)
    if isinstance(value, list):
        return [replace_inputs(child) for child in value]
    if isinstance(value, dict):
        return {key: copy.deepcopy(child) if key in {"result", "results", "output", "outputs"}
                else replace_inputs(child) for key, child in value.items()}
    return value


def crafting_recipe(recipe):
    """Keep the pedestal's grid and output, removing its ink and machine requirements."""
    result = copy.deepcopy(recipe["result"])
    if result["id"] == "pastel:bottle_of_ruin":
        pattern, key = ["GGG", "GGG", "GGG"], {"G": {"item": "minecraft:grass_block"}}
    else:
        key = {}
        omitted = set()
        for symbol, ingredient in recipe["key"].items():
            if isinstance(ingredient, str) and ingredient in REMOVED_PIGMENTS:
                omitted.add(symbol)
            else:
                key[symbol] = ({"tag": ingredient[1:]} if ingredient.startswith("#") else {"item": ingredient}) \
                    if isinstance(ingredient, str) else ingredient
        pattern = ["".join(" " if symbol in omitted else symbol for symbol in row)
                   for row in recipe["pattern"]]
        used = set("".join(pattern)) - {" "}
        key = {symbol: ingredient for symbol, ingredient in key.items() if symbol in used}
    converted = {"type": "minecraft:crafting_shaped", "category": "misc",
                 "pattern": pattern, "key": key, "result": result}
    if recipe.get("neoforge:conditions"):
        converted["neoforge:conditions"] = copy.deepcopy(recipe["neoforge:conditions"])
    mods = {result["id"].split(":")[0]}
    for ingredient in key.values():
        if "item" in ingredient:
            mods.add(ingredient["item"].split(":")[0])
    conditions = converted.setdefault("neoforge:conditions", [])
    for mod in sorted(mods - {"minecraft", "bertieprogression"}):
        condition = {"type": "neoforge:mod_loaded", "modid": mod}
        if condition not in conditions:
            conditions.append(condition)
    return converted


def apply_policy(write, recipes, resource_dir, written, removed, disabled):
    current = copy.deepcopy(recipes)
    for path in (Path(resource_dir) / "data").rglob("*.json"):
        relative = path.relative_to(resource_dir).as_posix()
        if "/recipe/" in relative and relative not in current:
            current[relative] = json.loads(path.read_text(encoding="utf-8"))
    for path in set(written):
        if "/recipe/" in path and path.endswith(".json"):
            current[path] = json.loads((Path(resource_dir) / path).read_text(encoding="utf-8"))
    changed = 0
    for path, recipe in sorted(current.items()):
        if {"type": "neoforge:false"} in recipe.get("neoforge:conditions", []):
            continue
        result = recipe.get("result", {})
        output = result.get("id", result.get("item")) if isinstance(result, dict) else result
        if not isinstance(output, str):
            output = None
        if output in UNCRAFTABLE_ARMOUR:
            write(path, disabled)
            changed += 1
            continue
        if output in removed:
            # Native paths remain owned by the reversible removal manifest.
            if output.startswith("pastel:") and path not in recipes:
                write(path, disabled)
                changed += 1
            continue
        fixed = replace_inputs(recipe)
        if output == "pastel:onyx_shard" and fixed.get("type") == "pastel:fusion_shrine":
            fixed["ingredients"] = [ingredient for ingredient in fixed["ingredients"]
                                    if not isinstance(ingredient, str)
                                    or ingredient not in {"pastel:topaz_shard", "pastel:citrine_shard"}]
        if fixed.get("type") == "pastel:pedestal" and output in CRAFTING_OUTPUTS:
            fixed = crafting_recipe(fixed)
        if fixed != recipe:
            if path.startswith("data/pastel/"):
                condition = {"type": "neoforge:mod_loaded", "modid": "pastel"}
                if condition not in fixed.setdefault("neoforge:conditions", []):
                    fixed["neoforge:conditions"].append(condition)
            write(path, fixed)
            changed += 1
    return changed
