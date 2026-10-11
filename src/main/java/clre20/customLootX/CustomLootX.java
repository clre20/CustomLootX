package clre20.customLootX;

import clre20.customLootX.command.ClxCommand;
import clre20.customLootX.config.ConfigManager;
import clre20.customLootX.gui.GuiManager;
import clre20.customLootX.listener.BlockEventListener;
import clre20.customLootX.listener.ChatListener;
import clre20.customLootX.listener.PlayerInteractListener;
import clre20.customLootX.manager.ChatInputManager;
import clre20.customLootX.manager.ItemManager;
import clre20.customLootX.manager.ResetManager;
import clre20.customLootX.manager.TemplateManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class CustomLootX extends JavaPlugin {

    private static CustomLootX instance;

    private ConfigManager configManager;
    private TemplateManager templateManager;
    private clre20.customLootX.manager.VaultTemplateManager vaultTemplateManager;
    private clre20.customLootX.manager.SpawnerTemplateManager spawnerTemplateManager;
    private clre20.customLootX.manager.DraftManager draftManager;
    private clre20.customLootX.manager.LootLimitManager lootLimitManager;
    private clre20.customLootX.manager.PlayerDataManager playerDataManager;
    private ItemManager itemManager;
    private ResetManager resetManager;
    private ChatInputManager chatInputManager;
    private GuiManager guiManager;
    private PlayerInteractListener playerInteractListener;

    public static CustomLootX getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.playerDataManager = new clre20.customLootX.manager.PlayerDataManager(this);
        this.lootLimitManager = new clre20.customLootX.manager.LootLimitManager(this);
        this.itemManager = new ItemManager(this);
        this.draftManager = new clre20.customLootX.manager.DraftManager(this);
        this.templateManager = new TemplateManager(this);
        this.vaultTemplateManager = new clre20.customLootX.manager.VaultTemplateManager(this);
        this.spawnerTemplateManager = new clre20.customLootX.manager.SpawnerTemplateManager(this);
        this.resetManager = new ResetManager(this);
        this.chatInputManager = new ChatInputManager(this);
        this.guiManager = new GuiManager(this);
        this.playerInteractListener = new PlayerInteractListener(this);

        // 非同步啟動巡檢清理逾期草稿，避免開服主執行緒檔案 I/O 卡頓
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            int cleaned = this.draftManager.cleanExpiredDrafts();
            if (cleaned > 0) {
                logConsole("&7[草稿·清理]&7 啟動巡檢已自動清除 &c" + cleaned + " &7個逾期草稿檔案。");
            }
        });

        // Register event listeners
        getServer().getPluginManager().registerEvents(guiManager, this);
        getServer().getPluginManager().registerEvents(playerInteractListener, this);
        getServer().getPluginManager().registerEvents(new BlockEventListener(this), this);
        getServer().getPluginManager().registerEvents(new clre20.customLootX.listener.VaultBlockListener(this), this);
        getServer().getPluginManager().registerEvents(new clre20.customLootX.listener.SpawnerBlockListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new clre20.customLootX.listener.DraftItemListener(this), this);

        try {
            Class.forName("io.papermc.paper.event.block.VaultChangeStateEvent");
            getServer().getPluginManager().registerEvents(new clre20.customLootX.listener.VaultStateChangeListener(this), this);
        } catch (Throwable ignored) {
        }

        // Register commands
        ClxCommand clxCmd = new ClxCommand(this);
        if (getCommand("clx") != null) {
            getCommand("clx").setExecutor(clxCmd);
            getCommand("clx").setTabCompleter(clxCmd);
        }

        // Start Vault, Spawner & Archaeology Reset tickers
        if (resetManager != null) {
            resetManager.startTicker();
        }
        if (vaultTemplateManager != null) {
            vaultTemplateManager.startTicker();
        }
        if (spawnerTemplateManager != null) {
            spawnerTemplateManager.startTicker();
        }

        logConsole("&a✔ 核心系統已成功啟動！");
    }

    @Override
    public void onDisable() {
        if (resetManager != null) {
            resetManager.stopTicker();
        }
        if (vaultTemplateManager != null) {
            vaultTemplateManager.stopTicker();
            vaultTemplateManager.saveRuntimeData(true);
        }
        if (spawnerTemplateManager != null) {
            spawnerTemplateManager.stopTicker();
            spawnerTemplateManager.saveRuntimeData(true);
        }
        if (playerDataManager != null) {
            playerDataManager.saveAll(true);
        }
        if (lootLimitManager != null) {
            lootLimitManager.save();
        }
        logConsole("&c✘ CustomLootX 插件已關閉");
    }

    /**
     * 輸出帶色彩與統一前綴的控制台訊息 (支援 MiniMessage 與 Legacy & 彩色碼)
     */
    public void logConsole(String message) {
        org.bukkit.Bukkit.getConsoleSender().sendMessage(clre20.customLootX.util.TextUtil.parse("&8[&6CustomLootX&8] " + message));
    }

    public void logWarn(String message) {
        org.bukkit.Bukkit.getConsoleSender().sendMessage(clre20.customLootX.util.TextUtil.parse("&8[&6CustomLootX&8] &e[系統警告]&c " + message));
    }

    public void logError(String message) {
        org.bukkit.Bukkit.getConsoleSender().sendMessage(clre20.customLootX.util.TextUtil.parse("&8[&6CustomLootX&8] &4[嚴重錯誤]&c " + message));
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public TemplateManager getTemplateManager() {
        return templateManager;
    }

    public clre20.customLootX.manager.VaultTemplateManager getVaultTemplateManager() {
        return vaultTemplateManager;
    }

    public clre20.customLootX.manager.SpawnerTemplateManager getSpawnerTemplateManager() {
        return spawnerTemplateManager;
    }

    public clre20.customLootX.manager.DraftManager getDraftManager() {
        return draftManager;
    }

    public ItemManager getItemManager() {
        return itemManager;
    }

    public ResetManager getResetManager() {
        return resetManager;
    }

    public ChatInputManager getChatInputManager() {
        return chatInputManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public PlayerInteractListener getPlayerInteractListener() {
        return playerInteractListener;
    }

    public clre20.customLootX.manager.LootLimitManager getLootLimitManager() {
        return lootLimitManager;
    }

    public clre20.customLootX.manager.PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
}
