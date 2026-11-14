package fr.mpieuvre.repeat;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;

import java.util.Locale;

public enum BossBarPreset {
    WITHER("Wither", "&5Wither", BarColor.PURPLE, BarStyle.SOLID, 15),
    DRAGON("Ender Dragon", "&dEnder Dragon", BarColor.PINK, BarStyle.SOLID, 20),
    RAID("Raid", "&cRaid", BarColor.RED, BarStyle.SEGMENTED_10, 12);

    private final String displayName;
    private final String title;
    private final BarColor color;
    private final BarStyle style;
    private final int durationSeconds;

    BossBarPreset(String displayName, String title, BarColor color, BarStyle style, int durationSeconds) {
        this.displayName = displayName;
        this.title = title;
        this.color = color;
        this.style = style;
        this.durationSeconds = durationSeconds;
    }

    public String displayName() {
        return displayName;
    }

    public String title() {
        return title;
    }

    public void apply(BossBarSettings settings) {
        settings.setColor(color);
        settings.setStyle(style);
        settings.setDurationSeconds(durationSeconds);
        settings.setProgress(1.0D);
    }

    public BossBarSettings toSettings() {
        BossBarSettings settings = new BossBarSettings();
        apply(settings);
        return settings;
    }

    public static BossBarPreset fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace("-", "_");
        return switch (normalized) {
            case "WITHER", "WITHER_BOSS" -> WITHER;
            case "DRAGON", "ENDER_DRAGON", "ENDERDRAGON", "END" -> DRAGON;
            case "RAID", "PILLAGER", "VILLAGE" -> RAID;
            default -> {
                try {
                    yield valueOf(normalized);
                } catch (IllegalArgumentException exception) {
                    yield null;
                }
            }
        };
    }

    public static BossBarPreset match(BossBarSettings settings) {
        if (settings == null) {
            return null;
        }
        for (BossBarPreset preset : values()) {
            if (settings.getColor() == preset.color && settings.getStyle() == preset.style) {
                return preset;
            }
        }
        return null;
    }
}
