package clre20.customLootX.listener;

import clre20.customLootX.CustomLootX;
import clre20.customLootX.gui.wizard.vault.VaultWizardContext;
import clre20.customLootX.gui.wizard.vault.VaultWizardStep1Gui;
import clre20.customLootX.model.VaultCooldownMode;
import clre20.customLootX.model.VaultTemplate;
import clre20.customLootX.util.PermissionUtil;
import clre20.customLootX.util.TextUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Vault;
import org.bukkit.block.data.type.Vault.State;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.VaultDisplayItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class VaultBlockListener implements Listener {

    private final CustomLootX plugin;

    public VaultBlockListener(CustomLootX plugin) {
        this.plugin = plugin;
    }

    private String toLocationKey(Location loc) {
        if (loc == null) return "";
        return (loc.getWorld() != null ? loc.getWorld().getName() : "world") + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    /**
     * 放置自訂試煉寶庫
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!plugin.getItemManager().isCustomVaultItem(item)) {
            return;
        }

        Player player = event.getPlayer();

        // 0. 若為未儲存草稿物品，禁止放置
        if (plugin.getItemManager().isDraftItem(item)) {
            event.setCancelled(true);
            plugin.getConfigManager().send(player, "cannot-place-draft");
            plugin.getConfigManager().playSound(player, "error");
            return;
        }

        // 1. 若為空白未設定物品，禁止放置
        if (plugin.getItemManager().isBlankVaultItem(item)) {
            event.setCancelled(true);
            plugin.getConfigManager().send(player, "empty-block-warning");
            plugin.getConfigManager().playSound(player, "error");
            return;
        }

        // 2. 取得配置名稱
        String templateName = plugin.getItemManager().getVaultTemplateName(item);
        if (templateName == null || templateName.isEmpty()) {
            event.setCancelled(true);
            plugin.getConfigManager().send(player, "empty-block-warning");
            return;
        }

        VaultTemplate template = plugin.getVaultTemplateManager().getTemplate(templateName);
        if (template == null) {
            event.setCancelled(true);
            plugin.getConfigManager().send(player, "template-not-found", "%name%", templateName);
            return;
        }

        Block block = event.getBlockPlaced();
        Location loc = block.getLocation();

        // 寫入方塊資料與 PDC 標記
        new BukkitRunnable() {
            @Override
            public void run() {
                if (block.getType() != Material.VAULT) return;

                // 設定不祥外觀
                if (block.getBlockData() instanceof org.bukkit.block.data.type.Vault vaultData) {
                    vaultData.setOminous(template.isOminous());
                    block.setBlockData(vaultData, true);
                }

                // 設定 TileState PDC 與鑰匙
                if (block.getState() instanceof Vault vaultState) {
                    PersistentDataContainer pdc = vaultState.getPersistentDataContainer();
                    pdc.set(plugin.getItemManager().KEY_CUSTOM_VAULT, PersistentDataType.BYTE, (byte) 1);
                    pdc.set(plugin.getItemManager().KEY_VAULT_TEMPLATE_NAME, PersistentDataType.STRING, templateName);
                    vaultState.setKeyItem(template.getKeyItem());
                    vaultState.update(true, false);
                }

                plugin.getVaultTemplateManager().registerVault(loc, templateName);

                plugin.getConfigManager().log("vault-placed",
                        "%player%", player.getName(),
                        "%name%", template.getName(),
                        "%type%", template.isOminous() ? "OMINOUS" : "NORMAL",
                        "%world%", loc.getWorld() != null ? loc.getWorld().getName() : "world",
                        "%x%", String.valueOf(loc.getBlockX()),
                        "%y%", String.valueOf(loc.getBlockY()),
                        "%z%", String.valueOf(loc.getBlockZ())
                );
            }
        }.runTaskLater(plugin, 1L);
    }

    /**
     * 挖破寶庫方塊清理冷卻與日誌
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.VAULT) return;

        if (block.getState() instanceof Vault vaultState) {
            PersistentDataContainer pdc = vaultState.getPersistentDataContainer();
            if (pdc.has(plugin.getItemManager().KEY_CUSTOM_VAULT, PersistentDataType.BYTE)) {
                String templateName = pdc.get(plugin.getItemManager().KEY_VAULT_TEMPLATE_NAME, PersistentDataType.STRING);
                Location loc = block.getLocation();
                plugin.getVaultTemplateManager().unregisterVault(loc);
                plugin.getVaultTemplateManager().clearCooldown(loc);

                plugin.getConfigManager().log("vault-broken",
                        "%player%", event.getPlayer().getName(),
                        "%name%", (templateName != null ? templateName : "unknown"),
                        "%world%", loc.getWorld() != null ? loc.getWorld().getName() : "world",
                        "%x%", String.valueOf(loc.getBlockX()),
                        "%y%", String.valueOf(loc.getBlockY()),
                        "%z%", String.valueOf(loc.getBlockZ())
                );
            }
        }
    }

    /**
     * 區塊載入時自動同步區塊內已放置的自訂寶庫狀態
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(org.bukkit.event.world.ChunkLoadEvent event) {
        plugin.getVaultTemplateManager().onChunkLoad(event.getChunk());
    }

    /**
     * 寶庫內部動態展示物品事件
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onVaultDisplayItem(VaultDisplayItemEvent event) {
        Block block = event.getBlock();
        if (block.getState() instanceof Vault vaultState) {
            PersistentDataContainer pdc = vaultState.getPersistentDataContainer();
            if (pdc.has(plugin.getItemManager().KEY_CUSTOM_VAULT, PersistentDataType.BYTE)) {
                String templateName = pdc.get(plugin.getItemManager().KEY_VAULT_TEMPLATE_NAME, PersistentDataType.STRING);
                if (templateName != null) {
                    VaultTemplate template = plugin.getVaultTemplateManager().getTemplate(templateName);
                    if (template != null && !template.getItems().isEmpty()) {
                        Location loc = block.getLocation();
                        if (plugin.getVaultTemplateManager().isGlobalCooldownActive(loc, template)) {
                            event.setDisplayItem(null);
                            return;
                        }
                        ItemStack item = template.rollSingleItem();
                        if (item != null && !item.getType().isAir()) {
                            event.setDisplayItem(item);
                        }
                    }
                }
            }
        }
    }

    /**
     * 玩家互動事件：手持右鍵空氣開精靈、手持鑰匙右鍵解鎖寶庫
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        ItemStack held = event.getItem();
        Action action = event.getAction();

        // 1. 手持自訂寶庫物品點擊空氣 -> 開啟步驟引導精靈 (需 OP 2+)
        if (plugin.getItemManager().isCustomVaultItem(held)) {
            if (action == Action.RIGHT_CLICK_AIR || (action == Action.RIGHT_CLICK_BLOCK && player.isSneaking())) {
                event.setCancelled(true);
                if (!PermissionUtil.hasAdminPermission(player)) {
                    plugin.getConfigManager().send(player, "no-permission");
                    plugin.getConfigManager().playSound(player, "error");
                    return;
                }
                openVaultWizard(player, held);
                return;
            } else if (action == Action.RIGHT_CLICK_BLOCK) {
                if (plugin.getItemManager().isDraftItem(held)) {
                    event.setCancelled(true);
                    plugin.getConfigManager().send(player, "cannot-place-draft");
                    plugin.getConfigManager().playSound(player, "error");
                    return;
                }
                if (plugin.getItemManager().isBlankVaultItem(held)) {
                    event.setCancelled(true);
                    plugin.getConfigManager().send(player, "empty-block-warning");
                    plugin.getConfigManager().playSound(player, "error");
                    return;
                }
                return;
            }
        }

        // 2. 右鍵點擊世界上的寶庫方塊
        if (action == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Block block = event.getClickedBlock();
            if (block.getType() == Material.VAULT && block.getState() instanceof Vault vaultState) {
                PersistentDataContainer pdc = vaultState.getPersistentDataContainer();
                if (pdc.has(plugin.getItemManager().KEY_CUSTOM_VAULT, PersistentDataType.BYTE)) {
                    event.setCancelled(true); // 攔截原版開鎖，由 CustomLootX 全權處理

                    String templateName = pdc.get(plugin.getItemManager().KEY_VAULT_TEMPLATE_NAME, PersistentDataType.STRING);
                    VaultTemplate template = plugin.getVaultTemplateManager().getTemplate(templateName);
                    if (template == null) {
                        plugin.getConfigManager().send(player, "template-not-found", "%name%", String.valueOf(templateName));
                        return;
                    }

                    // 確保方塊已註冊並同步最新外觀
                    plugin.getVaultTemplateManager().syncVaultBlock(block, templateName);

                    Location loc = block.getLocation();

                    // A. 檢查手持物品是否為對應鑰匙 (包含 NBT / PDC 防偽)
                    // 根據需求 1：手持非鑰匙物品或空手右鍵寶庫方塊無須做任何反應
                    ItemStack keyInHand = player.getInventory().getItemInMainHand();
                    if (!plugin.getItemManager().matchesKey(keyInHand, template.getKeyItem())) {
                        return;
                    }

                    // 檢查寶庫是否正在開獎吐物品中，避免多玩家或多次點擊重疊
                    if (plugin.getVaultTemplateManager().isEjecting(loc)) {
                        return;
                    }

                    // B. 手持鑰匙時，檢查冷卻與領取資格
                    long status = plugin.getVaultTemplateManager().checkCooldownStatus(loc, player.getUniqueId(), template);
                    if (status == -1) {
                        plugin.getConfigManager().send(player, "vault-already-rewarded");
                        plugin.getConfigManager().playSound(player, "error");
                        return;
                    }
                    if (status > 0) {
                        String timeFormatted = formatTime(status);
                        plugin.getConfigManager().send(player, "vault-cooldown", "%time%", timeFormatted);
                        plugin.getConfigManager().playSound(player, "error");
                        return;
                    }

                    // C. 成功解鎖：扣除 1 個鑰匙
                    if (keyInHand.getAmount() > 1) {
                        keyInHand.setAmount(keyInHand.getAmount() - 1);
                    } else {
                        player.getInventory().setItemInMainHand(null);
                    }

                    // D. 記錄冷卻 / 領取
                    plugin.getVaultTemplateManager().recordUnlock(loc, player.getUniqueId(), template);

                    // E. 抽取並彈出戰利品
                    List<ItemStack> rewards = template.rollAllItems();
                    if (rewards.isEmpty()) {
                        return;
                    }

                    plugin.getVaultTemplateManager().setEjecting(loc, true);

                    int totalItems = rewards.size();
                    long intervalTicks = 16L; // 物品彈出間隔 ~0.8 秒

                    // 1. 將世界上的真實方塊直接切換為 EJECTING，配合 VaultStateChangeListener 事件守衛阻止原版切回 INACTIVE，徹底杜絕伺服器於互動取消時發送 INACTIVE 確認包造成的瞬間閉合
                    if (block.getBlockData() instanceof org.bukkit.block.data.type.Vault vaultData) {
                        vaultData.setVaultState(org.bukkit.block.data.type.Vault.State.EJECTING);
                        vaultData.setOminous(template.isOminous());
                        block.setBlockData(vaultData, true);
                    }

                    // 2. 構建客戶端頂部開口樣式 (EJECTING)
                    org.bukkit.block.data.type.Vault ejectingVisualData = (org.bukkit.block.data.type.Vault) block.getBlockData().clone();
                    ejectingVisualData.setVaultState(org.bukkit.block.data.type.Vault.State.EJECTING);
                    ejectingVisualData.setOminous(template.isOminous());

                    loc.getWorld().playSound(loc, Sound.BLOCK_VAULT_OPEN_SHUTTER, 1.0f, 1.0f);
                    loc.getWorld().playSound(loc, Sound.BLOCK_VAULT_ACTIVATE, 1.0f, 1.0f);

                    for (Player p : loc.getWorld().getNearbyPlayers(loc, 64)) {
                        p.sendBlockChange(loc, ejectingVisualData);
                    }

                    // 持續視覺守衛：在吐物期間每 6 ticks 維持狀態，大幅降低封包與運算負擔，同時確保多玩家看到平滑開啟的百葉窗
                    org.bukkit.scheduler.BukkitTask visualKeeper = new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (block.getType() != Material.VAULT) {
                                cancel();
                                return;
                            }
                            if (block.getBlockData() instanceof org.bukkit.block.data.type.Vault vd) {
                                if (vd.getVaultState() != org.bukkit.block.data.type.Vault.State.EJECTING) {
                                    vd.setVaultState(org.bukkit.block.data.type.Vault.State.EJECTING);
                                    block.setBlockData(vd, false);
                                }
                            }
                            java.util.Collection<Player> nearby = loc.getWorld().getNearbyPlayers(loc, 48);
                            if (!nearby.isEmpty()) {
                                for (Player p : nearby) {
                                    p.sendBlockChange(loc, ejectingVisualData);
                                }
                            }
                        }
                    }.runTaskTimer(plugin, 1L, 6L);

                    for (int i = 0; i < totalItems; i++) {
                        ItemStack reward = rewards.get(i);
                        long delay = (long) i * intervalTicks;

                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (reward == null || reward.getType().isAir()) return;
                                if (block.getType() != Material.VAULT) return;

                                // 根據需求 8：從寶庫頂部中央向上彈 0.5 格後落在寶庫頂部
                                Location spawnLoc = loc.clone().add(0.5, 1.01, 0.5);
                                Item dropped = loc.getWorld().dropItem(spawnLoc, reward);
                                // vy = 0.22 向上躍起約 0.5 格後受重力垂直落回寶庫頂部
                                dropped.setVelocity(new Vector(0, 0.22, 0));

                                loc.getWorld().playSound(loc, Sound.BLOCK_VAULT_EJECT_ITEM, 0.8f, 1.0f);
                                loc.getWorld().spawnParticle(
                                        template.isOminous() ? Particle.SOUL_FIRE_FLAME : Particle.SMALL_FLAME,
                                        spawnLoc,
                                        6, 0.1, 0.1, 0.1, 0.02
                                );
                            }
                        }.runTaskLater(plugin, delay);
                    }

                    // 3. 閉口動畫：開口維持開啟，直到最後一個物品完全吐完並落定後才關閉
                    long closeDelay = ((long) (totalItems - 1) * intervalTicks) + 16L;

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            visualKeeper.cancel();
                            plugin.getVaultTemplateManager().setEjecting(loc, false);

                            if (block.getType() == Material.VAULT) {
                                if (block.getBlockData() instanceof org.bukkit.block.data.type.Vault closedData) {
                                    closedData.setVaultState(org.bukkit.block.data.type.Vault.State.INACTIVE);
                                    closedData.setOminous(template.isOminous());
                                    block.setBlockData(closedData, true);
                                    for (Player p : loc.getWorld().getNearbyPlayers(loc, 64)) {
                                        p.sendBlockChange(loc, closedData);
                                    }
                                }
                                loc.getWorld().playSound(loc, Sound.BLOCK_VAULT_CLOSE_SHUTTER, 1.0f, 1.0f);

                                // 立即同步方塊與冷卻狀態，保證百葉窗緊閉且原版 TileState 判定為已領取，避免對靠近的玩家再度激活動畫與聲音
                                plugin.getVaultTemplateManager().syncVaultBlock(block, template.getName());
                            }
                        }
                    }.runTaskLater(plugin, closeDelay);

                    // 原版試煉寶庫開獎無聊天室文字提示，僅透過百葉窗動畫、音效與物品噴發表現，此處僅於伺服器主控台記錄日誌
                    plugin.getConfigManager().log("vault-unlock",
                            "%player%", player.getName(),
                            "%name%", template.getName(),
                            "%count%", String.valueOf(rewards.size()),
                            "%world%", loc.getWorld().getName(),
                            "%x%", String.valueOf(loc.getBlockX()),
                            "%y%", String.valueOf(loc.getBlockY()),
                            "%z%", String.valueOf(loc.getBlockZ())
                    );
                }
            }
        }
    }

    private void openVaultWizard(Player player, ItemStack held) {
        // 檢查手持物品是否為草稿
        if (plugin.getItemManager().isDraftItem(held)) {
            if (plugin.getItemManager().isDraftExpired(held)) {
                plugin.getItemManager().markDraftExpired(held);
                plugin.getConfigManager().send(player, "draft-expired-reverted");
                plugin.getConfigManager().playSound(player, "error");
                plugin.getItemManager().removeDraft(held);

                String origName = plugin.getItemManager().getDraftOriginalName(held);
                if (origName != null && plugin.getVaultTemplateManager().getTemplate(origName) != null) {
                    VaultTemplate orig = plugin.getVaultTemplateManager().getTemplate(origName);
                    plugin.getItemManager().updatePlayerHeldVaultItem(player, orig);
                    VaultWizardContext context = new VaultWizardContext(plugin, player, orig.cloneTemplate(), held, true);
                    new VaultWizardStep1Gui(context).open();
                } else {
                    ItemStack blank = plugin.getItemManager().createBlankVaultItem(false);
                    player.getInventory().setItemInMainHand(blank);
                    VaultTemplate vt = new VaultTemplate("<未設定>", false, "<未設定>", new ItemStack(Material.TRIAL_KEY), 3, VaultCooldownMode.PLAYER_COOLDOWN, 10, new java.util.ArrayList<>());
                    VaultWizardContext context = new VaultWizardContext(plugin, player, vt, blank, false);
                    new VaultWizardStep1Gui(context).open();
                }
                return;
            }

            String draftId = plugin.getItemManager().getDraftId(held);
            clre20.customLootX.model.DraftSession session = plugin.getDraftManager().loadDraft(clre20.customLootX.model.DraftType.VAULT, draftId);
            if (session == null || !(session.getTemplateData() instanceof VaultTemplate vt)) {
                plugin.getItemManager().markDraftExpired(held);
                plugin.getConfigManager().send(player, "draft-expired-reverted");
                plugin.getConfigManager().playSound(player, "error");
                plugin.getItemManager().removeDraft(held);

                String origName = plugin.getItemManager().getDraftOriginalName(held);
                if (origName != null && plugin.getVaultTemplateManager().getTemplate(origName) != null) {
                    VaultTemplate orig = plugin.getVaultTemplateManager().getTemplate(origName);
                    plugin.getItemManager().updatePlayerHeldVaultItem(player, orig);
                    VaultWizardContext context = new VaultWizardContext(plugin, player, orig.cloneTemplate(), held, true);
                    new VaultWizardStep1Gui(context).open();
                } else {
                    ItemStack blank = plugin.getItemManager().createBlankVaultItem(false);
                    player.getInventory().setItemInMainHand(blank);
                    VaultTemplate nvt = new VaultTemplate("<未設定>", false, "<未設定>", new ItemStack(Material.TRIAL_KEY), 3, VaultCooldownMode.PLAYER_COOLDOWN, 10, new java.util.ArrayList<>());
                    VaultWizardContext context = new VaultWizardContext(plugin, player, nvt, blank, false);
                    new VaultWizardStep1Gui(context).open();
                }
                return;
            }

            boolean isExisting = session.getOriginalName() != null;
            VaultWizardContext context = new VaultWizardContext(plugin, player, vt, held, isExisting, session.getOriginalName(), draftId, true);
            plugin.getConfigManager().playSound(player, "click");
            plugin.getConfigManager().send(player, "draft-loaded");
            new VaultWizardStep1Gui(context).open();
            return;
        }

        String templateName = plugin.getItemManager().getVaultTemplateName(held);
        VaultTemplate template = null;
        boolean isExisting = false;

        if (templateName != null && !templateName.isEmpty()) {
            VaultTemplate loaded = plugin.getVaultTemplateManager().getTemplate(templateName);
            if (loaded != null) {
                template = loaded.cloneTemplate();
                isExisting = true;
            }
        }

        if (template == null) {
            String blockType = "NORMAL_VAULT";
            if (held.hasItemMeta()) {
                blockType = held.getItemMeta().getPersistentDataContainer().getOrDefault(
                        plugin.getItemManager().KEY_BLOCK_TYPE,
                        PersistentDataType.STRING,
                        "NORMAL_VAULT"
                );
            }
            boolean ominous = "OMINOUS_VAULT".equalsIgnoreCase(blockType);
            template = new VaultTemplate(
                    "<未設定>",
                    ominous,
                    "<未設定>",
                    new ItemStack(ominous ? Material.OMINOUS_TRIAL_KEY : Material.TRIAL_KEY),
                    3,
                    VaultCooldownMode.PLAYER_COOLDOWN,
                    10,
                    new java.util.ArrayList<>()
            );
        }

        VaultWizardContext context = new VaultWizardContext(plugin, player, template, held, isExisting, isExisting ? templateName : null, null, false);
        new VaultWizardStep1Gui(context).open();
    }

    private String formatTime(long totalSeconds) {
        if (totalSeconds < 60) {
            return totalSeconds + " 秒";
        }
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        if (seconds == 0) {
            return minutes + " 分鐘";
        }
        return minutes + " 分 " + seconds + " 秒";
    }
}
