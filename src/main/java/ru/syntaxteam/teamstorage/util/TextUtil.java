package ru.syntaxteam.teamstorage.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextUtil {
    private static final Pattern HEX_PATTERN = Pattern.compile("#[a-fA-F0-9]{6}");
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    private TextUtil() {
    }

    public static Component component(String legacyText) {
        return LEGACY_SERIALIZER.deserialize(color(legacyText));
    }

    public static String color(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        Matcher matcher = HEX_PATTERN.matcher(value);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(builder, Matcher.quoteReplacement(toLegacyHex(matcher.group())));
        }
        matcher.appendTail(builder);
        return translateAlternateColorCodes(builder.toString());
    }

    public static String applyPlaceholders(String input, Map<String, String> placeholders) {
        String result = input;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return result;
    }

    private static String toLegacyHex(String hex) {
        StringBuilder builder = new StringBuilder("§x");
        for (int i = 1; i < hex.length(); i++) {
            builder.append('§').append(hex.charAt(i));
        }
        return builder.toString();
    }

    private static String translateAlternateColorCodes(String value) {
        char[] characters = value.toCharArray();
        for (int index = 0; index < characters.length - 1; index++) {
            if (characters[index] == '&' && isLegacyColorCode(characters[index + 1])) {
                characters[index] = '§';
                characters[index + 1] = Character.toLowerCase(characters[index + 1]);
            }
        }
        return new String(characters);
    }

    private static boolean isLegacyColorCode(char value) {
        char normalized = Character.toLowerCase(value);
        return normalized >= '0' && normalized <= '9'
                || normalized >= 'a' && normalized <= 'f'
                || normalized >= 'k' && normalized <= 'o'
                || normalized == 'r'
                || normalized == 'x';
    }
}
