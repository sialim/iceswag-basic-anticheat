package net.serverplugins.anticheat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ViolationBufferTest {
    @Test
    void isolatedFlagsDecayBeforeReachingThreshold() {
        ViolationBuffer buffer = new ViolationBuffer();
        assertEquals(1, buffer.add(100, 0.25));
        assertEquals(1, buffer.add(200, 0.25));
        assertEquals(0, buffer.level(300, 0.25));
    }

    @Test
    void sustainedViolationsAccumulatePastThreshold() {
        ViolationBuffer buffer = new ViolationBuffer();
        for (int tick = 100; tick <= 140; tick += 10) {
            buffer.add(tick, 0.25);
        }
        assertEquals(4.5, buffer.level(140, 0.25));
    }

    @Test
    void inspectingLevelDoesNotChangeDecayRate() {
        ViolationBuffer buffer = new ViolationBuffer();
        buffer.add(100, 0.25);
        assertEquals(0.75, buffer.level(120, 0.25));
        assertEquals(0.5, buffer.level(140, 0.25));
        assertEquals(0.5, buffer.level(140, 0.25));
    }

    @Test
    void alertsWaitForCooldownBeforeRepeating() {
        ViolationBuffer buffer = new ViolationBuffer();
        assertTrue(buffer.takeAlert(100, 200));
        assertFalse(buffer.takeAlert(299, 200));
        assertTrue(buffer.takeAlert(300, 200));
    }
}
