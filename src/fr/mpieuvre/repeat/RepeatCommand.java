package fr.mpieuvre.repeat;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class RepeatCommand implements CommandExecutor, TabCompleter {
    private static final String PREFIX = TextUtils.colorize("&6[Repeat] &r");
    private static final String SEP = TextUtils.colorize("&8--------------------------------");
    private static final List<String> SUBCOMMANDS = List.of(
            "help", "status", "reload", "toggle", "list",
            "add", "set", "remove", "clear", "move", "test",
            "interval", "delivery", "worlds", "minplayers", "bossbar"
    );
    private static final List<String> DELIVERY_MODES = List.of("chat", "actionbar", "bossbar");
    private static final List<String> BOSSBAR_PROPERTIES = List.of("message", "color", "style", "duration", "progress");
    private static final List<String> BOSSBAR_COLORS = List.of("pink", "blue", "red", "green", "yellow", "purple", "white");
    private static final List<String> BOSSBAR_STYLES = List.of("solid", "segmented_6", "segmented_10", "segmented_12", "segmented_20");

    private final Main plugin;

    public RepeatCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("repeat.admin")) {
            send(sender, "&cYou don't have permission to use this command.");
            return true;
        }

        String commandName = command.getName().toLowerCase(Locale.ROOT);
        if ("repeat".equals(commandName)) {
            if (args.length == 0) {
                return handleHelp(sender);
            }
            return dispatch(sender, args[0].toLowerCase(Locale.ROOT), Arrays.copyOfRange(args, 1, args.length));
        }
        return dispatch(sender, mapLegacyCommand(commandName), args);
    }

    private boolean dispatch(CommandSender sender, String subcommand, String[] args) {
        return switch (subcommand) {
            case "set", "setmessage" -> handleSetMessage(sender, args);
            case "add", "addmessages", "addmessage" -> handleAddMessage(sender, args);
            case "remove", "removemessage" -> handleRemoveMessage(sender, args);
            case "clear", "clearmessages" -> handleClearMessages(sender, args);
            case "move", "movemessage" -> handleMoveMessage(sender, args);
            case "test", "testmessage", "testemessages" -> handleTest(sender, args);
            case "list", "listemessage" -> handleListMessage(sender);
            case "reload", "repeatreload" -> handleReload(sender);
            case "toggle", "togglemessage" -> handleToggleMessage(sender);
            case "interval", "setinterval" -> handleSetInterval(sender, args);
            case "delivery", "setdelivery" -> handleSetDelivery(sender, args);
            case "worlds", "setworlds" -> handleSetWorlds(sender, args);
            case "minplayers", "setminplayers" -> handleSetMinPlayers(sender, args);
            case "bossbar", "setbossbar" -> handleSetBossBar(sender, args);
            case "status", "repeatstatus" -> handleStatus(sender);
            case "help", "repeathelp" -> handleHelp(sender);
            default -> {
                send(sender, "&cUnknown subcommand. Use &f/repeat help&c.");
                yield true;
            }
        };
    }

    private String mapLegacyCommand(String commandName) {
        return switch (commandName) {
            case "setmessage" -> "set";
            case "addmessages", "addmessage" -> "add";
            case "removemessage" -> "remove";
            case "clearmessages" -> "clear";
            case "movemessage" -> "move";
            case "testmessage", "testemessages" -> "test";
            case "listemessage" -> "list";
            case "repeatreload" -> "reload";
            case "togglemessage" -> "toggle";
            case "setinterval" -> "interval";
            case "setdelivery" -> "delivery";
            case "setworlds" -> "worlds";
            case "setminplayers" -> "minplayers";
            case "setbossbar" -> "bossbar";
            case "repeatstatus" -> "status";
            case "repeathelp" -> "help";
            default -> commandName;
        };
    }

    private boolean handleSetMessage(CommandSender sender, String[] args) {
        if (args.length == 0) {
            send(sender, "&cUsage: &f/repeat set <message>");
            send(sender, "&7Or: &f/repeat set <index> <message>");
            return true;
        }

        if (args.length >= 2 && isInteger(args[0])) {
            int oneBasedIndex = Integer.parseInt(args[0]);
            if (!isValidIndex(sender, oneBasedIndex)) {
                return true;
            }
            String message = join(args, 1);
            plugin.setMessageAt(oneBasedIndex - 1, message);
            plugin.savePluginData();
            send(sender, "&aMessage &f#" + oneBasedIndex + " &aupdated.");
            return true;
        }

        plugin.setPrimaryMessage(join(args, 0));
        plugin.savePluginData();
        send(sender, "&aPrimary message updated (&f#1&a).");
        return true;
    }

    private boolean handleAddMessage(CommandSender sender, String[] args) {
        if (args.length == 0) {
            send(sender, "&cUsage: &f/repeat add <message>");
            send(sender, "&7Or: &f/repeat add bossbar <message>");
            return true;
        }

        if (args.length >= 2 && DeliveryMode.isKnown(args[0])) {
            DeliveryMode mode = DeliveryMode.fromString(args[0]);
            plugin.addMessage(join(args, 1), mode);
            plugin.savePluginData();
            send(sender, "&aMessage added as &f" + MessageEntry.formatDelivery(mode)
                    + "&a. Total: &f" + plugin.getMessageCount() + "&a.");
            return true;
        }

        plugin.addMessage(join(args, 0));
        plugin.savePluginData();
        send(sender, "&aMessage added. Total: &f" + plugin.getMessageCount() + "&a.");
        return true;
    }

    private boolean handleRemoveMessage(CommandSender sender, String[] args) {
        if (args.length != 1) {
            send(sender, "&cUsage: &f/repeat remove <index>");
            return true;
        }

        Integer oneBasedIndex = parseIndexArg(sender, args[0]);
        if (oneBasedIndex == null || !isValidIndex(sender, oneBasedIndex)) {
            return true;
        }

        plugin.removeMessageAt(oneBasedIndex - 1);
        plugin.savePluginData();
        plugin.startAutoMessageTask();
        send(sender, "&aMessage &f#" + oneBasedIndex + " &aremoved.");
        return true;
    }

    private boolean handleClearMessages(CommandSender sender, String[] args) {
        if (plugin.getMessageCount() == 0) {
            send(sender, "&cNo messages are configured.");
            return true;
        }
        if (args.length != 1 || !"confirm".equalsIgnoreCase(args[0])) {
            send(sender, "&cUsage: &f/repeat clear confirm");
            send(sender, "&7This will delete all &f" + plugin.getMessageCount() + " &7configured messages.");
            return true;
        }
        plugin.clearMessages();
        plugin.savePluginData();
        plugin.startAutoMessageTask();
        send(sender, "&aAll messages have been cleared.");
        return true;
    }

    private boolean handleMoveMessage(CommandSender sender, String[] args) {
        if (args.length != 2) {
            send(sender, "&cUsage: &f/repeat move <from> <to>");
            return true;
        }

        Integer fromIndex = parseIndexArg(sender, args[0]);
        Integer toIndex = parseIndexArg(sender, args[1]);
        if (fromIndex == null || toIndex == null) {
            return true;
        }
        if (!isValidIndex(sender, fromIndex) || !isValidIndex(sender, toIndex)) {
            return true;
        }

        plugin.moveMessage(fromIndex - 1, toIndex - 1);
        plugin.savePluginData();
        send(sender, "&aMoved message &f#" + fromIndex + " &ato position &f#" + toIndex + "&a.");
        return true;
    }

    private boolean handleTest(CommandSender sender, String[] args) {
        if (args.length == 0) {
            MessageEntry entry = plugin.getNextEntryForPreview();
            send(sender, "&aNext message preview (&f#" + (plugin.getNextMessageIndex() + 1) + "&a):");
            sendPreview(sender, entry);
            return true;
        }

        if (args.length != 1) {
            send(sender, "&cUsage: &f/repeat test [index]");
            return true;
        }

        Integer oneBasedIndex = parseIndexArg(sender, args[0]);
        if (oneBasedIndex == null || !isValidIndex(sender, oneBasedIndex)) {
            return true;
        }

        MessageEntry entry = plugin.getLiveEntryAt(oneBasedIndex - 1);
        send(sender, "&aMessage &f#" + oneBasedIndex + " &apreview:");
        sendPreview(sender, entry);
        return true;
    }

    private void sendPreview(CommandSender sender, MessageEntry entry) {
        send(sender, "&7delivery &f" + MessageEntry.formatDelivery(entry.getDeliveryMode())
                + " &8| &7interval &f" + entry.getIntervalSeconds() + "s"
                + " &8| &7worlds &f" + MessageEntry.formatWorlds(entry.getWorlds()));
        if (entry.getDeliveryMode() == DeliveryMode.BOSS_BAR) {
            send(sender, "&7bossbar &f" + entry.getBossBarSettings().summary());
        }
        plugin.preview(sender, entry);
        if (sender instanceof Player && entry.getDeliveryMode() != DeliveryMode.CHAT) {
            send(sender, "&7(" + entry.getDeliveryMode().displayName() + " preview sent)");
        }
    }

    private boolean handleListMessage(CommandSender sender) {
        List<MessageEntry> entries = plugin.getEntries();
        if (entries.isEmpty()) {
            send(sender, "&cNo messages are configured.");
            return true;
        }

        send(sender, "&eConfigured messages &7(" + entries.size() + ")&e:");
        sender.sendMessage(SEP);
        int nextIndex = plugin.getNextMessageIndex();
        for (int i = 0; i < entries.size(); i++) {
            MessageEntry entry = entries.get(i);
            String marker = (i == nextIndex) ? " &a(next)" : "";
            sender.sendMessage(TextUtils.colorize("&6#" + (i + 1) + marker
                    + " &8| &7interval &f" + entry.getIntervalSeconds() + "s"
                    + " &8| &7delivery &f" + MessageEntry.formatDelivery(entry.getDeliveryMode())
                    + " &8| &7worlds &f" + MessageEntry.formatWorlds(entry.getWorlds())));
            if (entry.getDeliveryMode() == DeliveryMode.BOSS_BAR) {
                sender.sendMessage(TextUtils.colorize("&7bossbar &f" + entry.getBossBarSettings().summary()));
            }
            sender.sendMessage(TextUtils.colorize("&8raw &7(&fedit&7): &f" + entry.getMessage()));
            sender.sendMessage(TextUtils.colorize("&8preview: &r") + TextUtils.colorize(entry.getMessage()));
            if (i < entries.size() - 1) {
                sender.sendMessage(SEP);
            }
        }
        return true;
    }

    private boolean handleToggleMessage(CommandSender sender) {
        boolean newState = !plugin.isMessageEnabled();
        plugin.setMessageEnabled(newState);
        plugin.savePluginData();
        send(sender, "&aAuto messages " + (newState ? "&aenabled" : "&cdisabled") + "&a.");
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        plugin.reloadPluginData();
        send(sender, "&aConfiguration reloaded.");
        return true;
    }

    private boolean handleSetInterval(CommandSender sender, String[] args) {
        if (args.length != 1 && args.length != 2) {
            send(sender, "&cUsage: &f/repeat interval <seconds>");
            send(sender, "&7Or: &f/repeat interval <index> <seconds>");
            return true;
        }

        if (args.length == 1) {
            long seconds = parsePositiveLong(sender, args[0]);
            if (seconds < 0) {
                return true;
            }
            plugin.setIntervalSecondsForAll(seconds);
            plugin.startAutoMessageTask();
            plugin.savePluginData();
            send(sender, "&aInterval updated for all messages: &f" + seconds + "s&a.");
            return true;
        }

        Integer oneBasedIndex = parseIndexArg(sender, args[0]);
        if (oneBasedIndex == null || !isValidIndex(sender, oneBasedIndex)) {
            return true;
        }

        long seconds = parsePositiveLong(sender, args[1]);
        if (seconds < 0) {
            return true;
        }

        plugin.setIntervalSecondsAt(oneBasedIndex - 1, seconds);
        plugin.startAutoMessageTask();
        plugin.savePluginData();
        send(sender, "&aInterval for message &f#" + oneBasedIndex + " &aupdated: &f" + seconds + "s&a.");
        return true;
    }

    private boolean handleSetDelivery(CommandSender sender, String[] args) {
        if (args.length != 1 && args.length != 2) {
            send(sender, "&cUsage: &f/repeat delivery <chat|actionbar|bossbar>");
            send(sender, "&7Or: &f/repeat delivery <index> <chat|actionbar|bossbar>");
            return true;
        }

        if (args.length == 1) {
            DeliveryMode mode = parseDeliveryMode(sender, args[0]);
            if (mode == null) {
                return true;
            }
            plugin.setDeliveryModeForAll(mode);
            plugin.savePluginData();
            send(sender, "&aDelivery mode updated for all messages: &f" + MessageEntry.formatDelivery(mode) + "&a.");
            return true;
        }

        Integer oneBasedIndex = parseIndexArg(sender, args[0]);
        if (oneBasedIndex == null || !isValidIndex(sender, oneBasedIndex)) {
            return true;
        }

        DeliveryMode mode = parseDeliveryMode(sender, args[1]);
        if (mode == null) {
            return true;
        }

        plugin.setDeliveryModeAt(oneBasedIndex - 1, mode);
        plugin.savePluginData();
        send(sender, "&aDelivery mode for message &f#" + oneBasedIndex + " &aupdated: &f"
                + MessageEntry.formatDelivery(mode) + "&a.");
        return true;
    }

    private boolean handleSetWorlds(CommandSender sender, String[] args) {
        if (args.length == 0) {
            send(sender, "&cUsage: &f/repeat worlds all");
            send(sender, "&7Or: &f/repeat worlds <world1,world2,...>");
            send(sender, "&7Or: &f/repeat worlds <index> <world1,world2,...|all>");
            return true;
        }

        if (args.length == 1) {
            List<String> worlds = parseWorldsArgument(args[0]);
            plugin.setWorldsForAll(worlds);
            plugin.savePluginData();
            send(sender, "&aWorld filter updated for all messages: &f" + MessageEntry.formatWorlds(worlds) + "&a.");
            return true;
        }

        Integer oneBasedIndex = parseIndexArg(sender, args[0]);
        if (oneBasedIndex == null || !isValidIndex(sender, oneBasedIndex)) {
            return true;
        }

        List<String> worlds = parseWorldsArgument(args[1]);
        plugin.setWorldsAt(oneBasedIndex - 1, worlds);
        plugin.savePluginData();
        send(sender, "&aWorld filter for message &f#" + oneBasedIndex + " &aupdated: &f"
                + MessageEntry.formatWorlds(worlds) + "&a.");
        return true;
    }

    private boolean handleSetMinPlayers(CommandSender sender, String[] args) {
        if (args.length != 1) {
            send(sender, "&cUsage: &f/repeat minplayers <count>");
            return true;
        }

        int minPlayers;
        try {
            minPlayers = Integer.parseInt(args[0]);
        } catch (NumberFormatException exception) {
            send(sender, "&cYou must enter a valid number.");
            return true;
        }
        if (minPlayers < 0) {
            send(sender, "&cThe number must be 0 or greater.");
            return true;
        }

        plugin.setMinPlayers(minPlayers);
        plugin.savePluginData();
        send(sender, "&aMinimum players required to broadcast: &f" + minPlayers + "&a.");
        return true;
    }

    private boolean handleSetBossBar(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendBossBarUsage(sender);
            return true;
        }

        if (isBossBarMessageProperty(args[0])) {
            if (args.length < 2) {
                send(sender, "&cUsage: &f/repeat bossbar message <texte>");
                return true;
            }
            return setBossBarMessage(sender, null, join(args, 1));
        }

        if (isInteger(args[0]) && args.length >= 2 && isBossBarMessageProperty(args[1])) {
            Integer oneBasedIndex = parseIndexArg(sender, args[0]);
            if (oneBasedIndex == null || !isValidIndex(sender, oneBasedIndex)) {
                return true;
            }
            if (args.length < 3) {
                send(sender, "&cUsage: &f/repeat bossbar <index> message <texte>");
                return true;
            }
            return setBossBarMessage(sender, oneBasedIndex, join(args, 2));
        }

        if (args.length != 2 && args.length != 3) {
            sendBossBarUsage(sender);
            return true;
        }

        if (args.length == 2) {
            if (isInteger(args[0])) {
                sendBossBarUsage(sender);
                return true;
            }
            return applyBossBarProperty(sender, args[0], args[1], null);
        }

        Integer oneBasedIndex = parseIndexArg(sender, args[0]);
        if (oneBasedIndex == null || !isValidIndex(sender, oneBasedIndex)) {
            return true;
        }
        return applyBossBarProperty(sender, args[1], args[2], oneBasedIndex);
    }

    private boolean setBossBarMessage(CommandSender sender, Integer oneBasedIndex, String message) {
        if (message.isBlank()) {
            send(sender, "&cThe bossbar message cannot be empty.");
            return true;
        }

        if (oneBasedIndex == null) {
            if (plugin.getMessageCount() == 0) {
                plugin.addMessage(message, DeliveryMode.BOSS_BAR);
            } else {
                plugin.setMessageAt(0, message);
                plugin.setDeliveryModeAt(0, DeliveryMode.BOSS_BAR);
            }
            plugin.savePluginData();
            send(sender, "&aBossbar message &f#1 &aupdated.");
            send(sender, "&7Preview: &r" + TextUtils.colorize(message));
            plugin.preview(sender, plugin.getLiveEntryAt(0));
            return true;
        }

        plugin.setMessageAt(oneBasedIndex - 1, message);
        plugin.setDeliveryModeAt(oneBasedIndex - 1, DeliveryMode.BOSS_BAR);
        plugin.savePluginData();
        send(sender, "&aBossbar message &f#" + oneBasedIndex + " &aupdated.");
        send(sender, "&7Preview: &r" + TextUtils.colorize(message));
        plugin.preview(sender, plugin.getLiveEntryAt(oneBasedIndex - 1));
        return true;
    }

    private boolean isBossBarMessageProperty(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return "message".equals(normalized) || "msg".equals(normalized) || "text".equals(normalized);
    }

    private boolean applyBossBarProperty(CommandSender sender, String property, String value, Integer oneBasedIndex) {
        String key = property.trim().toLowerCase(Locale.ROOT);
        Consumer<BossBarSettings> updater = switch (key) {
            case "color" -> {
                BarColor color = BossBarSettings.parseColor(value);
                if (color == null) {
                    send(sender, "&cInvalid color. Use: &f" + String.join(", ", BOSSBAR_COLORS));
                    yield null;
                }
                yield settings -> settings.setColor(color);
            }
            case "style" -> {
                BarStyle style = BossBarSettings.parseStyle(value);
                if (style == null) {
                    send(sender, "&cInvalid style. Use: &f" + String.join(", ", BOSSBAR_STYLES));
                    yield null;
                }
                yield settings -> settings.setStyle(style);
            }
            case "duration" -> {
                int seconds;
                try {
                    seconds = Integer.parseInt(value);
                } catch (NumberFormatException exception) {
                    send(sender, "&cDuration must be a number of seconds (0 = until replaced).");
                    yield null;
                }
                if (seconds < 0) {
                    send(sender, "&cDuration must be 0 or greater.");
                    yield null;
                }
                yield settings -> settings.setDurationSeconds(seconds);
            }
            case "progress" -> {
                int percent;
                try {
                    percent = Integer.parseInt(value);
                } catch (NumberFormatException exception) {
                    send(sender, "&cProgress must be a number between 0 and 100.");
                    yield null;
                }
                if (percent < 0 || percent > 100) {
                    send(sender, "&cProgress must be between 0 and 100.");
                    yield null;
                }
                yield settings -> settings.setProgress(percent / 100.0D);
            }
            default -> {
                send(sender, "&cUnknown bossbar property. Use &fmessage&c, &fcolor&c, &fstyle&c, &fduration &cor &fprogress&c.");
                yield null;
            }
        };

        if (updater == null) {
            return true;
        }

        if (oneBasedIndex == null) {
            if (plugin.getMessageCount() == 0) {
                send(sender, "&cNo messages are configured.");
                return true;
            }
            plugin.updateBossBarForAll(updater);
            plugin.savePluginData();
            send(sender, "&aBossbar &f" + key + " &aupdated for all messages: &f" + formatBossBarValue(key, value) + "&a.");
            return true;
        }

        plugin.updateBossBarAt(oneBasedIndex - 1, updater);
        plugin.savePluginData();
        send(sender, "&aBossbar &f" + key + " &afor message &f#" + oneBasedIndex + " &aupdated: &f"
                + formatBossBarValue(key, value) + "&a.");
        return true;
    }

    private String formatBossBarValue(String property, String value) {
        if ("duration".equals(property)) {
            return "0".equals(value) ? "until replaced" : value + "s";
        }
        if ("progress".equals(property)) {
            return value + "%";
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private void sendBossBarUsage(CommandSender sender) {
        send(sender, "&cUsage: &f/repeat bossbar message <texte>");
        send(sender, "&7Or: &f/repeat bossbar <index> message <texte>");
        send(sender, "&7Or: &f/repeat bossbar [index] <color|style|duration> <value>");
        send(sender, "&7Colors: &f" + String.join(", ", BOSSBAR_COLORS));
        send(sender, "&7Styles: &f" + String.join(", ", BOSSBAR_STYLES));
        send(sender, "&7Duration: &fseconds &7(time for the bar to empty, then next message)");
    }

    private boolean handleStatus(CommandSender sender) {
        send(sender, "&eStatus");
        sender.sendMessage(SEP);
        sender.sendMessage(TextUtils.colorize("&7Enabled: &f" + plugin.isMessageEnabled()));
        sender.sendMessage(TextUtils.colorize("&7Messages: &f" + plugin.getMessageCount()));
        sender.sendMessage(TextUtils.colorize("&7Next index: &f#" + (plugin.getNextMessageIndex() + 1)));
        sender.sendMessage(TextUtils.colorize("&7Default interval: &f" + plugin.getDefaultIntervalSeconds() + "s"));
        sender.sendMessage(TextUtils.colorize("&7Min players: &f" + plugin.getMinPlayers()));
        sender.sendMessage(TextUtils.colorize("&7Require repeat.receive: &f" + plugin.isRequireReceivePermission()));
        sender.sendMessage(TextUtils.colorize("&7Online players: &f" + Bukkit.getOnlinePlayers().size()));
        return true;
    }

    private boolean handleHelp(CommandSender sender) {
        send(sender, "&eHelp");
        send(sender, "&7Colors: &f&a &7&f&l &7&f&r &7hex &f&#RRGGBB");
        send(sender, "&7Placeholders: &f{online} {max} {player} {world} {server}");
        sender.sendMessage(SEP);
        send(sender, "&6Messages");
        send(sender, "&f/repeat add [chat|actionbar|bossbar] <message> &7- Add a message");
        send(sender, "&f/repeat set [index] <message> &7- Edit a message");
        send(sender, "&f/repeat remove <index> &7- Remove a message");
        send(sender, "&f/repeat move <from> <to> &7- Reorder messages");
        send(sender, "&f/repeat clear confirm &7- Delete all messages");
        send(sender, "&f/repeat list &7- List messages");
        send(sender, "&f/repeat test [index] &7- Preview a message");
        sender.sendMessage(SEP);
        send(sender, "&6Broadcast");
        send(sender, "&f/repeat toggle &7- Enable or disable auto messages");
        send(sender, "&f/repeat interval [index] <seconds> &7- Set delay");
        send(sender, "&f/repeat delivery [index] <chat|actionbar|bossbar> &7- Set display");
        send(sender, "&f/repeat worlds [index] <all|worlds> &7- Limit worlds");
        send(sender, "&f/repeat minplayers <count> &7- Minimum online players");
        sender.sendMessage(SEP);
        send(sender, "&6Bossbar");
        send(sender, "&f/repeat bossbar [index] message <texte> &7- Set bossbar text");
        send(sender, "&f/repeat bossbar [index] color <color> &7- Bar color");
        send(sender, "&f/repeat bossbar [index] style <style> &7- Bar style");
        send(sender, "&f/repeat bossbar [index] duration <seconds> &7- Time to empty the bar");
        sender.sendMessage(SEP);
        send(sender, "&6Admin");
        send(sender, "&f/repeat status &7- Show plugin status");
        send(sender, "&f/repeat reload &7- Reload config.yml");
        send(sender, "&f/repeat help &7- Show this help");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("repeat.admin")) {
            return Collections.emptyList();
        }

        String commandName = command.getName().toLowerCase(Locale.ROOT);
        if ("repeat".equals(commandName)) {
            if (args.length == 1) {
                return filterByPrefix(SUBCOMMANDS, args[0]);
            }
            return tabCompleteSubcommand(args[0].toLowerCase(Locale.ROOT), Arrays.copyOfRange(args, 1, args.length));
        }
        return tabCompleteSubcommand(mapLegacyCommand(commandName), args);
    }

    private List<String> tabCompleteSubcommand(String subcommand, String[] args) {
        return switch (subcommand) {
            case "interval" -> tabCompleteIndexed(args, List.of("30", "60", "120", "300"));
            case "delivery" -> tabCompleteIndexed(args, DELIVERY_MODES);
            case "worlds" -> tabCompleteWorlds(args);
            case "bossbar" -> tabCompleteBossBar(args);
            case "add" -> args.length == 1 ? filterByPrefix(DELIVERY_MODES, args[0]) : Collections.emptyList();
            case "clear" -> args.length == 1 ? filterByPrefix(List.of("confirm"), args[0]) : Collections.emptyList();
            case "set", "remove", "test" -> args.length == 1 ? tabCompleteIndexes(args) : Collections.emptyList();
            case "move" -> (args.length == 1 || args.length == 2) ? tabCompleteIndexes(args) : Collections.emptyList();
            case "minplayers" -> args.length == 1
                    ? filterByPrefix(List.of("0", "1", "2", "5", "10"), args[0])
                    : Collections.emptyList();
            default -> Collections.emptyList();
        };
    }

    private List<String> tabCompleteIndexed(String[] args, List<String> values) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>(values);
            addIndexSuggestions(suggestions);
            return filterByPrefix(suggestions, args[0]);
        }
        if (args.length == 2 && isInteger(args[0])) {
            return filterByPrefix(values, args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> tabCompleteWorlds(String[] args) {
        List<String> worlds = new ArrayList<>();
        worlds.add("all");
        for (var world : Bukkit.getWorlds()) {
            worlds.add(world.getName());
        }
        return tabCompleteIndexed(args, worlds);
    }

    private List<String> tabCompleteBossBar(String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>(BOSSBAR_PROPERTIES);
            addIndexSuggestions(suggestions);
            return filterByPrefix(suggestions, args[0]);
        }

        boolean indexed = isInteger(args[0]);
        if (indexed) {
            if (args.length == 2) {
                return filterByPrefix(BOSSBAR_PROPERTIES, args[1]);
            }
            if (args.length == 3) {
                return filterByPrefix(bossBarValueSuggestions(args[1]), args[2]);
            }
            return Collections.emptyList();
        }
        if (args.length == 2) {
            return filterByPrefix(bossBarValueSuggestions(args[0]), args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> bossBarValueSuggestions(String property) {
        return switch (property.toLowerCase(Locale.ROOT)) {
            case "color" -> BOSSBAR_COLORS;
            case "style" -> BOSSBAR_STYLES;
            case "duration" -> List.of("0", "3", "5", "8", "10");
            case "progress" -> List.of("25", "50", "75", "100");
            default -> Collections.emptyList();
        };
    }

    private List<String> tabCompleteIndexes(String[] args) {
        if (args.length == 1 || args.length == 2) {
            List<String> suggestions = new ArrayList<>();
            addIndexSuggestions(suggestions);
            return filterByPrefix(suggestions, args[args.length - 1]);
        }
        return Collections.emptyList();
    }

    private void addIndexSuggestions(List<String> suggestions) {
        int count = plugin.getMessageCount();
        for (int i = 1; i <= count; i++) {
            suggestions.add(String.valueOf(i));
        }
    }

    private List<String> parseWorldsArgument(String value) {
        if ("all".equalsIgnoreCase(value)) {
            return Collections.emptyList();
        }
        String[] parts = value.split(",");
        List<String> worlds = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                worlds.add(trimmed);
            }
        }
        return worlds;
    }

    private DeliveryMode parseDeliveryMode(CommandSender sender, String value) {
        if (!DeliveryMode.isKnown(value)) {
            send(sender, "&cInvalid delivery mode. Use &fchat&c, &factionbar &cor &fbossbar&c.");
            return null;
        }
        return DeliveryMode.fromString(value);
    }

    private long parsePositiveLong(CommandSender sender, String value) {
        try {
            long seconds = Long.parseLong(value);
            if (seconds <= 0) {
                send(sender, "&cThe number must be greater than 0.");
                return -1;
            }
            return seconds;
        } catch (NumberFormatException exception) {
            send(sender, "&cYou must enter a valid number.");
            return -1;
        }
    }

    private Integer parseIndexArg(CommandSender sender, String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            send(sender, "&cYou must enter a valid message index.");
            return null;
        }
    }

    private boolean isValidIndex(CommandSender sender, int oneBasedIndex) {
        int messageCount = plugin.getMessageCount();
        if (messageCount == 0) {
            send(sender, "&cNo messages are configured.");
            return false;
        }
        if (oneBasedIndex <= 0 || oneBasedIndex > messageCount) {
            send(sender, "&cInvalid index. Use &f/repeat list &cto see valid indexes.");
            return false;
        }
        return true;
    }

    private List<String> filterByPrefix(List<String> source, String typed) {
        String prefix = typed.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : source) {
            if (value.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                result.add(value);
            }
        }
        return result;
    }

    private boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private String join(String[] args, int fromIndex) {
        return String.join(" ", Arrays.copyOfRange(args, fromIndex, args.length)).trim();
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + TextUtils.colorize(message));
    }
}
