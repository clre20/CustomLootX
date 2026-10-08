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
 * 試煉生怪磚 [步驟 2/6] 波次規模與基礎戰鬥設定介面
 */
public class SpawnerWizardStep2WavesGui extends CustomGuiHolder {

    private final SpawnerWizardContext context;

    public SpawnerWizardStep2WavesGui(SpawnerWizardContext context) {
        this.context = context;
        this.inventory = Bukkit.createInventory(
                this,
                27,
                context.getPlugin().getConfigManager().getComponent("gui.spawner.step2.waves-title", "&8[步驟 2/6] 戰鬥與波次設定")
        );
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

        // Slot 10: 生成間隔
        int delay = template.getSpawnDelaySeconds();
        String delayName = "&6【生成間隔】: &a" + delay + " &f秒";
        List<String> delayLore = List.of(
                "&7每波怪物生成之間的等待間隔時間",
                "&7目前設定: &a" + delay + " &7秒",
                "&7",
                "&e點擊此處 &f開啟數值調整畫面",
                "&8(可設定範圍: 1 ~ 60 秒)"
        );
        inventory.setItem(10, createButton(Material.CLOCK, delayName, delayLore));

        // Slot 12: 等待波次肅清 (不同輪是否等所有怪物死亡後才下一輪)
        boolean waitCleared = template.isWaitWaveCleared();
        String waitTitle = waitCleared ? "&a✔ 等待全滅才下一輪: 開啟" : "&c✖ 等待全滅才下一輪: 關閉";
        List<String> waitLore = List.of(
                "&7不同輪次之間是否需等待場上怪物全部死亡才進入下一輪",
                "&7目前狀態: " + (waitCleared ? "&a開啟 (全滅才下一輪)" : "&c關閉 (時間到直接出怪)"),
                "&7",
                "&f▪ &a開啟 (推薦)&7: 當前輪次怪物全數被消滅後，",
                "&7  才倒數生成間隔秒數進入下一輪。",
                "&f▪ &c關閉 (極限壓迫)&7: 不論場上怪物是否存活，",
                "&7  只要生成間隔秒數一到立即出下一輪！",
                "&7",
                "&e點擊切換 開啟 / 關閉"
        );
        inventory.setItem(12, createButton(waitCleared ? Material.SHIELD : Material.IRON_SWORD, waitTitle, waitLore));

        // Slot 14: 感應範圍
        int range = template.getPlayerRange();
        String rangeName = "&6【感應範圍】: &a" + range + " &f格";
        List<String> rangeLore = List.of(
                "&7玩家進入此距離內將自動觸發試煉戰鬥",
                "&7目前設定: &a" + range + " &7格",
                "&7",
                "&e點擊此處 &f開啟數值調整畫面",
                "&8(可設定範圍: 4 ~ 48 格)"
        );
        inventory.setItem(14, createButton(Material.COMPASS, rangeName, rangeLore));

        // Slot 16: 試煉完成提示音效設定 (獨立配置介面)
        boolean soundOn = template.isVictorySoundEnabled();
        String soundTitle = soundOn ? "&a✔ 完成提示音效: 開啟" : "&c✖ 完成提示音效: 關閉";
        List<String> soundLore = List.of(
                "&7挑戰成功獲勝時播放提示音效",
                "&7此生怪磚設定: " + (soundOn ? "&a開啟" : "&c關閉"),
                "&7目前音效: &e" + template.getVictorySound(),
                "&7音量: &a" + String.format("%.1f", template.getVictorySoundVolume()) + " &7| 音調: &a" + String.format("%.1f", template.getVictorySoundPitch()),
                "&7",
                "&e點擊此處 &f開啟完成音效專屬設定介面"
        );
        inventory.setItem(16, createButton(soundOn ? Material.JUKEBOX : Material.NOTE_BLOCK, soundTitle, soundLore));

        // Slot 18: 上一步 (步驟一：種類與名稱)
        inventory.setItem(18, createButton(
                Material.ARROW,
                "&e⬅ 上一步 &f(種類與名稱)",
                List.of("&7返回 [步驟 1/6] 修改生怪磚種類或名稱")
        ));

        // Slot 22: 對戰中進度提示 (Action Bar) 開關
        boolean abOn = template.isShowActionBar();
        String abTitle = abOn ? "&a✔ 對戰進度提示: 開啟" : "&c✖ 對戰進度提示: 關閉";
        List<String> abLore = List.of(
                "&7試煉戰鬥中向參戰玩家推播底部小字",
                "&7顯示即時討伐進度與場上怪物數 (Action Bar)",
                "&7",
                "&7此生怪磚設定: " + (abOn ? "&a開啟" : "&c關閉"),
                "&7",
                "&e點擊切換 開啟 / 關閉"
        );
        inventory.setItem(22, createButton(abOn ? Material.LIME_DYE : Material.RED_DYE, abTitle, abLore));

        // Slot 26: 下一步 (前往步驟三：怪物名單挑選)
        inventory.setItem(26, createButton(
                Material.LIME_CONCRETE,
                "&a下一步 ➜ &f(怪物名單挑選)",
                List.of(
                        "&7前往 [步驟 3/6] 挑選試煉生成的怪物清單",
                        "&a點擊前往下一步"
                )
        ));
    }

    private ItemStack createButton(Material mat, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(mat != null ? mat : Material.STONE);
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
            context.setTransitioning(true);
            new SpawnerCountAdjustGui(context, SpawnerCountAdjustGui.TargetSetting.SPAWN_DELAY).open();
            context.setTransitioning(false);
        } else if (slot == 12) {
            // 切換 等待波次全滅
            context.getTemplate().setWaitWaveCleared(!context.getTemplate().isWaitWaveCleared());
            context.getPlugin().getConfigManager().playSound(player, "click");
            render();
        } else if (slot == 14) {
            context.getPlugin().getConfigManager().playSound(player, "click");
            context.setTransitioning(true);
            new SpawnerCountAdjustGui(context, SpawnerCountAdjustGui.TargetSetting.PLAYER_RANGE).open();
            context.setTransitioning(false);
        } else if (slot == 16) {
            context.getPlugin().getConfigManager().playSound(player, "click");
            context.setTransitioning(true);
            new SpawnerVictorySoundGui(context).open();
            context.setTransitioning(false);
        } else if (slot == 18) {
            // 上一步 (步驟一：種類與名稱)
            context.getPlugin().getConfigManager().playSound(player, "click");
            context.setTransitioning(true);
            new SpawnerWizardStep1Gui(context).open();
            context.setTransitioning(false);
        } else if (slot == 22) {
            context.getTemplate().setShowActionBar(!context.getTemplate().isShowActionBar());
            context.getPlugin().getConfigManager().playSound(player, "click");
            render();
        } else if (slot == 26) {
            // 下一步 (步驟三：怪物名單挑選)
            context.getPlugin().getConfigManager().playSound(player, "click");
            context.setTransitioning(true);
            new SpawnerWizardStep3MobGui(context, 1).open();
            context.setTransitioning(false);
        }
    }

    @Override
    public void handleClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (!context.isTransitioning() && !context.isSavedSuccessfully() && !context.isDraftAbandoned()) {
            context.saveAsDraft();
        }
    }
}
