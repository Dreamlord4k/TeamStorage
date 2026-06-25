package ru.syntaxteam.teamstorage.service;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.syntaxteam.teamstorage.config.PluginConfig;
import ru.syntaxteam.teamstorage.model.City;
import ru.syntaxteam.teamstorage.util.TextUtil;

import java.util.Map;

public final class TabNameService {
    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final CityService cityService;

    public TabNameService(JavaPlugin plugin, PluginConfig config, CityService cityService) {
        this.plugin = plugin;
        this.config = config;
        this.cityService = cityService;
    }

    public boolean active() {
        return config.tabEnabled();
    }

    public void refreshAll() {
        if (!active()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            refresh(player);
        }
    }

    public void refresh(Player player) {
        if (!active()) {
            return;
        }

        cityService.findPlayerCity(player.getUniqueId())
                .ifPresentOrElse(city -> applyCityTag(player, city), () -> clear(player));
    }

    public void clearAll() {
        if (!config.tabEnabled()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            clear(player);
        }
    }

    public void clear(Player player) {
        player.playerListName(Component.text(player.getName()));
    }

    private void applyCityTag(Player player, City city) {
        String color = config.tabColor(city.tagColor());
        String text = TextUtil.applyPlaceholders(config.tabFormat(), Map.of(
                "tag_color", color,
                "tag", city.tag(),
                "city", city.name(),
                "player", player.getName()
        ));
        player.playerListName(TextUtil.component(text));
    }

    public void logStatus() {
        plugin.getLogger().info(config.tabEnabled()
                ? "Tab tags are enabled in config.yml."
                : "Tab tags are disabled in config.yml.");
    }
}
