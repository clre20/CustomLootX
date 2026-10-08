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

public class VaultTimeAdjustGui extends CustomGuiHolder {

    private final VaultWizardContext context;

    public VaultTimeAdjustGui(VaultWizardContext context) {
        this.context = context;
        this.inventory = Bukkit.createInventory(this, 27, context.getPlugin().getConfigManager().getComponent("gui.vault.time-adjust.title", "&8寶庫冷卻時間設定"));
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

        int seconds = context.getTemplate().getCooldownSeconds();

        // Slot 13: 目前時間時鐘
        String clockName = "&e目前冷卻時間: &a" + TextUtil.formatTimeSeconds(seconds);
        List<String> clockLore = List.of(
                "&7開鎖後將進入冷卻",
                "&7經過此時間將自動復原並允許再次開啟",
                "&7",
                "&7目前設定: &a" + TextUtil.formatTimeSeconds(seconds),
                "&7",
                "&e請點擊兩側按鈕直接增減時間",
                "&7(最低設定為 1 秒)"
        );
        inventory.setItem(13, createButton(Material.CLOCK, clockName, clockLore));

        // 減少時間按鈕
        inventory.setItem(10, createButton(Material.RED_CONCRETE, "&c-60s", List.of("&7點擊減少 60 秒")));
        inventory.setItem(11, createButton(Material.RED_TERRACOTTA, "&c-10s", List.of("&7點擊減少 10 秒")));
        inventory.setItem(12, createButton(Material.RED_STAINED_GLASS_PANE, "&c-1s", List.of("&7點擊減少 1 秒")));

        // 增加時間按鈕
        inventory.setItem(14, createButton(Material.LIME_STAINED_GLASS_PANE, "&a+1s", List.of("&7點擊增加 1 秒")));
        inventory.setItem(15, createButton(Material.LIME_TERRACOTTA, "&a+10s", List.of("&7點擊增加 10 秒")));
        inventory.setItem(16, createButton(Material.LIME_CONCRETE, "&a+60s", List.of("&7點擊增加 60 秒")));

        // Slot 22: 聊天室手動輸入
        inventory.setItem(22, createButton(Material.OAK_SIGN, "&b聊天室直接輸入秒數",
                List.of("&7點擊後在聊天室直接輸入數字 (秒)", "&7完成後自動轉換為 &e分與秒")));

        // Slot 18: 返回步驟三
        inventory.setItem(18, createButton(Material.ARROW, "&e⬅ 返回 [步驟 3/5]", List.of("&7完成並返回")));

        // Slot 26: 完成並返回
        inventory.setItem(26, createButton(Material.EMERALD, "&a✔ 完成並返回", List.of("&7返回 [步驟 3/5]")));
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
            case 10 -> adjustTime(-60, player);
            case 11 -> adjustTime(-10, player);
            case 12 -> adjustTime(-1, player);
            case 14 -> adjustTime(1, player);
            case 15 -> adjustTime(10, player);
            case 16 -> adjustTime(60, player);
            case 22 -> {
                // 聊天室輸入
                context.getPlugin().getConfigManager().playSound(player, "click");
                context.setTransitioning(true);
                context.getPlugin().getChatInputManager().requestInput(
                        player,
                        "&e請在聊天室輸入冷卻秒數 (例如 30、60、180，輸入 cancel 取消)：",
                        input -> {
                            try {
                                int val = Integer.parseInt(input.trim());
                                if (val < 1) {
                                    context.getPlugin().getConfigManager().playSound(player, "error");
                                    player.sendMessage(TextUtil.parse(context.getPlugin().getConfigManager().getPrefix() + "&c冷卻時間最低必須為 1 秒！"));
                                } else {
                                    context.getTemplate().setCooldownSeconds(val);
                                    context.getPlugin().getConfigManager().playSound(player, "success");
                                    player.sendMessage(TextUtil.parse(context.getPlugin().getConfigManager().getPrefix() + "&a已將冷卻時間設定為: &e" + TextUtil.formatTimeSeconds(val)));
                                }
                            } catch (NumberFormatException e) {
                                context.getPlugin().getConfigManager().playSound(player, "error");
                                player.sendMessage(TextUtil.parse(context.getPlugin().getConfigManager().getPrefix() + "&c請輸入有效的整數秒數！"));
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

    private void adjustTime(int delta, Player player) {
        int current = context.getTemplate().getCooldownSeconds();
        int newVal = Math.max(1, current + delta);
        if (newVal != current) {
            context.getTemplate().setCooldownSeconds(newVal);
            context.getPlugin().getConfigManager().playSound(player, "click");
            render();
        } else {
            context.getPlugin().getConfigManager().playSound(player, "error");
        }
    }
}
