package clre20.customLootX.manager;

import clre20.customLootX.CustomLootX;
import clre20.customLootX.model.PlayerData;
import clre20.customLootX.util.AsyncIO;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家資料管理器 (Player Data Manager)
 * 負責維護 /playerdata/[uuid].yml 獨立玩家設定檔。
 * <p>
 * 效能設計：
 * <ul>
 *   <li>所有讀寫透過單一 I/O 佇列 ({@link AsyncIO}) 依序執行：同一玩家的存檔不會並行覆寫，離線後立即重登也不會讀到舊檔。</li>
 *   <li>髒資料快照在主執行緒擷取 (資料只在主執行緒變動，快照一致)，YAML 序列化與磁碟寫入全部在背景。</li>
 *   <li>玩家離線後資料保留於快取直到存檔完成才釋放；指令查詢的離線玩家資料閒置 5 分鐘後自動回收。</li>
 *   <li>僅保留近期的每日出貨紀錄，避免個人檔案隨天數無限膨脹。</li>
 * </ul>
 */
public class PlayerDataManager implements Listener {

    private static final long IDLE_EVICT_MILLIS = 5L * 60L * 1000L;

    private final CustomLootX plugin;
    private final File playerdataFolder;
    private final Map<UUID, PlayerData> dataCache = new ConcurrentHashMap<>();
    // 最近一次被存取的時間 (用於回收離線玩家快取)
    private final Map<UUID, Long> lastAccess = new ConcurrentHashMap<>();

    public PlayerDataManager(CustomLootX plugin) {
        this.plugin = plugin;
        this.playerdataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!playerdataFolder.exists()) {
            playerdataFolder.mkdirs();
        }

