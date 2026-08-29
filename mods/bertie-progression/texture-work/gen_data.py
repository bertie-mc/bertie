#!/usr/bin/env python3
"""
bertieprogression data/asset generator for the progression recipe ledger.

Writes into src/main/resources/:
  data/bertieprogression/recipe/**        authored recipes
  data/<other>/recipe/**          stock overrides (false-condition disables / replacements)
  data/forbidden_arcanus/forbidden_arcanus/hephaestus_forge/ritual/  augmented tier upgrades
  data/bertieprogression/forbidden_arcanus/hephaestus_forge/ritual/          authored rituals
  data/bertieprogression/tags/**          stripped_logs item+block tags
  assets/bertieprogression/**             lang, item models, blockstates, block models

Run:  python texture-work/gen_data.py
"""
import io
import json
import os
import re
import shutil

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
RES = os.path.join(ROOT, "src", "main", "resources")


def _removed_docs_dir():
    """Resolve the private workspace's canonical removed-item tables before writing output."""
    workspace_root = os.environ.get("BERTIE_WORKSPACE")
    if not workspace_root:
        raise SystemExit(
            "BERTIE_WORKSPACE is required; set it to the private bertie-workspace checkout"
        )

    docs = os.path.join(
        os.path.abspath(os.path.expanduser(workspace_root)), "docs", "removed"
    )
    if not os.path.isdir(docs) or not os.path.isfile(os.path.join(docs, "README.md")):
        raise SystemExit(
            f"removed-item docs not found under BERTIE_WORKSPACE: {docs}"
        )
    return docs


REMOVED_DOCS = _removed_docs_dir()
MODID = "bertieprogression"

# ---------------------------------------------------------------- helpers

written = []

def write(relpath, obj):
    path = os.path.join(RES, relpath.replace("/", os.sep))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")
    written.append(relpath)

def conds(*mods):
    """neoforge:conditions list — AND of mod_loaded for each external mod."""
    return [{"type": "neoforge:mod_loaded", "modid": m} for m in sorted(set(mods))]

DISABLED = {
    "neoforge:conditions": [{"type": "neoforge:false"}],
    "type": "minecraft:crafting_shapeless",
    "ingredients": [{"item": "minecraft:stone"}],
    "result": {"id": "minecraft:stone", "count": 1},
}

def external_mods(*ids):
    """Extract non-minecraft/bertieprogression namespaces from item/tag id strings."""
    out = set()
    for i in ids:
        if i is None:
            continue
        i = i.lstrip("#")  # tags carry the same namespace semantics
        ns = i.split(":")[0] if ":" in i else "minecraft"
        if ns not in ("minecraft", MODID, "c"):
            out.add(ns)
    return out

def shaped(pattern, key, result_id, count=1, category="misc", extra_conds=None):
    ids = list(key.values()) + [result_id]
    mods = external_mods(*[v if isinstance(v, str) else None for v in ids])
    obj = {}
    if mods or extra_conds:
        obj["neoforge:conditions"] = conds(*mods) + (extra_conds or [])
    obj.update({
        "type": "minecraft:crafting_shaped",
        "category": category,
        "key": {k: ({"tag": v[1:]} if v.startswith("#") else {"item": v}) for k, v in key.items()},
        "pattern": pattern,
        "result": {"id": result_id, "count": count},
    })
    return obj

def shapeless(ingredients, result_id, count=1):
    mods = external_mods(*[i for i in ingredients], result_id)
    obj = {}
    if mods:
        obj["neoforge:conditions"] = conds(*mods)
    obj.update({
        "type": "minecraft:crafting_shapeless",
        "category": "misc",
        "ingredients": [({"tag": i[1:]} if i.startswith("#") else {"item": i}) for i in ingredients],
        "result": {"id": result_id, "count": count},
    })
    return obj

def mech(pattern, key, result_id, count=1):
    """create:mechanical_crafting"""
    mods = external_mods(*key.values(), result_id) | {"create"}
    return {
        "neoforge:conditions": conds(*mods),
        "type": "create:mechanical_crafting",
        "accept_mirrored": False,
        "category": "misc",
        "key": {k: ({"tag": v[1:]} if v.startswith("#") else {"item": v}) for k, v in key.items()},
        "pattern": pattern,
        "result": {"id": result_id, "count": count},
        "show_notification": True,
    }

def infusion(input_item, input_count, extra, spirits, result_id, count=1):
    """malum:spirit_infusion. extra = [(id,count)...], spirits = [(type,count)...]

    A leading '#' on the input or any extra makes it a TAG ingredient (Malum's own
    recipes use {"tag": ...}, e.g. #minecraft:stone_tool_materials for the rocks).
    """
    def _ing(i, c):
        return ({"tag": i[1:], "count": c} if i.startswith("#") else {"item": i, "count": c})
    mods = external_mods(input_item, result_id, *[e[0] for e in extra]) | {"malum"}
    return {
        "neoforge:conditions": conds(*mods),
        "type": "malum:spirit_infusion",
        "input": _ing(input_item, input_count),
        "extraInputs": [_ing(i, c) for i, c in extra],
        "spirits": [{"type": t, "count": c} for t, c in spirits],
        "result": {"id": result_id, "count": count},
    }

def ritual(main, inputs, result_id, count=1, tier=None, essences=None, xp=0):
    """FA hephaestus ritual. inputs = [(id,count)...]

    NO neoforge:conditions here: rituals are a DATAPACK REGISTRY that only exists
    when forbidden_arcanus is present (absent-FA environments ignore the folder),
    and every referenced mod is a hard member of the current pack. Unconditional files
    make a parse problem a loud server-boot error instead of a silent skip.

    HARD CONSTRAINT (verified in-game): the Forge has 8 pedestals and each input
    item occupies one pedestal, so sum(amounts) must be <= 8 — every stock ritual
    obeys this and exceeding it makes the ritual uncraftable (and crashes the
    JEI/TMRV ritual display with AIOOBE index 8).
    """
    total = sum(c for _, c in inputs)
    assert total <= 8, f"ritual inputs exceed 8 pedestals ({total}): {result_id}"
    # FA stores XP cost as essences.experience (4th key; verified against stock rituals).
    ess = dict(essences) if essences else {"aureal": 0, "blood": 0, "souls": 0}
    if xp:
        ess["experience"] = xp
    obj = {
        "essences": ess,
        "inputs": [{"amount": c, "ingredient": ({"tag": i[1:]} if i.startswith("#") else {"item": i})}
                   for i, c in inputs],
        "magic_circle": "forbidden_arcanus:create_item",
        "main_ingredient": {"item": main},
        "result": {"type": "forbidden_arcanus:create_item",
                   "result_item": {"id": result_id, "count": count}},
    }
    if tier:
        obj["forge_tier"] = tier
    return obj

def ritual_component_input(obj, index, item_id, components, amount):
    """Swap input #index for a neoforge component-aware ingredient (slag plates)."""
    obj["inputs"][index] = {
        "amount": amount,
        "ingredient": {
            "type": "neoforge:components",
            "items": item_id,
            "components": components,
        },
    }
    return obj

def instiller(center, center_count, ing1, ing2, result_id, count=1, time=200, xp=2.0):
    mods = external_mods(center, ing1, ing2, result_id) | {"pastel"}
    return {
        "neoforge:conditions": conds(*mods),
        "type": "pastel:spirit_instiller",
        "time": time,
        "experience": xp,
        "ingredient1": ing1,
        "ingredient2": ing2,
        "center_ingredient": {"item": center, "count": center_count},
        "result": {"id": result_id, "count": count},
    }

def pedestal(pattern, key, colors, result_id, count=1, tier="complex", time=400, xp=4.0):
    mods = external_mods(*key.values(), result_id) | {"pastel"}
    return {
        "neoforge:conditions": conds(*mods),
        "type": "pastel:pedestal",
        "tier": tier,
        "time": time,
        "experience": xp,
        "colors": colors,
        "pattern": pattern,
        "key": key,
        "result": {"id": result_id, "count": count},
    }

def stonecutting(ingredient, result_id, count=1):
    mods = external_mods(ingredient, result_id)
    obj = {}
    if mods:
        obj["neoforge:conditions"] = conds(*mods)
    obj.update({
        "type": "minecraft:stonecutting",
        "ingredient": ({"tag": ingredient[1:]} if ingredient.startswith("#") else {"item": ingredient}),
        "result": {"id": result_id, "count": count},
    })
    return obj

def darkstone_pool(name, members):
    """All-pairs stonecutter exchange within a darkstone family (slab outputs = 2).
    Members are FA block paths (forbidden_arcanus:*). Members never cross pools."""
    for src in members:
        for dst in members:
            if src == dst:
                continue
            cnt = 2 if dst.endswith("_slab") else 1
            s = src.split(":")[-1]
            d = dst.split(":")[-1]
            write(f"{R}/stonecutting/darkstone_{name}/{s}__to__{d}.json",
                  stonecutting(src, dst, cnt))

def double_smelting(a, b, result_id, count=1, time=200, xp=0.5):
    mods = external_mods(a, b, result_id) | {"slag"}
    return {
        "neoforge:conditions": conds(*mods),
        "type": "slag:double_smelting",
        "ingredientA": ({"tag": a[1:]} if a.startswith("#") else {"item": a}),
        "ingredientB": ({"tag": b[1:]} if b.startswith("#") else {"item": b}),
        "result": {"id": result_id, "count": count},
        "experience": xp,
        "cookingTime": time,
    }

SP = lambda t, c: (f"malum:{t}", c)   # spirit tuple
EARLY4 = [SP("aerial", 4), SP("aqueous", 4), SP("earthen", 4), SP("infernal", 4)]
EARLY8 = [SP("aerial", 8), SP("aqueous", 8), SP("earthen", 8), SP("infernal", 8)]
ALL8x8 = EARLY8 + [SP("arcane", 8), SP("eldritch", 8), SP("sacred", 8), SP("wicked", 8)]

IRON_PLATE = {"slag:material_type": "slag:iron", "slag:part_type": "slag:plate"}
COPPER_PLATE = {"slag:material_type": "slag:copper", "slag:part_type": "slag:plate"}
GOLD_PLATE = {"slag:material_type": "slag:golden", "slag:part_type": "slag:plate"}

# ---------------------------------------------------------------- recipes

R = "data/bertieprogression/recipe"

# ---- inventory 2x2 (I2) ----
write(f"{R}/inventory_2x2/opening_mallet.json",            # R02B
      shapeless(["berlordscarving:wood_slate", "minecraft:stick"], "bertieprogression:opening_mallet"))
write(f"{R}/inventory_2x2/stone_crucible_blank.json",      # R02C
      shapeless(["berlordscarving:stone_slate", "minecraft:cobblestone"], "bertieprogression:stone_crucible_blank"))
write(f"{R}/inventory_2x2/stone_pour_channel.json",        # R02D
      shapeless(["berlordscarving:stone_slate", "minecraft:cobblestone", "minecraft:cobblestone"],
                "bertieprogression:stone_pour_channel"))
write(f"{R}/inventory_2x2/hand_crank.json",                # R14D  (PP/PA)
      shaped(["PP", "PA"], {"P": "#minecraft:planks", "A": "create:andesite_alloy"}, "create:hand_crank"))
# A chest without a crafting table. Vanilla's 8-plank ring is untouched; this is a second route.
write(f"{R}/inventory_2x2/chest.json",
      shaped(["SW", "PP"], {"S": "minecraft:stick", "W": "#minecraft:wooden_pressure_plates",
                            "P": "#minecraft:planks"},
             "minecraft:chest"))
# R18 (Licensed Crafting Plinth) was removed with the block.
# Pre-table Mundabitur Dust is shapeless 4 -> 1; the R28A table bulk route remains 6 -> 4.
write(f"{R}/inventory_2x2/mundabitur_dust_pretable.json",
      shapeless(["forbidden_arcanus:arcane_crystal_dust", "minecraft:phantom_membrane",
                 "minecraft:redstone", "minecraft:gunpowder"],
                "forbidden_arcanus:mundabitur_dust", 1))
# Pre-table Deorum Nugget: dust + rose gold + charcoal + one arcane speck -> one nugget.
write(f"{R}/inventory_2x2/deorum_nugget_pretable.json",
      shapeless(["forbidden_arcanus:mundabitur_dust", "slag:rose_gold_ingot",
                 "minecraft:charcoal", "forbidden_arcanus:arcane_crystal_dust_speck"],
                "forbidden_arcanus:deorum_nugget", 1))
# FA ships speck->dust (9 specks in a 3x3) but NO dust->speck split; add it so specks are
# obtainable pre-table (FA's own speck sources are gavel/loot, late and rare).
write(f"{R}/inventory_2x2/arcane_crystal_dust_split.json",
      shapeless(["forbidden_arcanus:arcane_crystal_dust"],
                "forbidden_arcanus:arcane_crystal_dust_speck", 9))
# Weeping Eye locates the Weeping Well. Its heated Create mixing recipe uses
# 1 Ender Pearl + 4 Prismarine Shards + 6 Refined Brilliance. (Basin recipes allow up to 64
# ingredient entries — BasinRecipe.getMaxInputCount, jar-verified — so 11 entries is fine.)
write(f"{R}/create/weeping_eye_mixing.json",
      {"neoforge:conditions": conds("create", "malum"), "type": "create:mixing",
       "heat_requirement": "heated",
       "ingredients": ([{"item": "minecraft:ender_pearl"}]
                       + [{"item": "minecraft:prismarine_shard"} for _ in range(4)]
                       + [{"item": "malum:refined_brilliance"} for _ in range(6)]),
       "results": [{"id": "bertieprogression:weeping_eye"}]})
# Stonecutter replaces the stock recipe with a shaped 2x2 that fits the pre-table grid.
write(f"{R}/inventory_2x2/stonecutter.json",
      shaped(["ID", "SS"], {"I": "minecraft:iron_ingot", "D": "slag:deep_alloy",
                            "S": "minecraft:smooth_stone"}, "minecraft:stonecutter"))

# ---- licensed-table 3x3 (T3) ----
write(f"{R}/table/mundabitur_bulk.json",                   # R28A
      shapeless(["forbidden_arcanus:arcane_crystal_dust", "minecraft:redstone", "minecraft:blaze_powder",
                 "minecraft:bone_meal", "minecraft:phantom_membrane", "minecraft:gunpowder"],
                "forbidden_arcanus:mundabitur_dust", 4))
# R22 (Fusion Shrine on the licensed table) was removed with the Pastel overrides. The Shrine uses
# Pastel's two pedestal recipes.
# R31 Warden Echo Pattern went with the Echo and Below questline.

# ---- Brick Forge double smelting (SLAG) ----
# Ore double-smelts consume two raw materials per ingot.
write(f"{R}/slag/first_copper_ingots.json",  double_smelting("minecraft:raw_copper", "minecraft:raw_copper", "minecraft:copper_ingot", 1))   # R05D1
write(f"{R}/slag/first_iron_ingots.json",    double_smelting("minecraft:raw_iron", "minecraft:raw_iron", "minecraft:iron_ingot", 1))         # R05D2
write(f"{R}/slag/first_gold_ingots.json",    double_smelting("minecraft:raw_gold", "minecraft:raw_gold", "minecraft:gold_ingot", 1))         # R05D3
write(f"{R}/slag/first_zinc_ingots.json",    double_smelting("create:raw_zinc", "create:raw_zinc", "create:zinc_ingot", 1))                  # R05D4
write(f"{R}/slag/first_silver_ingots.json",  double_smelting("#c:raw_materials/silver", "#c:raw_materials/silver", "iceandfire:silver_ingot", 1))
# Rose Gold: Slag's own bed gives 2 per copper+gold pair. One.
write("data/slag/recipe/double_smelting/rose_gold_ingot.json",
      double_smelting("#c:ingots/copper", "#c:ingots/gold", "slag:rose_gold_ingot", 1, 200, 1.4))
# Runes: two Runic Stones double-smelt into 2 Runes on the Brick Forge.
write(f"{R}/slag/runes.json",
      double_smelting("#forbidden_arcanus:runic_stones", "#forbidden_arcanus:runic_stones",
                      "forbidden_arcanus:rune", 2))
# R12 was removed because Brass now uses the Hephaestus ritual below.
# R18A was removed because Refined Soulstone now uses a Brick-Forge bed recipe
# (4 Raw Soulstone + 1 Diamond, see BedRecipes.refined_soulstone). Charcoal + vanilla-smelt routes gone.
# Arcane Crystal Dust is smelted in the Brick Forge, 2 crystal -> 1 dust
# (replaces the removed Mallet+crystal->4dust bed recipe R06C). The slag:double_smelting format has
# no secondary/chance output field, so this is a flat 2->1. A bonus would need a different recipe
# type or custom code.
write(f"{R}/slag/arcane_crystal_dust.json",
      double_smelting("forbidden_arcanus:arcane_crystal", "forbidden_arcanus:arcane_crystal",
                      "forbidden_arcanus:arcane_crystal_dust", 1, 200, 0.5))

# ---- Hephaestus rituals (HF1+, data-driven demo of the retained-recipe list) ----
RIT = "data/bertieprogression/forbidden_arcanus/hephaestus_forge/ritual"

# R13 Electron Tubes use the Spirit Altar.
# Same recipe feel: a quartz core + Redstone + Create's pressed Gold/Iron Sheets, paid in spirits.
# The Slag plate used to be an alternative here; Slag's parts are out of the pack, so the sheet is
# the only route and the compound wrapper went with it.
r13 = infusion("malum:natural_quartz", 1,
               [("minecraft:redstone", 2), ("create:golden_sheet", 1), ("create:iron_sheet", 1)],
               [SP("arcane", 2), SP("aerial", 2)], "create:electron_tube", 1)
# Either quartz works - Malum's Natural Quartz or the vanilla stone.
r13["input"] = {
    "type": "neoforge:compound",
    "ingredients": [{"item": "malum:natural_quartz"}, {"item": "minecraft:quartz"}],
    "count": 1,
}
write(f"{R}/malum/electron_tube.json", r13)

# Ashlord bone into Ice and Fire's: the Ashlord is killable long before a dragon is, and the
# progression asks for dragon bone either way. Bone Meal is the binder.
write(f"{R}/inventory_2x2/dragonbone_from_ashlord_bone.json",
      shapeless(["block_factorys_bosses:dragon_bone", "minecraft:bone_meal"],
                "iceandfire:dragonbone", 1))

# Brass Ingot: Hephaestus ritual with a Colossal Iron core, Deorum, 2 Zinc and 2 Rose
# Gold -> 2 Brass. Replaces the removed Brick-Forge double-smelt.
write(f"{RIT}/brass_ingot.json",
      ritual("armageddon_mod:colossal_iron_ingot",
             [("forbidden_arcanus:deorum_ingot", 1), ("create:zinc_ingot", 2),
              ("slag:rose_gold_ingot", 2)],
             "create:brass_ingot", 2, tier=1))

# Brass Casing: apply a Brass Ingot to an Edelwood Log with Create item application.
write(f"{R}/create/brass_casing_edelwood.json", {
    "neoforge:conditions": conds("create", "forbidden_arcanus"),
    "type": "create:item_application",
    "ingredients": [{"tag": "forbidden_arcanus:edelwood_logs"}, {"item": "create:brass_ingot"}],
    "results": [{"id": "create:brass_casing"}],
})

# r14_water_wheel was removed because Water Wheels now use bound-soul sequenced assembly.
# (Stale ritual file is rm'd; rituals are a hard registry so it must be deleted, not condition-disabled.)
# r14a (Brass Casing ritual) removed — Brass Casing is now the Edelwood-Log item application above.
# r14a0 andesite_casing_blank was removed with the custom casing chain;
# create:andesite_casing restored to its Create default item-application (see the DISABLED block below).
write(f"{RIT}/r14b_kinetic_pattern_plates.json",
      ritual("berlordscarving:stone_big_slate",
             [("create:brass_nugget", 4), ("forbidden_arcanus:arcane_crystal_dust", 1)],
             "bertieprogression:kinetic_pattern_plate", 4, tier=1))
# r14c gearbox-from-blank was removed; the gearbox uses Create's default
# recipe (andesite casing + 4 shafts), which works again now that andesite casing is restored.
# R15 Mechanical Crafter uses the Spirit Altar and yields one:
# Brass Casing core + Dragon Bone + Electron Tube + Cogwheel, paid in Eldritch/Earthen/Arcane.
write(f"{R}/malum/mechanical_crafter.json",
      infusion("create:brass_casing", 1,
               [("block_factorys_bosses:dragon_bone", 1), ("create:electron_tube", 1),
                ("create:cogwheel", 1), ("minecraft:diamond", 1)],
               [SP("eldritch", 4), SP("earthen", 4), SP("arcane", 4)],
               "create:mechanical_crafter", 1))
# R16 was removed with the Crafting Language Seal, Witness and Slate. The consumable Crafting
# License defined below is now the 3x3 gate.

# Earplugs overwrite Ice & Fire's recipe: String core + 2 Planks + 6 Wool, 20 aureal.
write(f"{RIT}/earplugs.json",
      ritual("minecraft:string",
             [("#minecraft:planks", 2), ("#minecraft:wool", 6)],
             "iceandfire:earplugs", 1, tier=1,
             essences={"aureal": 20, "blood": 0, "souls": 0}))
write("data/iceandfire/recipe/earplugs.json", DISABLED)

# Builder Stone overwrites Armageddon's recipe: Siren Tear core + 2 Colossal Iron +
# 2 Gilded Plate + Gilded Ingot smithing template + 2 Amethyst + 1 Emerald (8 = full pedestal ring).
write(f"{RIT}/builder_stone.json",
      ritual("iceandfire:siren_tear",
             [("armageddon_mod:colossal_iron_ingot", 2), ("armageddon_mod:gilded_plate", 2),
              ("armageddon_mod:gilded_ingot_smithing_template", 1),
              ("minecraft:amethyst_shard", 2), ("minecraft:emerald", 1)],
             "armageddon_mod:builder_stone", 1, tier=1,
             essences={"aureal": 200, "blood": 10000, "souls": 10}))
write("data/armageddon_mod/recipe/builderstonerecipe.json", DISABLED)

# Spirit Altar: Runewood Planks core + 4 Refined Soulstone + 4 Deorum Ingots,
# 100 XP / 5000 blood / 10 souls / 500 aureal. The four Slag golden plates were the original fourth
# input; Deorum replaces them now that Slag's parts are out of the pack.
write(f"{RIT}/r19_spirit_altar.json",
      ritual("malum:runewood_planks",
             [("malum:refined_soulstone", 4), ("forbidden_arcanus:deorum_ingot", 4)],
             "malum:spirit_altar", 1, tier=1,
             essences={"aureal": 500, "blood": 5000, "souls": 10}, xp=100))

# R26A Nether Lintel Core ritual was removed with the Nether Lintel and
# the Core themselves - both items are obsolete now the Nether is entered through the Netherly Meal.
# This also closes the long-open "what replaces the Crafting Language Seal as its third input"
# question: there is no ritual left to have a third input.

# R29B Ritual Burner Cage was removed with the item.

write(f"{RIT}/r29_spirit_instiller.json",
      ritual("pastel:pedestal_onyx",
             [("malum:arcana_pylon", 1), ("forbidden_arcanus:deorum_ingot", 1), ("create:brass_sheet", 1),
              ("malum:arcane_spirit", 2), ("malum:eldritch_spirit", 1),
              ("malum:sacred_spirit", 1), ("malum:wicked_spirit", 1)],
             "pastel:spirit_instiller", 1, tier=2))
# R30 Twilight Concord ritual was removed because it consumed the deleted Serpent Scale Blank and
# the Ritual Burner Cage, both now deleted. The Concord's spirit-infusion route (C2) is the sole one.
# R30A Echoing City Compass went with the Echo and Below questline.
# R31A Spirit Crucible ritual was removed; Malum's own spirit infusion
# (furnace + 2 hex ash + 8 tainted + 8 twisted, 8 infernal / 8 aqueous) is the sole route now.
# The Carving Station has an additive HF1 route while its stock 2x2 recipe remains, with no XP cost.
write(f"{RIT}/carving_station.json",
      ritual("minecraft:stonecutter",
             [("minecraft:amethyst_shard", 1), ("minecraft:heart_of_the_sea", 1),
              ("minecraft:chiseled_deepslate", 2)],
             "berlordscarving:carving_station", 1, tier=1,
             essences={"aureal": 100, "blood": 1000, "souls": 5}))
write(f"{RIT}/r32_descent_anchor.json",
      ritual("bertieprogression:spirit_focused_echo",
             [("deeperdarker:warden_carapace", 1), ("twilightforest:lich_trophy", 1), ("pastel:onyx_shard", 1)],
             "bertieprogression:descent_anchor", 1, tier=2))
write(f"{RIT}/r36a_soulbinding_brazier.json",
      ritual("betterend:aeternium_ingot",
             [("malum:soul_stained_steel_ingot", 4), ("malum:hallowed_gold_ingot", 2),
              ("minecraft:dragon_head", 1)],
             "malum:soulbinding_brazier", 1, tier=3))
write(f"{RIT}/r37f_ignis_rematch_seal.json",
      ritual("minecraft:blaze_powder",
             [("minecraft:blaze_powder", 3), ("minecraft:nether_bricks", 3), ("malum:infernal_spirit", 2)],
             "bertieprogression:boss_rematch_seal", 1, tier=3))

# ---- Malum spirit infusions ----
# R19A was removed with the Spirit Altar Witness item.
# R20A Runewood Resonance and R21C Arcana Resonance were both removed; the Obelisk, Pylon,
# Runic Workbench and every rune are back on Malum's own recipes.
write(f"{R}/malum/sculk_blocks.json",                      # R30S
      infusion("minecraft:deepslate", 1, [("malum:refined_soulstone", 1)], [SP("aqueous", 8)],
               "minecraft:sculk", 8))
write(f"{R}/malum/spirit_focused_echo.json",               # R31B (soul crystal consumed — demo deviation)
      infusion("deeperdarker:reinforced_echo_shard", 1, [("deeperdarker:soul_crystal", 1)],
               [SP("arcane", 8), SP("aqueous", 8)], "bertieprogression:spirit_focused_echo"))
write(f"{R}/malum/ashlord_rematch_seal.json",              # R37G
      infusion("deeperdarker:sculk_bone", 4,
               [("minecraft:end_stone_bricks", 4), ("bertieprogression:spirit_focused_echo", 1)],
               [SP("infernal", 16)], "bertieprogression:boss_rematch_seal"))

# ---- Mechanical Crafter recipes ----
# R17 was removed with the Seal.
# R21A Runic Workbench was removed: Malum's own spirit infusion is the only route again.
# R24A Victory Ledger and R27 Nether Lintel recipes were removed with their blocks and items.
# R31C Echo Lock went with the Echo and Below questline.
# R41 Nether Crafting Table went back to Avaritia's own recipe.
# The two compressed tables become crafter walls instead of 3x3 stacks of themselves: nine tables
# by hand was never the interesting part.
write(f"{R}/mechanical/exclusive/compressed_crafting_table.json",       # R41A
      mech(["CCCC", "CCCC", "CCCC", "CCCC"],
           {"C": "minecraft:crafting_table"},
           "avaritia:compressed_crafting_table"))
write(f"{R}/mechanical/exclusive/double_compressed_crafting_table.json",  # R41B
      mech(["CCCCC", "CCCCC", "CCCCC", "CCCCC", "CCCCC"],
           {"C": "avaritia:compressed_crafting_table"},
           "avaritia:double_compressed_crafting_table"))

# ---- Pastel ----
write(f"{R}/pastel/moonstone_synthesis.json",              # R32A
      instiller("pastel:moonstone_shard", 1, "pastel:bismuth_flake", "pastel:onyx_powder",
                "pastel:moonstone_shard", 4, 400, 4.0))
# R31R the 32-shard echo batch went with R31.

# R25 proof-replication family was removed with the proof items.

# ---- Avaritia capstone ----

# Sculk Crafting Table: still a plain 3x3, but it now costs the soul-infused metal and the two
# relics rather than a ring of sculk. Overrides Avaritia's own file.
write("data/avaritia/recipe/sculk_crafting_table.json",
      shaped(["SKS", "FDF", "SBS"],
             {"S": "elemental_metals:soul_infused_iron_ingot", "K": "minecraft:sculk_shrieker",
              "F": "irons_spellbooks:ancient_knowledge_fragment",
              "D": "avaritia:double_compressed_crafting_table",
              "B": "mythsandlegends:bound_soul_ingot"},
             "avaritia:sculk_crafting_table"))

