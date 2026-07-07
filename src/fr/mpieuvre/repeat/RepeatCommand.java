package fr.mpieuvre.repeat;

import org.bukkit.Bukkit;
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

public class RepeatCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;
    private static final String PREFIX = "\u00A76[Repeat] \u00A7r";
    private static final String SEP = "\u00A78--------------------------------";

    public RepeatCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasAdminPermission(sender)) {
            sender.sendMessage(PREFIX + "\u00A7cYou don't have permission to use this command.");
            return true;
        }

        String commandName = command.getName().toLowerCase(Locale.ROOT);
        return switch (commandName) {
            case "setmessage" -> handleSetMessage(sender, args);
            case "addmessages", "addmessage" -> handleAddMessage(sender, args);
            case "removemessage" -> handleRemoveMessage(sender, args);
            case "clearmessages" -> handleClearMessages(sender, args);
            case "movemessage" -> handleMoveMessage(sender, args);
            case "testmessage" -> handleTestMessage(sender);
            case "testemessages" -> handleTestSelectedMessage(sender, args);
            case "listemessage" -> handleListMessage(sender);
            case "repeatreload" -> handleReload(sender);
            case "togglemessage" -> handleToggleMessage(sender);
            case "setinterval" -> handleSetInterval(sender, args);
            case "setdelivery" -> handleSetDelivery(sender, args);
            case "setworlds" -> handleSetWorlds(sender, args);
            case "setminplayers" -> handleSetMinPlayers(sender, args);
            case "repeatstatus" -> handleStatus(sender);
            case "repeathelp" -> handleHelp(sender);
            default -> false;
        };
    }

    private boolean hasAdminPermission(CommandSender sender) {
        return sender.hasPermission("repeat.admin");
    }

    private boolean handleSetMessage(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(PREFIX + "\u00A7cUsage: \u00A7f/setmessage <message>");
            sender.sendMessage(PREFIX + "\u00A77Or: \u00A7f/setmessage <index> <message>");
            return true;
        }

        if (args.length >= 2 && isInteger(args[0])) {
            int oneBasedIndex = Integer.parseInt(args[0]);
            if (!isValidIndex(sender, oneBasedIndex)) {
                return true;
            }

            String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
            plugin.setMessageAt(oneBasedIndex - 1, message);
            plugin.savePluginData();
            sender.sendMessage(PREFIX + "\u00A7aMessage \u00A7f#" + oneBasedIndex + "\u00A7a updated.");
            return true;
        }

        String message = String.join(" ", args).trim();
        plugin.setPrimaryMessage(message);
        plugin.savePluginData();
        sender.sendMessage(PREFIX + "\u00A7aPrimary message updated (\u00A7f#1\u00A7a).");
        return true;
    }

    private boolean handleAddMessage(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(PREFIX + "\u00A7cUsage: \u00A7f/addmessages <message>");
            return true;
        }

        String message = String.join(" ", args).trim();
        plugin.addMessage(message);
        plugin.savePluginData();
        sender.sendMessage(PREFIX + "\u00A7aMessage added. Total: \u00A7f" + plugin.getMessageCount() + "\u00A7a.");
        return true;
    }

    private boolean handleRemoveMessage(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(PREFIX + "\u00A7cUsage: \u00A7f/removemessage <index>");
            return true;
        }

        int oneBasedIndex;
        try {
            oneBasedIndex = Integer.parseInt(args[0]);
        } catch (NumberFormatException exception) {
            sender.sendMessage(PREFIX + "\u00A7cYou must enter a valid message index.");
            return true;
        }

        if (!isValidIndex(sender, oneBasedIndex)) {
            return true;
        }

        plugin.removeMessageAt(oneBasedIndex - 1);
        plugin.savePluginData();
        plugin.startAutoMessageTask();
        sender.sendMessage(PREFIX + "\u00A7aMessage \u00A7f#" + oneBasedIndex + "\u00A7a removed.");
        return true;
    }

    private boolean handleClearMessages(CommandSender sender, String[] args) {
        if (plugin.getMessageCount() == 0) {
            sender.sendMessage(PREFIX + "\u00A7cNo messages are configured.");
            return true;
        }

        if (args.length != 1 || !"confirm".equalsIgnoreCase(args[0])) {
            sender.sendMessage(PREFIX + "\u00A7cUsage: \u00A7f/clearmessages confirm");
            sender.sendMessage(PREFIX + "\u00A77This will delete all \u00A7f" + plugin.getMessageCount() + "\u00A77 configured messages.");
            return true;
        }

        plugin.clearMessages();
        plugin.savePluginData();
        plugin.startAutoMessageTask();
        sender.sendMessage(PREFIX + "\u00A7aAll messages have been cleared.");
        return true;
    }

    private boolean handleMoveMessage(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(PREFIX + "\u00A7cUsage: \u00A7f/movemessage <from> <to>");
            return true;
        }

        int fromIndex;
        int toIndex;
        try {
            fromIndex = Integer.parseInt(args[0]);
            toIndex = Integer.parseInt(args[1]);
        } catch (NumberFormatException exception) {
            sender.sendMessage(PREFIX + "\u00A7cYou must enter valid message indexes.");
            return true;
        }

        if (!isValidIndex(sender, fromIndex) || !isValidIndex(sender, toIndex)) {
            return true;
        }

        plugin.moveMessage(fromIndex - 1, toIndex - 1);
        plugin.savePluginData();
        sender.sendMessage(PREFIX + "\u00A7aMoved message \u00A7f#" + fromIndex + "\u00A7a to position \u00A7f#" + toIndex + "\u00A7a.");
        return true;
    }

    private boolean handleTestMessage(CommandSender sender) {
        MessageEntry entry = plugin.getNextEntryForPreview();
        sender.sendMessage(PREFIX + "\u00A7aNext message preview (\u00A7f#" + (plugin.getNextMessageIndex() + 1) + "\u00A7a):");
        sendPreview(sender, entry);
        return true;
    }

    private boolean handleTestSelectedMessage(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(PREFIX + "\u00A7cUsage: \u00A7f/testemessages <index>");
            return true;
        }

        int oneBasedIndex;
        try {
            oneBasedIndex = Integer.parseInt(args[0]);
        } catch (NumberFormatException exception) {
            sender.sendMessage(PREFIX + "\u00A7cYou must enter a valid message index.");
            return true;
        }

        if (!isValidIndex(sender, oneBasedIndex)) {
            return true;
        }

        MessageEntry entry = plugin.getEntryAt(oneBasedIndex - 1);
        sender.sendMessage(PREFIX + "\u00A7aMessage \u00A7f#" + oneBasedIndex + "\u00A7a preview:");
        sendPreview(sender, entry);
        return true;
    }

    private void sendPreview(CommandSender sender, MessageEntry entry) {
        Player context = sender instanceof Player player ? player : null;
        String resolved = PlaceholderService.apply(entry.getMessage(), context);
        String formatted = TextUtils.colorize(resolved);

        if (sender instanceof Player player && entry.getDeliveryMode() == DeliveryMode.ACTION_BAR) {
            MessageDelivery.send(player, DeliveryMode.ACTION_BAR, formatted);
            sender.sendMessage(PREFIX + "\u00A77(action bar preview sent)");
            return;
        }

        sender.sendMessage(formatted);
    }

    private boolean handleListMessage(CommandSender sender) {
        List<MessageEntry> entries = plugin.getEntries();
        if (entries.isEmpty()) {
            sender.sendMessage(PREFIX + "\u00A7cNo messages are configured.");
            return true;
        }

        sender.sendMessage(PREFIX + "\u00A7eConfigured messages \u00A77(" + entries.size() + ")\u00A7e:");
        sender.sendMessage(SEP);
        int nextIndex = plugin.getNextMessageIndex();
        for (int i = 0; i < entries.size(); i++) {
            MessageEntry entry = entries.get(i);
            String marker = (i == nextIndex) ? " \u00A7a(next)" : "";
            sender.sendMessage("\u00A76#" + (i + 1) + marker
                    + " \u00A78| \u00A77interval \u00A7f" + entry.getIntervalSeconds() + "s"
                    + " \u00A78| \u00A77delivery \u00A7f" + MessageEntry.formatDelivery(entry.getDeliveryMode())
                    + " \u00A78| \u00A77worlds \u00A7f" + MessageEntry.formatWorlds(entry.getWorlds()));
            sender.sendMessage("\u00A78raw \u00A77(\u00A7fedit\u00A77): \u00A7f" + entry.getMessage());
            sender.sendMessage("\u00A78preview: \u00A7r" + TextUtils.colorize(entry.getMessage()));
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
        sender.sendMessage(PREFIX + "\u00A7aAuto messages " + (newState ? "\u00A7aenabled" : "\u00A7cdisabled") + "\u00A7a.");
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        plugin.reloadPluginData();
        sender.sendMessage(PREFIX + "\u00A7aConfiguration reloaded.");
        return true;
    }

    private boolean handleSetInterval(CommandSender sender, String[] args) {
        if (args.length != 1 && args.length != 2) {
            sender.sendMessage(PREFIX + "\u00A7cUsage: \u00A7f/setinterval <seconds>");
            sender.sendMessage(PREFIX + "\u00A77Or: \u00A7f/setinterval <index> <seconds>");
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
            sender.sendMessage(PREFIX + "\u00A7aInterval updated for all messages: \u00A7f" + seconds + "s\u00A7a.");
            return true;
        }

        int oneBasedIndex;
        try {
            oneBasedIndex = Integer.parseInt(args[0]);
        } catch (NumberFormatException exception) {
            sender.sendMessage(PREFIX + "\u00A7cUsage: \u00A7f/setinterval <index> <seconds>");
            return true;
        }

        if (!isValidIndex(sender, oneBasedIndex)) {
            return true;
        }

        long seconds = parsePositiveLong(sender, args[1]);
        if (seconds < 0) {
            return true;
        }

        plugin.setIntervalSecondsAt(oneBasedIndex - 1, seconds);
        plugin.startAutoMessageTask();
        plugin.savePluginData();
        sender.sendMessage(PREFIX + "\u00A7aInterval for message \u00A7f#" + oneBasedIndex + "\u00A7a updated: \u00A7f" + seconds + "s\u00A7a.");
        return true;
    }

    private boolean handleSetDelivery(CommandSender sender, String[] args) {
        if (args.length != 1 && args.length != 2) {
            sender.sendMessage(PREFIX + "\u00A7cUsage: \u00A7f/setdelivery <chat|actionbar>");
            sender.sendMessage(PREFIX + "\u00A77Or: \u00A7f/setdelivery <index> <chat|actionbar>");
            return true;
        }

        if (args.length == 1) {
            DeliveryMode mode = parseDeliveryMode(sender, args[0]);
            if (mode == null) {
                return true;
            }

            plugin.setDeliveryModeForAll(mode);
            plugin.savePluginData();
            sender.sendMessage(PREFIX + "\u00A7aDelivery mode updated for all messages: \u00A7f" + MessageEntry.formatDelivery(mode) + "\u00A7a.");
            return true;
        }

        int oneBasedIndex;
        try {
            oneBasedIndex = Integer.parseInt(args[0]);
        } catch (NumberFormatException exception) {
            sender.sendMessage(PREFIX + "\u00A7cUsage: \u00A7f/setdelivery <index> <chat|actionbar>");
            return true;
        }

        if (!isValidIndex(sender, oneBasedIndex)) {
            return true;
        }

        DeliveryMode mode = parseDeliveryMode(sender, args[1]);
        if (mode == null) {
            return true;
        }

        plugin.setDeliveryModeAt(oneBasedIndex - 1, mode);
        plugin.savePluginData();
        sender.sendMessage(PREFIX + "\u00A7aDelivery mode for message \u00A7f#" + oneBasedIndex + "\u00A7a updated: \u00A7f" + MessageEntry.formatDelivery(mode) + "\u00A7a.");
        return true;
    }

    private boolean handleSetWorlds(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(PREFIX + "\u00A7cUsage: \u00A7f/setworlds all");
            sender.sendMessage(PREFIX + "\u00A77Or: \u00A7f/setworlds <world1,world2,...>");
            sender.sendMessage(PREFIX + "\u00A77Or: \u00A7f/setworlds <index> <world1,world2,...|all>");
            return true;
        }

        if (args.length == 1) {
            List<String> worlds = parseWorldsArgument(args[0]);
            plugin.setWorldsForAll(worlds);
            plugin.savePluginData();
            sender.sendMessage(PREFIX + "\u00A7aWorld filter updated for all messages: \u00A7f" + MessageEntry.formatWorlds(worlds) + "\u00A7a.");
            return true;
        }

        int oneBasedIndex;
        try {
            oneBasedIndex = Integer.parseInt(args[0]);
        } catch (NumberFormatException exception) {
            sender.sendMessage(PREFIX + "\u00A7cUsage: \u00A7f/setworlds <index> <world1,world2,...|all>");
            return true;
        }

        if (!isValidIndex(sender, oneBasedIndex)) {
            return true;
        }

        List<String> worlds = parseWorldsArgument(args[1]);
        plugin.setWorldsAt(oneBasedIndex - 1, worlds);
        plugin.savePluginData();
        sender.sendMessage(PREFIX + "\u00A7aWorld filter for message \u00A7f#" + oneBasedIndex + "\u00A7a updated: \u00A7f" + MessageEntry.formatWorlds(worlds) + "\u00A7a.");
        return true;
    }

    private boolean handleSetMinPlayers(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(PREFIX + "\u00A7cUsage: \u00A7f/setminplayers <count>");
            return true;
        }

        int minPlayers;
        try {
            minPlayers = Integer.parseInt(args[0]);
        } catch (NumberFormatException exception) {
            sender.sendMessage(PREFIX + "\u00A7cYou must enter a valid number.");
            return true;
        }

        if (minPlayers < 0) {
            sender.sendMessage(PREFIX + "\u00A7cThe number must be 0 or greater.");
            return true;
        }

        plugin.setMinPlayers(minPlayers);
        plugin.savePluginData();
        sender.sendMessage(PREFIX + "\u00A7aMinimum players required to broadcast: \u00A7f" + minPlayers + "\u00A7a.");
        return true;
    }

    private boolean handleStatus(CommandSender sender) {
        sender.sendMessage(PREFIX + "\u00A7eStatus");
        sender.sendMessage(SEP);
        sender.sendMessage("\u00A77Enabled: \u00A7f" + plugin.isMessageEnabled());
        sender.sendMessage("\u00A77Messages: \u00A7f" + plugin.getMessageCount());
        sender.sendMessage("\u00A77Next index: \u00A7f#" + (plugin.getNextMessageIndex() + 1));
        sender.sendMessage("\u00A77Default interval: \u00A7f" + plugin.getDefaultIntervalSeconds() + "s");
        sender.sendMessage("\u00A77Min players: \u00A7f" + plugin.getMinPlayers());
        sender.sendMessage("\u00A77Require repeat.receive: \u00A7f" + plugin.isRequireReceivePermission());
        sender.sendMessage("\u00A77Online players: \u00A7f" + Bukkit.getOnlinePlayers().size());
        return true;
    }

    private boolean handleHelp(CommandSender sender) {
        sender.sendMessage(PREFIX + "\u00A7eHelp");
        sender.sendMessage(PREFIX + "\u00A77Colors: \u00A7f&a \u00A77\u00A7f&l \u00A77\u00A7f&r \u00A77hex \u00A7f&#RRGGBB");
        sender.sendMessage(PREFIX + "\u00A77Placeholders: \u00A7f{online} {max} {player} {world} {server}");
        sender.sendMessage(PREFIX + "\u00A7f/setmessage <message> \u00A77- Set message #1");
        sender.sendMessage(PREFIX + "\u00A7f/setmessage <index> <message> \u00A77- Edit message");
        sender.sendMessage(PREFIX + "\u00A7f/addmessages <message> \u00A77- Add message");
        sender.sendMessage(PREFIX + "\u00A7f/removemessage <index> \u00A77- Remove message");
        sender.sendMessage(PREFIX + "\u00A7f/clearmessages confirm \u00A77- Delete all messages");
        sender.sendMessage(PREFIX + "\u00A7f/movemessage <from> <to> \u00A77- Reorder messages");
        sender.sendMessage(PREFIX + "\u00A7f/testmessage \u00A77- Preview next message");
        sender.sendMessage(PREFIX + "\u00A7f/testemessages <index> \u00A77- Preview one message");
        sender.sendMessage(PREFIX + "\u00A7f/listemessage \u00A77- List messages");
        sender.sendMessage(PREFIX + "\u00A7f/repeatreload \u00A77- Reload config");
        sender.sendMessage(PREFIX + "\u00A7f/togglemessage \u00A77- Enable/disable");
        sender.sendMessage(PREFIX + "\u00A7f/setinterval <seconds> \u00A77- Set interval for all");
        sender.sendMessage(PREFIX + "\u00A7f/setinterval <index> <seconds> \u00A77- Set interval for one");
        sender.sendMessage(PREFIX + "\u00A7f/setdelivery <chat|actionbar> \u00A77- Set delivery for all");
        sender.sendMessage(PREFIX + "\u00A7f/setdelivery <index> <chat|actionbar> \u00A77- Set delivery for one");
        sender.sendMessage(PREFIX + "\u00A7f/setworlds all \u00A77- Broadcast in all worlds");
        sender.sendMessage(PREFIX + "\u00A7f/setworlds <worlds> \u00A77- Limit worlds for all messages");
        sender.sendMessage(PREFIX + "\u00A7f/setminplayers <count> \u00A77- Minimum online players");
        sender.sendMessage(PREFIX + "\u00A7f/repeatstatus \u00A77- Show plugin status");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!hasAdminPermission(sender)) {
            return Collections.emptyList();
        }

        String commandName = command.getName().toLowerCase(Locale.ROOT);

        if ("setinterval".equals(commandName) && args.length == 1) {
            List<String> suggestions = new ArrayList<>(List.of("30", "60", "120", "300"));
            addIndexSuggestions(suggestions);
            return filterByPrefix(suggestions, args[0]);
        }

        if ("setinterval".equals(commandName) && args.length == 2 && isInteger(args[0])) {
            return filterByPrefix(List.of("30", "60", "120", "300"), args[1]);
        }

        if ("setdelivery".equals(commandName) && (args.length == 1 || (args.length == 2 && isInteger(args[0])))) {
            List<String> suggestions = new ArrayList<>(List.of("chat", "actionbar"));
            if (args.length == 1) {
                addIndexSuggestions(suggestions);
            }
            return filterByPrefix(suggestions, args[args.length - 1]);
        }

        if ("setworlds".equals(commandName) && (args.length == 1 || (args.length == 2 && isInteger(args[0])))) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("all");
            for (var world : Bukkit.getWorlds()) {
                suggestions.add(world.getName());
            }
            if (args.length == 1) {
                addIndexSuggestions(suggestions);
            }
            return filterByPrefix(suggestions, args[args.length - 1]);
        }

        if ("clearmessages".equals(commandName) && args.length == 1) {
            return filterByPrefix(List.of("confirm"), args[0]);
        }

        if (List.of("setmessage", "testemessages", "removemessage", "movemessage").contains(commandName) && args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            addIndexSuggestions(suggestions);
            return filterByPrefix(suggestions, args[0]);
        }

        if ("movemessage".equals(commandName) && args.length == 2) {
            List<String> suggestions = new ArrayList<>();
            addIndexSuggestions(suggestions);
            return filterByPrefix(suggestions, args[1]);
        }

        if ("setminplayers".equals(commandName) && args.length == 1) {
            return filterByPrefix(List.of("0", "1", "2", "5", "10"), args[0]);
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
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
        if (!"chat".equals(normalized) && !"actionbar".equals(normalized)) {
            sender.sendMessage(PREFIX + "\u00A7cInvalid delivery mode. Use \u00A7fchat\u00A7c or \u00A7factionbar\u00A7c.");
            return null;
        }
        return DeliveryMode.fromString(value);
    }

    private long parsePositiveLong(CommandSender sender, String value) {
        try {
            long seconds = Long.parseLong(value);
            if (seconds <= 0) {
                sender.sendMessage(PREFIX + "\u00A7cThe number must be greater than 0.");
                return -1;
            }
            return seconds;
        } catch (NumberFormatException exception) {
            sender.sendMessage(PREFIX + "\u00A7cYou must enter a valid number.");
            return -1;
        }
    }

    private boolean isValidIndex(CommandSender sender, int oneBasedIndex) {
        int messageCount = plugin.getMessageCount();
        if (messageCount == 0) {
            sender.sendMessage(PREFIX + "\u00A7cNo messages are configured.");
            return false;
        }

        if (oneBasedIndex <= 0 || oneBasedIndex > messageCount) {
            sender.sendMessage(PREFIX + "\u00A7cInvalid index. Use \u00A7f/listemessage\u00A7c to see valid indexes.");
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
}
