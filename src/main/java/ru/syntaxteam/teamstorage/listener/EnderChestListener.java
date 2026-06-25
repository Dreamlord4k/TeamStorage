package ru.syntaxteam.teamstorage.listener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import ru.syntaxteam.teamstorage.model.City;
import ru.syntaxteam.teamstorage.service.CityService;
import ru.syntaxteam.teamstorage.service.PlayerSettingsService;
import ru.syntaxteam.teamstorage.service.StorageService;

import java.util.Optional;

public final class EnderChestListener implements Listener {
    private final org.bukkit.plugin.java.JavaPlugin plugin;
    private final CityService cityService;
    private final PlayerSettingsService playerSettingsService;
    private final StorageService storageService;

    public EnderChestListener(org.bukkit.plugin.java.JavaPlugin plugin, CityService cityService,
                              PlayerSettingsService playerSettingsService, StorageService storageService) {
        this.plugin = plugin;
        this.cityService = cityService;
        this.playerSettingsService = playerSettingsService;
        this.storageService = storageService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEnderChestInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.ENDER_CHEST) {
            return;
        }

        Player player = event.getPlayer();
        Optional<City> city = cityService.findPlayerCity(player.getUniqueId());
        if (city.isEmpty() || !playerSettingsService.isCityStorageMenuEnabled(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.openInventory(storageService.createSelectionMenu(player, city.get()));
            storageService.playSound(player, "open");
        });
    }
}
