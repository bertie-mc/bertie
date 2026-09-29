package io.github.bertie_mc.bertieprogression.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CombatMathTest {
    @Test
    void protectionMeetsChosenBreakpointsWithoutAGemCap() {
        assertEquals(1.0, CombatMath.protectionRemaining(0), 1e-12);
        assertEquals(0.1, CombatMath.protectionRemaining(40), 1e-12);
        assertEquals(0.01, CombatMath.protectionRemaining(80), 1e-12);
        assertTrue(CombatMath.protectionRemaining(48) < 0.1);
        assertEquals(1.0, CombatMath.protectionRemaining(-10), 1e-12);
        for (int points = 0; points <= 80; points++) {
            assertEquals(
                    0.1, CombatMath.protectionRemaining(points + 40) / CombatMath.protectionRemaining(points), 1e-12);
        }
    }

    @Test
    void largeHitsHaveABoundedArmorPenalty() {
        assertEquals(8, CombatMath.armorPressure(1), 1e-12);
        assertEquals(8, CombatMath.armorPressure(20), 1e-12);
        assertEquals(12, CombatMath.armorPressure(60), 1e-12);
        assertEquals(2.0 / 7, CombatMath.armorRemaining(20, 20), 1e-12);
        assertTrue(CombatMath.armorRemaining(1_000_000, 20) < 16.0 / 36);
        assertEquals(1, CombatMath.armorRemaining(50, 0), 1e-12);
        for (int damage = 1; damage <= 200; damage++) {
            assertTrue(CombatMath.armorRemaining(damage, 40) < CombatMath.armorRemaining(damage, 20));
        }
    }

    @Test
    void toughnessCountersBothFlatAndPercentageArmorPenalties() {
        assertEquals(4, CombatMath.counterArmorPenalty(5, 10), 1e-12);
        assertEquals(0.16, CombatMath.counterArmorPenalty(0.2, 10), 1e-12);
        assertEquals(0.08, CombatMath.counterArmorPenalty(0.2, 30), 1e-12);
        assertEquals(0.04, CombatMath.counterArmorPenalty(0.2, 40), 1e-12);
        assertEquals(2, CombatMath.counterArmorPenalty(10, 40), 1e-12);
        assertEquals(0, CombatMath.counterArmorPenalty(0.2, 50), 1e-12);
        assertEquals(0, CombatMath.counterArmorPenalty(10, 100), 1e-12);
    }

    @Test
    void weakeningSubtractsPointsAfterResistanceStacking() {
        double remaining = CombatMath.combineRemaining(CombatMath.combineRemaining(1, 0.5), 0.5);
        assertEquals(0.25, remaining, 1e-12);
        assertEquals(0.75, CombatMath.resistanceRemaining(remaining, 0.5), 1e-12);
        assertEquals(1.2, CombatMath.resistanceRemaining(0.7, 0.5), 1e-12);
    }

    @Test
    void subtractionAndLifeStealCannotCreateNegativeDamageOrOverkillCredit() {
        assertEquals(0, CombatMath.subtractDamage(3, 5), 1e-12);
        assertEquals(3, CombatMath.eligibleLifeStealDamage(15, 3), 1e-12);
        assertEquals(0, CombatMath.eligibleLifeStealDamage(0, 20), 1e-12);
    }

    @Test
    void healingBonusesAndPenaltiesShareAnAdditiveScale() {
        assertEquals(1.1, CombatMath.healingMultiplier(0.5, 0.4), 1e-12);
        assertEquals(1, CombatMath.healingMultiplier(0.5, 0.5), 1e-12);
        assertEquals(0, CombatMath.healingMultiplier(0, 1.2), 1e-12);
    }

    @Test
    void independentDodgesCombineWithoutAddingToImmunity() {
        assertEquals(0.36, DodgeRules.combinedChance(0.2, 0.2), 1e-12);
        assertEquals(0.488, DodgeRules.combinedChance(0.2, 0.2, 0.2), 1e-12);
        assertEquals(1, DodgeRules.combinedChance(0.2, 1), 1e-12);
    }
}
