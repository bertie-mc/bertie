"""Regression cases for implicit equipment outputs and typed non-item results."""

import unittest
from recipe_outputs import item_outputs


class ItemOutputsTest(unittest.TestCase):
    def test_shapeless_traveller_modifier_copies_vest(self):
        recipe = {"type": "twilightforest:travellers_gear_modifier_shapeless_recipe",
                  "ingredients": [{"item": "minecraft:chicken"}, {"item": "twilightforest:travellers_vest"}]}
        self.assertEqual({"twilightforest:travellers_vest"}, item_outputs(recipe, set()))

    def test_shaped_traveller_modifier_copies_equipment(self):
        recipe = {"type": "twilightforest:travellers_gear_modifier_shaped_recipe",
                  "pattern": {"key": {"B": {"item": "twilightforest:travellers_boots"}, "S": {"item": "minecraft:sugar"}},
                              "pattern": ["SBS"]}}
        self.assertEqual({"twilightforest:travellers_boots"}, item_outputs(recipe, set()))

    def test_vest_glove_merger_has_implicit_vest_output(self):
        self.assertEqual({"twilightforest:travellers_vest"}, item_outputs({
            "type": "twilightforest:travellers_vest_gloves_merge_recipe"}, set()))

    def test_tool_material_result_is_not_an_item(self):
        self.assertEqual(set(), item_outputs({"type": "magitech:tool_material", "result": "magitech:fluorite"}, set()))

    def test_multiple_item_outputs_are_all_detected(self):
        self.assertEqual({"minecraft:lapis_lazuli", "anvilcraft:tin_nugget"}, item_outputs({
            "type": "anvilcraft:mesh", "results": [{"id": "minecraft:lapis_lazuli"}, {"id": "anvilcraft:tin_nugget"}]}, set()))

    def test_nested_type_descriptors_do_not_break_recursion(self):
        self.assertEqual({"minecraft:stone"}, item_outputs({"conditions": {"type": {"tag": "test:kind"}},
            "result": {"id": "minecraft:stone"}}, set()))


if __name__ == "__main__":
    unittest.main()
