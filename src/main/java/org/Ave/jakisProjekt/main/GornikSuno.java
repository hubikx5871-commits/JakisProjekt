package org.Ave.jakisProjekt.main;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.Ave.jakisProjekt.commands.AdminCommand;
import org.Ave.jakisProjekt.commands.PointsCommand;
import org.Ave.jakisProjekt.database.Database;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandMap;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GornikSuno extends JavaPlugin {
    private Database database;
    private NamespacedKey speedKey;
    private FileConfiguration customConfig;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<UUID, PlayerData> cache = new HashMap<>();
    private PointsCommand pointsCommand;

    @Override
    public void onEnable() {
        this.setupFiles();
        this.speedKey = new NamespacedKey(this, "mining_speed");
        this.database = new Database();

        try {
            File dbFile = new File(getDataFolder(), "database/database.db");
            if (!dbFile.getParentFile().exists()) dbFile.getParentFile().mkdirs();
            this.database.connect(dbFile.getAbsolutePath());
        } catch (Exception e) {
            getLogger().severe("Blad bazy danych: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.registerCommands();
        getServer().getPluginManager().registerEvents(new PointsListener(this), this);

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            cache.forEach((uuid, data) -> database.savePlayerSync(uuid, data));
        }, 6000L, 6000L);
    }

    @Override
    public void onDisable() {
        cache.forEach((uuid, data) -> database.savePlayerSync(uuid, data));
        if (database != null) database.close();
    }

    private void setupFiles() {
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        File configFile = new File(getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            try {
                saveResource("config.yml", false);
            } catch (Exception e) {
                try {
                    saveResource("config/config.yml", false);
                    File oldConfig = new File(getDataFolder(), "config/config.yml");
                    if (oldConfig.exists()) {
                        oldConfig.renameTo(configFile);
                        new File(getDataFolder(), "config").delete();
                    }
                } catch (Exception e2) {
                    saveDefaultConfig();
                }
            }
        }
        this.customConfig = YamlConfiguration.loadConfiguration(configFile);
    }

    private void registerCommands() {
        try {
            Field f = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            f.setAccessible(true);
            CommandMap map = (CommandMap) f.get(Bukkit.getServer());
            String cmdName = customConfig.getString("settings.command", "gornik");
            this.pointsCommand = new PointsCommand(this, cmdName);
            map.register(cmdName, pointsCommand);
            map.register("gr", new AdminCommand(this));
        } catch (Exception e) { e.printStackTrace(); }
    }

    public PlayerData getPlayerData(UUID uuid) {
        return cache.computeIfAbsent(uuid, k -> database.loadPlayerSync(k));
    }

    public void removePlayerData(UUID uuid) {
        PlayerData data = cache.remove(uuid);
        if (data != null) {
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> database.savePlayerSync(uuid, data));
        }
    }

    public Component getMessage(String path) {
        String prefix = customConfig.getString("messages.prefix", "");
        String msg = customConfig.getString(path, path);
        return miniMessage.deserialize(prefix + msg);
    }

    @Override
    public FileConfiguration getConfig() { return this.customConfig; }
    public Database getDatabase() { return database; }
    public MiniMessage getMM() { return miniMessage; }
    public NamespacedKey getSpeedKey() { return speedKey; }
    public PointsCommand getPointsCommand() { return pointsCommand; }
}