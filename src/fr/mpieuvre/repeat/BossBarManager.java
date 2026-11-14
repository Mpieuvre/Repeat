package fr.mpieuvre.repeat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.KeyedBossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BossBarManager implements Listener {
    private static final long TICK_PERIOD = 2L;
    private static final AdventureBossBars ADVENTURE = AdventureBossBars.detect();

    private final Main plugin;
    private final Map<UUID, ActiveBar> activeBars = new ConcurrentHashMap<>();
    private final Map<UUID, DemoBar> demoBars = new ConcurrentHashMap<>();
    private BukkitTask ticker;
    private int currentIndex;
    private long elapsedTicks;

    public BossBarManager(Main plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stopTicker();
        currentIndex = 0;
        elapsedTicks = 0;
        ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, TICK_PERIOD, TICK_PERIOD);
    }

    public void stop() {
        stopTicker();
        hideAll();
        clearAllDemos();
        currentIndex = 0;
        elapsedTicks = 0;
    }

    public void showDemo(Player player, BossBarPreset preset) {
        if (player == null || preset == null) {
            return;
        }
        clearDemo(player);

        NamespacedKey key = new NamespacedKey(plugin, "demo-" + player.getUniqueId());
        removeRegisteredBar(key);

        String title = TextUtils.colorize(preset.title());
        BossBarSettings settings = preset.toSettings();
        BossBar bar = createBar(key, title, settings.getColor(), settings.getStyle());
        bar.setProgress(1.0D);
        bar.addPlayer(player);
        bar.setVisible(true);

        DemoBar demoBar = new DemoBar(bar, key);
        long totalTicks = Math.max(40L, settings.getDurationSeconds() * 20L);
        demoBar.task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                clearDemo(player);
                return;
            }
            demoBar.elapsed += TICK_PERIOD;
            if (demoBar.elapsed >= totalTicks) {
                clearDemo(player);
                return;
            }
            double progress = 1.0D - (demoBar.elapsed / (double) totalTicks);
            bar.setProgress(clamp(progress));
        }, TICK_PERIOD, TICK_PERIOD);
        demoBars.put(player.getUniqueId(), demoBar);
    }

    public void jumpTo(MessageEntry target) {
        List<MessageEntry> bars = plugin.getBossBarEntries();
        if (bars.isEmpty() || target == null) {
            return;
        }
        for (int i = 0; i < bars.size(); i++) {
            MessageEntry entry = bars.get(i);
            if (entry == target || entry.getMessage().equals(target.getMessage())) {
                currentIndex = i;
                elapsedTicks = 0;
                syncAll(entry, 1.0D);
                return;
            }
        }
    }

    private void tick() {
        if (!plugin.isEnabled() || !plugin.isMessageEnabled()) {
            hideAll();
            return;
        }

        List<MessageEntry> bars = plugin.getBossBarEntries();
        if (bars.isEmpty() || !plugin.hasEnoughPlayers()) {
            hideAll();
            currentIndex = 0;
            elapsedTicks = 0;
            return;
        }

        if (currentIndex >= bars.size()) {
            currentIndex = 0;
            elapsedTicks = 0;
        }

        MessageEntry current = bars.get(currentIndex);
        long totalTicks = cycleTicks(current);
        elapsedTicks += TICK_PERIOD;

        if (elapsedTicks >= totalTicks) {
            currentIndex = (currentIndex + 1) % bars.size();
            elapsedTicks = 0;
            current = bars.get(currentIndex);
            totalTicks = cycleTicks(current);
        }

        double progress = 1.0D - (elapsedTicks / (double) Math.max(1L, totalTicks));
        syncAll(current, clamp(progress));
    }

    private void syncAll(MessageEntry entry, double progress) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.canReceive(player, entry)) {
                showOrUpdate(player, entry, progress);
            } else {
                hide(player);
            }
        }
    }

    private void showOrUpdate(Player player, MessageEntry entry, double progress) {
        String title = formatTitle(entry, player);
        BossBarSettings settings = entry.getBossBarSettings();
        ActiveBar activeBar = activeBars.get(player.getUniqueId());

        if (activeBar == null) {
            activeBar = new ActiveBar();
            if (!ADVENTURE.show(player, title, settings, progress, activeBar)) {
                showBukkit(player, title, settings, progress, activeBar);
            }
            activeBars.put(player.getUniqueId(), activeBar);
            return;
        }

        if (!ADVENTURE.update(player, title, settings, progress, activeBar)) {
            updateBukkit(activeBar, title, settings, progress);
        }
    }

    private void showBukkit(Player player, String title, BossBarSettings settings, double progress, ActiveBar activeBar) {
        NamespacedKey key = barKey(player.getUniqueId());
        removeRegisteredBar(key);

        BossBar bar = createBar(key, title, settings.getColor(), settings.getStyle());
        bar.setProgress(progress);
        bar.addPlayer(player);
        bar.setVisible(true);

        activeBar.bukkitBar = bar;
        activeBar.barKey = key;
    }

    private void updateBukkit(ActiveBar activeBar, String title, BossBarSettings settings, double progress) {
        if (activeBar.bukkitBar == null) {
            return;
        }
        activeBar.bukkitBar.setTitle(title);
        activeBar.bukkitBar.setColor(settings.getColor());
        activeBar.bukkitBar.setStyle(settings.getStyle());
        activeBar.bukkitBar.setProgress(progress);
        activeBar.bukkitBar.setVisible(true);
    }

    private BossBar createBar(NamespacedKey key, String title, BarColor color, BarStyle style) {
        try {
            return Bukkit.createBossBar(key, title, color, style);
        } catch (Throwable ignored) {
            return Bukkit.createBossBar(key, ChatColor.stripColor(title), color, style);
        }
    }

    public void hide(Player player) {
        if (player == null) {
            return;
        }
        clear(player.getUniqueId(), player);
    }

    public void hideAll() {
        for (UUID playerId : new ArrayList<>(activeBars.keySet())) {
            clear(playerId, Bukkit.getPlayer(playerId));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        hide(event.getPlayer());
        clearDemo(event.getPlayer());
    }

    private void clearDemo(Player player) {
        if (player == null) {
            return;
        }
        DemoBar demoBar = demoBars.remove(player.getUniqueId());
        if (demoBar == null) {
            return;
        }
        if (demoBar.task != null) {
            demoBar.task.cancel();
        }
        if (demoBar.bar != null) {
            demoBar.bar.removeAll();
            demoBar.bar.setVisible(false);
        }
        if (demoBar.key != null) {
            removeRegisteredBar(demoBar.key);
        }
    }

    private void clearAllDemos() {
        for (UUID playerId : new ArrayList<>(demoBars.keySet())) {
            clearDemo(Bukkit.getPlayer(playerId));
            DemoBar leftover = demoBars.remove(playerId);
            if (leftover != null) {
                if (leftover.task != null) {
                    leftover.task.cancel();
                }
                if (leftover.bar != null) {
                    leftover.bar.removeAll();
                }
                if (leftover.key != null) {
                    removeRegisteredBar(leftover.key);
                }
            }
        }
    }

    private void clear(UUID playerId, Player player) {
        ActiveBar activeBar = activeBars.remove(playerId);
        if (activeBar == null) {
            return;
        }
        if (player != null) {
            ADVENTURE.hide(player, activeBar);
        }
        if (activeBar.bukkitBar != null) {
            activeBar.bukkitBar.removeAll();
            activeBar.bukkitBar.setVisible(false);
        }
        if (activeBar.barKey != null) {
            removeRegisteredBar(activeBar.barKey);
        }
    }

    private void stopTicker() {
        if (ticker != null) {
            ticker.cancel();
            ticker = null;
        }
    }

    private static long cycleTicks(MessageEntry entry) {
        int durationSeconds = entry.getBossBarSettings().getDurationSeconds();
        long seconds = durationSeconds > 0 ? durationSeconds : entry.getIntervalSeconds();
        return Math.max(20L, seconds * 20L);
    }

    private static String formatTitle(MessageEntry entry, Player player) {
        String resolved = PlaceholderService.apply(entry.getMessage(), player);
        String formatted = TextUtils.colorize(resolved);
        return formatted == null || formatted.isBlank() ? " " : formatted;
    }

    private NamespacedKey barKey(UUID playerId) {
        return new NamespacedKey(plugin, "repeat-" + playerId);
    }

    private static void removeRegisteredBar(NamespacedKey key) {
        KeyedBossBar existing = Bukkit.getBossBar(key);
        if (existing == null) {
            return;
        }
        existing.removeAll();
        existing.setVisible(false);
        Bukkit.removeBossBar(key);
    }

    private static double clamp(double value) {
        if (value < 0.0D) {
            return 0.0D;
        }
        if (value > 1.0D) {
            return 1.0D;
        }
        return value;
    }

    private static final class ActiveBar {
        private BossBar bukkitBar;
        private NamespacedKey barKey;
        private Object adventureBar;
    }

    private static final class DemoBar {
        private final BossBar bar;
        private final NamespacedKey key;
        private BukkitTask task;
        private long elapsed;

        private DemoBar(BossBar bar, NamespacedKey key) {
            this.bar = bar;
            this.key = key;
        }
    }

    private static final class AdventureBossBars {
        private final boolean available;
        private final Method showMethod;
        private final Method hideMethod;
        private final Method deserialize;
        private final Method factory;
        private final Method progressMethod;
        private final Method nameMethod;
        private final Method colorMethod;
        private final Method overlayMethod;
        private final Object serializer;
        private final Class<?> colorClass;
        private final Class<?> overlayClass;

        private AdventureBossBars(
                boolean available,
                Method showMethod,
                Method hideMethod,
                Method deserialize,
                Method factory,
                Method progressMethod,
                Method nameMethod,
                Method colorMethod,
                Method overlayMethod,
                Object serializer,
                Class<?> colorClass,
                Class<?> overlayClass
        ) {
            this.available = available;
            this.showMethod = showMethod;
            this.hideMethod = hideMethod;
            this.deserialize = deserialize;
            this.factory = factory;
            this.progressMethod = progressMethod;
            this.nameMethod = nameMethod;
            this.colorMethod = colorMethod;
            this.overlayMethod = overlayMethod;
            this.serializer = serializer;
            this.colorClass = colorClass;
            this.overlayClass = overlayClass;
        }

        static AdventureBossBars detect() {
            try {
                Class<?> audienceClass = Class.forName("net.kyori.adventure.audience.Audience");
                Class<?> bossBarClass = Class.forName("net.kyori.adventure.bossbar.BossBar");
                Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
                Class<?> legacyClass = Class.forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer");
                Class<?> colorClass = Class.forName("net.kyori.adventure.bossbar.BossBar$Color");
                Class<?> overlayClass = Class.forName("net.kyori.adventure.bossbar.BossBar$Overlay");

                Method showMethod = audienceClass.getMethod("showBossBar", bossBarClass);
                Method hideMethod = audienceClass.getMethod("hideBossBar", bossBarClass);
                Object serializer = legacyClass.getMethod("legacySection").invoke(null);
                Method deserialize = serializer.getClass().getMethod("deserialize", String.class);
                Method factory = bossBarClass.getMethod("bossBar", componentClass, float.class, colorClass, overlayClass);
                Method progressMethod = bossBarClass.getMethod("progress", float.class);
                Method nameMethod = bossBarClass.getMethod("name", componentClass);
                Method colorMethod = bossBarClass.getMethod("color", colorClass);
                Method overlayMethod = bossBarClass.getMethod("overlay", overlayClass);

                return new AdventureBossBars(
                        true,
                        showMethod,
                        hideMethod,
                        deserialize,
                        factory,
                        progressMethod,
                        nameMethod,
                        colorMethod,
                        overlayMethod,
                        serializer,
                        colorClass,
                        overlayClass
                );
            } catch (Throwable ignored) {
                return new AdventureBossBars(false, null, null, null, null, null, null, null, null, null, null, null);
            }
        }

        boolean show(Player player, String title, BossBarSettings settings, double progress, ActiveBar activeBar) {
            if (!available) {
                return false;
            }
            try {
                Object bar = factory.invoke(
                        null,
                        deserialize.invoke(serializer, title),
                        (float) progress,
                        enumValue(colorClass, settings.getColor().name()),
                        enumValue(overlayClass, overlayName(settings.getStyle()))
                );
                showMethod.invoke(player, bar);
                activeBar.adventureBar = bar;
                return true;
            } catch (Throwable ignored) {
                return false;
            }
        }

        boolean update(Player player, String title, BossBarSettings settings, double progress, ActiveBar activeBar) {
            if (!available || activeBar.adventureBar == null) {
                return false;
            }
            try {
                nameMethod.invoke(activeBar.adventureBar, deserialize.invoke(serializer, title));
                colorMethod.invoke(activeBar.adventureBar, enumValue(colorClass, settings.getColor().name()));
                overlayMethod.invoke(activeBar.adventureBar, enumValue(overlayClass, overlayName(settings.getStyle())));
                progressMethod.invoke(activeBar.adventureBar, (float) progress);
                return true;
            } catch (Throwable ignored) {
                hide(player, activeBar);
                activeBar.adventureBar = null;
                return false;
            }
        }

        void hide(Player player, ActiveBar activeBar) {
            if (!available || activeBar.adventureBar == null) {
                return;
            }
            try {
                hideMethod.invoke(player, activeBar.adventureBar);
            } catch (Throwable ignored) {
                // Best-effort cleanup.
            }
        }

        private Object enumValue(Class<?> enumClass, String name) {
            return Enum.valueOf(asEnumClass(enumClass), name);
        }

        private static String overlayName(BarStyle style) {
            if (style == null) {
                return "PROGRESS";
            }
            return switch (style) {
                case SEGMENTED_6 -> "NOTCHED_6";
                case SEGMENTED_10 -> "NOTCHED_10";
                case SEGMENTED_12 -> "NOTCHED_12";
                case SEGMENTED_20 -> "NOTCHED_20";
                default -> "PROGRESS";
            };
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static Class<? extends Enum> asEnumClass(Class<?> enumClass) {
            return (Class<? extends Enum>) enumClass;
        }
    }
}
