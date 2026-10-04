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
 * 寶庫每次彈出物品數量按鈕式調整介面
 */
public class VaultRollAdjustGui extends CustomGuiHolder {

    private final VaultWizardContext context;

    public VaultRollAdjustGui(VaultWizardContext context) {
        this.context = context;
        this.inventory = Bukkit.createInventory(this, 27, context.getPlugin().getConfigManager().getComponent("gui.vault.rolls-adjust.title", "&8寶庫每次彈出數量設定"));
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

        int rolls = context.getTemplate().getRollCount();

        // Slot 13: 目前彈出數量圖示
        String rollsName = context.getPlugin().getConfigManager().getText("gui.vault.rolls-adjust.current-name", "&6目前每次彈出: &a%rolls% &f件", "%rolls%", String.valueOf(rolls));
        List<String> rollsLore = context.getPlugin().getConfigManager().getStringList(
                "gui.vault.rolls-adjust.current-lore",
                List.of(
                        "&7開鎖成功時，寶庫將連續彈出幾件物品",
                        "&7目前設定: &a%rolls% &7件",
                        "&7",
                        "&e請點擊兩側按鈕直接增減數量",
                        "&7(範圍限制: 1 ~ 64 件)"
                ),
                "%rolls%", String.valueOf(rolls)
        );
        inventory.setItem(13, createButton(Material.GOLD_INGOT, rollsName, rollsLore));

        // 減少數量按鈕
        inventory.setItem(10, createButton(Material.RED_CONCRETE, context.getPlugin().getConfigManager().getText("gui.vault.rolls-adjust.btn-minus-10-name", "&c-10 件"),
                context.getPlugin().getConfigManager().getStringList("gui.vault.rolls-adjust.btn-minus-10-lore", List.of("&7點擊減少 10 件"))));
        inventory.setItem(11, createButton(Material.RED_TERRACOTTA, context.getPlugin().getConfigManager().getText("gui.vault.rolls-adjust.btn-minus-5-name", "&c-5 件"),
                context.getPlugin().getConfigManager().getStringList("gui.vault.rolls-adjust.btn-minus-5-lore", List.of("&7點擊減少 5 件"))));
        inventory.setItem(12, createButton(Material.RED_STAINED_GLASS_PANE, context.getPlugin().getConfigManager().getText("gui.vault.rolls-adjust.btn-minus-1-name", "&c-1 件"),
                context.getPlugin().getConfigManager().getStringList("gui.vault.rolls-adjust.btn-minus-1-lore", List.of("&7點擊減少 1 件"))));

        // 增加數量按鈕
        inventory.setItem(14, createButton(Material.LIME_STAINED_GLASS_PANE, context.getPlugin().getConfigManager().getText("gui.vault.rolls-adjust.btn-plus-1-name", "&a+1 件"),
                context.getPlugin().getConfigManager().getStringList("gui.vault.rolls-adjust.btn-plus-1-lore", List.of("&7點擊增加 1 件"))));
        inventory.setItem(15, createButton(Material.LIME_TERRACOTTA, context.getPlugin().getConfigManager().getText("gui.vault.rolls-adjust.btn-plus-5-name", "&a+5 件"),
                context.getPlugin().getConfigManager().getStringList("gui.vault.rolls-adjust.btn-plus-5-lore", List.of("&7點擊增加 5 件"))));
        inventory.setItem(16, createButton(Material.LIME_CONCRETE, context.getPlugin().getConfigManager().getText("gui.vault.rolls-adjust.btn-plus-10-name", "&a+10 件"),
                context.getPlugin().getConfigManager().getStringList("gui.vault.rolls-adjust.btn-plus-10-lore", List.of("&7點擊增加 10 件"))));

        // Slot 22: 聊天室手動輸入
        inventory.setItem(22, createButton(Material.OAK_SIGN, context.getPlugin().getConfigManager().getText("gui.vault.rolls-adjust.chat-input-name", "&b聊天室直接輸入"),
                context.getPlugin().getConfigManager().getStringList("gui.vault.rolls-adjust.chat-input-lore", List.of("&7點擊後在聊天室直接輸入整數 (1 ~ 64)"))));

        // Slot 18: 返回步驟二
        inventory.setItem(18, createButton(Material.ARROW, context.getPlugin().getConfigManager().getText("gui.common.back-to-step2-name", "&e⬅ 返回 [步驟 2/4]"),
                context.getPlugin().getConfigManager().getStringList("gui.common.back-to-step2-lore", List.of("&7完成並返回"))));

        // Slot 26: 完成並返回
        inventory.setItem(26, createButton(Material.EMERALD, context.getPlugin().getConfigManager().getText("gui.vault.rolls-adjust.confirm-button-name", "&a✔ 完成並返回"),
                context.getPlugin().getConfigManager().getStringList("gui.vault.rolls-adjust.confirm-button-lore", List.of("&7返回 [步驟 2/4]"))));
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

        switch (slot) {
            case 10 -> adjustRolls(-10, player);
            case 11 -> adjustRolls(-5, player);
            case 12 -> adjustRolls(-1, player);
            case 14 -> adjustRolls(1, player);
            case 15 -> adjustRolls(5, player);
            case 16 -> adjustRolls(10, player);
            case 22 -> {
                // 聊天室輸入
                context.getPlugin().getConfigManager().playSound(player, "click");
                context.setTransitioning(true);
                context.getPlugin().getChatInputManager().requestInput(
                        player,
                        context.getPlugin().getConfigManager().getRawMessage("roll-prompt"),
                        input -> {
                            try {
                                int val = Integer.parseInt(input.trim());
                                if (val < 1 || val > 64) {
                                    context.getPlugin().getConfigManager().send(player, "roll-invalid");
                                    context.getPlugin().getConfigManager().playSound(player, "error");
                                } else {
                                    context.getTemplate().setRollCount(val);
                                    context.getPlugin().getConfigManager().playSound(player, "success");
                                }
                            } catch (NumberFormatException e) {
                                context.getPlugin().getConfigManager().send(player, "roll-invalid");
                                context.getPlugin().getConfigManager().playSound(player, "error");
                            }
                            render();
                            open();
                            context.setTransitioning(false);
                        },
                        () -> {
                            render();
                            open();
                            context.setTransitioning(false);
                        }
                );
            }
            case 18, 26 -> {
                context.getPlugin().getConfigManager().playSound(player, "click");
                context.setTransitioning(true);
                new VaultWizardStep3CooldownGui(context).open();
                context.setTransitioning(false);
            }
        }
    }

    @Override
    public void handleClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (!context.isTransitioning() && !context.isSavedSuccessfully() && !context.isDraftAbandoned()) {
            context.saveAsDraft();
        }
    }

    private void adjustRolls(int delta, Player player) {
        int current = context.getTemplate().getRollCount();
        int newVal = Math.min(64, Math.max(1, current + delta));
        if (newVal != current) {
            context.getTemplate().setRollCount(newVal);
            context.getPlugin().getConfigManager().playSound(player, "click");
            render();
        } else {
            context.getPlugin().getConfigManager().playSound(player, "error");
        }
    }
}
