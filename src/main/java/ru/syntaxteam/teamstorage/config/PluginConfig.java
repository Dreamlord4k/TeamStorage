package ru.syntaxteam.teamstorage.config;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.plugin.java.JavaPlugin;
import ru.syntaxteam.teamstorage.model.City;
import ru.syntaxteam.teamstorage.util.TextUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PluginConfig {
    private final JavaPlugin plugin;

    public PluginConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public String language() {
        String language = plugin.getConfig().getString("language", "ru");
        return "en".equalsIgnoreCase(language) ? "en" : "ru";
    }

    public int storageSize() {
        int size = plugin.getConfig().getInt("storage.size", 54);
        if (size < 9 || size > 54 || size % 9 != 0) {
            plugin.getLogger().warning("Invalid storage.size in config.yml. Using 54.");
            return 54;
        }
        return size;
    }

    public net.kyori.adventure.text.Component storageTitle(City city) {
        return TextUtil.component(TextUtil.applyPlaceholders(
                localized("storage.title", "&2Хранилище города: %city_color%%city%"),
                cityPlaceholders(city)
        ));
    }

    public int storageTitleCityNameMaxLength() {
        return plugin.getConfig().getInt("storage.title-city-name-max-length", 14);
    }

    public Map<String, String> cityPlaceholders(City city) {
        String displayName = city.name().length() > storageTitleCityNameMaxLength() ? city.tag() : city.name();
        String cityColor = tabColor(city.tagColor());
        return Map.of(
                "city", displayName,
                "city_plain", displayName,
                "city_full", city.name(),
                "tag", city.tag(),
                "city_color", cityColor,
                "tag_color", cityColor
        );
    }

    public boolean adminOpenCommandEnabled() {
        return plugin.getConfig().getBoolean("storage.admin-open-command-enabled", true);
    }

    public net.kyori.adventure.text.Component menuTitle() {
        return TextUtil.component(localized("menu.title", "&2Эндер-сундук"));
    }

    public int menuSize() {
        int size = plugin.getConfig().getInt("menu.size", 27);
        if (size < 9 || size > 54 || size % 9 != 0) {
            return 27;
        }
        return size;
    }

    public int itemSlot(String key) {
        return plugin.getConfig().getInt("menu.items." + key + ".slot", key.equals("personal") ? 11 : 15);
    }

    public ItemStack menuItem(String key, Map<String, String> placeholders) {
        String path = "menu.items." + key;
        Material material = Material.matchMaterial(plugin.getConfig().getString(path + ".material", "CHEST"));
        if (material == null) {
            material = Material.CHEST;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtil.component(applyPlaceholders(localized(path + ".name", key), placeholders)));

            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            for (String line : localizedList(path + ".lore")) {
                lore.add(TextUtil.component(applyPlaceholders(line, placeholders)));
            }
            meta.lore(lore);

            int customModelData = plugin.getConfig().getInt(path + ".custom-model-data", 0);
            if (customModelData > 0) {
                CustomModelDataComponent modelData = meta.getCustomModelDataComponent();
                modelData.setFloats(List.of((float) customModelData));
                meta.setCustomModelDataComponent(modelData);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean soundsEnabled() {
        return plugin.getConfig().getBoolean("menu.sounds.enabled", true);
    }

    public Sound sound(String key) {
        String raw = plugin.getConfig().getString("menu.sounds." + key, "");
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String normalized = raw.toLowerCase(java.util.Locale.ROOT);
        NamespacedKey namespacedKey;
        if (!normalized.contains(":") && normalized.contains("_")) {
            namespacedKey = NamespacedKey.minecraft(normalized.replace('_', '.'));
        } else {
            namespacedKey = NamespacedKey.fromString(normalized);
        }
        if (namespacedKey == null) {
            namespacedKey = NamespacedKey.minecraft(normalized.replace('_', '.'));
        }

        Sound sound = Registry.SOUNDS.get(namespacedKey);
        if (sound == null) {
            plugin.getLogger().warning("Unknown sound in config.yml: " + raw);
        }
        return sound;
    }

    public boolean loggingEnabled() {
        return plugin.getConfig().getBoolean("logging.enabled", true);
    }

    public String logFileName() {
        return plugin.getConfig().getString("logging.file", "team-storage.log");
    }

    public boolean tabEnabled() {
        return plugin.getConfig().getBoolean("tab.enabled", true);
    }

    public String tabFormat() {
        return localized("tab.format", "&7[%tag_color%%tag%&7] &f%player%");
    }

    public String tabColor(String color) {
        String normalized = normalizeColor(color);
        if (normalized.startsWith("#")) {
            return normalized;
        }
        return plugin.getConfig().getString("tab.colors." + normalized, "&a");
    }

    public List<String> tabColorNames() {
        if (plugin.getConfig().getConfigurationSection("tab.colors") == null) {
            return List.of("green", "blue", "red", "gold", "aqua", "gray", "white", "yellow");
        }
        return new ArrayList<>(plugin.getConfig().getConfigurationSection("tab.colors").getKeys(false));
    }

    public boolean cityChatEnabled() {
        return plugin.getConfig().getBoolean("city-chat.enabled", true);
    }

    public String cityChatTrigger() {
        return plugin.getConfig().getString("city-chat.trigger", "@");
    }

    public boolean cityChatToggleCommandEnabled() {
        return plugin.getConfig().getBoolean("city-chat.toggle-command-enabled", true);
    }

    public boolean cityChatSpyEnabled() {
        return plugin.getConfig().getBoolean("city-chat.spy.enabled", true);
    }

    public String cityChatSpyPermission() {
        return plugin.getConfig().getString("city-chat.spy.permission", "teamstorage.chat.spy");
    }

    public String cityChatFormat() {
        return localized("city-chat.format", "&8[&2%city%&8] &f%player%&7: &f%message%");
    }

    public String cityChatSpyFormat() {
        return localized("city-chat.spy.format", "&8[&cSpy&8][&2%city%&8] &f%player%&7: &f%message%");
    }

    public boolean cityChatLogEnabled() {
        return plugin.getConfig().getBoolean("city-chat.log.enabled", true);
    }

    public String cityChatLogFileName() {
        return plugin.getConfig().getString("city-chat.log.file", "city-chat.log");
    }

    public String message(String key) {
        String prefix = localized("messages.prefix", "");
        String message = localized("messages." + key, plugin.getConfig().getString("messages." + key, key));
        return color(prefix + message);
    }

    public String message(String key, Map<String, String> placeholders) {
        return applyPlaceholders(message(key), placeholders);
    }

    public static String color(String value) {
        return TextUtil.color(value);
    }

    private static String applyPlaceholders(String input, Map<String, String> placeholders) {
        String result = input;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return result;
    }

    private String localized(String path, String fallback) {
        if (plugin.getConfig().isConfigurationSection(path)) {
            String value = plugin.getConfig().getString(path + "." + language());
            if (value != null) {
                return value;
            }
            value = plugin.getConfig().getString(path + ".en");
            if (value != null) {
                return value;
            }
            value = plugin.getConfig().getString(path + ".ru");
            if (value != null) {
                return value;
            }
        }
        return plugin.getConfig().getString(path, fallback);
    }

    private List<String> localizedList(String path) {
        if (plugin.getConfig().isConfigurationSection(path)) {
            List<String> value = plugin.getConfig().getStringList(path + "." + language());
            if (!value.isEmpty()) {
                return value;
            }
            value = plugin.getConfig().getStringList(path + ".en");
            if (!value.isEmpty()) {
                return value;
            }
            value = plugin.getConfig().getStringList(path + ".ru");
            if (!value.isEmpty()) {
                return value;
            }
        }
        return plugin.getConfig().getStringList(path);
    }

    private static String normalizeColor(String color) {
        return color == null ? "green" : color.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
