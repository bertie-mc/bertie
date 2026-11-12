"""Canonical technology, solar and teleportation recipe updates."""

import copy
import json
from pathlib import Path
from zipfile import ZipFile

from recipe_outputs import item_outputs

DISABLED = {"neoforge:conditions": [{"type": "neoforge:false"}],
            "type": "minecraft:crafting_shapeless", "ingredients": [{"item": "minecraft:stone"}],
            "result": {"id": "minecraft:stone"}}
PLASTIC = {"oritech:plastic_sheet": "mekanism:hdpe_sheet", "oritech:plastic_block": "mekanism:hdpe_sheet"}
GLASS = {"pastel:" + name + "semi_permeable_glass": replacement for name, replacement in [
    ("", "minecraft:glass"), ("tinted_", "minecraft:tinted_glass"),
    *[(name + "_", "pastel:" + name + "_glass")
      for name in ["amethyst", "citrine", "moonstone", "onyx", "radiant", "topaz"]]]}


def replace(value, replacements):
    if isinstance(value, str):
        return replacements.get(value, value)
    if isinstance(value, list):
        return [replace(child, replacements) for child in value]
    if isinstance(value, dict):
        return {key: replace(child, replacements) for key, child in value.items()}
    return value


def apply(module, native, mods=None):
    module = Path(module)
    resources = module / "src/main/resources"
    native = {(key if not key.startswith("data/") else
               key.split("/")[1] + ":" + "/".join(key.split("/")[3:])[:-5]): value
              for key, value in native.items() if not key.startswith("data/") or "/recipe/" in key}
    changed = []

    def write(path, value):
        target = resources / path
        text = json.dumps(value, indent=2, ensure_ascii=False) + "\n"
        if not target.exists() or target.read_text(encoding="utf-8") != text:
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(text, encoding="utf-8", newline="\n")
            changed.append(path)

    def path(identifier):
        namespace, name = identifier.split(":", 1)
        return f"data/{namespace}/recipe/{name}.json"

    def effective(identifier):
        local = resources / path(identifier)
        return json.loads(local.read_text(encoding="utf-8")) if local.exists() else copy.deepcopy(native[identifier])

    def save(identifier, recipe):
        write(path(identifier), recipe)

    def shaped(identifier, output, pattern, key, count=1):
        save(identifier, {"type": "avaritia:shaped_table", "tier": 2, "pattern": pattern,
                          "key": {symbol: {"item": item} for symbol, item in key.items()},
                          "result": {"id": output, "count": count}})

    def machine(identifier, kind, ingredients, output, count=1, time=80, **extra):
        save(identifier, {"type": "oritech:" + kind,
                          "ingredients": [{"item": item} for item in ingredients],
                          "results": [{"id": output, "count": count}], "time": time, **extra})

    # Remove the obsolete native production routes before emitting the selected replacements.
    for identifier, original in native.items():
        recipe = effective(identifier)
        outputs = item_outputs(recipe, set())
        if (("oritech:basic_battery" in outputs and recipe.get("type") in
             {"oritech:assembler", "oritech:centrifuge_fluid", "minecraft:crafting_shaped", "minecraft:crafting_shapeless"})
                or identifier == "mekanism:enriching/hdpe_sheet"
                or ("oritech:overcharged_crystal" in outputs and recipe.get("type", "").startswith("minecraft:crafting"))
                or identifier == "oritech:assembler/superconductor"):
            save(identifier, DISABLED)

    machine("bertieprogression:centrifuge/empty_capacitor", "centrifuge_fluid",
            ["mekanism:basic_chemical_tank"], "anvilcraft:capacitor_empty", time=100,
            fluidInput={"fluid": "oritech:still_sulfuric_acid", "amount": 1000})
    machine("bertieprogression:assembler/battery", "assembler",
            ["anvilcraft:capacitor_empty", "anvilcraft:capacitor_empty", "oritech:energite_ingot", "mekanism:hdpe_sheet"],
            "oritech:basic_battery", time=48)
    capacitor = effective("bertieprogression:reaction/empty_capacitor")
    capacitor["item_input"] = {"count": 1, "item": "mekanism:advanced_chemical_tank"}
    save("bertieprogression:reaction/empty_capacitor", capacitor)
    tablet = effective("mekanism:energy_tablet")
    save("mekanism:energy_tablet", replace(tablet, {"anvilcraft:capacitor_empty": "oritech:basic_battery"}))
    pole = effective("oritech:crafting/_pole")
    save("oritech:crafting/_pole", replace(pole, {"mekanism:basic_energy_cube": "mekanism:energy_tablet"}))
    for identifier in native:
        if "energite" in identifier:
            recipe = effective(identifier)
            updated = replace(recipe, {"c:ingots/nickel": "c:ingots/electrum", "oritech:nickel_ingot": "oritech:electrum_ingot",
                                       "productivemetalworks:molten_nickel": "productivemetalworks:molten_electrum"})
            if updated != recipe:
                save(identifier, updated)

    flux = effective("bertieprogression:malum/warp_flux")
    extras = [ingredient for ingredient in flux["extraInputs"]
              if ingredient.get("item") not in {"minecraft:ender_pearl", "forbidden_arcanus:ender_pearl_fragment"}]
    at = next(i for i, ingredient in enumerate(extras) if ingredient.get("item") == "malum:null_slate") + 1
    extras.insert(at, {"item": "forbidden_arcanus:ender_pearl_fragment", "count": 6})
    flux["extraInputs"] = extras
    save("bertieprogression:malum/warp_flux", flux)

    case = "mekanism:steel_casing"
    coil = "bertieprogression:electric_coil"
    resonant = "bertieprogression:platinum_coil"
    shaped("oritech:assembler/superconductor", "oritech:superconductor", ["UURUU", "FIIIF", "UURUU"],
           {"U": "mekanism:ultimate_universal_cable", "R": resonant, "F": "oritech:flux_gate", "I": "oritech:unholy_intelligence"}, 4)
    shaped("oritech:motor/dubios", "oritech:dubios_container", [" SS  ", "SEVS ", "SVCVS", " SVES", "  SS "],
           {"S": "mekanism:hdpe_sheet", "E": "oritech:enderic_compound", "V": "anvilcraft:void_matter", "C": "cognition:cognitive_crystal"})
    shaped("oritech:crafting/deepdrill", "oritech:deep_drill_block", ["RMRMR", "MEOEM", "RACAR", "HAXAH", "RHDHR"],
           {"R": "mekanism:alloy_reinforced", "M": "oritech:magnetic_coil", "E": coil, "O": "oritech:overcharged_crystal",
            "A": "mekanism_extras:alloy_radiance", "C": case, "H": "anvilcraft:earth_core_shard", "X": "create:mechanical_drill", "D": "oritech:duratium_ingot"})
    shaped("mekanism:teleportation_core", "mekanism:teleportation_core", ["FAAAF", "APCPA", "ACVDA", "APDPA", "FAAAF"],
           {"F": "oritech:fluxite", "A": "mekanism:alloy_atomic", "P": "minecraft:ender_pearl", "C": "refinedstorage:construction_core",
            "D": "refinedstorage:destruction_core", "V": "anvilcraft:void_matter_block"})
    shaped("mekanism:teleporter_frame", "mekanism:teleporter_frame", ["GGGGG", "GVVVG", "GVUVG", "GVVVG", "GGGGG"],
           {"G": "mekanism:ingot_refined_glowstone", "V": "anvilcraft:void_matter", "U": "mekanism:ultimate_control_circuit"}, 9)
    shaped("mekanism:teleporter", "mekanism:teleporter", ["GASAG", "ACFDA", "GCKDG", "ACTDA", "GASAG"],
           {"G": "anvilcraft:cursed_gold_ingot", "A": "mekanism:alloy_atomic", "S": "l2complements:sculkium_ingot", "C": "refinedstorage:construction_core",
            "D": "refinedstorage:destruction_core", "F": "oritech:flux_gate", "K": case, "T": "mekanism:teleportation_core"})
    shaped("mekanismgenerators:solar_panel", "mekanismgenerators:solar_panel", ["GGGGG", "OFFFO", "OARAO", "OOOOO"],
           {"G": "pastel:moonstone_glass", "O": "bertieprogression:osmium_sheet", "F": "oritech:fluxite", "A": "mekanism:alloy_infused", "R": "berlordscarving:redstone_big_slate"})
    shaped("mekanismgenerators:generator/solar", "mekanismgenerators:solar_generator", ["SSSSS", "OFTFO", "OCKCO", "OEEEO", "OOOOO"],
           {"S": "mekanismgenerators:solar_panel", "O": "bertieprogression:osmium_sheet", "F": "oritech:flux_gate", "T": "mekanism:energy_tablet",
            "C": coil, "K": case, "E": "oritech:energite_ingot"})
    shaped("oritech:crafting/solar", "oritech:big_solar_panel_block", ["SSSSS", "SFGFS", "RCKCR", "RCACR", "REEER"],
           {"S": "mekanismgenerators:solar_panel", "F": "oritech:flux_gate", "G": "mekanismgenerators:solar_generator", "R": "bertieprogression:royal_steel_sheet",
            "C": resonant, "K": case, "A": "mekanism_extras:absolute_control_circuit", "E": "anvilcraft:ember_metal_ingot"})
    for index in range(3):
        identifier = f"anvilcraft:time_warp/ember_metal_ingot_{index}"
        recipe = effective(identifier)
        recipe["ingredients"][0] = {"items": "anvilcraft:heated_tungsten_block"}
        save(identifier, recipe)

    shrine_path = "data/pastel/recipe/fusion_shrine/netherite_ingot.json"
    shrine = json.loads((resources / shrine_path).read_text(encoding="utf-8"))
    write(shrine_path, replace(shrine, {"minecraft:netherite_ingot": "mekanism:dust_netherite"}))
    machine("bertieprogression:atomic_forge/netherite", "atomic_forge",
            ["mekanism:dust_netherite", "anvilcraft:earth_core_shard", "anvilcraft:earth_core_shard"], "minecraft:netherite_ingot", 2, 200)
    machine("bertieprogression:foundry/netherite", "foundry", ["mekanism:dust_netherite", "anvilcraft:earth_core_shard_block"], "minecraft:netherite_ingot", 2, 80)

    # Native packing/unpacking is withdrawn rather than redirected into sheet multiplication.
    for identifier, original in native.items():
        recipe = effective(identifier)
        if item_outputs(recipe, set()) & PLASTIC.keys():
            save(identifier, DISABLED)
        else:
            updated = replace(recipe, PLASTIC | GLASS)
            if updated != recipe:
                save(identifier, updated)
    for recipe_path in list((resources / "data").glob("*/recipe/**/*.json")):
        value = json.loads(recipe_path.read_text(encoding="utf-8"))
        updated = replace(value, PLASTIC | GLASS)
        if updated != value:
            write(recipe_path.relative_to(resources).as_posix(), updated)
    merged = json.loads((resources / "merged_items.json").read_text())
    write("merged_items.json", merged | PLASTIC)
    removed = set(json.loads((resources / "removed_items.json").read_text())) | PLASTIC.keys()
    write("removed_items.json", sorted(removed))
    # Shared tag ingredients must also resolve to the surviving sheet and glass forms.
    if mods:
        for jar in Path(mods).glob("*.jar"):
            if jar.name.startswith("bertieprogression"):
                continue
            with ZipFile(jar) as archive:
                for name in archive.namelist():
                    if "/tags/" not in name or not name.endswith(".json"):
                        continue
                    raw = archive.read(name)
                    if not any(item.encode() in raw for item in PLASTIC | GLASS):
                        continue
                    target = resources / name
                    value = json.loads(target.read_text(encoding="utf-8")) if target.exists() else json.loads(raw)
                    if "/tags/block/" in name:
                        value["values"] = [v for v in value.get("values", [])
                                           if (v if isinstance(v, str) else v.get("id")) not in {*PLASTIC, "mekanism:hdpe_sheet"}]
                    else:
                        value = replace(value, PLASTIC | GLASS)
                    value["replace"] = True
                    write(name, value)
    # The pack already renames Magitech's fluorite block and charges its packing ingredients.
    block = "magitech:fluorite_block"
    obsolete = "bertieprogression:mana_charged_fluorite_block"
    for old in ["assets/bertieprogression/blockstates/mana_charged_fluorite_block.json",
                "assets/bertieprogression/models/block/mana_charged_fluorite_block.json",
                "assets/bertieprogression/models/item/mana_charged_fluorite_block.json",
                "data/bertieprogression/loot_table/blocks/mana_charged_fluorite_block.json",
                "data/bertieprogression/recipe/crafting/mana_charged_fluorite_block.json",
                "data/bertieprogression/recipe/crafting/mana_charged_fluorite_from_block.json"]:
        (resources / old).unlink(missing_ok=True)
    tag_path = "data/minecraft/tags/block/mineable/pickaxe.json"
    tag = json.loads((resources / tag_path).read_text()) if (resources / tag_path).exists() else {"values": []}
    tag["values"] = [value for value in tag["values"] if value != obsolete]
    write(tag_path, tag)
    lang_path = "assets/bertieprogression/lang/en_us.json"
    lang = json.loads((resources / lang_path).read_text(encoding="utf-8"))
    lang.pop("block.bertieprogression.mana_charged_fluorite_block", None)
    write(lang_path, lang)
    save("bertieprogression:multiblock/overcharged_crystal", {"type": "anvilcraft:multiblock", "pattern": {
        "layers": [["MSM", "SSS", "MSM"], ["SSS", "SCS", "SSS"], ["MSM", "SSS", "MSM"]],
        "symbols": {"M": {"block": block}, "S": {"block": "oritech:superconductor"}, "C": {"block": "cognition:cognitive_crystal_block"}}},
        "result": {"id": "oritech:overcharged_crystal", "count": 1}})
    for suffix in ["chests/woodland_mansion", "entities/evoker"]:
        write(f"data/endrem/loot_table/minecraft/{suffix}.json", {"pools": [
            {"name": "eye_pool", "rolls": 0, "entries": [{"type": "minecraft:empty"}]}]})
    eye_lang_path = "assets/endrem/lang/en_us.json"
    eye_lang = json.loads((resources / eye_lang_path).read_text(encoding="utf-8"))
    eye_lang["item.endrem.magical_eye.description"] = "§7§oIt was rumored that only most efficient mages could obtain this artifact"
    eye_lang.pop("advancements.main.magical_eye.description", None)
    write(eye_lang_path, eye_lang)
    from warden_wind_recipes import apply as apply_warden_wind
    changed.extend(apply_warden_wind(module, native))
    return changed


