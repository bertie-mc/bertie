"""Regression cases for item-tag repairs across material merges and optional integrations."""

import json
import tempfile
import unittest
from pathlib import Path

from recipe_cleanup import repair_ingredients


class IngredientRepairTest(unittest.TestCase):
    def run_repair(self, root, recipe, item_tags=None, block_tags=None, merged=None, present=None):
        resources = root / "resources"
        (root / "texture-work").mkdir(exist_ok=True)
        resources.mkdir(exist_ok=True)
        path = "data/example/recipe/test.json"
        emitted = {}

        def write(name, data):
            emitted[name] = data
            target = resources / name
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(json.dumps(data), encoding="utf-8")

        merged = merged or {}
        repair_ingredients(resources, root, write, [], {path: recipe}, item_tags or {}, block_tags or {},
                           merged, set(merged), {"retained:dust", "minecraft:oak_log"},
                           present or {"example"}, {"neoforge:conditions": [{"type": "neoforge:false"}]}, set())
        issues = json.loads((root / "texture-work/ingredient_issues.json").read_text(encoding="utf-8"))
        return emitted, issues

    def test_merge_replaces_empty_item_tag_without_polluting_gem_tags(self):
        with tempfile.TemporaryDirectory() as tmp:
            result, issues = self.run_repair(Path(tmp), {
                "type": "example:processing", "ingredients": [{"tag": "c:gems/sulfur"}],
                "fluid_ingredient": {"amount": 1000, "tag": "c:water"}, "result": {"id": "example:output"}},
                {"data/c/tags/item/gems/sulfur.json": [{"values": ["removed:sulfur"]}]},
                merged={"removed:sulfur": "retained:dust"})
            self.assertEqual([], issues)
            fixed = result["data/example/recipe/test.json"]
            self.assertEqual("c:water", fixed["fluid_ingredient"]["tag"])
            alias = fixed["ingredients"][0]["tag"]
            self.assertTrue(alias.startswith("bertieprogression:retained/"))
            self.assertEqual(["retained:dust"], result[
                "data/bertieprogression/tags/item/" + alias.split(":", 1)[1] + ".json"]["values"])
            self.assertNotIn("data/c/tags/item/gems/sulfur.json", result)

    def test_block_only_tag_supplies_existing_block_items(self):
        with tempfile.TemporaryDirectory() as tmp:
            result, issues = self.run_repair(Path(tmp), {
                "type": "minecraft:smoking", "ingredient": {"tag": "burnt:logs"}, "result": {"id": "example:output"}},
                block_tags={"data/burnt/tags/block/logs.json": [{"values": ["minecraft:oak_log"]}]})
            self.assertEqual([], issues)
            self.assertIn("data/example/recipe/test.json", result)

    def test_absent_optional_mod_does_not_create_fake_ingredients(self):
        with tempfile.TemporaryDirectory() as tmp:
            result, issues = self.run_repair(Path(tmp), {
                "neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": "absent"}],
                "type": "example:processing", "ingredient": {"tag": "absent:metal"}, "result": {"id": "example:output"}})
            self.assertEqual({}, result)
            self.assertEqual([], issues)

    def test_merge_repairs_tag_condition_as_well_as_ingredient(self):
        with tempfile.TemporaryDirectory() as tmp:
            result, issues = self.run_repair(Path(tmp), {
                "neoforge:conditions": [{"type": "neoforge:not", "value": {
                    "type": "neoforge:tag_empty", "tag": "c:old_material"}}],
                "type": "example:processing", "ingredient": {"tag": "c:old_material"},
                "result": {"id": "example:output"}},
                {"data/c/tags/item/old_material.json": [{"values": ["removed:sulfur"]}]},
                merged={"removed:sulfur": "retained:dust"})
            self.assertEqual([], issues)
            recipe = result["data/example/recipe/test.json"]
            self.assertEqual(recipe["ingredient"]["tag"], recipe["neoforge:conditions"][0]["value"]["tag"])


if __name__ == "__main__":
    unittest.main()
