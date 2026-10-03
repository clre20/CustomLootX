package clre20.customLootX.model;

import clre20.customLootX.hook.MythicMobHook;
import clre20.customLootX.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

/**
 * 試煉生怪磚生成怪物池項目 (支援原版生物與 MythicMobs 自訂怪物)
 */
public class SpawnerMobEntry {

    private boolean mythic;
    private String mobId;
    private String customDisplayName;
    private double chance;

    public SpawnerMobEntry(boolean mythic, String mobId, String customDisplayName, double chance) {
        this.mythic = mythic;
        this.mobId = (mobId == null || mobId.trim().isEmpty()) ? "ZOMBIE" : mobId.trim();
        this.customDisplayName = (customDisplayName == null || customDisplayName.trim().isEmpty()) ? null : customDisplayName.trim();
        this.chance = TextUtil.roundChance(chance);
    }

    public SpawnerMobEntry(EntityType vanillaType, double chance) {
        this(false, vanillaType != null ? vanillaType.name() : "ZOMBIE", null, chance);
    }

    public SpawnerMobEntry(String mythicMobId, double chance) {
        this(true, mythicMobId, null, chance);
    }

    public boolean isMythic() {
        return mythic;
    }

    public void setMythic(boolean mythic) {
        this.mythic = mythic;
    }

    public String getMobId() {
        return mobId;
    }

    public void setMobId(String mobId) {
        this.mobId = (mobId == null || mobId.trim().isEmpty()) ? "ZOMBIE" : mobId.trim();
    }

    public String getCustomDisplayName() {
        return customDisplayName;
    }

    public void setCustomDisplayName(String customDisplayName) {
        this.customDisplayName = (customDisplayName == null || customDisplayName.trim().isEmpty()) ? null : customDisplayName.trim();
    }

    public double getChance() {
        return chance;
    }

    public void setChance(double chance) {
        this.chance = TextUtil.roundChance(chance);
    }

    public EntityType getVanillaType() {
        if (mythic) {
            return MythicMobHook.getBaseEntityType(mobId);
        }
        try {
            return EntityType.valueOf(mobId.toUpperCase());
        } catch (IllegalArgumentException e) {
            return EntityType.ZOMBIE;
        }
    }

    public String getDisplayName() {
        if (customDisplayName != null && !customDisplayName.isEmpty()) {
            return customDisplayName;
        }
        if (mythic) {
            return MythicMobHook.getDisplayName(mobId);
        }
        return TextUtil.getMobDisplayName(getVanillaType());
    }

    public EntityType getPreviewEntityType() {
        if (mythic) {
            return MythicMobHook.getBaseEntityType(mobId);
        }
        return getVanillaType();
    }

    public Material getIconMaterial() {
        if (mythic) {
            return Material.NETHER_STAR;
        }
        EntityType vt = getVanillaType();
        if (vt == null) return Material.SPAWNER;
        try {
            return Material.valueOf(vt.name() + "_SPAWN_EGG");
        } catch (IllegalArgumentException e) {
            return switch (vt) {
                case ILLUSIONER -> Material.BOW;
                case GIANT -> Material.ZOMBIE_SPAWN_EGG;
                default -> Material.SPAWNER;
            };
        }
    }

    public SpawnerMobEntry cloneEntry() {
        return new SpawnerMobEntry(this.mythic, this.mobId, this.customDisplayName, this.chance);
    }
}