# Altar of Amethyst had no recipe at all - Cataclysm only places it in structures. On the Sculk
# table (avaritia:shaped_table tier 1: SCULK, NETHER, END, EXTREME are tiers 1-4).
write(f"{R}/avaritia/altar_of_amethyst.json", {
    "neoforge:conditions": conds("avaritia", "cataclysm", "irons_spellbooks", "l2complements",
                                 "iceandfire", "slag"),
    "type": "avaritia:shaped_table",
    "tier": 1,
    "key": {
        "A": {"item": "minecraft:amethyst_cluster"},
        "I": {"item": "cataclysm:ignitium_ingot"},
        "S": {"item": "irons_spellbooks:divine_soulshard"},
        "T": {"item": "l2complements:totemic_gold_block"},
        "R": {"item": "iceandfire:dragonscale_red"},
        "D": {"item": "slag:deep_alloy_block"},
    },
    "pattern": ["AIS", "TRT", "DRD"],
    "result": {"id": "cataclysm:altar_of_amethyst", "count": 1},
})

# Null Blaze Cube: Avaritia's own 5x5 now yields the inert cube, and the live one is blessed out of
# it on the Altar of Amethyst over a full Minecraft day.
write("data/avaritia/recipe/blaze_cube.json", {
    "neoforge:conditions": conds("avaritia"),
    "type": "avaritia:shaped_table",
    "tier": 2,
    "key": {"a": {"item": "minecraft:ancient_debris"}, "b": {"item": "minecraft:blaze_powder"},
            "c": {"item": "minecraft:fire_charge"}, "x": {"item": "minecraft:blaze_rod"},
            "y": {"item": "minecraft:bone"}},
    "pattern": [" bcb ", "byxyb", "cxaxc", "byxyb", " bcb "],
    "result": {"id": "bertieprogression:null_blaze_cube", "count": 1},
})
write(f"{R}/cataclysm/blaze_cube.json", {
    "neoforge:conditions": conds("avaritia", "cataclysm"),
    "type": "cataclysm:amethyst_bless",
    "ingredient": {"item": "bertieprogression:null_blaze_cube"},
    "result": {"id": "avaritia:blaze_cube"},
    "time": 24000,
})

# ---------------------------------------------------------------- stock overrides

# The 3x3 gate itself
write("data/minecraft/recipe/crafting_table.json", DISABLED)

# Smithing Table loses its bottom plank row -> fits the 2x2 inventory grid
# Replaces the Field Smithing Core route.
write("data/minecraft/recipe/smithing_table.json",
      shaped(["II", "PP"], {"I": "minecraft:iron_ingot", "P": "#minecraft:planks"},
             "minecraft:smithing_table"))

# Replace the vanilla Stonecutter recipe with the 2x2 iron/deep-alloy recipe over smooth stone.
write("data/minecraft/recipe/stonecutter.json", DISABLED)

# Deorum ingot <-> nugget uses a 4:1 ratio instead of FA's stock 9:1; override both recipes.
write("data/forbidden_arcanus/recipe/deorum_nugget_from_deorum_ingot.json",
      shapeless(["forbidden_arcanus:deorum_ingot"], "forbidden_arcanus:deorum_nugget", 4))
# Nugget -> ingot is a shaped 2x2 using four nuggets, not shapeless.
write("data/forbidden_arcanus/recipe/deorum_ingot_from_deorum_nugget.json",
      shaped(["NN", "NN"], {"N": "forbidden_arcanus:deorum_nugget"},
             "forbidden_arcanus:deorum_ingot", 1))

# Darkstone stonecutter exchange has two isolated pools (normal and arcane). Gilded Chiseled
# Polished Darkstone is the ARCANE entry point (obtainable early via the forge bed R07B).
FA = "forbidden_arcanus"
darkstone_pool("normal", [f"{FA}:{b}" for b in [
    "darkstone", "darkstone_slab", "darkstone_stairs", "darkstone_wall",
    "polished_darkstone", "polished_darkstone_slab", "polished_darkstone_stairs",
    "polished_darkstone_wall", "chiseled_polished_darkstone",
    "polished_darkstone_bricks", "cracked_polished_darkstone_bricks",
    "polished_darkstone_brick_slab", "polished_darkstone_brick_stairs",
    "polished_darkstone_brick_wall", "tiled_polished_darkstone_bricks"]])
darkstone_pool("arcane", [f"{FA}:{b}" for b in [
    "gilded_chiseled_polished_darkstone", "arcane_polished_darkstone",
    "arcane_polished_darkstone_slab", "arcane_polished_darkstone_stairs",
    "arcane_polished_darkstone_wall", "arcane_polished_darkstone_pillar",
    "chiseled_arcane_polished_darkstone"]])

# Create: pre-table kinetics get authored routes; no-grid bypasses closed (§3.3)
for p in ["crafting/kinetics/water_wheel", "crafting/kinetics/hand_crank",
          "crafting/materials/electron_tube", "crafting/kinetics/mechanical_crafter",
          "item_application/brass_casing_from_log", "item_application/brass_casing_from_wood"]:
    write(f"data/create/recipe/{p}.json", DISABLED)
# empty_blaze_burner remains enabled and receives the 3x3 and 5x5 recipes below. Lighting it by
# capturing a blaze remains a Create interaction. Create's default andesite-casing item applications
# also remain enabled as the intended casing route.

# Copper Casing accepts only stripped twilight oak as its wood, gating
# it behind the Twilight Forest. Overrides Create's any-stripped-log/wood item-applications.
def _copper_casing(wood_id, suffix):
    write(f"data/create/recipe/item_application/copper_casing_from_{suffix}.json",
          {"neoforge:conditions": conds("create", "twilightforest"),
           "type": "create:item_application",
           "ingredients": [{"item": wood_id}, {"tag": "c:ingots/copper"}],
           "results": [{"id": "create:copper_casing"}]})
_copper_casing("twilightforest:stripped_twilight_oak_log", "log")
_copper_casing("twilightforest:stripped_twilight_oak_wood", "wood")

# Furnace replaces vanilla with an iron frame and a cobble/netherrack base.
# On the crafting table (overwrites the vanilla recipe id) and the Mechanical Crafter.
_FURNACE_PAT = ["III", "I I", "CNC"]
_FURNACE_KEY = {"I": "minecraft:iron_ingot", "C": "minecraft:cobblestone", "N": "minecraft:netherrack"}
write("data/minecraft/recipe/furnace.json", shaped(_FURNACE_PAT, _FURNACE_KEY, "minecraft:furnace"))
write(f"{R}/mechanical/pre_table/furnace.json", mech(_FURNACE_PAT, _FURNACE_KEY, "minecraft:furnace"))

# ==================================================== Progression machine recipes
# Every id below was verified in the pack instance jars (elemental_metals, iceandfire, malum,
# forbidden_arcanus, irons_spellbooks, slag, create, twilightforest).

# --- Blast Furnace (table + mechanical), Clibano Core, Refined Brilliance smelts ---
_BLAST_PAT = ["FFF", "FUF", "SMS"]
_BLAST_KEY = {"F": "elemental_metals:fire_infused_iron_ingot", "U": "minecraft:furnace",
              "S": "minecraft:smooth_stone", "M": "minecraft:magma_block"}
write("data/minecraft/recipe/blast_furnace.json", shaped(_BLAST_PAT, _BLAST_KEY, "minecraft:blast_furnace"))
write(f"{R}/mechanical/pre_table/blast_furnace.json", mech(_BLAST_PAT, _BLAST_KEY, "minecraft:blast_furnace"))
# Clibano Core: Spirit Altar (Malum) infusion of a Brick Forge.
write(f"{R}/malum/clibano_core.json",
      infusion("slag:brick_forge", 1,
               [("minecraft:blast_furnace", 4), ("forbidden_arcanus:chiseled_polished_darkstone", 8),
                ("forbidden_arcanus:rune", 12), ("iceandfire:fire_lily", 6)],
               [("malum:infernal", 48), ("malum:wicked", 32), ("malum:sacred", 32),
                ("malum:earthen", 16), ("malum:aerial", 16)],
               "forbidden_arcanus:clibano_core", 1))
# ...and only that. FA's own 3x3 for the core is gone.
write("data/forbidden_arcanus/recipe/clibano_core.json", DISABLED)
# Refined Brilliance: Brick Forge 2->1; Furnace + Blast 1->1. (Clibano 1->1.5 NOT done — FA's clibano
# recipe only supports a typed `residue`, not a 50%-chance copy of the result; needs a mixin.)
write(f"{R}/slag/refined_brilliance_forge.json",
      double_smelting("malum:raw_brilliance", "malum:raw_brilliance", "malum:refined_brilliance", 1))
def _cook(kind, tid, src, dst, time):
    write(f"{R}/cooking/{tid}.json",
          {"neoforge:conditions": conds("malum"), "type": kind, "category": "misc",
           "ingredient": {"item": src}, "result": {"id": dst}, "experience": 0.3, "cookingtime": time})
_cook("minecraft:smelting", "refined_brilliance_smelt", "malum:raw_brilliance", "malum:refined_brilliance", 200)
_cook("minecraft:blasting", "refined_brilliance_blast", "malum:raw_brilliance", "malum:refined_brilliance", 100)

# --- Elemental infused-iron ingots (Spirit Altar; input iron ingot, +4 uncommon ink last) ---
_INK = ("irons_spellbooks:uncommon_ink", 4)
def _elem(out, scale, mid, mcount, extra_id, sp1, sp2):
    write(f"{R}/malum/elem_{out}.json",
          infusion("minecraft:iron_ingot", 1,
                   [(f"iceandfire:sea_serpent_scales_{scale}", 1), (mid, mcount), (extra_id, 1), _INK],
                   [(f"malum:{sp1}", 8), (f"malum:{sp2}", 4)],
                   f"elemental_metals:{out}_infused_iron_ingot", 1))
_elem("arcane", "purple", "minecraft:ender_pearl", 3, "irons_spellbooks:arcane_ingot", "arcane", "eldritch")
_elem("fire", "red", "minecraft:blaze_powder", 3, "minecraft:lava_bucket", "infernal", "wicked")
_elem("frost", "blue", "minecraft:snowball", 3, "minecraft:packed_ice", "aqueous", "wicked")
_elem("lightning", "bronze", "minecraft:glowstone_dust", 3, "irons_spellbooks:lightning_bottle", "aerial", "infernal")
_elem("soul", "teal", "minecraft:soul_sand", 3, "forbidden_arcanus:corrupt_soul", "sacred", "aerial")
# Healing uses a plain potion because Malum ignores the potion component; a component-matched
# regeneration potion appears in EMI as the base "Uncraftable Potion" instead.
write(f"{R}/malum/elem_healing.json",
      infusion("minecraft:iron_ingot", 1,
               [("iceandfire:sea_serpent_scales_green", 1), ("minecraft:seagrass", 3),
                ("minecraft:potion", 1), ("irons_spellbooks:uncommon_ink", 4)],
               [SP("earthen", 8), SP("sacred", 4)],
               "elemental_metals:healing_infused_iron_ingot", 1))

# --- Twilight Concord, Arcane Ingot, Soulstained Steel (all Spirit Altar) ---
write(f"{R}/malum/twilight_concord.json",
      infusion("iceandfire:cyclops_eye", 1,
               [("malum:mnemonic_fragment", 27), ("malum:null_slate", 9), ("irons_spellbooks:arcane_ingot", 3),
                ("minecraft:ender_pearl", 3), ("iceandfire:stymphalian_bird_feather", 3),
                ("create:andesite_alloy", 27), ("malum:soul_stained_steel_ingot", 3)],
               [(f"malum:{s}", 27) for s in ("aerial", "aqueous", "arcane", "earthen",
                                             "eldritch", "infernal", "sacred", "wicked")],
               "bertieprogression:twilight_concord", 1))
write(f"{R}/malum/arcane_ingot.json",
      infusion("forbidden_arcanus:deorum_ingot", 1, [("irons_spellbooks:arcane_essence", 8)],
               [("malum:arcane", 4)], "irons_spellbooks:arcane_ingot", 1))
write(f"{R}/malum/soulstained_steel.json",
      infusion("forbidden_arcanus:obsidiansteel_ingot", 1, [("malum:refined_soulstone", 4)],
               [("malum:earthen", 3), ("malum:arcane", 6), ("malum:wicked", 9)],
               "malum:soul_stained_steel_ingot", 1))

# --- Twilight Forest portal reconfiguration; replace=true drops the vanilla entries ---
write("data/twilightforest/tags/item/portal/activator.json",
      {"replace": True, "values": ["bertieprogression:twilight_concord"]})
write("data/twilightforest/tags/block/portal/fluid.json",
      {"replace": True, "values": ["slag:molten_prismarine"]})
write("data/twilightforest/tags/block/portal/decoration.json",
      {"replace": True, "values": ["iceandfire:fire_lily", "iceandfire:frost_lily", "iceandfire:lightning_lily"]})

# --- Slag foundry: mechanical versions, Deep Alloy block, crucible, drain/interface and melter ---
_DA = "#c:ingots/deep_alloy"
# Block of Deep Alloy: Create compacting (press + basin), 9 deep alloy.
write(f"{R}/create/deep_alloy_block_compacting.json",
      {"neoforge:conditions": conds("create", "slag"), "type": "create:compacting",
       "ingredients": [{"item": "slag:deep_alloy"} for _ in range(9)],
       "results": [{"id": "slag:deep_alloy_block"}]})
# Casting table + basin: mecha versions mirroring Slag's crafting recipes.
write(f"{R}/mechanical/pre_table/casting_table.json", mech(["AAA", "A A"], {"A": _DA}, "slag:table"))
write(f"{R}/mechanical/pre_table/casting_basin.json", mech(["A A", "A A", "AAA"], {"A": _DA}, "slag:basin"))
# Crucible: table recipe forced to output 1 (Slag ships 4) + mecha version, also 1.
_CRU_PAT, _CRU_KEY = ["D D", "D D", "DBD"], {"D": _DA, "B": "#c:storage_blocks/deep_alloy"}
write("data/slag/recipe/crafting/crucible.json", shaped(_CRU_PAT, _CRU_KEY, "slag:crucible", 1))
write(f"{R}/mechanical/pre_table/crucible.json", mech(_CRU_PAT, _CRU_KEY, "slag:crucible", 1))
# Drain: new shape (3 deep alloy) replaces Slag's 1-ingot shapeless, on table + mecha.
_DRAIN_PAT = ["DD ", "  D"]
write("data/slag/recipe/crafting/drain.json", shaped(_DRAIN_PAT, {"D": _DA}, "slag:drain", 1))
write(f"{R}/mechanical/pre_table/drain.json", mech(_DRAIN_PAT, {"D": _DA}, "slag:drain", 1))
# Fluid Interface (crucible_interface): deep-alloy frame + brass, on table + mecha.
_FI_PAT, _FI_KEY = ["DDD", "B B", "DDD"], {"D": _DA, "B": "#c:ingots/brass"}
write("data/slag/recipe/crafting/crucible_interface.json", shaped(_FI_PAT, _FI_KEY, "slag:crucible_interface", 1))
write(f"{R}/mechanical/pre_table/fluid_interface.json", mech(_FI_PAT, _FI_KEY, "slag:crucible_interface", 1))
# Melter: now a Hephaestus ritual (clibano core core + 8 pedestals); Slag's native craft AND the old
# bertie bed route (R05A, removed in BedRecipes.java) are gone so the ritual is the sole source.
write("data/slag/recipe/crafting/melter.json", DISABLED)
write(f"{RIT}/melter.json",
      ritual("forbidden_arcanus:clibano_core",
             [("slag:crucible", 4), ("slag:rose_gold_block", 1), ("create:fluid_tank", 1),
              ("forbidden_arcanus:smelter_prism", 1), ("slag:drain", 1)],
             "slag:melter", 1, tier=1, essences={"aureal": 500, "blood": 0, "souls": 10}))

# ==================================================== Quest line 3 — Create kinetics
# Table recipes OVERWRITE Create's own (same resource path); Mechanical Crafter versions are added.
# Symbols: A andesite_alloy, B andesite_casing, S shaft, C cogwheel, W large_cogwheel, H whisk,
# F propeller, R iron_bars, V wool, E iron_sheet, I iron_block (press) / iron_ingot (chute). N=empty.
_CK = "data/create/recipe/crafting/kinetics"
_AA, _AC, _SH, _CG, _LC = "create:andesite_alloy", "create:andesite_casing", "create:shaft", "create:cogwheel", "create:large_cogwheel"
_ISH, _BS, _CS = "create:iron_sheet", "create:brass_sheet", "create:copper_sheet"
_ET, _PR = "create:electron_tube", "create:propeller"
def _p(rows):  # Recipe diagrams use N for empty; Minecraft patterns use spaces.
    return [r.replace("N", " ") for r in rows]

# Depot
_dk = {"A": _AA, "B": _AC}
# Depot uses three casings on the bottom row and no top row.
write(f"{_CK}/depot.json", shaped(_p(["AAA", "BBB"]), _dk, "create:depot"))
write(f"{R}/mechanical/kinetics/depot.json", mech(_p(["ANNNA", "AAAAA", "NBBBN"]), _dk, "create:depot"))
# Mechanical Press (I = iron BLOCK)
_pk = {"S": _SH, "C": _CG, "B": _AC, "I": "minecraft:iron_block"}
# Press table: the top-middle remains a shaft and the cogwheels become casings.
write(f"{_CK}/mechanical_press.json", shaped(_p(["NSN", "BBB", "NIN"]), {"S": _SH, "B": _AC, "I": "minecraft:iron_block"}, "create:mechanical_press"))
# The 5x5 press keeps a shaft at the top-middle and a large cogwheel at the centre, with casings in
# place of the small cogwheels.
write(f"{R}/mechanical/kinetics/mechanical_press.json",
      mech(_p(["NBSBN", "SBWBS", "NBSBN", "NNSNN", "NIIIN"]),
           {"S": _SH, "B": _AC, "I": "minecraft:iron_block", "W": _LC}, "create:mechanical_press"))
# Mechanical Mixer
_mk = {"S": _SH, "C": _CG, "B": _AC, "H": "create:whisk"}
write(f"{_CK}/mechanical_mixer.json", shaped(_p(["NSN", "CBC", "NHN"]), _mk, "create:mechanical_mixer"))
write(f"{R}/mechanical/kinetics/mechanical_mixer.json",
      mech(_p(["NNSNN", "NBSBN", "NCWCN", "NBSBN", "NNHNN"]), {**_mk, "W": _LC}, "create:mechanical_mixer"))
# Basin
write(f"{_CK}/basin.json", shaped(_p(["ANA", "ANA", "AAA"]), {"A": _AA}, "create:basin"))
write(f"{R}/mechanical/kinetics/basin.json", mech(_p(["ANNNA", "ANNNA", "ANNNA", "AAAAA"]), {"A": _AA}, "create:basin"))
# Whisk — table recipe kept (Create default); Mechanical Crafter version added
write(f"{R}/mechanical/kinetics/whisk.json",
      mech(_p(["NNANN", "NEAEN", "EAEAE", "EAEAE", "NENEN"]), {"A": _AA, "E": _ISH}, "create:whisk"))
# Encased Fan — 3x3 table + 5x5 mecha
_fk = {"E": _ISH, "S": _SH, "B": _AC, "F": _PR}
write(f"{_CK}/encased_fan.json", shaped(_p(["EEE", "SBF", "EEE"]), _fk, "create:encased_fan"))
write(f"{R}/mechanical/kinetics/encased_fan.json",
      mech(_p(["BBBBB", "BCCCR", "SSNCF", "BCCCR", "BBBBB"]), {"B": _AC, "C": _CG, "R": "minecraft:iron_bars", "S": _SH, "F": _PR}, "create:encased_fan"))

# --- Recipe changes that overwrite Create's crafting recipes ---
# Brass ingot via mixing: Zinc + Rose Gold.
write("data/create/recipe/mixing/brass_ingot.json",
      {"neoforge:conditions": conds("create", "slag"), "type": "create:mixing",
       "ingredients": [{"item": "create:zinc_ingot"}, {"item": "slag:rose_gold_ingot"}],
       "results": [{"id": "create:brass_ingot"}]})
write(f"{_CK}/chute.json", shaped(_p(["INI", "INI", "ENE"]), {"I": "minecraft:iron_ingot", "E": _ISH}, "create:chute"))
write(f"{_CK}/smart_chute.json", shaped(_p(["PNP", "PKP", "PTP"]), {"P": _BS, "K": "create:chute", "T": _ET}, "create:smart_chute"))
write(f"{_CK}/fluid_pipe.json", shaped(_p(["SNS", "INI", "SNS"]), {"S": _CS, "I": "minecraft:copper_ingot"}, "create:fluid_pipe"))
# Remove the horizontal fluid-pipe recipe.
write(f"{R}/create/fluid_pipe_horizontal.json", DISABLED)
# Remove the old four-output fluid pipe recipe. Create ships two: fluid_pipe.json is
# overridden with the copper recipe above; fluid_pipe_vertical.json is the leftover, so disable it.
write("data/create/recipe/crafting/kinetics/fluid_pipe_vertical.json", DISABLED)
# Smart fluid pipe: electron tube in the middle, fluid pipes above and below.
write(f"{_CK}/smart_fluid_pipe.json", shaped(_p(["PFP", "PTP", "PFP"]), {"P": _BS, "F": "create:fluid_pipe", "T": _ET}, "create:smart_fluid_pipe"))
write(f"{_CK}/fluid_valve.json", shaped(_p(["IFI", "ISI", "IFI"]), {"I": _ISH, "F": "create:fluid_pipe", "S": "create:speedometer"}, "create:fluid_valve"))
write(f"{_CK}/mechanical_pump.json", shaped(_p(["IFI", "ICI", "IFI"]), {"I": _ISH, "F": "create:fluid_pipe", "C": _CG}, "create:mechanical_pump"))
write(f"{_CK}/weighted_ejector.json", shaped(_p(["GGG", "SDS", "NCN"]), {"G": "create:golden_sheet", "S": _SH, "D": "create:depot", "C": _CG}, "create:weighted_ejector"))
write(f"{_CK}/copper_valve_handle.json", shaped(_p(["ZNZ", "ZAZ", "NZN"]), {"Z": _CS, "A": _AA}, "create:copper_valve_handle"))
write(f"{_CK}/nozzle.json", shaped(_p(["AAA", "VNV", "AAA"]), {"A": _AA, "V": "#minecraft:wool"}, "create:nozzle"))
# Propeller and Wrench receive additive 5x5 mechanical recipes; Create's normal Wrench recipe
# remains.
write(f"{R}/mechanical/kinetics/propeller.json",
      mech(_p(["NEENN", "NNENE", "EEAEE", "ENENN", "NNEEN"]), {"E": _ISH, "A": _AA}, "create:propeller"))
write(f"{R}/mechanical/kinetics/wrench.json",
      mech(_p(["NGGGC", "NNGSC", "NGSCN", "NSNNN", "SNNNN"]),
           {"G": "create:golden_sheet", "S": "minecraft:stick", "C": _CG}, "create:wrench"))

# ==================================================== "slow Clibano" (was the ignitium demo)
# Slow soul-fire Clibano alloy: Hallowed Gold + Soulstained
# Steel -> Bound Soul Ingot (mythsandlegends), cooking_time 9000, with an Arcane Crystal Dust residue.
# Filename/residue_type id kept as "ignitium" to avoid stale files; ids are internal. Bound Soul Ingot
# keeps its own original recipe too (this is an extra route). NEEDS A RESTART (residue_type = dynamic
# registry). Gates: Artisan Relic + soul fire.
write(f"{R}/ignitium_ingot_from_clibano_combustion.json", {
    "type": "forbidden_arcanus:clibano_combustion",
    "category": "misc",
    "cooking_time": 9000,
    "enhancer": "forbidden_arcanus:artisan_relic",
    "experience": 1.0,
    "fire_type": "soul_fire",
    "ingredients": {"first": {"item": "malum:hallowed_gold_ingot"},
                    "second": {"item": "malum:soul_stained_steel_ingot"}},
    "residue": {"type": "bertieprogression:ignitium", "chance": 0.1},
    "result": {"count": 1, "id": "mythsandlegends:bound_soul_ingot"},
})
# Silver and zinc get a Clibano route as well as the Brick Forge one (zinc's forge bed is R05D4).
write(f"{R}/silver_ingot_from_clibano_combustion.json", {
    "type": "forbidden_arcanus:clibano_combustion",
    "category": "misc",
    "cooking_time": 200,
    "experience": 0.7,
    "fire_type": "fire",
    "ingredients": {"first": {"tag": "c:raw_materials/silver"},
                    "second": {"tag": "c:raw_materials/silver"}},
    "result": {"count": 3, "id": "iceandfire:silver_ingot"},
})
write(f"{R}/zinc_ingot_from_clibano_combustion.json", {
    "type": "forbidden_arcanus:clibano_combustion",
    "category": "misc",
    "cooking_time": 200,
    "experience": 0.7,
    "fire_type": "fire",
    "ingredients": {"first": {"item": "create:raw_zinc"}, "second": {"item": "create:raw_zinc"}},
    "result": {"count": 3, "id": "create:zinc_ingot"},
})
# Slow-clibano residue is Arcane Crystal Dust rather than the secondary input.
write("data/bertieprogression/forbidden_arcanus/residue_type/ignitium.json", {
    "combine_info": {"required_amount": 1, "result": {"count": 1, "id": "forbidden_arcanus:arcane_crystal_dust"}},
    "name": {"text": "Arcane Crystal Residue"},
})

# Deeper and Darker's own Reinforced Echo recipe is restored: R31/R31R, which replaced it, went
# with the Echo and Below questline, and two late recipes still need the shard.

# Sophisticated Backpacks: the plain backpack drops from a 3x3 to the 2x2 grid, so it lands with the
# chest rather than after it. Keeps sophisticatedbackpacks:basic_backpack - the type carries the
# upgrade slots and the item-enabled condition, and a vanilla shaped recipe would lose both.
write("data/sophisticatedbackpacks/recipe/backpack.json", {
    "neoforge:conditions": [{"type": "sophisticatedcore:item_enabled",
                             "itemRegistryName": "sophisticatedbackpacks:backpack"}],
    "type": "sophisticatedbackpacks:basic_backpack",
    "category": "misc",
    "key": {"L": {"tag": "c:leathers"}, "S": {"tag": "c:strings"},
            "C": {"tag": "c:chests/wooden"}, "U": {"item": "minecraft:bundle"}},
    "pattern": ["LS", "CU"],
    "result": {"count": 1, "id": "sophisticatedbackpacks:backpack"},
})
# The tiers above it stop being rings of ingots and become one step each of the pack's own systems.
# All four keep sophisticatedbackpacks' own recipe types where a type exists: backpack_upgrade is
# what moves a full backpack's contents into the new one, and a plain shaped recipe would hand the
# player an empty pack and eat what was inside.
write("data/sophisticatedbackpacks/recipe/copper_backpack.json", {
    "neoforge:conditions": [{"type": "sophisticatedcore:item_enabled",
                             "itemRegistryName": "sophisticatedbackpacks:copper_backpack"}],
    "type": "sophisticatedbackpacks:backpack_upgrade",
    "category": "misc",
    "key": {"C": {"tag": "c:storage_blocks/copper"}, "R": {"item": "minecraft:rabbit_hide"},
            "B": {"item": "sophisticatedbackpacks:backpack"}},
    "pattern": ["CR", "BC"],
    "result": {"count": 1, "id": "sophisticatedbackpacks:copper_backpack"},
})
# Iron: a Tier-I ritual. 5000 blood and 50 aureal are both inside the T1 ceiling (1000/10/10000/900),
# and the eight pedestals are exactly full.
_iron_backpack = ritual("sophisticatedbackpacks:copper_backpack",
                        [("minecraft:iron_block", 4), ("armageddon_mod:colossal_iron_ingot", 2),
                         ("alexsmobs:kangaroo_hide", 2)],
                        "sophisticatedbackpacks:iron_backpack", 1, tier=1,
                        essences={"aureal": 50, "blood": 5000, "souls": 0})
# upgrade_storage instead of create_item: it hands the copper pack's identity to the iron one, so
# the ritual keeps whatever was inside. See BackpackHandover.
_iron_backpack["result"] = {"type": "bertieprogression:upgrade_storage",
                            "result_item": "sophisticatedbackpacks:iron_backpack"}
