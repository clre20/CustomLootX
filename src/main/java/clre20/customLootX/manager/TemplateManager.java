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
    private final Map<String, LootTemplate> templates = new ConcurrentHashMap<>();

    public TemplateManager(CustomLootX plugin) {
        this.plugin = plugin;
        this.suspiciousDir = new File(plugin.getDataFolder(), "data/suspicious");
        if (!suspiciousDir.exists()) {
            suspiciousDir.mkdirs();
        }
        loadAll();
    }

    public void loadAll() {
        templates.clear();
        if (!suspiciousDir.exists()) {
            suspiciousDir.mkdirs();
            return;
        }

        File[] files = suspiciousDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            try {
                LootTemplate template = loadFromFile(file);
                if (template != null) {
                    templates.put(template.getName().toLowerCase(), template);
                }
            } catch (Exception e) {
                plugin.logWarn("&6[可疑方塊·載入]&c 讀取自訂可疑方塊檔案失敗: &e" + file.getName() + "&c - " + e.getMessage());
            }
        }
        plugin.logConsole("&6[可疑方塊·載入]&7 成功載入 &a" + templates.size() + "&7 個自訂可疑方塊配置。");
    }

    private LootTemplate loadFromFile(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String fileNameNoExt = file.getName().substring(0, file.getName().lastIndexOf('.'));
        String name = yaml.getString("name", fileNameNoExt);
        String typeStr = yaml.getString("type", "SUSPICIOUS_SAND");
        Material type = Material.getMaterial(typeStr);
        if (type != Material.SUSPICIOUS_GRAVEL) {
            type = Material.SUSPICIOUS_SAND;
        }
        String displayName = yaml.getString("display-name", "&e自訂可疑方塊: " + name);
        boolean resetEnabled = yaml.getBoolean("reset.enabled", false);
        int resetMinutes = Math.max(1, yaml.getInt("reset.minutes", 5));

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

        return new LootTemplate(name, type, displayName, resetEnabled, resetMinutes, items);
    }

    public boolean saveTemplate(LootTemplate template) {
        if (template == null || template.getName() == null || template.getName().trim().isEmpty()) {
            return false;
        }
        String safeName = template.getName().trim();
        File file = new File(suspiciousDir, safeName + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();

        yaml.set("name", safeName);
        yaml.set("type", template.getType().name());
        yaml.set("display-name", template.getDisplayName());
        yaml.set("reset.enabled", template.isResetEnabled());
        yaml.set("reset.minutes", template.getResetMinutes());

        List<Map<String, Object>> itemsList = new ArrayList<>();
        for (int i = 0; i < template.getItems().size(); i++) {
            LootItem item = template.getItems().get(i);
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("slot", i);
            map.put("chance", item.getChance());
            map.put("is-air", item.isAir());
            if (!item.isAir() && item.getItem() != null) {
                map.put("item", item.getItem());
            }
            itemsList.add(map);
        }
        yaml.set("items", itemsList);

        try {
            yaml.save(file);
            templates.put(safeName.toLowerCase(), template);
            return true;
        } catch (IOException e) {
            plugin.logError("&6[可疑方塊·儲存]&c 儲存可疑方塊配置 &e" + safeName + " &c失敗: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteTemplate(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase();
        templates.remove(lower);
        File file = new File(suspiciousDir, name + ".yml");
        if (file.exists()) {
            return file.delete();
        }
        return false;
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
