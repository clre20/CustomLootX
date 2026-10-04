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
 * 試煉寶庫鑰匙單行說明編輯與刪除獨立頁面
 * 避免將修改與刪除擠在左右鍵，提供清晰的操作選項
 */
public class VaultKeyLoreLineActionGui extends CustomGuiHolder {

    private final VaultKeyLoreGui parentGui;
    private final int lineIndex;

    public VaultKeyLoreLineActionGui(VaultKeyLoreGui parentGui, int lineIndex) {
        this.parentGui = parentGui;
        this.lineIndex = lineIndex;
        int lineNum = lineIndex + 1;
        this.inventory = Bukkit.createInventory(
                this,
                27,
                parentGui.getSetupGui().getContext().getPlugin().getConfigManager().getComponent(
                        "gui.vault.key-lore-line.title",
                        "&8編輯第 " + lineNum + " 行說明"
                )
        );
        render();
    }

    public void open() {
        render();
        parentGui.getSetupGui().getContext().getPlayer().openInventory(this.inventory);
    }

    private void render() {
        String fillerName = parentGui.getSetupGui().getContext().getPlugin().getConfigManager().getText("gui.common.filler-name", " ");
        ItemStack filler = createButton(Material.GRAY_STAINED_GLASS_PANE, fillerName, null);
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        List<String> loreList = parentGui.getSetupGui().getCustomKeyLore();
        String currentContent = (lineIndex >= 0 && lineIndex < loreList.size()) ? loreList.get(lineIndex) : "";
        int lineNum = lineIndex + 1;

        // Slot 4: 目前行內容展示
        String displayTitle = "&e【第 " + lineNum + " 行說明】";
        List<String> displayLore = List.of(
                "&7代碼內容: &f" + currentContent.replace("§", "&"),
                "&7預覽外觀: " + currentContent,
                "&7",
                "&7請在下方選擇要進行的操作："
        );
        inventory.setItem(4, createButton(Material.PAPER, displayTitle, displayLore));

        // Slot 11: ✏ 修改此行內容
        String editTitle = "&a✏ 修改此行內容";
        List<String> editLore = List.of(
                "&7點擊後在聊天室重新輸入這行文字",
                "&8(預設白色，支援彩色代碼如 &4紅色說明)"
        );
        inventory.setItem(11, createButton(Material.NAME_TAG, editTitle, editLore));

        // Slot 13: 🗑 刪除此行說明
        String deleteTitle = "&c🗑 刪除此行說明";
        List<String> deleteLore = List.of(
                "&7從鑰匙說明清單中刪除第 " + lineNum + " 行",
                "&c點擊立即刪除"
        );
        inventory.setItem(13, createButton(Material.BARRIER, deleteTitle, deleteLore));

        // Slot 15: ⬅ 返回說明列表
        String backTitle = "&e⬅ 返回說明列表";
        List<String> backLore = List.of("&7放棄操作並返回行清單");
        inventory.setItem(15, createButton(Material.ARROW, backTitle, backLore));
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

        VaultWizardContext context = parentGui.getSetupGui().getContext();
        List<String> loreList = parentGui.getSetupGui().getCustomKeyLore();

        if (slot == 11) {
            // ✏ 修改此行內容
            context.getPlugin().getConfigManager().playSound(player, "click");
            int lineNum = lineIndex + 1;
            String prompt = "&e請在聊天室輸入【第 " + lineNum + " 行】的新內容 (支援彩色代碼 &)，輸入 &ccancel &e取消：";
            context.setTransitioning(true);
            context.getPlugin().getChatInputManager().requestInput(
                    player,
                    prompt,
                    input -> {
                        if (lineIndex >= 0 && lineIndex < loreList.size()) {
                            String line = TextUtil.ensureDefaultWhite(input.trim());
                            loreList.set(lineIndex, line);
                            context.getPlugin().getConfigManager().playSound(player, "success");
                        }
                        parentGui.open();
                        context.setTransitioning(false);
                    },
                    () -> {
                        parentGui.open();
                        context.setTransitioning(false);
                    }
            );
        } else if (slot == 13) {
            // 🗑 刪除此行說明
            if (lineIndex >= 0 && lineIndex < loreList.size()) {
                loreList.remove(lineIndex);
                context.getPlugin().getConfigManager().playSound(player, "click");
            }
            context.setTransitioning(true);
            parentGui.open();
            context.setTransitioning(false);
        } else if (slot == 15) {
            // ⬅ 返回說明列表
            context.getPlugin().getConfigManager().playSound(player, "click");
            context.setTransitioning(true);
            parentGui.open();
            context.setTransitioning(false);
        }
    }

    @Override
    public void handleClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        VaultWizardContext context = parentGui.getSetupGui().getContext();
        if (!context.isTransitioning() && !context.isSavedSuccessfully() && !context.isDraftAbandoned()) {
            context.saveAsDraft();
        }
    }
}
