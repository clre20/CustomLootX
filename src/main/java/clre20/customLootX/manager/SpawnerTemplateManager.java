package clre20.customLootX.manager;

import clre20.customLootX.CustomLootX;
import clre20.customLootX.model.LootItem;
import clre20.customLootX.model.SpawnerMobEntry;
import clre20.customLootX.model.SpawnerTemplate;
import clre20.customLootX.model.VaultCooldownMode;
import clre20.customLootX.util.TextUtil;
import clre20.customLootX.util.TrialSpawnerNmsUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.TrialSpawner;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 試煉生怪磚配置與運行期管理器
 */
public class SpawnerTemplateManager {

    private final CustomLootX plugin;
    private final File spawnerDir;
    private final File runtimeFile;
    private final Map<String, SpawnerTemplate> templates = new ConcurrentHashMap<>();

    // 運行期冷卻與紀錄：locationKey = "world:x:y:z"
    // 1. 個人冷卻：locationKey -> (UUID -> 到期時間戳 ms)
    private final Map<String, Map<UUID, Long>> playerCooldowns = new ConcurrentHashMap<>();
    // 2. 全域冷卻：locationKey -> 到期時間戳 ms
    private final Map<String, Long> globalCooldowns = new ConcurrentHashMap<>();
    // 3. 終生一次：locationKey -> Set<UUID>
    private final Map<String, Set<UUID>> rewardedPlayers = new ConcurrentHashMap<>();
    // 4. 已放置生怪磚追蹤：locationKey -> templateName
    private final Map<String, String> placedSpawners = new ConcurrentHashMap<>();
    // 5. 進行中的戰鬥進程：locationKey -> SpawnerBattleSession
    private final Map<String, SpawnerBattleSession> activeBattles = new ConcurrentHashMap<>();

    private BukkitTask tickerTask;
    private int previewRotationTick = 0;

    public SpawnerTemplateManager(CustomLootX plugin) {
        this.plugin = plugin;
        this.spawnerDir = new File(plugin.getDataFolder(), "data/spawner");
        if (!spawnerDir.exists()) {
            spawnerDir.mkdirs();
        }
        this.runtimeFile = new File(plugin.getDataFolder(), "data/spawner_runtime.yml");
        loadAll();
        loadRuntimeData();
    }

