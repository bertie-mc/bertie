package com.berlord.rusticengineerfix.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlightAdjustmentPolicyTest {

    @Test
    void restoresYawOnlyAfterAnActualTurn() {
        assertFalse(FlightAdjustmentPolicy.yawChanged(45.0F, 45.0F));
        assertTrue(FlightAdjustmentPolicy.yawChanged(45.0F, 46.0F));
        assertTrue(FlightAdjustmentPolicy.yawChanged(-179.0F, 179.0F));
    }

    @Test
    void appliesNonZeroVerticalInputOnlyWhileMounted() {
        assertTrue(FlightAdjustmentPolicy.shouldApplyVertical(true, 0.3D));
        assertTrue(FlightAdjustmentPolicy.shouldApplyVertical(true, -0.3D));
        assertFalse(FlightAdjustmentPolicy.shouldApplyVertical(true, 0.0D));
        assertFalse(FlightAdjustmentPolicy.shouldApplyVertical(false, 0.3D));
    }
}
