package clre20.customLootX.gui.wizard.spawner;

import clre20.customLootX.gui.CustomGuiHolder;
import clre20.customLootX.model.SpawnerTemplate;
import clre20.customLootX.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 試煉生怪磚 [步驟 3/6] 波次與數量設定介面
 */
public class SpawnerWizardStep3WavesGui extends CustomGuiHolder {

    private final SpawnerWizardContext context;

    public SpawnerWizardStep3WavesGui(SpawnerWizardContext context) {
        this.context = context;
        this.inventory = Bukkit.createInventory(this, 27, context.getPlugin().getConfigManager().getComponent("gui.spawner.step3.title", "&8[步驟 3/6] 波次與數量設定"));
        render();
    }

    public void open() {
        context.getPlayer().openInventory(this.inventory);
    }

    private void render() {
        String fillerName = context.getPlugin().getConfigManager().getText("gui.common.filler-name", " ");
        ItemStack filler = createButton(Material.GRAY_STAINED_GLASS_PANE, fillerName, null);
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        SpawnerTemplate template = context.getTemplate();

        // Slot 10: 總怪數
        int total = template.getTotalMobs();
        String totalName = context.getPlugin().getConfigManager().getText("gui.spawner.step3.total-button-name",
                "&6【總怪數】: &a%total% &f隻", "%total%", String.valueOf(total));
        List<String> totalLore = context.getPlugin().getConfigManager().getStringList(
                "gui.spawner.step3.total-button-lore",
                List.of(
                        "&7試煉挑戰中必須擊敗的怪物總數量",
                        "&7目前設定: &a%total% &7隻",
                        "&7",
                        "&e點擊此處 &f開啟數值調整畫面",
                        "&8(可設定範圍: 1 ~ 64 隻)"
                ),
                "%total%", String.valueOf(total)
        );
        inventory.setItem(10, createButton(Material.IRON_SWORD, totalName, totalLore));

        // Slot 12: 同時存活上限
        int sim = template.getSimultaneousMobs();
        String simName = context.getPlugin().getConfigManager().getText("gui.spawner.step3.sim-button-name",
                "&6【同時存活上限】: &a%sim% &f隻", "%sim%", String.valueOf(sim));
        List<String> simLore = context.getPlugin().getConfigManager().getStringList(
                "gui.spawner.step3.sim-button-lore",
                List.of(
                        "&7戰鬥場上同時存在的最大怪物數量",
                        "&7防止怪物過多造成玩家負擔或伺服器卡頓",
                        "&7目前設定: &a%sim% &7隻",
                        "&7",
                        "&e點擊此處 &f開啟數值調整畫面",
                        "&8(可設定範圍: 1 ~ 16 隻)"
                ),
                "%sim%", String.valueOf(sim)
        );
        inventory.setItem(12, createButton(Material.ARMOR_STAND, simName, simLore));

        // Slot 14: 生成間隔
        int delay = template.getSpawnDelaySeconds();
        String delayName = context.getPlugin().getConfigManager().getText("gui.spawner.step3.delay-button-name",
                "&6【生成間隔】: &a%delay% &f秒", "%delay%", String.valueOf(delay));
        List<String> delayLore = context.getPlugin().getConfigManager().getStringList(
                "gui.spawner.step3.delay-button-lore",
                List.of(
                        "&7每波怪物生成之間的等待間隔時間",
                        "&7目前設定: &a%delay% &7秒",
                        "&7",
                        "&e點擊此處 &f開啟數值調整畫面",
                        "&8(可設定範圍: 1 ~ 60 秒)"
                ),
                "%delay%", String.valueOf(delay)
        );
        inventory.setItem(14, createButton(Material.CLOCK, delayName, delayLore));

        // Slot 16: 感應範圍
        int range = template.getPlayerRange();
        String rangeName = context.getPlugin().getConfigManager().getText("gui.spawner.step3.range-button-name",
                "&6【感應範圍】: &a%range% &f格", "%range%", String.valueOf(range));
        List<String> rangeLore = context.getPlugin().getConfigManager().getStringList(
                "gui.spawner.step3.range-button-lore",
                List.of(
                        "&7玩家進入此距離內將自動觸發試煉戰鬥",
                        "&7目前設定: &a%range% &7格",
                        "&7",
                        "&e點擊此處 &f開啟數值調整畫面",
                        "&8(可設定範圍: 4 ~ 48 格)"
                ),
                "%range%", String.valueOf(range)
        );
        inventory.setItem(16, createButton(Material.COMPASS, rangeName, rangeLore));

        // Slot 18: 上一步 (步驟二：選擇生成怪物種類)
        String backName = context.getPlugin().getConfigManager().getText("gui.spawner.step3.back-name", "&e⬅ 上一步 &f(選擇生成怪物)");
        List<String> backLore = context.getPlugin().getConfigManager().getStringList(
                "gui.spawner.step3.back-lore",
                List.of("&7返回 [步驟 2/6] 重新挑選怪物種類")
        );
        inventory.setItem(18, createButton(Material.ARROW, backName, backLore));

        // Slot 22: 對戰中進度提示 (Action Bar) 開關
        boolean abOn = template.isShowActionBar();
        String abTitle = abOn
                ? context.getPlugin().getConfigManager().getText("gui.spawner.step3.actionbar-on-name", "&a✔ 對戰進度提示: 開啟")
                : context.getPlugin().getConfigManager().getText("gui.spawner.step3.actionbar-off-name", "&c✖ 對戰進度提示: 關閉");
        List<String> abLore = context.getPlugin().getConfigManager().getStringList(
                "gui.spawner.step3.actionbar-lore",
                List.of(
                        "&7試煉戰鬥中向參戰玩家推播底部小字",
                        "&7顯示即時討伐進度與場上怪物數 (Action Bar)",
                        "&7",
                        "&7此生怪磚設定: %status%",
                        "&7",
                        "&e點擊切換 開啟 / 關閉"
                ),
                "%status%", abOn ? "&a開啟" : "&c關閉"
        );
        inventory.setItem(22, createButton(abOn ? Material.LIME_DYE : Material.RED_DYE, abTitle, abLore));

        // Slot 24: 試煉完成提示音效設定 (獨立配置介面)
        boolean soundOn = template.isVictorySoundEnabled();
        String soundTitle = soundOn
                ? "&a✔ 完成提示音效: 開啟"
                : "&c✖ 完成提示音效: 關閉";
        List<String> soundLore = List.of(
                "&7挑戰成功獲勝時播放提示音效",
                "&7此生怪磚設定: " + (soundOn ? "&a開啟" : "&c關閉"),
                "&7目前音效: &e" + template.getVictorySound(),
                "&7音量: &a" + String.format("%.1f", template.getVictorySoundVolume()) + " &7| 音調: &a" + String.format("%.1f", template.getVictorySoundPitch()),
                "&7",
                "&e點擊此處 &f開啟完成音效專屬設定介面",
                "&8(提供 27 種原版精選音效庫、聊天室自訂輸入與即時試聽)"
        );
        inventory.setItem(24, createButton(soundOn ? Material.JUKEBOX : Material.NOTE_BLOCK, soundTitle, soundLore));

        // Slot 26: 下一步 (步驟四：冷卻與獎勵設定)
        String nextName = context.getPlugin().getConfigManager().getText("gui.spawner.step3.next-name", "&a下一步 ➜ &f(冷卻與獎勵設定)");
        List<String> nextLore = context.getPlugin().getConfigManager().getStringList(
                "gui.spawner.step3.next-lore",
                List.of(
                        "&7前往 [步驟 4/6] 設定冷卻重置模式與獲勝獎勵數量",
                        "&a點擊前往下一步"
                )
        );
        inventory.setItem(26, createButton(Material.LIME_CONCRETE, nextName, nextLore));
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

        if (slot == 10) {
            context.getPlugin().getConfigManager().playSound(player, "click");
            new SpawnerCountAdjustGui(context, SpawnerCountAdjustGui.TargetSetting.TOTAL_MOBS).open();
        } else if (slot == 12) {
            context.getPlugin().getConfigManager().playSound(player, "click");
            new SpawnerCountAdjustGui(context, SpawnerCountAdjustGui.TargetSetting.SIMULTANEOUS_MOBS).open();
        } else if (slot == 14) {
            context.getPlugin().getConfigManager().playSound(player, "click");
            new SpawnerCountAdjustGui(context, SpawnerCountAdjustGui.TargetSetting.SPAWN_DELAY).open();
        } else if (slot == 16) {
            context.getPlugin().getConfigManager().playSound(player, "click");
            new SpawnerCountAdjustGui(context, SpawnerCountAdjustGui.TargetSetting.PLAYER_RANGE).open();
        } else if (slot == 18) {
            context.getPlugin().getConfigManager().playSound(player, "click");
            new SpawnerWizardStep2MobGui(context, 1).open();
        } else if (slot == 22) {
            context.getTemplate().setShowActionBar(!context.getTemplate().isShowActionBar());
            context.getPlugin().getConfigManager().playSound(player, "click");
            render();
        } else if (slot == 24) {
            context.getPlugin().getConfigManager().playSound(player, "click");
            new SpawnerVictorySoundGui(context).open();
        } else if (slot == 26) {
            context.getPlugin().getConfigManager().playSound(player, "click");
            new SpawnerWizardStep4CooldownGui(context).open();
        }
    }
}
