package ru.syntaxteam.teamstorage.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import ru.syntaxteam.teamstorage.config.PluginConfig;
import ru.syntaxteam.teamstorage.model.City;
import ru.syntaxteam.teamstorage.model.CityMember;
import ru.syntaxteam.teamstorage.service.CityService;
import ru.syntaxteam.teamstorage.service.TabNameService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class CityCommand implements TabExecutor {
    private static final java.util.regex.Pattern CITY_NAME_PATTERN = java.util.regex.Pattern.compile("[\\p{L}\\p{N}_.-]{2,32}");
    private static final java.util.regex.Pattern CITY_TAG_PATTERN = java.util.regex.Pattern.compile("[\\p{L}\\p{N}]{2,4}");
    private static final java.util.regex.Pattern HEX_COLOR_PATTERN = java.util.regex.Pattern.compile("#[a-fA-F0-9]{6}");
    private final PluginConfig config;
    private final CityService cityService;
    private final TabNameService tabNameService;

    public CityCommand(PluginConfig config, CityService cityService, TabNameService tabNameService) {
        this.config = config;
        this.cityService = cityService;
        this.tabNameService = tabNameService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("teamstorage.admin")) {
            sender.sendMessage(config.message("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(config.message("usage-city"));
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> create(sender, args);
            case "delete" -> delete(sender, args);
            case "add" -> add(sender, args);
            case "remove" -> remove(sender, args);
            case "info" -> info(sender, args);
            case "list" -> list(sender);
            case "tag" -> tag(sender, args);
            case "tagcolor" -> tagColor(sender, args);
            default -> sender.sendMessage(config.message("usage-city"));
        }
        return true;
    }

    private void create(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sender.sendMessage(config.message("usage-city-create"));
            return;
        }
        String name = args[1];
        String tag = args[2].toUpperCase(Locale.ROOT);
        if (!isValidCityName(name)) {
            sender.sendMessage(config.message("invalid-city-name"));
            return;
        }
        if (!isValidCityTag(tag)) {
            sender.sendMessage(config.message("invalid-city-tag"));
            return;
        }
        if (cityService.findCity(name).isPresent()) {
            sender.sendMessage(config.message("city-exists", Map.of("city", name)));
            return;
        }
        if (cityService.findCityByTag(tag).isPresent()) {
            sender.sendMessage(config.message("city-tag-exists", Map.of("tag", tag)));
            return;
        }
        City city = cityService.createCity(name, tag);
        sender.sendMessage(config.message("city-created", Map.of("city", city.name(), "tag", city.tag())));
    }

    private void delete(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(config.message("usage-city-delete"));
            return;
        }
        if (!isValidCityName(args[1])) {
            sender.sendMessage(config.message("invalid-city-name"));
            return;
        }
        Optional<City> city = cityService.findCity(args[1]);
        if (city.isEmpty()) {
            sender.sendMessage(config.message("city-not-found", Map.of("city", args[1])));
            return;
        }
        if (!cityService.canDeleteCity(city.get())) {
            sender.sendMessage(config.message("city-delete-storage-not-empty", Map.of("city", city.get().name())));
            return;
        }
        cityService.deleteCity(city.get());
        tabNameService.refreshAll();
        sender.sendMessage(config.message("city-deleted", Map.of("city", city.get().name())));
    }

    private void add(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sender.sendMessage(config.message("usage-city-add"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!isValidCityName(args[2])) {
            sender.sendMessage(config.message("invalid-city-name"));
            return;
        }
        Optional<City> city = cityService.findCity(args[2]);
        if (city.isEmpty()) {
            sender.sendMessage(config.message("city-not-found", Map.of("city", args[2])));
            return;
        }
        String lastName = target.getName() == null ? args[1] : target.getName();
        cityService.addPlayerToCity(target.getUniqueId(), lastName, city.get());
        if (target.getPlayer() != null) {
            tabNameService.refresh(target.getPlayer());
        }
        sender.sendMessage(config.message("city-added-player", Map.of("player", lastName, "city", city.get().name())));
    }

    private void remove(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(config.message("usage-city-remove"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        String lastName = target.getName() == null ? args[1] : target.getName();
        UUID playerId = target.getUniqueId();
        if (!cityService.removePlayer(playerId)) {
            sender.sendMessage(config.message("player-not-in-city", Map.of("player", lastName)));
            return;
        }
        if (target.getPlayer() != null) {
            tabNameService.refresh(target.getPlayer());
        }
        sender.sendMessage(config.message("city-removed-player", Map.of("player", lastName)));
    }

    private void info(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(config.message("usage-city-info"));
            return;
        }
        if (!isValidCityName(args[1])) {
            sender.sendMessage(config.message("invalid-city-name"));
            return;
        }
        Optional<City> city = cityService.findCity(args[1]);
        if (city.isEmpty()) {
            sender.sendMessage(config.message("city-not-found", Map.of("city", args[1])));
            return;
        }
        List<String> names = cityService.members(city.get()).stream()
                .map(CityMember::lastName)
                .toList();
        sender.sendMessage(config.message("city-info", Map.of(
                "city", city.get().name(),
                "tag", city.get().tag(),
                "color", city.get().tagColor(),
                "members", names.isEmpty() ? "0" : String.join(", ", names)
        )));
    }

    private void list(CommandSender sender) {
        List<City> cities = cityService.listCities();
        if (cities.isEmpty()) {
            sender.sendMessage(config.message("city-list-empty"));
            return;
        }
        sender.sendMessage(config.message("city-list", Map.of(
                "cities", String.join(", ", cities.stream().map(city -> city.name() + "(" + city.tag() + ")").toList())
        )));
    }

    private void tag(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sender.sendMessage(config.message("usage-city-tag"));
            return;
        }
        Optional<City> city = findValidCity(sender, args[1]);
        if (city.isEmpty()) {
            return;
        }
        String tag = args[2].toUpperCase(Locale.ROOT);
        if (!isValidCityTag(tag)) {
            sender.sendMessage(config.message("invalid-city-tag"));
            return;
        }
        Optional<City> existing = cityService.findCityByTag(tag);
        if (existing.isPresent() && existing.get().id() != city.get().id()) {
            sender.sendMessage(config.message("city-tag-exists", Map.of("tag", tag)));
            return;
        }
        City updated = cityService.updateTag(city.get(), tag).orElse(city.get());
        tabNameService.refreshAll();
        sender.sendMessage(config.message("city-tag-updated", Map.of("city", updated.name(), "tag", updated.tag())));
    }

    private void tagColor(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sender.sendMessage(config.message("usage-city-tagcolor"));
            return;
        }
        Optional<City> city = findValidCity(sender, args[1]);
        if (city.isEmpty()) {
            return;
        }
        String color = args[2].toLowerCase(Locale.ROOT);
        if (!isValidColor(color)) {
            sender.sendMessage(config.message("invalid-tag-color"));
            return;
        }
        City updated = cityService.updateTagColor(city.get(), color).orElse(city.get());
        tabNameService.refreshAll();
        sender.sendMessage(config.message("city-tag-color-updated", Map.of("city", updated.name(), "color", updated.tagColor())));
    }

    private Optional<City> findValidCity(CommandSender sender, String cityName) {
        if (!isValidCityName(cityName)) {
            sender.sendMessage(config.message("invalid-city-name"));
            return Optional.empty();
        }
        Optional<City> city = cityService.findCity(cityName);
        if (city.isEmpty()) {
            sender.sendMessage(config.message("city-not-found", Map.of("city", cityName)));
        }
        return city;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("teamstorage.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            return filter(List.of("create", "delete", "add", "remove", "info", "list", "tag", "tagcolor"), args[0]);
        }
        if ((args.length == 2 && List.of("delete", "info", "tag", "tagcolor").contains(args[0].toLowerCase(Locale.ROOT)))
                || (args.length == 3 && args[0].equalsIgnoreCase("add"))) {
            return filter(cityService.listCities().stream().map(City::name).toList(), args[args.length - 1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("tagcolor")) {
            return filter(config.tabColorNames(), args[2]);
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

    private static boolean isValidCityName(String name) {
        return CITY_NAME_PATTERN.matcher(name).matches();
    }

    private static boolean isValidCityTag(String tag) {
        return CITY_TAG_PATTERN.matcher(tag).matches();
    }

    private boolean isValidColor(String color) {
        return HEX_COLOR_PATTERN.matcher(color).matches() || config.tabColorNames().contains(color);
    }
}
