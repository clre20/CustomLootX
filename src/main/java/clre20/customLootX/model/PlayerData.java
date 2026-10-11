package clre20.customLootX.model;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家專屬資料模型 (對應 /playerdata/[uuid].yml)
 * 記錄該玩家的：
 * 1. 寶庫/生怪磚是否開過、完成記錄 (終生一次 PLAYER_ONCE)
 * 2. 數量計時 (冷卻到期時間戳)
 * 3. 獎品個人上限 (終生累計出貨次數)
 * 4. 獎品個人每日上限 (每日出貨次數)
 */
public class PlayerData {

    private final UUID uuid;
    private String name;
    private long lastSeen;

    // 寶庫開啟記錄 (方塊座標 key -> 記錄)
    private final Set<String> openedVaults = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> vaultCooldowns = new ConcurrentHashMap<>();

    // 生怪磚完成記錄 (方塊座標 key -> 記錄)
    private final Set<String> completedSpawners = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> spawnerCooldowns = new ConcurrentHashMap<>();

    // 出貨統計：個人總出貨上限累計 (fullKey -> 總獲取次數)
    private final Map<String, Integer> lootTotalCounts = new ConcurrentHashMap<>();

    // 出貨統計：個人每日出貨上限累計 (dateStr -> (fullKey -> 今日獲取次數))
    private final Map<String, Map<String, Integer>> lootDailyCounts = new ConcurrentHashMap<>();

