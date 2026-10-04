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

public class WizardStep1Gui extends CustomGuiHolder {

    private final WizardContext context;

    public WizardStep1Gui(WizardContext context) {
        this.context = context;
        this.inventory = Bukkit.createInventory(this, 27, context.getPlugin().getConfigManager().getComponent("gui.step1.title", "&8[步驟 1/4] 基本設定與名稱"));
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
        String currentName = template.getName();
        boolean hasValidName = currentName != null && !currentName.trim().isEmpty() && !currentName.startsWith("draft_");

        // Slot 12: Block Type
        Material type = template.getType();
        String sandDesc = context.getPlugin().getConfigManager().getText("gui.step1.type-sand-desc", "&e可疑沙 ");
        String gravelDesc = context.getPlugin().getConfigManager().getText("gui.step1.type-gravel-desc", "&7可疑礫石");
        String sandShort = context.getPlugin().getConfigManager().getText("gui.step1.type-sand-short", "可疑沙");
        String gravelShort = context.getPlugin().getConfigManager().getText("gui.step1.type-gravel-short", "可疑礫石");

        String typeDesc = (type == Material.SUSPICIOUS_SAND) ? sandDesc : gravelDesc;
        String otherTypeDesc = (type == Material.SUSPICIOUS_SAND) ? gravelShort : sandShort;

        String typeBtnName = context.getPlugin().getConfigManager().getText("gui.step1.type-button-name", "&6方塊種類");
        List<String> typeLore = context.getPlugin().getConfigManager().getStringList(
                "gui.step1.type-button-lore",
                List.of(
                        "&7目前設定: %type%",
                        "&e點擊切換為: &f%other_type%"
                ),
                "%type%", typeDesc,
                "%other_type%", otherTypeDesc
        );
        inventory.setItem(12, createButton(type, typeBtnName, typeLore));

        // Slot 14: Template Name
        String nameBtnName = context.getPlugin().getConfigManager().getText("gui.step1.name-button-name", "&e配置名稱");
        String unsetStr = context.getPlugin().getConfigManager().getText("gui.step1.name-unset", "<未設定>");
        String clickToSetStr = context.getPlugin().getConfigManager().getText("gui.step1.name-click-to-set", "&c[點擊設定]");

        String pathStr = hasValidName ? currentName : unsetStr;
        String nameStatusStr = hasValidName ? "&a" + currentName : clickToSetStr;

        List<String> nameLore = context.getPlugin().getConfigManager().getStringList(
                "gui.step1.name-button-lore",
                List.of(
                        "&7檔案儲存為: &f/data/suspicious/%path%.yml",
                        "&7目前名稱: %current_name%",
                        "&7(遊戲內顯示名稱將自動與此名稱同步)",
                        "&7",
                        "&e點擊此處 &f在聊天室輸入名稱",
                        "&8(只能包含英文字母、數字與底線，如: T01)"
                ),
                "%path%", pathStr,
                "%current_name%", nameStatusStr
        );
        inventory.setItem(14, createButton(Material.NAME_TAG, nameBtnName, nameLore));

        // Slot 18: Cancel
        String cancelName = context.getPlugin().getConfigManager().getText("gui.common.cancel-name", "&c取消編輯");
        List<String> cancelLore = context.getPlugin().getConfigManager().getStringList("gui.common.cancel-lore", List.of("&7放棄本次編輯並關閉介面"));
        inventory.setItem(18, createButton(Material.BARRIER, cancelName, cancelLore));

        // Slot 26: Next Step
        if (hasValidName) {
            String nextReadyName = context.getPlugin().getConfigManager().getText("gui.step1.next-ready-name", "&a下一步 ➜ &f(重置模式)");
            List<String> nextReadyLore = context.getPlugin().getConfigManager().getStringList(
                    "gui.step1.next-ready-lore",
                    List.of(
                            "&7前往 [步驟 2/4] 設定自動重置模式",
                            "&a點擊前往下一步"
                    )
            );
            inventory.setItem(26, createButton(Material.LIME_CONCRETE, nextReadyName, nextReadyLore));
        } else {
            String nextNotReadyName = context.getPlugin().getConfigManager().getText("gui.step1.next-not-ready-name", "&c下一步 ➜ &7(請先設定名稱)");
            List<String> nextNotReadyLore = context.getPlugin().getConfigManager().getStringList(
                    "gui.step1.next-not-ready-lore",
                    List.of(
                            "&c必須先設定有效的配置名稱",
                            "&7請點擊中間的【配置名稱】進行輸入"
                    )
            );
            inventory.setItem(26, createButton(Material.RED_CONCRETE, nextNotReadyName, nextNotReadyLore));
        }
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
                // Switch block type
                if (template.getType() == Material.SUSPICIOUS_SAND) {
                    template.setType(Material.SUSPICIOUS_GRAVEL);
                } else {
                    template.setType(Material.SUSPICIOUS_SAND);
                }
                context.getPlugin().getConfigManager().playSound(player, "click");
                render();
            }
            case 14 -> {
                // Input name in chat
                context.getPlugin().getConfigManager().playSound(player, "click");
                context.setTransitioning(true);
                context.getPlugin().getChatInputManager().requestInput(
                        player,
                        context.getPlugin().getConfigManager().getRawMessage("name-prompt"),
                        input -> {
                            context.setTransitioning(false);
                            if (!input.matches("^[a-zA-Z0-9_-]+$")) {
                                context.getPlugin().getConfigManager().send(player, "name-invalid");
                                context.getPlugin().getConfigManager().playSound(player, "error");
                            } else {
                                template.setName(input);
                                template.setDisplayName(input);
                                context.getPlugin().getConfigManager().playSound(player, "success");
                            }
                            render();
                            open();
                        },
                        () -> {
                            context.setTransitioning(false);
                            render();
                            open();
                        }
                );
            }
            case 18 -> {
                // Cancel / Close -> save draft
                context.getPlugin().getConfigManager().playSound(player, "click");
                player.closeInventory();
            }
            case 26 -> {
                // Next Step
                String currentName = template.getName();
                boolean hasValidName = currentName != null && !currentName.trim().isEmpty() && !currentName.startsWith("draft_");
                if (!hasValidName) {
                    context.getPlugin().getConfigManager().send(player, "name-invalid");
                    context.getPlugin().getConfigManager().playSound(player, "error");
                    return;
                }
                context.getPlugin().getConfigManager().playSound(player, "click");
                context.openStep2();
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