    public void loadAll() {
        clre20.customLootX.hook.MythicMobHook.resetStatus();
        templates.clear();
        if (!spawnerDir.exists()) {
            spawnerDir.mkdirs();
            return;
        }

        File[] files = spawnerDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            try {
                SpawnerTemplate template = loadFromFile(file);
                if (template != null) {
                    templates.put(template.getName().toLowerCase(), template);
                }
            } catch (Exception e) {
                plugin.logWarn("&5[試煉生怪磚·載入]&c 讀取試煉生怪磚配置檔案失敗: &e" + file.getName() + "&c - " + e.getMessage());
            }
        }
        plugin.logConsole("&5[試煉生怪磚·載入]&7 成功載入 &a" + templates.size() + "&7 個試煉生怪磚配置。");
    }

    private SpawnerTemplate loadFromFile(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String fileNameNoExt = file.getName().substring(0, file.getName().lastIndexOf('.'));
        String name = yaml.getString("name", fileNameNoExt);
        boolean ominous = yaml.getBoolean("ominous", false);
        String displayName = yaml.getString("display-name", name);

        String displayMobId = yaml.getString("display-mob-id", null);
        String mobTypeStr = yaml.getString("spawned-type", "ZOMBIE");

        EntityType spawnedType;
        try {
            spawnedType = EntityType.valueOf(mobTypeStr.toUpperCase());
        } catch (Exception e) {
            spawnedType = EntityType.ZOMBIE;
        }

        List<SpawnerMobEntry> mobPool = new ArrayList<>();
        if (yaml.isList("mobs")) {
            List<Map<?, ?>> mobList = yaml.getMapList("mobs");
            for (Map<?, ?> map : mobList) {
                boolean mythic = map.containsKey("mythic") && Boolean.parseBoolean(String.valueOf(map.get("mythic")));
                String id = map.containsKey("id") ? String.valueOf(map.get("id")) : "ZOMBIE";
                String customName = map.containsKey("display-name") ? String.valueOf(map.get("display-name")) : null;
                double chance = TextUtil.roundChance(map.containsKey("chance") ? ((Number) map.get("chance")).doubleValue() : 0.0);
                mobPool.add(new SpawnerMobEntry(mythic, id, customName, chance));
            }
        }
        if (mobPool.isEmpty()) {
            mobPool.add(new SpawnerMobEntry(spawnedType, 100.0));
        }

        boolean displayCycle;
        if (yaml.contains("display-cycle")) {
            displayCycle = yaml.getBoolean("display-cycle", false);
        } else {
            // 舊配置相容或未指定時：若怪物池多於 1 隻且未指定特定怪物，預設開啟循環輪播！
            displayCycle = (mobPool.size() > 1 && displayMobId == null);
        }
        if ("CYCLE".equalsIgnoreCase(mobTypeStr) || "CYCLE".equalsIgnoreCase(displayMobId)) {
            displayCycle = true;
        }

        int totalMobs = Math.max(1, yaml.getInt("wave.total-mobs", 6));
        int simultaneousMobs = Math.max(1, Math.min(16, yaml.getInt("wave.simultaneous-mobs", 2)));
        int spawnDelaySeconds = Math.max(1, yaml.getInt("wave.spawn-delay-seconds", 3));
        int playerRange = Math.max(4, Math.min(48, yaml.getInt("wave.player-range", 14)));

        String modeStr = yaml.getString("cooldown.mode", "PLAYER_COOLDOWN");
        VaultCooldownMode mode;
        try {
            mode = VaultCooldownMode.valueOf(modeStr.toUpperCase());
        } catch (Exception e) {
            mode = VaultCooldownMode.PLAYER_COOLDOWN;
        }
        int cooldownMinutes = Math.max(1, yaml.getInt("cooldown.minutes", 15));
        int rollCount = Math.max(1, yaml.getInt("roll-count", 3));

        List<LootItem> items = new ArrayList<>();
        if (yaml.isList("rewards")) {
            List<Map<?, ?>> list = yaml.getMapList("rewards");
            for (Map<?, ?> map : list) {
                double chance = TextUtil.roundChance(map.containsKey("chance") ? ((Number) map.get("chance")).doubleValue() : 0.0);
                boolean isAir = map.containsKey("is-air") && Boolean.parseBoolean(String.valueOf(map.get("is-air")));
                ItemStack item = null;
                if (!isAir && map.containsKey("item")) {
                    Object itemObj = map.get("item");
                    if (itemObj instanceof ItemStack is) {
                        item = is;
                    }
                }
                if (isAir || item == null) {
                    items.add(new LootItem(chance, true));
                } else {
                    items.add(new LootItem(item, chance));
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

        return new SpawnerTemplate(name, ominous, displayName, spawnedType, displayCycle, displayMobId,
                mobPool, spawnMode, waves,
                spawnDelaySeconds, playerRange, waitWaveCleared,
                showActionBar, victorySoundEnabled, victorySound, victorySoundVolume, victorySoundPitch,
                mode, cooldownMinutes, rollCount, items);
    }

    public boolean saveTemplate(SpawnerTemplate template) {
        if (template == null || template.getName() == null || template.getName().trim().isEmpty()) {
            return false;
        }
        String safeName = template.getName().trim();
        File file = new File(spawnerDir, safeName + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();

        yaml.set("name", safeName);
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
        yaml.set("cooldown.minutes", template.getCooldownMinutes());
        yaml.set("roll-count", template.getRollCount());

        List<Map<String, Object>> list = new ArrayList<>();
        for (LootItem loot : template.getRewards()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("chance", TextUtil.roundChance(loot.getChance()));
            if (loot.isAir() || loot.getItem() == null) {
                map.put("is-air", true);
            } else {
                map.put("item", loot.getItem());
            }
            list.add(map);
        }
        yaml.set("rewards", list);

        try {
            yaml.save(file);
            templates.put(safeName.toLowerCase(), template);
            return true;
        } catch (IOException e) {
            plugin.logError("&5[試煉生怪磚·儲存]&c 儲存試煉生怪磚配置失敗: &e" + safeName + " &c- " + e.getMessage());
            return false;
        }
    }

    public boolean deleteTemplate(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase();
        SpawnerTemplate removed = templates.remove(lower);
        File file = new File(spawnerDir, lower + ".yml");
        if (file.exists()) {
            return file.delete();
        }
        return removed != null;
    }

    public SpawnerTemplate getTemplate(String name) {
        if (name == null) return null;
        return templates.get(name.toLowerCase());
    }

    public Collection<SpawnerTemplate> getAllTemplates() {
        return Collections.unmodifiableCollection(templates.values());
    }

    public Set<String> getTemplateNames() {
        return Collections.unmodifiableSet(templates.keySet());
    }

    // ==========================================
    // 冷卻與領取檢查邏輯
    // ==========================================

    public String toLocationKey(Location loc) {
        if (loc == null) return "";
        return (loc.getWorld() != null ? loc.getWorld().getName() : "world") + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    /**
     * 檢查玩家是否可以觸發該試煉生怪磚
     * @return 0 代表可以觸發；大於 0 代表仍在冷卻中（剩餘秒數）；-1 代表終生一次已領取過
     */
    public long checkCooldownStatus(Location loc, UUID playerUuid, SpawnerTemplate template) {
        if (loc == null || playerUuid == null || template == null) return 0;
        String key = toLocationKey(loc);
        long now = System.currentTimeMillis();

        VaultCooldownMode mode = template.getCooldownMode();
        switch (mode) {
            case ONCE_PER_PLAYER -> {
                Set<UUID> set = rewardedPlayers.get(key);
                if (set != null && set.contains(playerUuid)) {
                    return -1; // 終生已完成過
                }
                return 0;
            }
            case GLOBAL_COOLDOWN -> {
                Long expire = globalCooldowns.get(key);
                if (expire != null && expire > now) {
                    return Math.max(1, (expire - now) / 1000);
                }
                return 0;
            }
            case PLAYER_COOLDOWN -> {
                Map<UUID, Long> map = playerCooldowns.get(key);
                if (map != null) {
                    Long expire = map.get(playerUuid);
                    if (expire != null && expire > now) {
                        return Math.max(1, (expire - now) / 1000);
                    }
                }
                return 0;
            }
        }
        return 0;
    }

    public boolean hasActiveBattle(String locKey) {
        return locKey != null && activeBattles.containsKey(locKey);
    }

    public boolean isSpawnerInCooldown(String locKey, SpawnerTemplate template) {
        if (locKey == null || template == null) return false;
        long now = System.currentTimeMillis();
        switch (template.getCooldownMode()) {
            case GLOBAL_COOLDOWN -> {
                Long expire = globalCooldowns.get(locKey);
                return expire != null && expire > now;
            }
            case PLAYER_COOLDOWN -> {
                Map<UUID, Long> map = playerCooldowns.get(locKey);
                if (map != null && !map.isEmpty()) {
                    for (long exp : map.values()) {
                        if (exp > now) return true;
                    }
                }
                return false;
            }
            case ONCE_PER_PLAYER -> {
                Set<UUID> set = rewardedPlayers.get(locKey);
                return set != null && !set.isEmpty();
            }
        }
        return false;
    }

    public long getRemainingCooldownSeconds(String locKey, SpawnerTemplate template) {
        if (locKey == null || template == null) return 0;
        long now = System.currentTimeMillis();
        switch (template.getCooldownMode()) {
            case GLOBAL_COOLDOWN -> {
                Long expire = globalCooldowns.get(locKey);
                return (expire != null && expire > now) ? Math.max(1, (expire - now) / 1000) : 0;
            }
            case PLAYER_COOLDOWN -> {
                Map<UUID, Long> map = playerCooldowns.get(locKey);
                if (map != null && !map.isEmpty()) {
                    long maxExp = 0;
                    for (long exp : map.values()) {
                        if (exp > maxExp) maxExp = exp;
                    }
                    return maxExp > now ? Math.max(1, (maxExp - now) / 1000) : 0;
                }
                return 0;
            }
            case ONCE_PER_PLAYER -> {
                Set<UUID> set = rewardedPlayers.get(locKey);
                return (set != null && !set.isEmpty()) ? 3600 : 0;
            }
        }
        return 0;
    }

    /**
     * 記錄試煉戰鬥勝利，寫入冷卻或領取紀錄
     */
    public void recordCompletion(Location loc, Collection<UUID> playerUuids, SpawnerTemplate template) {
        if (loc == null || template == null || playerUuids == null) return;
        String key = toLocationKey(loc);
        long now = System.currentTimeMillis();
        long expireTime = now + (long) template.getCooldownMinutes() * 60L * 1000L;

        switch (template.getCooldownMode()) {
            case ONCE_PER_PLAYER -> {
                rewardedPlayers.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).addAll(playerUuids);
            }
            case GLOBAL_COOLDOWN -> {
                globalCooldowns.put(key, expireTime);
            }
            case PLAYER_COOLDOWN -> {
                Map<UUID, Long> map = playerCooldowns.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
                for (UUID uuid : playerUuids) {
                    map.put(uuid, expireTime);
                }
            }
        }
        saveRuntimeData();
    }

    public void clearCooldown(Location loc) {
        if (loc == null) return;
        String key = toLocationKey(loc);
        playerCooldowns.remove(key);
        globalCooldowns.remove(key);
        rewardedPlayers.remove(key);
        saveRuntimeData();
    }

    // ==========================================
    // 已放置生怪磚方塊追蹤與同步
    // ==========================================

    public void registerSpawner(Location loc, String templateName) {
        if (loc == null || templateName == null) return;
        String key = toLocationKey(loc);
        placedSpawners.put(key, templateName);
        saveRuntimeData();
    }

    public void unregisterSpawner(Location loc) {
        if (loc == null) return;
        String key = toLocationKey(loc);
        cancelBattleSession(key);
        placedSpawners.remove(key);
        saveRuntimeData();
    }

    public Location parseLocation(String locKey) {
        if (locKey == null || locKey.isEmpty()) return null;
        String[] parts = locKey.split(":");
        if (parts.length != 4) return null;
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;
        try {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            return new Location(world, x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 當配置儲存（例如切換為不祥模式或修改怪物）時，立即同步世界上所有已載入的同名生怪磚方塊
     */
    public void updatePlacedSpawnerBlocks(SpawnerTemplate template) {
        if (template == null) return;
        for (Map.Entry<String, String> entry : placedSpawners.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(template.getName())) {
                Location loc = parseLocation(entry.getKey());
                if (loc != null && loc.getWorld() != null && loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                    Block block = loc.getBlock();
                    if (block.getType() == Material.TRIAL_SPAWNER) {
                        syncSpawnerBlock(block, template.getName());
                    }
                }
            }
        }
    }

    /**
     * 同步單一生怪磚方塊的不祥外觀、TileState 與內部預覽怪物模型
     */
    public void syncSpawnerBlock(Block block, String templateName) {
        if (block == null || block.getType() != Material.TRIAL_SPAWNER) return;
        SpawnerTemplate template = getTemplate(templateName);
        if (template == null) return;

        Location loc = block.getLocation();
        String locKey = toLocationKey(loc);
        placedSpawners.put(locKey, template.getName());

        boolean inCooldown = isSpawnerInCooldown(locKey, template);
        long cdSeconds = getRemainingCooldownSeconds(locKey, template);
        boolean inBattle = activeBattles.containsKey(locKey);

        // 1. 同步 BlockData (不祥狀態與視覺狀態)
        if (block.getBlockData() instanceof org.bukkit.block.data.type.TrialSpawner spawnerData) {
            spawnerData.setOminous(template.isOminous());
            if (inBattle) {
                spawnerData.setTrialSpawnerState(org.bukkit.block.data.type.TrialSpawner.State.ACTIVE);
            } else if (inCooldown) {
                spawnerData.setTrialSpawnerState(org.bukkit.block.data.type.TrialSpawner.State.COOLDOWN);
            } else {
                spawnerData.setTrialSpawnerState(org.bukkit.block.data.type.TrialSpawner.State.WAITING_FOR_PLAYERS);
            }
            block.setBlockData(spawnerData, true);
        }

        // 2. 同步 TileState (3D 旋轉實體預覽模型、感應範圍、冷卻時間與 PDC)
        if (block.getState() instanceof TrialSpawner tsState) {
            tsState.setOminous(template.isOminous());
            tsState.setRequiredPlayerRange(template.getPlayerRange());

            if (inCooldown) {
                long cdTicks = Math.max(20L, cdSeconds * 20L);
                tsState.setCooldownLength((int) Math.min(Integer.MAX_VALUE, cdTicks));
                tsState.setCooldownEnd(block.getWorld().getGameTime() + cdTicks);
                for (Player p : new ArrayList<>(tsState.getTrackedPlayers())) {
                    tsState.stopTrackingPlayer(p);
                }
            } else if (!inBattle) {
                tsState.setCooldownEnd(0);
                for (Player p : new ArrayList<>(tsState.getTrackedPlayers())) {
                    tsState.stopTrackingPlayer(p);
                }
            }

            // 設置籠內旋轉怪物實體外觀與防原版衝突配置
            try {
                tsState.getNormalConfiguration().setSpawnedType(template.getSpawnedType());
                tsState.getOminousConfiguration().setSpawnedType(template.getSpawnedType());
                // 原版生怪數設為超大值，延遲設為最大，清空原版自帶獎勵池，由 CustomLootX 全權驅動精準波次戰鬥
                tsState.getNormalConfiguration().setBaseSpawnsBeforeCooldown(999999f);
                tsState.getNormalConfiguration().setBaseSimultaneousEntities(0f);
                tsState.getNormalConfiguration().setDelay(Integer.MAX_VALUE);
                tsState.getNormalConfiguration().setPossibleRewards(Collections.emptyMap());

                tsState.getOminousConfiguration().setBaseSpawnsBeforeCooldown(999999f);
                tsState.getOminousConfiguration().setBaseSimultaneousEntities(0f);
                tsState.getOminousConfiguration().setDelay(Integer.MAX_VALUE);
                tsState.getOminousConfiguration().setPossibleRewards(Collections.emptyMap());
            } catch (Exception ignored) {}

            tsState.getPersistentDataContainer().set(plugin.getItemManager().KEY_CUSTOM_SPAWNER, PersistentDataType.BYTE, (byte) 1);
            tsState.getPersistentDataContainer().set(plugin.getItemManager().KEY_SPAWNER_TEMPLATE_NAME, PersistentDataType.STRING, template.getName());
            tsState.update(true, false);

            TrialSpawnerNmsUtil.setSpawnerMob(block, template.getSpawnedType(), template.isOminous(), plugin.getLogger());
        }
    }

    /**
     * 當區塊載入時，同步區塊內已放置的自訂生怪磚方塊
     */
    public void onChunkLoad(Chunk chunk) {
        if (chunk == null) return;
        int cx = chunk.getX();
        int cz = chunk.getZ();
        String worldName = chunk.getWorld().getName();

        for (Map.Entry<String, String> entry : placedSpawners.entrySet()) {
            String locKey = entry.getKey();
            String[] parts = locKey.split(":");
            if (parts.length == 4 && parts[0].equalsIgnoreCase(worldName)) {
                try {
                    int x = Integer.parseInt(parts[1]);
                    int z = Integer.parseInt(parts[3]);
                    if ((x >> 4) == cx && (z >> 4) == cz) {
                        int y = Integer.parseInt(parts[2]);
                        Block block = chunk.getWorld().getBlockAt(x, y, z);
                        syncSpawnerBlock(block, entry.getValue());
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    // ==========================================
    // 戰鬥進程管理
    // ==========================================

    public SpawnerBattleSession getBattleSession(String locKey) {
        return activeBattles.get(locKey);
    }

    public void startBattleSession(Location loc, SpawnerTemplate template, Collection<Player> players) {
        String key = toLocationKey(loc);
        if (activeBattles.containsKey(key)) return;

        SpawnerBattleSession session = new SpawnerBattleSession(plugin, this, key, loc, template, players);
        activeBattles.put(key, session);
        session.start();
    }

    public void endBattleSession(String locKey) {
        activeBattles.remove(locKey);
    }

    public void cancelBattleSession(String locKey) {
        SpawnerBattleSession session = activeBattles.remove(locKey);
        if (session != null) {
            session.cancel("戰鬥已中止。");
        }
    }

    public void cancelAllBattles() {
        for (SpawnerBattleSession session : activeBattles.values()) {
            session.cancel("插件關閉，戰鬥已中止。");
        }
        activeBattles.clear();
    }

    public void handleMobKilled(String locKey, UUID mobUuid) {
        if (locKey == null || mobUuid == null) return;
        SpawnerBattleSession session = activeBattles.get(locKey);
        if (session != null) {
            session.onMobKilled(mobUuid);
        }
    }

    // ==========================================
    // 心跳檢測計時器：冷卻更新、玩家感應與戰鬥進程
    // ==========================================

    public void startTicker() {
        if (tickerTask != null) {
            tickerTask.cancel();
        }
        tickerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickAll, 20L, 20L);
    }

    public void stopTicker() {
        if (tickerTask != null) {
            tickerTask.cancel();
            tickerTask = null;
        }
        cancelAllBattles();
    }

    public void tickAll() {
        // 1. 心跳檢測所有進行中的戰鬥進程
        for (SpawnerBattleSession session : new ArrayList<>(activeBattles.values())) {
            session.tick();
        }

        // 2. 檢測已放置的生怪磚，是否有玩家接近並符合戰鬥啟動條件
        tickPlayerDetection();

        // 3. 檢測並更新冷卻到期狀態
        tickCooldowns();

        // 4. 籠內 3D 旋轉實體預覽輪播 (每 3 秒輪替怪物池所有生物，如同寶庫展示物品輪播)
        previewRotationTick++;
        if (previewRotationTick % 3 == 0) {
            tickPreviewRotation();
        }

        // 5. 確保未在戰鬥中的生怪磚維持正確狀態 (若在冷卻中強制維持 COOLDOWN，若就緒則維持 WAITING_FOR_PLAYERS)
        tickSpawnerStateGuard();
    }

    private void tickSpawnerStateGuard() {
        for (Map.Entry<String, String> entry : placedSpawners.entrySet()) {
            String locKey = entry.getKey();
            if (activeBattles.containsKey(locKey)) {
                continue; // 正在進行 CustomLootX 戰鬥
            }

            SpawnerTemplate template = getTemplate(entry.getValue());
            if (template == null) continue;

            Location loc = parseLocation(locKey);
            if (loc == null || loc.getWorld() == null || !loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                continue;
            }

            Block block = loc.getBlock();
            if (block.getType() != Material.TRIAL_SPAWNER) continue;

            boolean inCd = isSpawnerInCooldown(locKey, template);
            if (block.getBlockData() instanceof org.bukkit.block.data.type.TrialSpawner data) {
                boolean dataChanged = false;

                // 核心防護 1：嚴格維持不祥外觀與方塊屬性 (防止原版在戰鬥結算後偷竄改為普通樣式)
                if (data.isOminous() != template.isOminous()) {
                    data.setOminous(template.isOminous());
                    dataChanged = true;
                }

                org.bukkit.block.data.type.TrialSpawner.State expectedState = inCd
                        ? org.bukkit.block.data.type.TrialSpawner.State.COOLDOWN
                        : org.bukkit.block.data.type.TrialSpawner.State.WAITING_FOR_PLAYERS;

                if (data.getTrialSpawnerState() != expectedState) {
                    data.setTrialSpawnerState(expectedState);
                    dataChanged = true;
                }

                if (dataChanged) {
                    block.setBlockData(data, true);
                }

                // 核心防護 2：TileState 不祥屬性、冷卻時間與清空原版追蹤玩家 (防止靠近偽觸發)
                if (block.getState() instanceof TrialSpawner ts) {
                    boolean stateChanged = false;

                    if (ts.isOminous() != template.isOminous()) {
                        ts.setOminous(template.isOminous());
                        stateChanged = true;
                    }

                    if (!ts.getTrackedPlayers().isEmpty()) {
                        for (Player p : new ArrayList<>(ts.getTrackedPlayers())) {
                            ts.stopTrackingPlayer(p);
                        }
                    }

                    if (inCd) {
                        long remain = getRemainingCooldownSeconds(locKey, template);
                        long cdTicks = Math.max(20L, remain * 20L);
                        long targetEnd = loc.getWorld().getGameTime() + cdTicks;
                        if (Math.abs(ts.getCooldownEnd() - targetEnd) > 40L) {
                            ts.setCooldownLength((int) Math.min(Integer.MAX_VALUE, cdTicks));
                            ts.setCooldownEnd(targetEnd);
                            stateChanged = true;
                        }
                    } else {
                        if (ts.getCooldownEnd() != 0) {
                            ts.setCooldownEnd(0);
                            stateChanged = true;
                        }
                    }

                    if (stateChanged) {
                        ts.update(true, false);
                    }
                }
            }
        }
    }

    private void tickPreviewRotation() {
        for (Map.Entry<String, String> entry : placedSpawners.entrySet()) {
            String locKey = entry.getKey();
            if (activeBattles.containsKey(locKey)) {
                continue; // 戰鬥進行中，保持原狀不進行預覽輪播
            }

            SpawnerTemplate template = getTemplate(entry.getValue());
            if (template == null) continue;
            // 只有設定為【循環】時才進行輪播，若為固定特定怪物則保持固定展示！
            if (!template.isDisplayCycle()) continue;

            List<SpawnerMobEntry> pool = template.getMobPool();
            if (pool.size() <= 1) continue;

            Location loc = parseLocation(locKey);
            if (loc == null || loc.getWorld() == null || !loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                continue;
            }

            Block block = loc.getBlock();
            if (block.getType() != Material.TRIAL_SPAWNER) continue;

            int index = (previewRotationTick / 3) % pool.size();
            SpawnerMobEntry currentEntry = pool.get(index);
            EntityType targetType = currentEntry.getPreviewEntityType();
            if (targetType == null) continue;

            TrialSpawnerNmsUtil.setSpawnerMob(block, targetType, template.isOminous(), plugin.getLogger());
        }
    }

    private void tickPlayerDetection() {
        for (Map.Entry<String, String> entry : placedSpawners.entrySet()) {
            String locKey = entry.getKey();
            if (activeBattles.containsKey(locKey)) {
                continue; // 已經在戰鬥中，略過
            }

            SpawnerTemplate template = getTemplate(entry.getValue());
            if (template == null) continue;

            Location loc = parseLocation(locKey);
            if (loc == null || loc.getWorld() == null || !loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                continue;
            }

            // 檢查全域冷卻
            if (template.getCooldownMode() == VaultCooldownMode.GLOBAL_COOLDOWN) {
                Long expire = globalCooldowns.get(locKey);
                if (expire != null && expire > System.currentTimeMillis()) {
                    continue; // 仍在全域冷卻中
                }
            }

            // 搜尋感應範圍內的合格玩家
            double rangeSq = Math.pow(template.getPlayerRange(), 2);
            List<Player> eligiblePlayers = new ArrayList<>();

            for (Player p : loc.getWorld().getPlayers()) {
                if (p.isDead() || !p.isValid()) continue;
                GameMode gm = p.getGameMode();
                if (gm != GameMode.SURVIVAL && gm != GameMode.ADVENTURE) continue;

                if (p.getWorld().equals(loc.getWorld()) && p.getLocation().distanceSquared(loc) <= rangeSq) {
                    // 檢查玩家冷卻資格
                    if (checkCooldownStatus(loc, p.getUniqueId(), template) == 0) {
                        eligiblePlayers.add(p);
                    }
                }
            }

            // 若有合格玩家接近，立即開戰！
            if (!eligiblePlayers.isEmpty()) {
                startBattleSession(loc, template, eligiblePlayers);
            }
        }
    }

    public void tickCooldowns() {
        long now = System.currentTimeMillis();
        boolean changed = false;

        // 1. 全域冷卻檢查
        Iterator<Map.Entry<String, Long>> gIt = globalCooldowns.entrySet().iterator();
        while (gIt.hasNext()) {
            Map.Entry<String, Long> entry = gIt.next();
            if (entry.getValue() <= now) {
                String locKey = entry.getKey();
                gIt.remove();
                changed = true;
                handleCooldownExpired(locKey);
            }
        }

        // 2. 個人冷卻檢查
        Iterator<Map.Entry<String, Map<UUID, Long>>> pIt = playerCooldowns.entrySet().iterator();
        while (pIt.hasNext()) {
            Map.Entry<String, Map<UUID, Long>> entry = pIt.next();
            String locKey = entry.getKey();
            Map<UUID, Long> map = entry.getValue();
            boolean anyExpired = false;
            Iterator<Map.Entry<UUID, Long>> subIt = map.entrySet().iterator();
            while (subIt.hasNext()) {
                if (subIt.next().getValue() <= now) {
                    subIt.remove();
                    anyExpired = true;
                    changed = true;
                }
            }
            if (anyExpired && !activeBattles.containsKey(locKey)) {
                handleCooldownExpired(locKey);
            }
            if (map.isEmpty()) {
                pIt.remove();
            }
        }

        if (changed) {
            saveRuntimeData();
        }
    }

    private void handleCooldownExpired(String locKey) {
        Location loc = parseLocation(locKey);
        if (loc == null || loc.getWorld() == null) return;
        if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) return;

        Block block = loc.getBlock();
        if (block.getType() != Material.TRIAL_SPAWNER) return;

        String templateName = placedSpawners.get(locKey);
        if (templateName == null) return;
        SpawnerTemplate template = getTemplate(templateName);
        if (template == null) return;

        // 恢復方塊狀態為等待玩家
        if (block.getBlockData() instanceof org.bukkit.block.data.type.TrialSpawner spawnerData) {
            spawnerData.setOminous(template.isOminous());
            spawnerData.setTrialSpawnerState(org.bukkit.block.data.type.TrialSpawner.State.WAITING_FOR_PLAYERS);
            block.setBlockData(spawnerData, true);
        }

        if (block.getState() instanceof TrialSpawner tsState) {
            tsState.setOminous(template.isOminous());
            tsState.setCooldownEnd(0);
            for (Player p : new ArrayList<>(tsState.getTrackedPlayers())) {
                tsState.stopTrackingPlayer(p);
            }
            tsState.update(true, false);
        }

        World world = loc.getWorld();
        Sound sound = template.isOminous() ? Sound.BLOCK_TRIAL_SPAWNER_AMBIENT_OMINOUS : Sound.BLOCK_TRIAL_SPAWNER_AMBIENT;
        world.playSound(loc, sound, 1.0f, 1.0f);

        Particle particle = template.isOminous() ? Particle.TRIAL_SPAWNER_DETECTION_OMINOUS : Particle.TRIAL_SPAWNER_DETECTION;
        world.spawnParticle(particle, loc.clone().add(0.5, 0.5, 0.5), 15, 0.3, 0.3, 0.3, 0.05);
    }

    // ==========================================
    // 運行期冷卻與紀錄持久化
    // ==========================================

    public void loadRuntimeData() {
        if (!runtimeFile.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(runtimeFile);

        // 載入已放置生怪磚
        placedSpawners.clear();
        if (yaml.isConfigurationSection("placed")) {
            for (String key : yaml.getConfigurationSection("placed").getKeys(false)) {
                String val = yaml.getString("placed." + key);
                if (val != null) {
                    placedSpawners.put(key, val);
                }
            }
        }

        // 載入全域冷卻
        globalCooldowns.clear();
        if (yaml.isConfigurationSection("global_cooldowns")) {
            for (String key : yaml.getConfigurationSection("global_cooldowns").getKeys(false)) {
                long val = yaml.getLong("global_cooldowns." + key);
                if (val > System.currentTimeMillis()) {
                    globalCooldowns.put(key, val);
                }
            }
        }

        // 載入個人冷卻
        playerCooldowns.clear();
        if (yaml.isConfigurationSection("player_cooldowns")) {
            for (String locKey : yaml.getConfigurationSection("player_cooldowns").getKeys(false)) {
                Map<UUID, Long> map = new ConcurrentHashMap<>();
                for (String uuidStr : yaml.getConfigurationSection("player_cooldowns." + locKey).getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        long expire = yaml.getLong("player_cooldowns." + locKey + "." + uuidStr);
                        if (expire > System.currentTimeMillis()) {
                            map.put(uuid, expire);
                        }
                    } catch (Exception ignored) {}
                }
                if (!map.isEmpty()) {
                    playerCooldowns.put(locKey, map);
                }
            }
        }

        // 載入終生一次名單
        rewardedPlayers.clear();
        if (yaml.isConfigurationSection("rewarded_players")) {
            for (String locKey : yaml.getConfigurationSection("rewarded_players").getKeys(false)) {
                List<String> list = yaml.getStringList("rewarded_players." + locKey);
                Set<UUID> set = ConcurrentHashMap.newKeySet();
                for (String s : list) {
                    try {
                        set.add(UUID.fromString(s));
                    } catch (Exception ignored) {}
                }
                if (!set.isEmpty()) {
                    rewardedPlayers.put(locKey, set);
                }
            }
        }
    }

    public void saveRuntimeData() {
        YamlConfiguration yaml = new YamlConfiguration();

        // 儲存已放置生怪磚
        for (Map.Entry<String, String> entry : placedSpawners.entrySet()) {
            yaml.set("placed." + entry.getKey(), entry.getValue());
        }

        // 儲存全域冷卻
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : globalCooldowns.entrySet()) {
            if (entry.getValue() > now) {
                yaml.set("global_cooldowns." + entry.getKey(), entry.getValue());
            }
        }

        // 儲存個人冷卻
        for (Map.Entry<String, Map<UUID, Long>> entry : playerCooldowns.entrySet()) {
            for (Map.Entry<UUID, Long> sub : entry.getValue().entrySet()) {
                if (sub.getValue() > now) {
                    yaml.set("player_cooldowns." + entry.getKey() + "." + sub.getKey().toString(), sub.getValue());
                }
            }
        }

        // 儲存終生一次名單
        for (Map.Entry<String, Set<UUID>> entry : rewardedPlayers.entrySet()) {
            List<String> list = new ArrayList<>();
            for (UUID u : entry.getValue()) {
                list.add(u.toString());
            }
            yaml.set("rewarded_players." + entry.getKey(), list);
        }

        try {
            yaml.save(runtimeFile);
        } catch (IOException e) {
            plugin.logWarn("&5[試煉生怪磚·資料]&c 儲存試煉生怪磚運行期資料失敗: " + e.getMessage());
        }
    }
}
