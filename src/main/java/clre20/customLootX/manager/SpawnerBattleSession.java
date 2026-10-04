package clre20.customLootX.manager;

import clre20.customLootX.CustomLootX;
import clre20.customLootX.model.SpawnerTemplate;
import clre20.customLootX.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.TrialSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 試煉生怪磚單次戰鬥進程監控器
 */
public class SpawnerBattleSession {

    private final CustomLootX plugin;
    private final SpawnerTemplateManager manager;
    private final String locKey;
    private final Location spawnerLocation;
    private final SpawnerTemplate template;

    private final Set<UUID> participatingPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> activeMobUuids = ConcurrentHashMap.newKeySet();

    private int mobsSpawnedCount = 0;
    private int mobsKilledCount = 0;
    private int currentWaveIndex = 0;
    private boolean currentWaveSpawned = false;
    private long lastWaveClearedTimeMs = 0L;
    private long lastSpawnTimeMs = 0L;
    private final long battleStartTimeMs;
    private long lastPlayerSeenTimeMs;

    private boolean finished = false;
    private boolean ejecting = false;
    private org.bukkit.scheduler.BukkitTask visualKeeper;

    public SpawnerBattleSession(CustomLootX plugin, SpawnerTemplateManager manager, String locKey,
                                Location spawnerLocation, SpawnerTemplate template, Collection<Player> initialPlayers) {
        this.plugin = plugin;
        this.manager = manager;
        this.locKey = locKey;
        this.spawnerLocation = spawnerLocation.clone();
        this.template = template;
        this.battleStartTimeMs = System.currentTimeMillis();
        this.lastPlayerSeenTimeMs = battleStartTimeMs;

        if (initialPlayers != null) {
            for (Player p : initialPlayers) {
                this.participatingPlayers.add(p.getUniqueId());
            }
        }
    }

