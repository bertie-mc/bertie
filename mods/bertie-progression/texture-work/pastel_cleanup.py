"""Temporary crafting routes and ingredient replacements for retained Pastel content."""

import copy
import json
from pathlib import Path


REPLACEMENTS = {
    "pastel:shimmerstone_gem": "minecraft:glowstone",
    "pastel:shimmerstone_block": "minecraft:glowstone",
    "pastel:stratine_gem": "minecraft:magma_cream",
    "pastel:stratine_fragments": "minecraft:magma_cream",
    "pastel:onyx_powder": "pastel:onyx_shard",
    "pastel:moonstone_powder": "pastel:moonstone_shard",
    "pastel:amethyst_powder": "minecraft:amethyst_shard",
    "#pastel:pigments": "#c:dyes",
    "pastel:pigments": "c:dyes",
    "pastel:topaz_shard": "minecraft:amethyst_shard",
    "pastel:citrine_shard": "minecraft:amethyst_shard",
    "pastel:polished_topaz_block": "pastel:polished_amethyst_block",
    "pastel:polished_citrine_block": "pastel:polished_amethyst_block",
    "pastel:topaz_block": "minecraft:amethyst_block",
    "pastel:citrine_block": "minecraft:amethyst_block",
    "pastel:pure_quartz": "minecraft:quartz",
    "pastel:pure_diamond": "minecraft:diamond",
    "pastel:pure_emerald": "minecraft:emerald",
    "pastel:pure_iron": "minecraft:iron_ingot",
    "pastel:pure_gold": "minecraft:gold_ingot",
    "pastel:pure_gold_block": "minecraft:gold_block",
    "pastel:pure_prismarine": "minecraft:prismarine_crystals",
    "pastel:pure_redstone": "minecraft:redstone",
    "pastel:pure_netherite_scrap": "minecraft:netherite_scrap",
}
REPLACEMENTS.update({f"pastel:{color}_pigment": f"minecraft:{color}_dye" for color in (
    "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
    "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black")})
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
                key[symbol] = vanilla_ingredient(ingredient)
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


def vanilla_ingredient(value):
    if isinstance(value, str):
        return {"tag": value[1:]} if value.startswith("#") else {"item": value}
    if isinstance(value, list):
        return [vanilla_ingredient(child) for child in value]
    if isinstance(value, dict) and "components" in value and "type" not in value:
        return {"type": "neoforge:components", "items": value.get("item", value.get("id")),
                "components": copy.deepcopy(value["components"])}
    return copy.deepcopy(value)


def shapeless_recipe(recipe, inputs):
    converted = {"type": "minecraft:crafting_shapeless", "category": "misc",
                 "ingredients": [vanilla_ingredient(value) for value in inputs if value not in ("minecraft:air", [])],
                 "result": copy.deepcopy(recipe["result"])}
    converted["neoforge:conditions"] = copy.deepcopy(recipe.get("neoforge:conditions", []))
    return converted


def scalar_values(value):
    if isinstance(value, str):
        yield value
    elif isinstance(value, list):
        for child in value:
            yield from scalar_values(child)
    elif isinstance(value, dict):
        for child in value.values():
            yield from scalar_values(child)


def input_ids(recipe):
    return set(scalar_values({key: value for key, value in recipe.items()
                             if key not in {"type", "result", "results", "output", "outputs",
                                            "colors", "neoforge:conditions"}}))


def apply_policy(write, recipes, resource_dir, written, removed, disabled):
    pending_path = Path(resource_dir) / "pastel_recipe_pending.json"
    previous_pending = {entry["path"] for entry in json.loads(pending_path.read_text(encoding="utf-8"))} \
        if pending_path.exists() else set()
    current = copy.deepcopy(recipes)
    for path in (Path(resource_dir) / "data").rglob("*.json"):
        relative = path.relative_to(resource_dir).as_posix()
        if "/recipe/" in relative and relative not in current:
            current[relative] = json.loads(path.read_text(encoding="utf-8"))
    for path in set(written):
        if "/recipe/" in path and path.endswith(".json"):
            current[path] = json.loads((Path(resource_dir) / path).read_text(encoding="utf-8"))
    changed = 0
    pending = []
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
        source = copy.deepcopy(recipe)
        if output == "pastel:onyx_shard" and source.get("type") == "pastel:fusion_shrine":
            source["ingredients"] = [value for value in source["ingredients"]
                                     if not isinstance(value, str)
                                     or value not in {"pastel:topaz_shard", "pastel:citrine_shard"}]
        fixed = replace_inputs(source)
        if path == "data/pastel/recipe/pedestal/tier2/fusion_shrine_basalt.json":
            fixed["key"]["T"] = "pastel:polished_amethyst_block"
            fixed["key"]["C"] = "pastel:polished_amethyst_block"
        if output == "pastel:onyx_shard" and fixed.get("type") == "pastel:fusion_shrine":
            fixed["ingredients"] = [ingredient for ingredient in fixed["ingredients"]
                                    if not isinstance(ingredient, str)
                                    or ingredient not in {"pastel:topaz_shard", "pastel:citrine_shard"}]
        if output == "pastel:bottle_of_ruin" and fixed.get("type") == "pastel:pedestal":
            fixed = crafting_recipe(fixed)
        missing = sorted(value for value in input_ids(fixed) & removed if value.startswith("pastel:"))
        reason = None
        if missing:
            reason = "removed ingredients"
        elif fixed.get("type") == "pastel:pedestal":
            fixed = crafting_recipe(fixed)
        elif fixed.get("type") == "pastel:pedestal_shapeless":
            fixed = shapeless_recipe(fixed, fixed["ingredients"])
        elif fixed.get("type") == "pastel:spirit_instiller":
            center = copy.deepcopy(fixed["center_ingredient"])
            count = center.pop("count", 1) if isinstance(center, dict) else 1
            center = vanilla_ingredient(center)
            inputs = [center] * count + [fixed["ingredient1"], fixed["ingredient2"]]
            if len(inputs) <= 9:
                fixed = shapeless_recipe(fixed, inputs)
            else:
                reason = "more than nine crafting inputs"
        elif fixed.get("type") in {"pastel:potion_workshop_crafting", "pastel:titration_barrel", "pastel:cinderhearth"}:
            reason = "removed workstation"
        if reason:
            pending.append({"path": path, "output": output, "reason": reason, "ingredients": missing})
            fixed = copy.deepcopy(disabled)
        if fixed != recipe or path in previous_pending:
            if path.startswith("data/pastel/"):
                condition = {"type": "neoforge:mod_loaded", "modid": "pastel"}
                if condition not in fixed.setdefault("neoforge:conditions", []):
                    fixed["neoforge:conditions"].append(condition)
            write(path, fixed)
            changed += 1
    write("pastel_recipe_pending.json", pending)
    return changed
