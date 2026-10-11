package net.serverplugins.anticheat;

import org.bukkit.configuration.file.FileConfiguration;

record Settings(
        boolean enabled,
        double minimumTps,
        int maximumPing,
        long maximumSampleGapMs,
        int joinGraceTicks,
        int teleportGraceTicks,
        int velocityGraceTicks,
        int environmentGraceTicks,
        boolean speedEnabled,
        double speedLimit,
        double speedTolerance,
        double speedFlagLevel,
        boolean flightEnabled,
        int hoverTicks,
        double minimumFall,
        double flightFlagLevel,
        boolean reachEnabled,
        double reachExtra,
        double reachLatencyAllowance,
        double reachFlagLevel,
        double decayPerSecond,
        boolean consoleAlerts,
        boolean staffAlerts,
        int alertCooldownTicks,
        boolean setbackMovement,
        boolean cancelAttacks,
        int actionCooldownTicks,
        boolean kickEnabled,
        double kickLevel,
        String kickMessage) {

    static Settings load(FileConfiguration config) {
        return new Settings(
                config.getBoolean("enabled", true),
                number(config, "performance.minimum-tps", 18, 0, 20),
                integer(config, "performance.maximum-ping-ms", 250, 0, 5000),
                integer(config, "performance.maximum-sample-gap-ms", 1000, 500, 10000),
                integer(config, "grace.join-ticks", 100, 0, 1200),
                integer(config, "grace.teleport-ticks", 60, 0, 1200),
                integer(config, "grace.velocity-ticks", 60, 0, 1200),
                integer(config, "grace.environment-ticks", 40, 0, 1200),
                config.getBoolean("checks.speed.enabled", true),
                number(config, "checks.speed.maximum-blocks-per-tick", 0.70, 0.1, 10),
                number(config, "checks.speed.tolerance-blocks", 0.35, 0, 10),
                number(config, "checks.speed.flag-level", 4, 1, 100),
                config.getBoolean("checks.flight.enabled", true),
                integer(config, "checks.flight.maximum-hover-ticks", 60, 20, 1200),
                number(config, "checks.flight.minimum-fall-per-tick", 0.03, 0, 0.2),
                number(config, "checks.flight.flag-level", 3, 1, 100),
                config.getBoolean("checks.reach.enabled", true),
                number(config, "checks.reach.extra-distance", 1, 0, 10),
                number(config, "checks.reach.maximum-latency-allowance", 0.50, 0, 5),
                number(config, "checks.reach.flag-level", 4, 1, 100),
                number(config, "violations.decay-per-second", 0.25, 0, 10),
                config.getBoolean("alerts.console", false),
                config.getBoolean("alerts.staff", false),
                integer(config, "alerts.cooldown-seconds", 10, 1, 3600) * 20,
                config.getBoolean("actions.setback-movement", false),
                config.getBoolean("actions.cancel-invalid-attacks", false),
                integer(config, "actions.cooldown-seconds", 2, 1, 3600) * 20,
                config.getBoolean("actions.kick.enabled", false),
                number(config, "actions.kick.violation-level", 20, 1, 1000),
                config.getString("actions.kick.message", "Unusual movement or combat detected. Please reconnect."));
    }

    double flagLevel(CheckType check) {
        return switch (check) {
            case SPEED -> speedFlagLevel;
            case FLIGHT -> flightFlagLevel;
            case REACH -> reachFlagLevel;
        };
    }

    private static double number(FileConfiguration config, String path, double fallback, double minimum, double maximum) {
        double value = config.getDouble(path, fallback);
        return Double.isFinite(value) ? Math.clamp(value, minimum, maximum) : fallback;
    }

    private static int integer(FileConfiguration config, String path, int fallback, int minimum, int maximum) {
        return Math.clamp(config.getInt(path, fallback), minimum, maximum);
    }
}
