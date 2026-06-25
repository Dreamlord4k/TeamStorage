package ru.syntaxteam.teamstorage.service;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import ru.syntaxteam.teamstorage.config.PluginConfig;
import ru.syntaxteam.teamstorage.model.City;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public final class AuditLogService {
    private final JavaPlugin plugin;
    private final PluginConfig config;
    private BufferedWriter writer;

    public AuditLogService(JavaPlugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
        openWriter();
    }

    public void logOpen(Player player, City city) {
        write("OPEN", player, city, null, 0);
    }

    public void logItemAdded(Player player, City city, ItemStack item, int amount) {
        write("ADD", player, city, item, amount);
    }

    public void logItemRemoved(Player player, City city, ItemStack item, int amount) {
        write("REMOVE", player, city, item, amount);
    }

    private synchronized void write(String action, Player player, City city, ItemStack item, int amount) {
        if (!config.loggingEnabled() || writer == null) {
            return;
        }

        String itemText = "-";
        if (item != null) {
            String displayName = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                    ? " name=" + sanitize(PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName()))
                    : "";
            itemText = item.getType().name() + displayName;
        }
        String line = String.join(" | ",
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now()),
                "action=" + action,
                "player=" + sanitize(player.getName()),
                "uuid=" + player.getUniqueId(),
                "city=" + sanitize(city.name()),
                "amount=" + amount,
                "item=" + itemText
        );

        try {
            writer.write(line);
            writer.newLine();
            writer.flush();
        } catch (IOException exception) {
            plugin.getLogger().warning("Cannot write city storage log: " + exception.getMessage());
        }
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.replace('\r', ' ').replace('\n', ' ');
    }

    private void openWriter() {
        if (!config.loggingEnabled()) {
            return;
        }

        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Cannot create plugin data folder for audit log.");
                return;
            }
            File logFile = new File(plugin.getDataFolder(), config.logFileName());
            writer = new BufferedWriter(new FileWriter(logFile, true));
        } catch (IOException exception) {
            plugin.getLogger().warning("Cannot open city storage log file: " + exception.getMessage());
        }
    }

    public synchronized void close() {
        if (writer == null) {
            return;
        }
        try {
            writer.close();
        } catch (IOException exception) {
            plugin.getLogger().warning("Cannot close city storage log file: " + exception.getMessage());
        }
    }
}
