package net.serverplugins.anticheat;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class BasicAnticheatPlugin extends JavaPlugin {
    private AnticheatService service;
    private BukkitTask task;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        service = new AnticheatService(this, Settings.load(getConfig()));
        getServer().getPluginManager().registerEvents(new AnticheatListener(service), this);
        AdminCommand executor = new AdminCommand(this, service);
        PluginCommand command = Objects.requireNonNull(getCommand("basicac"));
        command.setExecutor(executor);
        command.setTabCompleter(executor);
        getServer().getOnlinePlayers().forEach(service::join);
        task = getServer().getScheduler().runTaskTimer(this, service::tick, 1, 1);
    }

    @Override
    public void onDisable() {
        if (task != null) {
            task.cancel();
        }
    }

    void reloadSettings() throws IOException, InvalidConfigurationException {
        YamlConfiguration replacement = new YamlConfiguration();
        replacement.load(new File(getDataFolder(), "config.yml"));
        service.reload(Settings.load(replacement));
    }
}
