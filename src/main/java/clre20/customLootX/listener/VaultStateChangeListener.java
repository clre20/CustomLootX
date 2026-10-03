package clre20.customLootX.listener;

import clre20.customLootX.CustomLootX;
import io.papermc.paper.event.block.VaultChangeStateEvent;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Vault;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * 監聽 Paper 核心的 VaultChangeStateEvent 事件。
 * 當自訂寶庫正在開獎吐物品時，強制阻止原版核心將其狀態切換為 INACTIVE，徹底杜絕百葉窗抽搐閉合。
 */
public class VaultStateChangeListener implements Listener {

    private final CustomLootX plugin;

    public VaultStateChangeListener(CustomLootX plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVaultChangeState(VaultChangeStateEvent event) {
        Block block = event.getBlock();
        if (block != null && block.getType() == Material.VAULT) {
            if (plugin.getVaultTemplateManager().isEjecting(block.getLocation())) {
                // 若正在開獎噴發戰利品中，禁止原版核心私自將方塊切換為非 EJECTING 狀態
                if (event.getNewState() != Vault.State.EJECTING) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