    /**
     * 初始化並啟動試煉戰鬥
     */
    public void start() {
        Block block = spawnerLocation.getBlock();
        if (block.getState() instanceof org.bukkit.block.TrialSpawner tsState) {
            tsState.setOminous(template.isOminous());
            for (UUID uuid : participatingPlayers) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline() && p.getWorld().equals(spawnerLocation.getWorld())) {
                    tsState.startTrackingPlayer(p);
                }
            }
            tsState.update(true, false);
        }

        if (block.getBlockData() instanceof TrialSpawner trialSpawnerData) {
            trialSpawnerData.setOminous(template.isOminous());
            trialSpawnerData.setTrialSpawnerState(TrialSpawner.State.ACTIVE);
            block.setBlockData(trialSpawnerData, true);
        }

        World world = spawnerLocation.getWorld();
        if (world != null) {
            Sound activateSound = template.isOminous()
                    ? Sound.BLOCK_TRIAL_SPAWNER_OMINOUS_ACTIVATE
                    : Sound.BLOCK_TRIAL_SPAWNER_DETECT_PLAYER;
            world.playSound(spawnerLocation, activateSound, 1.5f, 1.0f);

            Particle particle = template.isOminous()
                    ? Particle.TRIAL_SPAWNER_DETECTION_OMINOUS
                    : Particle.TRIAL_SPAWNER_DETECTION;
            world.spawnParticle(particle, spawnerLocation.clone().add(0.5, 0.5, 0.5), 30, 0.5, 0.5, 0.5, 0.05);
        }

        // 立即觸發首波怪物的生成
        spawnCurrentWave();
    }

    /**
     * 戰鬥每秒心跳檢測
     */
    public void tick() {
        if (finished || ejecting) return;

        World world = spawnerLocation.getWorld();
        if (world == null || !world.isChunkLoaded(spawnerLocation.getBlockX() >> 4, spawnerLocation.getBlockZ() >> 4)) {
            return;
        }

        long now = System.currentTimeMillis();

        // 1. 檢測並更新附近玩家
        double maxDistanceSq = Math.pow(template.getPlayerRange() * 1.5, 2);
        boolean hasNearbyPlayer = false;

        for (Player p : world.getPlayers()) {
            if (p.isDead() || !p.isValid()) continue;
            if (p.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;

            if (p.getLocation().distanceSquared(spawnerLocation) <= maxDistanceSq) {
                hasNearbyPlayer = true;
                participatingPlayers.add(p.getUniqueId());
            }
        }

        // 同步追蹤玩家至 TileState，防止原版 TrialSpawner 內部 detectedPlayers 為空而報錯強制關閉百葉窗
        Block block = spawnerLocation.getBlock();
        if (block.getState() instanceof org.bukkit.block.TrialSpawner tsState) {
            boolean trackingChanged = false;
            for (UUID uuid : participatingPlayers) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline() && p.getWorld().equals(world)) {
                    if (!tsState.isTrackingPlayer(p)) {
                        tsState.startTrackingPlayer(p);
                        trackingChanged = true;
                    }
                }
            }
            if (trackingChanged) {
                tsState.update(true, false);
            }
        }

        // 確保百葉窗維持 ACTIVE 戰鬥燃燒樣式與不祥模式
        if (block.getBlockData() instanceof TrialSpawner trialSpawnerData) {
            boolean dataChanged = false;
            if (trialSpawnerData.isOminous() != template.isOminous()) {
                trialSpawnerData.setOminous(template.isOminous());
                dataChanged = true;
            }
            if (trialSpawnerData.getTrialSpawnerState() != TrialSpawner.State.ACTIVE) {
                trialSpawnerData.setTrialSpawnerState(TrialSpawner.State.ACTIVE);
                dataChanged = true;
            }
            if (dataChanged) {
                block.setBlockData(trialSpawnerData, true);
            }
        }

        if (hasNearbyPlayer) {
            lastPlayerSeenTimeMs = now;
        } else {
            // 超過 45 秒沒有任何玩家在場，判定放棄並重置戰鬥
            if (now - lastPlayerSeenTimeMs > 45000L) {
                cancel("玩家離開戰鬥區域，試煉戰鬥重置。");
                return;
            }
        }

        // 2. 清理無效或已死亡但未觸發事件的實體 UUID
        Iterator<UUID> it = activeMobUuids.iterator();
        while (it.hasNext()) {
            UUID id = it.next();
            Entity e = Bukkit.getEntity(id);
            if (e == null || !e.isValid() || e.isDead()) {
                it.remove();
            }
        }

        List<List<String>> waves = template.getWaves();
        long delayMs = template.getSpawnDelaySeconds() * 1000L;

        if (template.isWaitWaveCleared()) {
            // 模式 A (開啟): 必須等待場上怪物全數肅清，才倒數間隔進入下一波
            if (currentWaveSpawned && activeMobUuids.isEmpty()) {
                if (currentWaveIndex >= waves.size() - 1) {
                    finishVictory();
                    return;
                }
                currentWaveIndex++;
                currentWaveSpawned = false;
                lastWaveClearedTimeMs = now;
            }

            if (!currentWaveSpawned && currentWaveIndex < waves.size()) {
                if (now - lastWaveClearedTimeMs >= delayMs) {
                    spawnCurrentWave();
                }
            }
        } else {
            // 模式 B (關閉): 不等場上全滅，生成間隔秒數一到立即出下一輪
            if (currentWaveIndex >= waves.size() - 1 && currentWaveSpawned && activeMobUuids.isEmpty()) {
                finishVictory();
                return;
            }

            if (currentWaveIndex < waves.size() - 1) {
                if (now - lastSpawnTimeMs >= delayMs) {
                    currentWaveIndex++;
                    spawnCurrentWave();
                }
            }
        }

        // 6. 每秒向參戰玩家推播進度 Action Bar (依各生怪磚獨立設定)
        if (template.isShowActionBar()) {
            String format = plugin.getConfig().getString("settings.spawner.actionbar-format", "&e試煉戰鬥中: &a%killed%/%total% &7(場上: &f%active% &7隻)");
            int displayWave = Math.min(waves.size(), currentWaveIndex + 1);
            String progressMsg = format
                    .replace("%killed%", String.valueOf(mobsKilledCount))
                    .replace("%total%", String.valueOf(template.getTotalMobs()))
                    .replace("%active%", String.valueOf(activeMobUuids.size()))
                    .replace("%wave%", String.valueOf(displayWave))
                    .replace("%maxwave%", String.valueOf(waves.size()));
            for (UUID uuid : participatingPlayers) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline() && p.getWorld().equals(spawnerLocation.getWorld()) && p.getLocation().distanceSquared(spawnerLocation) <= maxDistanceSq) {
                    p.sendActionBar(TextUtil.parse(progressMsg));
                }
            }
        }
    }

    /**
     * 生成當前波次的所有怪物
     */
    private void spawnCurrentWave() {
        World world = spawnerLocation.getWorld();
        if (world == null) return;

        List<List<String>> waves = template.getWaves();
        if (waves.isEmpty() || currentWaveIndex >= waves.size()) {
            return;
        }

        List<String> waveMobs = waves.get(currentWaveIndex);
        if (waveMobs == null || waveMobs.isEmpty()) {
            currentWaveIndex++;
            currentWaveSpawned = false;
            return;
        }

        currentWaveSpawned = true;
        lastSpawnTimeMs = System.currentTimeMillis();

        for (int i = 0; i < waveMobs.size(); i++) {
            clre20.customLootX.model.SpawnerMobEntry chosenEntry = template.getMobEntryForWave(currentWaveIndex, i);
            Location spawnLoc = findSafeSpawnLocation(world, chosenEntry.getPreviewEntityType());
            if (spawnLoc == null) {
                spawnLoc = spawnerLocation.clone().add(0.5, 1.0, 0.5);
            }

            try {
                Entity entity = null;
                if (chosenEntry.isMythic()) {
                    entity = clre20.customLootX.hook.MythicMobHook.spawnMob(chosenEntry.getMobId(), spawnLoc, 1);
                }
                if (entity == null) {
                    entity = world.spawnEntity(spawnLoc, chosenEntry.getVanillaType());
                }

                if (entity instanceof Mob mob) {
                    // 目標導向最近的玩家
                    Player nearest = getNearestParticipatingPlayer(mob.getLocation());
                    if (nearest != null) {
                        mob.setTarget(nearest);
                    }
                }

                // 標記自訂生怪磚 PDC
                entity.getPersistentDataContainer().set(plugin.getItemManager().KEY_SPAWNER_MOB, PersistentDataType.BYTE, (byte) 1);
                entity.getPersistentDataContainer().set(plugin.getItemManager().KEY_SPAWNER_LOC, PersistentDataType.STRING, locKey);

                // 播放生成特效與音效
                world.playSound(spawnLoc, Sound.BLOCK_TRIAL_SPAWNER_SPAWN_MOB, 1.0f, 1.0f);
                world.spawnParticle(
                        template.isOminous() ? Particle.SOUL_FIRE_FLAME : Particle.FLAME,
                        spawnLoc.clone().add(0, 0.8, 0),
                        15, 0.3, 0.4, 0.3, 0.05
                );

                activeMobUuids.add(entity.getUniqueId());
                mobsSpawnedCount++;
            } catch (Exception e) {
                plugin.logWarn("&5[試煉生怪磚·戰鬥]&c 生成試煉怪物失敗: &e" + chosenEntry.getDisplayName() + " &c- " + e.getMessage());
            }
        }
    }

    /**
     * 尋找生怪磚周圍 1~4 格內安全的著地或浮空位置
     */
    private Location findSafeSpawnLocation(World world, EntityType type) {
        int baseBx = spawnerLocation.getBlockX();
        int baseBy = spawnerLocation.getBlockY();
        int baseBz = spawnerLocation.getBlockZ();

        for (int attempt = 0; attempt < 10; attempt++) {
            int ox = ThreadLocalRandom.current().nextInt(-3, 4);
            int oz = ThreadLocalRandom.current().nextInt(-3, 4);
            if (ox == 0 && oz == 0) continue;

            int targetX = baseBx + ox;
            int targetZ = baseBz + oz;

            // 尋找地面
            for (int dy = 2; dy >= -3; dy--) {
                int targetY = baseBy + dy;
                Block feet = world.getBlockAt(targetX, targetY, targetZ);
                Block head = world.getBlockAt(targetX, targetY + 1, targetZ);
                Block ground = world.getBlockAt(targetX, targetY - 1, targetZ);

                if (!feet.getType().isSolid() && !head.getType().isSolid() && (ground.getType().isSolid() || isFlyingMob(type) || feet.isLiquid())) {
                    return new Location(world, targetX + 0.5, targetY, targetZ + 0.5);
                }
            }
        }
        return null;
    }

    private boolean isFlyingMob(EntityType type) {
        if (type == null) return false;
        return switch (type) {
            case BREEZE, BLAZE, GHAST, PHANTOM, ALLAY, BAT, BEE, VEX, WITHER, ENDER_DRAGON -> true;
            default -> false;
        };
    }

    private Player getNearestParticipatingPlayer(Location loc) {
        Player nearest = null;
        double min = Double.MAX_VALUE;
        for (UUID uuid : participatingPlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline() && p.getWorld().equals(loc.getWorld())) {
                double d = p.getLocation().distanceSquared(loc);
                if (d < min) {
                    min = d;
                    nearest = p;
                }
            }
        }
        return nearest;
    }

    /**
     * 怪物死亡時呼叫
     */
    public void onMobKilled(UUID mobUuid) {
        if (finished || ejecting) return;

        if (activeMobUuids.remove(mobUuid)) {
            mobsKilledCount++;

            // 檢查是否完成所有怪物討伐
            if (mobsKilledCount >= template.getTotalMobs()) {
                finishVictory();
            }
        }
    }

    /**
     * 挑戰勝利：切換狀態、彈出戰利品、設定冷卻
     */
    public void finishVictory() {
        if (finished || ejecting) return;
        this.ejecting = true;

        Block block = spawnerLocation.getBlock();
        TrialSpawner ejectingVisualData = null;
        // 噴獎勵時開啟頂部百葉窗 (EJECTING_REWARD) 並維持正確不祥狀態
        if (block.getBlockData() instanceof TrialSpawner trialSpawnerData) {
            trialSpawnerData.setOminous(template.isOminous());
            trialSpawnerData.setTrialSpawnerState(TrialSpawner.State.EJECTING_REWARD);
            block.setBlockData(trialSpawnerData, true);
            ejectingVisualData = (TrialSpawner) trialSpawnerData.clone();
        }
        if (block.getState() instanceof org.bukkit.block.TrialSpawner tsState) {
            tsState.setOminous(template.isOminous());
            tsState.update(true, false);
        }

        World world = spawnerLocation.getWorld();
        if (world != null) {
            world.playSound(spawnerLocation, Sound.BLOCK_TRIAL_SPAWNER_OPEN_SHUTTER, 1.2f, 1.0f);

            // 試煉完成提示音效 (由各生怪磚獨立設定開啟/關閉與音效種類，預設關閉)
            if (template.isVictorySoundEnabled()) {
                String soundName = template.getVictorySound();
                float volume = template.getVictorySoundVolume();
                float pitch = template.getVictorySoundPitch();
                try {
                    Sound sound = Sound.valueOf(soundName.toUpperCase());
                    world.playSound(spawnerLocation, sound, volume, pitch);
                } catch (IllegalArgumentException e) {
                    world.playSound(spawnerLocation, soundName.toLowerCase(), volume, pitch);
                }
            }

            world.spawnParticle(Particle.FIREWORK, spawnerLocation.clone().add(0.5, 1.2, 0.5), 25, 0.3, 0.3, 0.3, 0.1);
            world.spawnParticle(Particle.HAPPY_VILLAGER, spawnerLocation.clone().add(0.5, 1.0, 0.5), 20, 0.4, 0.4, 0.4, 0.05);
        }

        final TrialSpawner visualData = ejectingVisualData;
        if (visualData != null && world != null) {
            for (Player p : world.getNearbyPlayers(spawnerLocation, 64)) {
                p.sendBlockChange(spawnerLocation, visualData);
            }
        }

        // 開口視覺守衛：在吐獎勵期間每 2 ticks 維持百葉窗開口狀態 (EJECTING_REWARD)，直到最後一個獎勵落定
        this.visualKeeper = new BukkitRunnable() {
            @Override
            public void run() {
                if (finished || block.getType() != Material.TRIAL_SPAWNER) {
                    cancel();
                    return;
                }
                if (block.getBlockData() instanceof TrialSpawner data) {
                    boolean fix = false;
                    if (data.isOminous() != template.isOminous()) {
                        data.setOminous(template.isOminous());
                        fix = true;
                    }
                    if (data.getTrialSpawnerState() != TrialSpawner.State.EJECTING_REWARD) {
                        data.setTrialSpawnerState(TrialSpawner.State.EJECTING_REWARD);
                        fix = true;
                    }
                    if (fix) {
                        block.setBlockData(data, false);
                    }
                }
                if (visualData != null && world != null) {
                    java.util.Collection<Player> nearby = world.getNearbyPlayers(spawnerLocation, 48);
                    if (!nearby.isEmpty()) {
                        for (Player p : nearby) {
                            p.sendBlockChange(spawnerLocation, visualData);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 6L);

        // 抽取獎勵物品
        List<ItemStack> rewards = template.rollAllItems();
        if (rewards.isEmpty()) {
            Bukkit.getScheduler().runTaskLater(plugin, this::completeAndCooldown, 20L);
            return;
        }

        // 依序連續彈射獎勵物品
        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                if (index < rewards.size()) {
                    ItemStack drop = rewards.get(index);
                    if (drop != null && drop.getType() != Material.AIR) {
                        ejectSingleItem(drop);
                    }
                    index++;
                } else {
                    cancel();
                    // 等候最後一個物品向上躍起並落定後閉口進入冷卻
                    Bukkit.getScheduler().runTaskLater(plugin, SpawnerBattleSession.this::completeAndCooldown, 10L);
                }
            }
        }.runTaskTimer(plugin, 10L, 10L);
    }

    private void ejectSingleItem(ItemStack item) {
        World world = spawnerLocation.getWorld();
        if (world == null) return;

        Location spawnLoc = spawnerLocation.clone().add(0.5, 1.2, 0.5);
        Item drop = world.dropItem(spawnLoc, item);

        // 賦予擬真向上噴發向量
        double vx = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.15;
        double vz = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.15;
        drop.setVelocity(new Vector(vx, 0.25, vz));

        world.playSound(spawnLoc, Sound.BLOCK_TRIAL_SPAWNER_EJECT_ITEM, 1.0f, 1.2f);
        world.spawnParticle(template.isOminous() ? Particle.SOUL_FIRE_FLAME : Particle.SMALL_FLAME, spawnLoc, 10, 0.1, 0.1, 0.1, 0.05);
    }

    private void completeAndCooldown() {
        this.finished = true;
        this.ejecting = false;
        if (visualKeeper != null) {
            visualKeeper.cancel();
            visualKeeper = null;
        }

        World world = spawnerLocation.getWorld();
        long cooldownTicks = (long) template.getCooldownMinutes() * 60L * 20L;
        long cooldownEnd = (world != null ? world.getGameTime() : 0L) + cooldownTicks;

        Block block = spawnerLocation.getBlock();
        if (block.getState() instanceof org.bukkit.block.TrialSpawner tsState) {
            tsState.setOminous(template.isOminous());
            for (Player p : new ArrayList<>(tsState.getTrackedPlayers())) {
                tsState.stopTrackingPlayer(p);
            }
            tsState.setCooldownLength((int) Math.min(Integer.MAX_VALUE, cooldownTicks));
            tsState.setCooldownEnd(cooldownEnd);
            tsState.update(true, false);
        }

        if (block.getBlockData() instanceof TrialSpawner trialSpawnerData) {
            trialSpawnerData.setOminous(template.isOminous());
            trialSpawnerData.setTrialSpawnerState(TrialSpawner.State.COOLDOWN);
            block.setBlockData(trialSpawnerData, true);
            if (world != null) {
                for (Player p : world.getNearbyPlayers(spawnerLocation, 64)) {
                    p.sendBlockChange(spawnerLocation, trialSpawnerData);
                }
            }
        }

        if (world != null) {
            world.playSound(spawnerLocation, Sound.BLOCK_TRIAL_SPAWNER_CLOSE_SHUTTER, 1.0f, 1.0f);
        }

        // 記錄冷卻時間
        manager.recordCompletion(spawnerLocation, participatingPlayers, template);
        manager.endBattleSession(locKey);

        // 安全延遲防護：在 20 ticks 後再次確保方塊與百葉窗為不祥 COOLDOWN 狀態
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (block.getType() == Material.TRIAL_SPAWNER) {
                if (block.getBlockData() instanceof TrialSpawner data) {
                    if (data.isOminous() != template.isOminous() || data.getTrialSpawnerState() != TrialSpawner.State.COOLDOWN) {
                        data.setOminous(template.isOminous());
                        data.setTrialSpawnerState(TrialSpawner.State.COOLDOWN);
                        block.setBlockData(data, true);
                    }
                }
                if (block.getState() instanceof org.bukkit.block.TrialSpawner ts) {
                    if (ts.isOminous() != template.isOminous()) {
                        ts.setOminous(template.isOminous());
                        ts.update(true, false);
                    }
                }
            }
        }, 20L);
    }

    /**
     * 強制中斷戰鬥
     */
    public void cancel(String reason) {
        this.finished = true;
        this.ejecting = false;
        if (visualKeeper != null) {
            visualKeeper.cancel();
            visualKeeper = null;
        }

        // 清除場上存活的怪物
        for (UUID id : activeMobUuids) {
            Entity e = Bukkit.getEntity(id);
            if (e != null && e.isValid()) {
                e.remove();
            }
        }
        activeMobUuids.clear();

        // 停止追蹤玩家並恢復方塊狀態為等待玩家
        Block block = spawnerLocation.getBlock();
        if (block.getState() instanceof org.bukkit.block.TrialSpawner tsState) {
            tsState.setOminous(template.isOminous());
            for (Player p : new ArrayList<>(tsState.getTrackedPlayers())) {
                tsState.stopTrackingPlayer(p);
            }
            tsState.setCooldownEnd(0);
            tsState.update(true, false);
        }

        if (block.getBlockData() instanceof TrialSpawner trialSpawnerData) {
            trialSpawnerData.setOminous(template.isOminous());
            trialSpawnerData.setTrialSpawnerState(TrialSpawner.State.WAITING_FOR_PLAYERS);
            block.setBlockData(trialSpawnerData, true);
        }

        if (reason != null && !reason.isEmpty()) {
            for (UUID uuid : participatingPlayers) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    p.sendMessage(TextUtil.parse("&c[CustomLootX] " + reason));
                }
            }
        }

        manager.endBattleSession(locKey);
    }

    public boolean isFinished() {
        return finished;
    }

    public String getLocKey() {
        return locKey;
    }

    public Set<UUID> getActiveMobUuids() {
        return activeMobUuids;
    }

    public Set<UUID> getParticipatingPlayers() {
        return participatingPlayers;
    }
}
