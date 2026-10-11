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
    private SpawnerSpawnMode spawnMode = SpawnerSpawnMode.RANDOM;
    private List<String> spawnSequence = new ArrayList<>();
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
    private int cooldownSeconds;
    private int rollCount;
    private String broadcastMessage;
    private List<LootItem> rewards;

    public SpawnerTemplate(String name, boolean ominous, String displayName, EntityType spawnedType,
                           int totalMobs, int simultaneousMobs, int spawnDelaySeconds, int playerRange,
                           VaultCooldownMode cooldownMode, int cooldownMinutes, int rollCount,
                           List<LootItem> rewards) {
        this(name, ominous, displayName, spawnedType, (List<SpawnerMobEntry>) null, totalMobs, simultaneousMobs,
                spawnDelaySeconds, playerRange, cooldownMode, cooldownMinutes, rollCount, rewards);
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

    private List<List<String>> waves = new ArrayList<>();
    private boolean waitWaveCleared = true;

    public SpawnerTemplate(String name, boolean ominous, String displayName, EntityType spawnedType,
                           boolean displayCycle, String displayMobId,
                           List<SpawnerMobEntry> mobPool,
                           int totalMobs, int simultaneousMobs, int spawnDelaySeconds, int playerRange,
                           boolean showActionBar, boolean victorySoundEnabled, String victorySound,
                           float victorySoundVolume, float victorySoundPitch,
                           VaultCooldownMode cooldownMode, int cooldownMinutes, int rollCount,
                           List<LootItem> rewards) {
        this(name, ominous, displayName, spawnedType, displayCycle, displayMobId, mobPool,
                SpawnerSpawnMode.SEQUENCE, (List<List<String>>) null,
                spawnDelaySeconds, playerRange, true,
                showActionBar, victorySoundEnabled, victorySound, victorySoundVolume, victorySoundPitch,
                cooldownMode, cooldownMinutes, rollCount, rewards);
    }

    public SpawnerTemplate(String name, boolean ominous, String displayName, EntityType spawnedType,
                           boolean displayCycle, String displayMobId,
                           List<SpawnerMobEntry> mobPool,
                           SpawnerSpawnMode spawnMode, List<String> spawnSequence,
                           int totalMobs, int simultaneousMobs, int spawnDelaySeconds, int playerRange,
                           boolean showActionBar, boolean victorySoundEnabled, String victorySound,
                           float victorySoundVolume, float victorySoundPitch,
                           VaultCooldownMode cooldownMode, int cooldownMinutes, int rollCount,
                           List<LootItem> rewards) {
        this(name, ominous, displayName, spawnedType, displayCycle, displayMobId, mobPool,
                spawnMode, convertSequenceToWaves(spawnSequence, simultaneousMobs, mobPool),
                spawnDelaySeconds, playerRange, true,
                showActionBar, victorySoundEnabled, victorySound, victorySoundVolume, victorySoundPitch,
                cooldownMode, cooldownMinutes, rollCount, rewards);
    }

    public SpawnerTemplate(String name, boolean ominous, String displayName, EntityType spawnedType,
                           boolean displayCycle, String displayMobId,
                           List<SpawnerMobEntry> mobPool,
                           SpawnerSpawnMode spawnMode, List<List<String>> waves,
                           int spawnDelaySeconds, int playerRange,
                           boolean showActionBar, boolean victorySoundEnabled, String victorySound,
                           float victorySoundVolume, float victorySoundPitch,
                           VaultCooldownMode cooldownMode, int cooldownMinutes, int rollCount,
                           List<LootItem> rewards) {
        this(name, ominous, displayName, spawnedType, displayCycle, displayMobId, mobPool,
                spawnMode, waves, spawnDelaySeconds, playerRange, true,
                showActionBar, victorySoundEnabled, victorySound, victorySoundVolume, victorySoundPitch,
                cooldownMode, cooldownMinutes, rollCount, rewards);
    }

    public SpawnerTemplate(String name, boolean ominous, String displayName, EntityType spawnedType,
                           boolean displayCycle, String displayMobId,
                           List<SpawnerMobEntry> mobPool,
                           SpawnerSpawnMode spawnMode, List<List<String>> waves,
                           int spawnDelaySeconds, int playerRange, boolean waitWaveCleared,
                           boolean showActionBar, boolean victorySoundEnabled, String victorySound,
                           float victorySoundVolume, float victorySoundPitch,
                           VaultCooldownMode cooldownMode, int cooldownMinutes, int rollCount,
                           List<LootItem> rewards) {
        this(name, ominous, displayName, spawnedType, displayCycle, displayMobId, mobPool,
                spawnMode, waves, spawnDelaySeconds, playerRange, waitWaveCleared,
                showActionBar, victorySoundEnabled, victorySound, victorySoundVolume, victorySoundPitch,
                cooldownMode, cooldownMinutes, rollCount, rewards, null);
    }

    public SpawnerTemplate(String name, boolean ominous, String displayName, EntityType spawnedType,
                           boolean displayCycle, String displayMobId,
                           List<SpawnerMobEntry> mobPool,
                           SpawnerSpawnMode spawnMode, List<List<String>> waves,
                           int spawnDelaySeconds, int playerRange, boolean waitWaveCleared,
                           boolean showActionBar, boolean victorySoundEnabled, String victorySound,
                           float victorySoundVolume, float victorySoundPitch,
                           VaultCooldownMode cooldownMode, int cooldownMinutes, int rollCount,
                           List<LootItem> rewards, String broadcastMessage) {
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
        this.spawnMode = (spawnMode == null) ? SpawnerSpawnMode.SEQUENCE : spawnMode;
        this.waves = (waves == null) ? new ArrayList<>() : new ArrayList<>(waves);
        if (this.waves.isEmpty()) {
            List<String> w1 = new ArrayList<>();
            w1.add(this.mobPool.get(0).getMobId());
            this.waves.add(w1);
        }
        this.spawnDelaySeconds = Math.max(1, spawnDelaySeconds);
        this.playerRange = Math.max(4, Math.min(48, playerRange));
        this.waitWaveCleared = waitWaveCleared;
        this.showActionBar = showActionBar;
        this.victorySoundEnabled = victorySoundEnabled;
        this.victorySound = (victorySound == null || victorySound.isEmpty()) ? "UI_TOAST_CHALLENGE_COMPLETE" : victorySound;
        this.victorySoundVolume = Math.max(0.1f, Math.min(2.0f, victorySoundVolume));
        this.victorySoundPitch = Math.max(0.5f, Math.min(2.0f, victorySoundPitch));
        this.cooldownMode = (cooldownMode == null) ? VaultCooldownMode.PLAYER_COOLDOWN : cooldownMode;
        this.cooldownSeconds = Math.max(1, cooldownMinutes);
        this.rollCount = Math.max(1, Math.min(16, rollCount));
        this.rewards = (rewards == null) ? new ArrayList<>() : new ArrayList<>(rewards);
        this.broadcastMessage = broadcastMessage;
    }

    private static List<List<String>> convertSequenceToWaves(List<String> sequence, int sim, List<SpawnerMobEntry> pool) {
        List<List<String>> res = new ArrayList<>();
        if (sequence != null && !sequence.isEmpty()) {
            int batchSize = Math.max(1, sim);
            List<String> cur = new ArrayList<>();
            for (String s : sequence) {
                cur.add(s);
                if (cur.size() >= batchSize) {
                    res.add(new ArrayList<>(cur));
                    cur.clear();
                }
            }
            if (!cur.isEmpty()) {
                res.add(cur);
            }
        } else {
            List<String> w1 = new ArrayList<>();
            if (pool != null && !pool.isEmpty()) {
                w1.add(pool.get(0).getMobId());
            } else {
                w1.add("UNSET");
            }
            res.add(w1);
        }
        return res;
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
        if (waves == null || waves.isEmpty()) {
            return 1;
        }
        int sum = 0;
        for (List<String> wave : waves) {
            if (wave != null) {
                sum += wave.size();
            }
        }
        return Math.max(1, sum);
    }

    public void setTotalMobs(int totalMobs) {
        this.totalMobs = Math.max(1, totalMobs);
    }

    public int getSimultaneousMobs() {
        int maxWave = 1;
        if (waves != null) {
            for (List<String> w : waves) {
                if (w != null && w.size() > maxWave) {
                    maxWave = w.size();
                }
            }
        }
        return maxWave;
    }

    public void setSimultaneousMobs(int simultaneousMobs) {
        this.simultaneousMobs = Math.max(1, Math.min(16, simultaneousMobs));
    }

    public List<List<String>> getWaves() {
        if (waves == null) {
            waves = new ArrayList<>();
        }
        if (waves.isEmpty()) {
            List<String> w1 = new ArrayList<>();
            if (mobPool != null && !mobPool.isEmpty()) {
                w1.add(mobPool.get(0).getMobId());
            } else {
                w1.add("UNSET");
            }
            waves.add(w1);
        }
        return waves;
    }

    public void setWaves(List<List<String>> waves) {
        this.waves = (waves == null) ? new ArrayList<>() : new ArrayList<>(waves);
        if (this.waves.isEmpty()) {
            List<String> w1 = new ArrayList<>();
            if (mobPool != null && !mobPool.isEmpty()) {
                w1.add(mobPool.get(0).getMobId());
            } else {
                w1.add("UNSET");
            }
            this.waves.add(w1);
        }
    }

    public boolean isWaitWaveCleared() {
        return waitWaveCleared;
    }

    public void setWaitWaveCleared(boolean waitWaveCleared) {
        this.waitWaveCleared = waitWaveCleared;
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

    public SpawnerSpawnMode getSpawnMode() {
        return spawnMode == null ? SpawnerSpawnMode.RANDOM : spawnMode;
    }

    public void setSpawnMode(SpawnerSpawnMode spawnMode) {
        this.spawnMode = (spawnMode == null) ? SpawnerSpawnMode.RANDOM : spawnMode;
    }

    public List<String> getSpawnSequence() {
        List<String> seq = new ArrayList<>();
        if (waves != null) {
            for (List<String> w : waves) {
                if (w != null) {
                    seq.addAll(w);
                }
            }
        }
        return seq;
    }

    public void setSpawnSequence(List<String> spawnSequence) {
        this.spawnSequence = (spawnSequence == null) ? new ArrayList<>() : new ArrayList<>(spawnSequence);
    }

    public void syncSequenceLength() {
        // No-op for wave-based structure
    }

    public boolean hasRandomInSequence() {
        if (waves != null) {
            for (List<String> w : waves) {
                if (w != null) {
                    for (String id : w) {
                        if (id == null || "RANDOM".equalsIgnoreCase(id.trim()) || "UNSET".equalsIgnoreCase(id.trim())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean hasUnsetInWaves() {
        if (waves == null || waves.isEmpty()) return true;
        for (List<String> wave : waves) {
            if (wave == null || wave.isEmpty()) return true;
            for (String mob : wave) {
                if (mob == null || mob.trim().isEmpty() || "UNSET".equalsIgnoreCase(mob.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isTotalMobChanceValid() {
        if (!hasRandomInSequence()) {
            // 自訂模式且全部指定固定怪物，完全免設機率！
            return true;
        }
        if (getMobPool().isEmpty()) return false;
        return Math.abs(getTotalMobChance() - 100.0) < 0.00001;
    }

    /**
     * 依波次與索引取得怪物項目
     */
    public SpawnerMobEntry getMobEntryForWave(int waveIndex, int mobIndex) {
        if (waves == null || waves.isEmpty()) {
            return rollSingleMob();
        }
        if (waveIndex < 0 || waveIndex >= waves.size()) {
            return rollSingleMob();
        }
        List<String> wave = waves.get(waveIndex);
        if (wave == null || mobIndex < 0 || mobIndex >= wave.size()) {
            return rollSingleMob();
        }
        String targetId = wave.get(mobIndex);
        if (targetId == null || "RANDOM".equalsIgnoreCase(targetId.trim()) || "UNSET".equalsIgnoreCase(targetId.trim())) {
            return rollSingleMob();
        }
        for (SpawnerMobEntry entry : getMobPool()) {
            if (entry.getMobId().equalsIgnoreCase(targetId)) {
                return entry;
            }
            if (entry.isMythic() && ("mm:" + entry.getMobId()).equalsIgnoreCase(targetId)) {
                return entry;
            }
        }
        try {
            if (targetId.toLowerCase().startsWith("mm:") || targetId.toLowerCase().startsWith("mythic:")) {
                String mmId = targetId.substring(targetId.indexOf(':') + 1);
                return new SpawnerMobEntry(true, mmId, mmId, 0.0);
            }
            EntityType et = EntityType.valueOf(targetId.toUpperCase());
            return new SpawnerMobEntry(et, 0.0);
        } catch (Exception ignored) {}
        return rollSingleMob();
    }

    /**
     * 依序號取得怪物項目（支援自訂順序模式與隨機抽取）
     */
    public SpawnerMobEntry getMobEntryForSequenceIndex(int index) {
        List<String> seq = getSpawnSequence();
        if (seq.isEmpty()) {
            return rollSingleMob();
        }
        int safeIndex = Math.max(0, Math.min(index, seq.size() - 1));
        String targetId = seq.get(safeIndex);
        if (targetId == null || "RANDOM".equalsIgnoreCase(targetId.trim()) || "UNSET".equalsIgnoreCase(targetId.trim())) {
            return rollSingleMob();
        }

        // 比對怪物池中的項目
        for (SpawnerMobEntry entry : getMobPool()) {
            if (entry.getMobId().equalsIgnoreCase(targetId)) {
                return entry;
            }
            if (entry.isMythic() && ("mm:" + entry.getMobId()).equalsIgnoreCase(targetId)) {
                return entry;
            }
        }

        // 若不在池中但為合法 ID，嘗試構建
        try {
            if (targetId.toLowerCase().startsWith("mm:") || targetId.toLowerCase().startsWith("mythic:")) {
                String mmId = targetId.substring(targetId.indexOf(':') + 1);
                return new SpawnerMobEntry(true, mmId, mmId, 0.0);
            }
            EntityType et = EntityType.valueOf(targetId.toUpperCase());
            return new SpawnerMobEntry(et, 0.0);
        } catch (Exception ignored) {}

        return rollSingleMob();
    }

    /**
     * 從 100% 怪物池中依機率抽取一隻怪物
     */
    public SpawnerMobEntry rollSingleMob() {
        List<SpawnerMobEntry> pool = getMobPool();
        if (pool.isEmpty()) {
            return new SpawnerMobEntry(getSpawnedType(), 100.0);
        }
        // 隨機擲骰取 5 位小數 (對齊 1.00000 ~ 100.00000 精度)
        double random = TextUtil.rollRandomChance(100.0);
        double cumulative = 0.0;
        for (SpawnerMobEntry entry : pool) {
            cumulative += entry.getChance();
            if (random <= cumulative) {
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
        return Math.abs(getTotalChance() - 100.0) < 0.00001;
    }

    public String getBroadcastMessage() {
        return broadcastMessage;
    }

    public void setBroadcastMessage(String broadcastMessage) {
        this.broadcastMessage = broadcastMessage;
    }

    /**
     * 從 100% 獎勵池中隨機抽取一項 LootItem
     */
    public LootItem rollSingleLoot() {
        return rollSingleLoot(null, null);
    }

    /**
     * 從獎勵池中隨機抽取一項 LootItem（排除已達上限選項，照樣抽獎）
     */
    public LootItem rollSingleLoot(clre20.customLootX.CustomLootX plugin, java.util.UUID playerUuid) {
        if (rewards.isEmpty()) {
            return null;
        }

        // 過濾掉已達到出貨上限的選項
        List<LootItem> available = new ArrayList<>();
        double totalWeight = 0.0;
        for (LootItem item : rewards) {
            if (!item.isAir() && item.getItem() != null && plugin != null) {
                if (plugin.getLootLimitManager().isLimitReached("spawner", this.name, item, playerUuid)) {
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
     * 依據 rollCount 抽取指定數量的獲勝獎勵物品物件清單（過濾掉落空者）
     */
    public List<LootItem> rollAllLootItems() {
        return rollAllLootItems(null, null);
    }

    /**
     * 依據 rollCount 抽取指定數量的獲勝獎勵物品物件清單（支援出貨上限防火牆攔截並照樣抽獎）
     */
    public List<LootItem> rollAllLootItems(clre20.customLootX.CustomLootX plugin, java.util.UUID playerUuid) {
        List<LootItem> rolled = new ArrayList<>();
        int count = getRollCount();
        for (int i = 0; i < count; i++) {
            LootItem loot = rollSingleLoot(plugin, playerUuid);
            if (loot != null && !loot.isAir() && loot.getItem() != null) {
                rolled.add(loot);
                if (plugin != null) {
                    plugin.getLootLimitManager().recordDrop("spawner", this.name, loot, playerUuid);
                }
            }
        }
        return rolled;
    }

    /**
     * 從 100% 獎勵池中隨機抽取一項物品（若落空則返回 null）
     */
    public ItemStack rollSingleItem() {
        LootItem loot = rollSingleLoot();
        if (loot == null || loot.isAir() || loot.getItem() == null) {
            return null;
        }
        return loot.getItem().clone();
    }

    /**
     * 依據 rollCount 抽取指定數量的獲勝獎勵物品清單
     */
    public List<ItemStack> rollAllItems() {
        List<ItemStack> rolled = new ArrayList<>();
        for (LootItem loot : rollAllLootItems()) {
            rolled.add(loot.getItem().clone());
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
        List<List<String>> clonedWaves = new ArrayList<>();
        if (this.waves != null) {
            for (List<String> w : this.waves) {
                clonedWaves.add(new ArrayList<>(w));
            }
        }
        return new SpawnerTemplate(
                this.name,
                this.ominous,
                this.displayName,
                this.spawnedType,
                this.displayCycle,
                this.displayMobId,
                clonedMobs,
                this.spawnMode,
                clonedWaves,
                this.spawnDelaySeconds,
                this.playerRange,
                this.waitWaveCleared,
                this.showActionBar,
                this.victorySoundEnabled,
                this.victorySound,
                this.victorySoundVolume,
                this.victorySoundPitch,
                this.cooldownMode,
                this.cooldownSeconds,
                this.rollCount,
                clonedRewards,
                this.broadcastMessage
        );
    }
}
