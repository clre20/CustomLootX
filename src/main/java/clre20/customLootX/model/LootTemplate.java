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
    private int resetMinutes;
    private final List<LootItem> items = new ArrayList<>();

    public LootTemplate(String name, Material type) {
        this.name = name;
        this.type = (type == Material.SUSPICIOUS_GRAVEL) ? Material.SUSPICIOUS_GRAVEL : Material.SUSPICIOUS_SAND;
        this.displayName = (this.type == Material.SUSPICIOUS_SAND) ? "&e自訂可疑沙: " + name : "&7自訂可疑礫石: " + name;
        this.resetEnabled = false;
        this.resetMinutes = 5;
    }

    public LootTemplate(String name, Material type, String displayName, boolean resetEnabled, int resetMinutes, List<LootItem> items) {
        this.name = name;
        this.type = (type == Material.SUSPICIOUS_GRAVEL) ? Material.SUSPICIOUS_GRAVEL : Material.SUSPICIOUS_SAND;
        this.displayName = (displayName != null && !displayName.isEmpty()) ? displayName : (this.type == Material.SUSPICIOUS_SAND ? "&e自訂可疑沙: " + name : "&7自訂可疑礫石: " + name);
        this.resetEnabled = resetEnabled;
        this.resetMinutes = Math.max(1, resetMinutes);
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

    public int getResetMinutes() {
        return resetMinutes;
    }

    public void setResetMinutes(int resetMinutes) {
        this.resetMinutes = Math.max(1, resetMinutes);
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
     * Verify if total chance is exactly 100.00%
     */
    public boolean isValidTotal() {
        return Math.abs(getTotalChance() - 100.00) < 0.001;
    }

    /**
     * Roll a random loot item according to chances.
     * Returns null if rolled air or empty.
     */
    public ItemStack rollItem() {
        if (items.isEmpty()) {
            return null;
        }
        double roll = ThreadLocalRandom.current().nextDouble() * 100.0;
        double accumulated = 0.0;

        for (LootItem lootItem : items) {
            accumulated += lootItem.getChance();
            if (roll < accumulated) {
                if (lootItem.isAir()) {
                    return null;
                }
                return lootItem.getItem();
            }
        }

        // Fallback to last item in case of floating point edge case
        LootItem last = items.get(items.size() - 1);
        return last.isAir() ? null : last.getItem();
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

    public LootTemplate cloneTemplate() {
        return new LootTemplate(this.name, this.type, this.displayName, this.resetEnabled, this.resetMinutes, this.items);
    }
}
