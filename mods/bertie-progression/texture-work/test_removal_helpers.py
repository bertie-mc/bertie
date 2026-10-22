"""Loot pruning handles both item IDs and structured text fields."""

import ast
import unittest
from pathlib import Path


class LootPruningTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        tree = ast.parse(Path(__file__).with_name("gen_data.py").read_text(encoding="utf-8"))
        function = next(node for node in tree.body if isinstance(node, ast.FunctionDef) and node.name == "_strip_loot")
        namespace = {}
        exec(compile(ast.Module(body=[function], type_ignores=[]), "gen_data.py", "exec"), namespace)
        cls.prune = staticmethod(namespace["_strip_loot"])

    def test_structured_names_are_preserved(self):
        value = {"function": "minecraft:set_name", "name": {"text": "Packed lunch"}}
        self.assertEqual(value, self.prune(value, {"pastel:hot_chocolate"}))

    def test_removed_item_entries_are_cut(self):
        value = {"pools": [{"entries": [{"type": "minecraft:item", "name": "pastel:hot_chocolate"}]}]}
        self.assertEqual({"pools": []}, self.prune(value, {"pastel:hot_chocolate"}))

    def test_other_items_survive(self):
        value = {"type": "minecraft:item", "name": "minecraft:apple"}
        self.assertEqual(value, self.prune(value, {"pastel:hot_chocolate"}))


if __name__ == "__main__":
    unittest.main()
