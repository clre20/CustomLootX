package clre20.customLootX.listener;

import clre20.customLootX.CustomLootX;
import clre20.customLootX.model.LootTemplate;
import clre20.customLootX.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BrushableBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

public class BlockEventListener implements Listener {

    private final CustomLootX plugin;
    private final java.util.Set<String> brokenLocations = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public BlockEventListener(CustomLootX plugin) {
        this.plugin = plugin;
    }

    private String toLocationKey(Location loc) {
        if (loc == null) return "";
        return (loc.getWorld() != null ? loc.getWorld().getName() : "world") + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        Location loc = event.getBlockPlaced().getLocation();

        // Safety: whenever any block is placed at this location, cancel any old pending reset session!
        // This prevents old reset timer from overwriting newly placed blocks
        plugin.getResetManager().cancelReset(loc);

        if (!plugin.getItemManager().isCustomLootItem(item)) {
            return;
        }

        Player player = event.getPlayer();

        // 檢查是否為未儲存草稿
        if (plugin.getItemManager().isDraftItem(item)) {
            event.setCancelled(true);
            plugin.getConfigManager().send(player, "cannot-place-draft");
            plugin.getConfigManager().playSound(player, "error");
            return;
        }

        // Check if blank
        if (plugin.getItemManager().isBlankCustomItem(item)) {
            event.setCancelled(true);
            plugin.getConfigManager().send(player, "empty-block-warning");
            plugin.getConfigManager().playSound(player, "error");
            return;
        }

        String templateName = plugin.getItemManager().getTemplateName(item);
        LootTemplate template = plugin.getTemplateManager().getTemplate(templateName);

        if (template == null) {
            event.setCancelled(true);
            plugin.getConfigManager().send(player, "template-not-found", "%name%", String.valueOf(templateName));
            plugin.getConfigManager().playSound(player, "error");
            return;
        }

        if (!template.isValidTotal()) {
            event.setCancelled(true);
            double total = template.getTotalChance();
            double diff = 100.00 - total;
            plugin.getConfigManager().send(player, "sum-not-100",
                    "%total%", TextUtil.formatPercent(total),
                    "%diff%", (diff > 0 ? "+" : "") + TextUtil.formatPercent(diff)
            );
            plugin.getConfigManager().playSound(player, "error");
            return;
        }

        // Apply custom loot properties to placed BrushableBlock on next tick
        Bukkit.getScheduler().runTask(plugin, () -> {
            Block block = loc.getBlock();
            if (block.getState() instanceof BrushableBlock brushable) {
                // Clear any vanilla archaeology loot table
                brushable.clearLootTable();
                brushable.setLootTable(null);

                PersistentDataContainer pdc = brushable.getPersistentDataContainer();
                pdc.set(plugin.getItemManager().KEY_CUSTOM_LOOT, PersistentDataType.BYTE, (byte) 1);
                pdc.set(plugin.getItemManager().KEY_TEMPLATE_NAME, PersistentDataType.STRING, template.getName());
                pdc.set(plugin.getItemManager().KEY_BLOCK_TYPE, PersistentDataType.STRING, template.getType().name());

                ItemStack rolled = template.rollItem();
                if (rolled != null && !rolled.getType().isAir()) {
                    pdc.set(plugin.getItemManager().KEY_ROLLED_ITEM, PersistentDataType.BYTE_ARRAY, rolled.serializeAsBytes());
                    brushable.setItem(rolled.clone());
                } else {
                    pdc.set(plugin.getItemManager().KEY_ROLLED_ITEM, PersistentDataType.BYTE_ARRAY, new byte[0]);
                    brushable.setItem(null);
                }

                brushable.update(true, true);

                String rolledDesc = clre20.customLootX.util.TextUtil.getItemDescription(rolled);
                String typeDesc = template.getType() == Material.SUSPICIOUS_SAND
                        ? plugin.getConfigManager().getText("items.configured.type-sand", "可疑沙")
                        : plugin.getConfigManager().getText("items.configured.type-gravel", "可疑礫石");

                plugin.getConfigManager().log("block-place",
                        "%player%", player.getName(),
                        "%world%", loc.getWorld() != null ? loc.getWorld().getName() : "world",
                        "%x%", loc.getBlockX(),
                        "%y%", loc.getBlockY(),
                        "%z%", loc.getBlockZ(),
                        "%name%", template.getName(),
                        "%type%", typeDesc,
                        "%rolled%", rolledDesc
                );
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location loc = block.getLocation();
        String locKey = toLocationKey(loc);

        // Immediately cancel any pending reset session at this location when broken
        plugin.getResetManager().cancelReset(loc);
        plugin.getPlayerInteractListener().removeLastBrushedFace(loc);

        if (block.getState() instanceof BrushableBlock brushable) {
            PersistentDataContainer pdc = brushable.getPersistentDataContainer();
            if (pdc.has(plugin.getItemManager().KEY_CUSTOM_LOOT, PersistentDataType.BYTE)) {
                String templateName = pdc.get(plugin.getItemManager().KEY_TEMPLATE_NAME, PersistentDataType.STRING);

                // Mark location as broken so onBlockDropItem will NOT drop treasure
                brokenLocations.add(locKey);
                event.setDropItems(false);

                // Safety cleanup on next tick in case onBlockDropItem does not fire
                Bukkit.getScheduler().runTask(plugin, () -> brokenLocations.remove(locKey));

                plugin.getConfigManager().log("block-break",
                        "%player%", event.getPlayer().getName(),
                        "%world%", loc.getWorld() != null ? loc.getWorld().getName() : "world",
                        "%x%", loc.getBlockX(),
                        "%y%", loc.getBlockY(),
                        "%z%", loc.getBlockZ(),
                        "%name%", templateName != null ? templateName : "未知"
                );
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLootGenerate(LootGenerateEvent event) {
        if (event.getInventoryHolder() instanceof BrushableBlock brushable) {
            PersistentDataContainer pdc = brushable.getPersistentDataContainer();
            if (pdc.has(plugin.getItemManager().KEY_CUSTOM_LOOT, PersistentDataType.BYTE)) {
                // Block vanilla loot tables from overwriting custom loot
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDropItem(BlockDropItemEvent event) {
        if (event.getBlockState() instanceof BrushableBlock brushable) {
            PersistentDataContainer pdc = brushable.getPersistentDataContainer();
            if (pdc.has(plugin.getItemManager().KEY_CUSTOM_LOOT, PersistentDataType.BYTE)) {
                Location loc = event.getBlock().getLocation();
                String locKey = toLocationKey(loc);

                // Check if this block was broken by digging/mining or not triggered by player or block became air: cancel all drops!
                if (brokenLocations.remove(locKey) || event.getPlayer() == null || event.getBlock().getType().isAir()) {
                    for (Item itemEntity : event.getItems()) {
                        itemEntity.remove();
                    }
                    event.getItems().clear();
                    return;
                }

                String templateName = pdc.get(plugin.getItemManager().KEY_TEMPLATE_NAME, PersistentDataType.STRING);
                Player player = event.getPlayer();

                // 1. Calculate drop location based on the face the crosshair aimed at
                BlockFace face = plugin.getPlayerInteractListener().getLastBrushedFace(loc);
                if (face == null) {
                    face = BlockFace.UP;
                }
                plugin.getPlayerInteractListener().removeLastBrushedFace(loc);

                Location dropLoc = loc.clone().add(
                        0.5 + face.getModX() * 0.55,
                        0.5 + face.getModY() * 0.55,
                        0.5 + face.getModZ() * 0.55
                );
                Vector velocity = new Vector(face.getModX() * 0.12, Math.max(0.06, face.getModY() * 0.12), face.getModZ() * 0.12);

                // 2. Retrieve expected item from brushable or PDC to guarantee exact match with preview
                ItemStack expectedItem = brushable.getItem();
                if ((expectedItem == null || expectedItem.getType().isAir()) && pdc.has(plugin.getItemManager().KEY_ROLLED_ITEM, PersistentDataType.BYTE_ARRAY)) {
                    byte[] bytes = pdc.get(plugin.getItemManager().KEY_ROLLED_ITEM, PersistentDataType.BYTE_ARRAY);
                    if (bytes != null && bytes.length > 0) {
                        try {
                            expectedItem = ItemStack.deserializeBytes(bytes);
                        } catch (Exception ignored) {
                        }
                    }
                }

                // 3. Ensure exact drop match and location
                if (expectedItem == null || expectedItem.getType().isAir()) {
                    for (Item itemEntity : event.getItems()) {
                        itemEntity.remove();
                    }
                    event.getItems().clear();
                } else {
                    if (event.getItems().isEmpty()) {
                        if (loc.getWorld() != null) {
                            Item itemEntity = loc.getWorld().dropItem(dropLoc, expectedItem.clone());
                            itemEntity.setVelocity(velocity);
                        }
                    } else {
                        boolean first = true;
                        for (Item itemEntity : event.getItems()) {
                            if (first) {
                                itemEntity.setItemStack(expectedItem.clone());
                                itemEntity.teleport(dropLoc);
                                itemEntity.setVelocity(velocity);
                                first = false;
                            } else {
                                itemEntity.remove();
                            }
                        }
                    }
                }

                // Detailed console log for brushing completion
                LootTemplate template = (templateName != null && !templateName.isEmpty())
                        ? plugin.getTemplateManager().getTemplate(templateName)
                        : null;
                double chance = (template != null) ? template.getItemChance(expectedItem) : 0.0;
                String itemDesc = clre20.customLootX.util.TextUtil.getItemDescription(expectedItem);

                if (expectedItem == null || expectedItem.getType().isAir()) {
                    plugin.getConfigManager().log("brush-air",
                            "%player%", player.getName(),
                            "%world%", loc.getWorld() != null ? loc.getWorld().getName() : "world",
                            "%x%", loc.getBlockX(),
                            "%y%", loc.getBlockY(),
                            "%z%", loc.getBlockZ(),
                            "%item%", itemDesc,
                            "%name%", templateName != null ? templateName : "未知",
                            "%chance%", clre20.customLootX.util.TextUtil.formatPercent(chance)
                    );
                } else {
                    plugin.getConfigManager().log("brush-loot",
                            "%player%", player.getName(),
                            "%world%", loc.getWorld() != null ? loc.getWorld().getName() : "world",
                            "%x%", loc.getBlockX(),
                            "%y%", loc.getBlockY(),
                            "%z%", loc.getBlockZ(),
                            "%item%", itemDesc,
                            "%name%", templateName != null ? templateName : "未知",
                            "%chance%", clre20.customLootX.util.TextUtil.formatPercent(chance),
                            "%face%", face.name()
                    );
                }

                // 4. Trigger reset if enabled
                if (template != null && template.isResetEnabled()) {
                    if (!plugin.getResetManager().hasPendingReset(loc)) {
                        plugin.getResetManager().scheduleReset(loc, templateName, template.getResetMinutes());
                    }
                }
            }
        }
    }
}
