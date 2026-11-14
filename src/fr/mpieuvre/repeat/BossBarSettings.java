package fr.mpieuvre.repeat;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class BossBarSettings {
    public static final BarColor DEFAULT_COLOR = BarColor.BLUE;
    public static final BarStyle DEFAULT_STYLE = BarStyle.SOLID;
    public static final int DEFAULT_DURATION_SECONDS = 10;
    public static final double DEFAULT_PROGRESS = 1.0D;

    private BarColor color;
    private BarStyle style;
    private int durationSeconds;
    private double progress;

    public BossBarSettings() {
        this(DEFAULT_COLOR, DEFAULT_STYLE, DEFAULT_DURATION_SECONDS, DEFAULT_PROGRESS);
    }

    public BossBarSettings(BarColor color, BarStyle style, int durationSeconds, double progress) {
        this.color = color == null ? DEFAULT_COLOR : color;
        this.style = style == null ? DEFAULT_STYLE : style;
        this.durationSeconds = Math.max(0, durationSeconds);
        this.progress = clampProgress(progress);
    }

    public BarColor getColor() {
        return color;
    }

    public void setColor(BarColor color) {
        this.color = color == null ? DEFAULT_COLOR : color;
    }

    public BarStyle getStyle() {
        return style;
    }

    public void setStyle(BarStyle style) {
        this.style = style == null ? DEFAULT_STYLE : style;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = Math.max(0, durationSeconds);
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = clampProgress(progress);
    }

    public int getProgressPercent() {
        return (int) Math.round(progress * 100.0D);
    }

    public BossBarSettings copy() {
        return new BossBarSettings(color, style, durationSeconds, progress);
    }

    public Map<String, Object> toConfigMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("color", color.name().toLowerCase(Locale.ROOT));
        map.put("style", style.name().toLowerCase(Locale.ROOT));
        map.put("durationSeconds", durationSeconds);
        map.put("progress", progress);
        return map;
    }

    public static BossBarSettings fromConfig(Object raw) {
        BossBarSettings settings = new BossBarSettings();
        if (!(raw instanceof Map<?, ?> map)) {
            return settings;
        }

        Object colorObject = map.get("color");
        if (colorObject instanceof String colorText) {
            BarColor parsed = parseColor(colorText);
            if (parsed != null) {
                settings.setColor(parsed);
            }
        }

        Object styleObject = map.get("style");
        if (styleObject instanceof String styleText) {
            BarStyle parsed = parseStyle(styleText);
            if (parsed != null) {
                settings.setStyle(parsed);
            }
        }

        Object durationObject = map.get("durationSeconds");
        if (durationObject instanceof Number number) {
            settings.setDurationSeconds(number.intValue());
        } else if (durationObject instanceof String durationText) {
            try {
                settings.setDurationSeconds(Integer.parseInt(durationText.trim()));
            } catch (NumberFormatException ignored) {
                // keep default
            }
        }

        Object progressObject = map.get("progress");
        if (progressObject instanceof Number number) {
            settings.setProgress(normalizeProgressValue(number.doubleValue()));
        } else if (progressObject instanceof String progressText) {
            try {
                settings.setProgress(normalizeProgressValue(Double.parseDouble(progressText.trim())));
            } catch (NumberFormatException ignored) {
                // keep default
            }
        }

        return settings;
    }

    public static BarColor parseColor(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return BarColor.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public static BarStyle parseStyle(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        return switch (normalized) {
            case "SOLID", "FULL" -> BarStyle.SOLID;
            case "6", "SEGMENTED6", "SEGMENTED_6" -> BarStyle.SEGMENTED_6;
            case "10", "SEGMENTED10", "SEGMENTED_10" -> BarStyle.SEGMENTED_10;
            case "12", "SEGMENTED12", "SEGMENTED_12" -> BarStyle.SEGMENTED_12;
            case "20", "SEGMENTED20", "SEGMENTED_20" -> BarStyle.SEGMENTED_20;
            default -> {
                try {
                    yield BarStyle.valueOf(normalized);
                } catch (IllegalArgumentException exception) {
                    yield null;
                }
            }
        };
    }

    public static String formatColor(BarColor color) {
        return (color == null ? DEFAULT_COLOR : color).name().toLowerCase(Locale.ROOT);
    }

    public static String formatStyle(BarStyle style) {
        return (style == null ? DEFAULT_STYLE : style).name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    public String summary() {
        String text = formatColor(color) + " / " + formatStyle(style) + " / " + durationSeconds + "s";
        BossBarPreset preset = BossBarPreset.match(this);
        if (preset != null) {
            text += " / like " + preset.displayName();
        }
        return text;
    }

    private static double normalizeProgressValue(double value) {
        if (value > 1.0D) {
            return value / 100.0D;
        }
        return value;
    }

    private static double clampProgress(double value) {
        if (value < 0.0D) {
            return 0.0D;
        }
        if (value > 1.0D) {
            return 1.0D;
        }
        return value;
    }
}