def retire_ostrum(module, mods):
    resources = Path(module) / "src/main/resources"

    def save(path, value):
        target = resources / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8", newline="\n")

    metadata = json.loads((resources / "ore_materials.json").read_text())
    metadata["materials"] = [m for m in metadata["materials"] if m["name"] != "ostrum"]
    for material in metadata["materials"]:
        if material.get("byproduct") == "ostrum":
            material["byproduct"] = "tharsite"
    for key in ["new_items", "recipes", "provisional_extras"]:
        metadata[key] = [v for v in metadata[key] if "ostrum" not in v]
    metadata["copied"] = [v for v in metadata["copied"] if v.get("material") != "ostrum"]
    metadata["new_slurries"].pop("ostrum", None)
    if "centrifuge" in metadata:
        metadata["centrifuge"].get("recipes", {}).pop("ostrum", None)
    save("ore_materials.json", metadata)
    for target in list((resources / "data").glob("*/recipe/**/*.json")):
        text = target.read_text(encoding="utf-8")
        if "ostrum" in target.as_posix():
            target.unlink()
        elif "ostrum" in text:
            value = replace(json.loads(text), {"bertieprogression:ostrum_spec": "bertieprogression:tharsite_spec"})
            if "ostrum" in json.dumps(value):
                raise ValueError("Unresolved Ostrum consumer: " + str(target))
            save(target.relative_to(resources).as_posix(), value)
    for target in list((resources / "assets").rglob("*ostrum*")):
        if target.is_file():
            target.unlink()
    for target in (resources / "data").glob("*/tags/**/*.json"):
        value = json.loads(target.read_text(encoding="utf-8"))
        if "ostrum" in target.name:
            value = {"replace": True, "values": []}
        elif "ostrum" in json.dumps(value):
            value["values"] = [v for v in value.get("values", []) if "ostrum" not in str(v)]
        else:
            continue
        save(target.relative_to(resources).as_posix(), value)
    lang_path = "assets/bertieprogression/lang/en_us.json"
    language = json.loads((resources / lang_path).read_text(encoding="utf-8"))
    save(lang_path, {k: v for k, v in language.items() if "ostrum" not in k})
    atlas_path = "assets/minecraft/atlases/blocks.json"
    atlas = json.loads((resources / atlas_path).read_text())
    atlas["sources"] = [v for v in atlas["sources"] if "ostrum" not in json.dumps(v)]
    save(atlas_path, atlas)
    removed = set(json.loads((resources / "removed_items.json").read_text())) | {"stellaris:mars_ostrum_ore"}
    save("removed_items.json", sorted(removed))
    hidden_path = "data/c/tags/item/hidden_from_recipe_viewers.json"
    hidden = json.loads((resources / hidden_path).read_text())
    present = {v if isinstance(v, str) else v.get("id") for v in hidden["values"]}
    for identifier in [*PLASTIC, "stellaris:mars_ostrum_ore"]:
        if identifier not in present:
            hidden["values"].append({"id": identifier, "required": False})
    save(hidden_path, hidden)
    java = Path(module) / "src/main/java/io/github/bertie_mc/bertieprogression/OreMaterials.java"
    items = ",\n".join('            "' + name + '"' for name in metadata["new_items"])
    slurries = ",\n".join(f'            Map.entry("{name}", 0x{color:06X})' for name, color in metadata["new_slurries"].items())
    java.write_text('package io.github.bertie_mc.bertieprogression;\n\nimport java.util.List;\nimport java.util.Map;\n\n'
                    'public final class OreMaterials {\n    public static final List<String> ITEMS = List.of(\n' + items + ');\n\n'
                    '    public static final Map<String, Integer> SLURRIES = Map.ofEntries(\n' + slurries + ');\n\n'
                    '    private OreMaterials() {}\n}\n', encoding="utf-8", newline="\n")
    save("data/stellaris/worldgen/configured_feature/mars_ostrum_ore_key.json", {"type": "minecraft:no_op", "config": {}})
    save("data/stellaris/loot_table/blocks/mars_ostrum_ore.json", {"type": "minecraft:block", "pools": []})
    for jar in Path(mods).glob("stellaris-*.jar"):
        with ZipFile(jar) as archive:
            for name in archive.namelist():
                if "/tags/" not in name or not name.endswith(".json") or b"ostrum" not in archive.read(name):
                    continue
                target = resources / name
                value = json.loads(target.read_text()) if target.exists() else json.loads(archive.read(name))
                value["replace"] = True
                value["values"] = [v for v in value.get("values", []) if "ostrum" not in str(v)]
                save(name, value)
