"""Retained processing routes and validation of item ingredients after material merges."""

import copy
import json
import re
from pathlib import Path


RESTORE = set("""
anvilcraft:mesh/cinerite
anvilcraft:mesh/red_sand
anvilcraft:item_crush/flour
anvilcraft:bulging/dough
anvilcraft:super_heating/bronze_ingot
anvilcraft:super_heating/raw/tin_ingot
anvilcraft:super_heating/raw/zinc_ingot
anvilcraft:super_heating/metal_block/tin_block_from_storage_blocks/raw_tin
anvilcraft:super_heating/metal_block/zinc_block_from_storage_blocks/raw_zinc
anvilcraft:time_warp/raw_zinc
anvilcraft:zinc_ingot_from_block
anvilcraft:raw_zinc
create:crushing/asurine
create:crushing/asurine_recycling
create:crushing/tuff
create:crushing/tuff_recycling
create:splashing/crushed_raw_zinc
create:splashing/wheat_flour
create:mixing/dough_by_mixing
create:filling/sweet_roll
malum:create/washing/zinc_node
malum:create/crushing/ore/crush_natural_quartz_ore
malum:create/crushing/ore/crush_natural_quartz_deepslate_ore
oritech:grinder/compat/mekanism/ore/lead
oritech:grinder/compat/mekanism/ore/tin
oritech:grinder/compat/mekanism/raw/lead
oritech:grinder/compat/mekanism/raw/tin
oritech:grinder/compat/create/ore/zinc
oritech:grinder/compat/create/raw/zinc
oritech:grinder/uraniumore
oritech:grinder/uraniumcrystal
oritech:pulverizer/compat/mekanism/raw/lead
oritech:pulverizer/raw/copper
oritech:centrifuge/compat/mekanism/clump/tin
oritech:grinder/coal
oritech:grinder/quartz
oritech:grinder/dust/copper
oritech:pulverizer/coal
oritech:pulverizer/quartz
oritech:pulverizer/dust/copper
oritech:crushing/compat/create/coal
oritech:crushing/compat/create/copperingot
oritech:crushing/compat/create/quartz
oritech:copper_dust
oritech:pulverizer/compat/create/ore/zinc
oritech:pulverizer/compat/mekanism/ore/lead
oritech:pulverizer/compat/mekanism/ore/tin
oritech:pulverizer/uraniumore
oritech:pulverizer/uraniumcrystal
oritech:deepdrill/uranium
oritech:centrifuge/compat/create/clump/zinc
oritech:centrifuge/fluid/compat/create/clump/zinc
oritech:centrifuge/fluid/compat/create/clumpacid/zinc
oritech:foundry/alloy/steel
oritech:mixing/compat/create/steel
oritech:crafting/alloy/steel
oritech:silicon_blockblockinv
oritech:pulverizer/recycle/copper_dust
oritech:pulverizer/recycle/2_quartz_dust
oritech:pulverizer/recycle/4_quartz_dust
pastel:mod_integration/create/blasting/pure_zinc
pastel:mod_integration/create/cinderhearth/pure_zinc
mynethersdelight:cooking/boiled_eggs
dungeonsdelight:integration/twilightforest/cooking/misc/meef_wellington
stellaris:misc/uranium_from_smelting
stellaris:misc/uranium_from_blasting
""".split())


def recipe_id(path):
    match = re.fullmatch(r"data/([^/]+)/recipe/(.+)\.json", path)
    return f"{match[1]}:{match[2]}" if match else None


def disabled(data):
    return {"type": "neoforge:false"} in data.get("neoforge:conditions", [])


def strings(node):
    if isinstance(node, str):
        yield node
    elif isinstance(node, dict):
        for value in node.values():
            yield from strings(value)
    elif isinstance(node, list):
        for value in node:
            yield from strings(value)


