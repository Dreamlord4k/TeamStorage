package ru.syntaxteam.teamstorage;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.syntaxteam.teamstorage.command.CityChatCommand;
import ru.syntaxteam.teamstorage.command.CityCommand;
import ru.syntaxteam.teamstorage.command.TeamStorageCommand;
import ru.syntaxteam.teamstorage.config.PluginConfig;
import ru.syntaxteam.teamstorage.listener.CityChatListener;
import ru.syntaxteam.teamstorage.listener.EnderChestListener;
import ru.syntaxteam.teamstorage.listener.InventoryListener;
import ru.syntaxteam.teamstorage.service.AuditLogService;
import ru.syntaxteam.teamstorage.service.CityChatService;
import ru.syntaxteam.teamstorage.service.CityService;
import ru.syntaxteam.teamstorage.service.PlayerSettingsService;
import ru.syntaxteam.teamstorage.service.StorageService;
import ru.syntaxteam.teamstorage.service.TabNameService;
import ru.syntaxteam.teamstorage.storage.SQLiteStorage;

public final class TeamStoragePlugin extends JavaPlugin {
    private PluginConfig pluginConfig;
    private SQLiteStorage sqliteStorage;
    private AuditLogService auditLogService;
    private PlayerSettingsService playerSettingsService;
    private StorageService storageService;
    private CityService cityService;
    private TabNameService tabNameService;
    private CityChatService cityChatService;

    @Override
    public void onEnable() {
        migrateLegacyDataFolder();
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        migrateLegacyConfig();
        saveConfig();
        pluginConfig = new PluginConfig(this);

        sqliteStorage = new SQLiteStorage(this);
        sqliteStorage.init();

        auditLogService = new AuditLogService(this, pluginConfig);
        playerSettingsService = new PlayerSettingsService(sqliteStorage);
        storageService = new StorageService(this, pluginConfig, sqliteStorage, auditLogService);
        cityService = new CityService(sqliteStorage, storageService);
        storageService.setCityLookup(cityService::findCity);
        storageService.loadAll(cityService.listCities());
        tabNameService = new TabNameService(this, pluginConfig, cityService);
        cityChatService = new CityChatService(this, pluginConfig, cityService, playerSettingsService);

        CityCommand cityCommand = new CityCommand(pluginConfig, cityService, tabNameService);
        getCommand("city").setExecutor(cityCommand);
        getCommand("city").setTabCompleter(cityCommand);

        TeamStorageCommand teamStorageCommand = new TeamStorageCommand(pluginConfig, cityService, playerSettingsService, storageService);
        getCommand("teamstorage").setExecutor(teamStorageCommand);
        getCommand("teamstorage").setTabCompleter(teamStorageCommand);

        CityChatCommand cityChatCommand = new CityChatCommand(pluginConfig, cityService, playerSettingsService);
        getCommand("citychat").setExecutor(cityChatCommand);
        getCommand("citychat").setTabCompleter(cityChatCommand);

        Bukkit.getPluginManager().registerEvents(new EnderChestListener(this, cityService, playerSettingsService, storageService), this);
        Bukkit.getPluginManager().registerEvents(new InventoryListener(this, pluginConfig, cityService, storageService), this);
        Bukkit.getPluginManager().registerEvents(new CityChatListener(this, cityChatService, tabNameService), this);

        storageService.startAutoSaveTask();
        tabNameService.logStatus();
        tabNameService.refreshAll();
        getLogger().info("TeamStorage enabled.");
    }

    @Override
    public void onDisable() {
        try {
            if (storageService != null) {
                storageService.saveAll();
            }
        } finally {
            if (auditLogService != null) {
                auditLogService.close();
            }
            if (cityChatService != null) {
                cityChatService.close();
            }
            if (tabNameService != null) {
                tabNameService.clearAll();
            }
            if (sqliteStorage != null) {
                sqliteStorage.close();
            }
        }
    }

    private void migrateLegacyDataFolder() {
        java.io.File target = getDataFolder();
        java.io.File parent = target.getParentFile();
        java.io.File legacy = parent == null ? null : new java.io.File(parent, "CityStorage");
        String[] targetFiles = target.list();
        if (legacy == null || !legacy.isDirectory() || targetFiles != null && targetFiles.length > 0) {
            return;
        }

        try (java.util.stream.Stream<java.nio.file.Path> paths = java.nio.file.Files.walk(legacy.toPath())) {
            paths.forEach(source -> {
                java.nio.file.Path destination = target.toPath().resolve(legacy.toPath().relativize(source));
                try {
                    if (java.nio.file.Files.isDirectory(source)) {
                        java.nio.file.Files.createDirectories(destination);
                    } else {
                        java.nio.file.Files.copy(source, destination);
                    }
                } catch (java.io.IOException exception) {
                    throw new java.io.UncheckedIOException(exception);
                }
            });
            getLogger().info("Migrated data from plugins/CityStorage. The old folder was kept as a backup.");
        } catch (java.io.IOException | java.io.UncheckedIOException exception) {
            throw new IllegalStateException("Cannot migrate the legacy CityStorage data folder.", exception);
        }
    }

    private void migrateLegacyConfig() {
        replaceLegacyConfigValue("messages.prefix.ru", "&8[&2CityStorage&8] &r", "&8[&2TeamStorage&8] &r");
        replaceLegacyConfigValue("messages.prefix.en", "&8[&2CityStorage&8] &r", "&8[&2TeamStorage&8] &r");
        replaceLegacyConfigValue("logging.file", "city-storage.log", "team-storage.log");
        replaceLegacyConfigValue("city-chat.spy.permission", "citystorage.chat.spy", "teamstorage.chat.spy");
    }

    private void replaceLegacyConfigValue(String path, String oldValue, String newValue) {
        if (oldValue.equals(getConfig().getString(path))) {
            getConfig().set(path, newValue);
        }
    }
}
