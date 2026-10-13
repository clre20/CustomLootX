package clre20.customLootX.manager;

import clre20.customLootX.CustomLootX;
import clre20.customLootX.model.LootTemplate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BrushableBlock;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 考古可疑方塊重置管理器
 * 具備：
 * 1. 集中式單一排程計時器 (消除獨立 Task 輪詢浪費)
 * 2. 零物理連鎖防護 (applyPhysics = false)
 * 3. 視距保護 (無在線玩家時不播粒子音效)
 * 4. 斷電/重啟自動還原持久化 (archaeology_runtime.yml)
 */
public class ResetManager {

    private final CustomLootX plugin;
    private final File runtimeFile;
    private final Map<String, ResetSession> pendingResets = new ConcurrentHashMap<>();
    private final Map<String, Location> locationCache = new ConcurrentHashMap<>();
    private BukkitTask tickerTask;
    private volatile boolean dirty = false;
    private int autoSaveCounter = 0;
    // 最近一筆排程的到期時間，未到期前 tick 直接跳過走訪
    private volatile long nextTriggerMillis = 0L;

    public static class ResetSession {
        final UUID sessionId;
        final Location location;
        final String templateName;
        final long scheduledAt;
        final long triggerTimeMillis;

        public ResetSession(UUID sessionId, Location location, String templateName, long delayTicks) {
            this.sessionId = sessionId;
            this.location = location.clone();
            this.templateName = templateName;
            this.scheduledAt = System.currentTimeMillis();
            this.triggerTimeMillis = scheduledAt + (delayTicks * 50L);
        }

        public ResetSession(UUID sessionId, Location location, String templateName, long scheduledAt, long triggerTimeMillis) {
            this.sessionId = sessionId;
            this.location = location.clone();
            this.templateName = templateName;
            this.scheduledAt = scheduledAt;
            this.triggerTimeMillis = triggerTimeMillis;
        }
    }

    public ResetManager(CustomLootX plugin) {
        this.plugin = plugin;
        this.runtimeFile = new File(plugin.getDataFolder(), "data/archaeology_runtime.yml");
        loadRuntimeData();
    }

