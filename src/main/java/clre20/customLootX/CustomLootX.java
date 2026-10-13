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

    // 主控台日誌專用背景執行緒：Component 解析與輸出不占用主執行緒
    private volatile java.util.concurrent.ExecutorService logExecutor;

    @Override
    public void onEnable() {
        instance = this;

        // 啟動集中式背景 I/O 佇列 (所有資料檔寫入皆依序在此執行)
        clre20.customLootX.util.AsyncIO.start(msg -> getLogger().warning(msg));
        this.logExecutor = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "CustomLootX-Log");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });

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
        clre20.customLootX.util.AsyncIO.submit(() -> {
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
            lootLimitManager.save(true);
        }
        // 等候背景佇列中所有存檔寫入完成
        clre20.customLootX.util.AsyncIO.shutdown(15000L);
        logConsole("&c✘ CustomLootX 插件已關閉");
        java.util.concurrent.ExecutorService ex = this.logExecutor;
        this.logExecutor = null;
        if (ex != null) {
            ex.shutdown();
            try {
                ex.awaitTermination(3, java.util.concurrent.TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 輸出帶色彩與統一前綴的控制台訊息 (支援 MiniMessage 與 Legacy & 彩色碼)
     */
    public void logConsole(String message) {
        emitConsole("&8[&6CustomLootX&8] " + message);
    }

    public void logWarn(String message) {
        emitConsole("&8[&6CustomLootX&8] &e[系統警告]&c " + message);
    }

    public void logError(String message) {
        emitConsole("&8[&6CustomLootX&8] &4[嚴重錯誤]&c " + message);
    }

    /**
     * 輸出主控台訊息：預設交由背景執行緒解析彩色碼並輸出 (主控台寫入為執行緒安全)，
     * 大量開獎/重置日誌不再占用主執行緒 tick 時間；可於 config.yml performance.async-console-log 關閉。
     */
    private void emitConsole(String raw) {
        java.util.concurrent.ExecutorService ex = this.logExecutor;
        boolean async = ex != null && !ex.isShutdown()
                && (configManager == null || configManager.isAsyncConsoleLog());
        if (async) {
            try {
                ex.execute(() -> sendToConsole(raw));
                return;
            } catch (java.util.concurrent.RejectedExecutionException ignored) {
            }
        }
        sendToConsole(raw);
    }

    private static void sendToConsole(String raw) {
        try {
            org.bukkit.Bukkit.getConsoleSender().sendMessage(clre20.customLootX.util.TextUtil.parse(raw));
        } catch (Throwable ignored) {
        }
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
