"""Machine cores and the retained Nether fusion progression."""

import copy


def apply_recipes(write, recipes, disabled, ritual):
    for name in ("alt_phantom_frame", "alt_glow_phantom_frame"):
        write(f"data/pastel/recipe/mod_integration/malum/pedestal/{name}.json", disabled)
    path = "data/packagedmekemicals/recipe/chemical_package_filler.json"
    filler = copy.deepcopy(recipes[path])
    filler["result"]["id"] = filler["result"].pop("item")
    for ingredient in filler["key"].values():
        if ingredient.get("tag", "").startswith("forge:"):
            ingredient["tag"] = "c:" + ingredient["tag"][6:]
    write(path, filler)

    for path, recipe in recipes.items():
        result = recipe.get("result", {})
        output = result.get("id", result.get("item")) if isinstance(result, dict) else result
        if recipe.get("type") == "pastel:fusion_shrine" and (
                isinstance(output, str) and output.endswith("_horse_armor")
                or output == "minecraft:netherite_ingot"):
            write(path, disabled)

    shrine = "data/pastel/recipe/pedestal/tier2/fusion_shrine_basalt.json"
    write(shrine, disabled)
    write("data/bertieprogression/forbidden_arcanus/hephaestus_forge/ritual/fusion_shrine.json",
          ritual("minecraft:polished_basalt", [("anvilcraft:ruby", 2), ("anvilcraft:topaz", 2),
                 ("anvilcraft:sapphire", 2), ("pastel:vegetal", 2)],
                 "pastel:fusion_shrine_basalt", tier=2,
                 essences={"aureal": 800, "blood": 0, "souls": 20}, xp=1000))

    for level in range(1, 8):
        path = f"data/oritech/recipe/crafting/core{level}.json"
        recipe = copy.deepcopy(recipes[path])
        side = recipe["key"]["s"]
        center = {"item": "mythsandlegends:bound_soul_ingot" if level == 1
                  else f"oritech:machine_core_{level - 1}"}
        corner = copy.deepcopy(side)
        if level == 2:
            side = corner = {"tag": "c:ingots/steel"}
        elif level == 3:
            corner = {"item": "oritech:nickel_ingot"}
            side = {"item": "oritech:carbon_fibre_strands"}
        elif level in (4, 5, 6):
            corner = {"item": {4: "oritech:enderic_compound", 5: "oritech:advanced_computing_engine",
                                6: "oritech:dubios_container"}[level]}
        elif level == 7:
            side = {"item": "oritech:superconductor"}
        recipe["pattern"] = ["KSK", "SCS", "KSK"]
        recipe["key"] = {"K": corner, "S": side, "C": center}
        write(path, recipe)
    for level in (2, 3):
        write(f"data/oritech/recipe/crafting/core{level}alt.json", disabled)