def apply_recipe_policy(write, recipes, merges, removed, items, redirect, off, authored):
    """Restore reviewed processing methods without reviving withdrawn inputs or equipment."""
    count = 0
    restored = set()
    for path, original in sorted(recipes.items()):
        rid = recipe_id(path)
        if not rid:
            continue
        typ = original.get("type", "")
        outputs = set(strings(original.get("result", original.get("results", []))))
        furnace = typ in {"minecraft:smelting", "minecraft:blasting"}
        metal_furnace = furnace and rid.split(":")[0] in {
            "anvilcraft", "create", "mekanism", "malum", "iceandfire", "antarchy"
        } and bool(outputs & merges.keys())
        food = typ in {"minecraft:smelting", "minecraft:smoking", "minecraft:campfire_cooking",
                       "farmersdelight:cooking"} and rid.split(":")[0] in {
                           "mynethersdelight", "minersdelight", "oceansdelight",
                           "trailandtales_delight", "twilightforest", "youkaisfeasts"
                       } and bool(outputs & merges.keys())
        small_byproduct = any(re.fullmatch(r"oritech:small_(?!uranium|plutonium).+_dust", value)
                              for value in outputs) and typ.startswith("oritech:")
        restore = rid in RESTORE or metal_furnace or food or small_byproduct
        if restore:
            fixed, _ = redirect(authored.get(path, original), merges)
            # A recipe consuming a withdrawn intermediate is obsolete even when its result survives.
            obsolete = (set(strings(fixed)) & removed) - {"magitech:fluorite"}
            if not obsolete:
                if rid.startswith("oritech:centrifuge/") and rid.endswith("/zinc"):
                    amount = 12 if "clumpacid" in rid else 8 if "/fluid/" in rid else 4
                    for output in fixed.get("results", []):
                        if output.get("id") == "anvilcraft:zinc_nugget":
                            output["count"] = amount
                write(path, fixed)
                restored.add(path)
                count += 1
        if typ == "anvilcraft:item_crush" and any(part in path for part in [
                "/armor/", "/tools/", "/tool/", "horse_armor"]):
            write(path, off)
        if typ == "anvilcraft:stamping" and any("pressure_plate" in out for out in outputs):
            write(path, off)
        if "oritech:small_" in json.dumps(original):
            ins = set(strings(original.get("ingredient", original.get("ingredients", original.get("key", [])))))
            small_inputs = {value for value in ins
                            if re.fullmatch(r"oritech:small_(?!uranium|plutonium).+_dust", value)}
            nugget_to_small = any("nugget" in value for value in ins) and bool(outputs & merges.keys())
            if nugget_to_small:
                write(path, off)
            elif small_inputs:
                fixed, _ = redirect(original, merges)
                targets = set(strings(fixed.get("result", fixed.get("results", []))))
                inputs = {merges.get(value, value) for value in ins}
                if targets & inputs:
                    write(path, off)
                elif not (set(strings(fixed)) & removed):
                    write(path, fixed)
                    restored.add(path)
        if rid.startswith(("antarchy:titanium/", "antarchy:uranium/")) and (
                "scrap" in rid or "from_scrap" in rid):
            write(path, off)
    # This result belongs to Magitech's ToolMaterial registry, not the item registry.
    path = "data/magitech/recipe/fluorite_material.json"
    if path in recipes:
        write(path, copy.deepcopy(recipes[path]))
    apply_anvil_recipes(write, recipes, off)
    apply_worldgen(write)
    print(f"  retained processing: {count} reviewed routes restored")
    return restored


