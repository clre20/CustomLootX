package clre20.customLootX.model;

import clre20.customLootX.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class LootItem {

    private ItemStack item;
    private double chance;
    private boolean isAir;
    private boolean broadcast = false;
    private String broadcastMessage = null;
    private int limitServerDaily = 0;
    private int limitServerMonthly = 0;
    private int limitPlayerDaily = 0;
    private int limitPlayerTotal = 0;

    public LootItem(ItemStack item, double chance) {
        this(item, chance, false, null);
    }

    public LootItem(ItemStack item, double chance, boolean broadcast) {
        this(item, chance, broadcast, null);
    }

    public LootItem(ItemStack item, double chance, boolean broadcast, String broadcastMessage) {
        this(item, chance, broadcast, broadcastMessage, 0, 0, 0, 0);
    }

    public LootItem(ItemStack item, double chance, boolean broadcast, String broadcastMessage, int limitServerDaily, int limitServerMonthly, int limitPlayerDaily) {
        this(item, chance, broadcast, broadcastMessage, limitServerDaily, limitServerMonthly, limitPlayerDaily, 0);
    }

    public LootItem(ItemStack item, double chance, boolean broadcast, String broadcastMessage, int limitServerDaily, int limitServerMonthly, int limitPlayerDaily, int limitPlayerTotal) {
        this.item = (item != null && item.getType() != Material.AIR) ? item.clone() : null;
        this.chance = TextUtil.roundChance(chance);
        this.isAir = (this.item == null);
        this.broadcast = broadcast;
        this.broadcastMessage = broadcastMessage;
        this.limitServerDaily = Math.max(0, limitServerDaily);
        this.limitServerMonthly = Math.max(0, limitServerMonthly);
        this.limitPlayerDaily = Math.max(0, limitPlayerDaily);
        this.limitPlayerTotal = Math.max(0, limitPlayerTotal);
    }

    public LootItem(double chance, boolean isAir) {
        this(chance, isAir, false, null);
    }

    public LootItem(double chance, boolean isAir, boolean broadcast) {
        this(chance, isAir, broadcast, null);
    }

    public LootItem(double chance, boolean isAir, boolean broadcast, String broadcastMessage) {
        this.item = null;
        this.chance = TextUtil.roundChance(chance);
        this.isAir = isAir;
        this.broadcast = broadcast;
        this.broadcastMessage = broadcastMessage;
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

    public boolean isBroadcast() {
        return broadcast;
    }

    public void setBroadcast(boolean broadcast) {
        this.broadcast = broadcast;
    }

    public String getBroadcastMessage() {
        return broadcastMessage;
    }

    public void setBroadcastMessage(String broadcastMessage) {
        this.broadcastMessage = broadcastMessage;
    }

    public int getLimitServerDaily() {
        return limitServerDaily;
    }

    public void setLimitServerDaily(int limitServerDaily) {
        this.limitServerDaily = Math.max(0, limitServerDaily);
    }

    public int getLimitServerMonthly() {
        return limitServerMonthly;
    }

    public void setLimitServerMonthly(int limitServerMonthly) {
        this.limitServerMonthly = Math.max(0, limitServerMonthly);
    }

    public int getLimitPlayerDaily() {
        return limitPlayerDaily;
    }

    public void setLimitPlayerDaily(int limitPlayerDaily) {
        this.limitPlayerDaily = Math.max(0, limitPlayerDaily);
    }

    public int getLimitPlayerTotal() {
        return limitPlayerTotal;
    }

    public void setLimitPlayerTotal(int limitPlayerTotal) {
        this.limitPlayerTotal = Math.max(0, limitPlayerTotal);
    }

    public boolean hasAnyLimit() {
        return limitServerDaily > 0 || limitServerMonthly > 0 || limitPlayerDaily > 0 || limitPlayerTotal > 0;
    }

    public LootItem cloneItem() {
        LootItem cloned;
        if (isAir()) {
            cloned = new LootItem(this.chance, true, this.broadcast, this.broadcastMessage);
        } else {
            cloned = new LootItem(this.item.clone(), this.chance, this.broadcast, this.broadcastMessage);
        }
        cloned.setLimitServerDaily(this.limitServerDaily);
        cloned.setLimitServerMonthly(this.limitServerMonthly);
        cloned.setLimitPlayerDaily(this.limitPlayerDaily);
        cloned.setLimitPlayerTotal(this.limitPlayerTotal);
        return cloned;
    }
}