    private String toLocationKey(Location loc) {
        return (loc.getWorld() != null ? loc.getWorld().getName() : "world") + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    public Location parseLocation(String locKey) {
        if (locKey == null || locKey.isEmpty()) return null;
        Location cached = locationCache.get(locKey);
        if (cached != null) return cached;

        String[] parts = locKey.split(":");
        if (parts.length != 4) return null;
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;
        try {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            Location loc = new Location(world, x, y, z);
            locationCache.put(locKey, loc);
            return loc;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void startTicker() {
        if (tickerTask != null) {
            tickerTask.cancel();
        }
        // 每秒 (20L) 集中巡檢一次所有待重置方塊
        tickerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickResets, 20L, 20L);
    }

    public void stopTicker() {
        if (tickerTask != null) {
            tickerTask.cancel();
            tickerTask = null;
        }
        saveRuntimeData(true);
    }

    private void tickResets() {
        if (pendingResets.isEmpty()) return;

        long now = System.currentTimeMillis();
        if (now < nextTriggerMillis) {
            // 尚未有任何排程到期：零走訪直接返回
            maybeAutoSave();
            return;
        }
        List<ResetSession> toExecute = new ArrayList<>();
        long nextTrigger = Long.MAX_VALUE;

        Iterator<Map.Entry<String, ResetSession>> it = pendingResets.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ResetSession> entry = it.next();
            ResetSession session = entry.getValue();
            if (now >= session.triggerTimeMillis) {
                Location sLoc = session.location;
                // 區塊未載入時不強制載入區塊 (大型伺服器上會造成大量區塊讀寫)，保留排程待區塊載入後再執行
                if (sLoc.getWorld() == null || !sLoc.getWorld().isChunkLoaded(sLoc.getBlockX() >> 4, sLoc.getBlockZ() >> 4)) {
                    nextTrigger = Math.min(nextTrigger, now + 5000L);
                    continue;
                }
                it.remove();
                locationCache.remove(entry.getKey());
                toExecute.add(session);
                dirty = true;
            } else {
                nextTrigger = Math.min(nextTrigger, session.triggerTimeMillis);
            }
        }
        nextTriggerMillis = nextTrigger;

        for (ResetSession session : toExecute) {
            try {
                executeReset(session);
            } catch (Exception e) {
                plugin.logWarn("&b[可疑方塊·重置]&c 執行方塊重置時發生錯誤: " + e.getMessage());
            }
        }

        maybeAutoSave();
    }

    private void maybeAutoSave() {
        autoSaveCounter++;
        if (autoSaveCounter >= plugin.getConfigManager().getRuntimeSaveIntervalSeconds() && dirty) {
            autoSaveCounter = 0;
            saveRuntimeData(false);
        }
    }

    /**
     * 排程方塊重置
     * @param seconds delay in seconds before resetting
     */
    public void scheduleReset(Location loc, String templateName, int seconds) {
        if (loc == null || loc.getWorld() == null || templateName == null || seconds <= 0) {
            return;
        }

        String locKey = toLocationKey(loc);
        if (pendingResets.containsKey(locKey)) {
            return;
        }

        locationCache.put(locKey, loc.clone());
        UUID sessionId = UUID.randomUUID();
        long delayTicks = (long) seconds * 20L;
        ResetSession session = new ResetSession(sessionId, loc, templateName, delayTicks);

        pendingResets.put(locKey, session);
        nextTriggerMillis = Math.min(nextTriggerMillis, session.triggerTimeMillis);
        dirty = true;

        plugin.getConfigManager().log("reset-scheduled",
                "%world%", loc.getWorld() != null ? loc.getWorld().getName() : "world",
                "%x%", loc.getBlockX(),
                "%y%", loc.getBlockY(),
                "%z%", loc.getBlockZ(),
                "%name%", templateName,
                "%time%", clre20.customLootX.util.TextUtil.formatTimeSeconds(seconds),
                "%seconds%", seconds,
                "%minutes%", Math.max(1, (int) Math.ceil((double) seconds / 60.0))
        );
    }

    /**
     * 取消指定座標的待重置排程
     */
    public void cancelReset(Location loc) {
        // 熱路徑快速返回：每次放置/破壞任何方塊都會呼叫，沒有待重置排程時不建立任何字串
        if (loc == null || pendingResets.isEmpty()) return;
        String locKey = toLocationKey(loc);
        locationCache.remove(locKey);
        ResetSession session = pendingResets.remove(locKey);
        if (session != null) {
            dirty = true;
            plugin.getConfigManager().log("reset-cancelled",
                    "%world%", loc.getWorld() != null ? loc.getWorld().getName() : "world",
                    "%x%", loc.getBlockX(),
                    "%y%", loc.getBlockY(),
                    "%z%", loc.getBlockZ(),
                    "%name%", session.templateName
            );
        }
    }

    private void executeReset(ResetSession session) {
        Location loc = session.location;
        if (loc.getWorld() == null) return;

        int chunkX = loc.getBlockX() >> 4;
        int chunkZ = loc.getBlockZ() >> 4;

        // 區塊已於 tickResets 確認載入；保險起見再檢查一次，未載入則放回排程
        if (!loc.getWorld().isChunkLoaded(chunkX, chunkZ)) {
            pendingResets.putIfAbsent(toLocationKey(loc), session);
            nextTriggerMillis = Math.min(nextTriggerMillis, System.currentTimeMillis() + 5000L);
            return;
        }

        applyResetBlock(loc, session);
    }

    private void applyResetBlock(Location loc, ResetSession session) {
        if (loc.getWorld() == null) return;

        Block block = loc.getBlock();
        // 核心安全檢查：方塊必須依然為被刷過後的普通沙子或普通礫石！
        // 若玩家已挖掉或替換為其他方塊，不執行覆寫！
        if (block.getType() != Material.SAND && block.getType() != Material.GRAVEL) {
            plugin.getConfigManager().log("reset-skipped",
                    "%world%", loc.getWorld() != null ? loc.getWorld().getName() : "world",
                    "%x%", loc.getBlockX(),
                    "%y%", loc.getBlockY(),
                    "%z%", loc.getBlockZ(),
                    "%type%", block.getType().name(),
                    "%name%", session.templateName
            );
            return;
        }

        LootTemplate template = plugin.getTemplateManager().getTemplate(session.templateName);
        if (template == null) {
            plugin.getConfigManager().log("reset-not-found",
                    "%world%", loc.getWorld() != null ? loc.getWorld().getName() : "world",
                    "%x%", loc.getBlockX(),
                    "%y%", loc.getBlockY(),
                    "%z%", loc.getBlockZ(),
                    "%name%", session.templateName
            );
            return;
        }

        Material targetType = template.getType();
        // 核心優化 1：傳入 applyPhysics = false，禁止觸發相鄰方塊的物理更新，徹底杜絕骨牌式掉落沙物理運算！
        block.setType(targetType, false);

        if (block.getState(false) instanceof BrushableBlock brushable) {
            brushable.clearLootTable();
            brushable.setLootTable(null);

            PersistentDataContainer pdc = brushable.getPersistentDataContainer();
            pdc.set(plugin.getItemManager().KEY_CUSTOM_LOOT, PersistentDataType.BYTE, (byte) 1);
            pdc.set(plugin.getItemManager().KEY_TEMPLATE_NAME, PersistentDataType.STRING, template.getName());
            pdc.set(plugin.getItemManager().KEY_BLOCK_TYPE, PersistentDataType.STRING, targetType.name());

            ItemStack rolled = template.rollItem();
            if (rolled != null && !rolled.getType().isAir()) {
                pdc.set(plugin.getItemManager().KEY_ROLLED_ITEM, PersistentDataType.BYTE_ARRAY, rolled.serializeAsBytes());
                brushable.setItem(rolled.clone());
            } else {
                pdc.set(plugin.getItemManager().KEY_ROLLED_ITEM, PersistentDataType.BYTE_ARRAY, new byte[0]);
                brushable.setItem(null);
            }

            // 核心優化 2：update(true, false) 禁止物理更新傳播
            brushable.update(true, false);

            // 核心優化 3：視距守衛 (View Guard)，32 格內無任何在線玩家時不產生粒子與音效
            if (loc.getWorld().getPlayerCount() > 0 && !loc.getWorld().getNearbyPlayers(loc, 32).isEmpty()) {
                try {
                    loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0.5, 0.5, 0.5), 10, 0.3, 0.3, 0.3, 0.05);
                    plugin.getConfigManager().playSound(null, "reset");
                    loc.getWorld().playSound(loc, Sound.BLOCK_SAND_PLACE, 1.0f, 1.0f);
                } catch (Exception ignored) {
                }
            }

            if (!plugin.getConfigManager().isLogEnabled("reset-executed")) {
                return;
            }
            String rolledDesc = clre20.customLootX.util.TextUtil.getItemDescription(rolled);
            String typeDesc = targetType == Material.SUSPICIOUS_SAND
                    ? plugin.getConfigManager().getText("items.configured.type-sand", "可疑沙")
                    : plugin.getConfigManager().getText("items.configured.type-gravel", "可疑礫石");

            plugin.getConfigManager().log("reset-executed",
                    "%world%", loc.getWorld() != null ? loc.getWorld().getName() : "world",
                    "%x%", loc.getBlockX(),
                    "%y%", loc.getBlockY(),
                    "%z%", loc.getBlockZ(),
                    "%name%", template.getName(),
                    "%type%", typeDesc,
                    "%rolled%", rolledDesc
            );
        }
    }

    public boolean hasPendingReset(Location loc) {
        if (loc == null || pendingResets.isEmpty()) return false;
        return pendingResets.containsKey(toLocationKey(loc));
    }

    public void cancelAll() {
        pendingResets.clear();
        locationCache.clear();
        dirty = true;
    }

    // ==========================================
    // 持久化資料管理 (Async Compute, Sync Apply)
    // ==========================================

    public void loadRuntimeData() {
        if (!runtimeFile.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(runtimeFile);

        long now = System.currentTimeMillis();
        org.bukkit.configuration.ConfigurationSection sec = yaml.getConfigurationSection("resets");
        if (sec == null) return;

        int restored = 0;
        for (String locKey : sec.getKeys(false)) {
            String templateName = sec.getString(locKey + ".template");
            long triggerTime = sec.getLong(locKey + ".trigger-time", 0L);
            long scheduledAt = sec.getLong(locKey + ".scheduled-at", now);

            if (templateName == null || triggerTime <= 0) continue;

            Location loc = parseLocation(locKey);
            if (loc == null) continue;

            ResetSession session = new ResetSession(UUID.randomUUID(), loc, templateName, scheduledAt, triggerTime);
            pendingResets.put(locKey, session);
            restored++;
        }

        if (restored > 0) {
            plugin.logConsole("&b[可疑方塊·持久化]&7 成功還原 &a" + restored + " &7個待重置的考古方塊排程。");
        }
    }

    public void saveRuntimeData(boolean sync) {
        if (!dirty && runtimeFile.exists()) {
            return;
        }

        // 複製快照，避免主執行緒卡頓，在背景非同步建構 YAML 樹與寫入硬碟
        Map<String, ResetSession> snapshot = new HashMap<>(pendingResets);
        dirty = false;

        Runnable saveAction = () -> {
            YamlConfiguration yaml = new YamlConfiguration();
            for (Map.Entry<String, ResetSession> entry : snapshot.entrySet()) {
                String locKey = entry.getKey();
                ResetSession session = entry.getValue();
                yaml.set("resets." + locKey + ".template", session.templateName);
                yaml.set("resets." + locKey + ".scheduled-at", session.scheduledAt);
                yaml.set("resets." + locKey + ".trigger-time", session.triggerTimeMillis);
            }
            try {
                clre20.customLootX.util.AsyncIO.writeYamlAtomic(yaml, runtimeFile);
            } catch (IOException e) {
                dirty = true;
                plugin.logWarn("&b[可疑方塊·持久化]&c 儲存待重置排程失敗: " + e.getMessage());
            }
        };

        clre20.customLootX.util.AsyncIO.run(saveAction, sync || !plugin.isEnabled());
    }
}
