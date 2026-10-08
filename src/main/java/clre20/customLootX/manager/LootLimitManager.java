package clre20.customLootX.manager;

import clre20.customLootX.CustomLootX;
import clre20.customLootX.model.LootItem;
import clre20.customLootX.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 出貨上限防火牆管理器 (Loot Limit Manager)
 * 負責追蹤與管理 3 種自訂獎品（可疑方塊、試煉寶庫、試煉生怪磚）的：
 * 1. 全服每日出貨上限
 * 2. 全服每月出貨上限
 * 3. 個人每日出貨上限 (每人每天)
 */
public class LootLimitManager {

    private final CustomLootX plugin;
    private final File dataFile;

    // date -> (fullKey -> serverCount)
    private final Map<String, Map<String, Integer>> dailyServerCounts = new ConcurrentHashMap<>();
    // yearMonth -> (fullKey -> serverCount)
    private final Map<String, Map<String, Integer>> monthlyServerCounts = new ConcurrentHashMap<>();
    // date -> (fullKey -> (playerUuid -> count))
    private final Map<String, Map<String, Map<UUID, Integer>>> dailyPlayerCounts = new ConcurrentHashMap<>();

    private boolean dirty = false;

    public LootLimitManager(CustomLootX plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data/loot_limits.yml");
        load();
        startAutoSaveTask();
    }