def apply_anvil_recipes(write, recipes, off):
    optional_knife = "data/farmersknives/recipe/tidesinger_knife.json"
    if optional_knife in recipes:
        data = copy.deepcopy(recipes[optional_knife])
        data.setdefault("neoforge:conditions", []).append({"type": "neoforge:mod_loaded", "modid": "mythicmetals"})
        write(optional_knife, data)
    for metal, ingot, plate in [
            ("brass", "create:brass_ingot", "create:brass_sheet"),
            ("copper", "minecraft:copper_ingot", "create:copper_sheet"),
            ("iron", "minecraft:iron_ingot", "create:iron_sheet"),
            ("gold", "minecraft:gold_ingot", "create:golden_sheet"),
            ("soul_stained_steel", "malum:soul_stained_steel_ingot", "malum:soul_stained_steel_plating"),
            ("malignant_pewter", "malum:malignant_pewter_ingot", "malum:malignant_pewter_plating")]:
        mods = {"anvilcraft", ingot.split(":")[0], plate.split(":")[0]} - {"minecraft"}
        write(f"data/bertieprogression/recipe/anvilcraft/stamping/{metal}_sheet.json", {
            "neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": mod} for mod in sorted(mods)],
            "type": "anvilcraft:stamping", "ingredients": [{"items": ingot}], "results": [{"id": plate}]})
    write("data/anvilcraft/recipe/bulging/fermented_spider_eye.json", off)
    chute = copy.deepcopy(recipes["data/anvilcraft/recipe/chute.json"])
    chute["key"]["A"] = {"item": "create:iron_sheet"}
    chute["result"]["count"] = 1
    write("data/anvilcraft/recipe/chute.json", chute)
    zinc = copy.deepcopy(recipes["data/anvilcraft/recipe/zinc_nugget.json"])
    zinc["result"]["count"] = 4
    write("data/anvilcraft/recipe/zinc_nugget.json", zinc)
    write("data/anvilcraft/recipe/boiling/beef_mushroom_stew.json", off)
    write("data/bertieprogression/recipe/anvilcraft/beef_mushroom_stew.json", {
        "neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": mod}
                                for mod in ("anvilcraft", "farmersdelight")],
        "type": "farmersdelight:cooking", "cookingtime": 200, "experience": 1.0,
        "ingredients": [{"item": item} for item in ("minecraft:beef", "minecraft:brown_mushroom", "minecraft:red_mushroom")],
        "container": {"id": "minecraft:bowl", "count": 1}, "recipe_book_tab": "meals",
        "result": {"id": "anvilcraft:beef_mushroom_stew", "count": 1}})
    for material, chances in {
            "soul_sand": {"minecraft:nether_wart": 0.01},
            "gravel": {"minecraft:iron_nugget": 0.1, "minecraft:flint": 0.2, "create:copper_nugget": 0.02},
            "quartz_sand": {"minecraft:quartz": 0.15},
            "nether_dust": {"anvilcraft:tungsten_nugget": 0.002, "minecraft:netherite_scrap": 0.006},
            "cinerite": {"minecraft:gunpowder": 0.2, "anvilcraft:zinc_nugget": 0.15,
                         "mekanism:nugget_tin": 0.15, "anvilcraft:silver_nugget": 0.05,
                         "anvilcraft:lead_nugget": 0.03, "minecraft:lapis_lazuli": 0.0},
    }.items():
        path = f"data/anvilcraft/recipe/mesh/{material}.json"
        data = copy.deepcopy(recipes[path])
        for result in data["results"]:
            if result["id"] == "anvilcraft:tin_nugget":
                result["id"] = "mekanism:nugget_tin"
        by_item = {result["id"]: result for result in data["results"]}
        for item, chance in chances.items():
            if not chance:
                by_item.pop(item, None)
            else:
                by_item[item] = {"id": item, "count": {"type": "minecraft:binomial", "n": 1.0, "p": chance}}
        data["results"] = sorted(by_item.values(), key=lambda out: -out.get("count", {}).get("p", 1.0)
                                 if isinstance(out.get("count", {}), dict) else -1.0)
        write(path, data)
    fluorite = copy.deepcopy(recipes["data/magitech/recipe/fluorite_block.json"])
    fluorite["key"]["#"] = {"item": "magitech:mana_charged_fluorite"}
    write("data/magitech/recipe/fluorite_block.json", fluorite)
    write("data/magitech/recipe/fluorite_from_fluorite_block.json", {
        "type": "minecraft:crafting_shapeless", "ingredients": [{"item": "magitech:fluorite_block"}],
        "result": {"id": "magitech:mana_charged_fluorite", "count": 9}})
    stratine = copy.deepcopy(recipes["data/pastel/recipe/pedestal/tier3/stratine_gem.json"])
    stratine["key"]["X"] = "hazennstuff:solar_core"
    stratine["colors"]["pastel:yellow"] = 6
    write("data/pastel/recipe/pedestal/tier3/stratine_gem.json", stratine)
    write("data/bertieprogression/forbidden_arcanus/hephaestus_forge/ritual/creative_blaze_cake.json", {
        "forge_tier": 2, "essences": {"aureal": 0, "blood": 15000, "souls": 50, "experience": 1350},
        "main_ingredient": {"item": "create:blaze_cake"},
        "inputs": [{"amount": count, "ingredient": {"item": item}} for item, count in (
            ("avaritia:blaze_cube", 4), ("cataclysm:ignitium_ingot", 2),
            ("elemental_metals:fire_infused_netherite_ingot", 2))],
        "magic_circle": "forbidden_arcanus:create_item",
        "result": {"type": "forbidden_arcanus:create_item", "result_item": {"id": "create:creative_blaze_cake", "count": 1}}})
    apply_anvil_progression(write, recipes, off)


