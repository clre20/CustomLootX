package clre20.customLootX.manager;

import clre20.customLootX.CustomLootX;
import clre20.customLootX.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ChatInputManager {

    private final CustomLootX plugin;
    private final Map<UUID, InputSession> waitingPlayers = new ConcurrentHashMap<>();

    public static class InputSession {
        final Consumer<String> onInput;
        final Runnable onCancel;
        final BukkitTask timeoutTask;

        public InputSession(Consumer<String> onInput, Runnable onCancel, BukkitTask timeoutTask) {
            this.onInput = onInput;
            this.onCancel = onCancel;
            this.timeoutTask = timeoutTask;
        }
    }

    public ChatInputManager(CustomLootX plugin) {
        this.plugin = plugin;
    }

    /**
     * Request input from a player with prompt message and callbacks
     */
    public void requestInput(Player player, String promptMessage, Consumer<String> onInput, Runnable onCancel) {
        cancelInput(player);

        player.closeInventory();
        if (promptMessage != null && !promptMessage.isEmpty()) {
            player.sendMessage(TextUtil.parse(plugin.getConfigManager().getPrefix() + promptMessage));
        }

        BukkitTask timeoutTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            InputSession session = waitingPlayers.remove(player.getUniqueId());
            if (session != null) {
                player.sendMessage(TextUtil.parse(plugin.getConfigManager().getPrefix() + "&c輸入逾時，已自動取消。"));
                if (session.onCancel != null) {
                    session.onCancel.run();
                }
            }
        }, 20L * 60L); // 60 seconds timeout

        waitingPlayers.put(player.getUniqueId(), new InputSession(onInput, onCancel, timeoutTask));
    }

    public void waitForInput(Player player, Consumer<String> onInput) {
        requestInput(player, null, onInput, null);
    }

    public boolean isWaitingInput(Player player) {
        return waitingPlayers.containsKey(player.getUniqueId());
    }

    /**
     * Handle incoming chat message from player
     */
    public boolean handleChat(Player player, String rawMessage) {
        InputSession session = waitingPlayers.remove(player.getUniqueId());
        if (session == null) {
            return false;
        }

        if (session.timeoutTask != null) {
            session.timeoutTask.cancel();
        }

        String input = rawMessage.trim();
        if (input.equalsIgnoreCase("cancel") || input.equalsIgnoreCase("取消")) {
            player.sendMessage(TextUtil.parse(plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getRawMessage("input-cancelled")));
            if (session.onCancel != null) {
                Bukkit.getScheduler().runTask(plugin, session.onCancel);
            }
            return true;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                session.onInput.accept(input);
            } catch (Exception e) {
                plugin.logWarn("&3[聊天輸入]&c 處理玩家輸入時發生異常: " + e.getMessage());
            }
        });
        return true;
    }

    public void cancelInput(Player player) {
        InputSession session = waitingPlayers.remove(player.getUniqueId());
        if (session != null && session.timeoutTask != null) {
            session.timeoutTask.cancel();
        }
    }
}
