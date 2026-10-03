package clre20.customLootX.model;

import clre20.customLootX.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class LootItem {

    private ItemStack item;
    private double chance;
    private boolean isAir;

    public LootItem(ItemStack item, double chance) {
        this.item = (item != null && item.getType() != Material.AIR) ? item.clone() : null;
        this.chance = TextUtil.roundChance(chance);
        this.isAir = (this.item == null);
    }

    public LootItem(double chance, boolean isAir) {
        this.item = null;
        this.chance = TextUtil.roundChance(chance);
        this.isAir = isAir;
    }

    public ItemStack getItem() {
        return item != null ? item.clone() : null;
    }

    public void setItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            this.item = null;
            this.isAir = true;
        } else {
            this.item = item.clone();
            this.isAir = false;
        }
    }

    public double getChance() {
        return chance;
    }

    public void setChance(double chance) {
        this.chance = TextUtil.roundChance(chance);
    }

    public boolean isAir() {
        return isAir || item == null || item.getType() == Material.AIR;
    }

    public void setAir(boolean air) {
        this.isAir = air;
        if (air) {
            this.item = null;
        }
    }

    public LootItem cloneItem() {
        if (isAir()) {
            return new LootItem(this.chance, true);
        }
        return new LootItem(this.item.clone(), this.chance);
    }
}
