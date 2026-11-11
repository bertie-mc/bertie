import json
import tempfile
import unittest
import zipfile
from pathlib import Path

from pastel_finalize import DISABLED, clean_feature, finalize


class PastelFinalizationTests(unittest.TestCase):
    def test_removing_a_rare_plant_preserves_the_other_plant_in_its_patch(self):
        provider = {"type": "minecraft:weighted_state_provider", "entries": [
            {"data": {"Name": "pastel:clover"}, "weight": 15},
            {"data": {"Name": "pastel:four_leaf_clover"}, "weight": 1},
        ]}
        cleaned = clean_feature(provider, {"pastel:four_leaf_clover"})
        self.assertEqual(cleaned["entries"], [{"data": {"Name": "pastel:clover"}, "weight": 15}])

    def test_restoration_removal_and_authored_recipes_remain_consistent(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            module, workspace = root / "module", root / "workspace"
            resources = module / "src/main/resources"

            def put(path, value):
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(json.dumps(value), encoding="utf-8")

            name_tag = "data/pastel/recipe/pedestal/tier2/vanilla/name_tag.json"
            custom_netherite = "data/pastel/recipe/fusion_shrine/netherite_ingot.json"
            native_netherite = "data/pastel/recipe/fusion_shrine/vanilla/netherite_ingot.json"
            native_horse = "data/pastel/recipe/fusion_shrine/vanilla/diamond_horse_armor.json"
            custom_horse = "data/bertieprogression/recipe/custom_horse_armor.json"
            put(module / "texture-work/pastel_content_policy.json", {"restore_recipe_paths": [name_tag]})
            put(module / "texture-work/pastel_worldgen_policy.json", {
                "configured_features": ["shimmerstone_ore"], "biome_modifiers": ["shimmerstone_ore"],
                "fluid_replacements": {"pastel:dragonrot": "minecraft:lava"}})
            put(resources / "removed_items.json", ["pastel:bismuth_crystal", "pastel:shimmerstone_gem"])
            doc = workspace / "docs/removed/pastel.md"
            doc.parent.mkdir(parents=True)
            doc.write_text("| Shimmerstone | `pastel:shimmerstone_gem` | removed | date |\n"
                           "| Ore | `pastel:shimmerstone_ore` | removed | date |\n", encoding="utf-8")

            recipe = "data/pastel/recipe/bismuth_block.json"
            loot = "data/pastel/loot_table/blocks/bismuth.json"
            tag = "data/pastel/tags/item/materials.json"
            onyx = "data/pastel/recipe/onyx.json"
            dragonrot_lake = "data/pastel/worldgen/configured_feature/lake_dragonrot.json"
            exposed_fossil = "data/pastel/worldgen/placed_feature/dragon_fossil_exposed.json"
            conversion = "data/pastel/recipe/dragonrot_converting/blackslag.json"
            extractor = "data/pastel/recipe/mod_integration/xycraft/slush.json"
            put(resources / recipe, DISABLED)
            put(resources / loot, {"type": "minecraft:block", "pools": []})
            put(resources / tag, {"replace": True, "values": []})
            authored = {"type": "minecraft:crafting_shapeless",
                        "ingredients": [{"item": "minecraft:emerald"}], "result": {"id": "pastel:onyx_shard"}}
            put(resources / onyx, authored)
            custom_fusion = {"type": "pastel:fusion_shrine", "time": 400, "experience": 4,
                             "fluid": {"fluid": "slag:molten_gold"}, "ingredients": [
                                 "hazennstuff:rose_gold_ingot", {"item": "minecraft:netherite_scrap", "count": 4},
                                 {"item": "pastel:neolith", "count": 2}],
                             "result": {"id": "minecraft:netherite_ingot", "count": 1}}
            custom_horse_fusion = {"type": "pastel:fusion_shrine", "ingredients": ["minecraft:diamond_block"],
                                   "result": {"id": "minecraft:diamond_horse_armor"}}
            put(resources / custom_netherite, custom_fusion)
            put(resources / custom_horse, custom_horse_fusion)
            put(resources / name_tag, DISABLED)
            put(resources / "pastel_recipe_pending.json", [{"path": recipe}])
            put(module / "texture-work/.removed_data.json", [loot])
            put(module / "texture-work/.merged_tags.json", [tag])
            stock = {
                recipe: {"type": "pastel:pedestal", "pattern": ["BB"],
                         "key": {"B": "pastel:bismuth_crystal"}, "result": {"id": "pastel:bismuth_block"}},
                loot: {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
                    {"type": "minecraft:item", "name": "pastel:bismuth_crystal"},
                    {"type": "minecraft:item", "name": "pastel:shimmerstone_gem"}]}]},
                tag: {"values": ["pastel:bismuth_crystal", "pastel:shimmerstone_gem"]},
                onyx: {"type": "minecraft:crafting_shapeless", "ingredients": [{"item": "minecraft:dirt"}],
                       "result": {"id": "pastel:onyx_shard"}},
                "data/pastel/recipe/enchanter.json": {"type": "pastel:enchanter",
                                                       "result": {"id": "minecraft:enchanted_book"}},
                native_netherite: {"type": "pastel:fusion_shrine", "result": {"id": "minecraft:netherite_ingot"}},
                native_horse: {"type": "pastel:fusion_shrine", "result": {"id": "minecraft:diamond_horse_armor"}},
                name_tag: {"type": "pastel:pedestal_shapeless", "ingredients": ["minecraft:string", "pastel:shimmerstone_gem"],
                           "result": {"id": "minecraft:name_tag"}},
                "data/pastel/worldgen/configured_feature/shimmerstone_ore.json": {
                    "type": "minecraft:ore", "config": {"targets": [{"state": {"Name": "pastel:shimmerstone_ore"}}]}},
                dragonrot_lake: {"type": "minecraft:lake", "config": {
                    "fluid": {"state": {"Name": "pastel:dragonrot", "Properties": {"level": "0"}}},
                    "barrier": {"state": {"Name": "pastel:hornslake"}}}},
                exposed_fossil: {"feature": "pastel:dragon_fossil", "placement": [{
                    "type": "minecraft:environment_scan", "target_condition": {
                        "type": "minecraft:matching_blocks", "blocks": ["pastel:dragonrot", "pastel:hornslake"]}}]},
                conversion: {"type": "pastel:dragonrot_converting", "ingredient": [{"item": "minecraft:deepslate"}],
                             "result": {"id": "pastel:blackslag"}},
                extractor: {"type": "xycraft_machines:extractor", "predicates": [
                    {"predicate_type": "xycraft_core:fluid_type_rule", "fluid_type": "pastel:dragonrot"}],
                    "output": {"id": "pastel:slush"}},
            }
            archive = root / "pastel.jar"
            with zipfile.ZipFile(archive, "w") as jar:
                for path, value in stock.items():
                    jar.writestr(path, json.dumps(value))

            finalize(module, workspace, [archive])
            self.assertEqual(json.loads((resources / onyx).read_text()), authored)
            self.assertEqual(json.loads((resources / recipe).read_text())["type"], "minecraft:crafting_shaped")
            self.assertIn("pastel:bismuth_crystal", (resources / loot).read_text())
            self.assertNotIn("pastel:shimmerstone_gem", (resources / loot).read_text())
            self.assertEqual(json.loads((resources / tag).read_text())["values"],
                             [{"id": "pastel:bismuth_crystal", "required": False}])
            self.assertEqual(json.loads((resources / "data/pastel/recipe/enchanter.json").read_text()), DISABLED)
            self.assertEqual(json.loads((resources / native_netherite).read_text()), DISABLED)
            self.assertEqual(json.loads((resources / native_horse).read_text()), DISABLED)
            self.assertEqual(json.loads((resources / custom_netherite).read_text()), custom_fusion)
            self.assertEqual(json.loads((resources / custom_horse).read_text()), custom_horse_fusion)
            self.assertEqual(json.loads((resources / name_tag).read_text())["ingredients"],
                             [{"item": "minecraft:string"}, {"item": "minecraft:glowstone"}])
            self.assertEqual(json.loads((resources / "data/pastel/worldgen/configured_feature/shimmerstone_ore.json").read_text())["type"],
                             "minecraft:no_op")
            lake_config = json.loads((resources / dragonrot_lake).read_text())["config"]
            self.assertEqual(lake_config["fluid"]["state"], {"Name": "minecraft:lava", "Properties": {"level": "0"}})
            self.assertEqual(lake_config["barrier"]["state"]["Name"], "pastel:hornslake")
            fossil = json.loads((resources / exposed_fossil).read_text())
            self.assertEqual(fossil["feature"], "pastel:dragon_fossil")
            self.assertEqual(fossil["placement"][0]["target_condition"]["blocks"],
                             ["minecraft:lava", "pastel:hornslake"])
            self.assertEqual(json.loads((resources / conversion).read_text()), DISABLED)
            self.assertEqual(json.loads((resources / extractor).read_text()), DISABLED)
            before = {path.relative_to(root): path.read_bytes() for path in module.rglob("*.json")}
            finalize(module, workspace, [archive])
            self.assertEqual(before, {path.relative_to(root): path.read_bytes() for path in module.rglob("*.json")})


if __name__ == "__main__":
    unittest.main()
