package clre20.customLootX.model;

import clre20.customLootX.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 試煉寶庫配置模型
 */
public class VaultTemplate {

    private String name;
    private boolean ominous;
    private String displayName;
    private ItemStack keyItem;
    private int rollCount;
    private VaultCooldownMode cooldownMode;
    private int cooldownSeconds;
    private String broadcastMessage;
    private List<LootItem> items;

    public VaultTemplate(String name, boolean ominous, String displayName, ItemStack keyItem,
                         int rollCount, VaultCooldownMode cooldownMode, int cooldownSeconds,
                         List<LootItem> items) {
        this(name, ominous, displayName, keyItem, rollCount, cooldownMode, cooldownSeconds, items, null);
    }

    public VaultTemplate(String name, boolean ominous, String displayName, ItemStack keyItem,
                         int rollCount, VaultCooldownMode cooldownMode, int cooldownSeconds,
                         List<LootItem> items, String broadcastMessage) {
        this.name = name;
        this.ominous = ominous;
        this.displayName = (displayName == null || displayName.isEmpty()) ? name : displayName;
        this.keyItem = (keyItem == null) ? new ItemStack(ominous ? Material.OMINOUS_TRIAL_KEY : Material.TRIAL_KEY) : keyItem.clone();
        this.rollCount = Math.max(1, rollCount);
        this.cooldownMode = (cooldownMode == null) ? VaultCooldownMode.PLAYER_COOLDOWN : cooldownMode;
        this.cooldownSeconds = Math.max(1, cooldownSeconds);
        this.items = (items == null) ? new ArrayList<>() : new ArrayList<>(items);
        this.broadcastMessage = broadcastMessage;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isOminous() {
        return ominous;
    }

    public void setOminous(boolean ominous) {
        this.ominous = ominous;
    }

    public String getDisplayName() {
        return (displayName == null || displayName.isEmpty()) ? name : displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public ItemStack getKeyItem() {
        if (keyItem == null) {
            keyItem = new ItemStack(ominous ? Material.OMINOUS_TRIAL_KEY : Material.TRIAL_KEY);
        }
        return keyItem.clone();
    }

    public void setKeyItem(ItemStack keyItem) {
        this.keyItem = (keyItem == null) ? new ItemStack(ominous ? Material.OMINOUS_TRIAL_KEY : Material.TRIAL_KEY) : keyItem.clone();
    }

    public int getRollCount() {
        return Math.max(1, rollCount);
    }

    public void setRollCount(int rollCount) {
        this.rollCount = Math.max(1, Math.min(64, rollCount));
    }

    public VaultCooldownMode getCooldownMode() {
        return cooldownMode == null ? VaultCooldownMode.PLAYER_COOLDOWN : cooldownMode;
    }

    public void setCooldownMode(VaultCooldownMode cooldownMode) {
        this.cooldownMode = cooldownMode;
    }

    public int getCooldownSeconds() {
        return Math.max(1, cooldownSeconds);
    }

    public void setCooldownSeconds(int cooldownSeconds) {
        this.cooldownSeconds = Math.max(1, cooldownSeconds);
    }

    public int getCooldownMinutes() {
        return Math.max(1, cooldownSeconds / 60);
    }

    public void setCooldownMinutes(int cooldownMinutes) {
        this.cooldownSeconds = Math.max(1, cooldownMinutes * 60);
    }

    public List<LootItem> getItems() {
        return items;
    }

    public void setItems(List<LootItem> items) {
        this.items = (items == null) ? new ArrayList<>() : new ArrayList<>(items);
    }

    public double getTotalChance() {
        double total = 0.0;
        for (LootItem item : items) {
            total += item.getChance();
        }
        return TextUtil.roundChance(total);
    }

    public boolean isTotalChanceValid() {
        return Math.abs(getTotalChance() - 100.0) < 0.00001;
    }

    public String getBroadcastMessage() {
        return broadcastMessage;
    }

    public void setBroadcastMessage(String broadcastMessage) {
        this.broadcastMessage = broadcastMessage;
    }

    /**
     * 從 100% 掉落池中隨機抽取一項 LootItem
     */
    public LootItem rollSingleLoot() {
        return rollSingleLoot(null, null);
    }

    /**
     * 從掉落池中隨機抽取一項 LootItem（排除已達上限選項，照樣抽獎）
     */
    public LootItem rollSingleLoot(clre20.customLootX.CustomLootX plugin, java.util.UUID playerUuid) {
        if (items.isEmpty()) {
            return null;
        }

        // 過濾掉已達到出貨上限的選項
        List<LootItem> available = new ArrayList<>();
        double totalWeight = 0.0;
        for (LootItem item : items) {
            if (!item.isAir() && item.getItem() != null && plugin != null) {
                if (plugin.getLootLimitManager().isLimitReached("vault", this.name, item, playerUuid)) {
                    continue; // 達到上限：沒有這個選項！
                }
            }
            available.add(item);
            totalWeight += item.getChance();
        }

        if (available.isEmpty() || totalWeight <= 0.0) {
            return null;
        }

        // 隨機擲骰取 5 位小數 (對齊 1.00000 ~ 100.00000 精度)
        double random = TextUtil.rollRandomChance(totalWeight);
        double cumulative = 0.0;

        for (LootItem item : available) {
            cumulative += item.getChance();
            if (random <= cumulative) {
                return item;
            }
        }

        return available.get(available.size() - 1);
    }

    /**
     * 依據 rollCount 抽取指定數量的物品物件清單（過濾掉落空者）
     */
    public List<LootItem> rollAllLootItems() {
        return rollAllLootItems(null, null);
    }

    /**
     * 依據 rollCount 抽取指定數量的物品物件清單（支援出貨上限防火牆攔截並照樣抽獎）
     */
    public List<LootItem> rollAllLootItems(clre20.customLootX.CustomLootX plugin, java.util.UUID playerUuid) {
        List<LootItem> rolled = new ArrayList<>();
        int count = getRollCount();
        for (int i = 0; i < count; i++) {
            LootItem loot = rollSingleLoot(plugin, playerUuid);
            if (loot != null && !loot.isAir() && loot.getItem() != null) {
                rolled.add(loot);
                if (plugin != null) {
                    plugin.getLootLimitManager().recordDrop("vault", this.name, loot, playerUuid);
                }
            }
        }
        return rolled;
    }

    /**
     * 從 100% 掉落池中隨機抽取一項物品（若落空則返回 null）
     */
    public ItemStack rollSingleItem() {
        LootItem loot = rollSingleLoot();
        if (loot == null || loot.isAir() || loot.getItem() == null) {
            return null;
        }
        return loot.getItem().clone();
    }

    /**
     * 依據 rollCount 抽取指定數量的物品清單（過濾掉落空者）
     */
    public List<ItemStack> rollAllItems() {
        List<ItemStack> rolled = new ArrayList<>();
        for (LootItem loot : rollAllLootItems()) {
            rolled.add(loot.getItem().clone());
        }
        return rolled;
    }

    /**
     * 複製一份獨立的 VaultTemplate 複本
     */
    public VaultTemplate cloneTemplate() {
        List<LootItem> clonedItems = new ArrayList<>();
        for (LootItem item : this.items) {
            clonedItems.add(item.cloneItem());
        }
        ItemStack clonedKey = (this.keyItem != null) ? this.keyItem.clone() : null;
        return new VaultTemplate(
                this.name,
                this.ominous,
                this.displayName,
                clonedKey,
                this.rollCount,
                this.cooldownMode,
                this.cooldownSeconds,
                clonedItems,
                this.broadcastMessage
        );
    }
}
