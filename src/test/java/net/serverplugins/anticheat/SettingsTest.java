package net.serverplugins.anticheat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class SettingsTest {
    @Test
    void shippedConfigurationEnablesDetectionAndDisablesPunishments() {
        var stream = Objects.requireNonNull(getClass().getResourceAsStream("/config.yml"));
        Settings settings = Settings.load(YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8)));
        assertTrue(settings.enabled());
        assertTrue(settings.speedEnabled());
        assertTrue(settings.flightEnabled());
        assertTrue(settings.reachEnabled());
        assertFalse(settings.setbackMovement());
        assertFalse(settings.cancelAttacks());
        assertFalse(settings.kickEnabled());
    }

    @Test
    void invalidNumbersAreClampedOrReplaced() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("checks.speed.maximum-blocks-per-tick", Double.NaN);
        config.set("checks.flight.maximum-hover-ticks", -100);
        config.set("performance.minimum-tps", 99);
        config.set("alerts.cooldown-seconds", Integer.MAX_VALUE);
        Settings settings = Settings.load(config);
        assertEquals(0.70, settings.speedLimit());
        assertEquals(20, settings.hoverTicks());
        assertEquals(20, settings.minimumTps());
        assertEquals(72000, settings.alertCooldownTicks());
    }

    @Test
    void configurationCanDisableChecksAndEnableActionsIndependently() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("checks.speed.enabled", false);
        config.set("actions.cancel-invalid-attacks", true);
        config.set("actions.kick.enabled", true);
        Settings settings = Settings.load(config);
        assertFalse(settings.speedEnabled());
        assertTrue(settings.flightEnabled());
        assertTrue(settings.cancelAttacks());
        assertTrue(settings.kickEnabled());
        assertFalse(settings.setbackMovement());
    }
}
