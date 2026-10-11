package net.serverplugins.anticheat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

final class AnticheatService {
    private final BasicAnticheatPlugin plugin;
    private final Map<UUID, PlayerData> players = new HashMap<>();
    private final Set<UUID> mutedAlerts = new HashSet<>();
    private final Set<UUID> subscribedAlerts = new HashSet<>();
    private Settings settings;
    private long tick;

    AnticheatService(BasicAnticheatPlugin plugin, Settings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    Settings settings() {
        return settings;
    }

    void reload(Settings replacement) {
        settings = replacement;
        players.clear();
    }

    void tick() {
        tick++;
        if (tick % 10 != 0) {
            return;
        }
        long now = System.nanoTime();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            sampleMovement(player, now);
        }
    }

    void join(Player player) {
        players.put(player.getUniqueId(), new PlayerData(player.getLocation(), tick, System.nanoTime(), settings.joinGraceTicks()));
    }

    void remove(Player player) {
        players.remove(player.getUniqueId());
        mutedAlerts.remove(player.getUniqueId());
        subscribedAlerts.remove(player.getUniqueId());
    }

    void reset(Player player) {
        join(player);
    }

    void grace(Player player, int duration) {
        data(player).grantGrace(tick, duration);
    }

    void positionGrace(Player player, int duration) {
        PlayerData data = data(player);
        data.grantGrace(tick, duration);
        data.reachGraceUntil = Math.max(data.reachGraceUntil, tick + duration);
    }

    boolean toggleAlerts(Player player) {
        boolean active = subscribedAlerts.contains(player.getUniqueId())
                || settings.staffAlerts() && !mutedAlerts.contains(player.getUniqueId());
        if (active) {
            subscribedAlerts.remove(player.getUniqueId());
            mutedAlerts.add(player.getUniqueId());
        } else {
            mutedAlerts.remove(player.getUniqueId());
            subscribedAlerts.add(player.getUniqueId());
        }
        return !active;
    }

    boolean healthy(Player player) {
        double[] tps = plugin.getServer().getTPS();
        return settings.enabled() && tps.length > 0 && tps[0] >= settings.minimumTps()
                && player.getPing() <= settings.maximumPing() && eligible(player);
    }

    boolean invalidReach(Player attacker, Player target) {
        if (!settings.reachEnabled() || !healthy(attacker) || target.isDead()
                || target.getPing() > settings.maximumPing() || !attacker.getWorld().equals(target.getWorld())
                || attacker.isInsideVehicle() || attacker.isGliding() || target.isInsideVehicle() || target.isGliding()
                || tick < data(attacker).reachGraceUntil || tick < data(target).reachGraceUntil) {
            return false;
        }
        Location eye = attacker.getEyeLocation();
        BoundingBox box = target.getBoundingBox();
        double distance = CheckMath.distanceToBox(eye.getX(), eye.getY(), eye.getZ(),
                box.getMinX(), box.getMinY(), box.getMinZ(), box.getMaxX(), box.getMaxY(), box.getMaxZ());
        double limit = CheckMath.reachAllowance(attribute(attacker, Attribute.PLAYER_ENTITY_INTERACTION_RANGE, 3),
                settings.reachExtra(), settings.reachLatencyAllowance(), attacker.getPing() + target.getPing());
        return distance > limit && flag(attacker, CheckType.REACH, format("distance %.2f, limit %.2f", distance, limit));
    }

    String status(Player player) {
        PlayerData data = data(player);
        return format("%s: speed %.1f, flight %.1f, reach %.1f | ping %d ms | bypass %s",
                player.getName(), data.buffer(CheckType.SPEED).level(tick, settings.decayPerSecond()),
                data.buffer(CheckType.FLIGHT).level(tick, settings.decayPerSecond()),
                data.buffer(CheckType.REACH).level(tick, settings.decayPerSecond()),
                player.getPing(), player.hasPermission("basicanticheat.bypass"));
    }

    void testAlert(String name, CheckType check) {
        sendAlert("[TEST] " + name + " | " + check.key() + " | simulated alert; no action taken", true);
    }