def apply_anvil_progression(write, recipes, off):
    resin = copy.deepcopy(recipes["data/anvilcraft/recipe/cooking/hardend_resin.json"])
    resin["ingredients"][0]["count"] = 4
    write("data/anvilcraft/recipe/cooking/hardend_resin.json", resin)
    paper = copy.deepcopy(recipes["data/anvilcraft/recipe/stamping/paper_from_wood_fiber.json"])
    paper["results"][0]["count"] = 1
    write("data/anvilcraft/recipe/stamping/paper_from_wood_fiber.json", paper)
    core = copy.deepcopy(recipes["data/anvilcraft/recipe/magnetoelectric_core.json"])
    core["key"]["A"] = {"item": "berlordscarving:copper_big_slate"}
    core["key"]["B"] = {"item": "magitech:mana_insulating_glass"}
    write("data/anvilcraft/recipe/magnetoelectric_core.json", core)
    for path, data in recipes.items():
        output_ids = set(strings(data.get("result", data.get("results", []))))
        if "anvilcraft:anvil_hammer" in output_ids or "create:mechanical_drill" in output_ids:
            write(path, off)
    write("data/create/recipe/crafting/kinetics/mechanical_drill.json", {
        "neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": mod}
                                for mod in ("create", "anvilcraft", "malum")],
        "type": "create:mechanical_crafting", "accept_mirrored": False,
        "pattern": ["  T  ", " TPT ", "ATPTA", "AAAAA", "CCCCC"],
        "key": {"T": {"item": "anvilcraft:tungsten_nugget"},
                "P": {"item": "malum:malignant_pewter_nugget"},
                "A": {"item": "create:andesite_alloy"}, "C": {"item": "create:andesite_casing"}},
        "result": {"id": "create:mechanical_drill", "count": 1}})
    write("data/bertieprogression/recipe/anvilcraft/anvil_hammer.json", {
        "neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": mod}
                                for mod in ("pastel", "anvilcraft", "twilightforest")],
        "type": "pastel:pedestal", "tier": "basic", "time": 100,
        "colors": {"pastel:cyan": 6, "pastel:magenta": 6, "pastel:yellow": 6},
        "pattern": ["A", "B", "C"], "key": {"A": "minecraft:anvil", "B": "minecraft:lightning_rod", "C": "twilightforest:ironwood_ingot"},
        "result": {"id": "anvilcraft:anvil_hammer", "count": 1}, "disable_yield_upgrades": True})
    write("data/bertieprogression/tags/block/lightning_magnet_inputs.json", {
        "replace": True, "values": [{"id": "oritech:nickel_block", "required": False}]})


