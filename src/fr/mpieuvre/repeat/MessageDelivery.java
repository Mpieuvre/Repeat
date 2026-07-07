package fr.mpieuvre.repeat;

import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class MessageDelivery {
    private static final boolean ACTION_BAR_SUPPORTED = detectActionBarSupport();

    private MessageDelivery() {
    }

    public static void send(Player player, DeliveryMode mode, String formattedMessage) {
        if (mode == DeliveryMode.ACTION_BAR && ACTION_BAR_SUPPORTED) {
            if (sendActionBar(player, formattedMessage)) {
                return;
            }
        }
        player.sendMessage(formattedMessage);
    }

    private static boolean sendActionBar(Player player, String formattedMessage) {
        try {
            Class<?> chatMessageTypeClass = Class.forName("net.md_5.bungee.api.ChatMessageType");
            Object actionBarType = Enum.valueOf(asEnumClass(chatMessageTypeClass), "ACTION_BAR");

            Class<?> textComponentClass = Class.forName("net.md_5.bungee.api.chat.TextComponent");
            Constructor<?> textComponentConstructor = textComponentClass.getConstructor(String.class);
            Object textComponent = textComponentConstructor.newInstance(formattedMessage);

            Method spigotMethod = player.getClass().getMethod("spigot");
            Object spigotPlayer = spigotMethod.invoke(player);

            Class<?> baseComponentClass = Class.forName("net.md_5.bungee.api.chat.BaseComponent");
            Method sendMessageMethod = spigotPlayer.getClass().getMethod("sendMessage", chatMessageTypeClass, baseComponentClass);
            sendMessageMethod.invoke(spigotPlayer, actionBarType, textComponent);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Class<? extends Enum> asEnumClass(Class<?> enumClass) {
        return (Class<? extends Enum>) enumClass;
    }

    private static boolean detectActionBarSupport() {
        try {
            Class.forName("net.md_5.bungee.api.ChatMessageType");
            Class.forName("net.md_5.bungee.api.chat.TextComponent");
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }
}
