package clre20.customLootX.listener;

import clre20.customLootX.CustomLootX;
import clre20.customLootX.model.DraftType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

public class DraftItemListener implements Listener {

    private final CustomLootX plugin;

    public DraftItemListener(CustomLootX plugin) {
        this.plugin = plugin;
    }

    /**
     * 當玩家切換手持物品時，檢查新欄位物品是否為草稿且是否過期
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        checkAndRefreshDraftItem(item);
    }

    /**
     * 當玩家在背包或容器點擊物品時，檢查點擊物品與游標物品
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        checkAndRefreshDraftItem(event.getCurrentItem());
        checkAndRefreshDraftItem(event.getCursor());
    }

    /**
     * 當玩家撿起物品時，檢查撿起物品是否為草稿
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            checkAndRefreshDraftItem(event.getItem().getItemStack());
        }
    }

    private static boolean isPotentialDraftMaterial(org.bukkit.Material type) {
        if (type == null) return false;
        return type == org.bukkit.Material.SUSPICIOUS_SAND
                || type == org.bukkit.Material.SUSPICIOUS_GRAVEL
                || type == org.bukkit.Material.VAULT
                || type == org.bukkit.Material.TRIAL_SPAWNER;
    }

    /**
     * 檢查草稿物品狀態，若逾期且尚未標記過期，自動刷新為過期狀態提示
     */
    private void checkAndRefreshDraftItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return;
        }
        // 1. 材質快速過濾 (Fast-fail)：99.9% 的玩家背包物品非自訂方塊材質，直接跳過，完全不觸發任何 Meta / NBT 反序列化
        if (!isPotentialDraftMaterial(item.getType())) {
            return;
        }
        // 2. 原版底層無任何 tag/meta 時快速跳過，避免建立 CraftMetaItem 暫存物件
        if (!item.hasItemMeta()) {
            return;
        }
        if (!plugin.getItemManager().isDraftItem(item)) {
            return;
        }

        // 若已經被標註為過期外觀，無須重複渲染
        if (plugin.getItemManager().isDraftMarkedExpired(item)) {
            return;
        }

        boolean expired = plugin.getItemManager().isDraftExpired(item);
        if (!expired) {
            // 亦檢查對應草稿檔案是否已被清理或遺失 (純記憶體比對)
            String draftId = plugin.getItemManager().getDraftId(item);
            DraftType draftType = plugin.getItemManager().getDraftType(item);
            if (draftId != null && draftType != null && !plugin.getDraftManager().hasDraft(draftType, draftId)) {
                expired = true;
            }
        }

        if (expired) {
            plugin.getItemManager().markDraftExpired(item);
        }
    }
}
