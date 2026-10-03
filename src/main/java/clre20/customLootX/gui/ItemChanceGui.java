package clre20.customLootX.gui;

import clre20.customLootX.CustomLootX;
import clre20.customLootX.model.LootItem;
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

public class ItemChanceGui extends CustomGuiHolder {

    private final CustomLootX plugin;
    private final Player player;
    private final LootTemplate template;
    private final int itemIndex;
    private final Runnable onReturn;

    public ItemChanceGui(CustomLootX plugin, Player player, LootTemplate template, int itemIndex, Runnable onReturn) {
        this.plugin = plugin;
        this.player = player;
        this.template = template;
        this.itemIndex = itemIndex;
        this.onReturn = onReturn;

        String defaultItemName = plugin.getConfigManager().getText("gui.chance.default-item-name", "物品");
        String airItemName = plugin.getConfigManager().getText("gui.chance.air-item-name", "落空");

        String itemName = defaultItemName;
        if (itemIndex >= 0 && itemIndex < template.getItems().size()) {
            LootItem lootItem = template.getItems().get(itemIndex);
            if (lootItem.isAir()) {
                itemName = airItemName;
            } else if (lootItem.getItem() != null) {
                ItemStack is = lootItem.getItem();
                if (is.hasItemMeta() && is.getItemMeta().hasDisplayName()) {
                    itemName = TextUtil.toPlainText(is.getItemMeta().displayName());
                } else {
                    itemName = is.getType().name();
                }
            }
        }

        this.inventory = Bukkit.createInventory(this, 27, plugin.getConfigManager().getComponent("gui.chance.title-format", "&8[%item%]機率設定", "%item%", itemName));
        render();
    }

    public void open() {
        player.openInventory(this.inventory);
    }

    private void render() {
        String fillerName = plugin.getConfigManager().getText("gui.common.filler-name", " ");
        ItemStack filler = createButton(Material.GRAY_STAINED_GLASS_PANE, fillerName, null);
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        if (itemIndex < 0 || itemIndex >= template.getItems().size()) {
            if (onReturn != null) onReturn.run();
            return;
        }

        LootItem lootItem = template.getItems().get(itemIndex);
        double currentChance = lootItem.getChance();
        double totalChance = template.getTotalChance();
        double diff = 100.00 - totalChance;

        // Slot 13: Target item preview
        String airDisplayName = plugin.getConfigManager().getText("gui.chance.air-display-name", "&c[落空 / 無掉落]");
        ItemStack displayItem;
        if (lootItem.isAir()) {
            displayItem = new ItemStack(Material.BARRIER);
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                meta.displayName(TextUtil.parse(airDisplayName));
                displayItem.setItemMeta(meta);
            }
        } else {
            displayItem = lootItem.getItem();
        }

        ItemMeta meta = displayItem.getItemMeta();
        if (meta != null) {
            List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            String totalStr = (template.isValidTotal() ? "&a" : "&c") + TextUtil.formatPercent(totalChance);
            String diffStr = Math.abs(diff) < 0.001 ? "&a0.00%" : (diff > 0 ? "&e+" : "&c") + TextUtil.formatPercent(diff);

            List<Component> extra = plugin.getConfigManager().getComponentList(
                    "gui.chance.preview-lore",
                    List.of(
                            "",
                            "&8------------------------",
                            "&e此物品設定機率: &a%chance%",
                            "&7所有物品總和: %total%",
                            "&7距離 100% 尚差: %diff%",
                            "&8------------------------"
                    ),
                    "%chance%", TextUtil.formatPercent(currentChance),
                    "%total%", totalStr,
                    "%diff%", diffStr
            );
            lore.addAll(extra);
            meta.lore(lore);
            displayItem.setItemMeta(meta);
        }
        inventory.setItem(13, displayItem);

