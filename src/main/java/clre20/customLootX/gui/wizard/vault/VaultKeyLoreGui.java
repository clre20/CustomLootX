package clre20.customLootX.gui.wizard.vault;

import clre20.customLootX.gui.CustomGuiHolder;
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
 * 試煉寶庫鑰匙多行說明 (Lore) 管理介面
 * 支援查看目前行數、左鍵點擊修改指定行、右鍵點擊刪除指定行、新增行、清空全部
 */
public class VaultKeyLoreGui extends CustomGuiHolder {

    private final VaultKeySetupGui setupGui;
    private final List<String> loreList;

    public VaultKeyLoreGui(VaultKeySetupGui setupGui) {
        this.setupGui = setupGui;
        this.loreList = setupGui.getCustomKeyLore();
        this.inventory = Bukkit.createInventory(this, 36, setupGui.getContext().getPlugin().getConfigManager().getComponent("gui.vault.key-lore.title", "&8鑰匙說明 (Lore) 管理"));
        render();
    }

    public VaultKeySetupGui getSetupGui() {
        return setupGui;
    }

    public void open() {
        render();
        setupGui.getContext().getPlayer().openInventory(this.inventory);
    }

    private void render() {
        String fillerName = setupGui.getContext().getPlugin().getConfigManager().getText("gui.common.filler-name", " ");
        ItemStack filler = createButton(Material.GRAY_STAINED_GLASS_PANE, fillerName, null);
        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, filler);
        }

        // Slot 4: 說明與操作指南
        String guideTitle = "&e【鑰匙說明管理指南】";
        List<String> guideLore = List.of(
                "&7目前行數: &a" + loreList.size() + " &7行",
                "&7",
                "&e▪ 點擊任一行紙張: &f開啟該行的操作頁面 (修改/刪除)",
                "&a▪ 點擊【+】書本: &f在聊天室新增一行說明"
        );
        inventory.setItem(4, createButton(Material.BOOK, guideTitle, guideLore));

        // 放置每一行說明 (Slots 9 ~ 26)
        int maxLines = 17;
        for (int i = 0; i < loreList.size() && i < maxLines; i++) {
            int slot = 9 + i;
            String text = loreList.get(i);
            int lineNum = i + 1;

            String paperName = "&e第 " + lineNum + " 行";
            List<String> paperLore = List.of(
                    "&7代碼內容: &f" + text.replace("§", "&"),
                    "&7預覽外觀: " + text,
                    "&7",
                    "&e點擊開啟操作選單 (修改 / 刪除)"
            );
            inventory.setItem(slot, createButton(Material.PAPER, paperName, paperLore));
        }

        // 若尚未達到行數上限，顯示新增按鈕
        if (loreList.size() < maxLines) {
            int addSlot = 9 + loreList.size();
            String addName = "&a[+ 新增一行說明]";
            List<String> addLore = List.of(
                    "&7點擊後在聊天室輸入新增的說明文字",
                    "&8(預設白色，支援彩色代碼如 &4紅色說明)"
            );
            inventory.setItem(addSlot, createButton(Material.WRITABLE_BOOK, addName, addLore));
        }

        // Slot 27: 返回鑰匙設定
        String backName = setupGui.getContext().getPlugin().getConfigManager().getText("gui.vault.key-lore.back-name", "&e⬅ 返回鑰匙設定");
        List<String> backLore = List.of("&7保留目前的說明並返回");
        inventory.setItem(27, createButton(Material.ARROW, backName, backLore));

        // Slot 31: 清空所有說明
        if (!loreList.isEmpty()) {
            String clearName = "&c✖ 清空所有說明";
            List<String> clearLore = List.of(
                    "&7刪除目前所有已設定的說明文字",
                    "&c點擊立即清空"
            );
            inventory.setItem(31, createButton(Material.BARRIER, clearName, clearLore));
        }

        // Slot 35: 完成並返回
        String confirmName = "&a✔ 完成設定";
        List<String> confirmLore = List.of("&7保存並返回鑰匙製作畫面");
        inventory.setItem(35, createButton(Material.EMERALD, confirmName, confirmLore));
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

        VaultWizardContext context = setupGui.getContext();
        if (slot >= 9 && slot <= 26) {
            int index = slot - 9;
            if (index < loreList.size()) {
                // 點擊已有行：開啟獨立的修改/刪除操作介面
                context.getPlugin().getConfigManager().playSound(player, "click");
                context.setTransitioning(true);
                new VaultKeyLoreLineActionGui(this, index).open();
                context.setTransitioning(false);
            } else if (index == loreList.size() && loreList.size() < 17) {
                // 點擊新增行按鈕
                context.getPlugin().getConfigManager().playSound(player, "click");
                String prompt = context.getPlugin().getConfigManager().getRawMessage("key-lore-prompt");
                context.setTransitioning(true);
                context.getPlugin().getChatInputManager().requestInput(
                        player,
                        prompt,
                        input -> {
                            String line = TextUtil.ensureDefaultWhite(input.trim());
                            loreList.add(line);
                            context.getPlugin().getConfigManager().playSound(player, "success");
                            open();
                            context.setTransitioning(false);
                        },
                        () -> {
                            open();
                            context.setTransitioning(false);
                        }
                );
            }
        } else if (slot == 31 && !loreList.isEmpty()) {
            // 清空所有說明
            loreList.clear();
            context.getPlugin().getConfigManager().playSound(player, "click");
            render();
        } else if (slot == 27 || slot == 35) {
            // 返回鑰匙設定
            context.getPlugin().getConfigManager().playSound(player, "click");
            context.setTransitioning(true);
            setupGui.open();
            context.setTransitioning(false);
        }
    }

    @Override
    public void handleClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        VaultWizardContext context = setupGui.getContext();
        if (!context.isTransitioning() && !context.isSavedSuccessfully() && !context.isDraftAbandoned()) {
            context.saveAsDraft();
        }
    }
}
