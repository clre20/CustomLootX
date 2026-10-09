package clre20.customLootX.listener;

import clre20.customLootX.CustomLootX;
import clre20.customLootX.gui.wizard.WizardContext;
import clre20.customLootX.model.LootTemplate;
import clre20.customLootX.util.PermissionUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BrushableBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.block.BlockFace;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerInteractListener implements Listener {

    private final CustomLootX plugin;

    private static class BrushingEntry {
        final Location location;
        final String templateName;
        final long startTimeMillis;

        BrushingEntry(Location location, String templateName, long startTimeMillis) {
            this.location = location;
            this.templateName = templateName;
            this.startTimeMillis = startTimeMillis;
        }
    }

    private final Map<String, BrushingEntry> activeBrushingEntries = new ConcurrentHashMap<>();
    private org.bukkit.scheduler.BukkitTask brushingTickerTask;
    private final Map<String, BlockFace> lastBrushedFace = new ConcurrentHashMap<>();

    public PlayerInteractListener(CustomLootX plugin) {
        this.plugin = plugin;
    }

    public BlockFace getLastBrushedFace(Location loc) {
        if (loc == null) return null;
        return lastBrushedFace.get(toLocationKey(loc));
    }

    public void removeLastBrushedFace(Location loc) {
        if (loc == null) return;
        lastBrushedFace.remove(toLocationKey(loc));
    }

    private String toLocationKey(Location loc) {
        return (loc.getWorld() != null ? loc.getWorld().getName() : "world") + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        ItemStack held = event.getItem();
        Action action = event.getAction();

        // 1. Check if holding custom suspicious block item
        if (plugin.getItemManager().isCustomLootItem(held)) {
            if (action == Action.RIGHT_CLICK_AIR) {
                // Holding in air right-click -> Open Step-by-step Wizard
                event.setCancelled(true);
                if (!PermissionUtil.hasAdminPermission(player)) {
                    plugin.getConfigManager().send(player, "no-permission");
                    plugin.getConfigManager().playSound(player, "error");
                    return;
                }
                openWizardForHeldItem(player, held);
                return;
            } else if (action == Action.RIGHT_CLICK_BLOCK) {
                // If sneaking -> open Wizard instead of placing
                if (player.isSneaking()) {
                    event.setCancelled(true);
                    if (!PermissionUtil.hasAdminPermission(player)) {
                        plugin.getConfigManager().send(player, "no-permission");
                        plugin.getConfigManager().playSound(player, "error");
                        return;
                    }
                    openWizardForHeldItem(player, held);
                    return;
                }

                // If draft -> cannot place on ground, warn player
                if (plugin.getItemManager().isDraftItem(held)) {
                    event.setCancelled(true);
                    plugin.getConfigManager().send(player, "cannot-place-draft");
                    plugin.getConfigManager().playSound(player, "error");
                    return;
                }

                // If blank -> cannot place on ground, warn player
                if (plugin.getItemManager().isBlankCustomItem(held)) {
                    event.setCancelled(true);
                    plugin.getConfigManager().send(player, "empty-block-warning");
                    plugin.getConfigManager().playSound(player, "error");
                    return;
                }
                // If configured item -> allow placing (BlockPlaceEvent will handle it)
                return;
            }
        }

        // 2. Interacting with placed block in world
        if (action == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Block block = event.getClickedBlock();
            if (block.getType() == Material.SUSPICIOUS_SAND || block.getType() == Material.SUSPICIOUS_GRAVEL) {
                if (block.getState(false) instanceof BrushableBlock brushable) {
                    PersistentDataContainer pdc = brushable.getPersistentDataContainer();
                    if (pdc.has(plugin.getItemManager().KEY_CUSTOM_LOOT, PersistentDataType.BYTE)) {
                        String templateName = pdc.get(plugin.getItemManager().KEY_TEMPLATE_NAME, PersistentDataType.STRING);

                        // If not holding a brush, do not react
                        ItemStack tool = player.getInventory().getItemInMainHand();
                        if (tool.getType() != Material.BRUSH) {
                            return;
                        }

                        // Holding a brush: record face and track brushing completion for reset
                        BlockFace face = event.getBlockFace();
                        lastBrushedFace.put(toLocationKey(block.getLocation()), face);

                        if (templateName != null && !templateName.isEmpty()) {
                            trackBrushing(block.getLocation(), templateName);
                        }
                    }
                }
            }
        }
    }

    private void openWizardForHeldItem(Player player, ItemStack held) {
        // 檢查手持物品是否為草稿
        if (plugin.getItemManager().isDraftItem(held)) {
            if (plugin.getItemManager().isDraftExpired(held)) {
                plugin.getItemManager().markDraftExpired(held);
                plugin.getConfigManager().send(player, "draft-expired-reverted");
                plugin.getConfigManager().playSound(player, "error");
                plugin.getItemManager().removeDraft(held);

                String origName = plugin.getItemManager().getDraftOriginalName(held);
                if (origName != null && plugin.getTemplateManager().hasTemplate(origName)) {
                    LootTemplate orig = plugin.getTemplateManager().getTemplate(origName);
                    plugin.getItemManager().updateHoldingItem(player, orig);
                    WizardContext context = new WizardContext(plugin, player, orig.cloneTemplate(), false);
                    context.openStep1();
                } else {
                    ItemStack blank = plugin.getItemManager().createBlankItem(held.getType());
                    player.getInventory().setItemInMainHand(blank);
                    WizardContext context = new WizardContext(plugin, player, new LootTemplate("", held.getType()), true);
                    context.openStep1();
                }
                return;
            }

            String draftId = plugin.getItemManager().getDraftId(held);
            clre20.customLootX.model.DraftSession session = plugin.getDraftManager().loadDraft(clre20.customLootX.model.DraftType.SUSPICIOUS, draftId);
            if (session == null || !(session.getTemplateData() instanceof LootTemplate lt)) {
                plugin.getItemManager().markDraftExpired(held);
                plugin.getConfigManager().send(player, "draft-expired-reverted");
                plugin.getConfigManager().playSound(player, "error");
                plugin.getItemManager().removeDraft(held);

                String origName = plugin.getItemManager().getDraftOriginalName(held);
                if (origName != null && plugin.getTemplateManager().hasTemplate(origName)) {
                    LootTemplate orig = plugin.getTemplateManager().getTemplate(origName);
                    plugin.getItemManager().updateHoldingItem(player, orig);
                    WizardContext context = new WizardContext(plugin, player, orig.cloneTemplate(), false);
                    context.openStep1();
                } else {
                    ItemStack blank = plugin.getItemManager().createBlankItem(held.getType());
                    player.getInventory().setItemInMainHand(blank);
                    WizardContext context = new WizardContext(plugin, player, new LootTemplate("", held.getType()), true);
                    context.openStep1();
                }
                return;
            }

            boolean isNew = session.getOriginalName() == null;
            WizardContext context = new WizardContext(plugin, player, lt, isNew, held, session.getOriginalName(), draftId, true);
            plugin.getConfigManager().playSound(player, "click");
            plugin.getConfigManager().send(player, "draft-loaded");
            context.openStep1();
            return;
        }

        String templateName = plugin.getItemManager().getTemplateName(held);
        LootTemplate template = null;
        boolean isNew = true;

        if (templateName != null && !templateName.trim().isEmpty()) {
            LootTemplate existing = plugin.getTemplateManager().getTemplate(templateName);
            if (existing != null) {
                template = existing.cloneTemplate();
                isNew = false;
            }
        }

        if (template == null) {
            // New blank template draft
            template = new LootTemplate("", held.getType());
            template.setResetEnabled(false);
            template.setResetSeconds(300);
            isNew = true;
        }

        plugin.getConfigManager().playSound(player, "click");
        WizardContext context = new WizardContext(plugin, player, template, isNew);
        context.openStep1();

        plugin.getConfigManager().log("gui-open",
                "%player%", player.getName(),
                "%mode%", isNew ? "新建草稿" : "現有配置",
                "%name%", template.getName().isEmpty() ? "未命名" : template.getName()
        );
    }

    /**
     * 追蹤玩家刷可疑方塊之進程，改用單一集中計時器統一巡檢，徹底消除獨立 Task 輪詢浪費
     */
    public void trackBrushing(Location loc, String templateName) {
        String key = toLocationKey(loc);
        if (activeBrushingEntries.containsKey(key)) {
            return;
        }

        LootTemplate template = plugin.getTemplateManager().getTemplate(templateName);
        if (template == null || !template.isResetEnabled()) {
            return;
        }

        activeBrushingEntries.put(key, new BrushingEntry(loc.clone(), templateName, System.currentTimeMillis()));

        if (brushingTickerTask == null || brushingTickerTask.isCancelled()) {
            brushingTickerTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (activeBrushingEntries.isEmpty()) {
                        cancel();
                        brushingTickerTask = null;
                        return;
                    }

                    long now = System.currentTimeMillis();
                    Iterator<Map.Entry<String, BrushingEntry>> it = activeBrushingEntries.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<String, BrushingEntry> entry = it.next();
                        BrushingEntry bEntry = entry.getValue();
                        Location bLoc = bEntry.location;

                        // 逾時 10 秒自動清除
                        if (now - bEntry.startTimeMillis > 10000L) {
                            it.remove();
                            continue;
                        }

                        if (bLoc.getWorld() == null || !bLoc.getWorld().isChunkLoaded(bLoc.getBlockX() >> 4, bLoc.getBlockZ() >> 4)) {
                            it.remove();
                            continue;
                        }

                        Block b = bLoc.getBlock();
                        Material type = b.getType();

                        // 刷洗完成：已變成普通沙子或礫石
                        if (type == Material.SAND || type == Material.GRAVEL) {
                            it.remove();
                            if (!plugin.getResetManager().hasPendingReset(bLoc)) {
                                LootTemplate t = plugin.getTemplateManager().getTemplate(bEntry.templateName);
                                if (t != null && t.isResetEnabled()) {
                                    plugin.getResetManager().scheduleReset(bLoc, bEntry.templateName, t.getResetSeconds());
                                }
                            }
                            continue;
                        }

                        // 方塊被挖除 (空氣) 或已被替換為其他材質
                        if (type != Material.SUSPICIOUS_SAND && type != Material.SUSPICIOUS_GRAVEL) {
                            it.remove();
                        }
                    }

                    if (activeBrushingEntries.isEmpty()) {
                        cancel();
                        brushingTickerTask = null;
                    }
                }
            }.runTaskTimer(plugin, 5L, 5L);
        }
    }
}