        // 註冊玩家進出監聽器，即時預載與安全儲存釋放記憶體
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // 啟動定時存檔排程 (每 30 秒：主執行緒擷取髒資料快照 -> 背景佇列寫入)
        startAutoSaveTask();
    }

    /**
     * 取得玩家資料物件 (記憶體優先，未載入時從磁碟讀取或新建)
     */
    public PlayerData getPlayerData(UUID uuid) {
        if (uuid == null) return null;
        lastAccess.put(uuid, System.currentTimeMillis());
        PlayerData cached = dataCache.get(uuid);
        if (cached != null) return cached;
        return dataCache.computeIfAbsent(uuid, this::loadFromFile);
    }

    /**
     * 僅查詢快取，不觸發磁碟讀取
     */
    public PlayerData getCachedPlayerData(UUID uuid) {
        return uuid == null ? null : dataCache.get(uuid);
    }

    /**
     * 取得玩家名稱 (線上玩家 -> 快取資料 -> UUID)，不觸發 OfflinePlayer 查詢或磁碟 I/O
     */
    public String getPlayerName(UUID uuid) {
        if (uuid == null) return "未知玩家";
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) return p.getName();
        PlayerData data = dataCache.get(uuid);
        if (data != null && data.getName() != null && !"Unknown".equals(data.getName())) {
            return data.getName();
        }
        return uuid.toString();
    }

    /**
     * 標記該玩家資料有異動需要儲存
     */
    public void markDirty(UUID uuid) {
        PlayerData data = dataCache.get(uuid);
        if (data != null) {
            data.setDirty(true);
        }
    }

    /**
     * 從磁碟載入指定玩家的 YML 設定檔
     */
    public PlayerData loadFromFile(UUID uuid) {
        File file = new File(playerdataFolder, uuid.toString() + ".yml");
        String playerName = "Unknown";
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) {
            playerName = p.getName();
        }

        PlayerData data = new PlayerData(uuid, playerName);

        if (!file.exists()) {
            data.setDirty(true);
            return data;
        }

        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            String storedName = yaml.getString("name", playerName);
            data.setName(p != null ? playerName : storedName);

            // 讀取寶庫資料
            List<String> openedVaults = yaml.getStringList("vaults.opened");
            for (String v : openedVaults) {
                data.addOpenedVault(v);
            }
            long now = System.currentTimeMillis();
            ConfigurationSection vCdSec = yaml.getConfigurationSection("vaults.cooldowns");
            if (vCdSec != null) {
                for (String key : vCdSec.getKeys(false)) {
                    long expire = vCdSec.getLong(key, 0L);
                    if (expire > now) {
                        data.setVaultCooldown(key, expire);
                    }
                }
            }

            // 讀取生怪磚資料
            List<String> completedSpawners = yaml.getStringList("spawners.completed");
            for (String s : completedSpawners) {
                data.addCompletedSpawner(s);
            }
            ConfigurationSection sCdSec = yaml.getConfigurationSection("spawners.cooldowns");
            if (sCdSec != null) {
                for (String key : sCdSec.getKeys(false)) {
                    long expire = sCdSec.getLong(key, 0L);
                    if (expire > now) {
                        data.setSpawnerCooldown(key, expire);
                    }
                }
            }

            // 讀取出貨個人上限 (總量累計)
            ConfigurationSection limitTotalSec = yaml.getConfigurationSection("loot-limits.total");
            if (limitTotalSec != null) {
                for (String fullKey : limitTotalSec.getKeys(false)) {
                    int count = limitTotalSec.getInt(fullKey, 0);
                    if (count > 0) {
                        data.setLootTotalCount(fullKey, count);
                    }
                }
            }

            // 讀取出貨個人單日上限 (每日累計，只保留近期日期)
            String oldestKept = oldestRetainedDate();
            ConfigurationSection limitDailySec = yaml.getConfigurationSection("loot-limits.daily");
            if (limitDailySec != null) {
                for (String dateStr : limitDailySec.getKeys(false)) {
                    if (dateStr.compareTo(oldestKept) < 0) continue;
                    ConfigurationSection dSec = limitDailySec.getConfigurationSection(dateStr);
                    if (dSec != null) {
                        for (String fullKey : dSec.getKeys(false)) {
                            int count = dSec.getInt(fullKey, 0);
                            if (count > 0) {
                                data.setLootDailyCount(dateStr, fullKey, count);
                            }
                        }
                    }
                }
            }

            data.setDirty(false);
        } catch (Throwable t) {
            plugin.logWarn("&c載入玩家資料檔案失敗 (" + uuid + "): " + t.getMessage());
        }

        return data;
    }

    private String oldestRetainedDate() {
        int days = plugin.getConfigManager() != null ? plugin.getConfigManager().getPlayerDailyRetentionDays() : 7;
        return LocalDate.now().minusDays(days).format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    /**
     * 不可變的存檔快照 (在主執行緒擷取，背景序列化)
     */
    private record Snapshot(UUID uuid, String name, long lastSeen,
                            List<String> openedVaults, Map<String, Long> vaultCds,
                            List<String> completedSpawners, Map<String, Long> spawnerCds,
                            Map<String, Integer> lootTotals, Map<String, Map<String, Integer>> lootDailies) {
    }

    private Snapshot snapshot(PlayerData data) {
        String oldestKept = oldestRetainedDate();
        Map<String, Map<String, Integer>> lootDailies = new HashMap<>();
        for (Map.Entry<String, Map<String, Integer>> entry : data.getLootDailyCounts().entrySet()) {
            if (entry.getKey().compareTo(oldestKept) < 0) continue;
            lootDailies.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        data.pruneDailyBefore(oldestKept);
        data.pruneExpiredCooldowns(System.currentTimeMillis());
        return new Snapshot(
                data.getUuid(),
                data.getName(),
                data.getLastSeen(),
                new ArrayList<>(data.getOpenedVaults()),
                new HashMap<>(data.getVaultCooldowns()),
                new ArrayList<>(data.getCompletedSpawners()),
                new HashMap<>(data.getSpawnerCooldowns()),
                new HashMap<>(data.getLootTotalCounts()),
                lootDailies
        );
    }

    private void writeSnapshot(Snapshot s) {
        File file = new File(playerdataFolder, s.uuid().toString() + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("uuid", s.uuid().toString());
        yaml.set("name", s.name());
        yaml.set("last-seen", s.lastSeen());

        long now = System.currentTimeMillis();
        // 寶庫紀錄
        if (!s.openedVaults().isEmpty()) {
            yaml.set("vaults.opened", s.openedVaults());
        }
        for (Map.Entry<String, Long> e : s.vaultCds().entrySet()) {
            if (e.getValue() > now) {
                yaml.set("vaults.cooldowns." + e.getKey(), e.getValue());
            }
        }

        // 生怪磚紀錄
        if (!s.completedSpawners().isEmpty()) {
            yaml.set("spawners.completed", s.completedSpawners());
        }
        for (Map.Entry<String, Long> e : s.spawnerCds().entrySet()) {
            if (e.getValue() > now) {
                yaml.set("spawners.cooldowns." + e.getKey(), e.getValue());
            }
        }

        // 出貨個人總上限
        for (Map.Entry<String, Integer> e : s.lootTotals().entrySet()) {
            if (e.getValue() > 0) {
                yaml.set("loot-limits.total." + e.getKey(), e.getValue());
            }
        }

        // 出貨個人單日上限
        for (Map.Entry<String, Map<String, Integer>> dateEntry : s.lootDailies().entrySet()) {
            String d = dateEntry.getKey();
            for (Map.Entry<String, Integer> e : dateEntry.getValue().entrySet()) {
                if (e.getValue() > 0) {
                    yaml.set("loot-limits.daily." + d + "." + e.getKey(), e.getValue());
                }
            }
        }

        try {
            AsyncIO.writeYamlAtomic(yaml, file);
        } catch (IOException e) {
            plugin.logWarn("&c儲存玩家個人資料失敗 (" + s.uuid() + "): " + e.getMessage());
        }
    }

    /**
     * 儲存玩家資料至 /playerdata/[uuid].yml (快照於呼叫端擷取，寫入排入背景 I/O 佇列)
     */
    public void savePlayerData(PlayerData data, boolean sync) {
        savePlayerData(data, sync, null);
    }

    private void savePlayerData(PlayerData data, boolean sync, Runnable afterWrite) {
        if (data == null) return;
        Snapshot snap = snapshot(data);
        data.setDirty(false);
        AsyncIO.run(() -> {
            writeSnapshot(snap);
            if (afterWrite != null) afterWrite.run();
        }, sync || !plugin.isEnabled());
    }

    /**
     * 儲存所有已標記髒資料的玩家
     */
    public void saveAll(boolean sync) {
        for (PlayerData data : dataCache.values()) {
            if (data.isDirty()) {
                savePlayerData(data, sync);
            }
        }
    }

    private void startAutoSaveTask() {
        // 於主執行緒擷取快照 (資料只在主執行緒變動，快照一致且耗時極低)，寫檔交由背景 I/O 佇列
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            saveAll(false);
            evictIdleOfflineEntries();
        }, 20L * 30L, 20L * 30L);
    }

    /**
     * 回收長時間未存取且已存檔的離線玩家快取 (例如透過指令查詢過的離線玩家)
     */
    private void evictIdleOfflineEntries() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, PlayerData>> it = dataCache.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, PlayerData> entry = it.next();
            UUID uuid = entry.getKey();
            if (entry.getValue().isDirty()) continue;
            if (Bukkit.getPlayer(uuid) != null) continue;
            Long last = lastAccess.get(uuid);
            if (last == null || now - last > IDLE_EVICT_MILLIS) {
                it.remove();
                lastAccess.remove(uuid);
            }
        }
    }

    // ==========================================
    // 監聽器與記憶體控管
    // ==========================================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        lastAccess.put(uuid, System.currentTimeMillis());

        PlayerData cached = dataCache.get(uuid);
        if (cached != null) {
            // 仍在快取中 (例如剛離線、存檔尚未完成就重新登入)，直接沿用記憶體資料
            cached.setName(name);
            cached.updateLastSeen();
            return;
        }

        // 透過 I/O 佇列預載入：排在該玩家任何尚未完成的存檔之後，保證讀到最新資料
        AsyncIO.submit(() -> {
            if (dataCache.containsKey(uuid)) return;
            PlayerData loaded = loadFromFile(uuid);
            PlayerData existing = dataCache.putIfAbsent(uuid, loaded);
            PlayerData target = existing != null ? existing : loaded;
            target.setName(name);
            target.updateLastSeen();
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        PlayerData data = dataCache.get(uuid);
        if (data == null) return;
        data.updateLastSeen();
        // 先存檔，寫入完成後才自快取移除 (若玩家已重新上線則保留)，避免重登讀到舊檔
        savePlayerData(data, false, () -> {
            if (!plugin.isEnabled()) return;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (Bukkit.getPlayer(uuid) == null && !data.isDirty()) {
                    dataCache.remove(uuid, data);
                    lastAccess.remove(uuid);
                }
            });
        });
    }

    public Map<UUID, PlayerData> getDataCache() {
        return Collections.unmodifiableMap(dataCache);
    }

    /**
     * 重設指定玩家的出貨上限 (支援終生上限、每日上限或兩者，並可選關鍵字過濾)
     */
    public void resetPlayerLimits(UUID uuid, String mode, String itemKeyword) {
        if (uuid == null) return;
        PlayerData data = getPlayerData(uuid);
        if (data == null) return;

        boolean resetTotal = "all".equalsIgnoreCase(mode) || "total".equalsIgnoreCase(mode);
        boolean resetDaily = "all".equalsIgnoreCase(mode) || "daily".equalsIgnoreCase(mode);

        if (resetTotal) {
            data.resetTotalLimit(itemKeyword);
        }
        if (resetDaily) {
            data.resetDailyLimit(null, itemKeyword);
        }

        savePlayerData(data, false);
    }

    /**
     * 重設全伺服器所有玩家的出貨上限 (包含快取中與磁碟離線檔案)
     */
    public void resetAllPlayersLimits(String mode, String itemKeyword) {
        boolean resetTotal = "all".equalsIgnoreCase(mode) || "total".equalsIgnoreCase(mode);
        boolean resetDaily = "all".equalsIgnoreCase(mode) || "daily".equalsIgnoreCase(mode);
        boolean hasKeyword = (itemKeyword != null && !itemKeyword.trim().isEmpty());
        String lowerKeyword = hasKeyword ? itemKeyword.trim().toLowerCase() : null;

        // 1. 重設記憶體中快取的所有玩家
        Set<UUID> cachedUuids = new HashSet<>(dataCache.keySet());
        for (PlayerData data : dataCache.values()) {
            if (resetTotal) {
                data.resetTotalLimit(itemKeyword);
            }
            if (resetDaily) {
                data.resetDailyLimit(null, itemKeyword);
            }
        }
        saveAll(false);

        // 2. 於 I/O 佇列中掃描磁碟 playerdata/*.yml 處理離線玩家檔案 (排在上述存檔之後)
        AsyncIO.submit(() -> {
            File[] files = playerdataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files == null || files.length == 0) return;

            for (File file : files) {
                String fileName = file.getName();
                String uuidStr = fileName.substring(0, fileName.length() - 4);
                try {
                    UUID u = UUID.fromString(uuidStr);
                    if (cachedUuids.contains(u) || dataCache.containsKey(u)) {
                        // 記憶體中已有快取並已在步驟 1 處理，跳過以避免覆蓋
                        continue;
                    }
                } catch (Exception ignored) {}

                try {
                    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                    boolean modified = false;

                    if (resetTotal) {
                        if (!hasKeyword) {
                            if (yaml.contains("loot-limits.total")) {
                                yaml.set("loot-limits.total", null);
                                modified = true;
                            }
                        } else {
                            ConfigurationSection totalSec = yaml.getConfigurationSection("loot-limits.total");
                            if (totalSec != null) {
                                for (String key : new ArrayList<>(totalSec.getKeys(false))) {
                                    if (key.toLowerCase().contains(lowerKeyword)) {
                                        totalSec.set(key, null);
                                        modified = true;
                                    }
                                }
                                if (totalSec.getKeys(false).isEmpty()) {
                                    yaml.set("loot-limits.total", null);
                                }
                            }
                        }
                    }

                    if (resetDaily) {
                        if (!hasKeyword) {
                            if (yaml.contains("loot-limits.daily")) {
                                yaml.set("loot-limits.daily", null);
                                modified = true;
                            }
                        } else {
                            ConfigurationSection dailySec = yaml.getConfigurationSection("loot-limits.daily");
                            if (dailySec != null) {
                                for (String dateKey : new ArrayList<>(dailySec.getKeys(false))) {
                                    ConfigurationSection dSec = dailySec.getConfigurationSection(dateKey);
                                    if (dSec != null) {
                                        for (String key : new ArrayList<>(dSec.getKeys(false))) {
                                            if (key.toLowerCase().contains(lowerKeyword)) {
                                                dSec.set(key, null);
                                                modified = true;
                                            }
                                        }
                                        if (dSec.getKeys(false).isEmpty()) {
                                            dailySec.set(dateKey, null);
                                        }
                                    }
                                }
                                if (dailySec.getKeys(false).isEmpty()) {
                                    yaml.set("loot-limits.daily", null);
                                }
                            }
                        }
                    }

                    if (modified) {
                        AsyncIO.writeYamlAtomic(yaml, file);
                    }
                } catch (Exception e) {
                    plugin.logWarn("&c非同步清理離線玩家出貨上限資料失敗 (" + fileName + "): " + e.getMessage());
                }
            }
        });
    }
}
