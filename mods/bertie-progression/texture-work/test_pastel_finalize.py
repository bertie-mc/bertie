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

            put(module / "texture-work/pastel_content_policy.json", {})
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
                "data/pastel/recipe/netherite.json": {"type": "pastel:fusion_shrine",
                                                      "result": {"id": "minecraft:netherite_ingot"}},
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
            self.assertEqual(json.loads((resources / "data/pastel/recipe/netherite.json").read_text()), DISABLED)
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
