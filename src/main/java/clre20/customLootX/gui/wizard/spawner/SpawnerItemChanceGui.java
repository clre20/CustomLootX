package clre20.customLootX.gui.wizard.spawner;

import clre20.customLootX.gui.CustomGuiHolder;
import clre20.customLootX.model.LootItem;
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
 * 試煉生怪磚獲勝獎勵單個物品機率設定介面
 */
public class SpawnerItemChanceGui extends CustomGuiHolder {

    private final SpawnerWizardContext context;
    private final LootItem targetItem;

    public SpawnerItemChanceGui(SpawnerWizardContext context, LootItem targetItem) {
        this.context = context;
        this.targetItem = targetItem;
        String itemName = targetItem.isAir() ? "&c落空" : TextUtil.getItemName(targetItem.getItem());
        this.inventory = Bukkit.createInventory(this, 27, context.getPlugin().getConfigManager().getComponent("gui.chance.title", "&8[%item%] 機率設定", "%item%", itemName));
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

        // Slot 13: 物品展示與目前機率
        ItemStack displayItem;
        if (targetItem.isAir() || targetItem.getItem() == null) {
            displayItem = new ItemStack(Material.STRUCTURE_VOID);
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                meta.displayName(TextUtil.parse(context.getPlugin().getConfigManager().getText("gui.step3.air-item-name", "&c[落空 / 無掉落]")));
                displayItem.setItemMeta(meta);
            }
        } else {
            displayItem = targetItem.getItem().clone();
        }

        ItemMeta meta = displayItem.getItemMeta();
        if (meta != null) {
            List<Component> currentLore = meta.lore();
            if (currentLore == null) currentLore = new ArrayList<>();
            List<String> chanceLore = context.getPlugin().getConfigManager().getStringList(
                    "gui.chance.target-lore",
                    List.of(
                            "&8------------------------",
                            "&e目前設定機率: &a%chance%",
                            "&7生怪磚掉落總機率: %total%",
                            "&8------------------------"
                    ),
                    "%chance%", String.format("%.2f%%", targetItem.getChance()),
                    "%total%", String.format("%.2f%%", context.getTemplate().getTotalChance())
            );
            for (String line : chanceLore) {
                currentLore.add(TextUtil.parse(line));
            }
            meta.lore(currentLore);
            displayItem.setItemMeta(meta);
        }
        inventory.setItem(13, displayItem);

        // 減少機率按鈕 (左側紅色：混泥土、混泥土、玻璃片、染料)
        inventory.setItem(9, createButton(Material.RED_CONCRETE, context.getPlugin().getConfigManager().getText("gui.chance.btn-minus-10-name", "&c-10.00%"), context.getPlugin().getConfigManager().getStringList("gui.chance.btn-minus-10-lore", List.of("&7點擊減少 10.00% 機率"))));
        inventory.setItem(10, createButton(Material.RED_CONCRETE, context.getPlugin().getConfigManager().getText("gui.chance.btn-minus-1-name", "&c-1.00%"), context.getPlugin().getConfigManager().getStringList("gui.chance.btn-minus-1-lore", List.of("&7點擊減少 1.00% 機率"))));
        inventory.setItem(11, createButton(Material.RED_STAINED_GLASS_PANE, context.getPlugin().getConfigManager().getText("gui.chance.btn-minus-010-name", "&c-0.10%"), context.getPlugin().getConfigManager().getStringList("gui.chance.btn-minus-010-lore", List.of("&7點擊減少 0.10% 機率"))));
        inventory.setItem(12, createButton(Material.RED_DYE, context.getPlugin().getConfigManager().getText("gui.chance.btn-minus-001-name", "&c-0.01%"), context.getPlugin().getConfigManager().getStringList("gui.chance.btn-minus-001-lore", List.of("&7點擊減少 0.01% 機率"))));

        // 增加機率按鈕 (右側綠色：染料、玻璃片、混泥土、混泥土)
        inventory.setItem(14, createButton(Material.LIME_DYE, context.getPlugin().getConfigManager().getText("gui.chance.btn-plus-001-name", "&a+0.01%"), context.getPlugin().getConfigManager().getStringList("gui.chance.btn-plus-001-lore", List.of("&7點擊增加 0.01% 機率"))));
        inventory.setItem(15, createButton(Material.LIME_STAINED_GLASS_PANE, context.getPlugin().getConfigManager().getText("gui.chance.btn-plus-010-name", "&a+0.10%"), context.getPlugin().getConfigManager().getStringList("gui.chance.btn-plus-010-lore", List.of("&7點擊增加 0.10% 機率"))));
        inventory.setItem(16, createButton(Material.LIME_CONCRETE, context.getPlugin().getConfigManager().getText("gui.chance.btn-plus-1-name", "&a+1.00%"), context.getPlugin().getConfigManager().getStringList("gui.chance.btn-plus-1-lore", List.of("&7點擊增加 1.00% 機率"))));
        inventory.setItem(17, createButton(Material.LIME_CONCRETE, context.getPlugin().getConfigManager().getText("gui.chance.btn-plus-10-name", "&a+10.00%"), context.getPlugin().getConfigManager().getStringList("gui.chance.btn-plus-10-lore", List.of("&7點擊增加 10.00% 機率"))));

