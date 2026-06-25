package ru.syntaxteam.teamstorage.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import ru.syntaxteam.teamstorage.config.PluginConfig;
import ru.syntaxteam.teamstorage.model.City;
import ru.syntaxteam.teamstorage.service.CityService;
import ru.syntaxteam.teamstorage.service.PlayerSettingsService;
import ru.syntaxteam.teamstorage.service.StorageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class TeamStorageCommand implements TabExecutor {
    private static final java.util.regex.Pattern CITY_NAME_PATTERN = java.util.regex.Pattern.compile("[\\p{L}\\p{N}_.-]{2,32}");
    private final PluginConfig config;
    private final CityService cityService;
    private final PlayerSettingsService playerSettingsService;
    private final StorageService storageService;

    public TeamStorageCommand(PluginConfig config, CityService cityService, PlayerSettingsService playerSettingsService, StorageService storageService) {
        this.config = config;
        this.cityService = cityService;
        this.playerSettingsService = playerSettingsService;
        this.storageService = storageService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("/teamstorage toggle");
            if (sender.hasPermission("teamstorage.admin")) {
                sender.sendMessage("/teamstorage open [city]");
            }
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "toggle" -> toggle(sender);
            case "open" -> open(sender, args);
            default -> sender.sendMessage("/teamstorage toggle");
        }
        return true;
    }

    private void toggle(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(config.message("only-player"));
            return;
        }
        if (!sender.hasPermission("teamstorage.toggle")) {
            sender.sendMessage(config.message("no-permission"));
            return;
        }
        if (cityService.findPlayerCity(player.getUniqueId()).isEmpty()) {
            sender.sendMessage(config.message("you-not-in-city"));
            return;
        }

        boolean enabled = playerSettingsService.toggleCityStorageMenu(player.getUniqueId());
        sender.sendMessage(config.message(enabled ? "toggle-enabled" : "toggle-disabled"));
    }

    private void open(CommandSender sender, String[] args) {
        if (!sender.hasPermission("teamstorage.admin")) {
            sender.sendMessage(config.message("no-permission"));
            return;
        }
        if (!config.adminOpenCommandEnabled()) {
            sender.sendMessage(config.message("admin-open-disabled"));
            return;
        }

        Optional<City> city;
        if (args.length >= 2) {
            if (!CITY_NAME_PATTERN.matcher(args[1]).matches()) {
                sender.sendMessage(config.message("invalid-city-name"));
                return;
            }
            city = cityService.findCity(args[1]);
            if (city.isEmpty()) {
                sender.sendMessage(config.message("city-not-found", Map.of("city", args[1])));
                return;
            }
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("/teamstorage open <city>");
                return;
            }
            city = cityService.findPlayerCity(player.getUniqueId());
            if (city.isEmpty()) {
                sender.sendMessage(config.message("you-not-in-city"));
                return;
            }
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(config.message("only-player"));
            return;
        }
        storageService.openCityStorage(player, city.get());
        sender.sendMessage(config.message("opened-city-storage", Map.of("city", city.get().name())));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> values = new ArrayList<>();
            values.add("toggle");
            if (sender.hasPermission("teamstorage.admin")) {
                values.add("open");
            }
            return filter(values, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("open") && sender.hasPermission("teamstorage.admin")) {
            return filter(cityService.listCities().stream().map(City::name).toList(), args[1]);
        }
        return List.of();
    }

    private static List<String> filter(List<String> source, String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : source) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(value);
            }
        }
        return result;
    }
}
