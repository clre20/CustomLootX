package clre20.customLootX.gui.wizard.spawner;

import clre20.customLootX.gui.CustomGuiHolder;
import clre20.customLootX.model.SpawnerMobEntry;
import clre20.customLootX.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 試煉生怪磚 - 原版生物挑選介面 (代表物全面使用生怪蛋，支援敵對/中立/被動友好分類與分頁)
 */
public class SpawnerVanillaMobSelectGui extends CustomGuiHolder {

    /**
     * 生物分類枚舉
     */
    public enum MobCategory {
        ALL("全部生物", Material.NETHER_STAR, "所有 83 種原版預設生物"),
        HOSTILE("敵對生物", Material.IRON_SWORD, "共 43 種具侵略性的敵對怪物"),
        NEUTRAL("中立生物", Material.ENDER_PEARL, "共 16 種條件敵對或反擊的中立生物"),
        PASSIVE("被動與友好", Material.WHEAT, "共 24 種溫和被動、友好與實用生物");

        private final String displayName;
        private final Material icon;
        private final String desc;

        MobCategory(String displayName, Material icon, String desc) {
            this.displayName = displayName;
            this.icon = icon;
            this.desc = desc;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Material getIcon() {
            return icon;
        }

        public String getDesc() {
            return desc;
        }
    }

    public record MobOption(EntityType type, Material icon, String name, String desc, MobCategory category) {}

