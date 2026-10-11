package net.serverplugins.anticheat;

final class ViolationBuffer {
    private double level;
    private long lastTick;
    private long nextAlertTick;

    double level(long tick, double decayPerSecond) {
        long elapsed = Math.max(0, tick - lastTick);
        level = Math.max(0, level - elapsed / 20.0 * decayPerSecond);
        lastTick = tick;
        return level;
    }

    double add(long tick, double decayPerSecond) {
        level(tick, decayPerSecond);
        return ++level;
    }

    boolean takeAlert(long tick, int cooldownTicks) {
        if (tick < nextAlertTick) {
            return false;
        }
        nextAlertTick = tick + cooldownTicks;
        return true;
    }
}
