package fr.mpieuvre.repeat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class PlaceholderService {
    private PlaceholderService() {
    }

    public static String apply(String template, Player player) {
        if (template == null || template.isEmpty()) {
            return template;
        }

        String playerName = player == null ? "Player" : player.getName();
        String worldName = player == null ? "world" : player.getWorld().getName();

        return template
                .replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("{max}", String.valueOf(Bukkit.getMaxPlayers()))
                .replace("{player}", playerName)
                .replace("{world}", worldName)
                .replace("{server}", Bukkit.getServer().getName());
    }
}