    public static final List<MobOption> MOBS = List.of(
            new MobOption(EntityType.BREEZE, Material.BREEZE_SPAWN_EGG, "旋風人 (Breeze)", "試煉大廳核心生物，具備高機動性與風彈擊退攻擊", MobCategory.HOSTILE),
            new MobOption(EntityType.BOGGED, Material.BOGGED_SPAWN_EGG, "沼澤骷髏 (Bogged)", "試煉大廳毒箭骷髏，射出帶有中毒效果的箭矢", MobCategory.HOSTILE),
            new MobOption(EntityType.ZOMBIE, Material.ZOMBIE_SPAWN_EGG, "殭屍 (Zombie)", "經典近戰亡靈怪物，成群結隊發動攻擊", MobCategory.HOSTILE),
            new MobOption(EntityType.SKELETON, Material.SKELETON_SPAWN_EGG, "骷髏 (Skeleton)", "具備骨弓遠程射擊能力的亡靈射手", MobCategory.HOSTILE),
            new MobOption(EntityType.STRAY, Material.STRAY_SPAWN_EGG, "流浪者 (Stray)", "寒冷地區骷髏，射出附帶遲緩效果之箭", MobCategory.HOSTILE),
            new MobOption(EntityType.WITHER_SKELETON, Material.WITHER_SKELETON_SPAWN_EGG, "凋零骷髏 (Wither Skeleton)", "下界要塞近戰骷髏，攻擊附帶凋零枯萎效果", MobCategory.HOSTILE),
            new MobOption(EntityType.CREEPER, Material.CREEPER_SPAWN_EGG, "苦力怕 (Creeper)", "接近目標後自我引爆，造成劇烈範圍破壞", MobCategory.HOSTILE),
            new MobOption(EntityType.SLIME, Material.SLIME_SPAWN_EGG, "史萊姆 (Slime)", "跳躍攻擊生物，被擊敗後分裂成複數小型個體", MobCategory.HOSTILE),
            new MobOption(EntityType.MAGMA_CUBE, Material.MAGMA_CUBE_SPAWN_EGG, "岩漿史萊姆 (Magma Cube)", "下界跳躍生物，具備高抗火性與強烈彈跳力", MobCategory.HOSTILE),
            new MobOption(EntityType.BLAZE, Material.BLAZE_SPAWN_EGG, "烈焰使者 (Blaze)", "下界空中飛行生物，連續噴射高溫烈焰火球", MobCategory.HOSTILE),
            new MobOption(EntityType.PIGLIN, Material.PIGLIN_SPAWN_EGG, "豬靈 (Piglin)", "下界武裝生物，未著金裝時會發動群體圍攻", MobCategory.HOSTILE),
            new MobOption(EntityType.PIGLIN_BRUTE, Material.PIGLIN_BRUTE_SPAWN_EGG, "豬靈蠻兵 (Piglin Brute)", "手持金斧且不受金錠誘惑的高傷害下界衛士", MobCategory.HOSTILE),
            new MobOption(EntityType.ZOMBIFIED_PIGLIN, Material.ZOMBIFIED_PIGLIN_SPAWN_EGG, "殭屍豬靈 (Zombified Piglin)", "下界亡靈生物，持金劍並在受挑釁時狂暴圍攻", MobCategory.HOSTILE),
            new MobOption(EntityType.WITCH, Material.WITCH_SPAWN_EGG, "女巫 (Witch)", "向目標投擲各種有害藥水，並能自行飲藥治療", MobCategory.HOSTILE),
            new MobOption(EntityType.DROWNED, Material.DROWNED_SPAWN_EGG, "沉屍 (Drowned)", "水中殭屍，水域戰鬥力極強且有機率投擲三叉戟", MobCategory.HOSTILE),
            new MobOption(EntityType.HUSK, Material.HUSK_SPAWN_EGG, "屍殼 (Husk)", "沙漠乾屍，在日間不燃燒且攻擊附帶飢餓效果", MobCategory.HOSTILE),
            new MobOption(EntityType.SPIDER, Material.SPIDER_SPAWN_EGG, "蜘蛛 (Spider)", "能夠攀爬牆壁並快速撲向獵物的節肢生物", MobCategory.HOSTILE),
            new MobOption(EntityType.CAVE_SPIDER, Material.CAVE_SPIDER_SPAWN_EGG, "洞穴蜘蛛 (Cave Spider)", "體型小且敏捷，攻擊附帶致命劇毒效果", MobCategory.HOSTILE),
            new MobOption(EntityType.ENDERMAN, Material.ENDERMAN_SPAWN_EGG, "終界使者 (Enderman)", "具備瞬移能力的高大生物，直視其雙眼時會狂暴攻擊", MobCategory.HOSTILE),
            new MobOption(EntityType.SILVERFISH, Material.SILVERFISH_SPAWN_EGG, "蠹蟲 (Silverfish)", "隱藏於石磚中的小型生物，受襲時呼叫同伴", MobCategory.HOSTILE),
            new MobOption(EntityType.ENDERMITE, Material.ENDERMITE_SPAWN_EGG, "終界蟎 (Endermite)", "投擲終界珍珠時機率出現的敏捷終界害蟲", MobCategory.HOSTILE),
            new MobOption(EntityType.PILLAGER, Material.PILLAGER_SPAWN_EGG, "掠奪者 (Pillager)", "災厄陣營遠程部隊，手持十字弓發射箭矢", MobCategory.HOSTILE),
            new MobOption(EntityType.VINDICATOR, Material.VINDICATOR_SPAWN_EGG, "衛道士 (Vindicator)", "手持鐵斧的高爆發近戰災厄狂戰士", MobCategory.HOSTILE),
            new MobOption(EntityType.RAVAGER, Material.RAVAGER_SPAWN_EGG, "劫毀獸 (Ravager)", "巨大生命值與強大擊退力的災厄攻城巨獸", MobCategory.HOSTILE),
            new MobOption(EntityType.EVOKER, Material.EVOKER_SPAWN_EGG, "喚魔者 (Evoker)", "召喚地刺尖牙與穿牆惱鬼的法系災厄施法者", MobCategory.HOSTILE),
            new MobOption(EntityType.VEX, Material.VEX_SPAWN_EGG, "惱鬼 (Vex)", "能夠自由穿越障礙方塊的飛行微型幽靈精靈", MobCategory.HOSTILE),
            new MobOption(EntityType.PHANTOM, Material.PHANTOM_SPAWN_EGG, "幻翼 (Phantom)", "夜空盤旋的俯衝飛行掠食者，專攻失眠玩家", MobCategory.HOSTILE),
            new MobOption(EntityType.WARDEN, Material.WARDEN_SPAWN_EGG, "伏守者 (Warden)", "深暗之域盲眼巨獸，聲波共振攻擊極具破壞力", MobCategory.HOSTILE),
            new MobOption(EntityType.GUARDIAN, Material.GUARDIAN_SPAWN_EGG, "守衛者 (Guardian)", "海底神殿水下守衛，發射鎖定高能雷射光束", MobCategory.HOSTILE),
            new MobOption(EntityType.ELDER_GUARDIAN, Material.ELDER_GUARDIAN_SPAWN_EGG, "遠古守衛者 (Elder Guardian)", "大型水下神殿首領，向周圍施加挖掘疲勞效果", MobCategory.HOSTILE),
            new MobOption(EntityType.SHULKER, Material.SHULKER_SPAWN_EGG, "界伏鬼 (Shulker)", "終界城防禦實體，發射追蹤飛彈造成飄浮效果", MobCategory.HOSTILE),
            new MobOption(EntityType.GHAST, Material.GHAST_SPAWN_EGG, "地獄幽靈 (Ghast)", "下界巨大飛行生物，發射引發爆炸的致命火球", MobCategory.HOSTILE),
            new MobOption(EntityType.HOGLIN, Material.HOGLIN_SPAWN_EGG, "伏獰 (Hoglin)", "下界巨大攻擊性野豬，具備強力向上擊飛攻擊", MobCategory.HOSTILE),
            new MobOption(EntityType.ZOGLIN, Material.ZOGLIN_SPAWN_EGG, "殭屍伏獰 (Zoglin)", "喪失理智攻擊一切非亡靈生物的殭屍化伏獰", MobCategory.HOSTILE),
            new MobOption(EntityType.ZOMBIE_VILLAGER, Material.ZOMBIE_VILLAGER_SPAWN_EGG, "殭屍村民 (Zombie Villager)", "受感染的村民，可藉由虛弱藥水與金蘋果治癒", MobCategory.HOSTILE),
            new MobOption(EntityType.WITHER, Material.WITHER_SPAWN_EGG, "凋零怪 (Wither)", "召喚型飛行三頭亡靈首領，發射爆炸凋零之首", MobCategory.HOSTILE),
            new MobOption(EntityType.ENDER_DRAGON, Material.ENDER_DRAGON_SPAWN_EGG, "終界龍 (Ender Dragon)", "終界最終首領，於天際翱翔並噴吐龍息", MobCategory.HOSTILE),
            new MobOption(EntityType.ILLUSIONER, Material.BOW, "幻術師 (Illusioner)", "擅長致盲玩家並製造分身幻象的未啟用災厄村民", MobCategory.HOSTILE),
            new MobOption(EntityType.GIANT, Material.ZOMBIE_SPAWN_EGG, "巨人 (Giant)", "體型龐大無比的巨型殭屍", MobCategory.HOSTILE),
            new MobOption(EntityType.CREAKING, Material.CREAKING_SPAWN_EGG, "嘎枝 (Creaking)", "蒼白之園神秘生物，直視時定身，受攻擊時不傷本體", MobCategory.HOSTILE),
            new MobOption(EntityType.PARCHED, Material.PARCHED_SPAWN_EGG, "焦骸 (Parched)", "乾旱地區特化骷髏，環境適應力強", MobCategory.HOSTILE),
            new MobOption(EntityType.CAMEL_HUSK, Material.CAMEL_HUSK_SPAWN_EGG, "駱駝屍殼 (Camel Husk)", "沙漠受感染的駱駝骸骨實體", MobCategory.HOSTILE),
            new MobOption(EntityType.ZOMBIE_HORSE, Material.ZOMBIE_HORSE_SPAWN_EGG, "殭屍馬 (Zombie Horse)", "綠色亡靈馬匹實體", MobCategory.HOSTILE),
            new MobOption(EntityType.BEE, Material.BEE_SPAWN_EGG, "蜜蜂 (Bee)", "平時中立採蜜，蜂巢受擾或被攻擊時集體螫刺反擊", MobCategory.NEUTRAL),
            new MobOption(EntityType.CAVE_SPIDER, Material.CAVE_SPIDER_SPAWN_EGG, "洞穴蜘蛛 (Cave Spider)", "低光照時主動撲擊，日間或高光照時保持中立", MobCategory.NEUTRAL),
            new MobOption(EntityType.DOLPHIN, Material.DOLPHIN_SPAWN_EGG, "海豚 (Dolphin)", "友善水生生物，給予海豚恩惠，但受到傷害時集體反擊", MobCategory.NEUTRAL),
            new MobOption(EntityType.ENDERMAN, Material.ENDERMAN_SPAWN_EGG, "終界使者 (Enderman)", "平時中立游蕩，受挑釁或直視其雙眼時發動瞬移攻擊", MobCategory.NEUTRAL),
            new MobOption(EntityType.FOX, Material.FOX_SPAWN_EGG, "狐狸 (Fox)", "靈巧夜行動物，信任玩家時會保護並攻擊威脅者", MobCategory.NEUTRAL),
            new MobOption(EntityType.GOAT, Material.GOAT_SPAWN_EGG, "山羊 (Goat)", "山地躍跳生物，會偶爾發動強力衝撞將目標擊飛", MobCategory.NEUTRAL),
            new MobOption(EntityType.IRON_GOLEM, Material.IRON_GOLEM_SPAWN_EGG, "鐵魔像 (Iron Golem)", "村莊保衛者，受到攻擊時會發動強大重拳擊飛加害者", MobCategory.NEUTRAL),
            new MobOption(EntityType.LLAMA, Material.LLAMA_SPAWN_EGG, "羊駝 (Llama)", "高地負重馱獸，受攻擊時會向目標連續吐唾沫攻擊", MobCategory.NEUTRAL),
            new MobOption(EntityType.TRADER_LLAMA, Material.TRADER_LLAMA_SPAWN_EGG, "行商羊駝 (Trader Llama)", "流浪商人的護衛羊駝，保護主人並攻擊來犯敵人", MobCategory.NEUTRAL),
            new MobOption(EntityType.PANDA, Material.PANDA_SPAWN_EGG, "熊貓 (Panda)", "竹林憨厚動物，受挑釁或攻擊時會進行撲打反擊", MobCategory.NEUTRAL),
            new MobOption(EntityType.PIGLIN, Material.PIGLIN_SPAWN_EGG, "豬靈 (Piglin)", "穿戴金裝時保持中立，可使用金錠進行物資以物易物", MobCategory.NEUTRAL),
            new MobOption(EntityType.POLAR_BEAR, Material.POLAR_BEAR_SPAWN_EGG, "北極熊 (Polar Bear)", "雪原巨獸，身邊有幼崽或自身被攻擊時會站立撲擊", MobCategory.NEUTRAL),
            new MobOption(EntityType.PUFFERFISH, Material.PUFFERFISH_SPAWN_EGG, "河豚 (Pufferfish)", "防禦型水生生物，玩家靠近時膨脹並造成劇毒傷害", MobCategory.NEUTRAL),
            new MobOption(EntityType.SPIDER, Material.SPIDER_SPAWN_EGG, "蜘蛛 (Spider)", "日間光線充足時保持中立，夜間或受攻擊時主動跳撲", MobCategory.NEUTRAL),
            new MobOption(EntityType.WOLF, Material.WOLF_SPAWN_EGG, "狼 (Wolf)", "森林犬科動物，馴服前受攻擊會引發狼群紅眼狂暴圍攻", MobCategory.NEUTRAL),
            new MobOption(EntityType.ZOMBIFIED_PIGLIN, Material.ZOMBIFIED_PIGLIN_SPAWN_EGG, "殭屍豬靈 (Zombified Piglin)", "下界中立亡靈，平時和平相處，被攻擊時連鎖怒氣反擊", MobCategory.NEUTRAL),
            new MobOption(EntityType.ALLAY, Material.ALLAY_SPAWN_EGG, "悅靈 (Allay)", "喜愛音樂的友善小精靈，能幫玩家拾取並收集特定物品", MobCategory.PASSIVE),
            new MobOption(EntityType.ARMADILLO, Material.ARMADILLO_SPAWN_EGG, "犰狳 (Armadillo)", "溫馴熱帶動物，受驚嚇時蜷縮成球，掉落狼鎧鱗甲", MobCategory.PASSIVE),
            new MobOption(EntityType.AXOLOTL, Material.AXOLOTL_SPAWN_EGG, "美西螈 (Axolotl)", "水下兩棲生物，能裝入水桶，協助玩家對抗水下敵對生物", MobCategory.PASSIVE),
            new MobOption(EntityType.BAT, Material.BAT_SPAWN_EGG, "蝙蝠 (Bat)", "洞穴環境被動飛行生物，在黑暗中發出吱吱聲啼叫", MobCategory.PASSIVE),
            new MobOption(EntityType.CAMEL, Material.CAMEL_SPAWN_EGG, "駱駝 (Camel)", "沙漠雙人騎乘坐騎，具備衝刺跨溝能力", MobCategory.PASSIVE),
            new MobOption(EntityType.CAT, Material.CAT_SPAWN_EGG, "貓 (Cat)", "可馴服伴侶動物，能驅趕苦力怕與幻翼並在早晨贈禮", MobCategory.PASSIVE),
            new MobOption(EntityType.CHICKEN, Material.CHICKEN_SPAWN_EGG, "雞 (Chicken)", "農場家禽生物，定期生蛋，掉落羽毛與生雞肉", MobCategory.PASSIVE),
            new MobOption(EntityType.COW, Material.COW_SPAWN_EGG, "牛 (Cow)", "農場主要家畜，可用鐵桶擠牛奶解除所有狀態效果", MobCategory.PASSIVE),
            new MobOption(EntityType.DONKEY, Material.DONKEY_SPAWN_EGG, "驢 (Donkey)", "可裝備箱子的負重馱獸，是長途探險運輸的好夥伴", MobCategory.PASSIVE),
            new MobOption(EntityType.FROG, Material.FROG_SPAWN_EGG, "青蛙 (Frog)", "沼澤跳躍生物，可吃下小史萊姆與小岩漿怪產出青蛙燈", MobCategory.PASSIVE),
            new MobOption(EntityType.GLOW_SQUID, Material.GLOW_SQUID_SPAWN_EGG, "發光魷魚 (Glow Squid)", "深海發光軟體生物，掉落可讓告示牌與物品發光的墨囊", MobCategory.PASSIVE),
            new MobOption(EntityType.HORSE, Material.HORSE_SPAWN_EGG, "馬 (Horse)", "經典高速陸地坐騎，具備不同速度與跳躍力屬性", MobCategory.PASSIVE),
            new MobOption(EntityType.MOOSHROOM, Material.MOOSHROOM_SPAWN_EGG, "哞菇 (Mooshroom)", "蘑菇島獨特共生生物，可用碗取得蘑菇煲", MobCategory.PASSIVE),
            new MobOption(EntityType.MULE, Material.MULE_SPAWN_EGG, "騾 (Mule)", "馬與驢雜交之後代，兼具馬匹速度與背包負重機能", MobCategory.PASSIVE),
            new MobOption(EntityType.OCELOT, Material.OCELOT_SPAWN_EGG, "豹貓 (Ocelot)", "叢林野生害羞貓科動物，受到信任後不會逃跑", MobCategory.PASSIVE),
            new MobOption(EntityType.PARROT, Material.PARROT_SPAWN_EGG, "鸚鵡 (Parrot)", "叢林羽翼鳥類，可停留在玩家肩上並模仿附近怪物叫聲", MobCategory.PASSIVE),
            new MobOption(EntityType.PIG, Material.PIG_SPAWN_EGG, "豬 (Pig)", "經典農場家畜，可裝備鞍使用胡蘿蔔釣竿引導騎乘", MobCategory.PASSIVE),
            new MobOption(EntityType.RABBIT, Material.RABBIT_SPAWN_EGG, "兔子 (Rabbit)", "敏捷跳躍生物，掉落兔肉、兔皮與幸運兔腳", MobCategory.PASSIVE),
            new MobOption(EntityType.SHEEP, Material.SHEEP_SPAWN_EGG, "綿羊 (Sheep)", "羊毛主要來源生物，吃草復原毛髮，可用剪刀剪毛", MobCategory.PASSIVE),
            new MobOption(EntityType.SNIFFER, Material.SNIFFER_SPAWN_EGG, "嗅探獸 (Sniffer)", "古老巨大溫和生物，能在土壤中嗅出遠古植物種子", MobCategory.PASSIVE),
            new MobOption(EntityType.SNOW_GOLEM, Material.SNOW_GOLEM_SPAWN_EGG, "雪魔像 (Snow Golem)", "玩家建造的友善傀儡，向敵對目標投擲雪球牽制", MobCategory.PASSIVE),
            new MobOption(EntityType.STRIDER, Material.STRIDER_SPAWN_EGG, "熾足獸 (Strider)", "熔岩漫步生物，使用詭異菌釣竿可在下界岩漿海自由騎乘", MobCategory.PASSIVE),
            new MobOption(EntityType.TURTLE, Material.TURTLE_SPAWN_EGG, "海龜 (Turtle)", "海灘水陸兩棲動物，成長時掉落海龜鱗甲用於製造海龜帽", MobCategory.PASSIVE),
            new MobOption(EntityType.VILLAGER, Material.VILLAGER_SPAWN_EGG, "村民 (Villager)", "文明村莊居民，擁有豐富職業分工，可與玩家進行綠寶石交易", MobCategory.PASSIVE)

    );