write(f"{RIT}/iron_backpack.json", _iron_backpack)
write("data/sophisticatedbackpacks/recipe/iron_backpack.json", DISABLED)
# Gold: the Spirit Altar, carrying components across so the pack keeps what is in it.
_gold_backpack = infusion("sophisticatedbackpacks:iron_backpack", 1,
                          [("minecraft:gold_block", 3), ("armageddon_mod:gilded_plate", 3),
                           ("malum:block_of_hallowed_gold", 1), ("slag:rose_gold_block", 1),
                           ("l2complements:totemic_gold_ingot", 3), ("alexscaves:tough_hide", 3),
                           ("armageddon_mod:gilded_ingot_smithing_template", 1)],
                          [SP("sacred", 6), SP("aqueous", 12), SP("earthen", 12)],
                          "sophisticatedbackpacks:gold_backpack", 1)
_gold_backpack["carryOverComponentData"] = True
write(f"{R}/malum/gold_backpack.json", _gold_backpack)
write("data/sophisticatedbackpacks/recipe/gold_backpack.json", DISABLED)
# Diamond is a sequenced assembly and lives with the other ones, below.

# Pastel's own recipes remain untouched. In particular, the Fusion Shrine keeps both stock pedestal
# recipes and R22 no longer moves it to the licensed table.

# Avaritia: both compressed tables are crafter-wall only (R41A/R41B); the stock 3x3 stacks go.
# The reverse "uncrafting" recipes are left alone - they only give back what you put in.
write("data/minecraft/recipe/compressed_crafting_table.json", DISABLED)
write("data/minecraft/recipe/double_compressed_crafting_table.json", DISABLED)

# Forbidden Arcanus: pedestal via Brick-Forge bed (R09A); stock 3x3 disabled
write("data/forbidden_arcanus/recipe/darkstone_pedestal.json", DISABLED)

# Slag armor swap: wooden/bone Slag armor is replaced by Immersive
# Armors' sets (carving armor-overrides config). Slag's own 2x2 part recipes for those 8 armor
# parts are disabled; TOOL parts (pickaxe heads etc.) are untouched.
for _part in ["helmet", "chestplate", "leggings", "boots"]:
    for _mat in ["wooden", "bone"]:
        write(f"data/slag/recipe/crafting/parts/{_part}_{_mat}.json", DISABLED)

# Immersive Armors originals removed for the same 8 pieces: CARVING is the route (the carving
# armor-overrides config points wood/bone big-slate carves at these items).
for _piece in ["helmet", "chestplate", "leggings", "boots"]:
    for _set in ["wooden", "bone"]:
        write(f"data/immersive_armors/recipe/{_set}_{_piece}.json", DISABLED)

# Malum: the pylon override was dropped with the resonance witness; catalyzer becomes the
# authored R31A2
write("data/malum/recipe/spirit_infusion/spirit_catalyzer.json",
      infusion("pastel:onyx_shard", 1,
               [("malum:hallowed_gold_ingot", 4), ("create:brass_sheet", 4)],
               ALL8x8, "malum:spirit_catalyzer"))

# God of War chapter: both spellbook entry recipes replace the
# stock 3x3 recipes with 2x2 pre-table shapes.
# Flimsy Journal (copper spell book): Copper Ingot + Leather over String + Paper.
write("data/irons_spellbooks/recipe/copper_spell_book.json",
      shaped(["CL", "SP"], {"C": "#c:ingots/copper", "L": "minecraft:leather",
                            "S": "minecraft:string", "P": "minecraft:paper"},
             "irons_spellbooks:copper_spell_book"))
# Inscription Table: Book and Quill top-right, two Planks below.
write("data/irons_spellbooks/recipe/inscription_table.json",
      shaped([" B", "PP"], {"B": "minecraft:writable_book", "P": "#minecraft:planks"},
             "irons_spellbooks:inscription_table"))

# FA: augmented tier upgrades (stock essences/main/magic_circle preserved; inputs appended)
# HF2 upgrade: the four elemental cores converge here, filling all eight pedestals.
# "all essence maxed" = the TIER I ceiling (1000/10/10000/900, jar-verified from HephaestusForgeLevel):
# the T1->T2 ritual is performed ON a tier-I forge, which physically cannot hold more than that.
write("data/forbidden_arcanus/forbidden_arcanus/hephaestus_forge/ritual/upgrade_tier_2.json", {
    "essences": {"aureal": 1000, "blood": 10000, "souls": 10, "experience": 900},
    "inputs": [
        {"amount": 2, "ingredient": {"item": "bertieprogression:abyssal_core"}},
        {"amount": 2, "ingredient": {"item": "bertieprogression:desert_core"}},
        {"amount": 2, "ingredient": {"item": "bertieprogression:cursed_core"}},
        {"amount": 2, "ingredient": {"item": "bertieprogression:storm_core"}},
        # Two of each core fill all eight pedestals, so the
        # 4 Arcane Crystal that used to fill the last four slots had to come out. There is no
        # arrangement of doubled cores that keeps them: 2+2+2+2+4 = 12 > 8.
    ],
    "magic_circle": "forbidden_arcanus:upgrade_tier",
    "main_ingredient": {"item": "forbidden_arcanus:carved_edelwood_log"},
    "match_tier_exact": True,
    "result": {"type": "forbidden_arcanus:upgrade_tier", "result_tier": 2},
})
write("data/forbidden_arcanus/forbidden_arcanus/hephaestus_forge/ritual/upgrade_tier_3.json", {
    "essences": {"aureal": 1000, "blood": 9000, "souls": 50},
    "forge_tier": 2,
    "inputs": [
        {"amount": 3, "ingredient": {"item": "forbidden_arcanus:arcane_crystal"}},
        {"amount": 3, "ingredient": {"item": "forbidden_arcanus:deorum_ingot"}},
        {"amount": 2, "ingredient": {"item": "pastel:moonstone_shard"}},
    ],
    "magic_circle": "forbidden_arcanus:upgrade_tier",
    "main_ingredient": {"item": "forbidden_arcanus:chiseled_polished_darkstone"},
    "match_tier_exact": True,
    "result": {"type": "forbidden_arcanus:upgrade_tier", "result_tier": 3},
})

# ================= Chapter 2 recipe overhaul =================

# Spirit Altar stock craft removed -> only the Hephaestus ritual (r19 above) makes it.
write("data/malum/recipe/spirit_altar.json", DISABLED)

# Refined Soulstone: only the Brick-Forge bed (4 Raw Soulstone + 1 Diamond, see BedRecipes).
# Disable the plain furnace/blast routes from raw. (Charcoal double-smelt R18A already removed.)
write("data/malum/recipe/soulstone_from_raw_smelting.json", DISABLED)
write("data/malum/recipe/soulstone_from_raw_blasting.json", DISABLED)

# Mnemonic Fragments: the Weeping Well only trades for Refined Brilliance. The raw-cluster favour
# handed out two fragments for unrefined ore and skipped the forge entirely.
write("data/malum/recipe/void_favor/mnemonic_fragment_from_cluster.json", DISABLED)

# Malignant Lead: still the Weeping Well's favour, but paid in lead rather than Cthonic Gold.
write("data/malum/recipe/void_favor/malignant_lead.json", {
    "type": "malum:void_favor",
    "input": {"tag": "c:ingots/lead"},
    "result": {"count": 1, "id": "malum:malignant_lead"},
})

# Andesite Alloy: only via the Brick Forge (Zinc Ingot + Andesite double-smelt). Disable Create's
# crafting-table routes (iron-nugget AND zinc-nugget) and both mixing routes.
write(f"{R}/slag/andesite_alloy.json",
      double_smelting("create:zinc_ingot", "minecraft:andesite", "create:andesite_alloy", 1, 200, 0.2))
# The zinc mixing route remains enabled; only the iron-nugget mixing recipe
# and both crafting-table routes are removed.
for _p in ["crafting/materials/andesite_alloy", "crafting/materials/andesite_alloy_from_zinc",
           "mixing/andesite_alloy"]:
    write(f"data/create/recipe/{_p}.json", DISABLED)

# Zinc nugget<->ingot becomes 4:1 (shaped 2x2 up; shapeless 1->4 down). Stock 1:9 both ways disabled.
write(f"{R}/inventory_2x2/zinc_ingot_from_nuggets.json",
      shaped(["NN", "NN"], {"N": "create:zinc_nugget"}, "create:zinc_ingot", 1))
write(f"{R}/inventory_2x2/zinc_nuggets_from_ingot.json",
      shapeless(["create:zinc_ingot"], "create:zinc_nugget", 4))
write("data/create/recipe/crafting/materials/zinc_ingot_from_compacting.json", DISABLED)
write("data/create/recipe/crafting/materials/zinc_nugget_from_decompacting.json", DISABLED)

# Windmill Sail (create:white_sail): 1 Edelwood Stick + 1 Shaft + 1 Wool + 1 Andesite Alloy -> 2.
write("data/create/recipe/crafting/kinetics/white_sail.json",
      shaped(["ES", "WA"], {"E": "forbidden_arcanus:edelwood_stick", "S": "create:shaft",
                            "W": "#minecraft:wool", "A": "create:andesite_alloy"},
             "create:white_sail", 2))

# Windmill Bearing: Hephaestus ritual (Polished Deepslate core + 4 Shaft + 2 Deorum Nuggets,
# 120 aureal / 5 souls). Stock crafting-table recipe disabled.
write(f"{RIT}/windmill_bearing.json",
      ritual("minecraft:polished_deepslate",
             [("create:shaft", 4), ("forbidden_arcanus:deorum_nugget", 2),
              ("minecraft:slime_ball", 2)],
             "create:windmill_bearing", 1, tier=1,
             essences={"aureal": 120, "blood": 0, "souls": 5}))
write("data/create/recipe/crafting/kinetics/windmill_bearing.json", DISABLED)

# ================= Chapter 2 row 5 + crafter wall =================

# Mundabitur: FA's stock 6-ingredient shapeless is identical in inputs to our R28A bulk (which yields
# 4), so EMI showed two identical recipes. Disable the stock one.
write("data/forbidden_arcanus/recipe/mundabitur_dust.json", DISABLED)

# Wayward Compass: Hephaestus — Compass core + 2 Arcane Essence + 4 Runes + 2 Ender Pearls (=8).
write(f"{RIT}/wayward_compass.json",
      ritual("minecraft:compass",
             [("forbidden_arcanus:rune", 4), ("irons_spellbooks:arcane_essence", 2),
              ("minecraft:ender_pearl", 2)],
             "irons_spellbooks:wayward_compass", 1, tier=1,
             essences={"aureal": 0, "blood": 0, "souls": 4}, xp=10))

# Crude Scythe: Hephaestus — Decrepit Scythe core + Colossal Iron + 3 Iron + 2 Ice&Fire Dragon Bone
# + Gilded Ingot smithing template + Lantern (=8).
write(f"{RIT}/crude_scythe.json",
      ritual("irons_spellbooks:decrepit_scythe",
             [("armageddon_mod:colossal_iron_ingot", 1), ("minecraft:iron_ingot", 3),
              ("iceandfire:dragonbone", 2), ("armageddon_mod:gilded_ingot_smithing_template", 1),
              ("minecraft:lantern", 1)],
             "malum:crude_scythe", 1, tier=1,
             essences={"aureal": 1000, "blood": 1000, "souls": 10}))

# The Dead King drops the Decrepit Scythe 100% of the time, unaffected by Looting. This is the mod's
# own loot table with one guaranteed, condition-free pool prepended (its original pools preserved).
write("data/irons_spellbooks/loot_table/entities/dead_king.json", {
    "type": "minecraft:entity",
    "pools": [
        {"rolls": 1, "bonus_rolls": 0.0,
         "entries": [{"type": "minecraft:item", "name": "irons_spellbooks:decrepit_scythe"}]},
        {"rolls": 1, "bonus_rolls": 0.0,
         "entries": [{"type": "minecraft:item", "name": "irons_spellbooks:arcane_essence",
                      "functions": [
                          {"function": "minecraft:set_count", "add": False,
                           "count": {"type": "minecraft:uniform", "min": 28.0, "max": 45.0}},
                          {"function": "minecraft:enchanted_count_increase",
                           "enchantment": "minecraft:looting",
                           "count": {"type": "minecraft:uniform", "min": 2.0, "max": 6.0}}]}]},
        {"rolls": 1,
         "entries": [{"type": "minecraft:item", "name": "irons_spellbooks:blood_staff"},
                     {"type": "minecraft:item", "name": "irons_spellbooks:necronomicon_spell_book"}],
         "conditions": [
             {"condition": "minecraft:killed_by_player"},
             {"condition": "minecraft:random_chance_with_enchanted_bonus",
              "enchantment": "minecraft:looting", "unenchanted_chance": 0.5,
              "enchanted_chance": {"type": "minecraft:linear", "base": 0.55,
                                   "per_level_above_first": 0.05}}]},
    ],
})

# Mechanical Crafters crafting Mechanical Crafters — additive, crafter-wall only, yields 1.
write(f"{R}/mechanical/exclusive/mechanical_crafter_wall.json",
      mech(["AEA", "SBS", "ACA"],
           {"A": "create:brass_ingot", "E": "create:electron_tube", "S": "create:shaft",
            "B": "create:brass_casing", "C": "create:cogwheel"},
           "create:mechanical_crafter", 1))

# THE CRAFTING TABLE: a 5x5 crafter wall — Edelwood Planks ring, Null Slate corners, Cogwheel sides
# and a Creaking Heart centre. vanillabackport registers the heart as minecraft:creaking_heart, with
# its blockstate, item model and loot table all in the Minecraft namespace.
CENTRE_ITEM = "minecraft:creaking_heart"
write(f"{R}/mechanical/pre_table/vanilla_crafting_table.json",
      mech(["PPPPP", "PNCNP", "PCHCP", "PNCNP", "PPPPP"],
           {"P": "forbidden_arcanus:edelwood_planks", "N": "malum:null_slate",
            "C": "create:cogwheel", "H": CENTRE_ITEM},
           "minecraft:crafting_table", 1))

# Once the wall has made the first one, a table copies itself out of nine planks. The stock 2x2
# recipe stays disabled — this one needs a grid you can only have by owning a table already.
write(f"{R}/table/crafting_table.json",
      shaped(["PPP", "PPP", "PPP"], {"P": "#minecraft:planks"},
             "minecraft:crafting_table", 1))

# ==================================================== Create machines + magic
# belt ITEM = create:belt_connector (create:belt is the placed block, not craftable).
_BELT = "create:belt_connector"
_p = lambda rows: [r.replace("N", " ") for r in rows]  # restore _p (a loop at ~L927 rebinds it to a str)

# --- Tunnels: 3x3 crafting-table recipe; 4x4 mechanical crafting.
#     Brass = swap andesite_alloy->brass_ingot, top-middle->electron_tube. (paths kept to avoid stale files) ---
write(f"{R}/mechanical/kinetics/andesite_tunnel_3x3.json",
      shaped(["AAA", "ABA", "ABA"], {"A": _AA, "B": _BELT}, "create:andesite_tunnel"))
write(f"{R}/mechanical/kinetics/andesite_tunnel_4x4.json",
      mech(["AAAA", "ABBA", "ABBA", "ABBA"], {"A": _AA, "B": _BELT}, "create:andesite_tunnel"))
write(f"{R}/mechanical/kinetics/brass_tunnel_3x3.json",
      shaped(["ATA", "ABA", "ABA"], {"A": "create:brass_ingot", "B": _BELT, "T": _ET}, "create:brass_tunnel"))
write(f"{R}/mechanical/kinetics/brass_tunnel_4x4.json",
      mech(["ATTA", "ABBA", "ABBA", "ABBA"], {"A": "create:brass_ingot", "B": _BELT, "T": _ET}, "create:brass_tunnel"))

# --- Funnels: 3x3 crafting table with ANA on the bottom row; 4x4 mechanical crafting ---
write(f"{R}/mechanical/kinetics/andesite_funnel_3x3.json",
      shaped(_p(["NAN", "ABA", "ANA"]), {"A": _AA, "B": _BELT}, "create:andesite_funnel"))
write(f"{R}/mechanical/kinetics/andesite_funnel_4x4.json",
      mech(_p(["NAAN", "ABBA", "ANNA", "ANNA"]), {"A": _AA, "B": _BELT}, "create:andesite_funnel"))
write(f"{R}/mechanical/kinetics/brass_funnel_3x3.json",
      shaped(_p(["NTN", "ABA", "ANA"]), {"A": "create:brass_ingot", "B": _BELT, "T": _ET}, "create:brass_funnel"))
write(f"{R}/mechanical/kinetics/brass_funnel_4x4.json",
      mech(_p(["NTTN", "ABBA", "ANNA", "ANNA"]), {"A": "create:brass_ingot", "B": _BELT, "T": _ET}, "create:brass_funnel"))
# Remove Create's stock tunnel and funnel recipes so the recipes above are the only routes.
for _tf in ["andesite_tunnel", "brass_tunnel", "andesite_funnel", "brass_funnel"]:
    write(f"data/create/recipe/crafting/logistics/{_tf}.json", DISABLED)

# --- Brass Hand (5x5 mech) ---
write(f"{R}/mechanical/kinetics/brass_hand.json",
      mech(_p(["NAAN", "NAAN", "BBBB", "NBNN"]), {"A": _AA, "B": _BS}, "create:brass_hand"))

# --- Crushing Wheel: override Create's 5x5 mechanical recipe. Keep the andesite-alloy filler,
#     replace planks with obsidiansteel ingots and the stone centre with canopy wood, and yield
#     one. ---
write("data/create/recipe/mechanical_crafting/crushing_wheel.json",
      mech(_p(["NAAAN", "AAOAA", "AOCOA", "AAOAA", "NAAAN"]),
           {"A": _AA, "O": "forbidden_arcanus:obsidiansteel_ingot", "C": "twilightforest:canopy_wood"},
           "create:crushing_wheel", 1))

# --- Hallowed Gold Ingot: Spirit Infusion (brass core + magic metals + 4 quartz + mnemonic) ---
_hg = infusion("create:brass_ingot", 1,
               [("malum:cthonic_gold", 1), ("forbidden_arcanus:deorum_ingot", 1),
                ("irons_spellbooks:arcane_ingot", 1), ("armageddon_mod:gilded_nugget", 1),
                ("minecraft:quartz", 4), ("malum:mnemonic_fragment", 1)],
               [SP("sacred", 9), SP("eldritch", 6), SP("infernal", 3)],
               "malum:hallowed_gold_ingot", 1)
# Either quartz works here too - Malum's Natural Quartz or the vanilla stone.
_hg["extraInputs"][4] = {
    "type": "neoforge:compound",
    "ingredients": [{"item": "minecraft:quartz"}, {"item": "malum:natural_quartz"}],
    "count": 4,
}
write(f"{R}/malum/hallowed_gold_ingot.json", _hg)

# --- Empty Blaze Burner: 3x3 table + 5x5 mech (replaces Create's craft; blaze-capture lighting stays) ---
_EBB = {"S": _ISH, "R": "minecraft:netherrack", "I": "slag:deep_alloy"}
write("data/create/recipe/crafting/kinetics/empty_blaze_burner.json",
      shaped(_p(["SNS", "SRS", "III"]), _EBB, "create:empty_blaze_burner"))
write(f"{R}/mechanical/kinetics/empty_blaze_burner.json",
      mech(_p(["SNNNS", "SNNNS", "SSSSS", "IRRRI", "IIIII"]), _EBB, "create:empty_blaze_burner"))

# --- Lit Blaze Burner: Spirit Infusion (additive; blaze-capture route unaffected) ---
write(f"{R}/malum/blaze_burner.json",
      infusion("create:empty_blaze_burner", 1,
               [("minecraft:nether_brick", 12), ("elemental_metals:fire_infused_iron_ingot", 2),
                ("born_in_chaos_v1:dark_metal_ingot", 1), ("minecraft:campfire", 1)],
               [SP("wicked", 16), SP("earthen", 16), SP("infernal", 32)],
               "create:blaze_burner", 1))

# An egg superheats the burner instead of merely lighting it — but only for a moment, so it is a
# stopgap you feed by hand, never a way to run a superheated basin unattended.
write("data/create/data_maps/item/superheated_blaze_burner_fuels.json",
      {"values": {"minecraft:egg": {"burn_time": 10}}})
# ...and then it goes OUT. Create would drop a spent special fuel to a lit burner with 5000 ticks
# left; that fallback is the Blaze Cake's reward and nothing else's. See BlazeBurnerBlockEntityMixin.
write("data/bertieprogression/tags/item/snuffing_blaze_fuel.json",
      {"replace": False, "values": ["minecraft:egg"]})

# --- Rose Quartz: additive mixing route with 8 redstone and 1 quartz. ---
write(f"{R}/create/rose_quartz_mixing.json",
      {"neoforge:conditions": conds("create"), "type": "create:mixing",
       "ingredients": ([{"item": "minecraft:redstone"} for _ in range(8)] + [{"item": "minecraft:quartz"}]),
       "results": [{"id": "create:rose_quartz"}]})

# --- Mechanical Saw: the 3x3 overrides Create's recipe by replacing the iron ingot with a
#     propeller; an additive 5x4 mechanical version uses the same ingredients. ---
write("data/create/recipe/crafting/kinetics/mechanical_saw.json",
      shaped(_p(["NAN", "APA", "NCN"]), {"A": "#c:plates/iron", "P": _PR, "C": _AC}, "create:mechanical_saw"))
write(f"{R}/mechanical/kinetics/mechanical_saw.json",
      mech(_p(["NSSSN", "SSISS", "SIPIS", "BBBBB"]),
           {"S": _ISH, "I": "minecraft:iron_ingot", "P": _PR, "B": _AC}, "create:mechanical_saw"))

# --- Deployer: the 3x3 overrides Create's stock column with shafts in the empty spots; an additive
#     5x5 mechanical version uses the same ingredients. ---
write("data/create/recipe/crafting/kinetics/deployer.json",
      shaped(["SBS", "SCS", "SIS"], {"S": _SH, "B": _ET, "C": _AC, "I": "create:brass_hand"}, "create:deployer"))
write(f"{R}/mechanical/kinetics/deployer.json",
      mech(_p(["NNBNN", "NACAN", "SACAS", "NATAN", "NNSNN"]),
           {"B": "create:brass_hand", "A": _AC, "S": _SH, "T": _ET, "C": _CG}, "create:deployer"))

# --- Fluid machines: table versions override Create; mechanical versions are additive.
#     "casing" here = copper_casing; "copper pipe" = create:fluid_pipe (no create:copper_pipe exists). ---
_COC = "create:copper_casing"
# Spout: 3x3 table + 5x5 mech
write("data/create/recipe/crafting/kinetics/spout.json",
      shaped(["SKS", "BPB", "SVS"],
             {"S": _CS, "K": _COC, "B": _BELT, "P": "create:fluid_pipe", "V": "create:copper_valve_handle"},
             "create:spout"))
write(f"{R}/mechanical/kinetics/spout.json",
      mech(_p(["CCPCC", "CBTBC", "CBTBC", "NCPCN", "NNVNN"]),
           {"C": _CS, "P": "create:fluid_pipe", "B": _COC, "T": "create:fluid_tank", "V": "create:copper_valve_handle"},
           "create:spout"))
# Copper Valve Handle: additive mech variant
write(f"{R}/mechanical/kinetics/copper_valve_handle_mecha.json",
      mech(_p(["CANAC", "CANAC", "NCACN", "NNCNN"]), {"C": _CS, "A": _AA}, "create:copper_valve_handle"))
# Fluid Tank: 3x3 table + 5x5 mech
write("data/create/recipe/crafting/kinetics/fluid_tank.json",
      shaped(["CCC", "CGC", "CCC"], {"C": _CS, "G": "minecraft:glass"}, "create:fluid_tank"))
write(f"{R}/mechanical/kinetics/fluid_tank.json",
      mech(["CCCCC", "CGGGC", "CGGGC", "CGGGC", "CCCCC"], {"C": _CS, "G": "minecraft:glass"}, "create:fluid_tank"))
# Copper (fluid) Pipe: additive 4x4 mech variant
write(f"{R}/mechanical/kinetics/fluid_pipe_mecha.json",
      mech(_p(["CNNC", "INNI", "INNI", "CNNC"]), {"C": _CS, "I": "minecraft:copper_ingot"}, "create:fluid_pipe"))
# Item Drain: 3x2 table + 5x3 mech
write("data/create/recipe/crafting/kinetics/item_drain.json",
      shaped(["CBC", "CDC"], {"C": _CS, "B": "minecraft:iron_bars", "D": _COC}, "create:item_drain"))
write(f"{R}/mechanical/kinetics/item_drain.json",
      mech(_p(["CBBBC", "CNNNC", "DDDDD"]), {"C": _CS, "B": "minecraft:iron_bars", "D": _COC}, "create:item_drain"))

# --- Precision Mechanism: override Create's sequenced assembly with a brass-sheet input and a
#     Lightning-Infused Iron Nugget for the third deploy; keep the other steps and output
#     chances. ---
_INC = "create:incomplete_precision_mechanism"
def _deploy(item):
    return {"type": "create:deploying", "ingredients": [{"item": _INC}, {"item": item}], "results": [{"id": _INC}]}
write("data/create/recipe/sequenced_assembly/precision_mechanism.json", {
    "neoforge:conditions": conds("create", "elemental_metals"),
    "type": "create:sequenced_assembly",
    "ingredient": {"item": "create:brass_sheet"},
    "loops": 5,
    "results": [
        {"chance": 120.0, "id": "create:precision_mechanism"},
        {"chance": 8.0, "id": "create:golden_sheet"},
        {"chance": 8.0, "id": "create:andesite_alloy"},
        {"chance": 5.0, "id": "create:cogwheel"},
        {"chance": 3.0, "id": "minecraft:gold_nugget"},
        {"chance": 2.0, "id": "create:shaft"},
        {"chance": 2.0, "id": "create:crushed_raw_gold"},
        {"id": "minecraft:iron_ingot"},
        {"id": "minecraft:clock"},
    ],
    "sequence": [_deploy("create:cogwheel"), _deploy("create:large_cogwheel"),
                 _deploy("elemental_metals:lightning_infused_iron_nugget")],
    "transitional_item": {"id": _INC},
})

# --- Mechanical Arm: the 3x3 overrides Create by replacing andesite alloy with a brass hand;
#     the 5x5 mechanical recipe is additive and uses brass sheets. ---
write("data/create/recipe/crafting/kinetics/mechanical_arm.json",
      shaped(["LLA", "L  ", "IC "],
             {"L": "#c:plates/brass", "A": "create:brass_hand",
              "I": "create:precision_mechanism", "C": "create:brass_casing"}, "create:mechanical_arm"))
write(f"{R}/mechanical/kinetics/mechanical_arm.json",
      mech(_p(["NNSSS", "NNSNH", "NSSNN", "NSPSN", "BBBBB"]),
           {"S": _BS, "H": "create:brass_hand", "P": "create:precision_mechanism", "B": "create:brass_casing"},
           "create:mechanical_arm"))

# --- Structural Beam and Water Wheel sequenced assemblies. Each has its own registered transitional
#     item and runs one loop. ---
def _seq_assembly(path, transitional, ingredient, steps, results, mods=(), loops=1):
    def _step(s):
        kind = s[0]
        if kind == "deploy":
            return {"type": "create:deploying", "ingredients": [{"item": transitional}, {"item": s[1]}], "results": [{"id": transitional}]}
        if kind == "press":
            return {"type": "create:pressing", "ingredients": [{"item": transitional}], "results": [{"id": transitional}]}
        if kind == "saw":
            return {"type": "create:cutting", "ingredients": [{"item": transitional}], "results": [{"id": transitional}]}
        if kind == "fill":
            return {"type": "create:filling", "ingredients": [{"item": transitional}, {"type": "neoforge:single", "amount": s[2], "fluid": s[1]}], "results": [{"id": transitional}]}
        raise ValueError(kind)
    write(path, {"neoforge:conditions": conds("create", *mods), "type": "create:sequenced_assembly",
                 "ingredient": {"item": ingredient}, "loops": loops, "results": results,
                 "sequence": [_step(s) for s in steps], "transitional_item": {"id": transitional}})

# Structural Beam: shaft -> 16-step sequence -> 70% x1 / 30% x2.
_seq_assembly(f"{R}/create/structural_beam_assembly.json", "bertieprogression:incomplete_structural_beam", "create:shaft",
              [("deploy", "create:brass_nugget"), ("deploy", "create:brass_nugget"), ("press",),
               ("deploy", "minecraft:vine"), ("deploy", "malum:earthen_spirit"),
               ("deploy", "minecraft:armadillo_scute"), ("deploy", "minecraft:armadillo_scute"),
               ("deploy", "minecraft:armadillo_scute"), ("deploy", "minecraft:armadillo_scute"), ("press",),
               ("fill", "slag:molten_quartz", 144),
               ("deploy", "create:copper_sheet"), ("deploy", "create:copper_sheet"), ("deploy", "create:copper_sheet"),
               ("deploy", "born_in_chaos_v1:diamond_termite_shard"), ("saw",)],
              [{"chance": 0.7, "id": "bertieprogression:kinetic_vane", "count": 1},
               {"chance": 0.3, "id": "bertieprogression:kinetic_vane", "count": 2}],
              mods=("malum", "slag", "born_in_chaos_v1"))