        // Slot 18: 返回掉落池
        inventory.setItem(18, createButton(Material.ARROW, context.getPlugin().getConfigManager().getText("gui.common.return-to-pool-name", "&e⬅ 返回掉落池"), context.getPlugin().getConfigManager().getStringList("gui.common.return-to-pool-lore", List.of("&7完成並回到掉落池清單"))));

        // Slot 21: 聊天室手動輸入
        inventory.setItem(21, createButton(Material.PAPER, context.getPlugin().getConfigManager().getText("gui.chance.chat-input-name", "&b聊天室直接輸入數值"), context.getPlugin().getConfigManager().getStringList("gui.chance.chat-input-lore", List.of("&7點擊後直接在聊天室鍵入機率數字"))));

        // Slot 22: 自動補足至 100%
        double currentTotal = context.getTemplate().getTotalChance();
        double currentItemChance = targetItem.getChance();
        double otherChances = currentTotal - currentItemChance;
        double needed = 100.0 - otherChances;
        if (needed > 0.0001 && needed <= 100.0) {
            inventory.setItem(22, createButton(Material.GOLD_BLOCK, context.getPlugin().getConfigManager().getText("gui.chance.btn-fill-name", "&6自動補足至 100%"), context.getPlugin().getConfigManager().getStringList("gui.chance.btn-fill-lore", List.of("&7將此物品機率直接調整為 &e%need%", "&7使整體掉落物總和恰好等於 100.00%"), "%need%", String.format("%.2f%%", needed))));
        } else {
            inventory.setItem(22, createButton(Material.GRAY_DYE, context.getPlugin().getConfigManager().getText("gui.chance.btn-fill-disabled-name", "&8自動補足至 100% (無法補足)"), context.getPlugin().getConfigManager().getStringList("gui.chance.btn-fill-disabled-lore", List.of("&7其他物品機率總和已滿或超出 100%"))));
        }

        // Slot 26: 移除此物品
        inventory.setItem(26, createButton(Material.LAVA_BUCKET, context.getPlugin().getConfigManager().getText("gui.chance.btn-delete-name", "&c🗑 移除此掉落物"), context.getPlugin().getConfigManager().getStringList("gui.chance.btn-delete-lore", List.of("&7將此物品從生怪磚掉落池中移除"))));
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
            case 9 -> adjustChance(-10.0, player);
            case 10 -> adjustChance(-1.0, player);
            case 11 -> adjustChance(-0.10, player);
            case 12 -> adjustChance(-0.01, player);
            case 14 -> adjustChance(0.01, player);
            case 15 -> adjustChance(0.10, player);
            case 16 -> adjustChance(1.0, player);
            case 17 -> adjustChance(10.0, player);
            case 18 -> {
                // 返回掉落池
                context.getPlugin().getConfigManager().playSound(player, "click");
                new SpawnerWizardStep5LootGui(context, 1).open();
            }
            case 21 -> {
                // 聊天室輸入
                context.getPlugin().getConfigManager().playSound(player, "click");
                context.getPlugin().getChatInputManager().requestInput(
                        player,
                        context.getPlugin().getConfigManager().getRawMessage("input-prompt"),
                        input -> {
                            try {
                                double val = Double.parseDouble(input.trim().replace("%", ""));
                                if (val <= 0.0 || val > 100.0) {
                                    context.getPlugin().getConfigManager().send(player, "chance-invalid");
                                    context.getPlugin().getConfigManager().playSound(player, "error");
                                } else {
                                    targetItem.setChance(TextUtil.roundChance(val));
                                    context.getPlugin().getConfigManager().playSound(player, "success");
                                }
                            } catch (NumberFormatException e) {
                                context.getPlugin().getConfigManager().send(player, "chance-invalid");
                                context.getPlugin().getConfigManager().playSound(player, "error");
                            }
                            render();
                            open();
                        },
                        () -> {
                            render();
                            open();
                        }
                );
            }
            case 22 -> {
                // 自動補足
                double currentTotal = context.getTemplate().getTotalChance();
                double currentItemChance = targetItem.getChance();
                double otherChances = currentTotal - currentItemChance;
                double needed = 100.0 - otherChances;
                if (needed > 0.0001 && needed <= 100.0) {
                    targetItem.setChance(TextUtil.roundChance(needed));
                    context.getPlugin().getConfigManager().playSound(player, "success");
                    render();
                } else {
                    context.getPlugin().getConfigManager().playSound(player, "error");
                }
            }
            case 26 -> {
                // 刪除此物品
                context.getTemplate().getRewards().remove(targetItem);
                context.getPlugin().getConfigManager().playSound(player, "success");
                new SpawnerWizardStep5LootGui(context, 1).open();
            }
        }
    }

    private void adjustChance(double delta, Player player) {
        double current = targetItem.getChance();
        double newVal = TextUtil.roundChance(current + delta);
        if (newVal <= 0.0) {
            newVal = 0.01;
        }
        if (newVal > 100.0) {
            newVal = 100.0;
        }

        if (newVal != current) {
            targetItem.setChance(newVal);
            context.getPlugin().getConfigManager().playSound(player, "click");
            render();
        } else {
            context.getPlugin().getConfigManager().playSound(player, "error");
        }
    }
}
