package clre20.customLootX.gui.wizard.spawner;

import clre20.customLootX.gui.CustomGuiHolder;
import clre20.customLootX.model.SpawnerMobEntry;
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
 * 試煉生怪磚生成怪物單個項目機率設定介面
 */
public class SpawnerMobChanceGui extends CustomGuiHolder {

    private final SpawnerWizardContext context;
    private final SpawnerMobEntry targetMob;

    public SpawnerMobChanceGui(SpawnerWizardContext context, SpawnerMobEntry targetMob) {
        this.context = context;
        this.targetMob = targetMob;
        String mobName = targetMob.getDisplayName();
        this.inventory = Bukkit.createInventory(
                this,
                27,
                context.getPlugin().getConfigManager().getComponent("gui.spawner.mob-chance.title", "&8[%mob%&8] 機率設定", "%mob%", mobName)
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

        // Slot 13: 怪物展示與目前機率
        ItemStack displayItem = new ItemStack(targetMob.getIconMaterial());
        ItemMeta meta = displayItem.getItemMeta();
        if (meta != null) {
            String title = (targetMob.isMythic() ? "&d[Mythic] &f" : "&e[原版] &f") + targetMob.getDisplayName();
            meta.displayName(TextUtil.parse(title));
            List<Component> currentLore = new ArrayList<>();
            List<String> chanceLore = List.of(
                    "&8------------------------",
                    "&7內部代號: &f" + targetMob.getMobId(),
                    "&7類型: " + (targetMob.isMythic() ? "&dMythicMob 自訂怪物" : "&a原版生物"),
                    "&e目前設定機率: &a" + String.format("%.2f%%", targetMob.getChance()),
                    "&7怪物池總機率: &f" + String.format("%.2f%%", context.getTemplate().getTotalMobChance()),
                    "&8------------------------"
            );
            for (String line : chanceLore) {
                currentLore.add(TextUtil.parse(line));
            }
            meta.lore(currentLore);
            displayItem.setItemMeta(meta);
        }
        inventory.setItem(13, displayItem);

        // 減少機率按鈕 (左側紅色)
        inventory.setItem(9, createButton(Material.RED_CONCRETE, context.getPlugin().getConfigManager().getText("gui.chance.btn-minus-10-name", "&c-10.00%"), context.getPlugin().getConfigManager().getStringList("gui.chance.btn-minus-10-lore", List.of("&7點擊減少 10.00% 機率"))));
        inventory.setItem(10, createButton(Material.RED_CONCRETE, context.getPlugin().getConfigManager().getText("gui.chance.btn-minus-1-name", "&c-1.00%"), context.getPlugin().getConfigManager().getStringList("gui.chance.btn-minus-1-lore", List.of("&7點擊減少 1.00% 機率"))));
        inventory.setItem(11, createButton(Material.RED_STAINED_GLASS_PANE, context.getPlugin().getConfigManager().getText("gui.chance.btn-minus-010-name", "&c-0.10%"), context.getPlugin().getConfigManager().getStringList("gui.chance.btn-minus-010-lore", List.of("&7點擊減少 0.10% 機率"))));
        inventory.setItem(12, createButton(Material.RED_DYE, context.getPlugin().getConfigManager().getText("gui.chance.btn-minus-001-name", "&c-0.01%"), context.getPlugin().getConfigManager().getStringList("gui.chance.btn-minus-001-lore", List.of("&7點擊減少 0.01% 機率"))));

        // 增加機率按鈕 (右側綠色)
        inventory.setItem(14, createButton(Material.LIME_DYE, context.getPlugin().getConfigManager().getText("gui.chance.btn-plus-001-name", "&a+0.01%"), context.getPlugin().getConfigManager().getStringList("gui.chance.btn-plus-001-lore", List.of("&7點擊增加 0.01% 機率"))));
        inventory.setItem(15, createButton(Material.LIME_STAINED_GLASS_PANE, context.getPlugin().getConfigManager().getText("gui.chance.btn-plus-010-name", "&a+0.10%"), context.getPlugin().getConfigManager().getStringList("gui.chance.btn-plus-010-lore", List.of("&7點擊增加 0.10% 機率"))));
        inventory.setItem(16, createButton(Material.LIME_CONCRETE, context.getPlugin().getConfigManager().getText("gui.chance.btn-plus-1-name", "&a+1.00%"), context.getPlugin().getConfigManager().getStringList("gui.chance.btn-plus-1-lore", List.of("&7點擊增加 1.00% 機率"))));
        inventory.setItem(17, createButton(Material.LIME_CONCRETE, context.getPlugin().getConfigManager().getText("gui.chance.btn-plus-10-name", "&a+10.00%"), context.getPlugin().getConfigManager().getStringList("gui.chance.btn-plus-10-lore", List.of("&7點擊增加 10.00% 機率"))));

        // Slot 18: 返回怪物池
        inventory.setItem(18, createButton(Material.ARROW, "&e⬅ 返回怪物池", List.of("&7完成並回到怪物池清單")));

        // Slot 21: 聊天室輸入
        inventory.setItem(21, createButton(Material.PAPER, context.getPlugin().getConfigManager().getText("gui.chance.chat-input-name", "&b聊天室直接輸入數值"), context.getPlugin().getConfigManager().getStringList("gui.chance.chat-input-lore", List.of("&7點擊後直接在聊天室鍵入機率數字"))));

        // Slot 22: 自動補足至 100%
        double currentTotal = context.getTemplate().getTotalMobChance();
        double currentMobChance = targetMob.getChance();
        double otherChances = currentTotal - currentMobChance;
        double needed = 100.0 - otherChances;
        if (needed > 0.0001 && needed <= 100.0) {
            inventory.setItem(22, createButton(Material.GOLD_BLOCK, "&6自動補足至 100%", List.of("&7將此怪物機率直接調整為 &e" + String.format("%.2f%%", needed), "&7使整體怪物池總和恰好等於 100.00%")));
        } else {
            inventory.setItem(22, createButton(Material.GRAY_DYE, "&8自動補足至 100% (無法補足)", List.of("&7其他怪物機率總和已滿或超出 100%")));
        }

        // Slot 26: 移除此怪物
        if (context.getTemplate().getMobPool().size() > 1) {
            inventory.setItem(26, createButton(Material.LAVA_BUCKET, "&c🗑 從生怪磚池移除此怪物", List.of("&7將此項目從怪物生成池中刪除")));
        } else {
            inventory.setItem(26, createButton(Material.BARRIER, "&8無法移除唯一怪物", List.of("&7怪物池中必須至少保留一隻怪物")));
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
                // 返回隨機池機率
                context.getPlugin().getConfigManager().playSound(player, "click");
                context.setTransitioning(true);
                new SpawnerWizardStep4RandomChanceGui(context).open();
                context.setTransitioning(false);
            }
            case 21 -> {
                // 聊天室輸入
                context.getPlugin().getConfigManager().playSound(player, "click");
                context.setTransitioning(true);
                context.getPlugin().getChatInputManager().requestInput(
                        player,
                        context.getPlugin().getConfigManager().getRawMessage("input-prompt"),
                        input -> {
                            try {
                                double val = Double.parseDouble(input.trim().replace("%", ""));
                                if (val <= 0.0 || val > 100.0) {
                                    context.getPlugin().getConfigManager().send(player, "input-invalid");
                                    context.getPlugin().getConfigManager().playSound(player, "error");
                                } else {
                                    targetMob.setChance(TextUtil.roundChance(val));
                                    context.getPlugin().getConfigManager().playSound(player, "success");
                                }
                            } catch (NumberFormatException e) {
                                context.getPlugin().getConfigManager().send(player, "input-invalid");
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
            case 22 -> {
                // 自動補足
                double currentTotal = context.getTemplate().getTotalMobChance();
                double currentMobChance = targetMob.getChance();
                double otherChances = currentTotal - currentMobChance;
                double needed = 100.0 - otherChances;
                if (needed > 0.0001 && needed <= 100.0) {
                    targetMob.setChance(TextUtil.roundChance(needed));
                    context.getPlugin().getConfigManager().playSound(player, "success");
                    render();
                } else {
                    context.getPlugin().getConfigManager().playSound(player, "error");
                }
            }
            case 26 -> {
                // 刪除此怪物
                if (context.getTemplate().getMobPool().size() > 1) {
                    context.getTemplate().getMobPool().remove(targetMob);
                    context.getPlugin().getConfigManager().playSound(player, "success");
                    context.setTransitioning(true);
                    new SpawnerWizardStep4RandomChanceGui(context).open();
                    context.setTransitioning(false);
                } else {
                    context.getPlugin().getConfigManager().playSound(player, "error");
                }
            }
        }
    }

    @Override
    public void handleClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (!context.isTransitioning() && !context.isSavedSuccessfully() && !context.isDraftAbandoned()) {
            context.saveAsDraft();
        }
    }

    private void adjustChance(double delta, Player player) {
        double current = targetMob.getChance();
        double newVal = TextUtil.roundChance(current + delta);
        if (newVal <= 0.0) {
            newVal = 0.01;
        }
        if (newVal > 100.0) {
            newVal = 100.0;
        }

        if (newVal != current) {
            targetMob.setChance(newVal);
            context.getPlugin().getConfigManager().playSound(player, "click");
            render();
        } else {
            context.getPlugin().getConfigManager().playSound(player, "error");
        }
    }
}