# Small Water Wheel: bound soul ingot + 8x deploy structural beam.
_seq_assembly(f"{R}/create/small_water_wheel_assembly.json", "bertieprogression:incomplete_small_water_wheel",
              "mythsandlegends:bound_soul_ingot", [("deploy", "bertieprogression:kinetic_vane")] * 8,
              [{"id": "create:water_wheel", "count": 1}], mods=("mythsandlegends",))
# Diamond: a sequenced assembly. Create's transitional item is a real registered item, so this one
# needs its own; it reuses a vanilla diamond model rather than new art.
_seq_assembly(f"{R}/create/diamond_backpack_assembly.json",
              "bertieprogression:incomplete_diamond_backpack",
              "sophisticatedbackpacks:gold_backpack",
              [("deploy", "minecraft:diamond_block")] * 4
              + [("fill", "slag:molten_diamond", 2592), ("press",)]
              + [("deploy", "born_in_chaos_v1:diamond_termite_shard")] * 4,
              [{"id": "sophisticatedbackpacks:diamond_backpack", "count": 1}],
              mods=("sophisticatedbackpacks", "slag", "born_in_chaos_v1"))
write("data/sophisticatedbackpacks/recipe/diamond_backpack.json", DISABLED)

# Large Water Wheel: small water wheel + 8x deploy structural beam.
_seq_assembly(f"{R}/create/large_water_wheel_assembly.json", "bertieprogression:incomplete_large_water_wheel",
              "create:water_wheel", [("deploy", "bertieprogression:kinetic_vane")] * 8,
              [{"id": "create:large_water_wheel", "count": 1}])

# --- Shield Maiden: eight-pedestal Hephaestus ritual on a Naga Trophy that grants Lich access. ---
write(f"{RIT}/shield_maiden.json",
      ritual("twilightforest:naga_trophy",
             [("twilightforest:firefly_jar", 2), ("create:precision_mechanism", 1),
              ("mythsandlegends:bound_soul_ingot", 1), ("iceandfire:hippocampus_fin", 1),
              ("born_in_chaos_v1:fangofthe_hound_leader", 1), ("born_in_chaos_v1:nightmare_claw", 1),
              ("born_in_chaos_v1:permafrost_shard", 1)],
             "bertieprogression:shield_maiden", 1, tier=1,
             essences={"aureal": 100, "blood": 3000, "souls": 10}, xp=100))

# --- Naga Trophy duplication: Spirit Infusion consumes one trophy and produces two. Extra inputs
#     stay in display order because EMI lays them out in list order. ---
write(f"{R}/malum/naga_trophy_dupe.json",
      infusion("twilightforest:naga_trophy", 1,
               [("born_in_chaos_v1:ethereal_spirit", 2), ("minecraft:cactus", 12),
                ("iceandfire:sea_serpent_scales_green", 3), ("malum:cthonic_gold", 1),
                ("l2complements:totemic_gold_nugget", 3)],
               [SP("wicked", 6), SP("eldritch", 6), SP("aerial", 6), SP("earthen", 6)],
               "twilightforest:naga_trophy", 2))

# --- Mason Jar: mechanical-crafting version of Twilight Forest's log-ring recipe, yielding
#     four. ---
write(f"{R}/mechanical/kinetics/mason_jar.json",
      mech(_p(["GWG", "GNG", "GGG"]), {"G": "minecraft:glass", "W": "twilightforest:twilight_oak_log"},
           "twilightforest:mason_jar", 4))

# --- Water Wheels: the sequenced assemblies are the sole route, so disable Create's defaults.
#     (The r14 Hephaestus ritual is also removed above.) ---
write("data/create/recipe/crafting/kinetics/water_wheel.json", DISABLED)
write("data/create/recipe/crafting/kinetics/large_water_wheel.json", DISABLED)

# --- Colossal Iron Ingot: Create compacting (press over basin), 16 iron ingots + 1000 mb Common Ink.
#     Fluid ingredient format verified from Create's diorite_from_flint compacting recipe. ---
write(f"{R}/create/colossal_iron_compacting.json",
      {"neoforge:conditions": conds("create", "armageddon_mod", "irons_spellbooks"),
       "type": "create:compacting",
       "heat_requirement": "heated",
       "ingredients": ([{"item": "minecraft:iron_ingot"} for _ in range(16)]
                       + [{"type": "neoforge:single", "amount": 1000, "fluid": "irons_spellbooks:common_ink"}]),
       "results": [{"id": "armageddon_mod:colossal_iron_ingot"}]})

# --- Zardius Crucible: a heated basin under a mixer, charged with 16 arcane spirits, 8 fluorite,
#     4 mundabitur dust and a bucket of lava. The declared result is the crucible, but
#     CrucibleTransmutation intercepts the craft and turns the BASIN into one, dropping the mixer.
#     Magitech's own bench recipe stays; this is the second route. ---
# Eye of Monstrous: Cataclysm's stock 3x3 goes; the same idea becomes a Tier-I ritual with an
# Ender Eye at the core and the eight pedestals filled exactly.
write("data/cataclysm/recipe/monstrous_eye.json", DISABLED)
write(f"{RIT}/monstrous_eye.json",
      ritual("minecraft:ender_eye",
             [("minecraft:netherite_ingot", 2), ("minecraft:gilded_blackstone", 2),
              ("alexsmobs:lava_bottle", 4)],
             "cataclysm:monstrous_eye", 1, tier=1))

# Onyx Shard: Pastel's midnight new-moon fusion, but bathed in Molten Netherite rather than lava
# and asking for four more things on the shrine.
write("data/pastel/recipe/fusion_shrine/onyx_shard.json",
      {"neoforge:conditions": conds("pastel", "slag", "malum", "deepwaters"),
       "type": "pastel:fusion_shrine",
       "time": 480,
       "experience": 2.0,
       "fluid": {"fluid": "slag:molten_netherite"},
       "ingredients": ["pastel:topaz_shard", "minecraft:amethyst_shard", "pastel:citrine_shard",
                       "pastel:shimmerstone_gem", "malum:mnemonic_fragment",
                       "deepwaters:aquamarine", "minecraft:prismarine_shard"],
       "result": {"id": "pastel:onyx_shard"},
       "required_advancement": "pastel:unlocks/blocks/fusion_shrine",
       "world_conditions": {"time_of_day": "midnight", "moon_phase": "new_moon"},
       "start_crafting_effect": "weather_thunder_short",
       "during_crafting_effects": ["visual_explosions_on_shrine", "nothing",
                                   "visual_explosions_on_shrine"],
       "finish_crafting_effect": "lightning_on_shrine",
       "description": {"type": "translatable",
                       "translate": "pastel.recipe.fusion_shrine.explanation.onyx_shard"}})

# Bottle of Fading: Pastel's own pedestal recipe, with the Honey Bottle swapped for an Ominous
# Bottle and the Blaze Powder either side for Rotting Essence. Now costs pigment as well.
_fading = pedestal(["FSF", "RBR", "FSF"],
                   {"F": "minecraft:fermented_spider_eye", "S": "pastel:shimmerstone_gem",
                    "R": "malum:rotting_essence", "B": "minecraft:ominous_bottle"},
                   {"pastel:cyan": 3, "pastel:magenta": 3, "pastel:yellow": 3,
                    "pastel:black": 3, "pastel:white": 3},
                   "pastel:bottle_of_fading", 1, tier="basic", time=400, xp=2.0)
_fading["skip_recipe_remainders"] = True
_fading["required_advancement"] = "pastel:unlocks/items/bottle_of_fading"
write("data/pastel/recipe/pedestal/tier1/bottle_of_fading.json", _fading)

# Fermented Spider Eye: the vanilla eye-sugar-wart shapeless goes; it is a full 3x3 now, and the
# Mechanical Mixer will churn one out of the same parts in water.
write("data/minecraft/recipe/fermented_spider_eye.json",
      shaped(["NMN", "SES", "NMN"],
             {"N": "minecraft:nether_wart", "M": "#c:mushrooms",
              "S": "minecraft:sugar", "E": "minecraft:spider_eye"},
             "minecraft:fermented_spider_eye", 1))
write(f"{R}/create/fermented_spider_eye_mixing.json",
      {"neoforge:conditions": conds("create"),
       "type": "create:mixing",
       "ingredients": [{"item": "minecraft:spider_eye"},
                       {"tag": "c:mushrooms"},
                       {"item": "minecraft:sugar"}, {"item": "minecraft:sugar"},
                       {"item": "minecraft:nether_wart"}, {"item": "minecraft:nether_wart"},
                       {"type": "neoforge:single", "amount": 1000, "fluid": "minecraft:water"}],
       "results": [{"id": "minecraft:fermented_spider_eye"}]})

# Deep Waters Key on the Sculk Crafting Table (Avaritia tier 1, a 3x3): a Nautilus Shell core,
# Deep Alloy corners, Sea Serpent Fangs either side, Mermaid's Gems above and below.
write(f"{R}/avaritia/deep_waters_key.json",
      {"neoforge:conditions": conds("avaritia", "slag", "pastel", "iceandfire", "deepwaters"),
       "type": "avaritia:shaped_table",
       "tier": 1,
       "key": {"D": {"item": "slag:deep_alloy"},
               "M": {"item": "pastel:mermaids_gem"},
               "F": {"item": "iceandfire:sea_serpent_fang"},
               "N": {"item": "minecraft:nautilus_shell"}},
       "pattern": ["DMD", "FNF", "DMD"],
       "result": {"count": 1, "id": "deepwaters:endlesscaves"}})

write(f"{R}/create/zardius_crucible_mixing.json",
      {"neoforge:conditions": conds("create", "magitech", "malum", "forbidden_arcanus"),
       "type": "create:mixing",
       "heat_requirement": "heated",
       "ingredients": ([{"item": "malum:arcane_spirit"} for _ in range(16)]
                       + [{"item": "magitech:fluorite"} for _ in range(8)]
                       + [{"item": "forbidden_arcanus:mundabitur_dust"} for _ in range(4)]
                       + [{"type": "neoforge:single", "amount": 1000, "fluid": "minecraft:lava"}]),
       "results": [{"id": "magitech:zardius_crucible"}]})

# --- Magitech's two early benches come down to a 2x2, so the tool line opens before a crafting
#     table exists: flint over logs assembles, andesite over logs upgrades. ---
write("data/magitech/recipe/assembly_workbench.json",
      shaped(["FF", "LL"], {"F": "minecraft:flint", "L": "#minecraft:logs"},
             "magitech:assembly_workbench"))
write("data/magitech/recipe/upgrade_workbench.json",
      shaped(["AA", "LL"], {"A": "minecraft:andesite", "L": "#minecraft:logs"},
             "magitech:upgrade_workbench"))


# --- Scroll Forge: Iron's own crafting-table recipe is replaced by a 5x5 mechanical craft, so the
#     forge lands after Create rather than beside it. Lacrima and Crying Obsidian over a course of
#     Deep Alloy blocks. ---
write("data/irons_spellbooks/recipe/scroll_forge.json", DISABLED)
write("data/bertieprogression/recipe/scroll_forge.json",
      {"neoforge:conditions": conds("create", "irons_spellbooks", "cataclysm", "slag"),
       "type": "create:mechanical_crafting",
       "pattern": ["LLLLL", "DDDDD", "CCCCC", "LLCLL", "LCCCL"],
       "key": {"L": {"item": "cataclysm:lacrima"},
               "D": {"item": "slag:deep_alloy_block"},
               "C": {"item": "minecraft:crying_obsidian"}},
       "result": {"id": "irons_spellbooks:scroll_forge", "count": 1},
       "category": "misc",
       # Create spells this one snake_case; the camelCase spelling parses as an unknown field and
       # silently throws the whole recipe away, which is how the forge ended up with no recipe.
       "accept_mirrored": False})

# --- Curios slots, rebuilt. Three things were wrong at once. Kaleidoscope Doll had taken the head
#     slot for itself by redeclaring it with a doll-only validator, which silently made every hat,
#     circlet and pair of goggles in the pack unequippable - Curios REPLACES a slot's validator set
#     rather than merging it, so the last mod to declare one wins outright. "trinket", "sheath" and
#     "accessory" duplicated slots that already existed. And the head tag had filled up with
#     Twilight Forest trophies and AnvilCraft hammers, which stay wearable through vanilla's own
#     helmet slot and do not need a curio slot as well. The doll now has `hat` to itself. ---
write("data/bertieprogression/curios/entities/player.json",
      {"replace": False, "entities": ["minecraft:player"],
       "slots": ["back", "belt", "cape", "charm", "hand", "hat", "head", "necklace", "pandora", "ring", "shoes", "wing", "wrist"]})

# Every namespace that already declares one of these slots. Curios applies slot files in
# namespace order, so writing only our own copy loses to any mod whose id sorts later -
# `kaleidoscope_doll` beat `bertieprogression` at head, which is what made every hat unequippable.
# Writing over each declaring mod's own path is what the AFTER ordering in neoforge.mods.toml
# actually governs, so the definition below is the one that survives whichever sorts last.
_SLOT_OWNERS = {"accessory": ["terra_curio"], "amulet": ["enigmaticlegacyplus"], "artifact": ["discerning_the_eldritch"], "back": ["curios", "enigmaticlegacyplus"], "belt": ["artifacts", "curios", "magitech", "malum"], "belt_slot": ["mythsandlegends"], "body": ["curios"], "body_slot": ["mythsandlegends"], "bracelet": ["curios"], "brooch": ["malum"], "charm": ["curios", "enderio", "enigmaticlegacyplus", "iceandfire", "magic_coins", "malum"], "cosmetic": ["hazennstuff"], "crystal": ["hazennstuff"], "curio": ["curios", "curseofpandora"], "deep_learner": ["hostilenetworks"], "face": ["pastel"], "feet": ["artifacts", "cataclysm", "pastel"], "geas": ["malum"], "hands": ["artifacts", "cataclysm", "curios"], "head": ["artifacts", "curios", "kaleidoscope_doll"], "hooked_hook": ["hooked"], "hostility_curse": ["l2hostility"], "necklace": ["artifacts", "cognition", "curios", "irons_spellbooks", "malum"], "necklace_slot": ["mythsandlegends"], "pandora": ["pandora"], "pigment_palette": ["pastel"], "pin": ["pastel"], "psd": ["compactmachines"], "qio": ["mekanismcurios"], "refinedstorage_curios_integration": ["refinedstorage_curios_integration"], "ring": ["curios", "enigmaticlegacyplus", "irons_spellbooks", "magitech", "malum", "pastel"], "ring_slot": ["mythsandlegends"], "rings": ["cataclysm"], "rune": ["malum"], "scroll": ["enigmaticlegacyplus"], "sheath": ["aces_spell_utils"], "spellbook": ["irons_spellbooks"], "spellstone": ["enigmaticlegacyplus"], "talisman": ["cataclysm"], "teleporter": ["mekanismcurios"], "threadbound": ["magitech"], "trinket": ["nameless_trinkets"], "waist": ["cataclysm"], "well": ["malstone"], "wing": ["hazennstuff", "hazentouvelib"]}


def _slot_write(slot, data):
    for _ns in _SLOT_OWNERS.get(slot, []) + ["bertieprogression"]:
        write(f"data/{_ns}/curios/slots/{slot}.json", data)


_ORDER = ["hat", "head", "necklace", "back", "cape", "wing", "belt", "hand", "wrist", "ring", "shoes", "spellbook", "spellstone", "scroll", "amulet", "crystal", "rune", "brooch", "well", "pin", "pigment_palette", "hooked_hook", "refinedstorage_curios_integration", "deep_learner", "psd", "qio", "teleporter"]
_SIZED = {"back": 1, "belt": 1, "cape": 1, "charm": 12, "hand": 2, "hat": 1, "head": 1, "necklace": 1, "pandora": 2, "ring": 2, "shoes": 1, "wing": 1, "wrist": 2}
for _slot, _size in sorted(_SIZED.items()):
    _d = {"operation": "SET", "size": _size, "validators": ["curios:tag"]}
    if _slot in _ORDER:                      # slots berlord did not place keep the order they had
        _d["order"] = (_ORDER.index(_slot) + 1) * 10
    if _slot == "hat":                       # the doll's own slot, and only the doll's
        _d["validators"] = ["kaleidoscope_doll:doll_item"]
    _slot_write(_slot, _d)

_CLOSED = ["trinket", "sheath", "accessory", "talisman", "rings", "waist",
           "ionocraft_backpack"]
for _slot in _CLOSED:
    _slot_write(_slot, {"operation": "SET", "size": 0})
    write(f"data/curios/tags/item/{_slot}.json", {"replace": True, "values": []})

# the tail of the list - the machine slots - only need their order changed
for _slot in _ORDER:
    if _slot not in _SIZED and _slot not in _CLOSED:
        _slot_write(_slot, {"order": (_ORDER.index(_slot) + 1) * 10})

write("data/curios/tags/item/belt.json",
      {"replace": False, "values": [
        {"id": "terra_curio:black_belt", "required": False},
        {"id": "terra_curio:inner_tube", "required": False},
        {"id": "terra_curio:toolbelt", "required": False},
    ]})
write("data/curios/tags/item/cape.json",
      {"replace": False, "values": [
        {"id": "armageddon_mod:cloak_of_the_abyss", "required": False},
        {"id": "armageddon_mod:hunter_cloak", "required": False},
        {"id": "l2hostility:triple_strip_cape", "required": False},
        {"id": "terra_curio:bee_cloak", "required": False},
        {"id": "terra_curio:star_cloak", "required": False},
    ]})
write("data/curios/tags/item/face.json",
      {"replace": False, "values": [
        {"id": "armageddon_mod:blindfold", "required": False},
        {"id": "terra_curio:blindfold", "required": False},
        {"id": "terra_curio:diving_gear", "required": False},
    ]})
write("data/curios/tags/item/hand.json",
      {"replace": False, "values": [
        {"id": "armageddon_mod:anathema_gauntlet", "required": False},
        {"id": "armageddon_mod:cinder_hand", "required": False},
        {"id": "armageddon_mod:hand_of_the_storms", "required": False},
        {"id": "armageddon_mod:hand_of_the_weaks", "required": False},
        {"id": "armageddon_mod:poisonous_hand", "required": False},
        {"id": "armageddon_mod:zoranths_hand", "required": False},
        {"id": "artifacts:feral_claws", "required": False},
        {"id": "terra_curio:bone_glove", "required": False},
        {"id": "terra_curio:feral_claws", "required": False},
        {"id": "terra_curio:hand_of_creation", "required": False},
        {"id": "terra_curio:hand_warmer", "required": False},
        {"id": "terra_curio:titan_glove", "required": False},
    ]})
# The two Pandora holders are curios in their own right, and were sitting in the necklace and
# bracelet slots as well as holding charms. They belong in the Pandora Charm slot alone, so
# both tags are rewritten without them.
write("data/curios/tags/item/necklace.json",
      {"replace": True, "values": [
        {"id": "aces_spell_utils:example_curio", "required": False},
        {"id": "aces_spell_utils:example_imbue_curio", "required": False},
        {"id": "armageddon_mod:life_necklace", "required": False},
        {"id": "armageddon_mod:pendant_of_convergence", "required": False},
        {"id": "artifacts:charm_of_shrinking", "required": False},
        {"id": "artifacts:charm_of_sinking", "required": False},
        {"id": "artifacts:cross_necklace", "required": False},
        {"id": "artifacts:flame_pendant", "required": False},
        {"id": "artifacts:lucky_scarf", "required": False},
        {"id": "artifacts:panic_necklace", "required": False},
        {"id": "artifacts:scarf_of_invisibility", "required": False},
        {"id": "artifacts:shock_pendant", "required": False},
        {"id": "artifacts:thorn_pendant", "required": False},
        {"id": "cataclysm:berserker_soul_amulet", "required": False},
        {"id": "cataclysm:vitality_ankh", "required": False},
        {"id": "cognition:enlightened_amulet", "required": False},
        {"id": "cognition:fortuitous_amulet", "required": False},
        {"id": "darkdoppelganger:elder_necklace", "required": False},
        {"id": "darkdoppelganger:summons_necklace", "required": False},
        {"id": "discerning_the_eldritch:amulet_of_sculk_treasure", "required": False},
        {"id": "hazennstuff:the_tribunes_medallion", "required": False},
        {"id": "irons_spellbooks:amethyst_resonance_charm", "required": False},
        {"id": "irons_spellbooks:concentration_amulet", "required": False},
        {"id": "irons_spellbooks:conjurers_talisman", "required": False},
        {"id": "irons_spellbooks:enchanted_ward_amulet", "required": False},
        {"id": "irons_spellbooks:greater_conjurers_talisman", "required": False},
        {"id": "irons_spellbooks:heavy_chain_necklace", "required": False},
        {"id": "irons_spellbooks:teleportation_amulet", "required": False},
        {"id": "malstone:breaking_the_life", "required": False},
        {"id": "malstone:falling_well", "required": False},
        {"id": "malstone:white_arrow_blade", "required": False},
        {"id": "malum:necklace_of_blissful_harmony", "required": False},
        {"id": "malum:necklace_of_the_hidden_blade", "required": False},
        {"id": "malum:necklace_of_the_mystic_mirror", "required": False},
        {"id": "malum:necklace_of_the_narrow_edge", "required": False},
        {"id": "malum:necklace_of_the_watcher", "required": False},
        {"id": "malum:ornate_necklace", "required": False},
        {"id": "mythsandlegends:necklace_of_torngarsuk", "required": False},
        {"id": "nameless_trinkets:broken_ankh", "required": False},
        {"id": "nameless_trinkets:gills", "required": False},
        {"id": "nameless_trinkets:moon_stone", "required": False},
        {"id": "nameless_trinkets:resonant_heart", "required": False},
        {"id": "nameless_trinkets:scarab_amulet", "required": False},
        {"id": "nameless_trinkets:woundbearer", "required": False},
        {"id": "pastel:greater_potion_pendant", "required": False},
        {"id": "pastel:laurels_of_serenity", "required": False},
        {"id": "pastel:lesser_potion_pendant", "required": False},
        {"id": "pastel:shieldgrasp_amulet", "required": False},
        {"id": "pastel:totem_pendant", "required": False},
        {"id": "terra_curio:cross_necklace", "required": False},
        {"id": "terra_curio:jellyfish_necklace", "required": False},
        {"id": "terra_curio:lava_charm", "required": False},
        {"id": "terra_curio:molten_charm", "required": False},
        {"id": "terra_curio:moon_stone", "required": False},
        {"id": "terra_curio:panic_necklace", "required": False},
        {"id": "terra_curio:star_veil", "required": False},
        {"id": "terra_curio:sun_stone", "required": False},
        {"id": "terra_curio:sweetheart_necklace", "required": False},
        {"id": "terra_curio:worm_scarf", "required": False},
    ]})
write("data/curios/tags/item/bracelet.json",
      {"replace": True, "values": []})
write("data/curios/tags/item/pandora.json",
      {"replace": False, "values": [
        {"id": "pandora:pandora_bracelet", "required": False},
        {"id": "pandora:pandora_necklace", "required": False},
    ]})
write("data/curios/tags/item/ring.json",
      {"replace": False, "values": [
        {"id": "armageddon_mod:iron_ring", "required": False},
        {"id": "armageddon_mod:rose_ring", "required": False},
        {"id": "enigmaticlegacyplus:iron_ring", "required": False},
        {"id": "hazennstuff:the_prefects_ring", "required": False},
    ]})
write("data/curios/tags/item/shoes.json",
      {"replace": True, "values": [
        {"id": "terra_curio:ambhipian_boots", "required": False},
        {"id": "terra_curio:dunerider_boots", "required": False},
        {"id": "terra_curio:fairy_boots", "required": False},
        {"id": "terra_curio:flipper", "required": False},
        {"id": "terra_curio:flower_boots", "required": False},
        {"id": "terra_curio:flurry_boots", "required": False},
        {"id": "terra_curio:frog_flipper", "required": False},
        {"id": "terra_curio:frostspark_boots", "required": False},
        {"id": "terra_curio:hermes_boots", "required": False},
        {"id": "terra_curio:ice_skates", "required": False},
        {"id": "terra_curio:lava_waders", "required": False},
        {"id": "terra_curio:lightning_boots", "required": False},
        {"id": "terra_curio:obsidian_water_walking_boots", "required": False},
        {"id": "terra_curio:rocket_boots", "required": False},
        {"id": "terra_curio:sailfish_boots", "required": False},
        {"id": "terra_curio:spectre_boots", "required": False},
        {"id": "terra_curio:tabi", "required": False},
        {"id": "terra_curio:terraspark_boots", "required": False},
        {"id": "terra_curio:water_walking_boots", "required": False},
    ]})
write("data/curios/tags/item/hands.json",
      {"replace": True, "values": [
        {"id": "artifacts:digging_claws", "required": False},
        {"id": "artifacts:fire_gauntlet", "required": False},
        {"id": "artifacts:golden_hook", "required": False},
        {"id": "artifacts:onion_ring", "required": False},
        {"id": "artifacts:pickaxe_heater", "required": False},
        {"id": "artifacts:pocket_piston", "required": False},
        {"id": "artifacts:power_glove", "required": False},
        {"id": "artifacts:vampiric_glove", "required": False},
        {"id": "artifacts:withered_bracelet", "required": False},
        {"id": "cataclysm:blazing_grips", "required": False},
        {"id": "cataclysm:chitin_claw", "required": False},
        {"id": "cataclysm:sticky_gloves", "required": False},
        {"id": "l2hostility:flaming_thorn", "required": False},
        {"id": "l2hostility:imagine_breaker", "required": False},
        {"id": "l2hostility:infinity_glove", "required": False},
        {"id": "l2hostility:platinum_star", "required": False},
        {"id": "pastel:aether_graced_nectar_gloves", "required": False},
        {"id": "pastel:gloves_of_dawns_grasp", "required": False},
    ]})
write("data/curios/tags/item/hat.json", {"replace": True, "values": []})
write("data/curios/tags/item/wing.json",
      {"replace": False, "values": [
        {"id": "alexsmobs:tarantula_hawk_elytra", "required": False},
        {"id": "anvilcraft:ionocraft_backpack", "required": False},
        {"id": "avaritia:infinity_elytra", "required": False},
        {"id": "crystalmod:black_tourmaline_elytra", "required": False},
        {"id": "crystalmod:sapphire_elytra", "required": False},
        {"id": "deeperdarker:soul_elytra", "required": False},
        {"id": "enderitemod:enderite_elytra_seperated", "required": False},
        {"id": "enigmaticlegacyplus:chaos_elytra", "required": False},
        {"id": "enigmaticlegacyplus:majestic_elytra", "required": False},
        {"id": "lilwings:aponi_elytra", "required": False},
        {"id": "lilwings:butter_gold_elytra", "required": False},
        {"id": "lilwings:cloudy_puff_elytra", "required": False},
        {"id": "lilwings:crystal_puff_elytra", "required": False},
        {"id": "lilwings:gold_applefly_elytra", "required": False},
        {"id": "lilwings:grayling_blooming_elytra", "required": False},
        {"id": "lilwings:grayling_elytra", "required": False},
        {"id": "lilwings:grayling_flowering_elytra", "required": False},
        {"id": "lilwings:painted_panther_elytra", "required": False},
        {"id": "lilwings:red_applefly_elytra", "required": False},
        {"id": "lilwings:shroom_skipper_elytra", "required": False},
        {"id": "lilwings:swallow_tail_elytra", "required": False},
        {"id": "lilwings:swamp_hopper_elytra", "required": False},
        {"id": "lilwings:white_fox_elytra", "required": False},
        {"id": "lolenderite:enderite_plated_elytra", "required": False},
        {"id": "mekanism:hdpe_elytra", "required": False},
        {"id": "minecraft:elytra", "required": False},
        {"id": "mna:spectral_elytra", "required": False},
        {"id": "mythicmetals:celestium_elytra", "required": False},
        {"id": "netherelytra:netherite_elytra", "required": False},
        {"id": "silentgear:elytra", "required": False},
        {"id": "wooden_elytra:wooden_elytra", "required": False},
    ]})
write("data/curios/tags/item/wrist.json",
      {"replace": False, "values": [
        {"id": "armageddon_mod:leather_bracer", "required": False},
    ]})
