package fr.mpieuvre.repeat;

public enum DeliveryMode {
    CHAT,
    ACTION_BAR;

    public static DeliveryMode fromString(String value) {
        if (value == null || value.isBlank()) {
            return CHAT;
        }

        String normalized = value.trim().toUpperCase().replace('-', '_');
        if ("ACTIONBAR".equals(normalized.replace("_", ""))) {
            return ACTION_BAR;
        }

        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            return CHAT;
        }
    }

    public String configValue() {
        return name().toLowerCase();
    }
}
