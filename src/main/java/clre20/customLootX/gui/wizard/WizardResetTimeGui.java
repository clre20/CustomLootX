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

public class WizardResetTimeGui extends CustomGuiHolder {

    private final WizardContext context;

    public WizardResetTimeGui(WizardContext context) {
        this.context = context;
        this.inventory = Bukkit.createInventory(this, 27, context.getPlugin().getConfigManager().getComponent("gui.step2-time.title", "&8重置間隔時間設定"));
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
        int seconds = template.getResetSeconds();

        // Slot 13: Clock showing current time formatted
        String clockName = context.getPlugin().getConfigManager().getText("gui.step2-time.clock-name", "&e目前重置時間: &a%time%",
                "%time%", TextUtil.formatTimeSeconds(seconds),
                "%minutes%", template.getResetMinutes());
        List<String> clockLore = context.getPlugin().getConfigManager().getStringList(
                "gui.step2-time.clock-lore",
                List.of(
                        "&7方塊被刷完變為普通方塊後",
                        "&7經過此時間將自動復原並重抽戰利品",
                        "&7",
                        "&7目前設定: &a" + TextUtil.formatTimeSeconds(seconds),
                        "&7",
                        "&e請點擊兩側按鈕直接增減時間",
                        "&7(最低設定為 1 秒)"
                ),
                "%time%", TextUtil.formatTimeSeconds(seconds),
                "%minutes%", template.getResetMinutes()
        );
        inventory.setItem(13, createButton(Material.CLOCK, clockName, clockLore));

        // Decrease buttons (Left side)
        String m60Name = context.getPlugin().getConfigManager().getText("gui.step2-time.btn-minus-60-name", "&c-60s");
        List<String> m60Lore = context.getPlugin().getConfigManager().getStringList("gui.step2-time.btn-minus-60-lore", List.of("&7點擊減少 60 秒"));
        inventory.setItem(10, createButton(Material.RED_CONCRETE, m60Name, m60Lore));

        String m10Name = context.getPlugin().getConfigManager().getText("gui.step2-time.btn-minus-10-name", "&c-10s");
        List<String> m10Lore = context.getPlugin().getConfigManager().getStringList("gui.step2-time.btn-minus-10-lore", List.of("&7點擊減少 10 秒"));
        inventory.setItem(11, createButton(Material.RED_TERRACOTTA, m10Name, m10Lore));

        String m1Name = context.getPlugin().getConfigManager().getText("gui.step2-time.btn-minus-1-name", "&c-1s");
        List<String> m1Lore = context.getPlugin().getConfigManager().getStringList("gui.step2-time.btn-minus-1-lore", List.of("&7點擊減少 1 秒"));
        inventory.setItem(12, createButton(Material.RED_STAINED_GLASS_PANE, m1Name, m1Lore));

        // Increase buttons (Right side)
        String p1Name = context.getPlugin().getConfigManager().getText("gui.step2-time.btn-plus-1-name", "&a+1s");
        List<String> p1Lore = context.getPlugin().getConfigManager().getStringList("gui.step2-time.btn-plus-1-lore", List.of("&7點擊增加 1 秒"));
        inventory.setItem(14, createButton(Material.LIME_STAINED_GLASS_PANE, p1Name, p1Lore));

        String p10Name = context.getPlugin().getConfigManager().getText("gui.step2-time.btn-plus-10-name", "&a+10s");
        List<String> p10Lore = context.getPlugin().getConfigManager().getStringList("gui.step2-time.btn-plus-10-lore", List.of("&7點擊增加 10 秒"));
        inventory.setItem(15, createButton(Material.LIME_TERRACOTTA, p10Name, p10Lore));

        String p60Name = context.getPlugin().getConfigManager().getText("gui.step2-time.btn-plus-60-name", "&a+60s");
        List<String> p60Lore = context.getPlugin().getConfigManager().getStringList("gui.step2-time.btn-plus-60-lore", List.of("&7點擊增加 60 秒"));
        inventory.setItem(16, createButton(Material.LIME_CONCRETE, p60Name, p60Lore));

        // Slot 4: Chat input
        String chatName = context.getPlugin().getConfigManager().getText("gui.step2-time.chat-input-name", "&b聊天室直接輸入秒數");
        List<String> chatLore = context.getPlugin().getConfigManager().getStringList(
                "gui.step2-time.chat-input-lore",
                List.of(
                        "&7點擊後在聊天室直接輸入數字 (秒)",
                        "&7完成後自動轉換為 &e分與秒",
                        "&7例如輸入: &e90 &7(即為 1分30秒 (90秒))"
                )
        );
        inventory.setItem(4, createButton(Material.PAPER, chatName, chatLore));

        // Slot 18: Back to Step 2
        String backName = context.getPlugin().getConfigManager().getText("gui.step2-time.back-button-name", "&e⬅ 返回 [步驟2/4] 重置模式");
        List<String> backLore = context.getPlugin().getConfigManager().getStringList("gui.step2-time.back-button-lore", List.of("&7儲存時間並返回重置模式設定"));
        inventory.setItem(18, createButton(Material.ARROW, backName, backLore));

        // Slot 26: Confirm and return
        String confirmName = context.getPlugin().getConfigManager().getText("gui.step2-time.confirm-button-name", "&a✔ 完成並返回");
        List<String> confirmLore = context.getPlugin().getConfigManager().getStringList("gui.step2-time.confirm-button-lore", List.of("&7返回 [步驟2/4] 重置模式"));
        inventory.setItem(26, createButton(Material.LIME_CONCRETE, confirmName, confirmLore));
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
            case 10 -> adjustTime(-60, player);
            case 11 -> adjustTime(-10, player);
            case 12 -> adjustTime(-1, player);
            case 14 -> adjustTime(1, player);
            case 15 -> adjustTime(10, player);
            case 16 -> adjustTime(60, player);
            case 4 -> {
                context.getPlugin().getConfigManager().playSound(player, "click");
                context.setTransitioning(true);
                context.getPlugin().getChatInputManager().requestInput(
                        player,
                        context.getPlugin().getConfigManager().getText("messages.time-prompt", "&e請在聊天室輸入自動重置秒數（請輸入大於等於 1 的整數），輸入 &ccancel &e取消："),
                        input -> {
                            context.setTransitioning(false);
                            try {
                                int val = Integer.parseInt(input.trim());
                                if (val < 1) {
                                    context.getPlugin().getConfigManager().send(player, "time-min-invalid");
                                    context.getPlugin().getConfigManager().playSound(player, "error");
                                } else {
                                    template.setResetSeconds(val);
                                    context.getPlugin().getConfigManager().playSound(player, "success");
                                    player.sendMessage(TextUtil.parse(context.getPlugin().getConfigManager().getPrefix() + "&a已將重置時間設定為: &e" + TextUtil.formatTimeSeconds(val)));
                                }
                            } catch (NumberFormatException e) {
                                context.getPlugin().getConfigManager().send(player, "time-invalid");
                                context.getPlugin().getConfigManager().playSound(player, "error");
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
            case 18, 26 -> {
                context.getPlugin().getConfigManager().playSound(player, "click");
                context.openStep2();
            }
        }
    }

    private void adjustTime(int delta, Player player) {
        int current = context.getTemplate().getResetSeconds();
        int newVal = Math.max(1, current + delta);
        if (newVal != current) {
            context.getTemplate().setResetSeconds(newVal);
            context.getPlugin().getConfigManager().playSound(player, "click");
            render();
        } else {
            context.getPlugin().getConfigManager().playSound(player, "error");
        }
    }

    @Override
    public void handleClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (!context.isTransitioning() && !context.isSavedSuccessfully() && !context.isDraftAbandoned()) {
            context.saveAsDraft();
        }
    }
}
