package clre20.customLootX.manager;

import clre20.customLootX.CustomLootX;
import clre20.customLootX.model.LootItem;
import clre20.customLootX.model.LootTemplate;
import clre20.customLootX.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TemplateManager {

    private final CustomLootX plugin;
    private final File suspiciousDir;
    // 資料夾索引：名稱 -> 子資料夾 / 檔案 (支援 /data 內以資料夾分類)
    private final clre20.customLootX.util.TemplateIndex index = new clre20.customLootX.util.TemplateIndex();
    private final Map<String, LootTemplate> templates = new ConcurrentHashMap<>();

    public TemplateManager(CustomLootX plugin) {
        this.plugin = plugin;
        this.suspiciousDir = new File(plugin.getDataFolder(), "data/suspicious");
        if (!suspiciousDir.exists()) {
            suspiciousDir.mkdirs();
        }
        loadAll();
    }

    /**
     * 同步載入全部配置 (啟動時使用；讀檔與解析會平行處理)
     */
    public void loadAll() {
        applyLoaded(readAll());
    }

    /**
     * 讀取並解析所有配置檔 (含子資料夾)。不修改任何共享狀態，可在背景執行緒呼叫
     */
    public java.util.List<clre20.customLootX.util.ParallelYamlLoader.Loaded> readAll() {
        if (!suspiciousDir.exists()) {
            suspiciousDir.mkdirs();
        }
        java.util.List<clre20.customLootX.util.TemplateFolders.Scanned> files = clre20.customLootX.util.TemplateFolders.scan(suspiciousDir,
                msg -> plugin.logWarn("&6[可疑方塊·載入]&c " + msg));
        return clre20.customLootX.util.ParallelYamlLoader.load(files, plugin.getConfigManager().getTemplateLoadThreads());
    }

    /**
     * 套用解析結果 (主執行緒)：轉換為模板並一次替換，重名時保留路徑排序較前者並警告
     */
    public void applyLoaded(java.util.List<clre20.customLootX.util.ParallelYamlLoader.Loaded> loaded) {
        Map<String, LootTemplate> next = new HashMap<>();
        Map<String, String> names = new HashMap<>();
        Map<String, String> folders = new HashMap<>();
        Map<String, File> files = new HashMap<>();
        String baseAbs = suspiciousDir.getAbsolutePath();
        for (clre20.customLootX.util.ParallelYamlLoader.Loaded l : loaded) {
            File file = l.source().file();
            String rel = file.getAbsolutePath().startsWith(baseAbs) ? file.getAbsolutePath().substring(baseAbs.length() + 1) : file.getName();
            if (l.yaml() == null) {
                plugin.logWarn("&6[可疑方塊·載入]&c 讀取自訂可疑方塊配置檔案失敗: &e" + rel + "&c - " + l.error());
                continue;
            }
            try {
                LootTemplate template = loadFromYaml(l.yaml(), file);
                if (template == null || template.getName() == null || template.getName().trim().isEmpty()) continue;
                String key = template.getName().toLowerCase();
                File existing = files.get(key);
                if (existing != null) {
                    String existRel = existing.getAbsolutePath().substring(Math.min(existing.getAbsolutePath().length(), baseAbs.length() + 1));
                    plugin.logWarn("&6[可疑方塊·載入]&c 配置名稱重複: &e" + template.getName() + "&c (" + rel + ")，已保留 &e" + existRel + "&c，請改名或刪除其中一個。");
                    continue;
                }
                template.setFolder(l.source().folder());
                next.put(key, template);
                names.put(key, template.getName());
                folders.put(key, l.source().folder());
                files.put(key, file);
            } catch (Exception e) {
                plugin.logWarn("&6[可疑方塊·載入]&c 讀取自訂可疑方塊配置檔案失敗: &e" + rel + "&c - " + e.getMessage());
            }
        }
        templates.clear();
        templates.putAll(next);
        index.replaceAll(names, folders, files);
        plugin.logConsole("&6[可疑方塊·載入]&7 成功載入 &a" + templates.size() + "&7 個自訂可疑方塊配置 (&f" + index.folders().size() + "&7 個資料夾)。");
    }

    // 儲存 / 刪除次數：背景重載期間若有變動，套用時改為重新同步讀取，避免蓋掉剛存的配置
    private volatile int modCount = 0;

    public int getModCount() {
        return modCount;
    }

    public clre20.customLootX.util.TemplateIndex getIndex() {
        return index;
    }

    private LootTemplate loadFromYaml(YamlConfiguration yaml, File file) {
        String fileNameNoExt = file.getName().substring(0, file.getName().lastIndexOf('.'));
        String name = yaml.getString("name", fileNameNoExt);
        String typeStr = yaml.getString("type", "SUSPICIOUS_SAND");
        Material type = Material.getMaterial(typeStr);
        if (type != Material.SUSPICIOUS_GRAVEL) {
            type = Material.SUSPICIOUS_SAND;
        }
        String displayName = yaml.getString("display-name", "&e自訂可疑方塊: " + name);
        boolean resetEnabled = yaml.getBoolean("reset.enabled", false);
        int resetSeconds;
        if (yaml.contains("reset.seconds")) {
            resetSeconds = Math.max(1, yaml.getInt("reset.seconds"));
        } else {
            resetSeconds = Math.max(1, yaml.getInt("reset.minutes", 5) * 60);
        }
        String broadcastMessage = yaml.getString("broadcast-message", null);

        List<LootItem> items = new ArrayList<>();
        if (yaml.isList("items")) {
            List<Map<?, ?>> list = yaml.getMapList("items");
            for (Map<?, ?> map : list) {
                double chance = TextUtil.roundChance(map.containsKey("chance") ? ((Number) map.get("chance")).doubleValue() : 0.0);
                boolean isAir = map.containsKey("is-air") && Boolean.parseBoolean(String.valueOf(map.get("is-air")));
                boolean broadcast = map.containsKey("broadcast") && Boolean.parseBoolean(String.valueOf(map.get("broadcast")));
                String itemBroadcastMsg = map.containsKey("broadcast-message") ? String.valueOf(map.get("broadcast-message")) : null;

                ItemStack item = null;
                if (!isAir && map.containsKey("item")) {
                    Object itemObj = map.get("item");
                    if (itemObj instanceof ItemStack is) {
                        item = is;
                    }
                }
                int limitServerDaily = (map.get("limit-server-daily") instanceof Number n) ? n.intValue() : 0;
                int limitServerMonthly = (map.get("limit-server-monthly") instanceof Number n) ? n.intValue() : 0;
                int limitPlayerDaily = (map.get("limit-player-daily") instanceof Number n) ? n.intValue() : 0;
                int limitPlayerTotal = (map.get("limit-player-total") instanceof Number n) ? n.intValue() : 0;

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

        return new LootTemplate(name, type, displayName, resetEnabled, resetSeconds, items, broadcastMessage);
    }

    public boolean saveTemplate(LootTemplate template) {
        if (template == null || template.getName() == null || template.getName().trim().isEmpty()) {
            return false;
        }
        String safeName = template.getName().trim();
        File file;
        String folder = clre20.customLootX.util.TemplateFolders.normalize(template.getFolder());
        if (folder == null) folder = "";
        try {
            file = clre20.customLootX.util.TemplateFolders.fileFor(suspiciousDir, folder, safeName);
        } catch (IOException e) {
            plugin.logError("&6[可疑方塊·儲存]&c 配置路徑不合法: &e" + safeName + " &c- " + e.getMessage());
            return false;
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        YamlConfiguration yaml = new YamlConfiguration();

        yaml.set("name", safeName);
        yaml.set("type", template.getType().name());
        yaml.set("display-name", template.getDisplayName());
        yaml.set("reset.enabled", template.isResetEnabled());
        yaml.set("reset.seconds", template.getResetSeconds());
        yaml.set("reset.minutes", template.getResetMinutes());
        if (template.getBroadcastMessage() != null && !template.getBroadcastMessage().trim().isEmpty()) {
            yaml.set("broadcast-message", template.getBroadcastMessage());
        }

        List<Map<String, Object>> itemsList = new ArrayList<>();
        for (int i = 0; i < template.getItems().size(); i++) {
            LootItem item = template.getItems().get(i);
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("slot", i);
            map.put("chance", item.getChance());
            map.put("is-air", item.isAir());
            map.put("broadcast", item.isBroadcast());
            if (item.getBroadcastMessage() != null && !item.getBroadcastMessage().trim().isEmpty()) {
                map.put("broadcast-message", item.getBroadcastMessage());
            }
            if (item.getLimitServerDaily() > 0) {
                map.put("limit-server-daily", item.getLimitServerDaily());
            }
            if (item.getLimitServerMonthly() > 0) {
                map.put("limit-server-monthly", item.getLimitServerMonthly());
            }
            if (item.getLimitPlayerDaily() > 0) {
                map.put("limit-player-daily", item.getLimitPlayerDaily());
            }
            if (item.getLimitPlayerTotal() > 0) {
                map.put("limit-player-total", item.getLimitPlayerTotal());
            }
            if (!item.isAir() && item.getItem() != null) {
                map.put("item", item.getItem());
            }
            itemsList.add(map);
        }
        yaml.set("items", itemsList);

        try {
            yaml.save(file);
            // 搬移資料夾或改變大小寫時，移除舊位置的檔案
            File old = index.fileOf(safeName);
            if (old != null && !old.getAbsoluteFile().equals(file.getAbsoluteFile()) && old.exists()) {
                old.delete();
            }
            template.setFolder(folder);
            templates.put(safeName.toLowerCase(), template);
            index.put(safeName, folder, file);
            modCount++;
            return true;
        } catch (IOException e) {
            plugin.logError("&6[可疑方塊·儲存]&c 儲存可疑方塊配置 &e" + safeName + " &c失敗: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteTemplate(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase();
        LootTemplate removed = templates.remove(lower);
        File file = index.fileOf(lower);
        index.remove(lower);
        modCount++;
        if (file != null && file.exists()) {
            return file.delete();
        }
        return removed != null;
    }

    public LootTemplate getTemplate(String name) {
        if (name == null) return null;
        return templates.get(name.toLowerCase());
    }

    public boolean hasTemplate(String name) {
        if (name == null) return false;
        return templates.containsKey(name.toLowerCase());
    }

    public Set<String> getTemplateNames() {
        return Collections.unmodifiableSet(templates.keySet());
    }

    public Collection<LootTemplate> getAllTemplates() {
        return Collections.unmodifiableCollection(templates.values());
    }
}
