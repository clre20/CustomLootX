package clre20.customLootX.listener;

import clre20.customLootX.CustomLootX;
import clre20.customLootX.gui.wizard.spawner.SpawnerWizardContext;
import clre20.customLootX.gui.wizard.spawner.SpawnerWizardStep1Gui;
import clre20.customLootX.model.SpawnerTemplate;
import clre20.customLootX.util.PermissionUtil;
import clre20.customLootX.util.TextUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.TrialSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

public class SpawnerBlockListener implements Listener {

    private final CustomLootX plugin;

    public SpawnerBlockListener(CustomLootX plugin) {
        this.plugin = plugin;
    }

    private String toLocationKey(Location loc) {
        if (loc == null) return "";
        return (loc.getWorld() != null ? loc.getWorld().getName() : "world") + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    /**
     * 放置自訂試煉生怪磚
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!plugin.getItemManager().isCustomSpawnerItem(item)) {
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
        if (plugin.getItemManager().isBlankSpawnerItem(item)) {
            event.setCancelled(true);
            plugin.getConfigManager().send(player, "empty-block-warning");
            plugin.getConfigManager().playSound(player, "error");
            return;
        }

        // 2. 取得配置名稱
        String templateName = plugin.getItemManager().getSpawnerTemplateName(item);
        if (templateName == null || templateName.isEmpty()) {
            event.setCancelled(true);
            plugin.getConfigManager().send(player, "empty-block-warning");
            return;
        }

        SpawnerTemplate template = plugin.getSpawnerTemplateManager().getTemplate(templateName);
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
                if (block.getType() != Material.TRIAL_SPAWNER) return;

                plugin.getSpawnerTemplateManager().syncSpawnerBlock(block, templateName);
                plugin.getSpawnerTemplateManager().registerSpawner(loc, templateName);

                plugin.getConfigManager().log("spawner-placed",
                        "%player%", player.getName(),
                        "%name%", template.getName(),
                        "%mob%", template.getSpawnedType().name(),
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
     * 挖破試煉生怪磚方塊清理戰鬥與掉落成品物品
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.TRIAL_SPAWNER) return;

        if (block.getState() instanceof TrialSpawner tsState) {
            PersistentDataContainer pdc = tsState.getPersistentDataContainer();
            if (pdc.has(plugin.getItemManager().KEY_CUSTOM_SPAWNER, PersistentDataType.BYTE)) {
                String templateName = pdc.get(plugin.getItemManager().KEY_SPAWNER_TEMPLATE_NAME, PersistentDataType.STRING);
                Location loc = block.getLocation();

                plugin.getSpawnerTemplateManager().unregisterSpawner(loc);
                plugin.getSpawnerTemplateManager().clearCooldown(loc);

                event.setDropItems(false);

                SpawnerTemplate template = plugin.getSpawnerTemplateManager().getTemplate(templateName);
                ItemStack toDrop = (template != null)
                        ? plugin.getItemManager().createTemplateSpawnerItem(template, 1)
                        : plugin.getItemManager().createBlankSpawnerItem(false);

                loc.getWorld().dropItemNaturally(loc.clone().add(0.5, 0.5, 0.5), toDrop);

                plugin.getConfigManager().log("spawner-broken",
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
     * 玩家互動事件：手持右鍵空氣開精靈、點擊世界上的生怪磚方塊
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        ItemStack held = event.getItem();
        Action action = event.getAction();

        // 1. 手持自訂生怪磚物品點擊空氣 -> 開啟步驟引導精靈 (需 OP 2+)
        if (plugin.getItemManager().isCustomSpawnerItem(held)) {
            if (action == Action.RIGHT_CLICK_AIR || (action == Action.RIGHT_CLICK_BLOCK && player.isSneaking())) {
                event.setCancelled(true);
                if (!PermissionUtil.hasAdminPermission(player)) {
                    plugin.getConfigManager().send(player, "no-permission");
                    plugin.getConfigManager().playSound(player, "error");
                    return;
                }
                openSpawnerWizard(player, held);
                return;
            } else if (action == Action.RIGHT_CLICK_BLOCK) {
                if (plugin.getItemManager().isDraftItem(held)) {
                    event.setCancelled(true);
                    plugin.getConfigManager().send(player, "cannot-place-draft");
                    plugin.getConfigManager().playSound(player, "error");
                    return;
                }
                if (plugin.getItemManager().isBlankSpawnerItem(held)) {
                    event.setCancelled(true);
                    plugin.getConfigManager().send(player, "empty-block-warning");
                    plugin.getConfigManager().playSound(player, "error");
                    return;
                }
                return;
            }
        }

        // 2. 右鍵點擊世界上的生怪磚方塊
        if (action == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Block block = event.getClickedBlock();
            if (block.getType() == Material.TRIAL_SPAWNER && block.getState() instanceof TrialSpawner tsState) {
                PersistentDataContainer pdc = tsState.getPersistentDataContainer();
                if (pdc.has(plugin.getItemManager().KEY_CUSTOM_SPAWNER, PersistentDataType.BYTE)) {
                    String templateName = pdc.get(plugin.getItemManager().KEY_SPAWNER_TEMPLATE_NAME, PersistentDataType.STRING);
                    SpawnerTemplate template = plugin.getSpawnerTemplateManager().getTemplate(templateName);
                    if (template != null) {
                        Location loc = block.getLocation();
                        plugin.getSpawnerTemplateManager().registerSpawner(loc, templateName);
                        long cd = plugin.getSpawnerTemplateManager().checkCooldownStatus(loc, player.getUniqueId(), template);
                        if (cd > 0) {
                            plugin.getConfigManager().send(player, "spawner-in-cooldown", "%time%", TextUtil.formatTimeSeconds(cd));
                            plugin.getConfigManager().playSound(player, "error");
                        } else if (cd == -1) {
                            plugin.getConfigManager().send(player, "spawner-already-completed");
                            plugin.getConfigManager().playSound(player, "error");
                        } else {
                            String mobDisplay = (template.getMobPool().size() > 1)
                                    ? (template.getMobPool().size() + " 種怪物 (機率)")
                                    : (!template.getMobPool().isEmpty() ? template.getMobPool().get(0).getDisplayName() : TextUtil.getMobDisplayName(template.getSpawnedType()));
                            plugin.getConfigManager().send(player, "spawner-ready-hint", "%mob%", mobDisplay);
                        }
                    }
                }
            }
        }
    }

    /**
     * 生怪磚怪物死亡事件
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        if (pdc.has(plugin.getItemManager().KEY_SPAWNER_MOB, PersistentDataType.BYTE)) {
            String locKey = pdc.get(plugin.getItemManager().KEY_SPAWNER_LOC, PersistentDataType.STRING);
            if (locKey != null) {
                plugin.getSpawnerTemplateManager().handleMobKilled(locKey, entity.getUniqueId());
            }
        }
    }

    /**
     * 攔截原版試煉生怪磚的自身自動生怪行為，確保完全由 CustomLootX 波次系統主導
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTrialSpawnerSpawn(org.bukkit.event.entity.TrialSpawnerSpawnEvent event) {
        TrialSpawner ts = event.getTrialSpawner();
        if (ts != null && ts.getPersistentDataContainer().has(plugin.getItemManager().KEY_CUSTOM_SPAWNER, PersistentDataType.BYTE)) {
            event.setCancelled(true);

            // 若目前並非由 CustomLootX 掌控的戰鬥進行中，立即平息原版自主激發的戰鬥狀態
            Location loc = ts.getLocation();
            String locKey = plugin.getSpawnerTemplateManager().toLocationKey(loc);
            if (!plugin.getSpawnerTemplateManager().hasActiveBattle(locKey)) {
                String templateName = ts.getPersistentDataContainer().get(plugin.getItemManager().KEY_SPAWNER_TEMPLATE_NAME, PersistentDataType.STRING);
                if (templateName != null) {
                    plugin.getSpawnerTemplateManager().syncSpawnerBlock(loc.getBlock(), templateName);
                }
            }
        }
    }

    /**
     * 區塊載入時自動同步區塊內已放置的自訂生怪磚狀態
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        plugin.getSpawnerTemplateManager().onChunkLoad(event.getChunk());
    }

    private void openSpawnerWizard(Player player, ItemStack item) {
        // 檢查手持物品是否為草稿
        if (plugin.getItemManager().isDraftItem(item)) {
            if (plugin.getItemManager().isDraftExpired(item)) {
                plugin.getItemManager().markDraftExpired(item);
                plugin.getConfigManager().send(player, "draft-expired-reverted");
                plugin.getConfigManager().playSound(player, "error");
                plugin.getItemManager().removeDraft(item);

                String origName = plugin.getItemManager().getDraftOriginalName(item);
                if (origName != null && plugin.getSpawnerTemplateManager().getTemplate(origName) != null) {
                    SpawnerTemplate orig = plugin.getSpawnerTemplateManager().getTemplate(origName);
                    ItemStack configured = plugin.getItemManager().createTemplateSpawnerItem(orig, item.getAmount());
                    player.getInventory().setItemInMainHand(configured);
                    SpawnerWizardContext context = new SpawnerWizardContext(plugin, player, orig.cloneTemplate(), configured, true);
                    new SpawnerWizardStep1Gui(context).open();
                } else {
                    ItemStack blank = plugin.getItemManager().createBlankSpawnerItem(false);
                    player.getInventory().setItemInMainHand(blank);
                    SpawnerTemplate st = new SpawnerTemplate("<未設定>", false, "<未設定>", org.bukkit.entity.EntityType.ZOMBIE, 6, 2, 3, 14, clre20.customLootX.model.VaultCooldownMode.PLAYER_COOLDOWN, 15, 3, new java.util.ArrayList<>());
                    SpawnerWizardContext context = new SpawnerWizardContext(plugin, player, st, blank, false);
                    new SpawnerWizardStep1Gui(context).open();
                }
                return;
            }

            String draftId = plugin.getItemManager().getDraftId(item);
            clre20.customLootX.model.DraftSession session = plugin.getDraftManager().loadDraft(clre20.customLootX.model.DraftType.SPAWNER, draftId);
            if (session == null || !(session.getTemplateData() instanceof SpawnerTemplate st)) {
                plugin.getItemManager().markDraftExpired(item);
                plugin.getConfigManager().send(player, "draft-expired-reverted");
                plugin.getConfigManager().playSound(player, "error");
                plugin.getItemManager().removeDraft(item);

                String origName = plugin.getItemManager().getDraftOriginalName(item);
                if (origName != null && plugin.getSpawnerTemplateManager().getTemplate(origName) != null) {
                    SpawnerTemplate orig = plugin.getSpawnerTemplateManager().getTemplate(origName);
                    ItemStack configured = plugin.getItemManager().createTemplateSpawnerItem(orig, item.getAmount());
                    player.getInventory().setItemInMainHand(configured);
                    SpawnerWizardContext context = new SpawnerWizardContext(plugin, player, orig.cloneTemplate(), configured, true);
                    new SpawnerWizardStep1Gui(context).open();
                } else {
                    ItemStack blank = plugin.getItemManager().createBlankSpawnerItem(false);
                    player.getInventory().setItemInMainHand(blank);
                    SpawnerTemplate nst = new SpawnerTemplate("<未設定>", false, "<未設定>", org.bukkit.entity.EntityType.ZOMBIE, 6, 2, 3, 14, clre20.customLootX.model.VaultCooldownMode.PLAYER_COOLDOWN, 15, 3, new java.util.ArrayList<>());
                    SpawnerWizardContext context = new SpawnerWizardContext(plugin, player, nst, blank, false);
                    new SpawnerWizardStep1Gui(context).open();
                }
                return;
            }

            String origName = session.getOriginalName();
            boolean isExisting = origName != null && !origName.trim().isEmpty();
            SpawnerWizardContext context = new SpawnerWizardContext(plugin, player, st, item, isExisting, origName, draftId, true);
            plugin.getConfigManager().playSound(player, "click");
            plugin.getConfigManager().send(player, "draft-loaded");
            new SpawnerWizardStep1Gui(context).open();
            return;
        }

        boolean isBlank = plugin.getItemManager().isBlankSpawnerItem(item);
        SpawnerTemplate template;

        if (isBlank) {
            String blockType = item.getItemMeta().getPersistentDataContainer().get(plugin.getItemManager().KEY_BLOCK_TYPE, PersistentDataType.STRING);
            boolean ominous = "OMINOUS_SPAWNER".equalsIgnoreCase(blockType);
            template = new SpawnerTemplate(
                    "<未設定>",
                    ominous,
                    "<未設定>",
                    org.bukkit.entity.EntityType.ZOMBIE,
                    6,
                    2,
                    3,
                    14,
                    clre20.customLootX.model.VaultCooldownMode.PLAYER_COOLDOWN,
                    15,
                    3,
                    new java.util.ArrayList<>()
            );
            SpawnerWizardContext context = new SpawnerWizardContext(plugin, player, template, item, false);
            new SpawnerWizardStep1Gui(context).open();
        } else {
            String templateName = plugin.getItemManager().getSpawnerTemplateName(item);
            SpawnerTemplate existing = plugin.getSpawnerTemplateManager().getTemplate(templateName);
            if (existing != null) {
                template = existing.cloneTemplate();
            } else {
                template = new SpawnerTemplate(
                        templateName != null ? templateName : "<未設定>",
                        false,
                        templateName != null ? templateName : "<未設定>",
                        org.bukkit.entity.EntityType.ZOMBIE,
                        6,
                        2,
                        3,
                        14,
                        clre20.customLootX.model.VaultCooldownMode.PLAYER_COOLDOWN,
                        15,
                        3,
                        new java.util.ArrayList<>()
                );
            }
            SpawnerWizardContext context = new SpawnerWizardContext(plugin, player, template, item, true, templateName, null, false);
            new SpawnerWizardStep1Gui(context).open();
        }
        plugin.getConfigManager().playSound(player, "click");
    }
}
