package clre20.customLootX.hook;

import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * MythicMobs 插件整合掛鉤
 */
public class MythicMobHook {

    private static Boolean mythicMobsAvailable = null;

    /**
     * 檢查伺服器是否已安裝並啟用 MythicMobs
     */
    public static boolean isEnabled() {
        if (mythicMobsAvailable == null) {
            mythicMobsAvailable = Bukkit.getPluginManager().isPluginEnabled("MythicMobs");
        }
        return mythicMobsAvailable;
    }

    /**
     * 重新檢測 MythicMobs 狀態 (重載時呼叫)
     */
    public static void resetStatus() {
        mythicMobsAvailable = null;
    }

    /**
     * 檢查指定的 MythicMob ID 是否有效存在
     */
    public static boolean isValidMob(String mobId) {
        if (!isEnabled() || mobId == null || mobId.trim().isEmpty()) {
            return false;
        }
        try {
            return MythicBukkit.inst().getMobManager().getMythicMob(mobId.trim()).isPresent();
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * 取得 MythicMob 的顯示名稱 (包含彩色或其配置名稱)
     */
    public static String getDisplayName(String mobId) {
        if (!isEnabled() || mobId == null || mobId.trim().isEmpty()) {
            return mobId;
        }
        try {
            Optional<MythicMob> opt = MythicBukkit.inst().getMobManager().getMythicMob(mobId.trim());
            if (opt.isPresent()) {
                MythicMob mob = opt.get();
                if (mob.getDisplayName() != null) {
                    String name = mob.getDisplayName().get();
                    if (name != null && !name.isEmpty()) {
                        return name;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return mobId;
    }

    /**
     * 取得 MythicMob 所基於的原版 EntityType (用於生怪磚旋轉 3D 模型預覽)
     */
    public static EntityType getBaseEntityType(String mobId) {
        if (!isEnabled() || mobId == null || mobId.trim().isEmpty()) {
            return EntityType.ZOMBIE;
        }
        try {
            Optional<MythicMob> opt = MythicBukkit.inst().getMobManager().getMythicMob(mobId.trim());
            if (opt.isPresent()) {
                MythicMob mob = opt.get();
                String typeStr = mob.getEntityTypeString();
                if (typeStr != null && !typeStr.isEmpty()) {
                    if (typeStr.contains(":")) {
                        typeStr = typeStr.substring(typeStr.indexOf(':') + 1);
                    }
                    try {
                        return EntityType.valueOf(typeStr.trim().toUpperCase());
                    } catch (IllegalArgumentException ignored) {}
                }
                if (mob.getEntityType() != null) {
                    try {
                        String entityName = mob.getEntityType().name();
                        if (entityName.contains(":")) {
                            entityName = entityName.substring(entityName.indexOf(':') + 1);
                        }
                        return EntityType.valueOf(entityName.trim().toUpperCase());
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (Throwable ignored) {}
        return EntityType.ZOMBIE;
    }

    /**
     * 取得目前所有已註冊的 MythicMob ID 清單
     */
    public static List<String> getAllMobIds() {
        List<String> list = new ArrayList<>();
        if (!isEnabled()) {
            return list;
        }
        try {
            Collection<MythicMob> mobs = MythicBukkit.inst().getMobManager().getMobTypes();
            for (MythicMob mob : mobs) {
                list.add(mob.getInternalName());
            }
        } catch (Throwable ignored) {}
        return list;
    }

    /**
     * 於指定位置生成一隻 MythicMob
     * @return Bukkit 原生 Entity 實體 (若生成失敗則為 null)
     */
    public static Entity spawnMob(String mobId, Location location, int level) {
        if (!isEnabled() || mobId == null || mobId.trim().isEmpty() || location == null) {
            return null;
        }
        try {
            return MythicBukkit.inst().getAPIHelper().spawnMythicMob(mobId.trim(), location, Math.max(1, level));
        } catch (Throwable t) {
            org.bukkit.Bukkit.getConsoleSender().sendMessage(clre20.customLootX.util.TextUtil.parse("&8[&6CustomLootX&8] &5[MythicMobs·生怪]&c 生成 MythicMob 失敗 (&e" + mobId + "&c): " + t.getMessage()));
        }
        return null;
    }
}