    private final SpawnerWizardContext context;
    private final MobCategory category;
    private final int page;
    private static final int ITEMS_PER_PAGE = 45;

    public SpawnerVanillaMobSelectGui(SpawnerWizardContext context, int page) {
        this(context, MobCategory.HOSTILE, page);
    }

    public SpawnerVanillaMobSelectGui(SpawnerWizardContext context, MobCategory category, int page) {
        this.context = context;
        this.category = (category != null) ? category : MobCategory.HOSTILE;
        this.page = Math.max(1, page);
        this.inventory = Bukkit.createInventory(this, 54, TextUtil.parse("&8選擇原版生物 - " + this.category.getDisplayName()));
        render();
    }

    public static List<MobOption> getMobsForCategory(MobCategory category) {
        if (category == MobCategory.ALL) {
            return MOBS;
        }
        List<MobOption> list = new ArrayList<>();
        for (MobOption mob : MOBS) {
            if (mob.category() == category) {
                list.add(mob);
            }
        }
        return list;
    }

    public void open() {
        context.getPlayer().openInventory(this.inventory);
    }

    private void render() {
        inventory.clear();

        List<MobOption> currentList = getMobsForCategory(category);
        int totalPages = Math.max(1, (int) Math.ceil((double) currentList.size() / ITEMS_PER_PAGE));
        int startIndex = (page - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, currentList.size());

        for (int i = startIndex; i < endIndex; i++) {
            MobOption opt = currentList.get(i);
            int slot = i - startIndex;

            List<String> lore = new ArrayList<>();
            lore.add("&7" + opt.desc());
            lore.add("&7分類: &e" + opt.category().getDisplayName());
            lore.add("&7實體代號: &f" + opt.type().name());
            lore.add("&7");
            lore.add("&a點擊將此生物加入生怪磚生成池！");

            ItemStack item = createButton(opt.icon(), "&e" + opt.name(), lore);
            inventory.setItem(slot, item);
        }

        // 底部工具列背景 (45 ~ 53)
        String fillerName = context.getPlugin().getConfigManager().getText("gui.common.filler-name", " ");
        ItemStack filler = createButton(Material.GRAY_STAINED_GLASS_PANE, fillerName, null);
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        // Slot 45: 返回怪物池
        inventory.setItem(45, createButton(Material.ARROW, "&e⬅ 返回怪物池", List.of("&7取消選擇並返回生怪磚怪物池列表")));

        // Slot 46: 全部生物 (83種)
        renderCategoryButton(46, MobCategory.ALL, 83);

        // Slot 47: 敵對生物 (43種)
        renderCategoryButton(47, MobCategory.HOSTILE, 43);

        // Slot 48: 中立生物 (16種)
        renderCategoryButton(48, MobCategory.NEUTRAL, 16);

        // Slot 49: 被動與友好 (24種)
        renderCategoryButton(49, MobCategory.PASSIVE, 24);

        // Slot 50: 上一頁
        if (page > 1) {
            inventory.setItem(50, createButton(Material.FEATHER, "&b⬅ 上一頁", List.of("&7前往第 " + (page - 1) + " 頁")));
        }

        // Slot 51: 頁碼指示
        inventory.setItem(51, createButton(Material.PAPER, "&f頁數: &e" + page + " / " + totalPages, List.of(
                "&7當前分類生物總數: &f" + currentList.size() + " &7隻",
                "&7分類: &a" + category.getDisplayName()
        )));

        // Slot 52: 下一頁
        if (page < totalPages) {
            inventory.setItem(52, createButton(Material.FEATHER, "&b下一頁 ➜", List.of("&7前往第 " + (page + 1) + " 頁")));
        }
    }

