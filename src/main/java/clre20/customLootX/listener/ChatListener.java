package clre20.customLootX.listener;

import clre20.customLootX.CustomLootX;
import clre20.customLootX.util.TextUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {

    private final CustomLootX plugin;

    public ChatListener(CustomLootX plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (plugin.getChatInputManager().isWaitingInput(player)) {
            event.setCancelled(true);
            String plainMessage = TextUtil.toPlainText(event.message());
            plugin.getChatInputManager().handleChat(player, plainMessage);
        }
    }
}
