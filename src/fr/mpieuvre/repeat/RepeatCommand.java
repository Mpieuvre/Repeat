package fr.mpieuvre.repeat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RepeatCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;

    public RepeatCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("\u00A7cOnly operators can use this command.");
            return true;
        }

        String commandName = command.getName().toLowerCase();
        switch (commandName) {
            case "setmessage":
                return handleSetMessage(sender, args);
            case "togglemessage":
                return handleToggleMessage(sender);
            case "setinterval":
                return handleSetInterval(sender, args);
            case "repeathelp":
                return handleHelp(sender);
            default:
                return false;
        }
    }

    private boolean handleSetMessage(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("\u00A7cUsage: /setmessage <message>");
            return true;
        }

        String message = String.join(" ", args).trim();
        plugin.setAutoMessage(message);
        plugin.savePluginData();
        sender.sendMessage("\u00A7aAuto message updated.");
        return true;
    }

    private boolean handleToggleMessage(CommandSender sender) {
        boolean newState = !plugin.isMessageEnabled();
        plugin.setMessageEnabled(newState);
        plugin.savePluginData();
        sender.sendMessage("\u00A7aAuto messages " + (newState ? "enabled" : "disabled") + ".");
        return true;
    }

    private boolean handleSetInterval(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage("\u00A7cUsage: /setinterval <seconds>");
            return true;
        }

        long seconds;
        try {
            seconds = Long.parseLong(args[0]);
        } catch (NumberFormatException exception) {
            sender.sendMessage("\u00A7cYou must enter a valid number.");
            return true;
        }

        if (seconds <= 0) {
            sender.sendMessage("\u00A7cThe number must be greater than 0.");
            return true;
        }

        plugin.setIntervalSeconds(seconds);
        plugin.startAutoMessageTask();
        plugin.savePluginData();
        sender.sendMessage("\u00A7aInterval updated: " + seconds + "s.");
        return true;
    }

    private boolean handleHelp(CommandSender sender) {
        sender.sendMessage("\u00A76Repeat Help");
        sender.sendMessage("\u00A7f/setmessage <message> \u00A77- Sets the auto message");
        sender.sendMessage("\u00A7f/togglemessage \u00A77- Enables or disables auto messages");
        sender.sendMessage("\u00A7f/setinterval <seconds> \u00A77- Changes the interval");
        sender.sendMessage("\u00A7f/repeathelp \u00A77- Shows this help");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String commandName = command.getName().toLowerCase();

        if ("setinterval".equals(commandName) && args.length == 1) {
            List<String> suggestions = List.of("30", "60", "120", "300");
            return filterByPrefix(suggestions, args[0]);
        }

        return Collections.emptyList();
    }

    private List<String> filterByPrefix(List<String> source, String typed) {
        String prefix = typed.toLowerCase();
        List<String> result = new ArrayList<>();

        for (String value : source) {
            if (value.startsWith(prefix)) {
                result.add(value);
            }
        }

        return result;
    }
}
