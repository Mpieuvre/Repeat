package fr.mpieuvre.repeat;

import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextUtils {
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("(?i)&#([0-9a-f]{6})");

    private TextUtils() {
    }

    public static String colorize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        return ChatColor.translateAlternateColorCodes('&', translateHexColors(text));
    }

    private static String translateHexColors(String input) {
        Matcher matcher = HEX_COLOR_PATTERN.matcher(input);
        StringBuilder out = new StringBuilder(input.length());
        int lastEnd = 0;

        while (matcher.find()) {
            out.append(input, lastEnd, matcher.start());

            String hex = matcher.group(1);
            out.append('\u00A7').append('x');
            for (int i = 0; i < hex.length(); i++) {
                out.append('\u00A7').append(Character.toLowerCase(hex.charAt(i)));
            }

            lastEnd = matcher.end();
        }

        out.append(input, lastEnd, input.length());
        return out.toString();
    }
}
