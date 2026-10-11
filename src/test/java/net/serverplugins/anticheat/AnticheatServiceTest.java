package net.serverplugins.anticheat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import be.seeseemelk.mockbukkit.MockBukkit;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.VoxelShape;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnticheatServiceTest {
    private AnticheatService service;
    private BasicAnticheatPlugin plugin;
    private Server server;
    private Player player;
    private World world;
    private AtomicReference<Location> position;
    private Logger logger;
    private YamlConfiguration config;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        plugin = mock(BasicAnticheatPlugin.class);
        server = mock(Server.class);
        player = mock(Player.class);
        world = mock(World.class);
        logger = mock(Logger.class);
        Block air = mock(Block.class);
        VoxelShape empty = mock(VoxelShape.class);
        when(air.getType()).thenReturn(Material.AIR);
        when(air.getCollisionShape()).thenReturn(empty);
        when(empty.getBoundingBoxes()).thenReturn(List.of());
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(air);
        when(plugin.getServer()).thenReturn(server);
        when(plugin.getLogger()).thenReturn(logger);
        when(server.getTPS()).thenReturn(new double[]{20, 20, 20});
        when(server.getOnlinePlayers()).thenAnswer(ignored -> List.of(player));
        when(player.getUniqueId()).thenReturn(java.util.UUID.randomUUID());
        when(player.getName()).thenReturn("Tester");
        when(player.isOnline()).thenReturn(true);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        when(player.getWorld()).thenReturn(world);
        position = new AtomicReference<>(new Location(world, 0, 100, 0));
        when(player.getLocation()).thenAnswer(ignored -> position.get().clone());
        when(player.getEyeLocation()).thenAnswer(ignored -> position.get().clone().add(0, 1.62, 0));
        when(player.getBoundingBox()).thenAnswer(ignored -> box(position.get()));
        config = new YamlConfiguration();
        config.set("alerts.console", true);
        config.set("grace.join-ticks", 0);
        config.set("checks.flight.enabled", false);
        service = new AnticheatService(plugin, Settings.load(config));
        service.join(player);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void sustainedGrossSpeedProducesAnAlertWithoutPunishmentByDefault() {
        for (int i = 0; i < 6; i++) {
            position.get().add(12, 0, 0);
            advance(10);
        }
        verify(logger).info(org.mockito.ArgumentMatchers.contains("speed"));
        assertTrue(service.status(player).contains("speed 5.4"));
        verify(player, never()).kick(any(net.kyori.adventure.text.Component.class));
        verify(player, never()).teleport(any(Location.class));
    }

    @Test
    void normalSprintJumpSpeedDoesNotAccumulateFlags() {
        for (int i = 0; i < 20; i++) {
            position.get().add(6, i % 2 == 0 ? 1 : -1, 0);
            advance(10);
        }
        assertTrue(service.status(player).contains("speed 0.0"));
        verify(logger, never()).info(any(String.class));
    }

    @Test
    void sustainedUnsupportedHoverProducesAFlightAlert() {
        config.set("checks.flight.enabled", true);
        service.reload(Settings.load(config));
        service.join(player);
        advance(100);
        verify(logger).info(org.mockito.ArgumentMatchers.contains("flight"));
    }

    @Test
    void fallingDoesNotProduceFlightAlerts() {
        config.set("checks.flight.enabled", true);
        service.reload(Settings.load(config));
        for (int i = 0; i < 20; i++) {
            position.get().subtract(0, 1, 0);
            advance(10);
        }
        assertTrue(service.status(player).contains("flight 0.0"));
    }

    @Test
    void lagHighPingFlightAndBypassExemptMovement() {
        when(server.getTPS()).thenReturn(new double[]{10, 20, 20});
        moveFast();
        when(server.getTPS()).thenReturn(new double[]{20, 20, 20});
        when(player.getPing()).thenReturn(400);
        moveFast();
        when(player.getPing()).thenReturn(0);
        when(player.getAllowFlight()).thenReturn(true);
        moveFast();
        when(player.getAllowFlight()).thenReturn(false);
        when(player.hasPermission("basicanticheat.bypass")).thenReturn(true);
        moveFast();
        assertTrue(service.status(player).contains("speed 0.0"));
        verify(logger, never()).info(any(String.class));
    }

    @Test
    void teleportGracePreventsMovementAndReachChecks() {
        Player target = target(8);
        service.positionGrace(player, 100);
        moveFast();
        assertFalse(service.invalidReach(player, target));
        assertTrue(service.status(player).contains("speed 0.0"));
    }

    @Test
    void repeatedOutOfRangeAttacksAccumulateDespiteTargetDamageGrace() {
        Player target = target(8);
        boolean flagged = false;
        for (int i = 0; i < 6; i++) {
            service.grace(target, 60);
            flagged |= service.invalidReach(player, target);
            advance(10);
        }
        assertTrue(flagged);
        verify(logger).info(org.mockito.ArgumentMatchers.contains("reach"));
    }

    @Test
    void normalReachAndHighPingAreIgnored() {
        Player target = target(3);
        for (int i = 0; i < 6; i++) {
            assertFalse(service.invalidReach(player, target));
        }
        when(target.getBoundingBox()).thenReturn(box(new Location(world, 8, 100, 0)));
        when(target.getPing()).thenReturn(400);
        assertFalse(service.invalidReach(player, target));
        assertTrue(service.status(player).contains("reach 0.0"));
    }

    @Test
    void kickRequiresConfigurationAndViolationThreshold() {
        config.set("actions.kick.enabled", true);
        config.set("actions.kick.violation-level", 4);
        service.reload(Settings.load(config));
        service.join(player);
        moveFast();
        verify(player).kick(any(net.kyori.adventure.text.Component.class));
    }

    @Test
    void configuredSetbackReturnsToPreviouslySupportedPosition() {
        Block ground = mock(Block.class);
        VoxelShape shape = mock(VoxelShape.class);
        when(ground.getType()).thenReturn(Material.STONE);
        when(ground.getCollisionShape()).thenReturn(shape);
        when(shape.getBoundingBoxes()).thenReturn(List.of(new BoundingBox(0, 0, 0, 1, 1, 1)));
        when(world.getBlockAt(anyInt(), org.mockito.ArgumentMatchers.eq(99), anyInt())).thenReturn(ground);
        when(player.teleport(any(Location.class))).thenAnswer(invocation -> {
            position.set(((Location) invocation.getArgument(0)).clone());
            return true;
        });
        config.set("actions.setback-movement", true);
        service.reload(Settings.load(config));
        service.join(player);
        advance(10);
        moveFast();
        verify(player).teleport(org.mockito.ArgumentMatchers.argThat((Location destination) ->
                destination.getX() == 0 && destination.getY() == 100 && destination.getZ() == 0));
    }

    @Test
    void setbacksRequireASupportedPosition() {
        config.set("actions.setback-movement", true);
        service.reload(Settings.load(config));
        service.join(player);
        moveFast();
        verify(player, never()).teleport(any(Location.class));
    }

    @Test
    void testAlertDoesNotChangeViolationHistory() {
        String before = service.status(player);
        service.testAlert("Tester", CheckType.SPEED);
        assertEquals(before, service.status(player));
        verify(logger).info(org.mockito.ArgumentMatchers.contains("[TEST]"));
        verify(player, never()).kick(any(net.kyori.adventure.text.Component.class));
    }

    @Test
    void defaultsDoNotLogOrSendUnrequestedAlerts() {
        config.set("alerts.console", false);
        config.set("alerts.staff", false);
        service.reload(Settings.load(config));
        service.join(player);
        when(player.hasPermission("basicanticheat.alerts")).thenReturn(true);
        moveFast();
        verify(logger, never()).info(any(String.class));
        verify(player, never()).sendMessage(any(net.kyori.adventure.text.Component.class));
    }

    private Player target(double x) {
        Player target = mock(Player.class);
        Location location = new Location(world, x, 100, 0);
        when(target.getUniqueId()).thenReturn(java.util.UUID.randomUUID());
        when(target.getLocation()).thenReturn(location);
        when(target.getWorld()).thenReturn(world);
        when(target.getBoundingBox()).thenReturn(box(location));
        service.join(target);
        return target;
    }

    private void moveFast() {
        for (int i = 0; i < 6; i++) {
            position.get().add(12, 0, 0);
            advance(10);
        }
    }

    private void advance(int ticks) {
        for (int i = 0; i < ticks; i++) {
            service.tick();
        }
    }

    private static BoundingBox box(Location location) {
        return new BoundingBox(location.getX() - 0.3, location.getY(), location.getZ() - 0.3,
                location.getX() + 0.3, location.getY() + 1.8, location.getZ() + 0.3);
    }
}
