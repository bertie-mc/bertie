"""Upgrade crafting must preserve spare-slot costs and survive repeated generation."""

from collections import Counter
import unittest

from technology_progression import with_predecessor


class PredecessorRecipeTest(unittest.TestCase):
    def test_spare_slot_preserves_the_original_dragon_head_cost(self):
        original = {
            "type": "minecraft:crafting_shaped",
            "pattern": ["DA ", "CBA", "DA "],
            "key": {"A": {"item": "minecraft:netherite_ingot"},
                    "B": {"item": "minecraft:dragon_head"},
                    "C": {"item": "minecraft:redstone"},
                    "D": {"item": "minecraft:cobblestone"}},
            "result": {"id": "anvilcraft:block_devourer"},
        }
        upgraded = with_predecessor(original, "pastel:block_breaker")
        costs = Counter(upgraded["key"][symbol]["item"]
                        for row in upgraded["pattern"] for symbol in row if symbol != " ")
        self.assertEqual({"minecraft:netherite_ingot": 3, "minecraft:dragon_head": 1,
                          "minecraft:redstone": 1, "minecraft:cobblestone": 2,
                          "pastel:block_breaker": 1}, costs)
        self.assertEqual("minecraft:dragon_head", original["key"]["B"]["item"])

    def test_repeated_generation_does_not_add_a_second_pump(self):
        original = {
            "type": "minecraft:crafting_shaped", "pattern": [" B ", "AXA", "OOO"],
            "key": {"A": {"tag": "mekanism:alloys/infused"},
                    "B": {"item": "minecraft:bucket"},
                    "O": {"tag": "c:ingots/osmium"},
                    "X": {"item": "mekanism:steel_casing"}},
            "result": {"id": "mekanism:electric_pump"},
        }
        once = with_predecessor(original, "oritech:pump_block")
        self.assertEqual(once, with_predecessor(once, "oritech:pump_block"))
        self.assertEqual(1, sum(once["key"][symbol].get("item") == "oritech:pump_block"
                                for row in once["pattern"] for symbol in row if symbol != " "))


if __name__ == "__main__":
    unittest.main()