    private void renderCategoryButton(int slot, MobCategory targetCategory, int count) {
        boolean isSelected = (this.category == targetCategory);
        String title = isSelected
                ? "&a✔ &e" + targetCategory.getDisplayName() + " &a(" + count + "種)"
                : "&7" + targetCategory.getDisplayName() + " &8(" + count + "種)";
        List<String> lore = new ArrayList<>();
        lore.add("&7" + targetCategory.getDesc());
        lore.add("&7生物數量: &f" + count + " &7種");
        lore.add("&7");
        if (isSelected) {
            lore.add("&a▶ 目前正在瀏覽此分類");
        } else {
            lore.add("&e點擊切換至此分類！");
        }
        ItemStack item = createButton(targetCategory.getIcon(), title, lore);
        if (isSelected) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                item.setItemMeta(meta);
            }
        }
        inventory.setItem(slot, item);
    }

    private ItemStack createButton(Material mat, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtil.parse(name));
            if (loreLines != null) {
                List<Component> lore = new ArrayList<>();
                for (String line : loreLines) {
                    lore.add(TextUtil.parse(line));
                }
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        List<MobOption> currentList = getMobsForCategory(category);
        int totalPages = Math.max(1, (int) Math.ceil((double) currentList.size() / ITEMS_PER_PAGE));

        if (slot >= 0 && slot < ITEMS_PER_PAGE) {
            int index = (page - 1) * ITEMS_PER_PAGE + slot;
            if (index < currentList.size()) {
                MobOption opt = currentList.get(index);
                double currentTotal = context.getTemplate().getTotalMobChance();
                double remain = TextUtil.roundChance(100.0 - currentTotal);
                double defaultChance = (remain > 0.0) ? Math.min(remain, 10.0) : 10.0;

                context.getTemplate().getMobPool().add(new SpawnerMobEntry(opt.type(), defaultChance));
                context.getPlugin().getConfigManager().playSound(player, "success");
                player.sendMessage(TextUtil.parse("&a[CustomLootX] 已成功將 &e" + opt.name() + " &a加入生怪磚生成池！"));
                new SpawnerWizardStep2MobGui(context, 1).open();
            }
        } else if (slot == 45) {
            context.getPlugin().getConfigManager().playSound(player, "click");
            new SpawnerWizardStep2MobGui(context, 1).open();
        } else if (slot == 46) {
            if (category != MobCategory.ALL) {
                context.getPlugin().getConfigManager().playSound(player, "click");
                new SpawnerVanillaMobSelectGui(context, MobCategory.ALL, 1).open();
            }
        } else if (slot == 47) {
            if (category != MobCategory.HOSTILE) {
                context.getPlugin().getConfigManager().playSound(player, "click");
                new SpawnerVanillaMobSelectGui(context, MobCategory.HOSTILE, 1).open();
            }
        } else if (slot == 48) {
            if (category != MobCategory.NEUTRAL) {
                context.getPlugin().getConfigManager().playSound(player, "click");
                new SpawnerVanillaMobSelectGui(context, MobCategory.NEUTRAL, 1).open();
            }
        } else if (slot == 49) {
            if (category != MobCategory.PASSIVE) {
                context.getPlugin().getConfigManager().playSound(player, "click");
                new SpawnerVanillaMobSelectGui(context, MobCategory.PASSIVE, 1).open();
            }
        } else if (slot == 50 && page > 1) {
            context.getPlugin().getConfigManager().playSound(player, "click");
            new SpawnerVanillaMobSelectGui(context, category, page - 1).open();
        } else if (slot == 52 && page < totalPages) {
            context.getPlugin().getConfigManager().playSound(player, "click");
            new SpawnerVanillaMobSelectGui(context, category, page + 1).open();
        }
    }
}
