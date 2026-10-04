package clre20.customLootX.gui.wizard.spawner;

import clre20.customLootX.gui.CustomGuiHolder;
import clre20.customLootX.model.SpawnerTemplate;
import clre20.customLootX.model.VaultCooldownMode;
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
 * 試煉生怪磚 [步驟 4/6] 冷卻模式與出貨數量設定介面
 */
public class SpawnerWizardStep4CooldownGui extends CustomGuiHolder {

    private final SpawnerWizardContext context;

    public SpawnerWizardStep4CooldownGui(SpawnerWizardContext context) {
        this.context = context;
        this.inventory = Bukkit.createInventory(this, 27, context.getPlugin().getConfigManager().getComponent(
                "gui.spawner.step4.cooldown-title",
                "&8[步驟 4-2/6] 冷卻模式與出貨數量"
        ));
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

        // Slot 11: 冷卻模式切換
        VaultCooldownMode mode = template.getCooldownMode();
        String modeBtnTitle = context.getPlugin().getConfigManager().getText("gui.vault.step2.mode-button-name", "&6【冷卻與重置模式】");
        String modeDesc;
        if (mode == VaultCooldownMode.PLAYER_COOLDOWN) {
            modeDesc = "&b個人獨立冷卻 &7(每人各算冷卻)";
        } else if (mode == VaultCooldownMode.GLOBAL_COOLDOWN) {
            modeDesc = "&e全域冷卻 &7(挑戰完方塊進入全服冷卻)";
        } else {
            modeDesc = "&c終生一次 &7(每人只能領取一次獲勝獎勵)";
        }

        List<String> modeLore = context.getPlugin().getConfigManager().getStringList(
                "gui.vault.step2.mode-button-lore",
                List.of(
                        "&7目前模式: %mode%",
                        "&7",
                        "&e點擊切換模式：",
                        "&7▪ &b個人獨立冷卻",
                        "&7▪ &e全域冷卻",
                        "&7▪ &c終生一次"
                ),
                "%mode%", modeDesc
        );
        inventory.setItem(11, createButton(Material.COMPASS, modeBtnTitle, modeLore));

        // Slot 13: 冷卻時間調整 (終生一次時反灰)
        boolean hasCooldownTime = (mode != VaultCooldownMode.ONCE_PER_PLAYER);
        String timeBtnTitle = context.getPlugin().getConfigManager().getText("gui.vault.step2.time-button-name", "&e冷卻間隔時間");
        List<String> timeLore;
        if (hasCooldownTime) {
            timeLore = context.getPlugin().getConfigManager().getStringList(
                    "gui.vault.step2.time-button-lore",
                    List.of(
                            "&7目前設定: &a%minutes% &7分鐘",
                            "&7",
                            "&e點擊此處 &f開啟時間調整畫面"
                    ),
                    "%minutes%", template.getCooldownMinutes()
            );
            inventory.setItem(13, createButton(Material.CLOCK, timeBtnTitle, timeLore));
        } else {
            timeLore = context.getPlugin().getConfigManager().getStringList(
                    "gui.vault.step2.time-disabled-lore",
                    List.of(
                            "&7目前為【終生一次】模式",
                            "&8無須設定冷卻時間"
                    )
            );
            inventory.setItem(13, createButton(Material.GRAY_DYE, "&8冷卻間隔時間 (無須設定)", timeLore));
        }

        // Slot 15: 每次獲勝彈出物品數量
        int rollCount = template.getRollCount();
        String rollBtnTitle = context.getPlugin().getConfigManager().getText("gui.vault.step2.rolls-button-name",
                "&6【每次獲勝出貨數量】: &a%rolls% &f件", "%rolls%", String.valueOf(rollCount));
        List<String> rollLore = context.getPlugin().getConfigManager().getStringList(
                "gui.vault.step2.rolls-button-lore",
                List.of(
                        "&7挑戰勝利時連續彈出幾件物品 (預設 3 件)",
                        "&7目前設定: &a%rolls% &7件",
                        "&7",
                        "&e點擊此處 &f開啟數量按鈕調整畫面",
                        "&8(可設定範圍: 1 ~ 16 件)"
                ),
                "%rolls%", String.valueOf(rollCount)
        );
        inventory.setItem(15, createButton(Material.GOLD_INGOT, rollBtnTitle, rollLore));

        // Slot 18: 上一步
        boolean hasRandom = template.hasRandomInSequence();
        String backName = hasRandom ? "&e⬅ 上一步 &f(隨機池機率)" : "&e⬅ 上一步 &f(怪物生成順序)";
        List<String> backLore = List.of(hasRandom ? "&7返回 [步驟 4-1-2/6] 調整隨機池機率" : "&7返回 [步驟 4/6] 調整怪物生成順序");
        inventory.setItem(18, createButton(Material.ARROW, backName, backLore));

        // Slot 26: 下一步 (前往步驟五：獲勝獎勵掉落池)
        String nextName = context.getPlugin().getConfigManager().getText("gui.spawner.step4.next-name", "&a下一步 ➜ &f(獲勝獎勵掉落池)");
        List<String> nextLore = context.getPlugin().getConfigManager().getStringList(
                "gui.spawner.step4.next-lore",
                List.of(
                        "&7前往 [步驟 5/6] 設定獲勝掉落池物品與機率",
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

        if (slot == 11) {
            // 循環切換冷卻模式
            VaultCooldownMode next = context.getTemplate().getCooldownMode().next();
            context.getTemplate().setCooldownMode(next);
            context.getPlugin().getConfigManager().playSound(player, "click");
            render();
        } else if (slot == 13) {
            // 點擊調整冷卻時間
            if (context.getTemplate().getCooldownMode() != VaultCooldownMode.ONCE_PER_PLAYER) {
                context.getPlugin().getConfigManager().playSound(player, "click");
                context.setTransitioning(true);
                new SpawnerTimeAdjustGui(context).open();
                context.setTransitioning(false);
            } else {
                context.getPlugin().getConfigManager().playSound(player, "error");
            }
        } else if (slot == 15) {
            // 點擊調整每次出貨數量
            context.getPlugin().getConfigManager().playSound(player, "click");
            context.setTransitioning(true);
            new SpawnerCountAdjustGui(context, SpawnerCountAdjustGui.TargetSetting.ROLL_COUNT).open();
            context.setTransitioning(false);
        } else if (slot == 18) {
            // 上一步 (智慧返回：若有隨機格返回隨機池機率，否則返回順序看板)
            context.getPlugin().getConfigManager().playSound(player, "click");
            context.setTransitioning(true);
            if (context.getTemplate().hasRandomInSequence()) {
                new SpawnerWizardStep4RandomChanceGui(context).open();
            } else {
                new SpawnerWizardStep4SequenceGui(context).open();
            }
            context.setTransitioning(false);
        } else if (slot == 26) {
            // 下一步 (步驟五：獲勝獎勵掉落池)
            context.getPlugin().getConfigManager().playSound(player, "click");
            context.setTransitioning(true);
            new SpawnerWizardStep5LootGui(context, 1).open();
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