write("data/curios/tags/item/back.json",
      {"replace": True, "values": [
        {"id": "nameless_trinkets:explosion_proof_jacket", "required": False},
        {"id": "sophisticatedbackpacks:backpack", "required": False},
        {"id": "sophisticatedbackpacks:copper_backpack", "required": False},
        {"id": "sophisticatedbackpacks:diamond_backpack", "required": False},
        {"id": "sophisticatedbackpacks:gold_backpack", "required": False},
        {"id": "sophisticatedbackpacks:iron_backpack", "required": False},
        {"id": "sophisticatedbackpacks:netherite_backpack", "required": False},
    ]})
write("data/curios/tags/item/charm.json",
      {"replace": True, "values": [
        {"id": "anvilcraft:abnormal_amulet", "required": False},
        {"id": "anvilcraft:anvil_amulet", "required": False},
        {"id": "anvilcraft:cat_amulet", "required": False},
        {"id": "anvilcraft:cogwheel_amulet", "required": False},
        {"id": "anvilcraft:comrade_amulet", "required": False},
        {"id": "anvilcraft:dog_amulet", "required": False},
        {"id": "anvilcraft:emerald_amulet", "required": False},
        {"id": "anvilcraft:feather_amulet", "required": False},
        {"id": "anvilcraft:gem_amulet", "required": False},
        {"id": "anvilcraft:nature_amulet", "required": False},
        {"id": "anvilcraft:ruby_amulet", "required": False},
        {"id": "anvilcraft:sapphire_amulet", "required": False},
        {"id": "anvilcraft:silence_amulet", "required": False},
        {"id": "anvilcraft:topaz_amulet", "required": False},
        {"id": "apotheosis:potion_charm", "required": False},
        {"id": "armageddon_mod:abyssal_charm", "required": False},
        {"id": "armageddon_mod:ancient_builders_charm", "required": False},
        {"id": "armageddon_mod:anhk_aegis", "required": False},
        {"id": "armageddon_mod:ankh_tablet", "required": False},
        {"id": "armageddon_mod:antique_pendant", "required": False},
        {"id": "armageddon_mod:arion_heart", "required": False},
        {"id": "armageddon_mod:bee_stinger", "required": False},
        {"id": "armageddon_mod:bone_effigy", "required": False},
        {"id": "armageddon_mod:burst_ball", "required": False},
        {"id": "armageddon_mod:charm_of_the_sea", "required": False},
        {"id": "armageddon_mod:colossal_shield", "required": False},
        {"id": "armageddon_mod:colossal_ward", "required": False},
        {"id": "armageddon_mod:elvenite_bell", "required": False},
        {"id": "armageddon_mod:elvenite_burst_ball", "required": False},
        {"id": "armageddon_mod:endermen_totem", "required": False},
        {"id": "armageddon_mod:eye_of_darkness", "required": False},
        {"id": "armageddon_mod:fossilized_claw", "required": False},
        {"id": "armageddon_mod:frog_leg", "required": False},
        {"id": "armageddon_mod:gilded_shackles", "required": False},
        {"id": "armageddon_mod:glowbug_lantern", "required": False},
        {"id": "armageddon_mod:golden_spoon", "required": False},
        {"id": "armageddon_mod:guardians_crystal", "required": False},
        {"id": "armageddon_mod:moon_charm", "required": False},
        {"id": "armageddon_mod:necrotoxic_shell", "required": False},
        {"id": "armageddon_mod:netherbound_shackles", "required": False},
        {"id": "armageddon_mod:obsidian_skull", "required": False},
        {"id": "armageddon_mod:piglins_emblem", "required": False},
        {"id": "armageddon_mod:pocket_compass", "required": False},
        {"id": "armageddon_mod:protectors_core", "required": False},
        {"id": "armageddon_mod:rose_shield", "required": False},
        {"id": "armageddon_mod:sandstep_anklet", "required": False},
        {"id": "armageddon_mod:sculk_eye_gem", "required": False},
        {"id": "armageddon_mod:seal_of_cataclysm", "required": False},
        {"id": "armageddon_mod:spelunker_talisman", "required": False},
        {"id": "armageddon_mod:spoon_of_the_last_repast", "required": False},
        {"id": "armageddon_mod:the_heart", "required": False},
        {"id": "armageddon_mod:thorned_requital", "required": False},
        {"id": "armageddon_mod:vampiric_talisman", "required": False},
        {"id": "armageddon_mod:voidcall_ward", "required": False},
        {"id": "armageddon_mod:wither_spine", "required": False},
        {"id": "armageddon_mod:withered_bone_effigy", "required": False},
        {"id": "avaritia:infinity_totem", "required": False},
        {"id": "enderio:electromagnet", "required": False},
        {"id": "enderio:staff_of_travelling", "required": False},
        {"id": "enigmaticlegacyplus:berserk_emblem", "required": False},
        {"id": "enigmaticlegacyplus:enchanter_pearl", "required": False},
        {"id": "enigmaticlegacyplus:enigmatic_eye", "required": False},
        {"id": "enigmaticlegacyplus:ethereal_forging_charm", "required": False},
        {"id": "enigmaticlegacyplus:forger_gem", "required": False},
        {"id": "enigmaticlegacyplus:hell_blade_charm", "required": False},
        {"id": "enigmaticlegacyplus:mining_charm", "required": False},
        {"id": "enigmaticlegacyplus:monster_charm", "required": False},
        {"id": "enigmaticlegacyplus:scorched_charm", "required": False},
        {"id": "enigmaticlegacyplus:spelltuner", "required": False},
        {"id": "hazennstuff:blade_of_the_legate", "required": False},
        {"id": "hazennstuff:pendant_of_harmony", "required": False},
        {"id": "iceandfire:hydra_heart", "required": False},
        {"id": "l2complements:eternal_totem_of_dream", "required": False},
        {"id": "l2complements:totem_of_dream", "required": False},
        {"id": "l2complements:totem_of_the_sea", "required": False},
        {"id": "l2hostility:pocket_of_restoration", "required": False},
        {"id": "magic_coins:prosperity_amulet", "required": False},
        {"id": "malstone:evil_engine", "required": False},
        {"id": "malstone:huge_soul", "required": False},
        {"id": "malstone:soul_device", "required": False},
        {"id": "malstone:soul_steel_components", "required": False},
        {"id": "malum:token_of_gratitude", "required": False},
        {"id": "malum:tophat", "required": False},
        {"id": "nameless_trinkets:blaze_nucleus", "required": False},
        {"id": "nameless_trinkets:callus", "required": False},
        {"id": "nameless_trinkets:dark_nelumbo", "required": False},
        {"id": "nameless_trinkets:dying_star", "required": False},
        {"id": "nameless_trinkets:experience_battery", "required": False},
        {"id": "nameless_trinkets:fate_emerald", "required": False},
        {"id": "nameless_trinkets:fertilizer", "required": False},
        {"id": "nameless_trinkets:four_leaf_clover", "required": False},
        {"id": "nameless_trinkets:fragile_cloud", "required": False},
        {"id": "nameless_trinkets:ice_cube", "required": False},
        {"id": "nameless_trinkets:lucky_rock", "required": False},
        {"id": "nameless_trinkets:miners_soul", "required": False},
        {"id": "nameless_trinkets:missing_page", "required": False},
        {"id": "nameless_trinkets:nelumbo", "required": False},
        {"id": "nameless_trinkets:puffer_fish_liver", "required": False},
        {"id": "nameless_trinkets:reforger", "required": False},
        {"id": "nameless_trinkets:reverse_card", "required": False},
        {"id": "nameless_trinkets:sigil_of_baphomet", "required": False},
        {"id": "nameless_trinkets:sleeping_pills", "required": False},
        {"id": "nameless_trinkets:speed_force", "required": False},
        {"id": "nameless_trinkets:spider_legs", "required": False},
        {"id": "nameless_trinkets:tear_of_the_sea", "required": False},
        {"id": "nameless_trinkets:tick", "required": False},
        {"id": "nameless_trinkets:true_heart_of_the_sea", "required": False},
        {"id": "nameless_trinkets:vampire_blood", "required": False},
        {"id": "nameless_trinkets:wooden_stick", "required": False},
        {"id": "terra_curio:aglet", "required": False},
        {"id": "terra_curio:amber_horseshoe_balloon", "required": False},
        {"id": "terra_curio:ancient_chisel", "required": False},
        {"id": "terra_curio:angler_earring", "required": False},
        {"id": "terra_curio:ankh_charm", "required": False},
        {"id": "terra_curio:ankh_shield", "required": False},
        {"id": "terra_curio:anklet_of_the_wind", "required": False},
        {"id": "terra_curio:architect_gizmo_pack", "required": False},
        {"id": "terra_curio:avenger_emblem", "required": False},
        {"id": "terra_curio:balloon_pufferfish", "required": False},
        {"id": "terra_curio:band_of_regeneration", "required": False},
        {"id": "terra_curio:base_point", "required": False},
        {"id": "terra_curio:berserkers_glove", "required": False},
        {"id": "terra_curio:bezoar", "required": False},
        {"id": "terra_curio:blizzard_in_a_balloon", "required": False},
        {"id": "terra_curio:blizzard_in_a_bottle", "required": False},
        {"id": "terra_curio:blue_horseshoe_balloon", "required": False},
        {"id": "terra_curio:brain_of_confusion", "required": False},
        {"id": "terra_curio:brick_layer", "required": False},
        {"id": "terra_curio:bundle_of_balloons", "required": False},
        {"id": "terra_curio:bundle_of_horseshoe_balloons", "required": False},
        {"id": "terra_curio:celestial_shell", "required": False},
        {"id": "terra_curio:celestial_starboard", "required": False},
        {"id": "terra_curio:celestial_stone", "required": False},
        {"id": "terra_curio:climbing_claws", "required": False},
        {"id": "terra_curio:cloud_in_a_balloon", "required": False},
        {"id": "terra_curio:cloud_in_a_bottle", "required": False},
        {"id": "terra_curio:cobalt_shield", "required": False},
        {"id": "terra_curio:compass", "required": False},
        {"id": "terra_curio:copper_watch", "required": False},
        {"id": "terra_curio:depth_meter", "required": False},
        {"id": "terra_curio:destroyer_emblem", "required": False},
        {"id": "terra_curio:detoxification_capsule", "required": False},
        {"id": "terra_curio:dps_meter", "required": False},
        {"id": "terra_curio:energy_bar", "required": False},
        {"id": "terra_curio:everlasting", "required": False},
        {"id": "terra_curio:explorers_equipment", "required": False},
        {"id": "terra_curio:extendo_grip", "required": False},
        {"id": "terra_curio:eye_of_the_golem", "required": False},
        {"id": "terra_curio:fart_in_a_balloon", "required": False},
        {"id": "terra_curio:fart_in_a_jar", "required": False},
        {"id": "terra_curio:fast_clock", "required": False},
        {"id": "terra_curio:fire_gauntlet", "required": False},
        {"id": "terra_curio:fish_finder", "required": False},
        {"id": "terra_curio:fishermans_pocket_guide", "required": False},
        {"id": "terra_curio:flashlight", "required": False},
        {"id": "terra_curio:flesh_knuckles", "required": False},
        {"id": "terra_curio:flying_carpet", "required": False},
        {"id": "terra_curio:frog_gear", "required": False},
        {"id": "terra_curio:frog_leg", "required": False},
        {"id": "terra_curio:frog_webbing", "required": False},
        {"id": "terra_curio:frozen_shield", "required": False},
        {"id": "terra_curio:frozen_turtle_shell", "required": False},
        {"id": "terra_curio:goblin_tech", "required": False},
        {"id": "terra_curio:gold_watch", "required": False},
        {"id": "terra_curio:gps", "required": False},
        {"id": "terra_curio:gravity_globe", "required": False},
        {"id": "terra_curio:green_horseshoe_balloon", "required": False},
        {"id": "terra_curio:hand_drill", "required": False},
        {"id": "terra_curio:hero_shield", "required": False},
        {"id": "terra_curio:hive_pack", "required": False},
        {"id": "terra_curio:holy_water", "required": False},
        {"id": "terra_curio:honey_balloon", "required": False},
        {"id": "terra_curio:honey_comb", "required": False},
        {"id": "terra_curio:life_form_analyzer", "required": False},
        {"id": "terra_curio:lucky_horseshoe", "required": False},
        {"id": "terra_curio:magic_quiver", "required": False},
        {"id": "terra_curio:magiluminescence", "required": False},
        {"id": "terra_curio:magma_skull", "required": False},
        {"id": "terra_curio:magma_stone", "required": False},
        {"id": "terra_curio:master_ninja_gear", "required": False},
        {"id": "terra_curio:mechanical_glove", "required": False},
        {"id": "terra_curio:metal_detector", "required": False},
        {"id": "terra_curio:molten_quiver", "required": False},
        {"id": "terra_curio:molten_skull_rose", "required": False},
        {"id": "terra_curio:moon_charm", "required": False},
        {"id": "terra_curio:moon_shell", "required": False},
        {"id": "terra_curio:neptunes_shell", "required": False},
        {"id": "terra_curio:nutrient_solution", "required": False},
        {"id": "terra_curio:obsidian_horseshoe", "required": False},
        {"id": "terra_curio:obsidian_rose", "required": False},
        {"id": "terra_curio:obsidian_shield", "required": False},
        {"id": "terra_curio:obsidian_skull", "required": False},
        {"id": "terra_curio:obsidian_skull_rose", "required": False},
        {"id": "terra_curio:paladins_shield", "required": False},
        {"id": "terra_curio:pda", "required": False},
        {"id": "terra_curio:pink_horseshoe_balloon", "required": False},
        {"id": "terra_curio:platinum_watch", "required": False},
        {"id": "terra_curio:portable_cement_mixer", "required": False},
        {"id": "terra_curio:power_glove", "required": False},
        {"id": "terra_curio:putrid_scent", "required": False},
        {"id": "terra_curio:radar", "required": False},
        {"id": "terra_curio:ranger_emblem", "required": False},
        {"id": "terra_curio:recon_scope", "required": False},
        {"id": "terra_curio:rek_3000", "required": False},
        {"id": "terra_curio:rifle_scope", "required": False},
        {"id": "terra_curio:royal_gel", "required": False},
        {"id": "terra_curio:sandstorm_in_a_balloon", "required": False},
        {"id": "terra_curio:sandstorm_in_a_bottle", "required": False},
        {"id": "terra_curio:searchlight", "required": False},
        {"id": "terra_curio:sextant", "required": False},
        {"id": "terra_curio:shark_tooth_necklace", "required": False},
        {"id": "terra_curio:sharkron_balloon", "required": False},
        {"id": "terra_curio:shield_of_cthulhu", "required": False},
        {"id": "terra_curio:shiny_red_balloon", "required": False},
        {"id": "terra_curio:shiny_stone", "required": False},
        {"id": "terra_curio:shoe_spikes", "required": False},
        {"id": "terra_curio:shot_put", "required": False},
        {"id": "terra_curio:silver_watch", "required": False},
        {"id": "terra_curio:sniper_scope", "required": False},
        {"id": "terra_curio:soaring_insignia", "required": False},
        {"id": "terra_curio:sorcerer_emblem", "required": False},
        {"id": "terra_curio:stalkers_quiver", "required": False},
        {"id": "terra_curio:step_stool", "required": False},
        {"id": "terra_curio:stinger_necklace", "required": False},
        {"id": "terra_curio:stopwatch", "required": False},
        {"id": "terra_curio:tally_counter", "required": False},
        {"id": "terra_curio:the_plan", "required": False},
        {"id": "terra_curio:tiger_climbing_gear", "required": False},
        {"id": "terra_curio:tin_watch", "required": False},
        {"id": "terra_curio:toolbox", "required": False},
        {"id": "terra_curio:treasure_magnet", "required": False},
        {"id": "terra_curio:trifold_map", "required": False},
        {"id": "terra_curio:tsunami_in_a_bottle", "required": False},
        {"id": "terra_curio:tungsten_watch", "required": False},
        {"id": "terra_curio:vitamins", "required": False},
        {"id": "terra_curio:warrior_emblem", "required": False},
        {"id": "terra_curio:weather_radio", "required": False},
        {"id": "terra_curio:white_horseshoe_balloon", "required": False},
        {"id": "terra_curio:yellow_horseshoe_balloon", "required": False},
        {"id": "twilightforest:charm_of_keeping_1", "required": False},
        {"id": "twilightforest:charm_of_keeping_2", "required": False},
        {"id": "twilightforest:charm_of_keeping_3", "required": False},
        {"id": "twilightforest:charm_of_life_1", "required": False},
        {"id": "twilightforest:charm_of_life_2", "required": False},
    ]})
write("data/curios/tags/item/head.json",
      {"replace": True, "values": [
        {"id": "armageddon_mod:fisher_hat", "required": False},
        {"id": "armageddon_mod:vagabonds_hood", "required": False},
        {"id": "artifacts:anglers_hat", "required": False},
        {"id": "artifacts:cowboy_hat", "required": False},
        {"id": "artifacts:night_vision_goggles", "required": False},
        {"id": "artifacts:novelty_drinking_hat", "required": False},
        {"id": "artifacts:plastic_drinking_hat", "required": False},
        {"id": "artifacts:snorkel", "required": False},
        {"id": "artifacts:superstitious_hat", "required": False},
        {"id": "artifacts:villager_hat", "required": False},
        {"id": "cataclysm:aptrgangr_head", "required": False},
        {"id": "cataclysm:draugr_head", "required": False},
        {"id": "cataclysm:kobolediator_skull", "required": False},
        {"id": "create:goggles", "required": False},
        {"id": "l2hostility:detector_glasses", "required": False},
        {"id": "l2hostility:oddeyes_glasses", "required": False},
        {"id": "nameless_trinkets:cracked_crown", "required": False},
        {"id": "nameless_trinkets:gods_crown", "required": False},
        {"id": "pastel:ashen_circlet", "required": False},
        {"id": "pastel:circlet_of_arrogance", "required": False},
        {"id": "pastel:puff_circlet", "required": False},
        {"id": "pastel:weeping_circlet", "required": False},
        {"id": "pastel:whispy_circlet", "required": False},
        {"id": "terra_curio:arctic_diving_gear", "required": False},
        {"id": "terra_curio:jellyfish_diving_gear", "required": False},
    ]})

# Cataclysm hands out the slots it thinks it owns from its own entity file; keep the four that are
# real and let the file above declare the rest.
write("data/cataclysm/curios/entities/slots.json",
      {"entities": ["player"], "slots": ["head", "necklace", "hands", "feet"]})

# --- Clibano: stock FA "secondary output" is a residue whose combine_info.result was a block.
#     Change each shared residue type to produce the corresponding primary ingot or
#     item, required_amount 1 (recipe chances untouched). Each residue_type maps 1:1 to its primary, so
#     copper->copper_ingot, iron->iron_ingot, etc. CAVEAT (jar-decompiled): FA only pays residue on
#     SOUL/ENCHANTED fire; on PLAIN fire there is NO secondary at all — that gate needs a mixin. ---
def _residue(name, result_id, label):
    write(f"data/forbidden_arcanus/forbidden_arcanus/residue_type/{name}.json",
          {"combine_info": {"required_amount": 1, "result": {"count": 1, "id": result_id}},
           "name": {"text": label}})
_residue("copper", "minecraft:copper_ingot", "Copper Residue")
_residue("iron", "minecraft:iron_ingot", "Iron Residue")
_residue("gold", "minecraft:gold_ingot", "Gold Residue")
_residue("coal", "minecraft:coal", "Coal Residue")
_residue("diamond", "minecraft:diamond", "Diamond Residue")
_residue("emerald", "minecraft:emerald", "Emerald Residue")
_residue("lapis_lazuli", "minecraft:lapis_lazuli", "Lapis Residue")
_residue("netherite", "minecraft:netherite_scrap", "Netherite Residue")
_residue("rune", "forbidden_arcanus:rune", "Rune Residue")
_residue("arcane_crystal", "forbidden_arcanus:arcane_crystal", "Arcane Crystal Residue")

# Obsidiansteel clibano: raw iron -> Colossal Iron Ingot; drop the secondary entirely (no residue field).
write("data/forbidden_arcanus/recipe/clibano_combustion/obsidiansteel_ingot_from_clibano_combustion.json",
      {"neoforge:conditions": conds("forbidden_arcanus", "armageddon_mod"),
       "type": "forbidden_arcanus:clibano_combustion", "category": "misc",
       "cooking_time": 100, "enhancer": "forbidden_arcanus:artisan_relic", "experience": 0.5,
       "fire_type": "fire",
       "ingredients": {"first": {"item": "armageddon_mod:colossal_iron_ingot"},
                       "second": {"item": "minecraft:obsidian"}},
       "result": {"count": 1, "id": "forbidden_arcanus:obsidiansteel_ingot"}})

# Remove Malum's iron-to-soul-stained-steel Spirit Infusion route.
write("data/malum/recipe/spirit_infusion/soul_stained_steel_ingot.json", DISABLED)

# Hallowed Gold: replace Malum's default production (gold ingot + 4 quartz + spirits) with our brass
# Spirit-Infusion above. (Block/nugget round-trip recipes left intact.)
write("data/malum/recipe/spirit_infusion/hallowed_gold_ingot.json", DISABLED)

# Remove Iron's Spellbooks' 3x3 Arcane Ingot recipe (8 Arcane Essence
# around an arcane-ingot-base). Disabled; the deorum-core Spirit Infusion (arcane_ingot.json) remains.
write("data/irons_spellbooks/recipe/arcane_ingot.json", DISABLED)

# Deorum Ingot 3x3: override FA's #*#/MXM/#*# recipe with brass at the centre instead of gold.
write("data/forbidden_arcanus/recipe/deorum_ingot.json",
      shaped(["#*#", "MXM", "#*#"],
             {"#": "minecraft:charcoal", "*": "forbidden_arcanus:arcane_crystal_dust",
              "M": "forbidden_arcanus:mundabitur_dust", "X": "create:brass_ingot"},
             "forbidden_arcanus:deorum_ingot"))

# ================================================= Crafting License and trophy duplication

# --- Glass Bottle: 2 Glass double-smelted on the Brick Forge. ---
write(f"{R}/slag/glass_bottle.json",
      double_smelting("minecraft:glass", "minecraft:glass", "minecraft:glass_bottle", 1))

# --- THE CRAFTING LICENSE. Tier-I Hephaestus ritual:
#     forge_tier omitted = any tier, matching FA's own early rituals (ferrognetic_mixture etc.).
#     Chapter 2's finale, so it must be reachable on the T1 forge the player raised in Chapter 1.
#     forge-ink pays the 900-experience cost in common ink.
#     Exactly 8 inputs = exactly 8 pedestals (the ritual() assert is at its ceiling). ---
write(f"{RIT}/crafting_license.json",
      ritual("minecraft:crafting_table",
             [("twilightforest:exanimate_essence", 1), ("create:precision_mechanism", 1),
              ("mythsandlegends:bound_soul_ingot", 1), ("l2complements:totemic_gold_ingot", 1),
              ("create:electron_tube", 1), ("bertieprogression:kinetic_vane", 1),
              ("irons_spellbooks:blank_rune", 1), ("minecraft:writable_book", 1)],
             "bertieprogression:crafting_license", 1,
             essences={"aureal": 1000, "blood": 10000, "souls": 10}, xp=900))

# --- Lich Trophy duplication: Spirit Infusion consumes one trophy and produces two, using the same
#     structure as the Naga route. Extra inputs remain in EMI display order. ---
write(f"{R}/malum/lich_trophy_dupe.json",
      infusion("twilightforest:lich_trophy", 1,
               [("iceandfire:stymphalian_bird_feather", 2), ("born_in_chaos_v1:shattered_skull", 1),
                ("l2complements:totemic_gold_ingot", 1), ("mythsandlegends:bound_soul_ingot", 1),
                ("malum:mnemonic_fragment", 4)],
               [SP("wicked", 6), SP("eldritch", 6), SP("arcane", 6), SP("aerial", 6)],
               "twilightforest:lich_trophy", 2))

# ================================================================ Pre-table summons and materials

# --- Armageddon summons, ported to the Brick Forge so they are reachable PRE-TABLE. Both stock
#     recipes are 3x3 (table-gated) with these exact ingredients, so these are additive early routes,
#     compressed to the 2 ingredients slag:double_smelting allows. small_flowers = the stock tag. ---
write(f"{R}/slag/iron_remote.json",
      double_smelting("minecraft:iron_ingot", "minecraft:sunflower",
                      "armageddon_mod:iron_remote", 1))
write(f"{R}/slag/strange_coin.json",
      double_smelting("armageddon_mod:colossal_iron_ingot", "minecraft:gold_ingot",
                      "armageddon_mod:strange_coin", 1))

# Both stock 3x3 routes are disabled, so the two Brick Forge recipes above are
# the only way to either item. The Iron Remote also narrows from #minecraft:small_flowers to the
# Sunflower specifically - which is a TALL flower and was never in that tag, so this is a real
# tightening, not a restatement. armageddon_mod:infinite_iron_remote_recipe is a DIFFERENT item and
# is left alone. Needs the armageddon_mod ordering="AFTER" edge in neoforge.mods.toml.
write("data/armageddon_mod/recipe/iron_remote_recipe.json", DISABLED)
write("data/armageddon_mod/recipe/strange_coin_recipe.json", DISABLED)

# --- Refined Soulstone: a Brick-Forge alloy of one Diamond and one Raw Soulstone. ---
write(f"{R}/slag/refined_soulstone.json",
      double_smelting("minecraft:diamond", "malum:raw_soulstone", "malum:refined_soulstone", 1))

# --- Compass: the Mallet bed recipe is gone and Iron's Spellbooks' 3x3 Wayward Compass is disabled;
#     a Hephaestus ritual is the route now (Redstone core + 4 Iron on pedestals). ---
write("data/irons_spellbooks/recipe/wayward_compass.json", DISABLED)
write(f"{RIT}/compass.json",
      ritual("minecraft:redstone", [("minecraft:iron_ingot", 4)], "minecraft:compass", 1))

# --- Aureal Bottle: a Hephaestus ritual so Aureal can be bootstrapped WITHOUT already having Aureal —
#     hence no essence cost. Main ingredient is a real WATER BOTTLE, pinned with a neoforge:components
#     ingredient (Ritual.mainIngredient is a plain vanilla Ingredient, so custom types work; the same
#     shape already drives the slag-plate ritual inputs). 4+2+2 = 8 pedestals, at the ceiling. ---
write(f"{RIT}/aureal_bottle.json",
      {**ritual("minecraft:potion",
                [("forbidden_arcanus:arcane_crystal_dust", 4), ("minecraft:rotten_flesh", 2),
                 ("#bertieprogression:meat", 2)],
                "forbidden_arcanus:aureal_bottle", 1),
       "main_ingredient": {"type": "neoforge:components", "items": "minecraft:potion",
                           "components": {"minecraft:potion_contents": {"potion": "minecraft:water"}}}})

# --- Crude Scythe: Malum's own recipe removed; the Hephaestus reforge of the Dead King's Decrepit
#     Scythe (r_harvest ritual) is the sole route. ---
write("data/malum/recipe/crude_scythe.json", DISABLED)

# ================================================================ Malum recipe replacements
# Malum totemic branch + crucible line.

# --- Totemic Staff: a 2x2 replaces Malum's 3x3 diagonal. Grid is
#     N B / A N  (N = empty), B = runewood planks tag, A = FA edelwood stick. ---
write("data/malum/recipe/totemic_staff.json",
      shaped([" B", "A "], {"B": "#malum:runewood_planks", "A": "forbidden_arcanus:edelwood_stick"},
             "malum:totemic_staff"))

# --- Runewood Totem Base: Spirit Altar, runewood LOG core, one of every spirit, and
#     4 Hex Ash + 4 FA Runes on the pedestals (replaces Malum's 4-log/6-plank/2-ash version). ---
write("data/malum/recipe/spirit_infusion/runewood_totem_base.json",
      infusion("malum:runewood_log", 1,
               [("malum:hex_ash", 4), ("forbidden_arcanus:rune", 4)],
               [SP(s, 1) for s in ("aerial", "aqueous", "arcane", "earthen",
                                   "eldritch", "infernal", "sacred", "wicked")],
               "malum:runewood_totem_base", 1))

# --- Hex Ash: keep Malum's gunpowder + 1 arcane spirit; one charcoal fragment and one soulstone. ---
write("data/malum/recipe/spirit_infusion/hex_ash.json",
      infusion("minecraft:gunpowder", 1,
               [("forbidden_arcanus:arcane_crystal_dust_speck", 1),
                ("malum:arcane_charcoal_fragment", 1),
                ("malum:refined_soulstone", 1)],
               [SP("arcane", 1)], "malum:hex_ash", 1))

