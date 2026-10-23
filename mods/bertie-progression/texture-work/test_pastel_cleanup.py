import copy
import json
import tempfile
import unittest
from pathlib import Path

from pastel_cleanup import apply_policy


class PastelCleanupTests(unittest.TestCase):
    def run_policy(self, root, recipes, removed):
        emitted = {}

        def write(path, value):
            emitted[path] = copy.deepcopy(value)
            target = root / path
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(json.dumps(value), encoding="utf-8")

        apply_policy(write, recipes, root, [], removed,
                     {"neoforge:conditions": [{"type": "neoforge:false"}]})
        return emitted

    def test_unrelated_removed_material_does_not_disable_foreign_recipe(self):
        path = "data/other/recipe/upgrade.json"
        recipe = {"type": "minecraft:crafting_shapeless",
                  "ingredients": [{"item": "other:retired_material"}],
                  "result": {"id": "minecraft:diamond"}}
        with tempfile.TemporaryDirectory() as directory:
            emitted = self.run_policy(Path(directory), {path: recipe}, {"other:retired_material"})
        self.assertNotIn(path, emitted)

    def test_light_keeps_pattern_and_output_when_ink_and_retired_material_are_replaced(self):
        path = "data/pastel/recipe/lamp.json"
        recipe = {"type": "pastel:pedestal", "colors": {"pastel:magenta": 4},
                  "pattern": ["TTT", "BSB", "TTT"],
                  "key": {"T": "pastel:polished_calcite", "B": "minecraft:amethyst_block",
                          "S": "pastel:shimmerstone_block"},
                  "result": {"id": "pastel:amethyst_calcite_light", "count": 4}}
        with tempfile.TemporaryDirectory() as directory:
            emitted = self.run_policy(Path(directory), {path: recipe}, {"pastel:shimmerstone_block"})
        converted = emitted[path]
        self.assertEqual(converted["type"], "minecraft:crafting_shaped")
        self.assertEqual(converted["pattern"], recipe["pattern"])
        self.assertEqual(converted["result"], recipe["result"])
        self.assertEqual(converted["key"]["S"], {"item": "minecraft:glowstone"})
        self.assertNotIn("colors", converted)

    def test_restoring_ingredient_restores_previously_suppressed_recipe(self):
        path = "data/pastel/recipe/optional.json"
        recipe = {"type": "minecraft:crafting_shapeless",
                  "ingredients": [{"item": "pastel:optional_material"}],
                  "result": {"id": "pastel:optional_result"}}
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            first = self.run_policy(root, {path: recipe}, {"pastel:optional_material"})
            self.assertIn({"type": "neoforge:false"}, first[path]["neoforge:conditions"])
            restored = self.run_policy(root, {path: recipe}, set())
            self.assertEqual(restored[path]["ingredients"], recipe["ingredients"])
            self.assertNotIn({"type": "neoforge:false"}, restored[path]["neoforge:conditions"])


if __name__ == "__main__":
    unittest.main()
