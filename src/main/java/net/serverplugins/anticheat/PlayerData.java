package net.serverplugins.anticheat;

import java.util.EnumMap;
import java.util.Map;
import org.bukkit.Location;

final class PlayerData {
    private final Map<CheckType, ViolationBuffer> violations = new EnumMap<>(CheckType.class);
    Location lastLocation;
    Location safeLocation;
    long lastSampleTick;
    long lastSampleNanos;
    long graceUntil;
    long reachGraceUntil;
    long nextActionTick;
    int hoverTicks;

    PlayerData(Location location, long tick, long now, int graceTicks) {
        lastLocation = location.clone();
        lastSampleTick = tick;
        lastSampleNanos = now;
        graceUntil = tick + graceTicks;
        reachGraceUntil = graceUntil;
    }

    ViolationBuffer buffer(CheckType check) {
        return violations.computeIfAbsent(check, ignored -> new ViolationBuffer());
    }

    void grantGrace(long tick, int ticks) {
        graceUntil = Math.max(graceUntil, tick + ticks);
        hoverTicks = 0;
        safeLocation = null;
    }
}
