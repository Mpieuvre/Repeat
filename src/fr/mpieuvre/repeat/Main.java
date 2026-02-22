package fr.mpieuvre.repeat;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;

public class Main extends JavaPlugin {
    private static final String DEFAULT_MESSAGE = "&6[Repeat] &fEdit this message with /setmessage <message>";
    private static final long DEFAULT_INTERVAL_SECONDS = 300L;

    private String autoMessage;
    private boolean messageEnabled;
    private long intervalSeconds;
    private BukkitTask broadcastTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadPluginData();
        registerCommands();
        startAutoMessageTask();
        getLogger().info("Repeat is enabled.");
    }

    @Override
    public void onDisable() {
        stopAutoMessageTask();
        savePluginData();
        getLogger().info("Repeat is disabled.");
    }

    private void registerCommands() {
        RepeatCommand commandExecutor = new RepeatCommand(this);
        bindCommand("setmessage", commandExecutor);
        bindCommand("togglemessage", commandExecutor);
        bindCommand("setinterval", commandExecutor);
        bindCommand("repeathelp", commandExecutor);
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
        long intervalTicks = Math.max(1L, intervalSeconds) * 20L;
        broadcastTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (messageEnabled) {
                Bukkit.broadcastMessage(colorize(autoMessage));
            }
        }, intervalTicks, intervalTicks);
    }

    private void stopAutoMessageTask() {
        if (broadcastTask != null) {
            broadcastTask.cancel();
            broadcastTask = null;
        }
    }

    public void savePluginData() {
        getConfig().set("autoMessage", autoMessage);
        getConfig().set("enabled", messageEnabled);
        getConfig().set("intervalSeconds", intervalSeconds);
        saveConfig();
    }

    private void loadPluginData() {
        getConfig().options().copyDefaults(true);
        saveConfig();
        autoMessage = Objects.requireNonNull(getConfig().getString("autoMessage", DEFAULT_MESSAGE));
        messageEnabled = getConfig().getBoolean("enabled", true);
        intervalSeconds = Math.max(1L, getConfig().getLong("intervalSeconds", DEFAULT_INTERVAL_SECONDS));
    }

    public String getAutoMessage() {
        return autoMessage;
    }

    public void setAutoMessage(String autoMessage) {
        this.autoMessage = autoMessage;
    }

    public boolean isMessageEnabled() {
        return messageEnabled;
    }

    public void setMessageEnabled(boolean messageEnabled) {
        this.messageEnabled = messageEnabled;
    }

    public long getIntervalSeconds() {
        return intervalSeconds;
    }

    public void setIntervalSeconds(long intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }

    public static String colorize(String text) {
        return text.replace('&', '\u00A7');
    }
}
