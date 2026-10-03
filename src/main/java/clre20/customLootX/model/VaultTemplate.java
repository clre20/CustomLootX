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
    private int cooldownMinutes;
    private List<LootItem> items;

    public VaultTemplate(String name, boolean ominous, String displayName, ItemStack keyItem,
                         int rollCount, VaultCooldownMode cooldownMode, int cooldownMinutes,
                         List<LootItem> items) {
        this.name = name;
        this.ominous = ominous;
        this.displayName = (displayName == null || displayName.isEmpty()) ? name : displayName;
        this.keyItem = (keyItem == null) ? new ItemStack(ominous ? Material.OMINOUS_TRIAL_KEY : Material.TRIAL_KEY) : keyItem.clone();
        this.rollCount = Math.max(1, rollCount);
        this.cooldownMode = (cooldownMode == null) ? VaultCooldownMode.PLAYER_COOLDOWN : cooldownMode;
        this.cooldownMinutes = Math.max(1, cooldownMinutes);
        this.items = (items == null) ? new ArrayList<>() : new ArrayList<>(items);
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

    public int getCooldownMinutes() {
        return Math.max(1, cooldownMinutes);
    }

    public void setCooldownMinutes(int cooldownMinutes) {
        this.cooldownMinutes = Math.max(1, cooldownMinutes);
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
        return Math.abs(getTotalChance() - 100.0) < 0.0001;
    }

    /**
     * 從 100% 掉落池中隨機抽取一項物品（若落空則返回 null）
     */
    public ItemStack rollSingleItem() {
        if (items.isEmpty()) {
            return null;
        }

        double random = ThreadLocalRandom.current().nextDouble() * 100.0;
        double cumulative = 0.0;

        for (LootItem item : items) {
            cumulative += item.getChance();
            if (random < cumulative) {
                if (item.isAir() || item.getItem() == null) {
                    return null;
                }
                return item.getItem().clone();
            }
        }

        LootItem last = items.get(items.size() - 1);
        if (last.isAir() || last.getItem() == null) {
            return null;
        }
        return last.getItem().clone();
    }

    /**
     * 依據 rollCount 抽取指定數量的物品清單（過濾掉落空者）
     */
    public List<ItemStack> rollAllItems() {
        List<ItemStack> rolled = new ArrayList<>();
        int count = getRollCount();
        for (int i = 0; i < count; i++) {
            ItemStack item = rollSingleItem();
            if (item != null && item.getType() != Material.AIR) {
                rolled.add(item);
            }
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
                this.cooldownMinutes,
                clonedItems
        );
    }
}
