package ru.syntaxteam.teamstorage.service;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import ru.syntaxteam.teamstorage.config.PluginConfig;
import ru.syntaxteam.teamstorage.gui.CityStorageHolder;
import ru.syntaxteam.teamstorage.gui.SelectionMenuHolder;
import ru.syntaxteam.teamstorage.model.City;
import ru.syntaxteam.teamstorage.storage.SQLiteStorage;
import ru.syntaxteam.teamstorage.util.InventorySerializer;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class StorageService {
    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final SQLiteStorage storage;
    private final AuditLogService auditLogService;
    private final Map<Integer, Inventory> inventories = new HashMap<>();
    private final Map<Inventory, City> inventoryCities = new IdentityHashMap<>();
    private final Map<UUID, Integer> viewingCityStorage = new HashMap<>();
    private final Map<Integer, Boolean> dirty = new HashMap<>();
    private CityLookup cityLookup;

    public StorageService(JavaPlugin plugin, PluginConfig config, SQLiteStorage storage, AuditLogService auditLogService) {
        this.plugin = plugin;
        this.config = config;
        this.storage = storage;
        this.auditLogService = auditLogService;
    }

    public void setCityLookup(CityLookup cityLookup) {
        this.cityLookup = cityLookup;
    }

    public Inventory createSelectionMenu(Player player, City city) {
        Inventory inventory = Bukkit.createInventory(new SelectionMenuHolder(player.getUniqueId(), city), config.menuSize(), config.menuTitle());
        Map<String, String> placeholders = config.cityPlaceholders(city);
        setMenuItem(inventory, "personal", placeholders);
        setMenuItem(inventory, "city", placeholders);
        return inventory;
    }

    private void setMenuItem(Inventory inventory, String key, Map<String, String> placeholders) {
        int slot = config.itemSlot(key);
        if (slot >= 0 && slot < inventory.getSize()) {
            inventory.setItem(slot, config.menuItem(key, placeholders));
        } else {
            plugin.getLogger().warning("Invalid menu slot for '" + key + "' in config.yml: " + slot);
        }
    }

    public Inventory getCityInventory(City city) {
        return inventories.computeIfAbsent(city.id(), ignored -> {
            Inventory inventory = Bukkit.createInventory(new CityStorageHolder(city.id()), config.storageSize(), config.storageTitle(city));
            byte[] data = storage.loadStorageData(city.id());
            inventory.setContents(InventorySerializer.deserialize(data, config.storageSize()));
            inventoryCities.put(inventory, city);
            return inventory;
        });
    }

    public void loadAll(Iterable<City> cities) {
        for (City city : cities) {
            getCityInventory(city);
        }
    }

    public void createEmptyStorage(City city) {
        Inventory inventory = getCityInventory(city);
        storage.saveStorageData(city.id(), InventorySerializer.serialize(inventory.getContents()));
    }

    public void openCityStorage(Player player, City city) {
        Inventory inventory = getCityInventory(city);
        player.openInventory(inventory);
        viewingCityStorage.put(player.getUniqueId(), city.id());
        auditLogService.logOpen(player, city);
    }

    public void openPersonalEnderChest(Player player) {
        player.openInventory(player.getEnderChest());
    }

    public Optional<City> cityByInventory(Inventory inventory) {
        City direct = inventoryCities.get(inventory);
        if (direct != null) {
            return Optional.of(direct);
        }
        if (inventory.getHolder() instanceof CityStorageHolder holder && cityLookup != null) {
            return cityLookup.findCity(holder.cityId());
        }
        return Optional.empty();
    }

    public boolean isCityStorageInventory(Inventory inventory) {
        return inventory.getHolder() instanceof CityStorageHolder || inventoryCities.containsKey(inventory);
    }

    public boolean isViewingCityStorage(UUID playerId, int cityId) {
        return viewingCityStorage.getOrDefault(playerId, -1) == cityId;
    }

    public void markClosed(Player player, int cityId) {
        if (isViewingCityStorage(player.getUniqueId(), cityId)) {
            viewingCityStorage.remove(player.getUniqueId());
        }
    }

    public void clearViewer(Player player) {
        viewingCityStorage.remove(player.getUniqueId());
    }

    public void closeAnyCityStorage(Player player) {
        if (viewingCityStorage.containsKey(player.getUniqueId())) {
            player.closeInventory();
        }
    }

    public void closeCityStorageViewers(int cityId) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isViewingCityStorage(player.getUniqueId(), cityId)) {
                player.closeInventory();
            }
        }
    }

    public void markDirty(City city) {
        dirty.put(city.id(), true);
    }

    public void save(City city) {
        Inventory inventory = inventories.get(city.id());
        if (inventory == null) {
            return;
        }
        storage.saveStorageData(city.id(), InventorySerializer.serialize(inventory.getContents()));
        dirty.remove(city.id());
    }

    public void saveAll() {
        for (Map.Entry<Integer, Inventory> entry : inventories.entrySet()) {
            try {
                storage.saveStorageData(entry.getKey(), InventorySerializer.serialize(entry.getValue().getContents()));
                dirty.remove(entry.getKey());
            } catch (RuntimeException exception) {
                plugin.getLogger().severe("Cannot save storage for city id " + entry.getKey() + ": " + exception.getMessage());
            }
        }
    }

    public void startAutoSaveTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Integer cityId : dirty.keySet().toArray(Integer[]::new)) {
                Inventory inventory = inventories.get(cityId);
                if (inventory == null) {
                    dirty.remove(cityId);
                    continue;
                }
                try {
                    storage.saveStorageData(cityId, InventorySerializer.serialize(inventory.getContents()));
                    dirty.remove(cityId);
                } catch (RuntimeException exception) {
                    plugin.getLogger().severe("Cannot auto-save storage for city id " + cityId + ": " + exception.getMessage());
                }
            }
        }, 20L * 15L, 20L * 15L);
    }

    public void dropStorage(int cityId) {
        Inventory inventory = inventories.remove(cityId);
        if (inventory != null) {
            inventoryCities.remove(inventory);
        }
        dirty.remove(cityId);
    }

    public void refreshInventoryMetadata(City city) {
        save(city);
        closeCityStorageViewers(city.id());
        dropStorage(city.id());
        getCityInventory(city);
    }

    public boolean isStorageEmpty(City city) {
        Inventory inventory = getCityInventory(city);
        for (ItemStack item : inventory.getContents()) {
            if (item != null && !item.getType().isAir() && item.getAmount() > 0) {
                return false;
            }
        }
        return true;
    }

    public void playSound(Player player, String key) {
        if (!config.soundsEnabled()) {
            return;
        }
        Sound sound = config.sound(key);
        if (sound != null) {
            player.playSound(player.getLocation(), sound, 0.8f, 1.0f);
        }
    }

    public AuditLogService auditLogService() {
        return auditLogService;
    }

    @FunctionalInterface
    public interface CityLookup {
        Optional<City> findCity(int cityId);
    }
}