# --- Arcane Charcoal: one coal produces one charcoal using one infernal spirit. ---
write("data/malum/recipe/spirit_infusion/arcane_charcoal.json",
      infusion("#minecraft:coals", 1, [], [SP("infernal", 1)], "malum:arcane_charcoal", 1))

# --- Tainted / Twisted Rock: use a bulk 64 -> 8 route, adding an Earthen spirit to both and
#     raising every spirit count to 4. Each still
#     takes its own stone - Tainted from DIORITE, Twisted from GRANITE. ---
for _rock, _sp, _stone in (("tainted", "sacred", "minecraft:diorite"),
                           ("twisted", "wicked", "minecraft:granite")):
    write(f"data/malum/recipe/spirit_infusion/{_rock}_rock.json",
          infusion(_stone, 64, [], [SP(_sp, 4), SP("arcane", 4), SP("earthen", 4)],
                   f"malum:{_rock}_rock", 8))

# --- Spirit Jar: replace Hallowed Gold in Malum's 1x2 with a Create brass sheet. ---
write("data/malum/recipe/spirit_jar.json",
      shaped(["X", "Y"], {"X": "create:brass_sheet", "Y": "#c:glass_blocks"}, "malum:spirit_jar"))

# --- Alchemical Calx: replace the stock 4 clay -> 4 route with 6 clay -> 1 using four pedestal
#     inputs including Hex Ash; keep the original spirit costs. ---
write("data/malum/recipe/spirit_infusion/alchemical_calx.json",
      infusion("minecraft:clay_ball", 6,
               [("malum:grim_talc", 1), ("malum:hex_ash", 1), ("minecraft:bone", 5),
                ("#c:mushrooms", 3)],
               [SP("arcane", 2), SP("earthen", 2), SP("aqueous", 2)],
               "malum:alchemical_calx", 1))

# --- Alchemical Impetus: 4 Calx core, 4 Earthen + 4 Aerial, four 4x pedestal inputs, and the result
#     is handed over PRE-DAMAGED to 8 durability. Impetuses are 800 max (ImpetusItem ctor) and
#     spirit_infusion deserializes `result` with ItemStack.CODEC, so `components` works here -
#     see docs/malum-impetus-recipes.md. 800 - 8 = damage 792. ---
_impetus = infusion("malum:alchemical_calx", 4,
                    [("malum:hex_ash", 4), ("malum:raw_soulstone", 4),
                     ("malum:raw_brilliance", 4), ("malum:natural_quartz", 4)],
                    [SP("earthen", 4), SP("aerial", 4)], "malum:alchemical_impetus", 1)
_impetus["result"]["components"] = {"minecraft:damage": 792}
write("data/malum/recipe/spirit_infusion/alchemical_impetus.json", _impetus)

# --- Spirit Crucible focusing (malum:spirit_focusing). Stock ships a whole alchemical-impetus family
#     under data/MINECRAFT/recipe/spirit_crucible/ (amethyst, glowstone, gunpowder, prismarine,
#     quartz, redstone - all cost 1 / 300t); only the glowstone one is replaced here. ---
def focusing(input_item, result_id, count, spirits, time=600, cost=2):
    return {"neoforge:conditions": conds("malum"), "type": "malum:spirit_focusing",
            "input": {"item": input_item},
            "result": {"id": result_id, "count": count},
            "spirits": [{"type": t, "count": c} for t, c in spirits],
            "time": time, "durabilityCost": cost}

write("data/minecraft/recipe/spirit_crucible/glowstone_dust.json", DISABLED)   # stock: 1 infernal -> 8
write(f"{R}/malum/focusing_glowstone_dust.json",
      focusing("malum:alchemical_impetus", "minecraft:glowstone_dust", 4,
               [SP("infernal", 2), SP("earthen", 1)]))
write(f"{R}/malum/focusing_blaze_powder.json",
      focusing("malum:alchemical_impetus", "minecraft:blaze_powder", 2,
               [SP("infernal", 4), SP("aerial", 2)]))

# ================================================================ Cooking and plating replacements

# --- Refined Brilliance: Malum's raw-brilliance smelt AND blast both yield 2, duplicating our own
#     one-output cooking recipes in EMI. Disable the duplicate pair. The brilliant_stone and
#     crushed/deepslate variants are untouched because they take different inputs. ---
write("data/malum/recipe/brilliance_from_raw_blasting.json", DISABLED)
write("data/malum/recipe/brilliance_from_raw_smelting.json", DISABLED)

# --- Platings: Malum's 3x3 crafting recipes (which yield 2) are out; a Create press turns
#     1 ingot into 1 plating instead. ---
for _metal in ("soul_stained_steel", "malignant_pewter"):
    write(f"data/malum/recipe/{_metal}_plating.json", DISABLED)
    write(f"{R}/create/{_metal}_plating_pressing.json",
          {"neoforge:conditions": conds("create", "malum"), "type": "create:pressing",
           "ingredients": [{"item": f"malum:{_metal}_ingot"}],
           "results": [{"id": f"malum:{_metal}_plating"}]})

# ================================================================ Finder and boss-gate recipes

# --- Acolyte of Deflection: the Lich-Trophy counterpart of the Shield Maiden. Same shape as
#     shield_maiden.json above - a boss trophy is forged into the key that opens the next path,
#     so the trophy is spent rather than hoarded (and the Lich dupe below it pays for a second).
#     8 pedestals exactly (2 + 2 + 1 + 1 + 1 + 1). Forge T1, like the Shield Maiden now is.
#     "400 ink" = the xp field, paid in Iron's ink by forgeink (see crafting_license above).
#     Dragon Bone here is ICE AND FIRE's (iceandfire:dragonbone), NOT
#     block_factorys_bosses:dragon_bone - both display as "Dragon Bone". ---
write(f"{RIT}/acolyte_of_deflection.json",
      ritual("twilightforest:lich_trophy",
             [("iceandfire:dragonbone", 2), ("malum:hallowed_gold_ingot", 2),
              ("born_in_chaos_v1:shattered_skull", 1), ("create:precision_mechanism", 1),
              ("mythsandlegends:bound_soul_ingot", 1), ("twilightforest:magic_map_focus", 1)],
             "bertieprogression:acolyte_of_deflection", 1, tier=1,
             essences={"aureal": 100, "blood": 6000, "souls": 10}, xp=400))

# --- Deep Waters Key: replace the plain 3x3 with a Hephaestus ritual using eight pedestals
#     (4 + 2 + 2). The 200-experience cost is paid in ink through forge-ink.
#     forge_tier omitted = any tier: it is the ROOT of the C3 water path, so it must be reachable on
#     the T1 forge, same reasoning as crafting_license. ---
write(f"{RIT}/deepwaters_key.json",
      ritual("minecraft:nautilus_shell",
             [("iceandfire:sea_serpent_fang", 4), ("malum:warp_flux", 2), ("slag:deep_alloy", 2)],
             "deepwaters:endlesscaves", 1,
             essences={"aureal": 60, "blood": 0, "souls": 4}, xp=200))
write("data/deepwaters/recipe/hovaport.json", DISABLED)

# --- Crowned Jelly: replace the stock recipe's gold with Hallowed Gold and fill the four corners
#     with Flaming Opal. ---
write("data/deepwaters/recipe/howa_crow_j.json",
      shaped(["dad", "bcb", "dad"],
             {"a": "deepwaters:pearl", "b": "malum:hallowed_gold_ingot",
              "c": "deepwaters:medusabucket", "d": "deepwaters:fopal"},
             "deepwaters:crownedjelly"))

# --- Block of Flaming Opal (deepwaters:howafopalblock): the mod already trades 4 gems for the block,
#     but shapeless. Keep the explicit 2x2 shape. The same 4-in/1-out ratio means the mod's own
#     unblock recipe (1 block -> 4 gems) stays balanced and is left alone. ---
write("data/deepwaters/recipe/howafopalblock.json",
      shaped(["ff", "ff"], {"f": "deepwaters:fopal"}, "deepwaters:fopal_block"))

# --- Snow Queen Trophy duplication: one trophy produces two using the same structure as the Naga
#     and Lich routes. Extra inputs remain in EMI display order. ---
write(f"{R}/malum/snow_queen_trophy_dupe.json",
      infusion("twilightforest:snow_queen_trophy", 1,
               [("minecraft:bone", 12), ("minecraft:blue_ice", 1),
                ("minecraft:snow_block", 6), ("malum:wind_nucleus", 2),
                ("born_in_chaos_v1:phantom_powder", 3)],
               [SP("wicked", 6), SP("eldritch", 6), SP("aqueous", 6), SP("aerial", 6)],
               "twilightforest:snow_queen_trophy", 2))

# --- Sirok's Nest map. A finder item rather than a filled map:
#     a recipe result cannot be a structure map (Recipe.assemble gets no level/position), so the craft
#     yields bertieprogression:sirok_nest_map and FinderItem resolves it. See FinderItem's class comment.
#     The Gorgon Head is a CATALYST - matched, required, returned to the grid - which is why this uses
#     our own bertieprogression:catalyst_shaped serializer instead of minecraft:crafting_shaped. ---
# "any type of chitin works": Ice and Fire ships three and NO chitin tag exists in any pack jar
# (checked all 109), so we ship our own rather than trust a tag we do not control.
write("data/bertieprogression/tags/item/deathworm_chitin.json",
      {"values": ["iceandfire:deathworm_chitin_red", "iceandfire:deathworm_chitin_yellow",
                  "iceandfire:deathworm_chitin_white"]})

write(f"{R}/sirok_nest_map.json",
      {"neoforge:conditions": conds("iceandfire", "irons_spellbooks", "block_factorys_bosses"),
       "type": "bertieprogression:catalyst_shaped",
       "category": "misc",
       "key": {"c": {"tag": "bertieprogression:deathworm_chitin"},
               "g": {"item": "iceandfire:gorgon_head"},
               "i": {"item": "minecraft:glow_ink_sac"},
               "m": {"item": "minecraft:map"},
               "r": {"item": "irons_spellbooks:rare_ink"}},
       "pattern": ["cgc", "imi", "crc"],
       "result": {"id": "bertieprogression:sirok_nest_map", "count": 1},
       "catalyst": {"item": "iceandfire:gorgon_head"}})

# --- The four Cataclysm eyes: Hephaestus rituals replace Cataclysm's 3x3 crafts. Each uses a core
#     plus eight pedestals (2+2+2+2), and each core is the trophy or gauntlet the
#     boss at the end of that C3 row drops - so every row now spends its own kill.
#     These rituals intentionally have no essence cost or forge-tier restriction. ---
# "any coral - not block, not dead, not fan": #minecraft:coral_plants is exactly that set, but vanilla
# ships it only as a BLOCK tag (verified against the 1.21.1 client jar), so we ship the item tag.
write("data/bertieprogression/tags/item/corals.json",
      {"values": ["minecraft:tube_coral", "minecraft:brain_coral", "minecraft:bubble_coral",
                  "minecraft:fire_coral", "minecraft:horn_coral"]})

for _stock in ("desert_eye", "cursed_eye", "storm_eye", "abyss_eye"):
    write(f"data/cataclysm/recipe/{_stock}.json", DISABLED)

write(f"{RIT}/desert_eye.json",
      ritual("block_factorys_bosses:sandworm_gauntlet",
             [("malum:grim_talc", 2), ("minecraft:dead_bush", 2),
              ("malum:cthonic_gold", 2), ("minecraft:chiseled_sandstone", 2)],
             "cataclysm:desert_eye", 1))
write(f"{RIT}/cursed_eye.json",
      ritual("block_factorys_bosses:ice_gauntlet",
             [("twilightforest:alpha_yeti_fur", 2), ("minecraft:snowball", 2),
              ("iceandfire:ectoplasm", 2), ("minecraft:packed_ice", 2)],
             "cataclysm:cursed_eye", 1))
write(f"{RIT}/storm_eye.json",
      ritual("twilightforest:ur_ghast_trophy",
             [("twilightforest:knightmetal_ingot", 2), ("minecraft:phantom_membrane", 2),
              ("iceandfire:amphithere_feather", 2), ("minecraft:sea_lantern", 2)],
             "cataclysm:storm_eye", 1))
write(f"{RIT}/abyss_eye.json",
      ritual("deepwaters:blackpearl",
             [("block_factorys_bosses:kraken_tooth", 2), ("#bertieprogression:corals", 2),
              ("iceandfire:sea_serpent_fang", 2), ("minecraft:crying_obsidian", 2)],
             "cataclysm:abyss_eye", 1))

# --- The two remaining finders and the four elemental cores. ---
# Kraken map: Black Pearl is the catalyst. "any sea serpent goes" -> Ice and Fire's OWN tag, which
# already lists all seven scale colours; no bertie tag needed here.
write(f"{R}/kraken_ship_map.json",
      {"neoforge:conditions": conds("iceandfire", "irons_spellbooks", "deepwaters", "block_factorys_bosses"),
       "type": "bertieprogression:catalyst_shaped", "category": "misc",
       "key": {"s": {"tag": "iceandfire:scales/sea_serpent"},
               "p": {"item": "deepwaters:blackpearl"},
               "i": {"item": "minecraft:glow_ink_sac"},
               "m": {"item": "minecraft:map"},
               "r": {"item": "irons_spellbooks:rare_ink"}},
       "pattern": ["sps", "imi", "srs"],
       "result": {"id": "bertieprogression:kraken_ship_map", "count": 1},
       "catalyst": {"item": "deepwaters:blackpearl"}})

# Skor hideout map: Snow Queen Trophy is the catalyst (so the trophy is HELD, not spent).
write(f"{R}/yeti_hideout_map.json",
      {"neoforge:conditions": conds("twilightforest", "irons_spellbooks", "block_factorys_bosses"),
       "type": "bertieprogression:catalyst_shaped", "category": "misc",
       "key": {"f": {"item": "twilightforest:alpha_yeti_fur"},
               "q": {"item": "twilightforest:snow_queen_trophy"},
               "i": {"item": "minecraft:glow_ink_sac"},
               "m": {"item": "minecraft:map"},
               "r": {"item": "irons_spellbooks:rare_ink"}},
       "pattern": ["fqf", "imi", "frf"],
       "result": {"id": "bertieprogression:yeti_hideout_map", "count": 1},
       "catalyst": {"item": "twilightforest:snow_queen_trophy"}})

# --- The four elemental cores use fourfold-symmetric 7x7 mechanical-crafter walls. Each wall yields
#     two, exactly what the doubled HF2 ritual consumes, so one wall per core upgrades the
#     forge. ---
write(f"{R}/mechanical/abyssal_core.json",
      mech(["WWPPPWW", "WPOAOPW", "PODCDOP", "PACBCAP", "PODCDOP", "WPOAOPW", "WWPPPWW"],
           {"W": "malum:astral_weave", "P": "malum:soul_stained_steel_plating",
            "O": "deepwaters:fopal", "A": "deepwaters:aquamarine_block",
            "D": "minecraft:diamond_block", "C": "cataclysm:coral_chunk",
            "B": "deepwaters:blackpearl"},
           "bertieprogression:abyssal_core", 2))
write(f"{R}/mechanical/desert_core.json",
      mech(["SSSGSSS", "SCRKRCS", "SRCKCRS", "GKKMKKG", "SRCKCRS", "SCRKRCS", "SSSGSSS"],
           {"S": "create:brass_sheet", "G": "armageddon_mod:gilded_plate",
            "C": "malum:cthonic_gold", "R": "slag:rose_gold_block",
            "K": "iceandfire:dragonbone", "M": "cataclysm:ancient_metal_block"},
           "bertieprogression:desert_core", 2))
write(f"{R}/mechanical/cursed_core.json",
      mech(["DDFDFDD", "DFSSSFD", "FSBBBSF", "DSBCBSD", "FSBBBSF", "DFSSSFD", "DDFDFDD"],
           {"D": "slag:deep_alloy_block", "F": "malum:imitation_flesh",
            "S": "malum:cursed_sapball", "B": "cataclysm:black_steel_ingot",
            "C": "cataclysm:cursium_ingot"},
           "bertieprogression:cursed_core", 2))
write(f"{R}/mechanical/storm_core.json",
      mech(["BLBLBLB", "LPNINPL", "BNEPENB", "LIPHPIL", "BNEPENB", "LPNINPL", "BLBLBLB"],
           {"B": "irons_spellbooks:lightning_bottle", "L": "cataclysm:lacrima",
            "P": "minecraft:lapis_block", "N": "malum:wind_nucleus",
            "I": "elemental_metals:lightning_infused_iron_ingot",
            "E": "cataclysm:essence_of_the_storm", "H": "minecraft:heart_of_the_sea"},
           "bertieprogression:storm_core", 2))

# ================================================================ Dark Arts materials
# Dark Arts textile/pouch/scythe branch and Imitation Heart rewrite.

# Any Ice and Fire dragon heart is the core. The mod's own dragon_hearts tag contains fire, ice and
# lightning hearts (jar-verified); all four pedestal stacks are unbounded Malum extras.
write("data/malum/recipe/spirit_infusion/imitation_heart.json",
      infusion("#iceandfire:dragon_hearts", 1,
               [("malum:imitation_flesh", 4), ("malum:warp_flux", 2),
                ("create:brass_sheet", 6), ("malum:iridescent_ether", 6)],
               [SP("sacred", 16), SP("wicked", 16), SP("arcane", 16), SP("eldritch", 16)],
               "malum:imitation_heart", 1))

# Soulwoven Silk: replaces Malum's 2 Wool + 2 String -> 4 stock infusion.
write("data/malum/recipe/spirit_infusion/soulwoven_silk.json",
      infusion("#minecraft:wool", 4,
               [("malum:hex_ash", 1), ("born_in_chaos_v1:spiritual_dust", 1),
                ("minecraft:string", 5), ("minecraft:leather", 3)],
               [SP("sacred", 4), SP("aerial", 3), SP("earthen", 3)],
               "malum:soulwoven_silk", 1))

# Arcane Cloth: replace Iron's Spellbooks' wool centre with Soulwoven Silk; the eight-essence ring
# and one-cloth output stay the same.
write("data/irons_spellbooks/recipe/magic_cloth.json",
      shaped(["AAA", "ASA", "AAA"],
             {"A": "irons_spellbooks:arcane_essence", "S": "malum:soulwoven_silk"},
             "irons_spellbooks:magic_cloth"))

# Astral Weave gains a Spirit Altar route; Malum's phantom/ghast reaping data remains available.
write(f"{R}/malum/astral_weave.json",
      infusion("irons_spellbooks:magic_cloth", 1,
               [("malum:soulwoven_silk", 2), ("minecraft:phantom_membrane", 3),
                ("minecraft:string", 8)],
               [SP("sacred", 8), SP("aerial", 12), SP("arcane", 16)],
               "malum:astral_weave", 1))

# Soulwoven Pouch: stock 1x2 craft disabled; Bundle core + exactly eight Forge pedestals.
write("data/malum/recipe/soulwoven_pouch.json", DISABLED)
write(f"{RIT}/soulwoven_pouch.json",
      ritual("minecraft:bundle",
             [("malum:soulwoven_silk", 4), ("minecraft:string", 4)],
             "malum:soulwoven_pouch", 1,
             essences={"aureal": 30, "blood": 1000, "souls": 0}))

# Ravenous Pouch: stock Spirit Infusion disabled; its Tier-I Forge replacement also fills all eight
# pedestals (3 + 2 + 1 + 2).
write("data/malum/recipe/spirit_infusion/ravenous_pouch.json", DISABLED)
write(f"{RIT}/ravenous_pouch.json",
      ritual("malum:soulwoven_pouch",
             [("twilightforest:raven_feather", 3), ("malum:soulwoven_silk", 2),
              ("malum:grim_talc", 1), ("minecraft:string", 2)],
             "malum:ravenous_pouch", 1, tier=1))

# Soulstained Steel Scythe: replace Malum's stock infusion and retain the Crude Scythe components.
_soulstained_scythe = infusion(
    "malum:crude_scythe", 1,
    [("iceandfire:dragonbone", 2), ("malum:soul_stained_steel_plating", 6),
     ("malum:malignant_lead", 12), ("malum:mnemonic_fragment", 20)],
    ALL8x8, "malum:soul_stained_steel_scythe", 1)
_soulstained_scythe["carryOverComponentData"] = True
write("data/malum/recipe/spirit_infusion/soul_stained_steel_scythe.json",
      _soulstained_scythe)

# --- Living Flesh: override Malum's spirit infusion. The
#     block round-trip (living_flesh_from_block) is LEFT ALONE - killing it would strand any
#     Block of Living Flesh a player already owns. ---
# "dragon flesh (any of 3)": the three share no stem and no mod ships a tag, so we ship one.
write("data/bertieprogression/tags/item/dragon_flesh.json",
      {"values": ["iceandfire:fire_dragon_flesh", "iceandfire:ice_dragon_flesh",
                  "iceandfire:lightning_dragon_flesh"]})
write("data/malum/recipe/spirit_infusion/living_flesh.json",
      infusion("#bertieprogression:dragon_flesh", 1,
               [("minecraft:rotten_flesh", 64), ("irons_spellbooks:blood_vial", 16),
                ("born_in_chaos_v1:monster_flesh", 4)],
               [SP("sacred", 6), SP("wicked", 6), SP("aqueous", 6)],
               "malum:living_flesh", 1))

# --- Sturdy Sheet + Powdered Obsidian -----------------------------------------------------------
# Sheet: the assembly now starts from an Obsidiansteel Ingot instead of obsidian dust, with two
# deploy steps applying Powdered Obsidian in front of Create's original fill/press/press. Written out
# by hand rather than through _seq_assembly so the absence of `loops` matches stock exactly - the
# stock recipe omits it, and writing an explicit value would be a silent behaviour change.
_UOS = "create:unprocessed_obsidian_sheet"
_deploy = lambda item: {"type": "create:deploying",
                        "ingredients": [{"item": _UOS}, {"item": item}],
                        "results": [{"id": _UOS}]}
write("data/create/recipe/sequenced_assembly/sturdy_sheet.json", {
    "neoforge:conditions": conds("create", "forbidden_arcanus"),
    "type": "create:sequenced_assembly",
    "ingredient": {"item": "forbidden_arcanus:obsidiansteel_ingot"},
    "results": [{"id": "create:sturdy_sheet"}],
    "sequence": [
        _deploy("create:powdered_obsidian"),
        _deploy("create:powdered_obsidian"),
        {"type": "create:filling",
         "ingredients": [{"item": _UOS},
                         {"type": "neoforge:single", "amount": 500, "fluid": "minecraft:lava"}],
         "results": [{"id": _UOS}]},
        {"type": "create:pressing", "ingredients": [{"item": _UOS}], "results": [{"id": _UOS}]},
        {"type": "create:pressing", "ingredients": [{"item": _UOS}], "results": [{"id": _UOS}]},
    ],
    "transitional_item": {"id": _UOS},
})

# Crushing obsidian: was 1 dust + 75% obsidian back. Now a second dust at 25% and the obsidian
# return cut to 10%, so crushing is a real conversion rather than a near-free duplicator.
write("data/create/recipe/crushing/obsidian.json", {
    "neoforge:conditions": conds("create"),
    "type": "create:crushing",
    "ingredients": [{"item": "minecraft:obsidian"}],
    "processing_time": 500,
    "results": [{"id": "create:powdered_obsidian"},
                {"chance": 0.25, "id": "create:powdered_obsidian"},
                {"chance": 0.1, "id": "minecraft:obsidian"}],
})

# --- Netherly Meal: Tier-II Hephaestus ritual with a Bowl core and seven pedestals.
#     "max souls, max blood" = the TIER II ceiling (50 / 15000), jar-verified from HephaestusForgeLevel
#     - the ritual runs ON a T2 forge, so those are the most it can hold. Aureal and ink unspecified,
#     so both are zero. ---
write(f"{RIT}/netherly_meal.json",
      ritual("twilightforest:meef_stroganoff",
             [("iceandfire:fire_dragon_heart", 1), ("cataclysm:koboleton_bone", 1),
              ("malum:living_flesh", 1), ("iceandfire:fire_dragon_blood", 1),
              ("#iceandfire:scales/dragon/fire", 2), ("minecraft:lava_bucket", 1)],
             "bertieprogression:netherly_meal", 1, tier=2,
             essences={"aureal": 0, "blood": 15000, "souls": 50}))

# --- Pastel's opening moves become forge work. ---
# Paintbrush: a Tier-I ritual on the Totemic Staff, two of each starter shard and a Brush, 500 ink.
# The stock stick-copper-wool craft goes, so the brush arrives with the forge rather than before it.
write(f"{RIT}/paintbrush.json",
      ritual("malum:totemic_staff",
             [("minecraft:amethyst_shard", 2), ("pastel:citrine_shard", 2),
              ("pastel:topaz_shard", 2), ("minecraft:brush", 1)],
             "pastel:paintbrush", 1, tier=1, xp=500))
write("data/pastel/recipe/crafting_table/paintbrush.json", DISABLED)
# The three starter Pigment Pedestals: Tier-II, a Compressed Crafting Table at the core, four of the
# matching shard and two of its wool. 50 souls and 1350 ink are the TIER II ceilings (jar-verified
# from HephaestusForgeLevel: 3000/50/15000/1350); the aureal cost is deliberate, not a ceiling.
for _gem, _shard, _wool in (("topaz", "pastel:topaz_shard", "minecraft:cyan_wool"),
                            ("amethyst", "minecraft:amethyst_shard", "minecraft:purple_wool"),
                            ("citrine", "pastel:citrine_shard", "minecraft:yellow_wool")):
    write(f"{RIT}/pedestal_basic_{_gem}.json",
          ritual("avaritia:compressed_crafting_table",
                 [(_shard, 4), (_wool, 2), ("minecraft:red_nether_bricks", 1)],
                 f"pastel:pedestal_basic_{_gem}", 1, tier=2,
                 essences={"aureal": 360, "blood": 0, "souls": 50}, xp=1350))
    write(f"data/pastel/recipe/crafting_table/pedestal_basic_{_gem}.json", DISABLED)

# --- Pastel: a crushed shard yields one powder, not two. ---
# Overrides carry every field of the stock recipe because the crushing bench reads all of them -
# the damage rate, sound, particle and advancement gate are not defaults. The sound and the gate
# differ per gem in ways no formula predicts (onyx and moonstone sit under different advancement
# trees, and amethyst's break sound is vanilla's), so each is copied from its own stock file.
for _gem, _shard, _sound, _adv in (
        ("topaz", "pastel:topaz_shard", "pastel:block.small_topaz_bud.break",
         "pastel:hidden/collect_shards/topaz"),
        ("citrine", "pastel:citrine_shard", "pastel:block.small_citrine_bud.break",
         "pastel:hidden/collect_shards/citrine"),
        ("amethyst", "minecraft:amethyst_shard", "block.small_amethyst_bud.break",
         "pastel:hidden/collect_shards/amethyst"),
        ("onyx", "pastel:onyx_shard", "pastel:block.small_onyx_bud.break",
         "pastel:create_onyx_shard"),
        ("moonstone", "pastel:moonstone_shard", "pastel:block.small_moonstone_bud.break",
         "pastel:lategame/collect_moonstone")):
    write(f"data/pastel/recipe/anvil_crushing/gemstone_powder/"
          f"{_gem}_powder_from_{_shard.split(':')[1]}.json", {
              "type": "pastel:anvil_crushing",
              "ingredient": [{"item": _shard}],
              "crushedItemsPerPointOfDamage": 0.6,
              "experience": 0.4,
              "result": {"id": f"pastel:{_gem}_powder", "count": 1},
              "particleEffectIdentifier": "explosion",
              "soundEventIdentifier": _sound,
              "group": "gemstone_crushing",
              "required_advancement": _adv,
          })

# --- Corrupti Dust: the same five ingredients, but one dust instead of four. ---
write("data/forbidden_arcanus/recipe/corrupti_dust.json",
      shapeless(["forbidden_arcanus:obsidiansteel_ingot", "minecraft:blaze_powder",
                 "minecraft:nether_wart", "forbidden_arcanus:arcane_crystal_dust",
                 "forbidden_arcanus:ender_pearl_fragment"],
                "forbidden_arcanus:corrupti_dust", 1))

# --- Malignant Pewter: iron and scrap give way to soulstained steel and a netherite ingot. ---
write("data/malum/recipe/spirit_infusion/malignant_pewter_ingot.json", DISABLED)
write(f"{R}/malum/malignant_pewter_ingot.json",
      infusion("malum:soul_stained_steel_ingot", 4,
               [("minecraft:netherite_ingot", 1), ("pastel:onyx_powder", 2),
                ("malum:malignant_lead", 2), ("malum:null_slate", 6)],
               [SP("earthen", 16), SP("eldritch", 16)],
               "malum:malignant_pewter_ingot", 1))

