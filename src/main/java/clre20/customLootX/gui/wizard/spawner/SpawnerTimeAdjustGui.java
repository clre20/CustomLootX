package clre20.customLootX.gui.wizard.spawner;

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
 * 試煉生怪磚冷卻時間調整介面
 */
public class SpawnerTimeAdjustGui extends CustomGuiHolder {

    private final SpawnerWizardContext context;

    public SpawnerTimeAdjustGui(SpawnerWizardContext context) {
        this.context = context;
        this.inventory = Bukkit.createInventory(this, 27, context.getPlugin().getConfigManager().getComponent("gui.step2-time.title", "&8試煉生怪磚冷卻時間設定"));
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

        int minutes = context.getTemplate().getCooldownMinutes();

        // Slot 13: 目前時間時鐘
        String clockName = context.getPlugin().getConfigManager().getText("gui.step2-time.clock-name", "&e目前冷卻時間: &a%minutes% &e分鐘", "%minutes%", minutes);
        List<String> clockLore = context.getPlugin().getConfigManager().getStringList(
                "gui.step2-time.clock-lore",
                List.of(
                        "&7試煉獲勝後將進入冷卻",
                        "&7經過此時間將自動復原並允許再次挑戰",
                        "&7",
                        "&e請點擊兩側按鈕直接增減時間",
                        "&7(最低設定為 1 分鐘)"
                )
        );
        inventory.setItem(13, createButton(Material.CLOCK, clockName, clockLore));

        // 減少時間按鈕
        inventory.setItem(10, createButton(Material.RED_CONCRETE, context.getPlugin().getConfigManager().getText("gui.step2-time.btn-minus-10-name", "&c-10 分鐘"),
                context.getPlugin().getConfigManager().getStringList("gui.step2-time.btn-minus-10-lore", List.of("&7點擊減少 10 分鐘"))));
        inventory.setItem(11, createButton(Material.RED_TERRACOTTA, context.getPlugin().getConfigManager().getText("gui.step2-time.btn-minus-5-name", "&c-5 分鐘"),
                context.getPlugin().getConfigManager().getStringList("gui.step2-time.btn-minus-5-lore", List.of("&7點擊減少 5 分鐘"))));
        inventory.setItem(12, createButton(Material.RED_STAINED_GLASS_PANE, context.getPlugin().getConfigManager().getText("gui.step2-time.btn-minus-1-name", "&c-1 分鐘"),
                context.getPlugin().getConfigManager().getStringList("gui.step2-time.btn-minus-1-lore", List.of("&7點擊減少 1 分鐘"))));

        // 增加時間按鈕
        inventory.setItem(14, createButton(Material.LIME_STAINED_GLASS_PANE, context.getPlugin().getConfigManager().getText("gui.step2-time.btn-plus-1-name", "&a+1 分鐘"),
                context.getPlugin().getConfigManager().getStringList("gui.step2-time.btn-plus-1-lore", List.of("&7點擊增加 1 分鐘"))));
        inventory.setItem(15, createButton(Material.LIME_TERRACOTTA, context.getPlugin().getConfigManager().getText("gui.step2-time.btn-plus-5-name", "&a+5 分鐘"),
                context.getPlugin().getConfigManager().getStringList("gui.step2-time.btn-plus-5-lore", List.of("&7點擊增加 5 分鐘"))));
        inventory.setItem(16, createButton(Material.LIME_CONCRETE, context.getPlugin().getConfigManager().getText("gui.step2-time.btn-plus-10-name", "&a+10 分鐘"),
                context.getPlugin().getConfigManager().getStringList("gui.step2-time.btn-plus-10-lore", List.of("&7點擊增加 10 分鐘"))));

        // Slot 22: 聊天室手動輸入
        inventory.setItem(22, createButton(Material.OAK_SIGN, context.getPlugin().getConfigManager().getText("gui.step2-time.chat-input-name", "&b聊天室直接輸入"),
                context.getPlugin().getConfigManager().getStringList("gui.step2-time.chat-input-lore", List.of("&7點擊後在聊天室直接輸入數字"))));

        // Slot 18 & 26: 完成並返回
        inventory.setItem(18, createButton(Material.ARROW, "&e⬅ 完成並返回", List.of("&7返回 [步驟 4/6]")));
        inventory.setItem(26, createButton(Material.EMERALD, "&a✔ 完成並返回", List.of("&7返回 [步驟 4/6]")));
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
            case 10 -> adjustTime(-10, player);
            case 11 -> adjustTime(-5, player);
            case 12 -> adjustTime(-1, player);
            case 14 -> adjustTime(1, player);
            case 15 -> adjustTime(5, player);
            case 16 -> adjustTime(10, player);
            case 22 -> {
                // 聊天室輸入
                context.getPlugin().getConfigManager().playSound(player, "click");
                context.setTransitioning(true);
                context.getPlugin().getChatInputManager().requestInput(
                        player,
                        context.getPlugin().getConfigManager().getRawMessage("time-prompt"),
                        input -> {
                            try {
                                int val = Integer.parseInt(input.trim());
                                if (val < 1) {
                                    context.getPlugin().getConfigManager().send(player, "time-min-invalid");
                                    context.getPlugin().getConfigManager().playSound(player, "error");
                                } else {
                                    context.getTemplate().setCooldownMinutes(val);
                                    context.getPlugin().getConfigManager().playSound(player, "success");
                                }
                            } catch (NumberFormatException e) {
                                context.getPlugin().getConfigManager().send(player, "time-invalid");
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
                new SpawnerWizardStep4CooldownGui(context).open();
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
        int current = context.getTemplate().getCooldownMinutes();
        int newVal = Math.max(1, current + delta);
        if (newVal != current) {
            context.getTemplate().setCooldownMinutes(newVal);
            context.getPlugin().getConfigManager().playSound(player, "click");
            render();
        } else {
            context.getPlugin().getConfigManager().playSound(player, "error");
        }
    }
}
