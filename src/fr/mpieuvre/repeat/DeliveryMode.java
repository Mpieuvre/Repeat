package fr.mpieuvre.repeat;

import java.util.Locale;

public enum DeliveryMode {
    CHAT,
    ACTION_BAR,
    BOSS_BAR;

    public static DeliveryMode fromString(String value) {
        if (value == null || value.isBlank()) {
            return CHAT;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        String compact = normalized.replace("_", "");

        return switch (compact) {
            case "ACTIONBAR", "ACTION" -> ACTION_BAR;
            case "BOSSBAR", "BOSS", "BAR" -> BOSS_BAR;
            case "CHAT" -> CHAT;
            default -> {
                try {
                    yield valueOf(normalized);
                } catch (IllegalArgumentException exception) {
                    yield CHAT;
                }
            }
        };
    }

    public static boolean isKnown(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String compact = value.trim().toUpperCase(Locale.ROOT).replace("-", "").replace("_", "");
        return compact.equals("CHAT")
                || compact.equals("ACTIONBAR")
                || compact.equals("ACTION")
                || compact.equals("BOSSBAR")
                || compact.equals("BOSS")
                || compact.equals("BAR");
    }

    public String configValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String displayName() {
        return configValue().replace('_', '-');
    }
}
