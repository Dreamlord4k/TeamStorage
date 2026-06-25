package ru.syntaxteam.teamstorage.service;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.syntaxteam.teamstorage.config.PluginConfig;
import ru.syntaxteam.teamstorage.model.City;
import ru.syntaxteam.teamstorage.model.CityMember;
import ru.syntaxteam.teamstorage.util.TextUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class CityChatService {
    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final CityService cityService;
    private final PlayerSettingsService playerSettingsService;
    private BufferedWriter writer;

    public CityChatService(JavaPlugin plugin, PluginConfig config, CityService cityService, PlayerSettingsService playerSettingsService) {
        this.plugin = plugin;
        this.config = config;
        this.cityService = cityService;
        this.playerSettingsService = playerSettingsService;
        openWriter();
    }

    public ChatDecision prepare(Player player, String plainMessage) {
        if (!config.cityChatEnabled()) {
            return ChatDecision.global(plainMessage);
        }

        String trigger = config.cityChatTrigger();
        boolean triggered = trigger != null && !trigger.isEmpty() && plainMessage.startsWith(trigger);
        boolean toggled = playerSettingsService.isCityChatEnabled(player.getUniqueId());
        if (!triggered && !toggled) {
            return ChatDecision.global(plainMessage);
        }

        Optional<City> city = cityService.findPlayerCity(player.getUniqueId());
        String message = triggered ? plainMessage.substring(trigger.length()).stripLeading() : plainMessage;
        if (city.isEmpty()) {
            return triggered ? ChatDecision.global(message) : ChatDecision.global(plainMessage);
        }

        if (message.isBlank()) {
            return ChatDecision.cancel();
        }
        return ChatDecision.city(city.get(), message);
    }

    public void sendCityMessage(Player sender, City city, String message) {
        Component cityMessage = TextUtil.component(TextUtil.applyPlaceholders(config.cityChatFormat(), Map.of(
                "tag", city.tag(),
                "city", city.name(),
                "city_color", config.tabColor(city.tagColor()),
                "tag_color", config.tabColor(city.tagColor()),
                "player", sender.getName(),
                "message", message
        )));
        Component spyMessage = TextUtil.component(TextUtil.applyPlaceholders(config.cityChatSpyFormat(), Map.of(
                "tag", city.tag(),
                "city", city.name(),
                "city_color", config.tabColor(city.tagColor()),
                "tag_color", config.tabColor(city.tagColor()),
                "player", sender.getName(),
                "message", message
        )));

        for (CityMember member : cityService.members(city)) {
            Player recipient = Bukkit.getPlayer(member.uuid());
            if (recipient != null) {
                recipient.sendMessage(cityMessage);
            }
        }

        if (config.cityChatSpyEnabled()) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.hasPermission(config.cityChatSpyPermission()) && !isMember(online.getUniqueId(), city)) {
                    online.sendMessage(spyMessage);
                }
            }
        }

        writeLog(sender, city, message);
    }

    private boolean isMember(UUID playerId, City city) {
        return cityService.findPlayerCity(playerId)
                .map(playerCity -> playerCity.id() == city.id())
                .orElse(false);
    }

    private void openWriter() {
        if (!config.cityChatLogEnabled()) {
            return;
        }

        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Cannot create plugin data folder for city chat log.");
                return;
            }
            writer = new BufferedWriter(new FileWriter(new File(plugin.getDataFolder(), config.cityChatLogFileName()), true));
        } catch (IOException exception) {
            plugin.getLogger().warning("Cannot open city chat log file: " + exception.getMessage());
        }
    }

    private synchronized void writeLog(Player sender, City city, String message) {
        if (!config.cityChatLogEnabled() || writer == null) {
            return;
        }
        try {
            writer.write(String.join(" | ",
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now()),
                    "city=" + sanitize(city.name()),
                    "tag=" + sanitize(city.tag()),
                    "player=" + sanitize(sender.getName()),
                    "uuid=" + sender.getUniqueId(),
                    "message=" + sanitize(message)
            ));
            writer.newLine();
            writer.flush();
        } catch (IOException exception) {
            plugin.getLogger().warning("Cannot write city chat log: " + exception.getMessage());
        }
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.replace('\r', ' ').replace('\n', ' ');
    }

    public synchronized void close() {
        if (writer == null) {
            return;
        }
        try {
            writer.close();
        } catch (IOException exception) {
            plugin.getLogger().warning("Cannot close city chat log file: " + exception.getMessage());
        }
    }

    public record ChatDecision(boolean cancelOriginal, boolean cityMessage, City city, String message) {
        public static ChatDecision global(String message) {
            return new ChatDecision(false, false, null, message);
        }

        public static ChatDecision city(City city, String message) {
            return new ChatDecision(true, true, city, message);
        }

        public static ChatDecision cancel() {
            return new ChatDecision(true, false, null, "");
        }
    }
}
