package net.serverplugins.anticheat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import java.nio.file.Files;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdminCommandTest {
    private ServerMock server;
    private BasicAnticheatPlugin plugin;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock(new HealthyServer());
        plugin = MockBukkit.load(BasicAnticheatPlugin.class);
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void pluginLoadsCommandsAndDefaultConfig() {
        assertTrue(plugin.isEnabled());
        assertNotNull(plugin.getCommand("basicac"));
        assertTrue(Files.exists(plugin.getDataFolder().toPath().resolve("config.yml")));
        player.setOp(true);
        assertTrue(server.dispatchCommand(player, "basicac status"));
        assertTrue(message().contains("kicks: false"));
    }

    @Test
    void regularPlayersCannotRunAdminCommands() {
        assertTrue(server.dispatchCommand(player, "basicac test speed"));
        assertTrue(message().contains("permission"));
    }

    @Test
    void staffCanToggleAlertsAndSeeTestNotifications() {
        player.setOp(true);
        server.dispatchCommand(player, "bac alerts");
        assertTrue(message().contains("enabled"));
        server.dispatchCommand(player, "bac alerts");
        assertTrue(message().contains("disabled"));
        server.dispatchCommand(server.getConsoleSender(), "basicac test speed");
        assertTrue(player.nextComponentMessage() == null);
        server.dispatchCommand(player, "bac alerts");
        assertTrue(message().contains("enabled"));
        server.dispatchCommand(server.getConsoleSender(), "basicac test flight");
        assertTrue(message().contains("[TEST]"));
    }

    @Test
    void validReloadChangesSettingsAndResetsPlayerHistory() throws Exception {
        player.setOp(true);
        Files.writeString(plugin.getDataFolder().toPath().resolve("config.yml"), "enabled: false\n");
        server.dispatchCommand(player, "basicac reload");
        assertTrue(message().contains("reloaded"));
        server.dispatchCommand(player, "basicac status");
        assertTrue(message().contains("Enabled: false"));
    }

    @Test
    void malformedReloadPreservesActiveSettings() throws Exception {
        player.setOp(true);
        Files.writeString(plugin.getDataFolder().toPath().resolve("config.yml"), "enabled: [broken\n");
        server.dispatchCommand(player, "basicac reload");
        assertTrue(message().contains("Reload failed"));
        server.dispatchCommand(player, "basicac status");
        assertTrue(message().contains("Enabled: true"));
    }

    @Test
    void defaultPermissionsAllowStaffAdministrationWithoutBypassingChecks() {
        player.setOp(true);
        assertTrue(player.hasPermission("basicanticheat.admin"));
        assertTrue(player.hasPermission("basicanticheat.alerts"));
        assertFalse(player.hasPermission("basicanticheat.bypass"));
    }

    private String message() {
        Component message = player.nextComponentMessage();
        assertNotNull(message);
        return PlainTextComponentSerializer.plainText().serialize(message);
    }

    private static final class HealthyServer extends ServerMock {
        @Override
        public double[] getTPS() {
            return new double[]{20, 20, 20};
        }
    }
}