    private volatile boolean dirty = false;

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name != null ? name : "Unknown";
        this.lastSeen = System.currentTimeMillis();
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name != null && !name.equals(this.name)) {
            this.name = name;
            this.dirty = true;
        }
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void updateLastSeen() {
        this.lastSeen = System.currentTimeMillis();
        this.dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    // ==========================================
    // 寶庫紀錄 (是否開過 / 冷卻計時)
    // ==========================================

    public boolean hasOpenedVault(String locKey) {
        return locKey != null && openedVaults.contains(locKey);
    }

    public void addOpenedVault(String locKey) {
        if (locKey != null && openedVaults.add(locKey)) {
            this.dirty = true;
        }
    }

    public void removeOpenedVault(String locKey) {
        if (locKey != null && openedVaults.remove(locKey)) {
            this.dirty = true;
        }
    }

    public Set<String> getOpenedVaults() {
        return Collections.unmodifiableSet(openedVaults);
    }

    public long getVaultCooldown(String locKey) {
        if (locKey == null) return 0L;
        Long expire = vaultCooldowns.get(locKey);
        return (expire != null) ? expire : 0L;
    }

    public void setVaultCooldown(String locKey, long expireTime) {
        if (locKey != null) {
            vaultCooldowns.put(locKey, expireTime);
            this.dirty = true;
        }
    }

    public void clearVaultCooldown(String locKey) {
        if (locKey != null && vaultCooldowns.remove(locKey) != null) {
            this.dirty = true;
        }
    }

    public Map<String, Long> getVaultCooldowns() {
        return Collections.unmodifiableMap(vaultCooldowns);
    }

    // ==========================================
    // 生怪磚紀錄 (是否完成 / 冷卻計時)
    // ==========================================

    public boolean hasCompletedSpawner(String locKey) {
        return locKey != null && completedSpawners.contains(locKey);
    }

    public void addCompletedSpawner(String locKey) {
        if (locKey != null && completedSpawners.add(locKey)) {
            this.dirty = true;
        }
    }

    public void removeCompletedSpawner(String locKey) {
        if (locKey != null && completedSpawners.remove(locKey)) {
            this.dirty = true;
        }
    }

    public Set<String> getCompletedSpawners() {
        return Collections.unmodifiableSet(completedSpawners);
    }

    public long getSpawnerCooldown(String locKey) {
        if (locKey == null) return 0L;
        Long expire = spawnerCooldowns.get(locKey);
        return (expire != null) ? expire : 0L;
    }

    public void setSpawnerCooldown(String locKey, long expireTime) {
        if (locKey != null) {
            spawnerCooldowns.put(locKey, expireTime);
            this.dirty = true;
        }
    }

    public void clearSpawnerCooldown(String locKey) {
        if (locKey != null && spawnerCooldowns.remove(locKey) != null) {
            this.dirty = true;
        }
    }

    public Map<String, Long> getSpawnerCooldowns() {
        return Collections.unmodifiableMap(spawnerCooldowns);
    }

    // ==========================================
    // 出貨上限紀錄 (個人總上限 & 個人單日上限)
    // ==========================================

    /**
     * 取得該物品在該玩家的終生累計出貨數量 (個人上限)
     */
    public int getLootTotalCount(String fullKey) {
        if (fullKey == null) return 0;
        return lootTotalCounts.getOrDefault(fullKey, 0);
    }

    /**
     * 累加一次個人總出貨量 (個人上限)
     */
    public int incrementLootTotalCount(String fullKey) {
        if (fullKey == null) return 0;
        int count = lootTotalCounts.merge(fullKey, 1, Integer::sum);
        this.dirty = true;
        return count;
    }

    public void setLootTotalCount(String fullKey, int count) {
        if (fullKey != null) {
            if (count <= 0) {
                lootTotalCounts.remove(fullKey);
            } else {
                lootTotalCounts.put(fullKey, count);
            }
            this.dirty = true;
        }
    }

    public Map<String, Integer> getLootTotalCounts() {
        return Collections.unmodifiableMap(lootTotalCounts);
    }

    /**
     * 取得該物品在該玩家當日的出貨數量 (個人單日上限)
     */
    public int getLootDailyCount(String date, String fullKey) {
        if (date == null || fullKey == null) return 0;
        Map<String, Integer> map = lootDailyCounts.get(date);
        return (map != null) ? map.getOrDefault(fullKey, 0) : 0;
    }

    /**
     * 累加一次個人當日出貨量 (個人單日上限)
     */
    public int incrementLootDailyCount(String date, String fullKey) {
        if (date == null || fullKey == null) return 0;
        int count = lootDailyCounts.computeIfAbsent(date, k -> new ConcurrentHashMap<>())
                .merge(fullKey, 1, Integer::sum);
        this.dirty = true;
        return count;
    }

    public void setLootDailyCount(String date, String fullKey, int count) {
        if (date != null && fullKey != null) {
            if (count <= 0) {
                Map<String, Integer> map = lootDailyCounts.get(date);
                if (map != null) {
                    map.remove(fullKey);
                }
            } else {
                lootDailyCounts.computeIfAbsent(date, k -> new ConcurrentHashMap<>()).put(fullKey, count);
            }
            this.dirty = true;
        }
    }

    public Map<String, Map<String, Integer>> getLootDailyCounts() {
        return Collections.unmodifiableMap(lootDailyCounts);
    }

    public void resetDailyLimits(String date) {
        if (date != null && lootDailyCounts.remove(date) != null) {
            this.dirty = true;
        }
    }

    /**
     * 重設所有日期的個人每日出貨量
     */
    public void resetAllDailyLimits() {
        if (!lootDailyCounts.isEmpty()) {
            lootDailyCounts.clear();
            this.dirty = true;
        }
    }

    /**
     * 依日期與關鍵字重設個人每日出貨量
     * 若 date 為 null，則重設所有日期的符合項目
     * @return 移除的項目數量
     */
    public int resetDailyLimit(String date, String itemKeyword) {
        int removed = 0;
        boolean hasKeyword = (itemKeyword != null && !itemKeyword.trim().isEmpty());
        String lower = hasKeyword ? itemKeyword.trim().toLowerCase() : "";

        if (date != null) {
            Map<String, Integer> map = lootDailyCounts.get(date);
            if (map != null) {
                if (!hasKeyword) {
                    removed = map.size();
                    lootDailyCounts.remove(date);
                } else {
                    Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
                    while (it.hasNext()) {
                        if (it.next().getKey().toLowerCase().contains(lower)) {
                            it.remove();
                            removed++;
                        }
                    }
                    if (map.isEmpty()) {
                        lootDailyCounts.remove(date);
                    }
                }
            }
        } else {
            Iterator<Map.Entry<String, Map<String, Integer>>> dateIt = lootDailyCounts.entrySet().iterator();
            while (dateIt.hasNext()) {
                Map.Entry<String, Map<String, Integer>> dateEntry = dateIt.next();
                Map<String, Integer> map = dateEntry.getValue();
                if (!hasKeyword) {
                    removed += map.size();
                    dateIt.remove();
                } else {
                    Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
                    while (it.hasNext()) {
                        if (it.next().getKey().toLowerCase().contains(lower)) {
                            it.remove();
                            removed++;
                        }
                    }
                    if (map.isEmpty()) {
                        dateIt.remove();
                    }
                }
            }
        }
        if (removed > 0) {
            this.dirty = true;
        }
        return removed;
    }

    /**
     * 重設個人總出貨量 (終生上限)
     */
    public void resetTotalLimits() {
        if (!lootTotalCounts.isEmpty()) {
            lootTotalCounts.clear();
            this.dirty = true;
        }
    }

    /**
     * 依關鍵字重設個人總出貨量 (終生上限)
     * @return 移除的項目數量
     */
    public int resetTotalLimit(String itemKeyword) {
        if (itemKeyword == null || itemKeyword.trim().isEmpty()) {
            int size = lootTotalCounts.size();
            resetTotalLimits();
            return size;
        }
        String lower = itemKeyword.trim().toLowerCase();
        int removed = 0;
        Iterator<Map.Entry<String, Integer>> it = lootTotalCounts.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getKey().toLowerCase().contains(lower)) {
                it.remove();
                removed++;
            }
        }
        if (removed > 0) {
            this.dirty = true;
        }
        return removed;
    }

    public void resetAllLimits() {
        lootTotalCounts.clear();
        lootDailyCounts.clear();
        this.dirty = true;
    }
}