# --- Cognition: the cognitive chain is rebuilt from the flux up. ---
# Flux is mixed rather than crafted, the alloy and the crystal both want a full ring of amalgam,
# and the Astute Assembly stops being a 3x3 and becomes a sequenced assembly.
write("data/cognition/recipe/cognitive_flux.json", DISABLED)
write(f"{R}/cognition/cognitive_flux_mixing.json",
      {"neoforge:conditions": conds("create", "cognition"),
       "type": "create:mixing",
       "ingredients": [{"tag": "c:dusts/copper"}, {"tag": "c:dusts/copper"},
                       {"item": "minecraft:lapis_lazuli"}, {"tag": "c:gems/quartz"}],
       "results": [{"id": "cognition:cognitive_flux", "count": 4}]})

write("data/cognition/recipe/cognitive_alloy.json",
      shaped(["AAA", "ILI", "AAA"],
             {"A": "cognition:cognitive_amalgam", "I": "twilightforest:ironwood_ingot",
              "L": "#c:ingots/lead"},
             "cognition:cognitive_alloy", 1))

write("data/cognition/recipe/cognitive_crystal.json",
      shaped(["AAA", "AEA", "AAA"],
             {"A": "cognition:cognitive_amalgam", "E": "minecraft:emerald_block"},
             "cognition:cognitive_crystal", 1))

write("data/cognition/recipe/astute_assembly.json", DISABLED)
_seq_assembly(f"{R}/cognition/astute_assembly.json",
              "cognition:cognitive_crystal", "cognition:cognitive_crystal",
              [("deploy", "cognition:cognitive_alloy"), ("deploy", "cognition:cognitive_alloy"),
               ("deploy", "cognition:cognitive_alloy"),
               ("fill", "irons_spellbooks:timeless_slurry", 250), ("press",)],
              [{"id": "cognition:astute_assembly", "count": 1}],
              mods=("cognition", "irons_spellbooks"))

# --- The Nether Crafting Table stops being a netherrack-and-skulls build. ---
write("data/avaritia/recipe/nether_crafting_table.json",
      {"neoforge:conditions": conds("avaritia", "cataclysm", "pastel"),
       "type": "avaritia:shaped_table", "tier": 1,
       "key": {"I": {"item": "cataclysm:ignitium_ingot"},
               "N": {"item": "minecraft:nether_star"},
               "S": {"item": "pastel:stratine_gem"},
               "D": {"item": "avaritia:double_compressed_crafting_table"},
               "B": {"item": "cataclysm:black_steel_block"},
               "E": {"item": "minecraft:netherite_block"}},
       "pattern": ["INI", "SDS", "BEB"],
       "result": {"count": 1, "id": "avaritia:nether_crafting_table"}})

# --- The late Pastel chain: CMY pedestal, rose quartz, failing, netherite. ---
# c:gems/quartz collected three things: NeoForge's Nether Quartz, Malum's Natural Quartz and Haze
# n Stuff's Rose Quartz. Rose Quartz is a crafted material, not a quartz, and having it in there
# made every "any quartz" recipe accept it. The tag is replaced with the two real ones.
write("data/c/tags/item/gems/quartz.json",
      {"replace": True,
       "values": ["minecraft:quartz",
                  {"id": "malum:natural_quartz", "required": False},
                  {"id": "#forge:gems/quartz", "required": False}]})

# The CMY Pedestal: the three polished gem blocks across the top, a Vegetal Block bedded in
# polished calcite. It costs a full sixteen of every pigment.
_cmy = pedestal(["TAC", "RVR", "RRR"],
                {"T": "pastel:polished_topaz_block", "A": "pastel:polished_amethyst_block",
                 "C": "pastel:polished_citrine_block", "R": "pastel:polished_calcite",
                 "V": "pastel:vegetal_block"},
                {"pastel:cyan": 16, "pastel:magenta": 16, "pastel:yellow": 16,
                 "pastel:black": 16, "pastel:white": 16},
                "pastel:pedestal_all_basic", 1, tier="basic", time=480, xp=4.0)
_cmy["required_advancement"] = "pastel:unlocks/blocks/cmy_pedestal"
_cmy["disable_yield_upgrades"] = True
write("data/pastel/recipe/pedestal/tier1/pedestal_all_basic.json", _cmy)

# Rose Quartz stops being two items off a shapeless craft and moves onto the Onyx Pedestal.
write("data/hazennstuff/recipe/crafting/materials/rose_quartz.json", DISABLED)
_rq = pedestal(["PDP", "OQO", "PDP"],
               {"P": "pastel:pink_pigment", "D": "irons_spellbooks:divine_pearl",
                "O": "pastel:orange_pigment", "Q": "minecraft:quartz"},
               {"pastel:cyan": 0, "pastel:magenta": 6, "pastel:yellow": 4,
                "pastel:black": 0, "pastel:white": 0},
               "hazennstuff:rose_quartz", 1, tier="advanced", time=40, xp=2.0)
_rq["key"]["Q"] = {"tag": "c:gems/quartz"}
_rq["required_advancement"] = "pastel:midgame/build_advanced_pedestal_structure"
write("data/pastel/recipe/pedestal/tier3/rose_quartz.json", _rq)

# Bottle of Failing: Pastel's own frame, but a Bottle of Fading at the heart, Stratine Gems
# instead of fragments and Dark Matter where the prismarine was.
_failing = pedestal(["FSF", "DBD", "FSF"],
                    {"F": "minecraft:fermented_spider_eye", "S": "pastel:stratine_gem",
                     "D": "forbidden_arcanus:dark_matter", "B": "pastel:bottle_of_fading"},
                    {"pastel:cyan": 16, "pastel:magenta": 16, "pastel:yellow": 16,
                     "pastel:black": 16, "pastel:white": 16},
                    "pastel:bottle_of_failing", 1, tier="advanced", time=1200, xp=2.0)
_failing["required_advancement"] = "pastel:unlocks/items/bottle_of_failing"
write("data/pastel/recipe/pedestal/tier3/bottle_of_failing.json", _failing)

# Ink storage gets more expensive: the flask wants tinted glass, and the assortment is built out
# of flasks and Deorum Glass rather than loose pigment.
_flask = pedestal(["SGS", "GPG", "SGS"],
                  {"P": "#pastel:pigments", "G": "minecraft:tinted_glass",
                   "S": "pastel:shimmerstone_gem"},
                  {"pastel:cyan": 4, "pastel:magenta": 4, "pastel:yellow": 4,
                   "pastel:black": 0, "pastel:white": 0},
                  "pastel:ink_flask", 1, tier="simple", time=300, xp=1.0)
_flask["required_advancement"] = "pastel:unlocks/items/basic_ink_storage_items"
write("data/pastel/recipe/pedestal/tier2/ink_flask.json", _flask)

_assort = pedestal(["SCS", "PPP", "GGG"],
                   {"P": "pastel:ink_flask", "G": "forbidden_arcanus:deorum_glass",
                    "C": "pastel:four_leaf_clover", "S": "pastel:shimmerstone_gem"},
                   {"pastel:cyan": 16, "pastel:magenta": 16, "pastel:yellow": 16,
                    "pastel:black": 0, "pastel:white": 0},
                   "pastel:ink_assortment", 1, tier="simple", time=600, xp=4.0)
_assort["required_advancement"] = "pastel:unlocks/items/ink_assortment"
write("data/pastel/recipe/pedestal/tier2/ink_assortment.json", _assort)

# The Cinderous Soulcaller comes off the crafting table and onto the CMY Pedestal, with a Bell at
# its heart and the netherite scrap gathered into the top-right corner.
write("data/irons_spellbooks/recipe/cinderous_soulcaller.json", DISABLED)
_soul = pedestal(["CNN", "CBN", "CCC"],
                 {"C": "irons_spellbooks:cinder_essence", "N": "minecraft:netherite_scrap",
                  "B": "minecraft:bell"},
                 {"pastel:cyan": 0, "pastel:magenta": 0, "pastel:yellow": 15,
                  "pastel:black": 0, "pastel:white": 0},
                 "irons_spellbooks:cinderous_soulcaller", 1, tier="simple", time=300, xp=4.0)
_soul["required_advancement"] = "pastel:unlocks/blocks/cmy_pedestal"
write("data/pastel/recipe/pedestal/tier2/cinderous_soulcaller.json", _soul)

# The Flame Eye leaves Cataclysm's own table for the CMY Pedestal: blaze powder down the left,
# aquamarine down the right, ancient scrap above and below, an Xpetrified Orb at the heart.
write("data/cataclysm/recipe/flame_eye.json", DISABLED)
_flame = pedestal(["BSA", "BXA", "BSA"],
                  {"B": "minecraft:blaze_powder", "S": "minecraft:netherite_scrap",
                   "A": "deepwaters:aquamarine", "X": "forbidden_arcanus:xpetrified_orb"},
                  {"pastel:cyan": 6, "pastel:magenta": 0, "pastel:yellow": 6,
                   "pastel:black": 0, "pastel:white": 0},
                  "cataclysm:flame_eye", 1, tier="simple", time=200, xp=4.0)
_flame["required_advancement"] = "pastel:unlocks/blocks/cmy_pedestal"
write("data/pastel/recipe/pedestal/tier2/flame_eye.json", _flame)

# Decay spreads and multiplies, so anything it drops is free: one Bottle of Fading turns into a
# field of Vegetal, one Bottle of Failing into a field of Neolith. The blocks stay breakable and
# stay cleanable with Decay Away - they just no longer hand anything back.
for _decay in ("fading", "failing", "ruin", "forfeiture"):
    write(f"data/pastel/loot_table/blocks/{_decay}.json",
          {"type": "minecraft:block", "pools": []})

# A route to Netherite through the Fusion Shrine. The three crafting-effect fields are not
# optional: leave any of them out and the whole recipe fails to parse.
write("data/pastel/recipe/fusion_shrine/netherite_ingot.json",
      {"neoforge:conditions": conds("pastel", "slag", "hazennstuff"),
       "type": "pastel:fusion_shrine", "time": 400, "experience": 4.0,
       "fluid": {"fluid": "slag:molten_gold"},
       "ingredients": ["hazennstuff:rose_gold_ingot",
                       {"item": "minecraft:netherite_scrap", "count": 4},
                       {"item": "pastel:neolith", "count": 2}],
       "result": {"id": "minecraft:netherite_ingot", "count": 1},
       "required_advancement": "pastel:unlocks/blocks/fusion_shrine",
       "start_crafting_effect": "nothing",
       "during_crafting_effects": ["nothing", "visual_explosions_on_shrine"],
       "finish_crafting_effect": "single_visual_explosion_on_shrine"})

# Two mods ship a Rose Gold Ingot. Haze n Stuff's gets a name of its own so the pack can tell
# them apart at a glance, and it is no longer a crafting-table item: the Clibano cooks Slag n'
# Embers' Rose Gold together with a Rose Quartz to make it.
write("assets/hazennstuff/lang/en_us.json",
      {"item.hazennstuff.rose_gold_ingot": "Rosest Gold Ingot",
       "material.hazennstuff.rose_gold": "Rosest Gold"})
write("data/hazennstuff/recipe/crafting/materials/rose_gold_ingot.json", DISABLED)
write(f"{R}/rosest_gold_ingot_from_clibano_combustion.json", {
    "type": "forbidden_arcanus:clibano_combustion",
    "category": "misc",
    "cooking_time": 400,
    "experience": 1.0,
    "fire_type": "fire",
    "ingredients": {"first": {"item": "slag:rose_gold_ingot"},
                    "second": {"item": "hazennstuff:rose_quartz"}},
    "result": {"count": 1, "id": "hazennstuff:rose_gold_ingot"},
})

# The vanilla four-scrap-four-gold shapeless goes, and so does Re-Avaritia's two-and-two, which
# is the same recipe made cheaper on the Nether Crafting Table.
write("data/minecraft/recipe/netherite_ingot.json", DISABLED)
write("data/avaritia/recipe/netherite_ingot_too.json", DISABLED)

# --- Tags that exist only so Ash and Twilight's quest tasks can name a set of things. ---
# The three starter Pigment Pedestals all render as "Pigment Pedestal" and Pastel's own
# pastel:pedestals tag also covers the Onyx and Moonstone upgrades, which the quest must not accept.
write("data/bertieprogression/tags/item/basic_pigment_pedestals.json",
      {"values": ["pastel:pedestal_basic_topaz", "pastel:pedestal_basic_amethyst",
                  "pastel:pedestal_basic_citrine"]})
# YUNG's Better Nether Fortresses replaces the vanilla fortress in the full pack but is absent from
# smaller test packs, so both ids are listed and neither is required.
write("data/bertieprogression/tags/worldgen/structure/nether_fortress.json",
      {"values": [{"id": "minecraft:fortress", "required": False},
                  {"id": "betterfortresses:fortress", "required": False}]})

INSTANCE_MODS = os.path.join(os.environ.get("APPDATA", ""), "PrismLauncher", "instances",
                             # This is a filesystem path, not prose. The instance is named
                             # "bertie-no-worldgen" and its game directory is "minecraft", not
                             # ".minecraft"; changing either makes the generator scan the wrong jars.
                             "bertie-no-worldgen", "minecraft", "mods")

# ================================================================ BRICK FORGE ORE BONUS
# Every Brick Forge ore smelt gets a 1% chance of also dropping a storage block of what it made.
# slag:double_smelting has no secondary-output field, so BrickForgeBonus.java reads this table and
# rolls it when the forge finishes; the table is generated, never hand-written.
#
# What counts as an "ore smelt" is decided by the pack's own data rather than by a list here: the
# recipe's result has to be a member of some `c:ingots/*` or `c:gems/*` tag. That is what makes a
# result a smelted MATERIAL, and it is why the terracotta, brick, stone and dye double-smelts are
# not in the table. A material with no storage block is skipped, as asked.
BRICK_FORGE_BONUS_PATH = "brick_forge_bonus.json"

def _brick_forge_bonus():
    import zipfile
    jars = ([os.path.join(INSTANCE_MODS, f) for f in sorted(os.listdir(INSTANCE_MODS))
             if f.endswith(".jar")] if os.path.isdir(INSTANCE_MODS) else [])
    vanilla = os.path.join(os.environ.get("APPDATA", ""), "PrismLauncher", "libraries", "com",
                           "mojang", "minecraft", "1.21.1", "minecraft-1.21.1-client.jar")
    if os.path.isfile(vanilla):
        jars.append(vanilla)
    if not jars:
        return None

    items, tags, results = set(), {}, {}

    def take_recipe(text):
        try:
            obj = json.loads(text)
        except ValueError:
            return
        if obj.get("type") != "slag:double_smelting":
            return
        res = obj.get("result") or {}
        if not (isinstance(res, dict) and isinstance(res.get("id"), str)):
            return
        ins = set()
        for side in ("ingredientA", "ingredientB"):
            ing = obj.get(side) or {}
            if isinstance(ing, dict):
                ins.update(v for v in (ing.get("item"), ing.get("tag")) if isinstance(v, str))
        results.setdefault(res["id"], set()).update(ins)

    for jp in jars:
        try:
            zf = zipfile.ZipFile(jp)
        except zipfile.BadZipFile:
            continue
        with zf:
            for n in zf.namelist():
                p = n.split("/")
                if (len(p) >= 5 and p[0] == "assets" and p[2] == "models" and p[3] == "item"
                        and n.endswith(".json")):
                    items.add(f"{p[1]}:{'/'.join(p[4:])[:-5]}")
                elif n.startswith("data/c/tags/item/") and n.endswith(".json"):
                    key = n[len("data/c/tags/item/"):-5]
                    try:
                        vals = json.loads(zf.read(n).decode("utf-8")).get("values", [])
                    except (ValueError, UnicodeDecodeError):
                        continue
                    tags.setdefault(key, set()).update(
                        v if isinstance(v, str) else v.get("id") for v in vals)
                elif "/recipe" in n and n.endswith(".json"):
                    try:
                        take_recipe(zf.read(n).decode("utf-8"))
                    except (KeyError, UnicodeDecodeError):
                        pass

    # This run's own recipes are not in any jar yet.
    for rel in written:
        if "/recipe/" in rel or rel.startswith("data/slag/recipe/"):
            try:
                with io.open(os.path.join(RES, rel.replace("/", os.sep)), encoding="utf-8") as f:
                    take_recipe(f.read())
            except OSError:
                pass

    material_of = {}
    for key, vals in tags.items():
        if key.startswith(("ingots/", "gems/")):
            for v in vals:
                if v:
                    material_of.setdefault(v, key.split("/", 1)[1])

    bonus, skipped = {}, []
    for res, ins in sorted(results.items()):
        ns, name = res.split(":", 1)
        mat = material_of.get(res)
        # Three ways to be an ore smelt: the result is a c:-tagged material, it is an ingot (the
        # vanilla metals are in no c: tag), or it was smelted out of raw ore.
        from_raw = any("raw_" in i or i.startswith("c:raw_materials/") for i in ins)
        if not mat and not name.endswith("_ingot") and not from_raw:
            continue
        stem = name[:-6] if name.endswith("_ingot") else name
        stems = [stem] + ([stem[len("refined_"):]] if stem.startswith("refined_") else [])
        # Same namespace first: several mods tag a c:storage_blocks/<mat> and picking whichever
        # sorts first hands Create's zinc to AnvilCraft's block.
        rank = {ns: 0, "minecraft": 1}
        candidates = sorted(tags.get(f"storage_blocks/{mat}", set()) if mat else (),
                            key=lambda c: (rank.get(c.split(":", 1)[0], 2), c))
        candidates += [f"{ns}:{s}{suffix}" for s in stems
                       for suffix in ("_block",)] + [f"{ns}:block_of_{s}" for s in stems]
        block = next((c for c in candidates if c and c in items), None)
        if block:
            bonus[res] = block
        else:
            skipped.append(res)
    return bonus, skipped

_bf = _brick_forge_bonus()
if _bf is None:
    print("  !! brick forge bonus: no jars found - table left as it was.")
else:
    _bonus, _skipped = _bf
    write(BRICK_FORGE_BONUS_PATH, dict(sorted(_bonus.items())))
    print(f"  brick forge bonus: {len(_bonus)} ore smelts got a 1% block")
    for _r, _b in sorted(_bonus.items()):
        print(f"      {_r}  ->  {_b}")
    for _r in _skipped:
        print(f"      {_r}  ->  (no storage block, skipped)")

# ================================================================ REMOVED ITEMS
# Edit bertie-workspace/docs/removed/<modid>.md, then run this generator. See that directory's
# README.md. The planning records remain in the private workspace; generated runtime data remains
# here in the public product repository.
# This section does three things:
#   1. finds EVERY recipe in the pack whose result is a removed id and overrides it with
#      neoforge:false - searched by RESULT, so no recipe file is ever named by hand;
#   2. writes removed_items.json at the jar root for RemovedItems.java, which drops the ids from
#      every creative tab (and therefore from EMI, whose index-source is `creative`);
#   3. rewrites the LEAKS block in each doc with the loot tables that still reference a removed id.
# Needs a synced bertie pack instance to scan. Without it: warn and skip, never silently emit nothing.

def _parse_removed(path, modid):
    """Strict pipe-table parser. A malformed row is a build error, never a silent skip."""
    rows, in_table = [], False
    for n, line in enumerate(io.open(path, encoding="utf-8"), 1):
        line = line.rstrip("\n")
        if line.startswith("<!-- LEAKS:"):
            break
        if not line.startswith("|"):
            in_table = False
            continue
        cells = [c.strip() for c in line.strip("|").split("|")]
        if len(cells) != 4:
            raise SystemExit(f"{path}:{n}: expected 4 columns, got {len(cells)}: {line}")
        if cells[1].lower() == "id":          # header
            in_table = True
            continue
        if set("".join(cells)) <= set("-: "):  # separator
            continue
        if not in_table:
            continue
        name, iid, reason, date = cells
        iid = iid.strip("`")
        if ":" not in iid:
            raise SystemExit(f"{path}:{n}: id has no namespace: {iid!r}")
        if iid.split(":")[0] != modid:
            raise SystemExit(f"{path}:{n}: id {iid!r} does not belong in {modid}.md")
        if not reason:
            raise SystemExit(f"{path}:{n}: {iid} has no reason")
        rows.append({"name": name, "id": iid, "reason": reason, "removed": date})
    return rows

# Loot tables are not the only data that hands an item over. Apotheosis rolls affixed gear from
# its own entry files and dresses its bosses from gear sets, and the Museum Curator checklist puts
# an item on a page whether or not anything drops it. All three are stripped the same way.
OTHER_KINDS = ("/affix_loot_entries/", "/gear_sets/", "/museumexhibits/")


def _strip_ids(node, gone):
    """A data file with every removed id taken out. None means the node itself has to go.

    Anything naming a removed id in one of its own string values dies, which covers both an entry
    that IS the item and a bare id sitting in a list. Apotheosis wraps its ids one level down in a
    `stack`, so that shape is named explicitly rather than left to a general deep search - a deep
    search would also delete a set for mentioning the item in an unrelated field.
    """
    if isinstance(node, list):
        return [x for x in (_strip_ids(v, gone) for v in node) if x is not None]
    if isinstance(node, str):
        return None if node in gone else node
    if not isinstance(node, dict):
        return node
    if any(isinstance(v, str) and v in gone for v in node.values()):
        return None
    _st = node.get("stack")
    if isinstance(_st, dict) and _st.get("id") in gone:
        return None
    return {k: _strip_ids(v, gone) for k, v in node.items()}


def _strip_loot(node, gone):
    """A loot table with every removed item taken out. None means the node itself has to go.

    An entry that hands over a removed id is deleted outright, and a pool or group left with no
    entries goes with it - an empty pool would still roll and produce nothing, which is the same
    result with more file. The table survives even when every pool dies: it has to, because an
    absent override lets the mod's original through.
    """
    if isinstance(node, list):
        return [x for x in (_strip_loot(v, gone) for v in node) if x is not None]
    if not isinstance(node, dict):
        return node
    if node.get("name") in gone or node.get("item") in gone:
        return None
    out = {}
    for k, v in node.items():
        nv = _strip_loot(v, gone)
        if k in ("entries", "children") and v and not nv:
            return None
        out[k] = nv
    return out


def _result_ids(obj, out):
    """Pull every result item id out of a recipe of ANY type/shape."""
    if isinstance(obj, dict):
        for k, v in obj.items():
            if k in ("result", "results"):
                for e in (v if isinstance(v, list) else [v]):
                    if isinstance(e, str):
                        out.add(e)
                    elif isinstance(e, dict):
                        for kk in ("id", "item"):
                            if isinstance(e.get(kk), str):
                                out.add(e[kk])
            elif isinstance(v, (dict, list)):
                _result_ids(v, out)
    elif isinstance(obj, list):
        for v in obj:
            _result_ids(v, out)
    return out

_removed = []
for _fn in sorted(os.listdir(REMOVED_DOCS)):
    if _fn.endswith(".md") and _fn != "README.md":
        _removed += _parse_removed(os.path.join(REMOVED_DOCS, _fn), _fn[:-3])
if not _removed:
    raise SystemExit(
        f"no removed-item rows found in {REMOVED_DOCS}; refusing to erase generated removals"
    )

# Walk the pack ONCE: registered item ids (for glob expansion), recipes by result, loot references.
_items, _hits, _leaks, _loot_src = set(), [], {}, {}
_scan_ok = False
if _removed:
    import fnmatch
    import zipfile
    # Our own jar is in that folder too, carrying the overrides the LAST run generated. Reading it
    # back makes every already-stripped file look like the mod's original, so nothing needs
    # stripping and the whole set silently disappears from the manifest. Skip ourselves.
    _scan = [os.path.join(INSTANCE_MODS, f) for f in sorted(os.listdir(INSTANCE_MODS))
             if f.endswith(".jar") and not f.startswith(MODID)] if os.path.isdir(INSTANCE_MODS) else []
    _vanilla = os.path.join(os.environ.get("APPDATA", ""), "PrismLauncher", "libraries", "com",
                            "mojang", "minecraft", "1.21.1", "minecraft-1.21.1-client.jar")
    if os.path.isfile(_vanilla):
        _scan.append(_vanilla)
    if not _scan:
        print("  !! REMOVED ITEMS: no jars found - recipe disables NOT emitted, LEAKS not refreshed.")
    else:
        _scan_ok = True
        _raw = [r["id"] for r in _removed]
        for _jp in _scan:
            _jn = os.path.basename(_jp)
            try:
                _zf = zipfile.ZipFile(_jp)
            except zipfile.BadZipFile:
                continue
            with _zf:
                for _n in _zf.namelist():
                    _parts = _n.split("/")
                    # registered items, same rule jarindex uses: an item MODEL is the proof
                    if (len(_parts) >= 5 and _parts[0] == "assets" and _parts[2] == "models"
                            and _parts[3] == "item" and _n.endswith(".json")):
                        _items.add(f"{_parts[1]}:{'/'.join(_parts[4:])[:-5]}")
        # Brace expansion first: `foo_{a,b}` -> `foo_a`, `foo_b`. Lets one row say "this material,
        # these slots", which a bare `*` cannot - and a bare material glob over-matches every tool
        # and ingot in the mod; l2complements:eternium_* is 12 items rather than four.
        def _braces(pat):
            i = pat.find("{")
            if i < 0:
                return [pat]
            j = pat.find("}", i)
            if j < 0:
                raise SystemExit(f"docs/removed: unclosed brace in {pat!r}")
            out = []
            for _opt in pat[i + 1:j].split(","):
                out += _braces(pat[:i] + _opt.strip() + pat[j + 1:])
            return out

        _removed = [dict(_r, id=_e) for _r in _removed for _e in _braces(_r["id"])]

        # expand globs now that we know what exists
        _expanded, _pattern_of = [], {}
        for _r in _removed:
            if "*" in _r["id"] or "?" in _r["id"]:
                _m = sorted(i for i in _items if fnmatch.fnmatchcase(i, _r["id"]))
                if not _m:
                    raise SystemExit(f"docs/removed: pattern {_r['id']!r} matched NOTHING. "
                                     f"A pattern that matches nothing is always a mistake.")
                print(f"  removed items: {_r['id']}  ->  {len(_m)} items")
                _expanded += _m
                for _i in _m:
                    _pattern_of[_i] = _r["id"]
            else:
                if _r["id"] not in _items:
                    raise SystemExit(f"docs/removed: {_r['id']!r} is not a registered item in this pack.")
                _expanded.append(_r["id"])
        _want = set(_expanded)
        # second pass: recipes and loot, now that we know the concrete ids
        for _jp in _scan:
            _jn = os.path.basename(_jp)
            try:
                _zf = zipfile.ZipFile(_jp)
            except zipfile.BadZipFile:
                continue
            with _zf:
                for _n in _zf.namelist():
                    if not _n.endswith(".json") or not _n.startswith("data/"):
                        continue
                    _is_recipe, _is_loot = "/recipe" in _n, "/loot_table" in _n
                    _is_other = any(_k in _n for _k in OTHER_KINDS)
                    if not (_is_recipe or _is_loot or _is_other):
                        continue
                    try:
                        _d = json.loads(_zf.read(_n).decode("utf-8-sig"))
                    except Exception:
                        continue
                    if _is_recipe:
                        for _r2 in _result_ids(_d, set()):
                            if _r2 in _want:
                                _hits.append((_n, _r2))
                    else:
                        _txt = json.dumps(_d)
                        _found = [_r2 for _r2 in _want if f'"{_r2}"' in _txt]
                        if not _found:
                            continue
                        if _is_loot:
                            for _r2 in _found:
                                _leaks.setdefault(_r2, []).append(f"{_jn}: {_n}")
                        # the file itself, so it can be re-emitted without the removed entries
                        _loot_src[_n] = _d

_removed_ids = sorted({i for i in (_expanded if _removed and _scan_ok else [])})
if _removed and not _scan_ok:
    # A failed jar scan must preserve both generated removal artifacts. The manifest already did
    # this below, but removed_items.json used to be overwritten with [], making every hidden item
    # visible again even though its recipe override survived.
    _removed_items_path = os.path.join(RES, "removed_items.json")
    try:
        with open(_removed_items_path, encoding="utf-8") as _f:
            _removed_ids = json.load(_f)
    except (OSError, json.JSONDecodeError) as _e:
        raise SystemExit(
            f"cannot preserve removed items after the failed jar scan: {_removed_items_path}: {_e}"
        )
    if not isinstance(_removed_ids, list) or not all(isinstance(_i, str) for _i in _removed_ids):
        raise SystemExit(f"cannot preserve malformed removed-item list: {_removed_items_path}")