        // Decrease buttons (Left side)
        String m10Name = plugin.getConfigManager().getText("gui.chance.btn-minus-10-name", "&c-10.00%");
        List<String> m10Lore = plugin.getConfigManager().getStringList("gui.chance.btn-minus-10-lore", List.of("&7點擊將此物品機率減少 10%"));
        inventory.setItem(9, createButton(Material.RED_CONCRETE, m10Name, m10Lore));

        String m1Name = plugin.getConfigManager().getText("gui.chance.btn-minus-1-name", "&c-1.00%");
        List<String> m1Lore = plugin.getConfigManager().getStringList("gui.chance.btn-minus-1-lore", List.of("&7點擊將此物品機率減少 1%"));
        inventory.setItem(10, createButton(Material.RED_CONCRETE, m1Name, m1Lore));

        String m01Name = plugin.getConfigManager().getText("gui.chance.btn-minus-01-name", "&c-0.10%");
        List<String> m01Lore = plugin.getConfigManager().getStringList("gui.chance.btn-minus-01-lore", List.of("&7點擊將此物品機率減少 0.1%"));
        inventory.setItem(11, createButton(Material.RED_STAINED_GLASS_PANE, m01Name, m01Lore));

        String m001Name = plugin.getConfigManager().getText("gui.chance.btn-minus-001-name", "&c-0.01%");
        List<String> m001Lore = plugin.getConfigManager().getStringList("gui.chance.btn-minus-001-lore", List.of("&7點擊將此物品機率減少 0.01%"));
        inventory.setItem(12, createButton(Material.RED_DYE, m001Name, m001Lore));

        // Increase buttons (Right side)
        String p001Name = plugin.getConfigManager().getText("gui.chance.btn-plus-001-name", "&a+0.01%");
        List<String> p001Lore = plugin.getConfigManager().getStringList("gui.chance.btn-plus-001-lore", List.of("&7點擊將此物品機率增加 0.01%"));
        inventory.setItem(14, createButton(Material.LIME_DYE, p001Name, p001Lore));

        String p01Name = plugin.getConfigManager().getText("gui.chance.btn-plus-01-name", "&a+0.10%");
        List<String> p01Lore = plugin.getConfigManager().getStringList("gui.chance.btn-plus-01-lore", List.of("&7點擊將此物品機率增加 0.1%"));
        inventory.setItem(15, createButton(Material.LIME_STAINED_GLASS_PANE, p01Name, p01Lore));

        String p1Name = plugin.getConfigManager().getText("gui.chance.btn-plus-1-name", "&a+1.00%");
        List<String> p1Lore = plugin.getConfigManager().getStringList("gui.chance.btn-plus-1-lore", List.of("&7點擊將此物品機率增加 1%"));
        inventory.setItem(16, createButton(Material.LIME_CONCRETE, p1Name, p1Lore));

        String p10Name = plugin.getConfigManager().getText("gui.chance.btn-plus-10-name", "&a+10.00%");
        List<String> p10Lore = plugin.getConfigManager().getStringList("gui.chance.btn-plus-10-lore", List.of("&7點擊將此物品機率增加 10%"));
        inventory.setItem(17, createButton(Material.LIME_CONCRETE, p10Name, p10Lore));

        // Functional buttons
        String returnName = plugin.getConfigManager().getText("gui.common.return-to-pool-name", "&e⬅ 返回掉落池");
        List<String> returnLore = plugin.getConfigManager().getStringList("gui.common.return-to-pool-lore", List.of("&7回到掉落物箱子總覽"));
        inventory.setItem(18, createButton(Material.ARROW, returnName, returnLore));

        String chatName = plugin.getConfigManager().getText("gui.chance.chat-input-name", "&b聊天室精確輸入");
        List<String> chatLore = plugin.getConfigManager().getStringList(
                "gui.chance.chat-input-lore",
                List.of(
                        "&7點擊後在聊天室直接輸入數字",
                        "&7支援範圍: &e0.01 ~ 100.00",
                        "&7例如輸入: &f15.25"
                )
        );
        inventory.setItem(21, createButton(Material.PAPER, chatName, chatLore));

