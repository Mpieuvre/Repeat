package fr.mpieuvre.repeat;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class Main extends JavaPlugin {
    private static final String DEFAULT_MESSAGE = "&6[Repeat] &fUse &e/repeathelp &ffor help and to configure the plugin.";
    private static final long DEFAULT_INTERVAL_SECONDS = 300L;
    private static final String CONFIG_ENTRIES = "entries";

    private final List<MessageEntry> entries = new ArrayList<>();
    private final BossBarManager bossBarManager = new BossBarManager(this);
    private final MessageDelivery messageDelivery = new MessageDelivery();

    private boolean messageEnabled = true;
    private long defaultIntervalSeconds = DEFAULT_INTERVAL_SECONDS;
    private int nextMessageIndex;
    private int minPlayers;
    private boolean requireReceivePermission;
    private BukkitTask broadcastTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configureYamlOptions();
        loadPluginData();
        Bukkit.getPluginManager().registerEvents(bossBarManager, this);
        registerCommands();
        startAutoMessageTask();
        bossBarManager.start();
        getLogger().info("Repeat is enabled.");
    }

    @Override
    public void onDisable() {
        stopAutoMessageTask();
        bossBarManager.stop();
        getLogger().info("Repeat is disabled.");
    }

    private void registerCommands() {
        RepeatCommand commandExecutor = new RepeatCommand(this);
        for (String commandName : List.of(
                "repeat",
                "setmessage",
                "addmessages",
                "removemessage",
                "clearmessages",
                "movemessage",
                "testmessage",
                "testemessages",
                "listemessage",
                "repeatreload",
                "togglemessage",
                "setinterval",
                "setdelivery",
                "setworlds",
                "setminplayers",
                "setbossbar",
                "repeatstatus",
                "repeathelp"
        )) {
            bindCommand(commandName, commandExecutor);
        }
    }

    private void bindCommand(String commandName, RepeatCommand executor) {
        PluginCommand command = getCommand(commandName);
        if (command == null) {
            getLogger().severe("Missing command in plugin.yml: /" + commandName);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    public void startAutoMessageTask() {
        stopAutoMessageTask();
        scheduleNextBroadcast(getDelayTicksForCurrentIndex());
    }

    private void scheduleNextBroadcast(long delayTicks) {
        if (!isEnabled()) {
            return;
        }

        broadcastTask = Bukkit.getScheduler().runTaskLater(this, () -> {
            if (messageEnabled && !entries.isEmpty()) {
                broadcastCurrentEntry();
                nextMessageIndex = (nextMessageIndex + 1) % entries.size();
            }
            scheduleNextBroadcast(getDelayTicksForCurrentIndex());
        }, Math.max(1L, delayTicks));
    }

    public void broadcastCurrentEntry() {
        if (entries.isEmpty() || Bukkit.getOnlinePlayers().size() < minPlayers) {
            return;
        }
        MessageEntry entry = entries.get(nextMessageIndex);
        if (entry.getDeliveryMode() == DeliveryMode.BOSS_BAR) {
            return;
        }
        deliverToEligiblePlayers(entry);
    }

    public void deliverToEligiblePlayers(MessageEntry entry) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (requireReceivePermission && !player.hasPermission("repeat.receive")) {
                continue;
            }
            if (!entry.appliesToWorld(player.getWorld().getName())) {
                continue;
            }
            deliverTo(player, entry);
        }
    }

    public void deliverTo(Player player, MessageEntry entry) {
        String resolved = PlaceholderService.apply(entry.getMessage(), player);
        messageDelivery.send(player, entry, TextUtils.colorize(resolved));
    }

    public void preview(CommandSender sender, MessageEntry entry) {
        if (entry.getDeliveryMode() == DeliveryMode.BOSS_BAR) {
            bossBarManager.jumpTo(entry);
            if (!(sender instanceof Player)) {
                sender.sendMessage(TextUtils.colorize(PlaceholderService.apply(entry.getMessage(), null)));
            }
            return;
        }

        Player context = sender instanceof Player player ? player : null;
        String resolved = PlaceholderService.apply(entry.getMessage(), context);
        String formatted = TextUtils.colorize(resolved);
        messageDelivery.preview(sender, entry, formatted);
    }

    public List<MessageEntry> getBossBarEntries() {
        List<MessageEntry> result = new ArrayList<>();
        for (MessageEntry entry : entries) {
            if (entry.getDeliveryMode() == DeliveryMode.BOSS_BAR) {
                result.add(entry);
            }
        }
        return result;
    }

    public boolean canReceive(Player player, MessageEntry entry) {
        if (requireReceivePermission && !player.hasPermission("repeat.receive")) {
            return false;
        }
        return entry.appliesToWorld(player.getWorld().getName());
    }

    public boolean hasEnoughPlayers() {
        return Bukkit.getOnlinePlayers().size() >= minPlayers;
    }

    private long getDelayTicksForCurrentIndex() {
        if (entries.isEmpty()) {
            return Math.max(1L, defaultIntervalSeconds) * 20L;
        }
        return Math.max(1L, entries.get(nextMessageIndex).getIntervalSeconds()) * 20L;
    }

    private void stopAutoMessageTask() {
        if (broadcastTask != null) {
            broadcastTask.cancel();
            broadcastTask = null;
        }
    }

    public void savePluginData() {
        configureYamlOptions();

        List<Map<String, Object>> serializedEntries = new ArrayList<>();
        for (MessageEntry entry : entries) {
            serializedEntries.add(entry.toConfigMap());
        }

        getConfig().set(CONFIG_ENTRIES, serializedEntries);
        getConfig().set("messages", null);
        getConfig().set("messageIntervals", null);
        getConfig().set("autoMessage", null);
        getConfig().set("enabled", messageEnabled);
        getConfig().set("intervalSeconds", defaultIntervalSeconds);
        getConfig().set("nextMessageIndex", nextMessageIndex);
        getConfig().set("minPlayers", minPlayers);
        getConfig().set("requireReceivePermission", requireReceivePermission);
        saveConfig();
    }

    private void loadPluginData() {
        messageEnabled = getConfig().getBoolean("enabled", true);
        defaultIntervalSeconds = Math.max(1L, getConfig().getLong("intervalSeconds", DEFAULT_INTERVAL_SECONDS));
        minPlayers = Math.max(0, getConfig().getInt("minPlayers", 0));
        requireReceivePermission = getConfig().getBoolean("requireReceivePermission", false);

        entries.clear();
        if (!loadFromEntries()) {
            loadFromLegacyKeys();
        }
        normalizeNextMessageIndex();
    }

    private boolean loadFromEntries() {
        List<?> raw = getConfig().getList(CONFIG_ENTRIES);
        if (raw == null || raw.isEmpty()) {
            return false;
        }

        List<MessageEntry> loaded = new ArrayList<>();
        for (Object item : raw) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            MessageEntry entry = MessageEntry.fromConfigMap(map, defaultIntervalSeconds);
            if (entry != null) {
                loaded.add(entry);
            }
        }

        if (loaded.isEmpty()) {
            return false;
        }

        entries.addAll(loaded);
        return true;
    }

    private void loadFromLegacyKeys() {
        List<String> configuredMessages = getConfig().getStringList("messages");
        if (configuredMessages.isEmpty()) {
            String legacyMessage = Objects.requireNonNull(getConfig().getString("autoMessage", DEFAULT_MESSAGE));
            configuredMessages = new ArrayList<>();
            configuredMessages.add(legacyMessage);
        }

        List<Long> configuredIntervals = loadConfiguredIntervals();
        for (int i = 0; i < configuredMessages.size(); i++) {
            long interval = defaultIntervalSeconds;
            if (i < configuredIntervals.size()) {
                interval = configuredIntervals.get(i);
            }
            entries.add(new MessageEntry(configuredMessages.get(i), interval));
        }
    }

    private List<Long> loadConfiguredIntervals() {
        List<Long> result = new ArrayList<>();
        List<?> configured = getConfig().getList("messageIntervals");
        if (configured == null) {
            return result;
        }

        for (Object value : configured) {
            if (value instanceof Number number) {
                result.add(Math.max(1L, number.longValue()));
            } else if (value instanceof String text) {
                try {
                    result.add(Math.max(1L, Long.parseLong(text)));
                } catch (NumberFormatException ignored) {
                    // Ignore invalid values from config.
                }
            }
        }
        return result;
    }

    private void configureYamlOptions() {
        Object options = getConfig().options();
        try {
            Method indent = options.getClass().getMethod("indent", int.class);
            indent.invoke(options, 2);
        } catch (Throwable ignored) {
            // Not available on this server / API version.
        }
        try {
            Method parseComments = options.getClass().getMethod("parseComments", boolean.class);
            parseComments.invoke(options, true);
        } catch (Throwable ignored) {
            // Not available on this server / API version.
        }
    }

    private void normalizeNextMessageIndex() {
        nextMessageIndex = getConfig().getInt("nextMessageIndex", nextMessageIndex);
        if (entries.isEmpty() || nextMessageIndex < 0 || nextMessageIndex >= entries.size()) {
            nextMessageIndex = 0;
        }
    }

    public void reloadPluginData() {
        reloadConfig();
        loadPluginData();
        startAutoMessageTask();
    }

    public MessageEntry getNextEntryForPreview() {
        if (entries.isEmpty()) {
            return new MessageEntry(DEFAULT_MESSAGE, defaultIntervalSeconds);
        }
        return entries.get(nextMessageIndex);
    }

    public int getNextMessageIndex() {
        return nextMessageIndex;
    }

    public int getMessageCount() {
        return entries.size();
    }

    public List<MessageEntry> getEntries() {
        List<MessageEntry> copy = new ArrayList<>();
        for (MessageEntry entry : entries) {
            copy.add(entry.copy());
        }
        return copy;
    }

    public MessageEntry getEntryAt(int index) {
        return entries.get(index).copy();
    }

    public MessageEntry getLiveEntryAt(int index) {
        return entries.get(index);
    }

    public void setPrimaryMessage(String message) {
        String normalized = message.trim();
        if (entries.isEmpty()) {
            entries.add(new MessageEntry(normalized, defaultIntervalSeconds));
        } else {
            entries.get(0).setMessage(normalized);
        }
        normalizeNextMessageIndex();
    }

    public void setMessageAt(int index, String message) {
        entries.get(index).setMessage(message);
    }

    public void addMessage(String message) {
        addMessage(message, DeliveryMode.CHAT);
    }

    public void addMessage(String message, DeliveryMode deliveryMode) {
        addMessage(message, deliveryMode, null);
    }

    public void addMessage(String message, DeliveryMode deliveryMode, BossBarSettings bossBarSettings) {
        MessageEntry entry = new MessageEntry(
                message.trim(),
                defaultIntervalSeconds,
                deliveryMode,
                List.of(),
                bossBarSettings == null ? new BossBarSettings() : bossBarSettings
        );
        entries.add(entry);
    }

    public void applyBossBarPreset(int index, BossBarPreset preset) {
        MessageEntry entry = entries.get(index);
        entry.setDeliveryMode(DeliveryMode.BOSS_BAR);
        preset.apply(entry.getBossBarSettings());
    }

    public void showBossBarDemo(Player player, BossBarPreset preset) {
        bossBarManager.showDemo(player, preset);
    }

    public boolean removeMessageAt(int index) {
        if (index < 0 || index >= entries.size()) {
            return false;
        }
        entries.remove(index);
        if (nextMessageIndex >= entries.size()) {
            nextMessageIndex = 0;
        }
        return true;
    }

    public void clearMessages() {
        entries.clear();
        nextMessageIndex = 0;
    }

    public boolean moveMessage(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex >= entries.size() || toIndex < 0 || toIndex >= entries.size()) {
            return false;
        }
        if (fromIndex == toIndex) {
            return true;
        }

        MessageEntry moved = entries.remove(fromIndex);
        entries.add(toIndex, moved);

        if (nextMessageIndex == fromIndex) {
            nextMessageIndex = toIndex;
        } else if (fromIndex < nextMessageIndex && toIndex >= nextMessageIndex) {
            nextMessageIndex--;
        } else if (fromIndex > nextMessageIndex && toIndex <= nextMessageIndex) {
            nextMessageIndex++;
        }
        return true;
    }

    public boolean isMessageEnabled() {
        return messageEnabled;
    }

    public void setMessageEnabled(boolean messageEnabled) {
        this.messageEnabled = messageEnabled;
    }

    public long getDefaultIntervalSeconds() {
        return defaultIntervalSeconds;
    }

    public void setDefaultIntervalSeconds(long intervalSeconds) {
        this.defaultIntervalSeconds = Math.max(1L, intervalSeconds);
    }

    public void setIntervalSecondsAt(int index, long intervalSeconds) {
        entries.get(index).setIntervalSeconds(intervalSeconds);
    }

    public void setIntervalSecondsForAll(long intervalSeconds) {
        long normalized = Math.max(1L, intervalSeconds);
        this.defaultIntervalSeconds = normalized;
        for (MessageEntry entry : entries) {
            entry.setIntervalSeconds(normalized);
        }
    }

    public void setDeliveryModeForAll(DeliveryMode mode) {
        for (MessageEntry entry : entries) {
            entry.setDeliveryMode(mode);
        }
    }

    public void setDeliveryModeAt(int index, DeliveryMode mode) {
        entries.get(index).setDeliveryMode(mode);
    }

    public void setWorldsForAll(List<String> worlds) {
        for (MessageEntry entry : entries) {
            entry.setWorlds(worlds);
        }
    }

    public void setWorldsAt(int index, List<String> worlds) {
        entries.get(index).setWorlds(worlds);
    }

    public void updateBossBarForAll(Consumer<BossBarSettings> updater) {
        for (MessageEntry entry : entries) {
            updater.accept(entry.getBossBarSettings());
        }
    }

    public void updateBossBarAt(int index, Consumer<BossBarSettings> updater) {
        updater.accept(entries.get(index).getBossBarSettings());
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public void setMinPlayers(int minPlayers) {
        this.minPlayers = Math.max(0, minPlayers);
    }

    public boolean isRequireReceivePermission() {
        return requireReceivePermission;
    }

    public void setRequireReceivePermission(boolean requireReceivePermission) {
        this.requireReceivePermission = requireReceivePermission;
    }
}