def apply_worldgen(write):
    write("data/hazennstuff/neoforge/biome_modifier/add_solar_core_ore.json", {
        "type": "neoforge:add_features", "biomes": "minecraft:basalt_deltas",
        "features": "hazennstuff:solar_core_ore_placed", "step": "underground_ores"})
    write("data/hazennstuff/worldgen/configured_feature/solar_core_ore.json", {
        "type": "bertieprogression:solar_pool_ore", "config": {}})
    write("data/hazennstuff/worldgen/placed_feature/solar_core_ore_placed.json", {
        "feature": "hazennstuff:solar_core_ore", "placement": [
            {"type": "minecraft:count", "count": 16}, {"type": "minecraft:in_square"}]})
    write("data/bertieprogression/neoforge/biome_modifier/tungsten_ore.json", {
        "neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": "anvilcraft"}],
        "type": "neoforge:add_features", "biomes": "#minecraft:is_overworld",
        "features": "bertieprogression:tungsten_ore", "step": "underground_ores"})
    write("data/bertieprogression/worldgen/configured_feature/tungsten_ore.json", {
        "type": "bertieprogression:rising_tungsten_ore", "config": {}})
    write("data/bertieprogression/worldgen/placed_feature/tungsten_ore.json", {
        "feature": "bertieprogression:tungsten_ore", "placement": [
            {"type": "minecraft:count", "count": 4}, {"type": "minecraft:in_square"},
            {"type": "minecraft:biome"}]})


