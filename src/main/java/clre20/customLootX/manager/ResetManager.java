package clre20.customLootX.manager;

import clre20.customLootX.CustomLootX;
import clre20.customLootX.model.LootTemplate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BrushableBlock;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ResetManager {

    private final CustomLootX plugin;
    private final Map<String, ResetSession> pendingResets = new ConcurrentHashMap<>();

    public static class ResetSession {
        final UUID sessionId;
        final Location location;
        final String templateName;
        final long scheduledAt;
        final long triggerTimeMillis;
        BukkitTask task;

        public ResetSession(UUID sessionId, Location location, String templateName, long delayTicks) {
            this.sessionId = sessionId;
            this.location = location.clone();
            this.templateName = templateName;
            this.scheduledAt = System.currentTimeMillis();
            this.triggerTimeMillis = scheduledAt + (delayTicks * 50L);
        }
    }

    public ResetManager(CustomLootX plugin) {
        this.plugin = plugin;
    }

    private String toLocationKey(Location loc) {
        return (loc.getWorld() != null ? loc.getWorld().getName() : "world") + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    /**
     * Schedule a reset task for a suspicious block that has been brushed.
     */
    public void scheduleReset(Location loc, String templateName, int minutes) {
        if (loc == null || loc.getWorld() == null || templateName == null || minutes <= 0) {
            return;
        }

        String locKey = toLocationKey(loc);
        if (pendingResets.containsKey(locKey)) {
            // Already scheduled
            return;
        }

        UUID sessionId = UUID.randomUUID();
        long delayTicks = (long) minutes * 60L * 20L;

        ResetSession session = new ResetSession(sessionId, loc, templateName, delayTicks);

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                executeReset(locKey, session);
            } catch (Exception e) {
                plugin.logWarn("&b[可疑方塊·重置]&c 執行方塊重置時發生錯誤: " + e.getMessage());
            }
        }, delayTicks);

        session.task = task;
        pendingResets.put(locKey, session);

        plugin.getConfigManager().log("reset-scheduled",
                "%world%", loc.getWorld() != null ? loc.getWorld().getName() : "world",
                "%x%", loc.getBlockX(),
                "%y%", loc.getBlockY(),
                "%z%", loc.getBlockZ(),
                "%name%", templateName,
                "%minutes%", minutes
        );
    }

    /**
     * Cancel any pending reset at this location.
     * Prevents old reset tasks from replacing newly placed blocks when an old block was broken and replaced.
     */
    public void cancelReset(Location loc) {
        if (loc == null) return;
        String locKey = toLocationKey(loc);
        ResetSession session = pendingResets.remove(locKey);
        if (session != null) {
            if (session.task != null) {
                try {
                    session.task.cancel();
                } catch (Exception ignored) {
                }
            }
            plugin.getConfigManager().log("reset-cancelled",
                    "%world%", loc.getWorld() != null ? loc.getWorld().getName() : "world",
                    "%x%", loc.getBlockX(),
                    "%y%", loc.getBlockY(),
                    "%z%", loc.getBlockZ(),
                    "%name%", session.templateName
            );
        }
    }

    private void executeReset(String locKey, ResetSession session) {
        // Double check session identity to ensure it was not replaced or cancelled
        ResetSession current = pendingResets.get(locKey);
        if (current == null || !current.sessionId.equals(session.sessionId)) {
            return;
        }
        pendingResets.remove(locKey);

        Location loc = session.location;
        if (loc.getWorld() == null) return;

        // Ensure chunk is loaded
        if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
            loc.getWorld().getChunkAt(loc);
        }

        Block block = loc.getBlock();
        // Crucial safety check: The block must still be the brushed ordinary sand or gravel!
        // If player broke it (AIR) or placed another block (STONE, DIRT, etc.), DO NOT OVERWRITE!
        if (block.getType() != Material.SAND && block.getType() != Material.GRAVEL) {
            plugin.getConfigManager().log("reset-skipped",
                    "%world%", loc.getWorld() != null ? loc.getWorld().getName() : "world",
                    "%x%", loc.getBlockX(),
                    "%y%", loc.getBlockY(),
                    "%z%", loc.getBlockZ(),
                    "%type%", block.getType().name(),
                    "%name%", session.templateName
            );
            return;
        }

        LootTemplate template = plugin.getTemplateManager().getTemplate(session.templateName);
        if (template == null) {
            plugin.getConfigManager().log("reset-not-found",
                    "%world%", loc.getWorld() != null ? loc.getWorld().getName() : "world",
                    "%x%", loc.getBlockX(),
                    "%y%", loc.getBlockY(),
                    "%z%", loc.getBlockZ(),
                    "%name%", session.templateName
            );
            return;
        }

        // Restore to suspicious block of the latest template type
        Material targetType = template.getType();
        block.setType(targetType);

        // Schedule 1 tick later to guarantee BlockEntity is completely instantiated in the chunk
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (block.getState() instanceof BrushableBlock brushable) {
                // Clear any vanilla archaeology loot table
                brushable.clearLootTable();
                brushable.setLootTable(null);

                PersistentDataContainer pdc = brushable.getPersistentDataContainer();
                pdc.set(plugin.getItemManager().KEY_CUSTOM_LOOT, PersistentDataType.BYTE, (byte) 1);
                pdc.set(plugin.getItemManager().KEY_TEMPLATE_NAME, PersistentDataType.STRING, template.getName());
                pdc.set(plugin.getItemManager().KEY_BLOCK_TYPE, PersistentDataType.STRING, targetType.name());

                ItemStack rolled = template.rollItem();
                if (rolled != null && !rolled.getType().isAir()) {
                    pdc.set(plugin.getItemManager().KEY_ROLLED_ITEM, PersistentDataType.BYTE_ARRAY, rolled.serializeAsBytes());
                    brushable.setItem(rolled.clone());
                } else {
                    pdc.set(plugin.getItemManager().KEY_ROLLED_ITEM, PersistentDataType.BYTE_ARRAY, new byte[0]);
                    brushable.setItem(null);
                }

                brushable.update(true, true);

                // Particle and sound effect
                try {
                    loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0.5, 0.5, 0.5), 10, 0.3, 0.3, 0.3, 0.05);
                    plugin.getConfigManager().playSound(null, "reset");
                    loc.getWorld().playSound(loc, Sound.BLOCK_SAND_PLACE, 1.0f, 1.0f);
                } catch (Exception ignored) {
                }

                String rolledDesc = clre20.customLootX.util.TextUtil.getItemDescription(rolled);
                String typeDesc = targetType == Material.SUSPICIOUS_SAND
                        ? plugin.getConfigManager().getText("items.configured.type-sand", "可疑沙")
                        : plugin.getConfigManager().getText("items.configured.type-gravel", "可疑礫石");

                plugin.getConfigManager().log("reset-executed",
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

    public boolean hasPendingReset(Location loc) {
        if (loc == null) return false;
        return pendingResets.containsKey(toLocationKey(loc));
    }

    public void cancelAll() {
        for (ResetSession session : pendingResets.values()) {
            if (session.task != null) {
                try {
                    session.task.cancel();
                } catch (Exception ignored) {
                }
            }
        }
        pendingResets.clear();
    }
}
