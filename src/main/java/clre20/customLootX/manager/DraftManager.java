package clre20.customLootX.manager;

import clre20.customLootX.CustomLootX;
import clre20.customLootX.model.*;
import clre20.customLootX.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DraftManager {

    private final CustomLootX plugin;
    private final File baseDraftsDir;

    private final Set<String> activeDraftKeys = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public DraftManager(CustomLootX plugin) {
        this.plugin = plugin;
        this.baseDraftsDir = new File(plugin.getDataFolder(), "drafts");
        ensureDirectories();
        refreshActiveDrafts();
    }

    private String toDraftKey(DraftType type, String draftId) {
        if (type == null || draftId == null) return "";
        return type.name() + ":" + draftId.trim().toLowerCase();
    }

    public void refreshActiveDrafts() {
        activeDraftKeys.clear();
        for (DraftType type : DraftType.values()) {
            File dir = getDraftDir(type);
            if (!dir.exists()) continue;
            File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".yml"));
            if (files == null) continue;
            for (File file : files) {
                String draftId = file.getName().substring(0, file.getName().length() - 4);
                activeDraftKeys.add(toDraftKey(type, draftId));
            }
        }
    }

    private void ensureDirectories() {
        for (DraftType type : DraftType.values()) {
            File dir = getDraftDir(type);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
    }

    public File getDraftDir(DraftType type) {
        return new File(baseDraftsDir, type.getFolderName());
    }

    public int getExpireDays() {
        return plugin.getConfigManager().getConfig().getInt("settings.draft.expire-days", 7);
    }

    /**
     * 生成短唯一識別碼
     */
    public String generateDraftId() {
        return "d_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 儲存或更新草稿
     */
    public DraftSession saveDraft(DraftType type, String draftId, String originalName, Object templateData) {
        if (type == null || templateData == null) return null;
        if (draftId == null || draftId.trim().isEmpty()) {
            if (originalName != null && !originalName.trim().isEmpty()) {
                DraftSession existing = findDraftByOriginalName(type, originalName);
                if (existing != null) {
                    draftId = existing.getDraftId();
                }
            }
            if (draftId == null || draftId.trim().isEmpty()) {
                draftId = generateDraftId();
            }
        }

        File file = new File(getDraftDir(type), draftId + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();

        long now = System.currentTimeMillis();
        long expireTime = now + (getExpireDays() * 86400000L);

        // Header info
        yaml.set("draft.id", draftId);
        yaml.set("draft.type", type.name());
        yaml.set("draft.original-name", originalName);
        yaml.set("draft.created-time", now);
        yaml.set("draft.expire-time", expireTime);

        // Serialize body
        if (templateData instanceof LootTemplate lt) {
            serializeLootTemplate(yaml, lt);
        } else if (templateData instanceof VaultTemplate vt) {
            serializeVaultTemplate(yaml, vt);
        } else if (templateData instanceof SpawnerTemplate st) {
            serializeSpawnerTemplate(yaml, st);
        }

        try {
            yaml.save(file);
            activeDraftKeys.add(toDraftKey(type, draftId));
            return new DraftSession(draftId, type, originalName, now, expireTime, templateData);
        } catch (IOException e) {
            plugin.logError("&c[草稿·儲存]&c 儲存草稿檔案失敗: " + e.getMessage());
            return null;
        }
    }

    /**
     * 讀取草稿
     */
    public DraftSession loadDraft(DraftType type, String draftId) {
        if (type == null || draftId == null || draftId.trim().isEmpty()) return null;

        File file = new File(getDraftDir(type), draftId + ".yml");
        if (!file.exists()) return null;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        long createdTime = yaml.getLong("draft.created-time", System.currentTimeMillis());
        long expireTime = yaml.getLong("draft.expire-time", 0);
        String originalName = yaml.getString("draft.original-name", null);

        Object templateData = null;
        if (type == DraftType.SUSPICIOUS) {
            templateData = deserializeLootTemplate(yaml);
        } else if (type == DraftType.VAULT) {
            templateData = deserializeVaultTemplate(yaml);
        } else if (type == DraftType.SPAWNER) {
            templateData = deserializeSpawnerTemplate(yaml);
        }

        if (templateData == null) return null;

        return new DraftSession(draftId, type, originalName, createdTime, expireTime, templateData);
    }

    /**
     * 檢查草稿檔案是否存在 (純記憶體快速比對，零硬碟 I/O)
     */
    public boolean hasDraft(DraftType type, String draftId) {
        if (type == null || draftId == null || draftId.trim().isEmpty()) return false;
        return activeDraftKeys.contains(toDraftKey(type, draftId));
    }

    /**
     * 依據配置原名搜尋尚未過期的草稿
     */
    public DraftSession findDraftByOriginalName(DraftType type, String originalName) {
        if (type == null || originalName == null || originalName.trim().isEmpty()) return null;
        File dir = getDraftDir(type);
        if (!dir.exists()) return null;

        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null) return null;

        DraftSession newest = null;
        for (File file : files) {
            String draftId = file.getName().substring(0, file.getName().length() - 4);
            DraftSession session = loadDraft(type, draftId);
            if (session != null && !session.isExpired()) {
                if (originalName.equalsIgnoreCase(session.getOriginalName())) {
                    if (newest == null || session.getCreatedTime() > newest.getCreatedTime()) {
                        newest = session;
                    }
                }
            }
        }
        return newest;
    }

    /**
     * 依據配置原名刪除所有草稿
     */
    public boolean deleteDraftByOriginalName(DraftType type, String originalName) {
        if (type == null || originalName == null || originalName.trim().isEmpty()) return false;
        File dir = getDraftDir(type);
        if (!dir.exists()) return false;

        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null) return false;

        boolean deleted = false;
        for (File file : files) {
            String draftId = file.getName().substring(0, file.getName().length() - 4);
            DraftSession session = loadDraft(type, draftId);
            if (session != null && originalName.equalsIgnoreCase(session.getOriginalName())) {
                if (file.delete()) {
                    activeDraftKeys.remove(toDraftKey(type, draftId));
                    deleted = true;
                }
            }
        }
        return deleted;
    }

    /**
     * 刪除指定草稿
     */
    public boolean deleteDraft(DraftType type, String draftId) {
        if (type == null || draftId == null || draftId.trim().isEmpty()) return false;
        activeDraftKeys.remove(toDraftKey(type, draftId));
        File file = new File(getDraftDir(type), draftId + ".yml");
        if (file.exists()) {
            return file.delete();
        }
        return false;
    }

    /**
     * 清理所有已過期的草稿檔案
     */
    public int cleanExpiredDrafts() {
        int cleaned = 0;
        long now = System.currentTimeMillis();
        for (DraftType type : DraftType.values()) {
            File dir = getDraftDir(type);
            if (!dir.exists()) continue;

            File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".yml"));
            if (files == null) continue;

            for (File file : files) {
                try {
                    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                    long expire = yaml.getLong("draft.expire-time", 0);
                    if (expire > 0 && now > expire) {
                        String draftId = file.getName().substring(0, file.getName().length() - 4);
                        if (file.delete()) {
                            activeDraftKeys.remove(toDraftKey(type, draftId));
                            cleaned++;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        return cleaned;
    }

    // ==========================================
    // Serialization & Deserialization Helpers
    // ==========================================

    private void serializeLootTemplate(YamlConfiguration yaml, LootTemplate template) {
        yaml.set("name", template.getName());
        yaml.set("type", template.getType().name());
        yaml.set("display-name", template.getDisplayName());
        yaml.set("reset-enabled", template.isResetEnabled());
        yaml.set("reset-seconds", template.getResetSeconds());
        yaml.set("reset-minutes", template.getResetMinutes());
        if (template.getBroadcastMessage() != null && !template.getBroadcastMessage().trim().isEmpty()) {
            yaml.set("broadcast-message", template.getBroadcastMessage());
        }

        List<Map<String, Object>> itemsList = new ArrayList<>();
        List<LootItem> items = template.getItems();
        for (int i = 0; i < items.size(); i++) {
            LootItem item = items.get(i);
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("slot", i);
            map.put("chance", item.getChance());
            map.put("is-air", item.isAir());
            map.put("broadcast", item.isBroadcast());
            if (item.getBroadcastMessage() != null && !item.getBroadcastMessage().trim().isEmpty()) {
                map.put("broadcast-message", item.getBroadcastMessage());
            }
            if (item.getLimitServerDaily() > 0) map.put("limit-server-daily", item.getLimitServerDaily());
            if (item.getLimitServerMonthly() > 0) map.put("limit-server-monthly", item.getLimitServerMonthly());
            if (item.getLimitPlayerDaily() > 0) map.put("limit-player-daily", item.getLimitPlayerDaily());
            if (item.getLimitPlayerTotal() > 0) map.put("limit-player-total", item.getLimitPlayerTotal());
            if (!item.isAir() && item.getItem() != null) {
                map.put("item", item.getItem());
            }
            itemsList.add(map);
        }
        yaml.set("items", itemsList);
    }

    private LootTemplate deserializeLootTemplate(YamlConfiguration yaml) {
        String name = yaml.getString("name", "");
        String typeStr = yaml.getString("type", "SUSPICIOUS_SAND");
        Material type = Material.getMaterial(typeStr);
        if (type == null) type = Material.SUSPICIOUS_SAND;

        LootTemplate template = new LootTemplate(name, type);
        template.setDisplayName(yaml.getString("display-name", null));
        template.setResetEnabled(yaml.getBoolean("reset-enabled", false));
        int resetSeconds = yaml.contains("reset-seconds")
                ? Math.max(1, yaml.getInt("reset-seconds"))
                : Math.max(1, yaml.getInt("reset-minutes", 5) * 60);
        template.setResetSeconds(resetSeconds);
        template.setBroadcastMessage(yaml.getString("broadcast-message", null));

        List<?> list = yaml.getList("items");
        if (list != null) {
            for (Object obj : list) {
                if (obj instanceof Map<?, ?> map) {
                    double chance = 0.0;
                    Object cObj = map.get("chance");
                    if (cObj instanceof Number n) chance = n.doubleValue();
                    boolean isAir = Boolean.TRUE.equals(map.get("is-air"));
                    boolean broadcast = Boolean.TRUE.equals(map.get("broadcast"));
                    String itemBroadcastMsg = (String) map.get("broadcast-message");
                    int limitServerDaily = (map.get("limit-server-daily") instanceof Number n) ? n.intValue() : 0;
                    int limitServerMonthly = (map.get("limit-server-monthly") instanceof Number n) ? n.intValue() : 0;
                    int limitPlayerDaily = (map.get("limit-player-daily") instanceof Number n) ? n.intValue() : 0;
                    int limitPlayerTotal = (map.get("limit-player-total") instanceof Number n) ? n.intValue() : 0;

                    ItemStack item = null;
                    Object iObj = map.get("item");
                    if (iObj instanceof ItemStack is) item = is;

                    LootItem lootItem;
                    if (isAir || item == null) {
                        lootItem = new LootItem(chance, true, broadcast, itemBroadcastMsg);
                    } else {
                        lootItem = new LootItem(item, chance, broadcast, itemBroadcastMsg);
                    }
                    lootItem.setLimitServerDaily(limitServerDaily);
                    lootItem.setLimitServerMonthly(limitServerMonthly);
                    lootItem.setLimitPlayerDaily(limitPlayerDaily);
                    lootItem.setLimitPlayerTotal(limitPlayerTotal);
                    template.addItem(lootItem);
                }
            }
        }
        return template;
    }

    private void serializeVaultTemplate(YamlConfiguration yaml, VaultTemplate template) {
        yaml.set("name", template.getName());
        yaml.set("ominous", template.isOminous());
        yaml.set("display-name", template.getDisplayName());
        yaml.set("key-item", template.getKeyItem());
        yaml.set("roll-count", template.getRollCount());
        yaml.set("cooldown.mode", template.getCooldownMode().name());
        yaml.set("cooldown.seconds", template.getCooldownSeconds());
        yaml.set("cooldown.minutes", template.getCooldownMinutes());
        if (template.getBroadcastMessage() != null && !template.getBroadcastMessage().trim().isEmpty()) {
            yaml.set("broadcast-message", template.getBroadcastMessage());
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (LootItem loot : template.getItems()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("chance", TextUtil.roundChance(loot.getChance()));
            map.put("broadcast", loot.isBroadcast());
            if (loot.getBroadcastMessage() != null && !loot.getBroadcastMessage().trim().isEmpty()) {
                map.put("broadcast-message", loot.getBroadcastMessage());
            }
            if (loot.getLimitServerDaily() > 0) map.put("limit-server-daily", loot.getLimitServerDaily());
            if (loot.getLimitServerMonthly() > 0) map.put("limit-server-monthly", loot.getLimitServerMonthly());
            if (loot.getLimitPlayerDaily() > 0) map.put("limit-player-daily", loot.getLimitPlayerDaily());
            if (loot.getLimitPlayerTotal() > 0) map.put("limit-player-total", loot.getLimitPlayerTotal());
            if (loot.isAir() || loot.getItem() == null) {
                map.put("is-air", true);
            } else {
                map.put("item", loot.getItem());
            }
            list.add(map);
        }
        yaml.set("items", list);
    }

    private VaultTemplate deserializeVaultTemplate(YamlConfiguration yaml) {
        String name = yaml.getString("name", "");
        boolean ominous = yaml.getBoolean("ominous", false);
        String displayName = yaml.getString("display-name", name);
        ItemStack keyItem = yaml.getItemStack("key-item");
        if (keyItem == null) {
            keyItem = new ItemStack(ominous ? Material.OMINOUS_TRIAL_KEY : Material.TRIAL_KEY);
        }
        int rollCount = yaml.getInt("roll-count", 3);
        String modeStr = yaml.getString("cooldown.mode", "PLAYER_COOLDOWN");
        VaultCooldownMode mode = VaultCooldownMode.fromString(modeStr);
        int cooldownSeconds = yaml.contains("cooldown.seconds")
                ? yaml.getInt("cooldown.seconds")
                : yaml.getInt("cooldown.minutes", 10) * 60;
        String broadcastMessage = yaml.getString("broadcast-message", null);

        List<LootItem> items = new ArrayList<>();
        List<?> rawItems = yaml.getList("items");
        if (rawItems != null) {
            for (Object obj : rawItems) {
                if (obj instanceof Map<?, ?> map) {
                    double chance = 0.0;
                    Object cObj = map.get("chance");
                    if (cObj instanceof Number n) chance = n.doubleValue();
                    boolean isAir = Boolean.TRUE.equals(map.get("is-air"));
                    boolean broadcast = Boolean.TRUE.equals(map.get("broadcast"));
                    String itemBroadcastMsg = (String) map.get("broadcast-message");
                    int limitServerDaily = (map.get("limit-server-daily") instanceof Number n) ? n.intValue() : 0;
                    int limitServerMonthly = (map.get("limit-server-monthly") instanceof Number n) ? n.intValue() : 0;
                    int limitPlayerDaily = (map.get("limit-player-daily") instanceof Number n) ? n.intValue() : 0;
                    int limitPlayerTotal = (map.get("limit-player-total") instanceof Number n) ? n.intValue() : 0;

                    ItemStack item = null;
                    Object iObj = map.get("item");
                    if (iObj instanceof ItemStack is) item = is;

                    LootItem lootItem;
                    if (isAir || item == null) {
                        lootItem = new LootItem(chance, true, broadcast, itemBroadcastMsg);
                    } else {
                        lootItem = new LootItem(item, chance, broadcast, itemBroadcastMsg);
                    }
                    lootItem.setLimitServerDaily(limitServerDaily);
                    lootItem.setLimitServerMonthly(limitServerMonthly);
                    lootItem.setLimitPlayerDaily(limitPlayerDaily);
                    lootItem.setLimitPlayerTotal(limitPlayerTotal);
                    items.add(lootItem);
                }
            }
        }
        VaultTemplate vt = new VaultTemplate(name, ominous, displayName, keyItem, rollCount, mode, Math.max(1, (int) Math.ceil((double) cooldownSeconds / 60.0)), items, broadcastMessage);
        vt.setCooldownSeconds(cooldownSeconds);
        return vt;
    }

    private void serializeSpawnerTemplate(YamlConfiguration yaml, SpawnerTemplate template) {
        yaml.set("name", template.getName());
        yaml.set("ominous", template.isOminous());
        yaml.set("display-name", template.getDisplayName());
        yaml.set("display-cycle", template.isDisplayCycle());
        if (template.getDisplayMobId() != null) {
            yaml.set("display-mob-id", template.getDisplayMobId());
        }
        yaml.set("spawned-type", template.getSpawnedType().name());
        yaml.set("spawn-mode", "SEQUENCE");
        yaml.set("spawn-sequence", template.getSpawnSequence());
        List<List<String>> waveData = new ArrayList<>();
        for (List<String> w : template.getWaves()) {
            waveData.add(new ArrayList<>(w));
        }
        yaml.set("waves", waveData);

        List<Map<String, Object>> mobList = new ArrayList<>();
        for (SpawnerMobEntry entry : template.getMobPool()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("mythic", entry.isMythic());
            map.put("id", entry.getMobId());
            if (entry.getCustomDisplayName() != null) {
                map.put("display-name", entry.getCustomDisplayName());
            }
            map.put("chance", TextUtil.roundChance(entry.getChance()));
            mobList.add(map);
        }
        yaml.set("mobs", mobList);

        yaml.set("wave.total-mobs", template.getTotalMobs());
        yaml.set("wave.simultaneous-mobs", template.getSimultaneousMobs());
        yaml.set("wave.spawn-delay-seconds", template.getSpawnDelaySeconds());
        yaml.set("wave.player-range", template.getPlayerRange());
        yaml.set("wave.wait-wave-cleared", template.isWaitWaveCleared());
        yaml.set("show-actionbar", template.isShowActionBar());
        yaml.set("victory-sound.enabled", template.isVictorySoundEnabled());
        yaml.set("victory-sound.sound", template.getVictorySound());
        yaml.set("victory-sound.volume", template.getVictorySoundVolume());
        yaml.set("victory-sound.pitch", template.getVictorySoundPitch());
        yaml.set("cooldown.mode", template.getCooldownMode().name());
        yaml.set("cooldown.seconds", template.getCooldownSeconds());
        yaml.set("cooldown.minutes", template.getCooldownMinutes());
        yaml.set("roll-count", template.getRollCount());
        if (template.getBroadcastMessage() != null && !template.getBroadcastMessage().trim().isEmpty()) {
            yaml.set("broadcast-message", template.getBroadcastMessage());
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (LootItem loot : template.getRewards()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("chance", TextUtil.roundChance(loot.getChance()));
            map.put("broadcast", loot.isBroadcast());
            if (loot.getBroadcastMessage() != null && !loot.getBroadcastMessage().trim().isEmpty()) {
                map.put("broadcast-message", loot.getBroadcastMessage());
            }
            if (loot.getLimitServerDaily() > 0) map.put("limit-server-daily", loot.getLimitServerDaily());
            if (loot.getLimitServerMonthly() > 0) map.put("limit-server-monthly", loot.getLimitServerMonthly());
            if (loot.getLimitPlayerDaily() > 0) map.put("limit-player-daily", loot.getLimitPlayerDaily());
            if (loot.getLimitPlayerTotal() > 0) map.put("limit-player-total", loot.getLimitPlayerTotal());
            if (loot.isAir() || loot.getItem() == null) {
                map.put("is-air", true);
            } else {
                map.put("item", loot.getItem());
            }
            list.add(map);
        }
        yaml.set("items", list);
    }

    private SpawnerTemplate deserializeSpawnerTemplate(YamlConfiguration yaml) {
        String name = yaml.getString("name", "");
        boolean ominous = yaml.getBoolean("ominous", false);
        String displayName = yaml.getString("display-name", name);
        boolean displayCycle = yaml.getBoolean("display-cycle", false);
        String displayMobId = yaml.getString("display-mob-id", null);
        String typeStr = yaml.getString("spawned-type", "ZOMBIE");
        EntityType spawnedType;
        try {
            spawnedType = EntityType.valueOf(typeStr);
        } catch (Exception e) {
            spawnedType = EntityType.ZOMBIE;
        }

        List<SpawnerMobEntry> mobPool = new ArrayList<>();
        List<?> rawMobs = yaml.getList("mobs");
        if (rawMobs != null) {
            for (Object obj : rawMobs) {
                if (obj instanceof Map<?, ?> map) {
                    boolean mythic = Boolean.TRUE.equals(map.get("mythic"));
                    String id = String.valueOf(map.get("id"));
                    String customName = (String) map.get("display-name");
                    double chance = 0.0;
                    Object cObj = map.get("chance");
                    if (cObj instanceof Number n) chance = n.doubleValue();
                    mobPool.add(new SpawnerMobEntry(mythic, id, customName, chance));
                }
            }
        }

        int totalMobs = yaml.getInt("wave.total-mobs", 6);
        int simultaneousMobs = yaml.getInt("wave.simultaneous-mobs", 3);
        int spawnDelaySeconds = yaml.getInt("wave.spawn-delay-seconds", 2);
        int playerRange = yaml.getInt("wave.player-range", 14);

        String modeStr = yaml.getString("cooldown.mode", "GLOBAL_COOLDOWN");
        VaultCooldownMode mode = VaultCooldownMode.fromString(modeStr);
        int cooldownSeconds = yaml.contains("cooldown.seconds")
                ? yaml.getInt("cooldown.seconds")
                : yaml.getInt("cooldown.minutes", 30) * 60;
        int rollCount = yaml.getInt("roll-count", 2);

        List<LootItem> items = new ArrayList<>();
        List<?> rawItems = yaml.getList("items");
        if (rawItems != null) {
            for (Object obj : rawItems) {
                if (obj instanceof Map<?, ?> map) {
                    double chance = 0.0;
                    Object cObj = map.get("chance");
                    if (cObj instanceof Number n) chance = n.doubleValue();
                    boolean isAir = Boolean.TRUE.equals(map.get("is-air"));
                    boolean broadcast = Boolean.TRUE.equals(map.get("broadcast"));
                    String itemBroadcastMsg = (String) map.get("broadcast-message");
                    int limitServerDaily = (map.get("limit-server-daily") instanceof Number n) ? n.intValue() : 0;
                    int limitServerMonthly = (map.get("limit-server-monthly") instanceof Number n) ? n.intValue() : 0;
                    int limitPlayerDaily = (map.get("limit-player-daily") instanceof Number n) ? n.intValue() : 0;
                    int limitPlayerTotal = (map.get("limit-player-total") instanceof Number n) ? n.intValue() : 0;

                    ItemStack item = null;
                    Object iObj = map.get("item");
                    if (iObj instanceof ItemStack is) item = is;

                    LootItem lootItem;
                    if (isAir || item == null) {
                        lootItem = new LootItem(chance, true, broadcast, itemBroadcastMsg);
                    } else {
                        lootItem = new LootItem(item, chance, broadcast, itemBroadcastMsg);
                    }
                    lootItem.setLimitServerDaily(limitServerDaily);
                    lootItem.setLimitServerMonthly(limitServerMonthly);
                    lootItem.setLimitPlayerDaily(limitPlayerDaily);
                    lootItem.setLimitPlayerTotal(limitPlayerTotal);
                    items.add(lootItem);
                }
            }
        }

        boolean showActionBar = yaml.getBoolean("show-actionbar", true);
        boolean victorySoundEnabled = yaml.getBoolean("victory-sound.enabled", false);
        String victorySound = yaml.getString("victory-sound.sound", "UI_TOAST_CHALLENGE_COMPLETE");
        float victorySoundVolume = (float) yaml.getDouble("victory-sound.volume", 1.0);
        float victorySoundPitch = (float) yaml.getDouble("victory-sound.pitch", 1.2);

        String spawnModeStr = yaml.getString("spawn-mode", "SEQUENCE");
        clre20.customLootX.model.SpawnerSpawnMode spawnMode = clre20.customLootX.model.SpawnerSpawnMode.fromString(spawnModeStr);

        List<List<String>> waves = new ArrayList<>();
        if (yaml.isList("waves")) {
            List<?> rawWaves = yaml.getList("waves");
            if (rawWaves != null) {
                for (Object obj : rawWaves) {
                    if (obj instanceof List<?> l) {
                        List<String> w = new ArrayList<>();
                        for (Object o : l) {
                            if (o != null) w.add(String.valueOf(o));
                        }
                        waves.add(w);
                    }
                }
            }
        }
        if (waves.isEmpty()) {
            List<String> spawnSequence = yaml.getStringList("spawn-sequence");
            int sim = Math.max(1, simultaneousMobs);
            if (!spawnSequence.isEmpty()) {
                List<String> cur = new ArrayList<>();
                for (String s : spawnSequence) {
                    cur.add(s);
                    if (cur.size() >= sim) {
                        waves.add(new ArrayList<>(cur));
                        cur.clear();
                    }
                }
                if (!cur.isEmpty()) waves.add(cur);
            }
        }

        boolean waitWaveCleared = yaml.getBoolean("wave.wait-wave-cleared", true);
        String broadcastMessage = yaml.getString("broadcast-message", null);

        SpawnerTemplate st = new SpawnerTemplate(name, ominous, displayName, spawnedType, displayCycle, displayMobId,
                mobPool, spawnMode, waves,
                spawnDelaySeconds, playerRange, waitWaveCleared,
                showActionBar, victorySoundEnabled, victorySound, victorySoundVolume, victorySoundPitch,
                mode, Math.max(1, (int) Math.ceil((double) cooldownSeconds / 60.0)), rollCount, items, broadcastMessage);
        st.setCooldownSeconds(cooldownSeconds);
        return st;
    }
}
