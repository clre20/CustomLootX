package clre20.customLootX.model;

import clre20.customLootX.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class LootTemplate {

    private String name;
    private Material type;
    private String displayName;
    private boolean resetEnabled;
    private int resetSeconds;
    private String broadcastMessage;
    private final List<LootItem> items = new ArrayList<>();

    public LootTemplate(String name, Material type) {
        this.name = name;
        this.type = (type == Material.SUSPICIOUS_GRAVEL) ? Material.SUSPICIOUS_GRAVEL : Material.SUSPICIOUS_SAND;
        this.displayName = (this.type == Material.SUSPICIOUS_SAND) ? "&e自訂可疑沙: " + name : "&7自訂可疑礫石: " + name;
        this.resetEnabled = false;
        this.resetSeconds = 300;
    }

    public LootTemplate(String name, Material type, String displayName, boolean resetEnabled, int resetSeconds, List<LootItem> items) {
        this(name, type, displayName, resetEnabled, resetSeconds, items, null);
    }

    public LootTemplate(String name, Material type, String displayName, boolean resetEnabled, int resetSeconds, List<LootItem> items, String broadcastMessage) {
        this.name = name;
        this.type = (type == Material.SUSPICIOUS_GRAVEL) ? Material.SUSPICIOUS_GRAVEL : Material.SUSPICIOUS_SAND;
        this.displayName = (displayName != null && !displayName.isEmpty()) ? displayName : (this.type == Material.SUSPICIOUS_SAND ? "&e自訂可疑沙: " + name : "&7自訂可疑礫石: " + name);
        this.resetEnabled = resetEnabled;
        this.resetSeconds = Math.max(1, resetSeconds);
        this.broadcastMessage = broadcastMessage;
        if (items != null) {
            for (LootItem item : items) {
                this.items.add(item.cloneItem());
            }
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Material getType() {
        return type;
    }

    public void setType(Material type) {
        this.type = (type == Material.SUSPICIOUS_GRAVEL) ? Material.SUSPICIOUS_GRAVEL : Material.SUSPICIOUS_SAND;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isResetEnabled() {
        return resetEnabled;
    }

    public void setResetEnabled(boolean resetEnabled) {
        this.resetEnabled = resetEnabled;
    }

    public int getResetSeconds() {
        return resetSeconds;
    }

    public void setResetSeconds(int resetSeconds) {
        this.resetSeconds = Math.max(1, resetSeconds);
    }

    public int getResetMinutes() {
        return Math.max(1, (int) Math.ceil((double) resetSeconds / 60.0));
    }

    public void setResetMinutes(int resetMinutes) {
        this.resetSeconds = Math.max(1, resetMinutes) * 60;
    }

    public List<LootItem> getItems() {
        return items;
    }

    public void addItem(LootItem item) {
        this.items.add(item);
    }

    public void removeItem(int index) {
        if (index >= 0 && index < items.size()) {
            items.remove(index);
        }
    }

    /**
     * Calculate sum of all items chance (percentage, 0.00 ~ 100.00)
     */
    public double getTotalChance() {
        double sum = 0.0;
        for (LootItem item : items) {
            sum += item.getChance();
        }
        return TextUtil.roundChance(sum);
    }

    /**
     * Verify if total chance is exactly 100.00000%
     */
    public boolean isValidTotal() {
        return Math.abs(getTotalChance() - 100.0) < 0.00001;
    }

    /**
     * Roll a random loot item according to chances.
     * Returns null if rolled air or empty.
     */
    public ItemStack rollItem() {
        return rollItem(null, null);
    }

    /**
     * 抽取隨機獎品物件，並過濾掉已達到出貨上限的選項（照樣抽獎）
     */
    public LootItem rollLoot(clre20.customLootX.CustomLootX plugin, java.util.UUID playerUuid) {
        if (items.isEmpty()) {
            return null;
        }

        // 過濾掉已達到出貨上限的選項
        List<LootItem> available = new ArrayList<>();
        double totalWeight = 0.0;
        for (LootItem lootItem : items) {
            if (!lootItem.isAir() && lootItem.getItem() != null && plugin != null) {
                if (plugin.getLootLimitManager().isLimitReached("suspicious", this.name, lootItem, playerUuid)) {
                    continue; // 達到上限：沒有這個選項！
                }
            }
            available.add(lootItem);
            totalWeight += lootItem.getChance();
        }

        if (available.isEmpty() || totalWeight <= 0.0) {
            return null;
        }

        // 隨機擲骰取 5 位小數 (對齊 1.00000 ~ 100.00000 精度)
        double roll = TextUtil.rollRandomChance(totalWeight);
        double accumulated = 0.0;

        for (LootItem lootItem : available) {
            accumulated += lootItem.getChance();
            if (roll <= accumulated) {
                return lootItem;
            }
        }

        return available.get(available.size() - 1);
    }

    public ItemStack rollItem(clre20.customLootX.CustomLootX plugin, java.util.UUID playerUuid) {
        LootItem loot = rollLoot(plugin, playerUuid);
        if (loot == null || loot.isAir() || loot.getItem() == null) {
            return null;
        }
        return loot.getItem().clone();
    }

    public double getItemChance(ItemStack target) {
        if (target == null || target.getType().isAir()) {
            for (LootItem li : items) {
                if (li.isAir()) return li.getChance();
            }
            return 0.0;
        }
        for (LootItem li : items) {
            if (!li.isAir() && li.getItem() != null && li.getItem().isSimilar(target)) {
                return li.getChance();
            }
        }
        return 0.0;
    }

    public String getBroadcastMessage() {
        return broadcastMessage;
    }

    public void setBroadcastMessage(String broadcastMessage) {
        this.broadcastMessage = broadcastMessage;
    }

    public LootItem findLootItem(ItemStack target) {
        if (target == null || target.getType().isAir()) {
            for (LootItem li : items) {
                if (li.isAir()) return li;
            }
            return null;
        }
        LootItem fallback = null;
        for (LootItem li : items) {
            if (!li.isAir() && li.getItem() != null && li.getItem().isSimilar(target)) {
                if (li.isBroadcast()) return li;
                if (fallback == null) fallback = li;
            }
        }
        return fallback;
    }

    public LootTemplate cloneTemplate() {
        return new LootTemplate(this.name, this.type, this.displayName, this.resetEnabled, this.resetSeconds, this.items, this.broadcastMessage);
    }
}
