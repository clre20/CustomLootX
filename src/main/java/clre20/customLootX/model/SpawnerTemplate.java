package clre20.customLootX.model;

import clre20.customLootX.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 試煉生怪磚配置模型
 */
public class SpawnerTemplate {

    private String name;
    private boolean ominous;
    private String displayName;
    private EntityType spawnedType;
    private boolean displayCycle = true;
    private String displayMobId = "CYCLE";
    private List<SpawnerMobEntry> mobPool;
    private int totalMobs;
    private int simultaneousMobs;
    private int spawnDelaySeconds;
    private int playerRange;
    private boolean showActionBar;
    private boolean victorySoundEnabled = false;
    private String victorySound = "UI_TOAST_CHALLENGE_COMPLETE";
    private float victorySoundVolume = 1.0f;
    private float victorySoundPitch = 1.2f;
    private VaultCooldownMode cooldownMode;
    private int cooldownMinutes;
    private int rollCount;
    private List<LootItem> rewards;

    public SpawnerTemplate(String name, boolean ominous, String displayName, EntityType spawnedType,
                           int totalMobs, int simultaneousMobs, int spawnDelaySeconds, int playerRange,
                           VaultCooldownMode cooldownMode, int cooldownMinutes, int rollCount,
                           List<LootItem> rewards) {
        this(name, ominous, displayName, spawnedType, null, totalMobs, simultaneousMobs,
                spawnDelaySeconds, playerRange, true, cooldownMode, cooldownMinutes, rollCount, rewards);
    }

    public SpawnerTemplate(String name, boolean ominous, String displayName, EntityType spawnedType,
                           List<SpawnerMobEntry> mobPool,
                           int totalMobs, int simultaneousMobs, int spawnDelaySeconds, int playerRange,
                           VaultCooldownMode cooldownMode, int cooldownMinutes, int rollCount,
                           List<LootItem> rewards) {
        this(name, ominous, displayName, spawnedType, mobPool, totalMobs, simultaneousMobs,
                spawnDelaySeconds, playerRange, true, cooldownMode, cooldownMinutes, rollCount, rewards);
    }

    public SpawnerTemplate(String name, boolean ominous, String displayName, EntityType spawnedType,
                           List<SpawnerMobEntry> mobPool,
                           int totalMobs, int simultaneousMobs, int spawnDelaySeconds, int playerRange,
                           boolean showActionBar,
                           VaultCooldownMode cooldownMode, int cooldownMinutes, int rollCount,
                           List<LootItem> rewards) {
        this(name, ominous, displayName, spawnedType, mobPool, totalMobs, simultaneousMobs,
                spawnDelaySeconds, playerRange, showActionBar, false, "UI_TOAST_CHALLENGE_COMPLETE", 1.0f, 1.2f,
                cooldownMode, cooldownMinutes, rollCount, rewards);
    }

    public SpawnerTemplate(String name, boolean ominous, String displayName, EntityType spawnedType,
                           List<SpawnerMobEntry> mobPool,
                           int totalMobs, int simultaneousMobs, int spawnDelaySeconds, int playerRange,
                           boolean showActionBar, boolean victorySoundEnabled, String victorySound,
                           float victorySoundVolume, float victorySoundPitch,
                           VaultCooldownMode cooldownMode, int cooldownMinutes, int rollCount,
                           List<LootItem> rewards) {
        this(name, ominous, displayName, spawnedType, true, "CYCLE", mobPool, totalMobs, simultaneousMobs,
                spawnDelaySeconds, playerRange, showActionBar, victorySoundEnabled, victorySound,
                victorySoundVolume, victorySoundPitch, cooldownMode, cooldownMinutes, rollCount, rewards);
    }

