package clre20.customLootX.config;

import clre20.customLootX.CustomLootX;
import clre20.customLootX.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class ConfigManager {

    private final CustomLootX plugin;
    private FileConfiguration config;
    private org.bukkit.configuration.file.YamlConfiguration defaultConfig;
    private String prefix;
    private final Map<String, Sound> sounds = new HashMap<>();
    private final Map<String, String> defaultFallbacks = new HashMap<>();

    public ConfigManager(CustomLootX plugin) {
        this.plugin = plugin;
        initFallbacks();
        load();
    }

    private void initFallbacks() {
        defaultFallbacks.put("name-prompt", "&e請在聊天室輸入配置識別名稱 (英文字母、數字與底線)，輸入 &ccancel &e取消：");
        defaultFallbacks.put("name-invalid", "&c名稱無效！只能包含英文字母、數字與底線，且不能空白。");
        defaultFallbacks.put("name-already-used", "&c該名稱已被其他配置使用，請使用不同的名稱！");
        defaultFallbacks.put("name-not-set", "&c必須先設定有效的配置名稱！");
        defaultFallbacks.put("input-prompt", "&e請在聊天室輸入此物品的機率 (0.01 ~ 100.00)，輸入 &ccancel &e取消：");
        defaultFallbacks.put("input-invalid", "&c輸入數值無效，請輸入介於 0.01 到 100.00 的有效數字（最多兩位小數）！");
        defaultFallbacks.put("input-success", "&a已將物品機率調整為: &e%chance%&a！");
        defaultFallbacks.put("input-cancelled", "&e已取消輸入操作。");
        defaultFallbacks.put("chance-invalid", "&c機率數值無效！請輸入介於 0.01 到 100.00 的有效數字。");
        defaultFallbacks.put("chance-not-100", "&c怪物池機率總和必須恰好為 100.00%（目前總和: &e%total%&c，差額: &e%diff%&c）！");
        defaultFallbacks.put("time-prompt", "&e請在聊天室輸入自動重置時間（單位：分鐘，請輸入大於等於 1 的整數），輸入 &ccancel &e取消：");
        defaultFallbacks.put("time-min-invalid", "&c時間必須至少為 1 分鐘！");
        defaultFallbacks.put("time-invalid", "&c請輸入有效的整數數字！");
        defaultFallbacks.put("roll-prompt", "&e請在聊天室輸入每次彈出數量 (1 ~ 64)，輸入 &ccancel &e取消：");
        defaultFallbacks.put("roll-invalid", "&c彈出數量無效！請輸入介於 1 到 64 的整數。");
        defaultFallbacks.put("key-name-prompt", "&e請在聊天室輸入鑰匙自訂名稱 (支援彩色代碼 &)，輸入 &ccancel &e取消：");
        defaultFallbacks.put("key-lore-prompt", "&e請在聊天室輸入鑰匙自訂說明 (支援彩色代碼 &)，輸入 &ccancel &e取消：");
        defaultFallbacks.put("vault-key-empty", "&c請先在中間放入一把有效的鑰匙物品！");
        defaultFallbacks.put("key-craft-success", "&a已成功製作專屬防偽鑰匙！已將 1 把鑰匙發送至您的背包。");
        defaultFallbacks.put("key-copy-success", "&a已成功套用手持物品的外觀材質與模型！");
        defaultFallbacks.put("key-copy-empty", "&c請先用滑鼠手持物品後再點擊此格以複製外觀！");
        defaultFallbacks.put("cmd-key-usage", "&c用法: /%label% key <寶庫名稱> [玩家] [數量]");
        defaultFallbacks.put("cmd-key-give", "&a已給予玩家 &e%player% &f%amount% 把 &e[%name%] &a專屬防偽鑰匙！");
        defaultFallbacks.put("cmd-key-receive", "&a你收到了 &f%amount% 把 &e[%name%] &a專屬防偽鑰匙！");
        defaultFallbacks.put("template-created", "&a已創建自訂%type%方塊！");
        defaultFallbacks.put("vault-template-created", "&a已創建全新自訂試煉寶庫！");
        defaultFallbacks.put("spawner-template-created", "&a已創建全新自訂試煉生怪磚！");
        defaultFallbacks.put("vault-unlocked", "");
        defaultFallbacks.put("empty-block-warning", "&c此方塊尚未完成設定！");
        defaultFallbacks.put("cannot-edit-placed", "");
        defaultFallbacks.put("create-usage", "&c用法: /%label% create <suspicious|vault|spawner>");
        defaultFallbacks.put("give-usage", "&c用法: /%label% give <suspicious|vault|spawner> <名稱> [給誰] [數量]");
        defaultFallbacks.put("give-console-target-required", "&c主控台執行時必須指定目標玩家！用法: /%label% give <suspicious|vault|spawner> <名稱> <給誰> [數量]");
        defaultFallbacks.put("edit-usage", "&c用法: /%label% edit <suspicious|vault|spawner> <名稱>");
        defaultFallbacks.put("delete-usage", "&c用法: /%label% delete <suspicious|vault|spawner> <名稱>");
        defaultFallbacks.put("draft-saved", "&a已為您暫存當前編輯進度至手持方塊中（草稿保留 %days% 天）。");
        defaultFallbacks.put("draft-saved-cmd", "&a已為您暫存當前編輯進度（草稿保留 %days% 天），下次輸入 &e/clx edit &a指令即可繼續編輯！");
        defaultFallbacks.put("draft-loaded", "&a已為您還原上次未儲存的草稿編輯進度！");
        defaultFallbacks.put("draft-loaded-cmd", "&a已為您載入 [%name%] 上次未儲存的暫存草稿！(若欲放棄草稿，可在最後確認介面點擊「放棄變更」)");
        defaultFallbacks.put("draft-expired-reverted", "&e此方塊的草稿已過期，已自動載入正式版本！");
        defaultFallbacks.put("save-cancelled", "&e已放棄變更並清理草稿暫存檔案。");
        defaultFallbacks.put("cannot-place-draft", "&c此方塊包含尚未儲存的草稿！請手持對空氣點擊右鍵完成儲存後再放置。");
        defaultFallbacks.put("save-success", "&a已成功儲存自訂可疑方塊配置: &e%name%&a！");
        defaultFallbacks.put("save-failed", "&c儲存失敗，請檢查後台日誌！");
        defaultFallbacks.put("sum-not-100", "&c儲存失敗！所有掉落物機率總和必須恰好為 100.00%（目前總和: &e%total%&c，差額: &e%diff%&c）！");
        defaultFallbacks.put("template-save-failed", "&c儲存配置檔案失敗，請檢查主控台日誌！");
        defaultFallbacks.put("spawner-save-success", "&a已成功儲存試煉生怪磚配置: &e%name%&a！");
        defaultFallbacks.put("spawner-mode-switched", "&8[&6CustomLootX&8] &a已切換生怪磚生成模式為: &e%mode%&a！");
        defaultFallbacks.put("spawner-in-cooldown", "&c此試煉生怪磚冷卻中！剩餘 &e%time% 秒&c後可再次挑戰。");
        defaultFallbacks.put("spawner-already-completed", "&c你已經完成過此試煉挑戰，無法再次領取獲勝獎勵！");
        defaultFallbacks.put("spawner-ready-hint", "&a此試煉生怪磚已就緒！進入感應範圍即可啟動挑戰 (怪物: &e%mob%&a)！");
        defaultFallbacks.put("spawner-victory", "&a✔ 恭喜完成試煉挑戰！獲勝獎勵已噴發！");
        defaultFallbacks.put("vault-key-mismatch", "&c解鎖失敗！此寶庫需要鑰匙: &e%key% &c才能開啟！");
        defaultFallbacks.put("vault-cooldown", "&c此寶庫冷卻中！剩餘 &e%time% &c後可再次開啟。");
        defaultFallbacks.put("vault-already-rewarded", "&c你已經開啟過此寶庫，無法再次領取戰利品！");
        defaultFallbacks.put("no-permission", "&c你沒有權限執行此指令！");
        defaultFallbacks.put("only-player", "&c此指令只能由玩家執行！");
        defaultFallbacks.put("player-not-found", "&c找不到目標玩家: %player%");
        defaultFallbacks.put("reload-success", "&aCustomLootX 設定與所有資料重載完成！");
        defaultFallbacks.put("give-success", "&a已給予玩家 &e%player% &f%amount% 個 &e[%name%] &a自訂方塊！");
        defaultFallbacks.put("give-self-tip", "&7(手持對空氣右鍵可重新編輯此配置，對地面右鍵可直接放置)");
        defaultFallbacks.put("template-not-found", "&c找不到名為 &e%name% &c的配置！");
        defaultFallbacks.put("template-deleted", "&a已成功刪除配置 &e%name%&a！");
        defaultFallbacks.put("receive-success", "&a你收到了 &f%amount% 個 &e[%name%] &a自訂方塊！");
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        try (java.io.InputStream defStream = plugin.getResource("config.yml")) {
            if (defStream != null) {
                try (java.io.Reader reader = new java.io.InputStreamReader(defStream, java.nio.charset.StandardCharsets.UTF_8)) {
                    this.defaultConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(reader);
                    this.config.setDefaults(this.defaultConfig);
                }
            }
        } catch (Exception ignored) {
        }

        this.prefix = config.getString("prefix", "&8[&6CustomLootX&8] &r");

        sounds.clear();
        if (config.isConfigurationSection("sound")) {
            for (String key : config.getConfigurationSection("sound").getKeys(false)) {
                String soundName = config.getString("sound." + key, "");
                if (soundName.isEmpty()) continue;
                try {
                    Sound sound = null;
                    try {
                        sound = Sound.valueOf(soundName.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        String formatted = soundName.toLowerCase().replace('_', '.');
                        sound = Registry.SOUNDS.get(NamespacedKey.minecraft(formatted));
                        if (sound == null) {
                            sound = Registry.SOUNDS.get(NamespacedKey.minecraft(soundName.toLowerCase()));
                        }
                    }
                    if (sound != null) {
                        sounds.put(key, sound);
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    public String getPrefix() {
        return prefix;
    }

    public static String replace(String template, Object... placeholders) {
        if (template == null) return "";
        String result = template;
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            String k = String.valueOf(placeholders[i]);
            String v = String.valueOf(placeholders[i + 1]);
            result = result.replace(k, v);
        }
        return result;
    }

    public String getRawMessage(String key) {
        String msg = config.getString("messages." + key);
        if (msg == null && defaultConfig != null) {
            msg = defaultConfig.getString("messages." + key);
        }
        if (msg == null) {
            msg = defaultFallbacks.get(key);
        }
        return (msg != null) ? msg : key;
    }

    public void send(CommandSender sender, String key, Object... placeholders) {
        String msg = getRawMessage(key);
        if (msg == null || msg.trim().isEmpty()) return;
        msg = replace(msg, placeholders);
        if (msg.trim().isEmpty()) return;
        sender.sendMessage(TextUtil.parse(prefix + msg));
    }

    public void sendRaw(CommandSender sender, String rawMessage) {
        if (rawMessage == null || rawMessage.trim().isEmpty()) return;
        sender.sendMessage(TextUtil.parse(prefix + rawMessage));
    }

    public String getText(String path, String def, Object... placeholders) {
        String raw = config.getString(path);
        if (raw == null && defaultConfig != null) {
            raw = defaultConfig.getString(path);
        }
        if (raw == null) raw = (def != null ? def : "");
        return replace(raw, placeholders);
    }

    public Component getComponent(String path, String def, Object... placeholders) {
        return TextUtil.parse(getText(path, def, placeholders));
    }

    public List<String> getStringList(String path, List<String> def, Object... placeholders) {
        List<String> rawList = null;
        if (config.isList(path)) {
            rawList = config.getStringList(path);
        } else if (defaultConfig != null && defaultConfig.isList(path)) {
            rawList = defaultConfig.getStringList(path);
        }
        if (rawList == null) {
            rawList = def;
        }
        if (rawList == null) return Collections.emptyList();
        List<String> list = new ArrayList<>(rawList.size());
        for (String s : rawList) {
            list.add(replace(s, placeholders));
        }
        return list;
    }

    public List<Component> getComponentList(String path, List<String> def, Object... placeholders) {
        List<String> strings = getStringList(path, def, placeholders);
        List<Component> result = new ArrayList<>(strings.size());
        for (String s : strings) {
            result.add(TextUtil.parse(s));
        }
        return result;
    }

    public void log(String logKey, Object... placeholders) {
        String pattern = config.getString("logs." + logKey);
        if (pattern == null && defaultConfig != null) {
            pattern = defaultConfig.getString("logs." + logKey);
        }
        if (pattern == null || pattern.trim().isEmpty()) {
            return;
        }
        String msg = replace(pattern, placeholders);
        plugin.logConsole(msg);
    }

    public void playSound(Player player, String soundKey) {
        if (player == null) return;
        Sound sound = sounds.get(soundKey);
        if (sound == null) {
            // Reliable fallback
            sound = switch (soundKey) {
                case "save", "success" -> Sound.ENTITY_PLAYER_LEVELUP;
                case "error" -> Sound.ENTITY_VILLAGER_NO;
                case "click" -> Sound.UI_BUTTON_CLICK;
                case "reset" -> Sound.BLOCK_SAND_PLACE;
                default -> null;
            };
        }
        if (sound != null) {
            float pitch = soundKey.equals("save") ? 1.2f : 1.0f;
            player.playSound(player.getLocation(), sound, 1.0f, pitch);
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }
}
