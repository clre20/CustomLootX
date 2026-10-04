package clre20.customLootX.gui.wizard;

import clre20.customLootX.gui.CustomGuiHolder;
import clre20.customLootX.model.LootTemplate;
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

public class WizardStep2Gui extends CustomGuiHolder {

    private final WizardContext context;

    public WizardStep2Gui(WizardContext context) {
        this.context = context;
        this.inventory = Bukkit.createInventory(this, 27, context.getPlugin().getConfigManager().getComponent("gui.step2.title", "&8[步驟2/4] 重置模式"));
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

        LootTemplate template = context.getTemplate();
        boolean enabled = template.isResetEnabled();
        int minutes = template.getResetMinutes();

        // Slot 12: Reset toggle
        String toggleName = context.getPlugin().getConfigManager().getText("gui.step2.toggle-button-name", "&6自動重置功能");
        String statusStr = enabled
                ? context.getPlugin().getConfigManager().getText("gui.step2.status-on", "&a✔ 開啟")
                : context.getPlugin().getConfigManager().getText("gui.step2.status-off", "&c✖ 關閉");

        List<String> toggleLore = context.getPlugin().getConfigManager().getStringList(
                "gui.step2.toggle-button-lore",
                List.of(
                        "&7方塊被刷完變為普通沙/礫石後",
                        "&7在指定時間後自動復原並重抽掉落物",
                        "&7",
                        "&7當前狀態: %status%",
                        "&7",
                        "&e點擊切換 開啟 / 關閉"
                ),
                "%status%", statusStr
        );
        String iconOnStr = context.getPlugin().getConfigManager().getText("gui.step2.icon-on", "LIME_DYE");
        String iconOffStr = context.getPlugin().getConfigManager().getText("gui.step2.icon-off", "RED_DYE");
        Material matOn = Material.matchMaterial(iconOnStr);
        if (matOn == null) matOn = Material.LIME_DYE;
        Material matOff = Material.matchMaterial(iconOffStr);
        if (matOff == null) matOff = Material.RED_DYE;

        inventory.setItem(12, createButton(enabled ? matOn : matOff, toggleName, toggleLore));

        // Slot 14: Minutes adjustment
        String timeBtnName = context.getPlugin().getConfigManager().getText("gui.step2.time-button-name", "&e重置間隔時間");
        List<String> timeLore = context.getPlugin().getConfigManager().getStringList(
                "gui.step2.time-button-lore",
                List.of(
                        "&7目前設定: &a%minutes% &7分鐘",
                        "&7",
                        "&e點擊此處 &f開啟時間調整畫面 (支援按鈕直覺增減)"
                ),
                "%minutes%", minutes
        );
        inventory.setItem(14, createButton(Material.CLOCK, timeBtnName, timeLore));

        // Slot 18: Back to Step 1
        String backName = context.getPlugin().getConfigManager().getText("gui.common.back-to-step1-name", "&e⬅ 上一步 &f(基本設定)");
        List<String> backLore = context.getPlugin().getConfigManager().getStringList(
                "gui.common.back-to-step1-lore",
                List.of("&7返回 [步驟 1/4] 修改名稱或材質")
        );
        inventory.setItem(18, createButton(Material.ARROW, backName, backLore));

        // Slot 26: Next to Step 3
        String nextName = context.getPlugin().getConfigManager().getText("gui.step2.next-button-name", "&a下一步 ➜ &f(掉落池設定)");
        List<String> nextLore = context.getPlugin().getConfigManager().getStringList(
                "gui.step2.next-button-lore",
                List.of(
                        "&7前往 [步驟 3/4] 設定掉落物與機率",
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
        int slot = event.getRawSlot();
        Player player = context.getPlayer();
        LootTemplate template = context.getTemplate();

        switch (slot) {
            case 12 -> {
                // Toggle reset enabled
                template.setResetEnabled(!template.isResetEnabled());
                context.getPlugin().getConfigManager().playSound(player, "click");
                render();
            }
            case 14 -> {
                // Open dedicated time adjustment screen
                context.getPlugin().getConfigManager().playSound(player, "click");
                context.setTransitioning(true);
                new WizardResetTimeGui(context).open();
                context.setTransitioning(false);
            }
            case 18 -> {
                // Back to Step 1
                context.getPlugin().getConfigManager().playSound(player, "click");
                context.openStep1();
            }
            case 26 -> {
                // Next to Step 3
                context.getPlugin().getConfigManager().playSound(player, "click");
                context.openStep3();
            }
        }
    }

    @Override
    public void handleClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (!context.isTransitioning() && !context.isSavedSuccessfully() && !context.isDraftAbandoned()) {
            context.saveAsDraft();
        }
    }
}
