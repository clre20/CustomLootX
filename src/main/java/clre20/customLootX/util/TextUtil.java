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
     * Get human-readable item name (custom display name or material name)
     */
    public static String getItemName(org.bukkit.inventory.ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return "落空";
        }
        if (item.hasItemMeta()) {
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                try {
                    Component comp = meta.displayName();
                    if (comp != null) {
                        String plain = toPlainText(comp).trim();
                        if (!plain.isEmpty()) {
                            return plain;
                        }
                    }
                } catch (Throwable ignored) {
                }
                try {
                    String legacy = meta.getDisplayName();
                    if (legacy != null && !legacy.trim().isEmpty()) {
                        String stripped = ChatColor.stripColor(legacy).trim();
                        if (!stripped.isEmpty()) {
                            return stripped;
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
            if (meta != null && meta.hasItemName()) {
                try {
                    Component comp = meta.itemName();
                    if (comp != null) {
                        String plain = toPlainText(comp).trim();
                        if (!plain.isEmpty()) {
                            return plain;
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        }

        String zhName = getMaterialFriendlyName(item.getType());
        if (zhName != null && !zhName.isEmpty()) {
            return zhName;
        }

        try {
            String i18n = item.getI18NDisplayName();
            if (i18n != null && !i18n.trim().isEmpty()) {
                return i18n.trim();
            }
        } catch (Throwable ignored) {
        }

        String raw = item.getType().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        for (String word : raw.split(" ")) {
            if (!word.isEmpty()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
        }
        return sb.toString();
    }

    /**
     * Format an ItemStack into a clear human-readable string for console logs
     */
    public static String getItemDescription(org.bukkit.inventory.ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return "落空";
        }
        String name = getItemName(item);
        if (item.getAmount() > 1) {
            return name + " x" + item.getAmount();
        }
        return name;
    }

    /**
     * Format a list of ItemStacks into JSON array format: ["草地", "葉子", "葉子", "草地", "石頭", "石頭"]
     */
    public static String formatItemNamesJson(java.util.List<org.bukkit.inventory.ItemStack> items) {
        if (items == null || items.isEmpty()) {
            return "[\"無掉落\"]";
        }
        java.util.List<String> list = new java.util.ArrayList<>();
        for (org.bukkit.inventory.ItemStack it : items) {
            if (it == null || it.getType().isAir()) continue;
            String name = getItemName(it);
            if (it.getAmount() > 1) {
                list.add("\"" + name + " x" + it.getAmount() + "\"");
            } else {
                list.add("\"" + name + "\"");
            }
        }
        if (list.isEmpty()) {
            return "[\"無掉落\"]";
        }
        return "[" + String.join(", ", list) + "]";
    }

    /**
     * Format a list of ItemStacks into JSON array format (alias)
     */
    public static String formatItemList(java.util.List<org.bukkit.inventory.ItemStack> items) {
        return formatItemNamesJson(items);
    }

    /**
     * Format a list of ItemStacks into pure names in JSON array format (alias)
     */
    public static String formatItemNames(java.util.List<org.bukkit.inventory.ItemStack> items) {
        return formatItemNamesJson(items);
    }

    /**
     * 取得原版材質的中文繁體友善顯示名稱
     */
    public static String getMaterialFriendlyName(org.bukkit.Material mat) {
        if (mat == null) return "未知物品";
        return switch (mat) {
            case GRASS_BLOCK -> "草地";
            case DIRT -> "泥土";
            case COARSE_DIRT -> "粗泥";
            case ROOTED_DIRT -> "纏根泥土";
            case MUD -> "泥巴";
            case PODZOL -> "灰化土";
            case CLAY -> "黏土";
            case STONE -> "石頭";
            case COBBLESTONE -> "鵝卵石";
            case MOSSY_COBBLESTONE -> "青苔鵝卵石";
            case SMOOTH_STONE -> "平滑石頭";
            case GRANITE -> "花崗岩";
            case POLISHED_GRANITE -> "平滑花崗岩";
            case DIORITE -> "閃長岩";
            case POLISHED_DIORITE -> "平滑閃長岩";
            case ANDESITE -> "安山岩";
            case POLISHED_ANDESITE -> "平滑安山岩";
            case DEEPSLATE -> "深層岩";
            case COBBLED_DEEPSLATE -> "深層碎石";
            case POLISHED_DEEPSLATE -> "平滑深層岩";
            case TUFF -> "凝灰岩";
            case CALCITE -> "方解石";
            case BASALT -> "玄武岩";
            case SMOOTH_BASALT -> "平滑玄武岩";
            case BLACKSTONE -> "黑石";
            case POLISHED_BLACKSTONE -> "平滑黑石";
            case OBSIDIAN -> "黑曜石";
            case CRYING_OBSIDIAN -> "哭泣黑曜石";
            case BEDROCK -> "基岩";
            case SAND -> "沙子";
            case RED_SAND -> "紅沙";
            case GRAVEL -> "礫石";
            case SUSPICIOUS_SAND -> "可疑沙";
            case SUSPICIOUS_GRAVEL -> "可疑礫石";
            case SANDSTONE -> "砂岩";
            case RED_SANDSTONE -> "紅砂岩";
            case SOUL_SAND -> "靈魂沙";
            case SOUL_SOIL -> "靈魂土";
            case NETHERRACK -> "地獄石";
            case END_STONE -> "終界石";
            case PURPUR_BLOCK -> "紫珀塊";

            // 樹葉
            case OAK_LEAVES -> "葉子";
            case SPRUCE_LEAVES -> "杉木葉";
            case BIRCH_LEAVES -> "樺木葉";
            case JUNGLE_LEAVES -> "叢林葉";
            case ACACIA_LEAVES -> "相思木葉";
            case DARK_OAK_LEAVES -> "黑橡木葉";
            case MANGROVE_LEAVES -> "紅樹林葉";
            case CHERRY_LEAVES -> "櫻花葉";
            case AZALEA_LEAVES -> "杜鵑葉";
            case FLOWERING_AZALEA_LEAVES -> "開花杜鵑葉";
            case SHORT_GRASS -> "草";
            case TALL_GRASS -> "高草";
            case FERN -> "蕨";
            case LARGE_FERN -> "大型蕨";

            // 原木與木板
            case OAK_LOG -> "橡木原木";
            case SPRUCE_LOG -> "杉木原木";
            case BIRCH_LOG -> "樺木原木";
            case JUNGLE_LOG -> "叢林原木";
            case ACACIA_LOG -> "相思木原木";
            case DARK_OAK_LOG -> "黑橡木原木";
            case MANGROVE_LOG -> "紅樹林原木";
            case CHERRY_LOG -> "櫻花原木";
            case BAMBOO_BLOCK -> "竹塊";
            case CRIMSON_STEM -> "緋紅蕈柄";
            case WARPED_STEM -> "扭曲蕈柄";
            case OAK_PLANKS -> "橡木木板";
            case SPRUCE_PLANKS -> "杉木木板";
            case BIRCH_PLANKS -> "樺木木板";
            case JUNGLE_PLANKS -> "叢林木板";
            case ACACIA_PLANKS -> "相思木木板";
            case DARK_OAK_PLANKS -> "黑橡木木板";
            case MANGROVE_PLANKS -> "紅樹林木板";
            case CHERRY_PLANKS -> "櫻花木板";
            case BAMBOO_PLANKS -> "竹木板";

            // 礦物
            case COAL -> "煤炭";
            case CHARCOAL -> "木炭";
            case RAW_IRON -> "鐵原礦";
            case RAW_COPPER -> "銅原礦";
            case RAW_GOLD -> "金原礦";
            case IRON_INGOT -> "鐵錠";
            case COPPER_INGOT -> "銅錠";
            case GOLD_INGOT -> "金錠";
            case NETHERITE_INGOT -> "獄髓錠";
            case NETHERITE_SCRAP -> "獄髓碎片";
            case DIAMOND -> "鑽石";
            case EMERALD -> "綠寶石";
            case LAPIS_LAZULI -> "青金石";
            case REDSTONE -> "紅石";
            case QUARTZ -> "石英";
            case AMETHYST_SHARD -> "紫水晶碎片";
            case IRON_BLOCK -> "鐵塊";
            case COPPER_BLOCK -> "銅塊";
            case GOLD_BLOCK -> "金塊";
            case DIAMOND_BLOCK -> "鑽石塊";
            case NETHERITE_BLOCK -> "獄髓塊";
            case EMERALD_BLOCK -> "綠寶石塊";
            case LAPIS_BLOCK -> "青金石塊";
            case REDSTONE_BLOCK -> "紅石塊";
            case COAL_BLOCK -> "煤炭塊";

            // 武器與工具
            case WOODEN_SWORD -> "木劍";
            case STONE_SWORD -> "石劍";
            case IRON_SWORD -> "鐵劍";
            case GOLDEN_SWORD -> "金劍";
            case DIAMOND_SWORD -> "鑽石劍";
            case NETHERITE_SWORD -> "獄髓劍";
            case WOODEN_PICKAXE -> "木鎬";
            case STONE_PICKAXE -> "石鎬";
            case IRON_PICKAXE -> "鐵鎬";
            case GOLDEN_PICKAXE -> "金鎬";
            case DIAMOND_PICKAXE -> "鑽石鎬";
            case NETHERITE_PICKAXE -> "獄髓鎬";
            case WOODEN_AXE -> "木斧";
            case STONE_AXE -> "石斧";
            case IRON_AXE -> "鐵斧";
            case GOLDEN_AXE -> "金斧";
            case DIAMOND_AXE -> "鑽石斧";
            case NETHERITE_AXE -> "獄髓斧";
            case WOODEN_SHOVEL -> "木鏟";
            case STONE_SHOVEL -> "石鏟";
            case IRON_SHOVEL -> "鐵鏟";
            case GOLDEN_SHOVEL -> "金鏟";
            case DIAMOND_SHOVEL -> "鑽石鏟";
            case NETHERITE_SHOVEL -> "獄髓鏟";
            case WOODEN_HOE -> "木鋤";
            case STONE_HOE -> "石鋤";
            case IRON_HOE -> "鐵鋤";
            case GOLDEN_HOE -> "金鋤";
            case DIAMOND_HOE -> "鑽石鋤";
            case NETHERITE_HOE -> "獄髓鋤";
            case BOW -> "弓";
            case CROSSBOW -> "弩";
            case TRIDENT -> "三叉戟";
            case MACE -> "重錘";
            case SHIELD -> "盾牌";
            case ARROW -> "箭矢";
            case SPECTRAL_ARROW -> "光靈箭";
            case ELYTRA -> "鞘翅";

            // 防具
            case LEATHER_HELMET -> "皮革帽子";
            case CHAINMAIL_HELMET -> "鎖鏈頭盔";
            case IRON_HELMET -> "鐵頭盔";
            case GOLDEN_HELMET -> "金頭盔";
            case DIAMOND_HELMET -> "鑽石頭盔";
            case NETHERITE_HELMET -> "獄髓頭盔";
            case TURTLE_HELMET -> "海龜殼";
            case LEATHER_CHESTPLATE -> "皮革外套";
            case CHAINMAIL_CHESTPLATE -> "鎖鏈胸甲";
            case IRON_CHESTPLATE -> "鐵胸甲";
            case GOLDEN_CHESTPLATE -> "金胸甲";
            case DIAMOND_CHESTPLATE -> "鑽石胸甲";
            case NETHERITE_CHESTPLATE -> "獄髓胸甲";
            case LEATHER_LEGGINGS -> "皮革褲子";
            case CHAINMAIL_LEGGINGS -> "鎖鏈護腿";
            case IRON_LEGGINGS -> "鐵護腿";
            case GOLDEN_LEGGINGS -> "金護腿";
            case DIAMOND_LEGGINGS -> "鑽石護腿";
            case NETHERITE_LEGGINGS -> "獄髓護腿";
            case LEATHER_BOOTS -> "皮革靴子";
            case CHAINMAIL_BOOTS -> "鎖鏈靴子";
            case IRON_BOOTS -> "鐵靴子";
            case GOLDEN_BOOTS -> "金靴子";
            case DIAMOND_BOOTS -> "鑽石靴子";
            case NETHERITE_BOOTS -> "獄髓靴子";

            // 試煉與特殊
            case TRIAL_KEY -> "試煉鑰匙";
            case OMINOUS_TRIAL_KEY -> "不祥試煉鑰匙";
            case VAULT -> "試煉寶庫";
            case TRIAL_SPAWNER -> "試煉生怪磚";
            case OMINOUS_BOTTLE -> "不祥之瓶";
            case BREEZE_ROD -> "旋風棒";
            case WIND_CHARGE -> "風彈";
            case HEAVY_CORE -> "重芯";
            case ENCHANTED_BOOK -> "附魔書";
            case BOOK -> "書";
            case TOTEM_OF_UNDYING -> "不死圖騰";
            case NETHER_STAR -> "地獄之星";
            case BEACON -> "烽火台";
            case EXPERIENCE_BOTTLE -> "附魔之瓶";
            case PRISMARINE_SHARD -> "海磷石碎片";
            case PRISMARINE_CRYSTALS -> "海磷石晶體";
            case HEART_OF_THE_SEA -> "海洋之心";
            case NAUTILUS_SHELL -> "鸚鵡螺殼";
            case SHULKER_SHELL -> "界伏殼";
            case SHULKER_BOX -> "界伏盒";

            // 食物
            case APPLE -> "蘋果";
            case GOLDEN_APPLE -> "金蘋果";
            case ENCHANTED_GOLDEN_APPLE -> "附魔金蘋果";
            case GOLDEN_CARROT -> "金胡蘿蔔";
            case CARROT -> "胡蘿蔔";
            case POTATO -> "馬鈴薯";
            case BAKED_POTATO -> "烤馬鈴薯";
            case BREAD -> "麵包";
            case BEEF -> "生牛肉";
            case COOKED_BEEF -> "牛排";
            case PORKCHOP -> "生豬排";
            case COOKED_PORKCHOP -> "烤豬排";
            case CHICKEN -> "生雞肉";
            case COOKED_CHICKEN -> "烤雞肉";
            case MUTTON -> "生羊肉";
            case COOKED_MUTTON -> "烤羊肉";
            case COD -> "生鱈魚";
            case COOKED_COD -> "烤鱈魚";
            case SALMON -> "生鮭魚";
            case COOKED_SALMON -> "烤鮭魚";
            case SWEET_BERRIES -> "甜漿果";
            case GLOW_BERRIES -> "發光漿果";
            case MELON_SLICE -> "西瓜片";

            // 怪物掉落物與雜項
            case ROTTEN_FLESH -> "腐肉";
            case BONE -> "骨頭";
            case BONE_MEAL -> "骨粉";
            case STRING -> "線";
            case FEATHER -> "羽毛";
            case GUNPOWDER -> "火藥";
            case SPIDER_EYE -> "蜘蛛眼";
            case ENDER_PEARL -> "終界珍珠";
            case ENDER_EYE -> "終界之眼";
            case BLAZE_ROD -> "烈焰棒";
            case BLAZE_POWDER -> "烈焰粉";
            case MAGMA_CREAM -> "岩漿球";
            case SLIME_BALL -> "史萊姆球";
            case GHAST_TEAR -> "幽靈之淚";
            case LEATHER -> "皮革";
            case SADDLE -> "鞍";
            case NAME_TAG -> "命名牌";
            case LEAD -> "韁繩";
            case BUNDLE -> "收納袋";
            case COMPASS -> "指南針";
            case CLOCK -> "時鐘";
            case SPYGLASS -> "望遠鏡";
            case RECOVERY_COMPASS -> "回溯指南針";
            case BUCKET -> "鐵桶";
            case WATER_BUCKET -> "水桶";
            case LAVA_BUCKET -> "岩漿桶";
            case MILK_BUCKET -> "牛奶桶";
            case CHEST -> "箱子";
            case TRAPPED_CHEST -> "陷阱箱";
            case ENDER_CHEST -> "終界箱";
            case BARREL -> "木桶";
            case HOPPER -> "漏斗";
            case DISPENSER -> "發射器";
            case DROPPER -> "投擲器";
            case TNT -> "TNT";

            default -> {
                String name = mat.name();
                if (name.endsWith("_LEAVES")) yield "葉子";
                if (name.endsWith("_SWORD")) yield "劍";
                if (name.endsWith("_PICKAXE")) yield "鎬";
                if (name.endsWith("_AXE")) yield "斧";
                if (name.endsWith("_SHOVEL")) yield "鏟";
                if (name.endsWith("_HOE")) yield "鋤";
                if (name.endsWith("_HELMET")) yield "頭盔";
                if (name.endsWith("_CHESTPLATE")) yield "胸甲";
                if (name.endsWith("_LEGGINGS")) yield "護腿";
                if (name.endsWith("_BOOTS")) yield "靴子";
                if (name.endsWith("_SHULKER_BOX")) yield "界伏盒";
                if (name.endsWith("_ORE")) yield "礦石";
                yield null;
            }
        };
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

    /**
     * 將秒數格式化為 "x分x秒 (x秒)"
     * 例如:
     * 180 -> "3分0秒 (180秒)"
     * 90  -> "1分30秒 (90秒)"
     * 45  -> "0分45秒 (45秒)"
     */
    public static String formatTimeSeconds(long totalSeconds) {
        if (totalSeconds < 0) totalSeconds = 0;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return minutes + "分" + seconds + "秒 (" + totalSeconds + "秒)";
    }
}