    private void sampleMovement(Player player, long now) {
        PlayerData data = data(player);
        Location current = player.getLocation();
        Location previous = data.lastLocation;
        int elapsedTicks = (int) Math.max(1, tick - data.lastSampleTick);
        long elapsedMs = Math.max(0, (now - data.lastSampleNanos) / 1_000_000);
        data.lastLocation = current.clone();
        data.lastSampleTick = tick;
        data.lastSampleNanos = now;
        if (!current.getWorld().equals(previous.getWorld())) {
            data.grantGrace(tick, settings.teleportGraceTicks());
            return;
        }
        if (!healthy(player) || elapsedMs > settings.maximumSampleGapMs()
                || elapsedTicks > 20 || MovementEnvironment.exempt(player)
                || MovementEnvironment.specialBlocks(current) || MovementEnvironment.specialBlocks(previous)) {
            data.grantGrace(tick, settings.environmentGraceTicks());
            return;
        }
        if (tick < data.graceUntil) {
            data.hoverTicks = 0;
            return;
        }
        double dx = current.getX() - previous.getX();
        double dz = current.getZ() - previous.getZ();
        double distance = Math.hypot(dx, dz);
        double speedLimit = CheckMath.speedAllowance(settings.speedLimit(), settings.speedTolerance(),
                attribute(player, Attribute.GENERIC_MOVEMENT_SPEED, 0.1), elapsedTicks);
        boolean speedInvalid = settings.speedEnabled() && distance > speedLimit;
        if (speedInvalid && flag(player, CheckType.SPEED, format("distance %.2f, limit %.2f", distance, speedLimit))) {
            if (!player.isOnline() || tick < data.graceUntil) {
                return;
            }
        }
        boolean supported = MovementEnvironment.supported(player);
        boolean flightExempt = attribute(player, Attribute.GENERIC_GRAVITY, 0.08) < 0.079
                || attribute(player, Attribute.GENERIC_JUMP_STRENGTH, 0.42) > 0.43;
        data.hoverTicks = CheckMath.hoverTicks(data.hoverTicks, current.getY() - previous.getY(), elapsedTicks,
                settings.minimumFall(), supported || flightExempt);
        boolean flightInvalid = settings.flightEnabled() && data.hoverTicks > settings.hoverTicks();
        if (flightInvalid && flag(player, CheckType.FLIGHT, "hover " + data.hoverTicks + " ticks")) {
            if (!player.isOnline() || tick < data.graceUntil) {
                return;
            }
        }
        if (supported && !speedInvalid && !flightInvalid) {
            data.safeLocation = current.clone();
        }
    }

    private boolean flag(Player player, CheckType check, String detail) {
        PlayerData data = data(player);
        ViolationBuffer buffer = data.buffer(check);
        double level = buffer.add(tick, settings.decayPerSecond());
        if (level < settings.flagLevel(check)) {
            return false;
        }
        if (buffer.takeAlert(tick, settings.alertCooldownTicks())) {
            sendAlert(format("%s | %s | VL %.1f | %s", player.getName(), check.key(), level, detail));
        }
        if (tick >= data.nextActionTick) {
            if (settings.kickEnabled() && level >= settings.kickLevel()) {
                data.nextActionTick = tick + settings.actionCooldownTicks();
                player.kick(Component.text(settings.kickMessage(), NamedTextColor.RED));
            } else if (check != CheckType.REACH && settings.setbackMovement() && data.safeLocation != null) {
                Location destination = data.safeLocation.clone();
                if (destination.getWorld().equals(player.getWorld())
                        && destination.getWorld().isChunkLoaded(destination.getBlockX() >> 4, destination.getBlockZ() >> 4)) {
                    destination.setYaw(player.getLocation().getYaw());
                    destination.setPitch(player.getLocation().getPitch());
                    data.nextActionTick = tick + settings.actionCooldownTicks();
                    if (player.teleport(destination)) {
                        data.grantGrace(tick, settings.teleportGraceTicks());
                    }
                }
            }
        }
        return true;
    }

    private void sendAlert(String message) {
        sendAlert(message, false);
    }

    private void sendAlert(String message, boolean requested) {
        if (settings.consoleAlerts() || requested) {
            plugin.getLogger().info(message);
        }
        if (settings.staffAlerts() || !subscribedAlerts.isEmpty()) {
            Component alert = Component.text("[BasicAnticheat] " + message, NamedTextColor.YELLOW);
            for (Player staff : plugin.getServer().getOnlinePlayers()) {
                if (staff.hasPermission("basicanticheat.alerts") && (subscribedAlerts.contains(staff.getUniqueId())
                        || settings.staffAlerts() && !mutedAlerts.contains(staff.getUniqueId()))) {
                    staff.sendMessage(alert);
                }
            }
        }
    }

    private PlayerData data(Player player) {
        return players.computeIfAbsent(player.getUniqueId(), ignored ->
                new PlayerData(player.getLocation(), tick, System.nanoTime(), settings.joinGraceTicks()));
    }

    private boolean eligible(Player player) {
        return player.isOnline() && !player.isDead() && !player.hasPermission("basicanticheat.bypass")
                && (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE);
    }

    private static double attribute(Player player, Attribute attribute, double fallback) {
        AttributeInstance instance = player.getAttribute(attribute);
        return instance == null ? fallback : instance.getValue();
    }

    private static String format(String format, Object... values) {
        return String.format(Locale.ROOT, format, values);
    }
}
