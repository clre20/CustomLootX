package clre20.customLootX.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TrialSpawner;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * 試煉生怪磚 NMS 實體模型切換與封包同步工具類
 */
public class TrialSpawnerNmsUtil {

    private static boolean initialized = false;

    // CraftWorld / Level
    private static Method craftWorldGetHandleMethod;
    private static Constructor<?> blockPosConstructor;
    private static Method levelGetBlockEntityMethod;

    // CraftBlockEntityState fallback
    private static Method getBlockEntityMethod;

    // TrialSpawnerBlockEntity
    private static Method getTrialSpawnerMethod;
    private static Method markUpdatedMethod;
    private static Method getUpdatePacketMethod;

    // TrialSpawner
    private static Method getStateDataMethod;

    // TrialSpawnerStateData
    private static Field nextSpawnDataField;
    private static Field displayEntityField;

    // NBT & SpawnData
    private static Class<?> compoundTagClass;
    private static Method putStringMethod;
    private static Class<?> spawnDataClass;
    private static Constructor<?> spawnDataConstructor;
    private static Method getEntityToSpawnMethod;

    // Network / Player Packet sending
    private static Method craftPlayerGetHandleMethod;
    private static Field connectionField;
    private static Method sendPacketMethod;
    private static BlockData barrierData;

    public static synchronized void init(Logger logger) {
        if (initialized) return;
        try {
            // 1. CraftWorld & Level
            Class<?> craftWorldClass = Class.forName("org.bukkit.craftbukkit.CraftWorld");
            craftWorldGetHandleMethod = craftWorldClass.getMethod("getHandle");

            Class<?> blockPosClass = Class.forName("net.minecraft.core.BlockPos");
            blockPosConstructor = blockPosClass.getConstructor(int.class, int.class, int.class);

            Class<?> levelClass = Class.forName("net.minecraft.world.level.Level");
            levelGetBlockEntityMethod = levelClass.getMethod("getBlockEntity", blockPosClass);

            // 2. CraftBlockEntityState fallback
            Class<?> craftBlockEntityStateClass = Class.forName("org.bukkit.craftbukkit.block.CraftBlockEntityState");
            getBlockEntityMethod = craftBlockEntityStateClass.getMethod("getBlockEntity");

            // 3. TrialSpawnerBlockEntity
            Class<?> tsBlockEntityClass = Class.forName("net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity");
            getTrialSpawnerMethod = tsBlockEntityClass.getMethod("getTrialSpawner");
            markUpdatedMethod = tsBlockEntityClass.getMethod("markUpdated");
            try {
                getUpdatePacketMethod = tsBlockEntityClass.getMethod("getUpdatePacket");
            } catch (Throwable ignored) {}

            // 4. TrialSpawner
            Class<?> trialSpawnerClass = Class.forName("net.minecraft.world.level.block.entity.trialspawner.TrialSpawner");
            getStateDataMethod = trialSpawnerClass.getMethod("getStateData");

            // 5. TrialSpawnerStateData
            Class<?> stateDataClass = Class.forName("net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerStateData");
            nextSpawnDataField = stateDataClass.getField("nextSpawnData");

            try {
                displayEntityField = stateDataClass.getDeclaredField("displayEntity");
                displayEntityField.setAccessible(true);
            } catch (Throwable ignored) {}

            // 6. CompoundTag
            compoundTagClass = Class.forName("net.minecraft.nbt.CompoundTag");
            putStringMethod = compoundTagClass.getMethod("putString", String.class, String.class);

            // 7. SpawnData
            spawnDataClass = Class.forName("net.minecraft.world.level.SpawnData");
            try {
                spawnDataConstructor = spawnDataClass.getConstructor(compoundTagClass, Optional.class, Optional.class);
            } catch (Throwable t) {
                try {
                    getEntityToSpawnMethod = spawnDataClass.getMethod("getEntityToSpawn");
                } catch (Throwable ignored) {}
            }

            // 8. Player Packet Sending
            Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer");
            craftPlayerGetHandleMethod = craftPlayerClass.getMethod("getHandle");

            Class<?> serverPlayerClass = Class.forName("net.minecraft.server.level.ServerPlayer");
            connectionField = serverPlayerClass.getField("connection");

            Class<?> packetClass = Class.forName("net.minecraft.network.protocol.Packet");
            Class<?> listenerClass = connectionField.getType();
            sendPacketMethod = listenerClass.getMethod("send", packetClass);

            initialized = true;
            org.bukkit.Bukkit.getConsoleSender().sendMessage(clre20.customLootX.util.TextUtil.parse("&8[&6CustomLootX&8] &5[試煉生怪磚·驅動]&a 試煉生怪磚 NMS 反射驅動器初始化成功！"));
        } catch (Throwable t) {
            org.bukkit.Bukkit.getConsoleSender().sendMessage(clre20.customLootX.util.TextUtil.parse("&8[&6CustomLootX&8] &5[試煉生怪磚·驅動]&c 試煉生怪磚 NMS 反射初始化失敗: " + t.getMessage()));
        }
    }