        String fillName = plugin.getConfigManager().getText("gui.chance.fill-100-name", "&6一鍵補齊至 100%");
        String diffPrefixStr = (diff > 0 ? "+" : "") + TextUtil.formatPercent(diff);
        List<String> fillLore = plugin.getConfigManager().getStringList(
                "gui.chance.fill-100-lore",
                List.of(
                        "&7自動將剩餘的差額加至此物品",
                        "&7目前尚需補齊: &e%diff%"
                ),
                "%diff%", diffPrefixStr
        );
        inventory.setItem(22, createButton(Material.GOLD_INGOT, fillName, fillLore));

        String removeName = plugin.getConfigManager().getText("gui.chance.remove-name", "&c從獎池移除此物品");
        List<String> removeLore = plugin.getConfigManager().getStringList("gui.chance.remove-lore", List.of("&7將此項目從自訂可疑方塊中刪除"));
        inventory.setItem(26, createButton(Material.LAVA_BUCKET, removeName, removeLore));
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

        if (slot < 0 || slot >= 27) {
            return;
        }

        if (itemIndex < 0 || itemIndex >= template.getItems().size()) {
            if (onReturn != null) onReturn.run();
            return;
        }

        LootItem item = template.getItems().get(itemIndex);

        switch (slot) {
            case 9 -> adjustChance(item, -10.00);
            case 10 -> adjustChance(item, -1.00);
            case 11 -> adjustChance(item, -0.10);
            case 12 -> adjustChance(item, -0.01);

            case 14 -> adjustChance(item, 0.01);
            case 15 -> adjustChance(item, 0.10);
            case 16 -> adjustChance(item, 1.00);
            case 17 -> adjustChance(item, 10.00);

            case 18 -> {
                // Return to parent
                plugin.getConfigManager().playSound(player, "click");
                if (onReturn != null) onReturn.run();
            }

            case 21 -> {
                // Exact chat input
                plugin.getConfigManager().playSound(player, "click");
                plugin.getChatInputManager().requestInput(
                        player,
                        plugin.getConfigManager().getRawMessage("input-prompt"),
                        input -> {
                            try {
                                double parsed = Double.parseDouble(input);
                                if (parsed < 0.01 || parsed > 100.00) {
                                    plugin.getConfigManager().send(player, "input-invalid");
                                } else {
                                    item.setChance(parsed);
                                    plugin.getConfigManager().send(player, "input-success", "%chance%", TextUtil.formatPercent(TextUtil.roundChance(parsed)));
                                    plugin.getConfigManager().playSound(player, "success");
                                }
                            } catch (NumberFormatException e) {
                                plugin.getConfigManager().send(player, "input-invalid");
                                plugin.getConfigManager().playSound(player, "error");
                            }
                            this.render();
                            this.open();
                        },
                        () -> {
                            this.render();
                            this.open();
                        }
                );
            }

            case 22 -> {
                // Fill remainder to 100%
                double otherSum = 0.0;
                for (int i = 0; i < template.getItems().size(); i++) {
                    if (i != itemIndex) {
                        otherSum += template.getItems().get(i).getChance();
                    }
                }
                double needed = TextUtil.roundChance(100.00 - otherSum);
                if (needed >= 0.01 && needed <= 100.00) {
                    item.setChance(needed);
                    plugin.getConfigManager().playSound(player, "success");
                } else if (needed < 0.01) {
                    item.setChance(0.01);
                    plugin.getConfigManager().playSound(player, "error");
                }
                render();
            }

            case 26 -> {
                // Delete item
                template.removeItem(itemIndex);
                plugin.getConfigManager().playSound(player, "click");
                if (onReturn != null) onReturn.run();
            }
        }
    }

    private void adjustChance(LootItem item, double delta) {
        double newChance = TextUtil.roundChance(item.getChance() + delta);
        if (newChance < 0.0) newChance = 0.0;
        if (newChance > 100.0) newChance = 100.0;
        item.setChance(newChance);
        plugin.getConfigManager().playSound(player, "click");
        render();
    }
}
