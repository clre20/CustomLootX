package clre20.customLootX.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

public final class TextUtil {

    private static final LegacyComponentSerializer AMPERSAND_SERIALIZER = 
            LegacyComponentSerializer.builder().character('&').hexColors().useUnusualXRepeatedCharacterHexFormat().build();
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer PLAIN_SERIALIZER = 
            net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText();

    private TextUtil() {}

    public static String toPlainText(Component component) {
        if (component == null) return "";
        return PLAIN_SERIALIZER.serialize(component);
    }

    /**
     * Convert an Adventure Component back to legacy & color code format (e.g. &4紅色)
     */
    public static String toLegacyText(Component component) {
        if (component == null) return "";
        return AMPERSAND_SERIALIZER.serialize(component);
    }

    /**
     * Check if a string starts with a color or formatting code (&, §, or MiniMessage tag)
     */
    public static boolean hasColorPrefix(String text) {
        if (text == null || text.isEmpty()) return false;
        String trimmed = text.trim();
        if (trimmed.startsWith("&") || trimmed.startsWith("§")) {
            return trimmed.length() >= 2;
        }
        if (trimmed.startsWith("<") && trimmed.contains(">")) {
            return true;
        }
        return false;
    }

    /**
     * Ensure text has a color prefix, defaulting to white (&f) if none is provided
     */
    public static String ensureDefaultWhite(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        String trimmed = text.trim();
        if (!hasColorPrefix(trimmed)) {
            return "&f" + trimmed;
        }
        return trimmed;
    }

    /**
     * Parse lore line with default white color (&f) and no italic style
     */
    public static Component parseLore(String line) {
        if (line == null || line.isEmpty()) {
            return Component.empty();
        }
        String formatted = ensureDefaultWhite(line);
        return parse(formatted).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
    }

    /**
     * Convert legacy & codes or MiniMessage tags to Adventure Component
     */
    public static Component parse(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }
        if (message.contains("&")) {
            return AMPERSAND_SERIALIZER.deserialize(message);
        }
        if (message.contains("<") && message.contains(">")) {
            try {
                return MINI_MESSAGE.deserialize(message);
            } catch (Exception ignored) {
            }
        }
        return Component.text(message);
    }

    /**
     * Colorize legacy string using & codes
     */
    public static String color(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Format double percentage to exactly two decimal places (e.g. 15.50%)
     */
    public static String formatPercent(double percent) {
        return String.format(java.util.Locale.US, "%.2f%%", percent);
    }

    /**
     * Round double to 2 decimal places to prevent floating point precision issues
     */
    public static double roundChance(double val) {
        return Math.round(val * 100.0) / 100.0;
    }

    /**
     * Format an ItemStack into a clear human-readable string for console logs
     */
    public static String getItemDescription(org.bukkit.inventory.ItemStack item) {
        if (item == null || item.getType() == org.bukkit.Material.AIR) {
            return "[無掉落]";
        }
        StringBuilder sb = new StringBuilder();
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            sb.append(toPlainText(item.getItemMeta().displayName()));
            sb.append(" (").append(item.getType().name()).append(" x").append(item.getAmount()).append(")");
        } else {
            sb.append(item.getType().name()).append(" x").append(item.getAmount());
        }
        return sb.toString();
    }

    /**
     * Get human-readable item name (custom display name or material name)
     */
    public static String getItemName(org.bukkit.inventory.ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return "[落空]";
        }
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return toPlainText(item.getItemMeta().displayName());
        }
        return item.getType().name();
    }

    /**
     * 取得常見怪物的中文友善顯示名稱
     */
    public static String getMobDisplayName(org.bukkit.entity.EntityType type) {
        if (type == null) return "未知生物";
        return switch (type) {
            case BREEZE -> "旋風人";
            case BOGGED -> "沼澤骷髏";
            case ZOMBIE -> "殭屍";
            case SKELETON -> "骷髏";
            case STRAY -> "流浪者";
            case WITHER_SKELETON -> "凋零骷髏";
            case SPIDER -> "蜘蛛";
            case CAVE_SPIDER -> "洞穴蜘蛛";
            case CREEPER -> "苦力怕";
            case SLIME -> "史萊姆";
            case MAGMA_CUBE -> "岩漿史萊姆";
            case BLAZE -> "烈焰使者";
            case PIGLIN -> "豬靈";
            case PIGLIN_BRUTE -> "豬靈蠻兵";
            case ZOMBIFIED_PIGLIN -> "殭屍豬靈";
            case ENDERMAN -> "終界使者";
            case WITCH -> "女巫";
            case DROWNED -> "沉屍";
            case HUSK -> "屍殼";
            case SILVERFISH -> "蠹蟲";
            case ENDERMITE -> "終界蟎";
            case PILLAGER -> "掠奪者";
            case VINDICATOR -> "衛道士";
            case RAVAGER -> "劫毀獸";
            case EVOKER -> "喚魔者";
            case VEX -> "惱鬼";
            case PHANTOM -> "幻翼";
            case WARDEN -> "伏守者";
            case GUARDIAN -> "守衛者";
            case ELDER_GUARDIAN -> "遠古守衛者";
            case SHULKER -> "界伏鬼";
            case GHAST -> "地獄幽靈";
            case HOGLIN -> "伏獰";
            case ZOGLIN -> "殭屍伏獰";
            default -> type.name();
        };
    }
}