    /**
     * 向玩家發送底層 NMS 封包
     */
    public static void sendNmsPacket(Player player, Object packet) {
        if (player == null || packet == null) return;
        try {
            if (craftPlayerGetHandleMethod != null && connectionField != null && sendPacketMethod != null) {
                Object serverPlayer = craftPlayerGetHandleMethod.invoke(player);
                if (serverPlayer != null) {
                    Object connection = connectionField.get(serverPlayer);
                    if (connection != null) {
                        sendPacketMethod.invoke(connection, packet);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    /**
     * 同步更新試煉生怪磚在世界上的真實 BlockEntity 預覽實體，並向周圍玩家發送更新封包
     */
    public static boolean setSpawnerMob(Block block, EntityType targetType, boolean ominous, Logger logger) {
        if (block == null || block.getType() != Material.TRIAL_SPAWNER || targetType == null) {
            return false;
        }
        if (!initialized) {
            init(logger);
        }

        try {
            BlockState state = block.getState();
            TrialSpawner ts = null;
            if (state instanceof TrialSpawner) {
                ts = (TrialSpawner) state;
                ts.setOminous(ominous);
                ts.setRequiredPlayerRange(0);
                try {
                    ts.getNormalConfiguration().setSpawnedType(targetType);
                    ts.getOminousConfiguration().setSpawnedType(targetType);
                } catch (Throwable ignored) {}
                ts.update(true, false);
            }

            // 1. 獲取世界真實的 BlockEntity (而非 Snapshot)
            Object realBlockEntity = null;
            if (craftWorldGetHandleMethod != null && blockPosConstructor != null && levelGetBlockEntityMethod != null) {
                try {
                    Object level = craftWorldGetHandleMethod.invoke(block.getWorld());
                    Object blockPos = blockPosConstructor.newInstance(block.getX(), block.getY(), block.getZ());
                    realBlockEntity = levelGetBlockEntityMethod.invoke(level, blockPos);
                } catch (Throwable t) {
                    org.bukkit.Bukkit.getConsoleSender().sendMessage(clre20.customLootX.util.TextUtil.parse("&8[&6CustomLootX&8] &5[試煉生怪磚·NMS]&c 從世界獲取 BlockEntity 異常: " + t.getMessage()));
                }
            }
            if (realBlockEntity == null && state != null && getBlockEntityMethod != null) {
                try {
                    realBlockEntity = getBlockEntityMethod.invoke(state);
                } catch (Throwable ignored) {}
            }

            // 2. 透過 NMS 修改世界真實 BlockEntity 的 nextSpawnData (原版客戶端渲染籠內實體真正讀取的資料)
            Object updatePacket = null;
            if (realBlockEntity != null && getTrialSpawnerMethod != null && getStateDataMethod != null && nextSpawnDataField != null) {
                try {
                    Object trialSpawner = getTrialSpawnerMethod.invoke(realBlockEntity);
                    Object stateData = getStateDataMethod.invoke(trialSpawner);

                    // 建立 CompoundTag 並寫入 entity id: "minecraft:..."
                    Object compoundTag = compoundTagClass.getDeclaredConstructor().newInstance();
                    String mobKey = targetType.getKey().toString();
                    putStringMethod.invoke(compoundTag, "id", mobKey);

                    // 建立 SpawnData
                    Object spawnData;
                    if (spawnDataConstructor != null) {
                        spawnData = spawnDataConstructor.newInstance(compoundTag, Optional.empty(), Optional.empty());
                    } else {
                        spawnData = spawnDataClass.getDeclaredConstructor().newInstance();
                        if (getEntityToSpawnMethod != null) {
                            Object entityTag = getEntityToSpawnMethod.invoke(spawnData);
                            putStringMethod.invoke(entityTag, "id", mobKey);
                        }
                    }

                    // 寫入 stateData.nextSpawnData
                    nextSpawnDataField.set(stateData, Optional.of(spawnData));

                    // 重設 displayEntity 快取，確保伺服器端刷新展示模型
                    if (displayEntityField != null) {
                        displayEntityField.set(stateData, null);
                    }

                    // 呼叫 markUpdated() 廣播原版方塊實體更新
                    if (markUpdatedMethod != null) {
                        markUpdatedMethod.invoke(realBlockEntity);
                    }

                    // 獲取原版已注入最新 nextSpawnData 的更新封包
                    if (getUpdatePacketMethod != null) {
                        updatePacket = getUpdatePacketMethod.invoke(realBlockEntity);
                    }
                } catch (Throwable t) {
                    org.bukkit.Bukkit.getConsoleSender().sendMessage(clre20.customLootX.util.TextUtil.parse("&8[&6CustomLootX&8] &5[試煉生怪磚·NMS]&c NMS 設定 nextSpawnData 異常: " + t.getMessage()));
                }
            }

            // 3. 客戶端瞬時重置機制 (Client-side Resync)：
            // 原版 Minecraft 客戶端在接收 TrialSpawnerBlockEntity 更新封包時，因 Mojang 未將 displayEntity 快取重設為 null，
            // 導致客戶端畫面永遠顯示最初快取的生物實體。
            // 解決方案：向客戶端發送屏障方塊 (觸發 removeBlockEntity 徹底清除舊模型)，
            // 緊接著發送回試煉生怪磚方塊 (觸發 addBlockEntity 建立乾淨物件)，再發送最新更新封包注入目標生物！
            // 由於在同一個 tick 發送，客戶端在下一次繪製畫面時已是新生物，完全不會有屏障閃爍。
            Location loc = block.getLocation();
            if (loc.getWorld() != null) {
                java.util.Collection<Player> nearby = loc.getWorld().getNearbyPlayers(loc, 64);
                if (!nearby.isEmpty()) {
                    if (updatePacket != null) {
                        if (barrierData == null) {
                            barrierData = Material.BARRIER.createBlockData();
                        }
                        BlockData realBlockData = block.getBlockData();

                        for (Player p : nearby) {
                            try {
                                p.sendBlockChange(loc, barrierData);
                                p.sendBlockChange(loc, realBlockData);
                                sendNmsPacket(p, updatePacket);
                            } catch (Throwable ignored) {}
                        }
                    } else if (ts != null) {
                        // Fallback
                        for (Player p : nearby) {
                            try {
                                p.sendBlockUpdate(loc, ts);
                            } catch (Throwable ignored) {}
                        }
                    }
                }
            }

            return true;
        } catch (Throwable t) {
            org.bukkit.Bukkit.getConsoleSender().sendMessage(clre20.customLootX.util.TextUtil.parse("&8[&6CustomLootX&8] &5[試煉生怪磚·預覽]&c 更新試煉生怪磚預覽實體失敗: " + t.getMessage()));
            return false;
        }
    }
}
