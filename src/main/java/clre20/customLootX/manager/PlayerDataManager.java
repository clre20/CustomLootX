package clre20.customLootX.manager;

import clre20.customLootX.CustomLootX;
import clre20.customLootX.model.PlayerData;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家資料管理器 (Player Data Manager)
 * 負責維護 /playerdata/[uuid].yml 獨立玩家設定檔。
 * 支援非同步 I/O 儲存、快取記憶體回收與自動定時存檔。
 */
public class PlayerDataManager implements Listener {

    private final CustomLootX plugin;
    private final File playerdataFolder;
    private final Map<UUID, PlayerData> dataCache = new ConcurrentHashMap<>();

    public PlayerDataManager(CustomLootX plugin) {
        this.plugin = plugin;
        this.playerdataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!playerdataFolder.exists()) {
            playerdataFolder.mkdirs();
        }

        // 註冊玩家進出監聽器，即時預載與安全儲存釋放記憶體
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // 啟動定時非同步儲存排程 (每 30 秒非同步巡檢寫入髒資料)
        startAutoSaveTask();
    }

    /**
     * 取得玩家資料物件 (記憶體優先，未載入時從磁碟讀取或新建)
     */
    public PlayerData getPlayerData(UUID uuid) {
        if (uuid == null) return null;
        return dataCache.computeIfAbsent(uuid, this::loadFromFile);
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
        } else {
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            if (op.getName() != null) playerName = op.getName();
        }

        PlayerData data = new PlayerData(uuid, playerName);

        if (!file.exists()) {
            data.setDirty(true);
            return data;
        }

        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            data.setName(yaml.getString("name", playerName));

            // 讀取寶庫資料
            List<String> openedVaults = yaml.getStringList("vaults.opened");
            for (String v : openedVaults) {
                data.addOpenedVault(v);
            }
            ConfigurationSection vCdSec = yaml.getConfigurationSection("vaults.cooldowns");
            if (vCdSec != null) {
                long now = System.currentTimeMillis();
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
                long now = System.currentTimeMillis();
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

            // 讀取出貨個人單日上限 (每日累計)
            ConfigurationSection limitDailySec = yaml.getConfigurationSection("loot-limits.daily");
            if (limitDailySec != null) {
                for (String dateStr : limitDailySec.getKeys(false)) {
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

    /**
     * 儲存玩家資料至 /playerdata/[uuid].yml (支援非同步磁碟寫入)
     */
    public void savePlayerData(PlayerData data, boolean sync) {
        if (data == null) return;
        UUID uuid = data.getUuid();
        File file = new File(playerdataFolder, uuid.toString() + ".yml");

        // 複製一份快照資料，避免多執行緒序列化競爭
        String name = data.getName();
        long lastSeen = data.getLastSeen();
        List<String> openedVaults = new ArrayList<>(data.getOpenedVaults());
        Map<String, Long> vaultCds = new HashMap<>(data.getVaultCooldowns());
        List<String> completedSpawners = new ArrayList<>(data.getCompletedSpawners());
        Map<String, Long> spawnerCds = new HashMap<>(data.getSpawnerCooldowns());
        Map<String, Integer> lootTotals = new HashMap<>(data.getLootTotalCounts());
        Map<String, Map<String, Integer>> lootDailies = new HashMap<>();
        for (Map.Entry<String, Map<String, Integer>> entry : data.getLootDailyCounts().entrySet()) {
            lootDailies.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }

        data.setDirty(false);

        Runnable writeTask = () -> {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set("uuid", uuid.toString());
            yaml.set("name", name);
            yaml.set("last-seen", lastSeen);

            // 寶庫紀錄
            if (!openedVaults.isEmpty()) {
                yaml.set("vaults.opened", openedVaults);
            }
            long now = System.currentTimeMillis();
            for (Map.Entry<String, Long> e : vaultCds.entrySet()) {
                if (e.getValue() > now) {
                    yaml.set("vaults.cooldowns." + e.getKey(), e.getValue());
                }
            }

            // 生怪磚紀錄
            if (!completedSpawners.isEmpty()) {
                yaml.set("spawners.completed", completedSpawners);
            }
            for (Map.Entry<String, Long> e : spawnerCds.entrySet()) {
                if (e.getValue() > now) {
                    yaml.set("spawners.cooldowns." + e.getKey(), e.getValue());
                }
            }

            // 出貨個人總上限
            for (Map.Entry<String, Integer> e : lootTotals.entrySet()) {
                if (e.getValue() > 0) {
                    yaml.set("loot-limits.total." + e.getKey(), e.getValue());
                }
            }

            // 出貨個人單日上限
            for (Map.Entry<String, Map<String, Integer>> dateEntry : lootDailies.entrySet()) {
                String d = dateEntry.getKey();
                for (Map.Entry<String, Integer> e : dateEntry.getValue().entrySet()) {
                    if (e.getValue() > 0) {
                        yaml.set("loot-limits.daily." + d + "." + e.getKey(), e.getValue());
                    }
                }
            }

            try {
                yaml.save(file);
            } catch (IOException e) {
                plugin.logWarn("&c儲存玩家個人資料失敗 (" + uuid + "): " + e.getMessage());
            }
        };

        if (sync || !plugin.isEnabled()) {
            writeTask.run();
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, writeTask);
        }
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
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (PlayerData data : dataCache.values()) {
                if (data.isDirty()) {
                    savePlayerData(data, false);
                }
            }
        }, 20L * 30L, 20L * 30L);
    }

    // ==========================================
    // 監聽器與記憶體控管
    // ==========================================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        // 非同步預載入檔案資料
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData data = getPlayerData(uuid);
            if (data != null) {
                data.setName(player.getName());
                data.updateLastSeen();
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        PlayerData data = dataCache.get(uuid);
        if (data != null) {
            data.updateLastSeen();
            // 玩家離線時，非同步存檔後自快取中移除，嚴格釋放記憶體避免 GC 負擔
            savePlayerData(data, false);
            dataCache.remove(uuid);
        }
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
        for (PlayerData data : dataCache.values()) {
            if (resetTotal) {
                data.resetTotalLimit(itemKeyword);
            }
            if (resetDaily) {
                data.resetDailyLimit(null, itemKeyword);
            }
        }
        saveAll(false);

        // 2. 非同步掃描磁碟中 playerdata/*.yml 處理離線玩家檔案
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File[] files = playerdataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files == null || files.length == 0) return;

            for (File file : files) {
                String fileName = file.getName();
                String uuidStr = fileName.substring(0, fileName.length() - 4);
                try {
                    UUID u = UUID.fromString(uuidStr);
                    if (dataCache.containsKey(u)) {
                        // 記憶體中已有快取並已在步驟 1 標記儲存，跳過以避免覆蓋
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
                        yaml.save(file);
                    }
                } catch (Exception e) {
                    plugin.logWarn("&c非同步清理離線玩家出貨上限資料失敗 (" + fileName + "): " + e.getMessage());
                }
            }
        });
    }
}