    private String getTodayKey() {
        return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private String getCurrentMonthKey() {
        return YearMonth.now().toString();
    }

    /**
     * 產生物品在特定模板中的唯一識別 Key
     */
    public String getItemKey(LootItem loot) {
        if (loot == null || loot.isAir() || loot.getItem() == null) {
            return "AIR";
        }
        org.bukkit.inventory.ItemStack item = loot.getItem();
        String typeName = item.getType().name();
        String name = TextUtil.getItemName(item);
        if (!name.equals(typeName) && !name.isEmpty() && !name.equals("[落空]")) {
            return typeName + ":" + name;
        }
        return typeName;
    }

    /**
     * 產生完整識別 Key: templateType:templateName:itemKey
     */
    public String getFullKey(String templateType, String templateName, LootItem loot) {
        String tType = (templateType != null) ? templateType.toLowerCase() : "unknown";
        String tName = (templateName != null) ? templateName : "unknown";
        return tType + ":" + tName + ":" + getItemKey(loot);
    }

    /**
     * 檢查指定獎勵是否已達到出貨上限
     * 若達到上限，返回 true（表示應被防火牆攔截轉為落空）
     */
    public boolean isLimitReached(String templateType, String templateName, LootItem loot, UUID playerUuid) {
        if (loot == null || !loot.hasAnyLimit() || loot.isAir()) {
            return false;
        }

        String fullKey = getFullKey(templateType, templateName, loot);
        String today = getTodayKey();
        String currentMonth = getCurrentMonthKey();

        // 1. 全服每日上限檢查
        if (loot.getLimitServerDaily() > 0) {
            int currentServerDaily = getServerDailyCount(today, fullKey);
            if (currentServerDaily >= loot.getLimitServerDaily()) {
                String itemName = (loot.getItem() != null) ? TextUtil.getItemName(loot.getItem()) : getItemKey(loot);
                plugin.logConsole("&6[出貨上限·攔截]&7 物品 &f" + itemName + " &7已達全服每日上限 &c(" + currentServerDaily + "/" + loot.getLimitServerDaily() + ")&7，已自抽獎池移除！");
                return true;
            }
        }

        // 2. 全服每月上限檢查
        if (loot.getLimitServerMonthly() > 0) {
            int currentServerMonthly = getServerMonthlyCount(currentMonth, fullKey);
            if (currentServerMonthly >= loot.getLimitServerMonthly()) {
                String itemName = (loot.getItem() != null) ? TextUtil.getItemName(loot.getItem()) : getItemKey(loot);
                plugin.logConsole("&6[出貨上限·攔截]&7 物品 &f" + itemName + " &7已達全服每月上限 &c(" + currentServerMonthly + "/" + loot.getLimitServerMonthly() + ")&7，已自抽獎池移除！");
                return true;
            }
        }

        // 3. 個人每日上限檢查 (每人每天)
        if (playerUuid != null && loot.getLimitPlayerDaily() > 0) {
            int currentPlayerDaily = getPlayerDailyCount(today, fullKey, playerUuid);
            if (currentPlayerDaily >= loot.getLimitPlayerDaily()) {
                String pName = playerUuid.toString();
                if (Bukkit.getPlayer(playerUuid) != null) {
                    pName = Bukkit.getPlayer(playerUuid).getName();
                } else {
                    org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(playerUuid);
                    if (op.getName() != null) pName = op.getName();
                }
                String itemName = (loot.getItem() != null) ? TextUtil.getItemName(loot.getItem()) : getItemKey(loot);
                plugin.logConsole("&6[出貨上限·攔截]&7 玩家 &f" + pName + " &7於 &e[" + templateType + ":" + templateName + "]&7 的物品 &f" + itemName +
                        " &7已達個人每日上限 &c(" + currentPlayerDaily + "/" + loot.getLimitPlayerDaily() + ")&7，已自抽獎池移除！");
                return true;
            }
        }

        return false;
    }

    /**
     * 記錄一次成功出貨
     */
    public void recordDrop(String templateType, String templateName, LootItem loot, UUID playerUuid) {
        if (loot == null || !loot.hasAnyLimit() || loot.isAir()) {
            return;
        }

        String fullKey = getFullKey(templateType, templateName, loot);
        String today = getTodayKey();
        String currentMonth = getCurrentMonthKey();

        // 累計全服日出貨量
        if (loot.getLimitServerDaily() > 0) {
            dailyServerCounts.computeIfAbsent(today, k -> new ConcurrentHashMap<>())
                    .merge(fullKey, 1, Integer::sum);
        }

        // 累計全服月出貨量
        if (loot.getLimitServerMonthly() > 0) {
            monthlyServerCounts.computeIfAbsent(currentMonth, k -> new ConcurrentHashMap<>())
                    .merge(fullKey, 1, Integer::sum);
        }

        // 累計個人日出貨量
        if (playerUuid != null && loot.getLimitPlayerDaily() > 0) {
            int newCount = dailyPlayerCounts.computeIfAbsent(today, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(fullKey, k -> new ConcurrentHashMap<>())
                    .merge(playerUuid, 1, Integer::sum);
            String pName = playerUuid.toString();
            if (Bukkit.getPlayer(playerUuid) != null) {
                pName = Bukkit.getPlayer(playerUuid).getName();
            } else {
                org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(playerUuid);
                if (op.getName() != null) pName = op.getName();
            }
            String itemName = (loot.getItem() != null) ? TextUtil.getItemName(loot.getItem()) : getItemKey(loot);
            plugin.logConsole("&a[出貨上限·計數]&7 玩家 &f" + pName + " &7於 &e[" + templateType + ":" + templateName + "]&7 獲得物品 &f" + itemName +
                    "&7，今日個人累計: &e" + newCount + "/" + loot.getLimitPlayerDaily());
        }

        this.dirty = true;
    }

    /**
     * 輸出出貨上限攔截日誌
     */
    public void logBlocked(UUID playerUuid, String templateType, String templateName, LootItem loot) {
        if (loot == null || loot.getItem() == null) return;
        String playerName = "未知玩家";
        if (playerUuid != null) {
            org.bukkit.entity.Player p = Bukkit.getPlayer(playerUuid);
            if (p != null) playerName = p.getName();
            else {
                org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(playerUuid);
                if (op.getName() != null) playerName = op.getName();
            }
        }
        String itemName = TextUtil.getItemName(loot.getItem());
        plugin.getConfigManager().log("limit-blocked",
                "%player%", playerName,
                "%name%", templateName != null ? templateName : "未知",
                "%item%", itemName,
                "%type%", templateType != null ? templateType : "方塊"
        );
    }

    public void logBlocked(String playerName, String templateType, String templateName, LootItem loot) {
        if (loot == null || loot.getItem() == null) return;
        String itemName = TextUtil.getItemName(loot.getItem());
        plugin.getConfigManager().log("limit-blocked",
                "%player%", (playerName != null ? playerName : "未知玩家"),
                "%name%", templateName != null ? templateName : "未知",
                "%item%", itemName,
                "%type%", templateType != null ? templateType : "方塊"
        );
    }

    public int getServerDailyCount(String today, String fullKey) {
        Map<String, Integer> dayMap = dailyServerCounts.get(today);
        return (dayMap != null) ? dayMap.getOrDefault(fullKey, 0) : 0;
    }

    public int getServerMonthlyCount(String currentMonth, String fullKey) {
        Map<String, Integer> monthMap = monthlyServerCounts.get(currentMonth);
        return (monthMap != null) ? monthMap.getOrDefault(fullKey, 0) : 0;
    }

    public int getPlayerDailyCount(String today, String fullKey, UUID playerUuid) {
        Map<String, Map<UUID, Integer>> dayMap = dailyPlayerCounts.get(today);
        if (dayMap == null) return 0;
        Map<UUID, Integer> pMap = dayMap.get(fullKey);
        return (pMap != null) ? pMap.getOrDefault(playerUuid, 0) : 0;
    }

    /**
     * 從磁碟讀取上限紀錄
     */
    public synchronized void load() {
        if (!dataFile.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);

        // 讀取日紀錄
        ConfigurationSection datesSec = yaml.getConfigurationSection("dates");
        if (datesSec != null) {
            for (String dateKey : datesSec.getKeys(false)) {
                ConfigurationSection daySec = datesSec.getConfigurationSection(dateKey);
                if (daySec == null) continue;

                Map<String, Integer> serverMap = new ConcurrentHashMap<>();
                Map<String, Map<UUID, Integer>> playerMap = new ConcurrentHashMap<>();

                // 優先讀取清單格式 (防 YAML dot 切割)
                if (daySec.isList("server-list")) {
                    List<Map<?, ?>> sList = daySec.getMapList("server-list");
                    for (Map<?, ?> entry : sList) {
                        String k = String.valueOf(entry.get("key"));
                        int c = (entry.get("count") instanceof Number n) ? n.intValue() : 0;
                        if (!k.isEmpty() && c > 0) {
                            serverMap.put(k, c);
                        }
                    }
                } else {
                    ConfigurationSection sSec = daySec.getConfigurationSection("server");
                    if (sSec != null) {
                        for (String key : sSec.getKeys(false)) {
                            serverMap.put(key, sSec.getInt(key, 0));
                        }
                    }
                }

                if (daySec.isList("players-list")) {
                    List<Map<?, ?>> pList = daySec.getMapList("players-list");
                    for (Map<?, ?> entry : pList) {
                        String itemKey = String.valueOf(entry.get("key"));
                        String uStr = String.valueOf(entry.get("player"));
                        int c = (entry.get("count") instanceof Number n) ? n.intValue() : 0;
                        if (!itemKey.isEmpty() && !uStr.isEmpty() && c > 0) {
                            try {
                                UUID uuid = UUID.fromString(uStr);
                                playerMap.computeIfAbsent(itemKey, k -> new ConcurrentHashMap<>()).put(uuid, c);
                            } catch (IllegalArgumentException ignored) {
                            }
                        }
                    }
                } else {
                    ConfigurationSection pSec = daySec.getConfigurationSection("players");
                    if (pSec != null) {
                        for (String key : pSec.getKeys(false)) {
                            ConfigurationSection itemSec = pSec.getConfigurationSection(key);
                            if (itemSec != null) {
                                Map<UUID, Integer> uMap = new ConcurrentHashMap<>();
                                for (String uStr : itemSec.getKeys(false)) {
                                    try {
                                        UUID uuid = UUID.fromString(uStr);
                                        uMap.put(uuid, itemSec.getInt(uStr, 0));
                                    } catch (IllegalArgumentException ignored) {
                                    }
                                }
                                playerMap.put(key, uMap);
                            }
                        }
                    }
                }

                dailyServerCounts.put(dateKey, serverMap);
                dailyPlayerCounts.put(dateKey, playerMap);
            }
        }

        // 讀取月紀錄
        ConfigurationSection monthsSec = yaml.getConfigurationSection("months");
        if (monthsSec != null) {
            for (String monthKey : monthsSec.getKeys(false)) {
                ConfigurationSection mSec = monthsSec.getConfigurationSection(monthKey);
                if (mSec == null) continue;
                Map<String, Integer> map = new ConcurrentHashMap<>();

                if (mSec.isList("server-list")) {
                    List<Map<?, ?>> sList = mSec.getMapList("server-list");
                    for (Map<?, ?> entry : sList) {
                        String k = String.valueOf(entry.get("key"));
                        int c = (entry.get("count") instanceof Number n) ? n.intValue() : 0;
                        if (!k.isEmpty() && c > 0) {
                            map.put(k, c);
                        }
                    }
                } else {
                    for (String key : mSec.getKeys(false)) {
                        map.put(key, mSec.getInt(key, 0));
                    }
                }
                monthlyServerCounts.put(monthKey, map);
            }
        }

        pruneOldRecords();
    }

    /**
     * 重設所有出貨上限紀錄 (清空所有計數)
     */
    public synchronized void resetAllLimits() {
        dailyServerCounts.clear();
        monthlyServerCounts.clear();
        dailyPlayerCounts.clear();
        this.dirty = true;
        save();
        plugin.logConsole("&a[出貨上限]&7 已重設清空所有伺服器與玩家的出貨上限紀錄。");
    }

    /**
     * 重設指定玩家的今日出貨紀錄
     */
    public synchronized void resetPlayerLimits(UUID uuid) {
        String today = getTodayKey();
        Map<String, Map<UUID, Integer>> dayMap = dailyPlayerCounts.get(today);
        if (dayMap != null) {
            for (Map<UUID, Integer> pMap : dayMap.values()) {
                pMap.remove(uuid);
            }
        }
        this.dirty = true;
        save();
    }

    public Map<String, Integer> getPlayerTodayCounts(UUID playerUuid) {
        Map<String, Integer> result = new java.util.LinkedHashMap<>();
        if (playerUuid == null) return result;
        String today = getTodayKey();
        Map<String, Map<UUID, Integer>> dayMap = dailyPlayerCounts.get(today);
        if (dayMap == null) return result;
        for (Map.Entry<String, Map<UUID, Integer>> entry : dayMap.entrySet()) {
            Integer count = entry.getValue().get(playerUuid);
            if (count != null && count > 0) {
                result.put(entry.getKey(), count);
            }
        }
        return result;
    }

    /**
     * 取得全伺服器今日所有玩家的出貨統計 (UUID -> (ItemKey -> Count))
     */
    public Map<UUID, Map<String, Integer>> getAllPlayersTodayCounts() {
        Map<UUID, Map<String, Integer>> result = new java.util.LinkedHashMap<>();
        String today = getTodayKey();
        Map<String, Map<UUID, Integer>> dayMap = dailyPlayerCounts.get(today);
        if (dayMap == null) return result;

        for (Map.Entry<String, Map<UUID, Integer>> itemEntry : dayMap.entrySet()) {
            String itemKey = itemEntry.getKey();
            for (Map.Entry<UUID, Integer> pEntry : itemEntry.getValue().entrySet()) {
                UUID u = pEntry.getKey();
                int count = pEntry.getValue();
                if (count > 0) {
                    result.computeIfAbsent(u, k -> new java.util.LinkedHashMap<>()).put(itemKey, count);
                }
            }
        }
        return result;
    }

    /**
     * 清理超過 35 天的過期歷史紀錄以保證檔案精簡
     */
    private void pruneOldRecords() {
        LocalDate cutoff = LocalDate.now().minusDays(35);
        dailyServerCounts.keySet().removeIf(dateStr -> {
            try {
                LocalDate d = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
                return d.isBefore(cutoff);
            } catch (Exception e) {
                return false;
            }
        });
        dailyPlayerCounts.keySet().removeIf(dateStr -> {
            try {
                LocalDate d = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
                return d.isBefore(cutoff);
            } catch (Exception e) {
                return false;
            }
        });

        YearMonth cutoffMonth = YearMonth.now().minusMonths(3);
        monthlyServerCounts.keySet().removeIf(mStr -> {
            try {
                YearMonth m = YearMonth.parse(mStr);
                return m.isBefore(cutoffMonth);
            } catch (Exception e) {
                return false;
            }
        });
    }

    /**
     * 儲存上限紀錄至磁碟
     */
    public synchronized void save() {
        if (!dirty && dataFile.exists()) {
            return;
        }

        if (!dataFile.getParentFile().exists()) {
            dataFile.getParentFile().mkdirs();
        }

        YamlConfiguration yaml = new YamlConfiguration();

        // 寫入日紀錄 (清單化儲存防 YAML dot 切割)
        for (Map.Entry<String, Map<String, Integer>> dEntry : dailyServerCounts.entrySet()) {
            String date = dEntry.getKey();
            List<Map<String, Object>> serverList = new ArrayList<>();
            for (Map.Entry<String, Integer> sEntry : dEntry.getValue().entrySet()) {
                if (sEntry.getValue() > 0) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("key", sEntry.getKey());
                    m.put("count", sEntry.getValue());
                    serverList.add(m);
                }
            }
            if (!serverList.isEmpty()) {
                yaml.set("dates." + date + ".server-list", serverList);
            }
        }

        for (Map.Entry<String, Map<String, Map<UUID, Integer>>> dEntry : dailyPlayerCounts.entrySet()) {
            String date = dEntry.getKey();
            List<Map<String, Object>> playerList = new ArrayList<>();
            for (Map.Entry<String, Map<UUID, Integer>> itemEntry : dEntry.getValue().entrySet()) {
                String itemKey = itemEntry.getKey();
                for (Map.Entry<UUID, Integer> pEntry : itemEntry.getValue().entrySet()) {
                    if (pEntry.getValue() > 0) {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("key", itemKey);
                        m.put("player", pEntry.getKey().toString());
                        m.put("count", pEntry.getValue());
                        playerList.add(m);
                    }
                }
            }
            if (!playerList.isEmpty()) {
                yaml.set("dates." + date + ".players-list", playerList);
            }
        }

        // 寫入月紀錄
        for (Map.Entry<String, Map<String, Integer>> mEntry : monthlyServerCounts.entrySet()) {
            String month = mEntry.getKey();
            List<Map<String, Object>> serverList = new ArrayList<>();
            for (Map.Entry<String, Integer> sEntry : mEntry.getValue().entrySet()) {
                if (sEntry.getValue() > 0) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("key", sEntry.getKey());
                    m.put("count", sEntry.getValue());
                    serverList.add(m);
                }
            }
            if (!serverList.isEmpty()) {
                yaml.set("months." + month + ".server-list", serverList);
            }
        }

        try {
            yaml.save(dataFile);
            this.dirty = false;
        } catch (IOException e) {
            plugin.logError("&c[出貨上限·儲存]&c 儲存出貨上限紀錄檔案失敗: " + e.getMessage());
        }
    }

    private void startAutoSaveTask() {
        // 每 30 秒異步巡檢儲存髒資料
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (dirty) {
                save();
            }
        }, 20L * 30L, 20L * 30L);
    }
}