    public SpawnerTemplate(String name, boolean ominous, String displayName, EntityType spawnedType,
                           boolean displayCycle, String displayMobId,
                           List<SpawnerMobEntry> mobPool,
                           int totalMobs, int simultaneousMobs, int spawnDelaySeconds, int playerRange,
                           boolean showActionBar, boolean victorySoundEnabled, String victorySound,
                           float victorySoundVolume, float victorySoundPitch,
                           VaultCooldownMode cooldownMode, int cooldownMinutes, int rollCount,
                           List<LootItem> rewards) {
        this.name = name;
        this.ominous = ominous;
        this.displayName = (displayName == null || displayName.isEmpty()) ? name : displayName;
        this.displayCycle = displayCycle;
        this.displayMobId = displayMobId;
        this.mobPool = (mobPool == null) ? new ArrayList<>() : new ArrayList<>(mobPool);
        if (this.mobPool.isEmpty()) {
            this.mobPool.add(new SpawnerMobEntry(spawnedType != null ? spawnedType : EntityType.ZOMBIE, 100.0));
        }
        if (displayCycle) {
            this.spawnedType = this.mobPool.get(0).getPreviewEntityType();
        } else if (displayMobId != null) {
            EntityType resolved = null;
            for (SpawnerMobEntry entry : this.mobPool) {
                if (entry.getMobId().equalsIgnoreCase(displayMobId)) {
                    resolved = entry.getPreviewEntityType();
                    break;
                }
            }
            this.spawnedType = (resolved != null) ? resolved : (spawnedType == null ? this.mobPool.get(0).getPreviewEntityType() : spawnedType);
        } else {
            this.spawnedType = (spawnedType == null) ? this.mobPool.get(0).getPreviewEntityType() : spawnedType;
        }
        this.totalMobs = Math.max(1, totalMobs);
        this.simultaneousMobs = Math.max(1, Math.min(16, simultaneousMobs));
        this.spawnDelaySeconds = Math.max(1, spawnDelaySeconds);
        this.playerRange = Math.max(4, Math.min(48, playerRange));
        this.showActionBar = showActionBar;
        this.victorySoundEnabled = victorySoundEnabled;
        this.victorySound = (victorySound == null || victorySound.isEmpty()) ? "UI_TOAST_CHALLENGE_COMPLETE" : victorySound;
        this.victorySoundVolume = Math.max(0.1f, Math.min(2.0f, victorySoundVolume));
        this.victorySoundPitch = Math.max(0.5f, Math.min(2.0f, victorySoundPitch));
        this.cooldownMode = (cooldownMode == null) ? VaultCooldownMode.PLAYER_COOLDOWN : cooldownMode;
        this.cooldownMinutes = Math.max(1, cooldownMinutes);
        this.rollCount = Math.max(1, Math.min(16, rollCount));
        this.rewards = (rewards == null) ? new ArrayList<>() : new ArrayList<>(rewards);
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

    public boolean isDisplayCycle() {
        return displayCycle;
    }

    public void setDisplayCycle(boolean displayCycle) {
        this.displayCycle = displayCycle;
    }

    public String getDisplayMobId() {
        return displayMobId;
    }

    public void setDisplayMobId(String displayMobId) {
        this.displayMobId = displayMobId;
    }

    public EntityType getSpawnedType() {
        if (displayCycle) {
            if (mobPool != null && !mobPool.isEmpty()) {
                return mobPool.get(0).getPreviewEntityType();
            }
        } else if (displayMobId != null && mobPool != null) {
            for (SpawnerMobEntry entry : mobPool) {
                if (entry.getMobId().equalsIgnoreCase(displayMobId)) {
                    return entry.getPreviewEntityType();
                }
            }
        }
        return spawnedType == null ? EntityType.ZOMBIE : spawnedType;
    }

    public void setSpawnedType(EntityType spawnedType) {
        this.spawnedType = (spawnedType == null) ? EntityType.ZOMBIE : spawnedType;
    }

    public int getTotalMobs() {
        return Math.max(1, totalMobs);
    }

    public void setTotalMobs(int totalMobs) {
        this.totalMobs = Math.max(1, Math.min(64, totalMobs));
    }

    public int getSimultaneousMobs() {
        return Math.max(1, Math.min(16, simultaneousMobs));
    }

    public void setSimultaneousMobs(int simultaneousMobs) {
        this.simultaneousMobs = Math.max(1, Math.min(16, simultaneousMobs));
    }

    public int getSpawnDelaySeconds() {
        return Math.max(1, spawnDelaySeconds);
    }

    public void setSpawnDelaySeconds(int spawnDelaySeconds) {
        this.spawnDelaySeconds = Math.max(1, Math.min(60, spawnDelaySeconds));
    }

    public int getPlayerRange() {
        return Math.max(4, Math.min(48, playerRange));
    }

    public void setPlayerRange(int playerRange) {
        this.playerRange = Math.max(4, Math.min(48, playerRange));
    }

    public boolean isShowActionBar() {
        return showActionBar;
    }

    public void setShowActionBar(boolean showActionBar) {
        this.showActionBar = showActionBar;
    }

    public boolean isVictorySoundEnabled() {
        return victorySoundEnabled;
    }

    public void setVictorySoundEnabled(boolean victorySoundEnabled) {
        this.victorySoundEnabled = victorySoundEnabled;
    }

    public String getVictorySound() {
        return (victorySound == null || victorySound.isEmpty()) ? "UI_TOAST_CHALLENGE_COMPLETE" : victorySound;
    }

    public void setVictorySound(String victorySound) {
        this.victorySound = (victorySound == null || victorySound.isEmpty()) ? "UI_TOAST_CHALLENGE_COMPLETE" : victorySound;
    }

    public float getVictorySoundVolume() {
        return victorySoundVolume;
    }

    public void setVictorySoundVolume(float victorySoundVolume) {
        this.victorySoundVolume = Math.max(0.1f, Math.min(2.0f, victorySoundVolume));
    }

    public float getVictorySoundPitch() {
        return victorySoundPitch;
    }

    public void setVictorySoundPitch(float victorySoundPitch) {
        this.victorySoundPitch = Math.max(0.5f, Math.min(2.0f, victorySoundPitch));
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

    public int getRollCount() {
        return Math.max(1, Math.min(16, rollCount));
    }

    public void setRollCount(int rollCount) {
        this.rollCount = Math.max(1, Math.min(16, rollCount));
    }

    public List<SpawnerMobEntry> getMobPool() {
        if (mobPool == null) {
            mobPool = new ArrayList<>();
        }
        if (mobPool.isEmpty()) {
            mobPool.add(new SpawnerMobEntry(getSpawnedType(), 100.0));
        }
        return mobPool;
    }

    public void setMobPool(List<SpawnerMobEntry> mobPool) {
        this.mobPool = (mobPool == null) ? new ArrayList<>() : new ArrayList<>(mobPool);
        if (!this.mobPool.isEmpty()) {
            this.spawnedType = this.mobPool.get(0).getPreviewEntityType();
        }
    }

    public double getTotalMobChance() {
        double total = 0.0;
        for (SpawnerMobEntry entry : getMobPool()) {
            total += entry.getChance();
        }
        return TextUtil.roundChance(total);
    }

    public boolean isTotalMobChanceValid() {
        if (getMobPool().isEmpty()) return false;
        return Math.abs(getTotalMobChance() - 100.0) < 0.0001;
    }

    /**
     * 從 100% 怪物池中依機率抽取一隻怪物
     */
    public SpawnerMobEntry rollSingleMob() {
        List<SpawnerMobEntry> pool = getMobPool();
        if (pool.isEmpty()) {
            return new SpawnerMobEntry(getSpawnedType(), 100.0);
        }
        double random = ThreadLocalRandom.current().nextDouble() * 100.0;
        double cumulative = 0.0;
        for (SpawnerMobEntry entry : pool) {
            cumulative += entry.getChance();
            if (random < cumulative) {
                return entry;
            }
        }
        return pool.get(pool.size() - 1);
    }

    public List<LootItem> getRewards() {
        return rewards;
    }

    public void setRewards(List<LootItem> rewards) {
        this.rewards = (rewards == null) ? new ArrayList<>() : new ArrayList<>(rewards);
    }

    public double getTotalChance() {
        double total = 0.0;
        for (LootItem item : rewards) {
            total += item.getChance();
        }
        return TextUtil.roundChance(total);
    }

    public boolean isTotalChanceValid() {
        return Math.abs(getTotalChance() - 100.0) < 0.0001;
    }

    /**
     * 從 100% 獎勵池中隨機抽取一項物品（若落空則返回 null）
     */
    public ItemStack rollSingleItem() {
        if (rewards.isEmpty()) {
            return null;
        }

        double random = ThreadLocalRandom.current().nextDouble() * 100.0;
        double cumulative = 0.0;

        for (LootItem item : rewards) {
            cumulative += item.getChance();
            if (random < cumulative) {
                if (item.isAir() || item.getItem() == null) {
                    return null;
                }
                return item.getItem().clone();
            }
        }

        LootItem last = rewards.get(rewards.size() - 1);
        if (last.isAir() || last.getItem() == null) {
            return null;
        }
        return last.getItem().clone();
    }

    /**
     * 依據 rollCount 抽取指定數量的獲勝獎勵物品清單
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
     * 複製一份獨立的 SpawnerTemplate 複本
     */
    public SpawnerTemplate cloneTemplate() {
        List<LootItem> clonedRewards = new ArrayList<>();
        for (LootItem item : this.rewards) {
            clonedRewards.add(item.cloneItem());
        }
        List<SpawnerMobEntry> clonedMobs = new ArrayList<>();
        for (SpawnerMobEntry mob : getMobPool()) {
            clonedMobs.add(mob.cloneEntry());
        }
        return new SpawnerTemplate(
                this.name,
                this.ominous,
                this.displayName,
                this.spawnedType,
                this.displayCycle,
                this.displayMobId,
                clonedMobs,
                this.totalMobs,
                this.simultaneousMobs,
                this.spawnDelaySeconds,
                this.playerRange,
                this.showActionBar,
                this.victorySoundEnabled,
                this.victorySound,
                this.victorySoundVolume,
                this.victorySoundPitch,
                this.cooldownMode,
                this.cooldownMinutes,
                this.rollCount,
                clonedRewards
        );
    }
}
