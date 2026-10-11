package net.serverplugins.anticheat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CheckMathTest {
    @Test
    void sprintJumpingStaysBelowDefaultAllowance() {
        double allowance = CheckMath.speedAllowance(0.70, 0.35, 0.13, 10);
        assertTrue(0.61 * 10 < allowance);
    }

    @Test
    void grossSpeedExceedsDefaultAllowance() {
        assertTrue(1.1 * 10 > CheckMath.speedAllowance(0.70, 0.35, 0.13, 10));
    }

    @Test
    void speedAttributeModifiersIncreaseAllowance() {
        assertEquals(14.35, CheckMath.speedAllowance(0.70, 0.35, 0.2, 10), 0.0001);
    }

    @Test
    void slownessDoesNotReduceConservativeAllowance() {
        assertEquals(7.35, CheckMath.speedAllowance(0.70, 0.35, 0.05, 10), 0.0001);
    }

    @Test
    void hoveringAndAscendingAccumulateAirTime() {
        int hover = 0;
        for (int sample = 0; sample < 7; sample++) {
            hover = CheckMath.hoverTicks(hover, sample % 2 == 0 ? 0 : 0.4, 10, 0.03, false);
        }
        assertEquals(70, hover);
    }

    @Test
    void normalFallingAndLandingResetAirTime() {
        assertEquals(0, CheckMath.hoverTicks(50, -0.5, 10, 0.03, false));
        assertEquals(0, CheckMath.hoverTicks(50, 0, 10, 0.03, true));
    }

    @Test
    void reachUsesNearestPointOfTargetInsteadOfItsCenter() {
        assertEquals(2.7, CheckMath.distanceToBox(0, 1.62, 0, 2.7, 0, -0.3, 3.3, 1.8, 0.3), 0.0001);
    }

    @Test
    void reachDistanceIncludesVerticalSeparation() {
        assertEquals(5, CheckMath.distanceToBox(0, 0, 0, 3, 4, 0, 4, 5, 1), 0.0001);
    }

    @Test
    void overlappingTargetHasZeroDistance() {
        assertEquals(0, CheckMath.distanceToBox(0, 1, 0, -0.3, 0, -0.3, 0.3, 1.8, 0.3));
    }

    @Test
    void reachHonorsCustomAttributesAndCapsLatencyTolerance() {
        assertEquals(4, CheckMath.reachAllowance(3, 1, 0.5, 0));
        assertEquals(4.2, CheckMath.reachAllowance(3, 1, 0.5, 100), 0.0001);
        assertEquals(6.5, CheckMath.reachAllowance(5, 1, 0.5, 1000));
    }
}