def repair_ingredients(resources, root, write, written, recipes, tag_sources, block_sources,
                       merges, removed, items, present, off, restored):
    """Repair empty item ingredients while keeping deliberately separated material tags distinct."""
    resources, root = Path(resources), Path(root)
    manifest = root / "texture-work/.ingredient_repairs.json"
    previous = set(json.loads(manifest.read_text(encoding="utf-8"))) if manifest.exists() else set()
    authored = set(written)
    generated = set()
    item_tags, block_tags = {}, {}

    def union(sources, target, kind):
        for path, contributions in sources.items():
            match = re.fullmatch(rf"data/([^/]+)/tags/{kind}/(.+)\.json", path)
            if not match:
                continue
            values = []
            for contribution in contributions:
                if contribution.get("replace"):
                    values = []
                values += contribution.get("values", [])
            target[f"{match[1]}:{match[2]}"] = values

    union(tag_sources, item_tags, "items?")
    union(block_sources, block_tags, "block")
    original_tags = copy.deepcopy(item_tags)
    for path in (resources / "data").glob("*/tags/item/**/*.json"):
        relative = path.relative_to(resources).as_posix()
        if relative in previous and relative not in authored:
            continue
        parts = relative.split("/")
        key = parts[1] + ":" + "/".join(parts[4:])[:-5]
        data = json.loads(path.read_text(encoding="utf-8"))
        item_tags[key] = ([] if data.get("replace") else item_tags.get(key, [])) + data.get("values", [])

    def expand(key, tags, seen=None, redirect_aliases=False):
        seen = set() if seen is None else seen
        if key in seen:
            return set()
        found = set()
        for entry in tags.get(key, []):
            value = entry.get("id", "") if isinstance(entry, dict) else entry
            if not isinstance(value, str):
                continue
            if value.startswith("#"):
                found |= expand(value[1:], tags, seen | {key}, redirect_aliases)
            else:
                value = merges.get(value, value) if redirect_aliases else value
                if value in items and value not in removed:
                    found.add(value)
        return found

    def emit(path, data):
        write(path, data)
        generated.add(path)

    # A pressure plate is a redstone device, not a sheet. Keep genuine plate-only tags unchanged.
    for key in sorted(item_tags):
        if key == "c:plates" or key.startswith("c:plates/"):
            members = expand(key, item_tags)
            plates = {item for item in members if not item.endswith("pressure_plate")}
            if members != plates:
                path = "data/c/tags/item/" + key.split(":", 1)[1] + ".json"
                data = {"replace": True, "values": sorted(plates)}
                emit(path, data)
                item_tags[key] = data["values"]

    repaired_tags = {}
    issues = []

    def replacement(tag, rid):
        tag = tag if ":" in tag else "minecraft:" + tag
        if expand(tag, item_tags):
            return tag
        aliases = {
            "refinedstorage:controller": "refinedstorage:controllers",
            "c:flowers/tall": "minecraft:tall_flowers",
            "c:coal": "minecraft:coals",
            "forge:circuits/basic": "c:circuits/basic",
            "forge:ingots/osmium": "c:ingots/osmium",
            "c:steel_ingots": "c:ingots/steel",
            "c:rubies": "c:gems/ruby",
            "c:foods/raw_meats": "c:foods/raw_meat",
            "c:foods/rices": "c:crops/rice",
            "c:foods/rice": "c:crops/rice",
        }
        alternate = aliases.get(tag)
        if tag.startswith(("hazennstuff:armors/school/", "hazennstuff:armors/pure/")):
            alternate = tag.replace("hazennstuff:", "hazentouvelib:", 1)
        if alternate and expand(alternate, item_tags):
            return alternate
        if tag == "c:ingots/rose_gold":
            winner = "hazennstuff:rose_gold_ingot" if rid.startswith("hazennstuff:") else "slag:rose_gold_ingot"
            members = {winner} if winner in items and winner not in removed else set()
        else:
            members = expand(tag, original_tags, redirect_aliases=True)
            if not members:
                members = expand(tag, block_tags, redirect_aliases=True)
        if tag == "c:plates" or tag.startswith("c:plates/"):
            members = {item for item in members if not item.endswith("pressure_plate")}
        if not members:
            issues.append((rid, tag))
            return tag
        # Namespacing these aliases avoids turning a dust into an ingredient of every gem recipe.
        key = (tag, tuple(sorted(members)))
        if key not in repaired_tags:
            namespace, name = tag.split(":", 1)
            suffix = "_" + rid.split(":", 1)[0] if tag == "c:ingots/rose_gold" else ""
            alias = f"bertieprogression:retained/{namespace}/{name}{suffix}"
            emit("data/bertieprogression/tags/item/" + alias.split(":", 1)[1] + ".json",
                 {"replace": True, "values": sorted(members)})
            item_tags[alias] = sorted(members)
            repaired_tags[key] = alias
        return repaired_tags[key]

    def fix(node, rid, context="", item_context=False):
        if isinstance(node, list):
            return [fix(value, rid, context, item_context) for value in node]
        if not isinstance(node, dict):
            if item_context and isinstance(node, str) and node.startswith("#"):
                return "#" + replacement(node[1:], rid)
            return node
        if node.get("tag") == "wover:water_bottles" and item_context:
            return {"type": "neoforge:components", "items": "minecraft:potion",
                    "components": {"minecraft:potion_contents": {"potion": "minecraft:water"}}, "strict": False}
        out = {}
        for key, value in node.items():
            fluid_context = any(part in (context + "/" + key).lower()
                                for part in ("fluid", "chemical", "biome", "enchantment", "predicate"))
            fluid_context = fluid_context or ("amount" in node and "tag" in node and "count" not in node)
            ingredient = item_context or key in {"ingredient", "ingredients", "key", "input", "inputs",
                                               "base", "addition", "template", "main_ingredient"}
            if node.get("type") == "neoforge:tag_empty" and key == "tag":
                out[key] = replacement(value, rid)
            elif node.get("type") == "pastel:tags_populated" and key == "values" and isinstance(value, list):
                out[key] = [replacement(tag, rid) for tag in value]
            elif key == "tag" and isinstance(value, str) and ingredient and not fluid_context:
                out[key] = replacement(value, rid)
            elif key == "items" and isinstance(value, str) and value.startswith("#") and not fluid_context:
                out[key] = "#" + replacement(value[1:], rid)
            else:
                out[key] = fix(value, rid, context + "/" + key, ingredient and not fluid_context)
        return out

    def condition(value):
        typ = value.get("type", "")
        if typ.endswith(":mod_loaded"):
            return value.get("modid", value.get("mod_id")) in present
        if typ == "neoforge:not":
            result = condition_state(value.get("value", {}))
            return None if result is None else not result
        if typ == "neoforge:and":
            return all(condition(c) is not False for c in value.get("values", []))
        if typ == "neoforge:or":
            return any(condition(c) is not False for c in value.get("values", []))
        return None

    def condition_state(value):
        if value.get("type") == "neoforge:tag_empty":
            tag = value.get("tag", "")
            # A missing optional ingredient deliberately suppresses the recipe. A merged one is repaired.
            return not (expand(tag, item_tags) or expand(tag, original_tags, redirect_aliases=True))
        return condition(value)

    current = dict(recipes)
    for path in (resources / "data").glob("*/recipe/**/*.json"):
        relative = path.relative_to(resources).as_posix()
        if relative in previous and relative not in authored:
            continue
        current[relative] = json.loads(path.read_text(encoding="utf-8"))
    changed = 0
    for path, data in sorted(current.items()):
        if not isinstance(data, dict) or disabled(data):
            continue
        if any(condition(c) is False for c in data.get("neoforge:conditions", [])):
            continue
        rid = recipe_id(path)
        if not rid:
            continue
        before = len(issues)
        fixed = fix(data, rid)
        missing = {tag for _, tag in issues[before:]}
        optional = {"mythicmetals:tidesinger_coral", "hazennstuff:items/hydro_runes",
                    "quark:revertable_chests", "quark:revertable_trapped_chests",
                    "c:nuggets/diamond", "c:nuggets/emerald", "c:nuggets/lapis", "c:nuggets/netherite"}
        if missing and all(tag in optional or tag.startswith("c:plates/") for tag in missing):
            fixed.setdefault("neoforge:conditions", []).extend(
                {"type": "neoforge:not", "value": {"type": "neoforge:tag_empty", "tag": tag}}
                for tag in sorted(missing))
            del issues[before:]
        if fixed != data:
            emit(path, fixed)
            changed += 1
        current[path] = fixed

    def signature(node):
        if isinstance(node, list):
            return [signature(value) for value in node]
        if not isinstance(node, dict):
            return node
        out = {key: signature(value) for key, value in node.items()
               if key not in {"neoforge:conditions", "group", "category"}}
        if set(out) == {"tag"}:
            members = expand(out["tag"], item_tags)
            if len(members) == 1:
                return {"item": next(iter(members))}
        if isinstance(out.get("ingredients"), list):
            out["ingredients"] = sorted(out["ingredients"], key=lambda value: json.dumps(value, sort_keys=True))
        return out

    seen = {}
    duplicates = 0
    for path, data in sorted(current.items(), key=lambda row: (row[0] in restored, row[0])):
        if disabled(data) or any(condition(c) is False for c in data.get("neoforge:conditions", [])):
            continue
        key = json.dumps(signature(data), sort_keys=True)
        if key in seen and path in restored:
            emit(path, off)
            duplicates += 1
        else:
            seen.setdefault(key, path)
    for stale in sorted(previous - generated - authored):
        path = resources / stale
        if path.is_file():
            path.unlink()
    manifest.write_text(json.dumps(sorted(generated), indent=2) + "\n", encoding="utf-8", newline="\n")
    report = root / "texture-work/ingredient_issues.json"
    report.write_text(json.dumps(sorted(set(issues)), indent=2) + "\n", encoding="utf-8", newline="\n")
    print(f"  item ingredients: {changed} recipes repaired, {len(repaired_tags)} retained tags, "
          f"{len(set(issues))} unresolved tag references, {duplicates} duplicate routes suppressed")
    if issues:
        raise SystemExit("Unresolved active item ingredients: " + repr(sorted(set(issues))))
