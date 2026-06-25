package ru.syntaxteam.teamstorage.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import ru.syntaxteam.teamstorage.config.PluginConfig;
import ru.syntaxteam.teamstorage.service.CityService;
import ru.syntaxteam.teamstorage.service.PlayerSettingsService;

import java.util.List;

public final class CityChatCommand implements TabExecutor {
    private final PluginConfig config;
    private final CityService cityService;
    private final PlayerSettingsService playerSettingsService;

    public CityChatCommand(PluginConfig config, CityService cityService, PlayerSettingsService playerSettingsService) {
        this.config = config;
        this.cityService = cityService;
        this.playerSettingsService = playerSettingsService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(config.message("only-player"));
            return true;
        }
        if (!config.cityChatToggleCommandEnabled()) {
            sender.sendMessage(config.message("no-permission"));
            return true;
        }
        if (cityService.findPlayerCity(player.getUniqueId()).isEmpty()) {
            sender.sendMessage(config.message("you-not-in-city"));
            return true;
        }

        boolean enabled = playerSettingsService.toggleCityChat(player.getUniqueId());
        player.sendMessage(config.message(enabled ? "chat-toggle-enabled" : "chat-toggle-disabled"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
