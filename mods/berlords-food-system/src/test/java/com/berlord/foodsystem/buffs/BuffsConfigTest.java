package com.berlord.foodsystem.buffs;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuffsConfigTest {

    @Test
    void parsesEffectsAttributesAbilitiesAndCategory() {
        BuffsConfig.applyJson("""
                {"foods":{"minecraft:apple":{
                  "category":"fruit",
                  "effects":[{"id":"minecraft:speed","amplifier":1}],
                  "attributes":[{"id":"minecraft:generic.max_health","amount":4,
                    "operation":"add_multiplied_base"}],
                  "abilities":{"flight":true,"climbing":true,"enderman_calm":true,
                    "magnet":6,"xp_boost":1.5,"durability_saver":0.3}
                }}}
                """);

        BuffsConfig.FoodBuff apple = BuffsConfig.get().foods.get(Items.APPLE);
        assertEquals("fruit", apple.category);
        assertEquals(1, apple.effects.size());
        assertEquals(1, apple.effects.getFirst().amplifier());
        assertEquals(1, apple.attributes.size());
        assertEquals(AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                apple.attributes.getFirst().modifier().operation());
        assertTrue(apple.abilities.flight);
        assertTrue(apple.abilities.climbing);
        assertTrue(apple.abilities.endermanCalm);
        assertEquals(6, apple.abilities.magnetRadius);
        assertEquals(1.5, apple.abilities.xpBoost);
        assertEquals(0.3, apple.abilities.durabilitySaver);
    }

    @Test
    void invalidReloadKeepsThePreviousRuntime() {
        BuffsConfig.applyJson("{\"foods\":{\"minecraft:apple\":{}}}");
        BuffsConfig.Runtime valid = BuffsConfig.get();
        BuffsConfig.applyJson("not json");
        assertSame(valid, BuffsConfig.get());
    }
}
