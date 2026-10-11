package net.serverplugins.anticheat;

final class CheckMath {
    private CheckMath() {
    }

    static double speedAllowance(double limit, double tolerance, double movementAttribute, int ticks) {
        return limit * Math.max(1, movementAttribute / 0.1) * ticks + tolerance;
    }

    static int hoverTicks(int previousTicks, double verticalDistance, int ticks, double minimumFall, boolean supported) {
        return supported || verticalDistance < -minimumFall * ticks ? 0 : previousTicks + ticks;
    }

    static double reachAllowance(double interactionRange, double extra, double latencyCap, int combinedPing) {
        return interactionRange + extra + Math.min(latencyCap, Math.max(0, combinedPing) * 0.002);
    }

    static double distanceToBox(double x, double y, double z,
                                double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        double dx = x - Math.clamp(x, minX, maxX);
        double dy = y - Math.clamp(y, minY, maxY);
        double dz = z - Math.clamp(z, minZ, maxZ);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
