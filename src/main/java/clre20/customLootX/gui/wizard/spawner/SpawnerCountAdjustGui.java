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
 * 試煉生怪磚波次數值按鈕式調整介面
 */
public class SpawnerCountAdjustGui extends CustomGuiHolder {

    public enum TargetSetting {
        TOTAL_MOBS("總怪數", 1, 64, "隻", Material.IRON_SWORD),
        SIMULTANEOUS_MOBS("同時存活上限", 1, 16, "隻", Material.ARMOR_STAND),
        SPAWN_DELAY("生成間隔秒數", 1, 60, "秒", Material.CLOCK),
        PLAYER_RANGE("感應範圍格數", 4, 48, "格", Material.COMPASS),
        ROLL_COUNT("獲勝出貨數量", 1, 16, "件", Material.GOLD_INGOT);

        private final String title;
        private final int min;
        private final int max;
        private final String unit;
        private final Material icon;

        TargetSetting(String title, int min, int max, String unit, Material icon) {
            this.title = title;
            this.min = min;
            this.max = max;
            this.unit = unit;
            this.icon = icon;
        }

        public String getTitle() {
            return title;
        }

        public int getMin() {
            return min;
        }

        public int getMax() {
            return max;
        }

        public String getUnit() {
            return unit;
        }

        public Material getIcon() {
            return icon;
        }
    }

    private final SpawnerWizardContext context;
    private final TargetSetting setting;

    public SpawnerCountAdjustGui(SpawnerWizardContext context, TargetSetting setting) {
        this.context = context;
        this.setting = setting;
        this.inventory = Bukkit.createInventory(this, 27, TextUtil.parse("&8" + setting.getTitle() + " 數值調整"));
        render();
    }

    public void open() {
        context.getPlayer().openInventory(this.inventory);
    }

    private int getCurrentValue() {
        return switch (setting) {
            case TOTAL_MOBS -> context.getTemplate().getTotalMobs();
            case SIMULTANEOUS_MOBS -> context.getTemplate().getSimultaneousMobs();
            case SPAWN_DELAY -> context.getTemplate().getSpawnDelaySeconds();
            case PLAYER_RANGE -> context.getTemplate().getPlayerRange();
            case ROLL_COUNT -> context.getTemplate().getRollCount();
        };
    }

    private void setCurrentValue(int val) {
        int clamped = Math.max(setting.getMin(), Math.min(setting.getMax(), val));
        switch (setting) {
            case TOTAL_MOBS -> context.getTemplate().setTotalMobs(clamped);
            case SIMULTANEOUS_MOBS -> context.getTemplate().setSimultaneousMobs(clamped);
            case SPAWN_DELAY -> context.getTemplate().setSpawnDelaySeconds(clamped);
            case PLAYER_RANGE -> context.getTemplate().setPlayerRange(clamped);
            case ROLL_COUNT -> context.getTemplate().setRollCount(clamped);
        }
    }

    private void render() {
        String fillerName = context.getPlugin().getConfigManager().getText("gui.common.filler-name", " ");
        ItemStack filler = createButton(Material.GRAY_STAINED_GLASS_PANE, fillerName, null);
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        int current = getCurrentValue();

        // Slot 13: 目前數值圖示
        String title = "&6【" + setting.getTitle() + "】: &a" + current + " &f" + setting.getUnit();
        List<String> lore = List.of(
                "&7目前數值: &a" + current + " &7" + setting.getUnit(),
                "&7允許範圍: &f" + setting.getMin() + " ~ " + setting.getMax() + " " + setting.getUnit(),
                "&7",
                "&e點擊兩側按鈕直接增減數量",
                "&b或點擊下方告示牌在聊天室直接輸入"
        );
        inventory.setItem(13, createButton(setting.getIcon(), title, lore));

        // 減少按鈕
        inventory.setItem(10, createButton(Material.RED_CONCRETE, "&c-10 " + setting.getUnit(), List.of("&7減少 10")));
        inventory.setItem(11, createButton(Material.RED_TERRACOTTA, "&c-5 " + setting.getUnit(), List.of("&7減少 5")));
        inventory.setItem(12, createButton(Material.RED_STAINED_GLASS_PANE, "&c-1 " + setting.getUnit(), List.of("&7減少 1")));

        // 增加按鈕
        inventory.setItem(14, createButton(Material.LIME_STAINED_GLASS_PANE, "&a+1 " + setting.getUnit(), List.of("&7增加 1")));
        inventory.setItem(15, createButton(Material.LIME_TERRACOTTA, "&a+5 " + setting.getUnit(), List.of("&7增加 5")));
        inventory.setItem(16, createButton(Material.LIME_CONCRETE, "&a+10 " + setting.getUnit(), List.of("&7增加 10")));

        // Slot 22: 聊天室輸入
        inventory.setItem(22, createButton(Material.OAK_SIGN, "&b聊天室手動輸入", List.of("&7在聊天室直接輸入整數 (" + setting.getMin() + " ~ " + setting.getMax() + ")")));

        // Slot 18 & 26: 完成並返回
        inventory.setItem(18, createButton(Material.ARROW, "&e⬅ 完成並返回", List.of("&7返回前一步驟")));
        inventory.setItem(26, createButton(Material.EMERALD, "&a✔ 完成並返回", List.of("&7返回前一步驟")));
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
            case 10 -> adjustDelta(-10, player);
            case 11 -> adjustDelta(-5, player);
            case 12 -> adjustDelta(-1, player);
            case 14 -> adjustDelta(1, player);
            case 15 -> adjustDelta(5, player);
            case 16 -> adjustDelta(10, player);
            case 22 -> {
                // 聊天室輸入
                context.getPlugin().getConfigManager().playSound(player, "click");
                context.getPlugin().getChatInputManager().requestInput(
                        player,
                        "&e請在聊天室輸入數值 (" + setting.getMin() + " ~ " + setting.getMax() + "):",
                        input -> {
                            try {
                                int val = Integer.parseInt(input.trim());
                                if (val < setting.getMin() || val > setting.getMax()) {
                                    context.getPlugin().getConfigManager().send(player, "roll-invalid");
                                    context.getPlugin().getConfigManager().playSound(player, "error");
                                } else {
                                    setCurrentValue(val);
                                    context.getPlugin().getConfigManager().playSound(player, "success");
                                }
                            } catch (NumberFormatException e) {
                                context.getPlugin().getConfigManager().send(player, "roll-invalid");
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
            case 18, 26 -> {
                context.getPlugin().getConfigManager().playSound(player, "click");
                if (setting == TargetSetting.ROLL_COUNT) {
                    new SpawnerWizardStep4CooldownGui(context).open();
                } else {
                    new SpawnerWizardStep3WavesGui(context).open();
                }
            }
        }
    }

    private void adjustDelta(int delta, Player player) {
        int current = getCurrentValue();
        int newVal = Math.min(setting.getMax(), Math.max(setting.getMin(), current + delta));
        if (newVal != current) {
            setCurrentValue(newVal);
            context.getPlugin().getConfigManager().playSound(player, "click");
            render();
        } else {
            context.getPlugin().getConfigManager().playSound(player, "error");
        }
    }
}
