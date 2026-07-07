package fr.mpieuvre.repeat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MessageEntry {
    private String message;
    private long intervalSeconds;
    private DeliveryMode deliveryMode;
    private List<String> worlds;

    public MessageEntry(String message, long intervalSeconds) {
        this(message, intervalSeconds, DeliveryMode.CHAT, Collections.emptyList());
    }

    public MessageEntry(String message, long intervalSeconds, DeliveryMode deliveryMode, List<String> worlds) {
        this.message = message == null ? "" : message.trim();
        this.intervalSeconds = Math.max(1L, intervalSeconds);
        this.deliveryMode = deliveryMode == null ? DeliveryMode.CHAT : deliveryMode;
        this.worlds = normalizeWorlds(worlds);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message == null ? "" : message.trim();
    }

    public long getIntervalSeconds() {
        return intervalSeconds;
    }

    public void setIntervalSeconds(long intervalSeconds) {
        this.intervalSeconds = Math.max(1L, intervalSeconds);
    }

    public DeliveryMode getDeliveryMode() {
        return deliveryMode;
    }

    public void setDeliveryMode(DeliveryMode deliveryMode) {
        this.deliveryMode = deliveryMode == null ? DeliveryMode.CHAT : deliveryMode;
    }

    public List<String> getWorlds() {
        return Collections.unmodifiableList(worlds);
    }

    public void setWorlds(List<String> worlds) {
        this.worlds = normalizeWorlds(worlds);
    }

    public boolean appliesToWorld(String worldName) {
        if (worlds.isEmpty()) {
            return true;
        }

        for (String world : worlds) {
            if (world.equalsIgnoreCase(worldName)) {
                return true;
            }
        }
        return false;
    }

    public Map<String, Object> toConfigMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("message", message);
        map.put("intervalSeconds", intervalSeconds);

        if (deliveryMode != DeliveryMode.CHAT) {
            map.put("delivery", deliveryMode.configValue());
        }

        if (!worlds.isEmpty()) {
            map.put("worlds", new ArrayList<>(worlds));
        }

        return map;
    }

    public static MessageEntry fromConfigMap(Map<?, ?> map, long defaultIntervalSeconds) {
        Object messageObject = map.get("message");
        if (!(messageObject instanceof String message) || message.trim().isEmpty()) {
            return null;
        }

        long intervalSeconds = defaultIntervalSeconds;
        Object intervalObject = map.get("intervalSeconds");
        if (intervalObject instanceof Number number) {
            intervalSeconds = Math.max(1L, number.longValue());
        } else if (intervalObject instanceof String intervalText) {
            try {
                intervalSeconds = Math.max(1L, Long.parseLong(intervalText));
            } catch (NumberFormatException ignored) {
                // keep default
            }
        }

        DeliveryMode deliveryMode = DeliveryMode.CHAT;
        Object deliveryObject = map.get("delivery");
        if (deliveryObject instanceof String deliveryText) {
            deliveryMode = DeliveryMode.fromString(deliveryText);
        }

        List<String> worlds = Collections.emptyList();
        Object worldsObject = map.get("worlds");
        if (worldsObject instanceof List<?> worldList) {
            worlds = new ArrayList<>();
            for (Object world : worldList) {
                if (world instanceof String worldName && !worldName.isBlank()) {
                    worlds.add(worldName.trim());
                }
            }
        }

        return new MessageEntry(message, intervalSeconds, deliveryMode, worlds);
    }

    public MessageEntry copy() {
        return new MessageEntry(message, intervalSeconds, deliveryMode, worlds);
    }

    private static List<String> normalizeWorlds(List<String> source) {
        if (source == null || source.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> normalized = new ArrayList<>();
        for (String world : source) {
            if (world != null && !world.isBlank()) {
                normalized.add(world.trim());
            }
        }
        return normalized;
    }

    public static String formatWorlds(List<String> worlds) {
        if (worlds == null || worlds.isEmpty()) {
            return "all";
        }
        return String.join(", ", worlds);
    }

    public static String formatDelivery(DeliveryMode mode) {
        return mode == null ? DeliveryMode.CHAT.configValue() : mode.configValue().replace('_', '-');
    }
}