else:
    write("removed_items.json", _removed_ids)

# MANIFEST. gen_data only ever writes, so without this, deleting a row would leave its
# neoforge:false override behind and the item would stay uncraftable forever - which defeats the
# whole point of the list being reversible. This MUST run even when the list is empty: the first
# version gated it and deleting the last row left the override in place (caught end-to-end).
_manifest_path = os.path.join(ROOT, "texture-work", ".removed_recipes.json")
_old_manifest = []
if os.path.isfile(_manifest_path):
    with open(_manifest_path, encoding="utf-8") as _f:
        _old_manifest = json.load(_f)
_new_manifest = sorted({_p for _p, _ in _hits})
if _removed and not _scan_ok:
    _new_manifest = _old_manifest          # could not scan: change nothing rather than wipe
else:
    for _stale in sorted(set(_old_manifest) - set(_new_manifest)):
        _abs = os.path.join(RES, _stale.replace("/", os.sep))
        if os.path.isfile(_abs):
            os.remove(_abs)
            print(f"  removed items: re-enabled {_stale}")
    for _path in _new_manifest:
        write(_path, DISABLED)
    with open(_manifest_path, "w", encoding="utf-8", newline="\n") as _f:
        json.dump(_new_manifest, _f, indent=2)
# Cutting the recipe only stops an item being MADE. A removed item that drops is still in the
# game and still drawn on the mob's EMI page, and one that Apotheosis rolls as affixed gear is
# still handed to the player wholesale. So every file that hands one over is re-emitted without
# it. Same manifest discipline as the recipes above: deleting a row from a doc has to give the
# drop back, which means tracking what we wrote and deleting what we no longer want written.
_data_manifest_path = os.path.join(ROOT, "texture-work", ".removed_data.json")
_old_data = []
if os.path.isfile(_data_manifest_path):
    with open(_data_manifest_path, encoding="utf-8") as _f:
        _old_data = json.load(_f)
if _removed and not _scan_ok:
    _new_data = _old_data                  # could not scan: change nothing rather than wipe
else:
    _new_data, _gone = [], set(_removed_ids)
    for _lp in sorted(_loot_src):
        _was = _loot_src[_lp]
        if "/loot_table" in _lp:
            _clean = _strip_loot(_was, _gone)
        else:
            _clean = _strip_ids(_was, _gone)
            if _clean is None:
                # The file IS the removed item, so there is nothing to strip it down to. Switch it
                # off through the mod's own gate instead: these files already ship a
                # `neoforge:conditions` block - it is how Apotheosis skips an entry whose mod is
                # absent - so the shape stays valid and the loader simply passes it over.
                _clean = dict(_was)
                _clean["neoforge:conditions"] = [{"type": "neoforge:false"}]
        if _clean != _was:
            write(_lp, _clean)
            _new_data.append(_lp)
    for _stale in sorted(set(_old_data) - set(_new_data)):
        _abs = os.path.join(RES, _stale.replace("/", os.sep))
        if os.path.isfile(_abs):
            os.remove(_abs)
            print(f"  removed items: restored {_stale}")
    with open(_data_manifest_path, "w", encoding="utf-8", newline="\n") as _f:
        json.dump(sorted(_new_data), _f, indent=2)

_n_loot = sum(1 for _p in _new_data if "/loot_table" in _p)
print(f"  removed items: {len(_removed_ids)} ids, {len(_new_manifest)} recipes disabled, "
      f"{_n_loot} loot tables and {len(_new_data) - _n_loot} other data files rewritten")

# Refresh the LEAKS block in every doc, replacing ONLY between the markers.
_OPEN = "<!-- LEAKS: generated every build, do not edit by hand -->"
_CLOSE = "<!-- /LEAKS -->"
if not _removed or _scan_ok:
    for _fn in sorted(os.listdir(REMOVED_DOCS)):
        if not _fn.endswith(".md") or _fn == "README.md":
            continue
        _p = os.path.join(REMOVED_DOCS, _fn)
        _src = io.open(_p, encoding="utf-8").read()
        if _OPEN not in _src or _CLOSE not in _src:
            # A doc written by hand has no markers yet, and skipping it meant a brand new mod's
            # table never reported a leak. Give it an empty pair and fill it in the same pass.
            _src = _src.rstrip() + "\n\n" + _OPEN + "\n" + _CLOSE + "\n"
        _body = []
        for _i in sorted(i for i in _removed_ids if i.split(":")[0] == _fn[:-3]):
            _l = _leaks.get(_i)
            if _l:
                _body.append(f"- **{_i}** — taken out of {len(_l)} loot table(s):")
                _body += [f"  - `{x}`" for x in _l[:8]]
                if len(_l) > 8:
                    _body.append(f"  - ...and {len(_l) - 8} more")
        _block = ("\n\n## Loot\n\nLoot tables that handed one of these out. Each is re-emitted "
                  "without the entry, so the drop is gone as well as the recipe.\n\n"
                  + ("\n".join(_body) if _body else "_None._") + "\n\n")
        _head, _rest = _src.split(_OPEN, 1)
        _tail = _rest.split(_CLOSE, 1)[1]
        io.open(_p, "w", encoding="utf-8", newline="\n").write(
            _head + _OPEN + _block + _CLOSE + _tail)

# ================================================================ Deep Waters Shrine ponder schematic
# The scene structure is generated from the SAME grid the matcher uses, and this script ASSERTS the
# two agree (it parses LAYERS straight out of DeepWatersShrineHandler.java). A ponder that teaches a
# shrine the handler would then reject is worse than no ponder at all, so the check is load-bearing.
SHRINE_LAYERS = [
    ["MMMMMMM", "MMMMMMM", "MMMMMMM", "MMMMMMM", "MMMMMMM", "MMMMMMM", "MMMMMMM"],  # L1 floor
    ["..SBS..", ".M...M.", "S.MMM.S", "B.MPM.B", "S.MMM.S", ".M...M.", "..SBS.."],  # L2
    [".......", ".M...M.", "..M.M..", "...C...", "..M.M..", ".M...M.", "......."],  # L3 conduit
    [".......", ".M...M.", "..MMM..", "..MPM..", "..MMM..", ".M...M.", "......."],  # L4
    ["MMMMMMM", "MMMMMMM", "MMMMMMM", "MMMMMMM", "MMMMMMM", "MMMMMMM", "MMMMMMM"],  # L5 roof
    ["..SLS..", ".....B.", "S.S.S.S", "L...S.L", "SS...SS", ".......", "..SLS.."],  # L6 crystals
]
SHRINE_BLOCKS = {
    "M": "minecraft:mossy_stone_bricks",
    "P": "deepwaters:fopal_pillar",
    "C": "minecraft:conduit",
    "L": "deepwaters:cryslaaquamarine",
    "B": "deepwaters:crysmeaquamarine",
    "S": "deepwaters:cryssmaquamarine",
}

def _assert_shrine_matches_java():
    """Parse LAYERS out of the handler and fail loudly if it has drifted from SHRINE_LAYERS."""
    java = os.path.join(ROOT, "src", "main", "java", "com", "berlord", "bertieprogression",
                        "shrine", "DeepWatersShrineHandler.java")
    if not os.path.isfile(java):
        return
    import re as _re
    src = open(java, encoding="utf-8").read()
    body = src.split("String[][] LAYERS = {", 1)[1].split("\n    };", 1)[0]
    rows = _re.findall(r'"([.MPCLBS]{7})"', body)
    flat = [r for layer in SHRINE_LAYERS for r in layer]
    assert rows == flat, (
        "Deep Waters shrine schematic DRIFT: gen_data.SHRINE_LAYERS != "
        "DeepWatersShrineHandler.LAYERS\n"
        f"  java  ({len(rows)} rows): {rows}\n"
        f"  python({len(flat)} rows): {flat}")

_assert_shrine_matches_java()

def _write_shrine_nbt():
    """Vanilla structure NBT (gzipped) for the ponder scene: 7 wide x 6 tall x 7 deep."""
    import gzip, struct

    def _str(s):
        b = s.encode("utf-8")
        return struct.pack(">H", len(b)) + b

    def _named(tag_id, name, payload):
        return bytes([tag_id]) + _str(name) + payload

    palette, index = [], {}
    for ch, bid in SHRINE_BLOCKS.items():
        index[ch] = len(palette)
        palette.append(bid)

    blocks = []
    for y, layer in enumerate(SHRINE_LAYERS):
        for z, row in enumerate(layer):          # row 0 = north = z 0
            for x, ch in enumerate(row):
                if ch == ".":
                    continue
                blocks.append((x, y, z, index[ch]))

    # palette: TAG_List of TAG_Compound {Name:String}
    pal = b""
    for bid in palette:
        pal += _named(8, "Name", _str(bid)) + b"\x00"
    palette_tag = _named(9, "palette", bytes([10]) + struct.pack(">i", len(palette)) + pal)

    # CRITICAL: `size` and each block's `pos` are TAG_LIST OF TAG_INT (list type 9, element type 3),
    # NOT TAG_Int_Array (11). StructureTemplate.load reads them with getList(..., Tag.TAG_INT), which
    # returns an EMPTY list for an int-array — so an int-array version loads a 0x0x0 structure and
    # logs NOTHING. That produced a silently blank ponder scene. Verified against Create's own
    # assets/create/ponder/gauges.nbt, which uses list-of-int for both.
    def _int_list(*vals):
        return bytes([3]) + struct.pack(">i", len(vals)) + b"".join(struct.pack(">i", v) for v in vals)

    # blocks: TAG_List of TAG_Compound {pos:[list of 3 int], state:Int}
    blk = b""
    for x, y, z, st in blocks:
        pos = _named(9, "pos", _int_list(x, y, z))
        blk += pos + _named(3, "state", struct.pack(">i", st)) + b"\x00"
    blocks_tag = _named(9, "blocks", bytes([10]) + struct.pack(">i", len(blocks)) + blk)

    size_tag = _named(9, "size", _int_list(7, len(SHRINE_LAYERS), 7))
    entities_tag = _named(9, "entities", bytes([10]) + struct.pack(">i", 0))
    data_version = _named(3, "DataVersion", struct.pack(">i", 3955))   # 1.21.1

    root = _named(10, "", size_tag + entities_tag + palette_tag + blocks_tag
                  + data_version + b"\x00")

    path = os.path.join(RES, "assets", MODID, "ponder", "deepwaters_shrine.nbt")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    # mtime=0: gzip writes the clock into its header, which made every run rewrite this
    # file with identical content and show up as a change in git.
    with gzip.GzipFile(path, "wb", mtime=0) as f:
        f.write(root)
    written.append("assets/bertieprogression/ponder/deepwaters_shrine.nbt")

_write_shrine_nbt()

# ---------------------------------------------------------------- tags

STRIPPED = [f"minecraft:stripped_{w}_log" for w in
            ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry"]] + \
           ["minecraft:stripped_crimson_stem", "minecraft:stripped_warped_stem", "minecraft:stripped_bamboo_block"]
write("data/bertieprogression/tags/item/stripped_logs.json", {"replace": False, "values": STRIPPED})
write("data/bertieprogression/tags/block/stripped_logs.json", {"replace": False, "values": STRIPPED})

# Natural in-world logs only (no stripped/wood variants) — quest-1 detection tag
NATURAL_LOGS = [f"minecraft:{w}_log" for w in
                ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry"]] + \
               ["minecraft:crimson_stem", "minecraft:warped_stem"]
write("data/bertieprogression/tags/item/natural_logs.json", {"replace": False, "values": NATURAL_LOGS})

# "Any meat" for the Aureal ritual. NOT #minecraft:meat — jar-checked, that tag holds ONLY modded
# meats in this pack (jerkies, dragon flesh, meef, crab) and not one vanilla cut, so a player holding
# beef could never finish the ritual. List the vanilla meats explicitly and pull the convention tags
# in optionally (required:false, so a missing tag is not a load error) to pick up modded ones too.
MEATS = [f"minecraft:{m}" for m in
         ["beef", "cooked_beef", "porkchop", "cooked_porkchop", "chicken", "cooked_chicken",
          "mutton", "cooked_mutton", "rabbit", "cooked_rabbit"]]
write("data/bertieprogression/tags/item/meat.json",
      {"replace": False,
       "values": MEATS + [{"id": t, "required": False} for t in
                          ("#minecraft:meat", "#c:foods/raw_meat", "#c:foods/cooked_meat")]})

# Hidden advancement fired by holding 8+ natural logs — the FTB "Get wood" quest task
write("data/bertieprogression/advancement/got_wood.json", {
    "criteria": {
        "got_wood": {
            "trigger": "minecraft:inventory_changed",
            "conditions": {"items": [{"items": "#bertieprogression:natural_logs", "count": {"min": 8}}]},
        }
    }
})

# "Copper tier" minability: vanilla has no native tier
# between stone and iron, and Slag makes copper/bone = stone tier and flint <= stone (jar-verified
# tiers copper/bone/stone=3, flint=2, iron=4), so a true STONE-EXCLUDING copper tier isn't possible
# without custom Tier code + Slag tool overrides. Shipped as a STONE-tier gate: these ores drop out
# of needs_iron_tool into needs_stone_tool -> mineable before iron by stone/copper/bone tools (bone
# reliably; flint depends on Slag's internal mapping). `needs_copper_tool` is a forward marker for
# "add stuff later". Plain stone picks also work (can't be excluded here).
COPPER_TIER_ORES = ["forbidden_arcanus:arcane_crystal_ore", "forbidden_arcanus:deepslate_arcane_crystal_ore",
                    "minecraft:redstone_ore", "minecraft:deepslate_redstone_ore"]
# NeoForge tag remove requires a values key present alongside remove (vanilla TagLoader reads values first).
write("data/minecraft/tags/block/needs_iron_tool.json",
      {"replace": False, "values": [], "remove": COPPER_TIER_ORES})
write("data/minecraft/tags/block/needs_stone_tool.json", {"replace": False, "values": COPPER_TIER_ORES})
write("data/bertieprogression/tags/block/needs_copper_tool.json", {"replace": False, "values": COPPER_TIER_ORES})

# Chapter 1 gear quests detect wooden parts because assembled modular gear has no stable predicate
# surface: no Slag triggers or advancements exist, and assembled component
# values are per-instance). Part component values are jar-verified from Slag-n-Embers 1.1a
# data/slag/recipe/crafting/parts/*_wooden.json; detection is source-agnostic (carved or crafted).
def _wooden_part(part):
    return {
        "trigger": "minecraft:inventory_changed",
        "conditions": {"items": [{
            "items": "slag:dynamic_part",
            "components": {"slag:material_type": "slag:wooden", "slag:part_type": f"slag:{part}"},
        }]},
    }

write("data/bertieprogression/advancement/wooden_armor_set.json", {
    "criteria": {part: _wooden_part(part) for part in ["helmet", "chestplate", "leggings", "boots"]},
    "requirements": [[part] for part in ["helmet", "chestplate", "leggings", "boots"]],
})

write("data/bertieprogression/advancement/wooden_pickaxe_head.json", {
    "criteria": {"head": _wooden_part("pickaxe_head")},
})

# A copper-tier pickaxe may use either a flint or bone head; detect the head part.
def _mat_part(material, part):
    return {
        "trigger": "minecraft:inventory_changed",
        "conditions": {"items": [{
            "items": "slag:dynamic_part",
            "components": {"slag:material_type": f"slag:{material}", "slag:part_type": f"slag:{part}"},
        }]},
    }
write("data/bertieprogression/advancement/copper_pickaxe_head.json", {
    "criteria": {"flint": _mat_part("flint", "pickaxe_head"), "bone": _mat_part("bone", "pickaxe_head")},
    "requirements": [["flint", "bone"]],
})

# §5.5/R30: the Twilight portal accepts ONLY the Twilight Concord (stock tag = diamonds).
write("data/twilightforest/tags/item/portal/activator.json",
      {"replace": True, "values": [{"id": "bertieprogression:twilight_concord", "required": False}]})

# --- Ominous-fire fan processing. Create's four fan types are water, fire,
#     soul fire and lava; this is a fifth, on Twilight Forest's Ominous Fire, registered into
#     CreateRegistries.FAN_PROCESSING_TYPE. A real recipe type rather than a hard-coded pair, so
#     it is data-driven and a recipe viewer can be pointed at it. One entry for now. ---
write(f"{R}/ominous_fan/steeleaf_from_leaves.json",
      {"neoforge:conditions": conds("twilightforest", "create"),
       "type": "bertieprogression:ominous_fan",
       "ingredient": {"tag": "minecraft:leaves"},
       "result": {"id": "twilightforest:steeleaf_ingot", "count": 1}})

# ================================================================ Ur-Ghast Trophy duplication

# --- Ur-Ghast Trophy duplication, on the Spirit Altar. One trophy in, two out: the trophy is the
#     infusion INPUT (Malum consumes it) so the recipe pays for itself once and profits thereafter.
#     Five extraInputs, which spirit_infusion allows - its cap is pedestals reachable in the 4x3x4
#     capture box, NOT the Hephaestus 8-pedestal rule. Blood Vial is irons_spellbooks:blood_vial
#     (jar-verified by lang value + item model), Dragon Bone is iceandfire:dragonbone. ---
write("data/malum/recipe/spirit_infusion/ur_ghast_trophy_dupe.json",
      infusion("twilightforest:ur_ghast_trophy", 1,
               [("minecraft:bone_block", 16), ("irons_spellbooks:blood_vial", 6),
                ("iceandfire:dragonbone", 6), ("minecraft:ghast_tear", 4),
                ("minecraft:fire_charge", 8)],
               [SP("wicked", 6), SP("eldritch", 6), SP("aerial", 6), SP("infernal", 6)],
               "twilightforest:ur_ghast_trophy", 2))

# ---------------------------------------------------------------- assets

ITEMS = {
    "opening_mallet": "Opening Mallet",
    "stone_crucible_blank": "Stone Crucible Blank",
    "stone_pour_channel": "Stone Pour Channel",
    "weeping_eye": "Weeping Eye",
    "kinetic_vane": "Structural Beam",
    "incomplete_structural_beam": "Incomplete Structural Beam",
    "incomplete_small_water_wheel": "Incomplete Water Wheel",
    "incomplete_large_water_wheel": "Incomplete Large Water Wheel",
    "incomplete_diamond_backpack": "Incomplete Diamond Backpack",
    "shield_maiden": "Shield Maiden",
    "acolyte_of_deflection": "Acolyte of Deflection",
    "netherly_meal": "Netherly Meal",
    "sirok_nest_map": "Sirok's Nest Map",
    "kraken_ship_map": "Kraken Ship Map",
    "yeti_hideout_map": "Skor's Hideout Map",
    "abyssal_core": "Abyssal Core",
    "desert_core": "Desert Core",
    "cursed_core": "Cursed Core",
    "storm_core": "Storm Core",
    "kinetic_pattern_plate": "Kinetic Pattern Plate",
    "crafting_license": "Crafting License",
    "twilight_concord": "Twilight Concord",
    "spirit_focused_echo": "Spirit-Focused Echo",
    "null_blaze_cube": "Null Blaze Cube",
    "descent_anchor": "Descent Anchor",
    "boss_rematch_seal": "Boss Rematch Seal",
    "innocent_soul": "Innocent Soul",
}

BLOCKS = {
}

# item models
for item_id in ITEMS:
    write(f"assets/bertieprogression/models/item/{item_id}.json",
          {"parent": "minecraft:item/generated", "textures": {"layer0": f"bertieprogression:item/{item_id}"}})
# The abyssal core sprite is 48x48 and renders at scale 3 so a texel lands on a GUI pixel; the
# whirlpool then fills its slot and the tentacle reaches into the neighbouring ones. Render size is
# set by the model, not the texture, so this transform has to survive the ITEMS loop above.
write("assets/bertieprogression/models/item/abyssal_core.json",
      {"parent": "minecraft:item/generated",
       "textures": {"layer0": "bertieprogression:item/abyssal_core"},
       "display": {"gui": {"scale": [3, 3, 1]}}})
# weeping_eye is NOT overridden here any more: it has real art now
# (texture-work/make_weeping_eye.py, animated) and takes the generated model the
# ITEMS loop above writes. Re-adding it would hide that texture behind vanilla's
# Eye of Ender, the same model-parent trap described for storm_core below.
# Crafting License borrows vanilla's paper model (no bespoke texture yet).
write("assets/bertieprogression/models/item/crafting_license.json", {"parent": "minecraft:item/paper"})
# Finder maps use vanilla's empty-map model; no bespoke textures are registered.
for _m in ("sirok_nest_map", "kraken_ship_map", "yeti_hideout_map"):
    write(f"assets/bertieprogression/models/item/{_m}.json", {"parent": "minecraft:item/map"})
# These cores have no bespoke texture yet - each borrows a vanilla item that reads close to
# its element, so they are at least distinguishable on sight. storm_core is NOT in this list:
# it has real art (texture-work/make_storm_core.py) and takes the generated model written by
# the ITEMS loop above. Re-adding it here would hide that texture behind an amethyst shard.
# abyssal_core has real animated art (the reach variant with its .mcmeta), so it takes the generated
# model from the ITEMS loop like storm_core. Parenting it to a Heart of the Sea would hide the
# texture.
# desert_core left this list when it got its own animated sprite; only cursed_core still borrows.
for _c, _par in (("cursed_core", "minecraft:item/echo_shard"),):
    write(f"assets/bertieprogression/models/item/{_c}.json", {"parent": _par})
# Transitional sequenced-assembly items: the beam reuses a vanilla stick, while incomplete water
# wheels reuse the corresponding finished wheel models.
write("assets/bertieprogression/models/item/incomplete_structural_beam.json",
      {"parent": "minecraft:item/generated", "textures": {"layer0": "minecraft:item/stick"}})
write("assets/bertieprogression/models/item/incomplete_small_water_wheel.json", {"parent": "create:item/water_wheel"})
write("assets/bertieprogression/models/item/incomplete_large_water_wheel.json", {"parent": "create:item/large_water_wheel"})
write("assets/bertieprogression/models/item/incomplete_diamond_backpack.json",
      {"parent": "minecraft:item/generated", "textures": {"layer0": "minecraft:item/diamond"}})

# block models + blockstates + block items
for block_id in BLOCKS:
    write(f"assets/bertieprogression/models/block/{block_id}.json",
          {"parent": "minecraft:block/cube_all", "textures": {"all": f"bertieprogression:block/{block_id}"}})
    write(f"assets/bertieprogression/blockstates/{block_id}.json",
          {"variants": {"": {"model": f"bertieprogression:block/{block_id}"}}})
    write(f"assets/bertieprogression/models/item/{block_id}.json",
          {"parent": f"bertieprogression:block/{block_id}"})

# lang
lang = {"itemGroup.bertieprogression": "Bertie Progression"}
for item_id, name in ITEMS.items():
    lang[f"item.bertieprogression.{item_id}"] = name
for block_id, name in BLOCKS.items():
    lang[f"block.bertieprogression.{block_id}"] = name
lang.update({
    "message.bertieprogression.table_unlicensed": "You do not know how to use a crafting grid yet - consume a Crafting License first.",
    "message.bertieprogression.crafting_licensed": "The crafting language settles into your hands. The 3x3 grid is yours.",
    "message.bertieprogression.already_licensed": "You already hold the crafting language.",
    # Altar of Amethyst: none of its new conditions are discoverable in game, and Cataclysm ships
    # no .desc key for the block, so this goes on as a tooltip line (AltarTooltipHandler).
    "tooltip.bertieprogression.altar_of_amethyst": "Works best under a full moon, or in lush caves.",
    "message.bertieprogression.forge_formed": "The Brick Forge roars to life!",
    "message.bertieprogression.pedestal_formed": "The darkstone column settles into a pedestal.",
    # Ponder scene text. Ponder does NOT fall back to the literal passed to .text(...) — it looks up
    # "<modid>.ponder.<sceneId>.header" / ".text_N", numbered in the order the showText calls run.
    # Without these the scene shows raw lang keys. Keep in step with ShrinePonderPlugin.
    "bertieprogression.ponder.deepwaters_shrine.header": "Raising the Deep Waters Shrine",
    "bertieprogression.ponder.deepwaters_shrine.text_1": "Seven by seven of Mossy Stone Bricks. Build it underwater, in the Deep Waters - nowhere else works.",
    "bertieprogression.ponder.deepwaters_shrine.text_2": "A Flaming Opal Pillar at the centre, wrapped in a solid three by three, with four posts on the diagonals.",
    "bertieprogression.ponder.deepwaters_shrine.text_3": "Aquamarine crystals ring the edge - Small at the corners of each face, a Bundle in the middle.",
    "bertieprogression.ponder.deepwaters_shrine.text_4": "The Conduit sits at the very centre, held in a diagonal lattice. This is the heart of the shrine.",
    "bertieprogression.ponder.deepwaters_shrine.text_5": "Above the Conduit, the pillar and the posts repeat - but no crystals this time.",
    "bertieprogression.ponder.deepwaters_shrine.text_6": "Cap it with a second seven by seven roof.",
    "bertieprogression.ponder.deepwaters_shrine.text_7": "Crown it with crystals. This layer is NOT symmetrical - copy it exactly. The centre stays empty.",
    "bertieprogression.ponder.deepwaters_shrine.text_8": "Any rotation works. Leave water around the shrine and a clear column above it, or nothing will happen.",
    "bertieprogression.ponder.deepwaters_shrine.text_9": "Use a Crowned Jelly on the Conduit.",
    "bertieprogression.ponder.deepwaters_shrine.text_10": "The shrine floods, and a Stormcall Altar rises on a pyramid of Polished Azure Seastone where the Conduit stood.",
    "message.bertieprogression.shrine_no_space": "not enough space",
    "message.bertieprogression.shrine_formed": "The shrine floods, and the Stormcall Altar rises.",
    "message.bertieprogression.paper_need_cane": "You need at least 3 Sugar Cane to press paper.",
    "message.bertieprogression.paper_need_slates": "You need two Wood Slates in your inventory to press paper.",
    "message.bertieprogression.no_imbrifer": "Imbrifer (pastel:deeper_down) is not present in this world.",
    "message.bertieprogression.descended": "The anchor drags you down into Imbrifer.",
    "message.bertieprogression.locator_searching": "The needle spins, searching...",
    "message.bertieprogression.locator_missing": "Target structure %s is not present in this world.",
    "message.bertieprogression.locator_none": "No target found within range.",
    "message.bertieprogression.locator_found": "%s located near X=%s, Z=%s.",
    # FinderItem uses separate keys from the locator because a finder consumes itself into a map.
    "message.bertieprogression.finder_searching": "The chart darkens, reading the land...",
    "message.bertieprogression.finder_missing": "Nothing like %s exists in this world.",
    "message.bertieprogression.finder_none": "Nothing within range. Carry the chart further and try again.",
    "message.bertieprogression.finder_found": "The chart is now a map. X=%s, Z=%s.",
    "message.bertieprogression.nether_locked": "The heat refuses you. Eat a Netherly Meal first.",
    "item.bertieprogression.sirok_nest_map.filled": "Map to Sirok's Nest",
    "item.bertieprogression.kraken_ship_map.filled": "Map to the Kraken's Ship",
    "item.bertieprogression.yeti_hideout_map.filled": "Map to Skor's Hideout",
})
write("assets/bertieprogression/lang/en_us.json", lang)

# A data override only wins if this mod loads AFTER the one it overrides, and when it does not,
# nothing complains - the file is just ignored. Magitech's bench recipes and TerraCurio's disables
# both shipped broken that way, so the manifest is checked against what we actually write.
_TOML = os.path.join(ROOT, "src", "main", "templates", "META-INF", "neoforge.mods.toml")
_declared = set()
for _blk in io.open(_TOML, encoding="utf-8").read().split("[[dependencies.")[1:]:
    _m = re.search(r'modId="([a-z0-9_]+)"', _blk)
    if _m and 'ordering="AFTER"' in _blk:
        _declared.add(_m.group(1))
# minecraft and neoforge always load first; `c` and `forge` are shared tag namespaces nobody owns.
_own = {MODID, "minecraft", "neoforge", "c", "forge"}
_overridden = {p.split("/")[1] for p in written if p.startswith("data/") and "/" in p[5:]}
_undeclared = sorted(_overridden - _declared - _own)
if _undeclared:
    raise SystemExit(
        "these namespaces get data written for them but are not declared with ordering=\"AFTER\" "
        "in neoforge.mods.toml, so the original files would win: " + ", ".join(_undeclared))

print(f"Wrote {len(written)} files under {RES}")
