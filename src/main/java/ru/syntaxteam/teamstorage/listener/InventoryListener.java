package ru.syntaxteam.teamstorage.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import ru.syntaxteam.teamstorage.config.PluginConfig;
import ru.syntaxteam.teamstorage.gui.SelectionMenuHolder;
import ru.syntaxteam.teamstorage.model.City;
import ru.syntaxteam.teamstorage.service.CityService;
import ru.syntaxteam.teamstorage.service.StorageService;
import ru.syntaxteam.teamstorage.util.ItemDiff;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class InventoryListener implements Listener {
    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final CityService cityService;
    private final StorageService storageService;
    private final Map<Inventory, ItemStack[]> lastKnownContents = new IdentityHashMap<>();
    private final Set<Inventory> pendingReconcile = java.util.Collections.newSetFromMap(new IdentityHashMap<>());

    public InventoryListener(JavaPlugin plugin, PluginConfig config, CityService cityService, StorageService storageService) {
        this.plugin = plugin;
        this.config = config;
        this.cityService = cityService;
        this.storageService = storageService;
    }

    @EventHandler
    public void onSelectionMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getView().getTopInventory().getHolder() instanceof SelectionMenuHolder holder)) {
            return;
        }

        event.setCancelled(true);
        if (!holder.playerId().equals(player.getUniqueId())) {
            player.closeInventory();
            return;
        }

        int clickedSlot = event.getRawSlot();
        if (clickedSlot == config.itemSlot("personal")) {
            storageService.playSound(player, "click");
            Bukkit.getScheduler().runTask(plugin, () -> storageService.openPersonalEnderChest(player));
            return;
        }

        if (clickedSlot == config.itemSlot("city")) {
            Optional<City> currentCity = cityService.findPlayerCity(player.getUniqueId());
            if (currentCity.isEmpty() || currentCity.get().id() != holder.city().id()) {
                player.closeInventory();
                player.sendMessage(config.message("city-access-changed"));
                return;
            }
            storageService.playSound(player, "click");
            Bukkit.getScheduler().runTask(plugin, () -> storageService.openCityStorage(player, holder.city()));
        }
    }

    @EventHandler
    public void onSelectionMenuDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof SelectionMenuHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCityStorageClickAccessCheck(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        Optional<City> city = storageService.cityByInventory(top);
        if (city.isEmpty()) {
            return;
        }
        if (!hasStorageAccess(player, city.get())) {
            event.setCancelled(true);
            player.closeInventory();
            player.sendMessage(config.message("city-access-changed"));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCityStorageClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        Optional<City> city = storageService.cityByInventory(top);
        if (city.isEmpty()) {
            return;
        }

        scheduleReconcile(player, city.get(), top);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCityStorageDragAccessCheck(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        Optional<City> city = storageService.cityByInventory(top);
        if (city.isEmpty()) {
            return;
        }
        if (!hasStorageAccess(player, city.get())) {
            event.setCancelled(true);
            player.closeInventory();
            player.sendMessage(config.message("city-access-changed"));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCityStorageDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        Optional<City> city = storageService.cityByInventory(top);
        if (city.isEmpty()) {
            return;
        }

        scheduleReconcile(player, city.get(), top);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        Inventory inventory = event.getInventory();
        storageService.cityByInventory(inventory).ifPresent(city -> {
            compareAndLog(player, city, inventory);
            storageService.save(city);
            storageService.markClosed(player, city.id());
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        storageService.clearViewer(event.getPlayer());
    }

    private boolean hasStorageAccess(Player player, City city) {
        if (player.hasPermission("teamstorage.admin")) {
            return true;
        }
        return cityService.findPlayerCity(player.getUniqueId())
                .map(currentCity -> currentCity.id() == city.id())
                .orElse(false);
    }

    private void scheduleReconcile(Player player, City city, Inventory inventory) {
        lastKnownContents.putIfAbsent(inventory, copy(inventory.getContents()));
        if (!pendingReconcile.add(inventory)) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            pendingReconcile.remove(inventory);
            compareAndLog(player, city, inventory);
        });
    }

    private void compareAndLog(Player player, City city, Inventory inventory) {
        ItemStack[] before = lastKnownContents.getOrDefault(inventory, copy(inventory.getContents()));
        ItemStack[] after = inventory.getContents();
        List<ItemDiff.Change> changes = ItemDiff.diff(before, after);
        lastKnownContents.put(inventory, copy(after));
        if (changes.isEmpty()) {
            return;
        }

        storageService.markDirty(city);
        storageService.save(city);
        for (ItemDiff.Change change : changes) {
            if (change.amount() > 0) {
                storageService.auditLogService().logItemAdded(player, city, change.item(), change.amount());
            } else {
                storageService.auditLogService().logItemRemoved(player, city, change.item(), Math.abs(change.amount()));
            }
        }
    }

    private static ItemStack[] copy(ItemStack[] contents) {
        ItemStack[] copy = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            copy[i] = contents[i] == null ? null : contents[i].clone();
        }
        return copy;
    }
}
