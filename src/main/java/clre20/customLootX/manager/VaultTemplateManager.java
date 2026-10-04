package clre20.customLootX.manager;

import clre20.customLootX.CustomLootX;
import clre20.customLootX.model.LootItem;
import clre20.customLootX.model.VaultCooldownMode;
import clre20.customLootX.model.VaultTemplate;
import clre20.customLootX.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Vault;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VaultTemplateManager {

    private final CustomLootX plugin;
    private final File vaultDir;
    private final File runtimeFile;
    private final Map<String, VaultTemplate> templates = new ConcurrentHashMap<>();

    // 運行期冷卻與紀錄：locationKey = "world:x:y:z"
    // 1. 個人冷卻：locationKey -> (UUID -> 到期時間戳 ms)
    private final Map<String, Map<UUID, Long>> playerCooldowns = new ConcurrentHashMap<>();
    // 2. 全域冷卻：locationKey -> 到期時間戳 ms
    private final Map<String, Long> globalCooldowns = new ConcurrentHashMap<>();
    // 3. 終生一次：locationKey -> Set<UUID>
    private final Map<String, Set<UUID>> rewardedPlayers = new ConcurrentHashMap<>();
    // 4. 已放置寶庫追蹤：locationKey -> templateName
    private final Map<String, String> placedVaults = new ConcurrentHashMap<>();
    // 快取解析後的 Location，避免每秒心跳重複分割字串與配置 Location 物件
    private final Map<String, Location> locationCache = new ConcurrentHashMap<>();
    // 5. 正在開獎噴發戰利品中的寶庫：locationKey
    private final Set<String> ejectingVaults = ConcurrentHashMap.newKeySet();
    private BukkitTask tickerTask;

    public VaultTemplateManager(CustomLootX plugin) {
        this.plugin = plugin;
        this.vaultDir = new File(plugin.getDataFolder(), "data/vault");
        if (!vaultDir.exists()) {
            vaultDir.mkdirs();
        }
        this.runtimeFile = new File(plugin.getDataFolder(), "data/vault_runtime.yml");
        loadAll();
        loadRuntimeData();
    }

    public void loadAll() {
        templates.clear();
        if (!vaultDir.exists()) {
            vaultDir.mkdirs();
            return;
        }

        File[] files = vaultDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            try {
                VaultTemplate template = loadFromFile(file);
                if (template != null) {
                    templates.put(template.getName().toLowerCase(), template);
                }
            } catch (Exception e) {
                plugin.logWarn("&6[試煉寶庫·載入]&c 讀取試煉寶庫配置檔案失敗: &e" + file.getName() + "&c - " + e.getMessage());
            }
        }
        plugin.logConsole("&6[試煉寶庫·載入]&7 成功載入 &a" + templates.size() + "&7 個試煉寶庫配置。");
    }

    private VaultTemplate loadFromFile(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String fileNameNoExt = file.getName().substring(0, file.getName().lastIndexOf('.'));
        String name = yaml.getString("name", fileNameNoExt);
        boolean ominous = yaml.getBoolean("ominous", false);
        String displayName = yaml.getString("display-name", name);

        ItemStack keyItem = yaml.getItemStack("key-item");
        if (keyItem == null) {
            keyItem = new ItemStack(ominous ? Material.OMINOUS_TRIAL_KEY : Material.TRIAL_KEY);
        }

        int rollCount = Math.max(1, yaml.getInt("roll-count", 3));
        String modeStr = yaml.getString("cooldown.mode", "PLAYER_COOLDOWN");
        VaultCooldownMode mode;
        try {
            mode = VaultCooldownMode.valueOf(modeStr.toUpperCase());
        } catch (Exception e) {
            mode = VaultCooldownMode.PLAYER_COOLDOWN;
        }
        int cooldownMinutes = Math.max(1, yaml.getInt("cooldown.minutes", 10));

        List<LootItem> items = new ArrayList<>();
        if (yaml.isList("items")) {
            List<Map<?, ?>> list = yaml.getMapList("items");
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

        return new VaultTemplate(name, ominous, displayName, keyItem, rollCount, mode, cooldownMinutes, items);
    }

    public boolean saveTemplate(VaultTemplate template) {
        if (template == null || template.getName() == null || template.getName().trim().isEmpty()) {
            return false;
        }
        String safeName = template.getName().trim();
        File file = new File(vaultDir, safeName + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();

        yaml.set("name", safeName);
        yaml.set("ominous", template.isOminous());
        yaml.set("display-name", template.getDisplayName());
        yaml.set("key-item", template.getKeyItem());
        yaml.set("roll-count", template.getRollCount());
        yaml.set("cooldown.mode", template.getCooldownMode().name());
        yaml.set("cooldown.minutes", template.getCooldownMinutes());

        List<Map<String, Object>> list = new ArrayList<>();
        for (LootItem loot : template.getItems()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("chance", TextUtil.roundChance(loot.getChance()));
            if (loot.isAir() || loot.getItem() == null) {
                map.put("is-air", true);
            } else {
                map.put("item", loot.getItem());
            }
            list.add(map);
        }
        yaml.set("items", list);

        try {
            yaml.save(file);
            templates.put(safeName.toLowerCase(), template);
            return true;
        } catch (IOException e) {
            plugin.logError("&6[試煉寶庫·儲存]&c 儲存試煉寶庫配置失敗: &e" + safeName + " &c- " + e.getMessage());
            return false;
        }
    }

    public boolean deleteTemplate(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase();
        VaultTemplate removed = templates.remove(lower);
        File file = new File(vaultDir, lower + ".yml");
        if (file.exists()) {
            return file.delete();
        }
        return removed != null;
    }

    public VaultTemplate getTemplate(String name) {
        if (name == null) return null;
        return templates.get(name.toLowerCase());
    }

    public Collection<VaultTemplate> getAllTemplates() {
        return Collections.unmodifiableCollection(templates.values());
    }

    public Set<String> getTemplateNames() {
        return Collections.unmodifiableSet(templates.keySet());
    }

    // ==========================================
    // 冷卻與領取檢查邏輯
    // ==========================================

    private String toLocationKey(Location loc) {
        if (loc == null) return "";
        return (loc.getWorld() != null ? loc.getWorld().getName() : "world") + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    /**
     * 檢查玩家是否可以開啟該寶庫
     * @return 0 代表可以開啟；大於 0 代表仍在冷卻中（剩餘秒數）；-1 代表終生一次已領取過
     */
    public long checkCooldownStatus(Location loc, UUID playerUuid, VaultTemplate template) {
        if (loc == null || playerUuid == null || template == null) return 0;
        String key = toLocationKey(loc);
        long now = System.currentTimeMillis();

        VaultCooldownMode mode = template.getCooldownMode();
        switch (mode) {
            case ONCE_PER_PLAYER -> {
                Set<UUID> set = rewardedPlayers.get(key);
                if (set != null && set.contains(playerUuid)) {
                    return -1; // 終生已領過
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

    public boolean isGlobalCooldownActive(Location loc, VaultTemplate template) {
        if (loc == null || template == null) return false;
        if (template.getCooldownMode() != VaultCooldownMode.GLOBAL_COOLDOWN) return false;
        String key = toLocationKey(loc);
        Long expire = globalCooldowns.get(key);
        return expire != null && expire > System.currentTimeMillis();
    }

    /**
     * 記錄玩家開鎖成功，寫入冷卻或領取紀錄
     */
    public void recordUnlock(Location loc, UUID playerUuid, VaultTemplate template) {
        if (loc == null || playerUuid == null || template == null) return;
        String key = toLocationKey(loc);
        long now = System.currentTimeMillis();
        long expireTime = now + (long) template.getCooldownMinutes() * 60L * 1000L;

        switch (template.getCooldownMode()) {
            case ONCE_PER_PLAYER -> {
                rewardedPlayers.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(playerUuid);
            }
            case GLOBAL_COOLDOWN -> {
                globalCooldowns.put(key, expireTime);
            }
            case PLAYER_COOLDOWN -> {
                playerCooldowns.computeIfAbsent(key, k -> new ConcurrentHashMap<>()).put(playerUuid, expireTime);
            }
        }
        saveRuntimeData();

        // 立即同步至世界上原版 Vault TileState，避免冷卻中玩家靠近時觸發原版開啟樣式與音效
        Block block = loc.getBlock();
        if (block.getType() == Material.VAULT && block.getState() instanceof Vault vaultState) {
            vaultState.addRewardedPlayer(playerUuid);
            if (template.getCooldownMode() == VaultCooldownMode.GLOBAL_COOLDOWN) {
                vaultState.setActivationRange(0.0);
            }
            vaultState.update(true, false);
        }
    }

    public void clearCooldown(Location loc) {
        if (loc == null) return;
        String key = toLocationKey(loc);
        playerCooldowns.remove(key);
        globalCooldowns.remove(key);
        rewardedPlayers.remove(key);
        saveRuntimeData();

        Block block = loc.getBlock();
        if (block.getType() == Material.VAULT && block.getState() instanceof Vault vaultState) {
            for (UUID u : new ArrayList<>(vaultState.getRewardedPlayers())) {
                vaultState.removeRewardedPlayer(u);
            }
            vaultState.setActivationRange(4.5);
            vaultState.update(true, false);
        }
        ejectingVaults.remove(key);
    }

    public boolean isEjecting(Location loc) {
        if (loc == null) return false;
        return ejectingVaults.contains(toLocationKey(loc));
    }

    public void setEjecting(Location loc, boolean ejecting) {
        if (loc == null) return;
        String key = toLocationKey(loc);
        if (ejecting) {
            ejectingVaults.add(key);
        } else {
            ejectingVaults.remove(key);
        }
    }

    // ==========================================
    // 已放置寶庫方塊追蹤與同步
    // ==========================================

    public void registerVault(Location loc, String templateName) {
        if (loc == null || templateName == null) return;
        String key = toLocationKey(loc);
        locationCache.put(key, loc.clone());
        placedVaults.put(key, templateName);
        saveRuntimeData();
    }

    public void unregisterVault(Location loc) {
        if (loc == null) return;
        String key = toLocationKey(loc);
        locationCache.remove(key);
        placedVaults.remove(key);
        saveRuntimeData();
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

    /**
     * 當配置儲存（例如切換為不祥模式）時，立即同步世界上所有已載入的同名寶庫方塊
     */
    public void updatePlacedVaultBlocks(VaultTemplate template) {
        if (template == null) return;
        for (Map.Entry<String, String> entry : placedVaults.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(template.getName())) {
                Location loc = parseLocation(entry.getKey());
                if (loc != null && loc.getWorld() != null && loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                    Block block = loc.getBlock();
                    if (block.getType() == Material.VAULT) {
                        syncVaultBlock(block, template.getName());
                    }
                }
            }
        }
    }

    /**
     * 同步單一方塊的不祥狀態、外觀與 TileState
     */
    public void syncVaultBlock(Block block, String templateName) {
        if (block == null || block.getType() != Material.VAULT) return;
        VaultTemplate template = getTemplate(templateName);
        if (template == null) return;

        Location loc = block.getLocation();
        String locKey = toLocationKey(loc);
        placedVaults.put(locKey, template.getName());
        long now = System.currentTimeMillis();

        boolean globalInCd = false;
        if (template.getCooldownMode() == VaultCooldownMode.GLOBAL_COOLDOWN) {
            Long expire = globalCooldowns.get(locKey);
            if (expire != null && expire > now) {
                globalInCd = true;
            }
        }

        // 1. 同步 BlockData (不祥屬性與冷卻外觀)
        if (block.getBlockData() instanceof org.bukkit.block.data.type.Vault vaultData) {
            vaultData.setOminous(template.isOminous());
            // 若在全域冷卻中，外觀強制維持關閉 (INACTIVE)；若在吐物品中維持 EJECTING
            if (globalInCd && vaultData.getVaultState() != org.bukkit.block.data.type.Vault.State.EJECTING) {
                vaultData.setVaultState(org.bukkit.block.data.type.Vault.State.INACTIVE);
            }
            block.setBlockData(vaultData, true);
        }

        // 2. 同步 TileState (原版鑰匙、感應半徑、冷卻名單)
        if (block.getState() instanceof Vault vaultState) {
            vaultState.setKeyItem(template.getKeyItem());

            // 全域冷卻時將感應半徑設為 0.0，使任何玩家靠近都無法觸發原版開啟樣式與音效
            if (globalInCd) {
                vaultState.setActivationRange(0.0);
            } else {
                vaultState.setActivationRange(4.5);
            }

            // 同步終生一次已領取名單
            Set<UUID> onceSet = rewardedPlayers.get(locKey);
            if (onceSet != null) {
                for (UUID u : onceSet) {
                    if (!vaultState.hasRewardedPlayer(u)) {
                        vaultState.addRewardedPlayer(u);
                    }
                }
            }

            // 同步個人冷卻名單：未過期者加入已領取（靠近不啟動），已過期者移除（靠近可啟動）
            Map<UUID, Long> pMap = playerCooldowns.get(locKey);
            if (pMap != null) {
                for (Map.Entry<UUID, Long> e : pMap.entrySet()) {
                    if (e.getValue() > now) {
                        if (!vaultState.hasRewardedPlayer(e.getKey())) {
                            vaultState.addRewardedPlayer(e.getKey());
                        }
                    } else {
                        vaultState.removeRewardedPlayer(e.getKey());
                    }
                }
            }

            vaultState.update(true, false);
        }
    }

    /**
     * 當區塊載入時，同步區塊內已放置的自訂寶庫方塊
     */
    public void onChunkLoad(Chunk chunk) {
        if (chunk == null) return;
        int cx = chunk.getX();
        int cz = chunk.getZ();
        String worldName = chunk.getWorld().getName();

        for (Map.Entry<String, String> entry : placedVaults.entrySet()) {
            String locKey = entry.getKey();
            String[] parts = locKey.split(":");
            if (parts.length == 4 && parts[0].equalsIgnoreCase(worldName)) {
                try {
                    int x = Integer.parseInt(parts[1]);
                    int z = Integer.parseInt(parts[3]);
                    if ((x >> 4) == cx && (z >> 4) == cz) {
                        int y = Integer.parseInt(parts[2]);
                        Block block = chunk.getWorld().getBlockAt(x, y, z);
                        syncVaultBlock(block, entry.getValue());
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    // ==========================================
    // 冷卻結束心跳檢測計時器
    // ==========================================

    public void startTicker() {
        if (tickerTask != null) {
            tickerTask.cancel();
        }
        tickerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickCooldowns, 20L, 20L);
    }

    public void stopTicker() {
        if (tickerTask != null) {
            tickerTask.cancel();
            tickerTask = null;
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
                handleGlobalCooldownExpired(locKey);
            }
        }

        // 2. 個人冷卻檢查
        Iterator<Map.Entry<String, Map<UUID, Long>>> pIt = playerCooldowns.entrySet().iterator();
        while (pIt.hasNext()) {
            Map.Entry<String, Map<UUID, Long>> entry = pIt.next();
            String locKey = entry.getKey();
            Map<UUID, Long> map = entry.getValue();
            List<UUID> expiredUuids = new ArrayList<>();
            Iterator<Map.Entry<UUID, Long>> subIt = map.entrySet().iterator();
            while (subIt.hasNext()) {
                Map.Entry<UUID, Long> sub = subIt.next();
                if (sub.getValue() <= now) {
                    expiredUuids.add(sub.getKey());
                    subIt.remove();
                    changed = true;
                }
            }
            if (!expiredUuids.isEmpty()) {
                handlePlayerCooldownExpired(locKey, expiredUuids);
            }
            if (map.isEmpty()) {
                pIt.remove();
            }
        }

        if (changed) {
            saveRuntimeData();
        }

        // 3. 每秒狀態守衛巡檢：嚴格保證冷卻中寶庫維持關閉樣式 (INACTIVE) 與無感應聲音
        tickVaultStateGuard();
    }

    private void handleGlobalCooldownExpired(String locKey) {
        Location loc = parseLocation(locKey);
        if (loc == null || loc.getWorld() == null || !loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) return;
        Block block = loc.getBlock();
        if (block.getType() != Material.VAULT) return;

        String templateName = placedVaults.get(locKey);
        if (templateName == null) return;
        VaultTemplate template = getTemplate(templateName);
        if (template == null) return;

        if (block.getBlockData() instanceof org.bukkit.block.data.type.Vault vaultData) {
            vaultData.setOminous(template.isOminous());
            block.setBlockData(vaultData, true);
        }

        if (block.getState() instanceof Vault vaultState) {
            vaultState.setActivationRange(4.5);
            vaultState.update(true, false);
        }
    }

    private void handlePlayerCooldownExpired(String locKey, List<UUID> expiredUuids) {
        Location loc = parseLocation(locKey);
        if (loc == null || loc.getWorld() == null || !loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) return;
        Block block = loc.getBlock();
        if (block.getType() != Material.VAULT) return;

        if (block.getState() instanceof Vault vaultState) {
            for (UUID u : expiredUuids) {
                vaultState.removeRewardedPlayer(u);
            }
            vaultState.update(true, false);
        }
    }

    private void tickVaultStateGuard() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, String> entry : placedVaults.entrySet()) {
            String locKey = entry.getKey();
            if (ejectingVaults.contains(locKey)) {
                continue;
            }
            VaultTemplate template = getTemplate(entry.getValue());
            if (template == null) continue;

            Location loc = parseLocation(locKey);
            if (loc == null || loc.getWorld() == null || !loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                continue;
            }

            // 視距守衛：48 格內無任何在線玩家時直接略過，避免無謂的 BlockData 與 TileState 操作
            if (loc.getWorld().getNearbyPlayers(loc, 48).isEmpty()) {
                continue;
            }

            Block block = loc.getBlock();
            if (block.getType() != Material.VAULT) continue;

            Long globalExpire = globalCooldowns.get(locKey);
            boolean inGlobalCd = (template.getCooldownMode() == VaultCooldownMode.GLOBAL_COOLDOWN && globalExpire != null && globalExpire > now);

            if (block.getBlockData() instanceof org.bukkit.block.data.type.Vault data) {
                boolean changed = false;
                if (data.isOminous() != template.isOminous()) {
                    data.setOminous(template.isOminous());
                    changed = true;
                }
                // 全域冷卻時百葉窗與外觀絕對不可被激活，維持 INACTIVE 關閉樣式
                if (inGlobalCd && data.getVaultState() == org.bukkit.block.data.type.Vault.State.ACTIVE) {
                    data.setVaultState(org.bukkit.block.data.type.Vault.State.INACTIVE);
                    changed = true;
                }
                if (changed) {
                    block.setBlockData(data, true);
                }
            }

            // 使用 Paper block.getState(false) 避免產生沈重的 TileEntity 記憶體快照 (GC 優化)
            if (block.getState(false) instanceof Vault vs) {
                boolean stateChanged = false;
                if (inGlobalCd) {
                    if (vs.getActivationRange() != 0.0) {
                        vs.setActivationRange(0.0);
                        stateChanged = true;
                    }
                } else {
                    if (vs.getActivationRange() != 4.5) {
                        vs.setActivationRange(4.5);
                        stateChanged = true;
                    }
                }

                // 個人冷卻模式：確保目前在冷卻中的所有玩家都在 TileState 已領取名單中
                if (template.getCooldownMode() == VaultCooldownMode.PLAYER_COOLDOWN) {
                    Map<UUID, Long> pMap = playerCooldowns.get(locKey);
                    if (pMap != null) {
                        for (Map.Entry<UUID, Long> pEntry : pMap.entrySet()) {
                            if (pEntry.getValue() > now && !vs.hasRewardedPlayer(pEntry.getKey())) {
                                vs.addRewardedPlayer(pEntry.getKey());
                                stateChanged = true;
                            }
                        }
                    }
                }

                if (stateChanged) {
                    vs.update(true, false);
                }
            }
        }
    }

    // ==========================================
    // 運行期冷卻與位置資料持久化 (vault_runtime.yml)
    // ==========================================

    public void saveRuntimeData() {
        saveRuntimeData(false);
    }

    public void saveRuntimeData(boolean sync) {
        YamlConfiguration yaml = new YamlConfiguration();
        long now = System.currentTimeMillis();

        // 1. 全域冷卻
        for (Map.Entry<String, Long> entry : globalCooldowns.entrySet()) {
            if (entry.getValue() > now) {
                yaml.set("global." + entry.getKey(), entry.getValue());
            }
        }

        // 2. 個人冷卻
        for (Map.Entry<String, Map<UUID, Long>> entry : playerCooldowns.entrySet()) {
            String locKey = entry.getKey();
            for (Map.Entry<UUID, Long> pEntry : entry.getValue().entrySet()) {
                if (pEntry.getValue() > now) {
                    yaml.set("player." + locKey + "." + pEntry.getKey().toString(), pEntry.getValue());
                }
            }
        }

        // 3. 終生一次
        for (Map.Entry<String, Set<UUID>> entry : rewardedPlayers.entrySet()) {
            List<String> uuids = entry.getValue().stream().map(UUID::toString).toList();
            yaml.set("once." + entry.getKey(), uuids);
        }

        // 4. 已放置寶庫方塊位置
        for (Map.Entry<String, String> entry : placedVaults.entrySet()) {
            yaml.set("placed." + entry.getKey(), entry.getValue());
        }

        // 運行期間使用非同步執行緒寫入檔案，徹底避免伺服器主執行緒因硬碟 I/O 阻塞造成 TPS 波動
        if (sync || !plugin.isEnabled()) {
            try {
                yaml.save(runtimeFile);
            } catch (IOException ignored) {}
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    yaml.save(runtimeFile);
                } catch (IOException ignored) {}
            });
        }
    }

    public void loadRuntimeData() {
        if (!runtimeFile.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(runtimeFile);
        long now = System.currentTimeMillis();

        globalCooldowns.clear();
        if (yaml.isConfigurationSection("global")) {
            for (String key : yaml.getConfigurationSection("global").getKeys(false)) {
                long expire = yaml.getLong("global." + key, 0);
                if (expire > now) {
                    globalCooldowns.put(key, expire);
                }
            }
        }

        playerCooldowns.clear();
        if (yaml.isConfigurationSection("player")) {
            for (String locKey : yaml.getConfigurationSection("player").getKeys(false)) {
                if (yaml.isConfigurationSection("player." + locKey)) {
                    Map<UUID, Long> map = new ConcurrentHashMap<>();
                    for (String uuidStr : yaml.getConfigurationSection("player." + locKey).getKeys(false)) {
                        long expire = yaml.getLong("player." + locKey + "." + uuidStr, 0);
                        if (expire > now) {
                            try {
                                map.put(UUID.fromString(uuidStr), expire);
                            } catch (Exception ignored) {}
                        }
                    }
                    if (!map.isEmpty()) {
                        playerCooldowns.put(locKey, map);
                    }
                }
            }
        }

        rewardedPlayers.clear();
        if (yaml.isConfigurationSection("once")) {
            for (String locKey : yaml.getConfigurationSection("once").getKeys(false)) {
                List<String> list = yaml.getStringList("once." + locKey);
                Set<UUID> set = ConcurrentHashMap.newKeySet();
                for (String uStr : list) {
                    try {
                        set.add(UUID.fromString(uStr));
                    } catch (Exception ignored) {}
                }
                if (!set.isEmpty()) {
                    rewardedPlayers.put(locKey, set);
                }
            }
        }

        placedVaults.clear();
        if (yaml.isConfigurationSection("placed")) {
            for (String locKey : yaml.getConfigurationSection("placed").getKeys(false)) {
                String tName = yaml.getString("placed." + locKey);
                if (tName != null) {
                    placedVaults.put(locKey, tName);
                }
            }
        }
        for (String locKey : globalCooldowns.keySet()) {
            placedVaults.putIfAbsent(locKey, "");
        }
        for (String locKey : playerCooldowns.keySet()) {
            placedVaults.putIfAbsent(locKey, "");
        }
        for (String locKey : rewardedPlayers.keySet()) {
            placedVaults.putIfAbsent(locKey, "");
        }
    }
}
