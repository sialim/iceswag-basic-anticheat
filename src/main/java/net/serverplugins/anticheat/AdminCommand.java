package net.serverplugins.anticheat;

import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

final class AdminCommand implements CommandExecutor, TabCompleter {
    private final BasicAnticheatPlugin plugin;
    private final AnticheatService service;

    AdminCommand(BasicAnticheatPlugin plugin, AnticheatService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("alerts")) {
            if (sender instanceof Player player && sender.hasPermission("basicanticheat.alerts")) {
                reply(sender, "Alerts " + (service.toggleAlerts(player) ? "enabled." : "disabled."));
            } else {
                reply(sender, "This requires an in-game player with basicanticheat.alerts.");
            }
            return true;
        }
        if (!sender.hasPermission("basicanticheat.admin")) {
            reply(sender, "You do not have permission to manage the anticheat.");
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            try {
                plugin.reloadSettings();
                reply(sender, "Configuration reloaded. Player check histories reset.");
            } catch (Exception exception) {
                reply(sender, "Reload failed: " + exception.getMessage());
            }
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("status")) {
            Settings settings = service.settings();
            reply(sender, String.format(Locale.ROOT,
                    "Enabled: %s | TPS: %.1f | speed: %s | flight: %s | reach: %s | setbacks: %s | cancel attacks: %s | kicks: %s",
                    settings.enabled(), plugin.getServer().getTPS()[0], settings.speedEnabled(), settings.flightEnabled(),
                    settings.reachEnabled(), settings.setbackMovement(), settings.cancelAttacks(), settings.kickEnabled()));
            return true;
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("status") || args[0].equalsIgnoreCase("reset"))) {
            Player target = plugin.getServer().getPlayerExact(args[1]);
            if (target == null) {
                reply(sender, "That player is not online.");
            } else if (args[0].equalsIgnoreCase("reset")) {
                service.reset(target);
                reply(sender, "Check history reset for " + target.getName() + ".");
            } else {
                reply(sender, service.status(target));
            }
            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("test")) {
            try {
                CheckType check = CheckType.valueOf(args[1].toUpperCase(Locale.ROOT));
                service.testAlert(sender.getName(), check);
                reply(sender, "Test alert sent. This tests alert delivery only.");
            } catch (IllegalArgumentException exception) {
                reply(sender, "Usage: /basicac test <speed|flight|reach>");
            }
            return true;
        }
        reply(sender, "Usage: /basicac alerts | reload | status [player] | reset <player> | test <speed|flight|reach>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) {
            return List.of();
        }
        List<String> suggestions = List.of();
        if (args.length == 1) {
            if (sender.hasPermission("basicanticheat.admin")) {
                suggestions = List.of("alerts", "reload", "status", "reset", "test");
            } else if (sender.hasPermission("basicanticheat.alerts")) {
                suggestions = List.of("alerts");
            }
        } else if (args.length == 2 && sender.hasPermission("basicanticheat.admin")) {
            if (args[0].equalsIgnoreCase("test")) {
                suggestions = List.of("speed", "flight", "reach");
            } else if (args[0].equalsIgnoreCase("status") || args[0].equalsIgnoreCase("reset")) {
                suggestions = plugin.getServer().getOnlinePlayers().stream().map(Player::getName).toList();
            }
        }
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        return suggestions.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix)).sorted().toList();
    }

    private void reply(CommandSender sender, String message) {
        sender.sendMessage(Component.text("[BasicAnticheat] " + message, NamedTextColor.YELLOW));
    }
}
