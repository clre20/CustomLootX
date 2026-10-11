package clre20.customLootX.gui.wizard;

import clre20.customLootX.gui.CustomGuiHolder;
import clre20.customLootX.gui.ItemChanceGui;
import clre20.customLootX.model.LootItem;
import clre20.customLootX.model.LootTemplate;
import clre20.customLootX.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class WizardStep3Gui extends CustomGuiHolder {

    private final WizardContext context;
    private int page = 0;
    private static final int ITEMS_PER_PAGE = 45;

    public WizardStep3Gui(WizardContext context) {
        this.context = context;
        this.inventory = Bukkit.createInventory(this, 54, context.getPlugin().getConfigManager().getComponent("gui.step3.title", "&8[步驟 3/4] 掉落物與機率設定"));
        render();
    }

    public void open() {
        context.getPlayer().openInventory(this.inventory);
    }

    public void refresh() {
        render();
    }

    private void render() {
        inventory.clear();

        LootTemplate template = context.getTemplate();
        List<LootItem> items = template.getItems();
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, items.size());

        String airName = context.getPlugin().getConfigManager().getText("gui.step3.air-item-name", "&c[落空 / 無掉落]");

        // Slots 0 ~ 44: Items in current page
        for (int i = startIndex; i < endIndex; i++) {
            int slot = i - startIndex;
            LootItem lootItem = items.get(i);
            ItemStack displayItem;

            if (lootItem.isAir()) {
                displayItem = new ItemStack(Material.BARRIER);
                ItemMeta meta = displayItem.getItemMeta();
                if (meta != null) {
                    meta.displayName(TextUtil.parse(airName));
                    displayItem.setItemMeta(meta);
                }
            } else {
                displayItem = lootItem.getItem();
            }

            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                List<Component> extra = context.getPlugin().getConfigManager().getComponentList(
                        "gui.step3.item-lore-format",
                        List.of(
                                "",
                                "&8------------------------",
                                "&e設定機率: &a%chance%",
                                "&7",
                                "&e[點擊] &f設定機率或移除此物品",
                                "&8------------------------"
                        ),
                        "%chance%", TextUtil.formatPercent(lootItem.getChance())
                );
                lore.addAll(extra);
                if (lootItem.isBroadcast()) {
                    lore.add(TextUtil.parse("&6📢 全服獲獎通告: &a【已開啟】"));
                }
                if (lootItem.hasAnyLimit()) {
                    List<String> limitParts = new ArrayList<>();
                    if (lootItem.getLimitServerDaily() > 0) limitParts.add("全服日:" + lootItem.getLimitServerDaily());
                    if (lootItem.getLimitServerMonthly() > 0) limitParts.add("全服月:" + lootItem.getLimitServerMonthly());
                    if (lootItem.getLimitPlayerDaily() > 0) limitParts.add("個人日:" + lootItem.getLimitPlayerDaily());
                    if (lootItem.getLimitPlayerTotal() > 0) limitParts.add("個人總:" + lootItem.getLimitPlayerTotal());
                    lore.add(TextUtil.parse("&c🛡️ 出貨上限: &e" + String.join(" &8| &e", limitParts)));
                }
                meta.lore(lore);
                displayItem.setItemMeta(meta);
            }
            inventory.setItem(slot, displayItem);
        }

        // Fill row 5 background
        String fillerName = context.getPlugin().getConfigManager().getText("gui.common.filler-name", " ");
        ItemStack filler = createButton(Material.GRAY_STAINED_GLASS_PANE, fillerName, null);
        for (int s = 45; s < 54; s++) {
            inventory.setItem(s, filler);
        }

        // Slot 45: Previous page
        if (page > 0) {
            String prevName = context.getPlugin().getConfigManager().getText("gui.common.prev-page-name", "&e上一頁");
            List<String> prevLore = context.getPlugin().getConfigManager().getStringList("gui.common.prev-page-lore", List.of("&7前往第 %page% 頁"), "%page%", page);
            inventory.setItem(45, createButton(Material.ARROW, prevName, prevLore));
        }

        // Slot 46: Back to Step 2
        String backName = context.getPlugin().getConfigManager().getText("gui.common.back-to-step2-name", "&e⬅ 上一步 &f(重置模式)");
        List<String> backLore = context.getPlugin().getConfigManager().getStringList("gui.common.back-to-step2-lore", List.of("&7返回 [步驟 2/4] 修改重置模式"));
        inventory.setItem(46, createButton(Material.ARROW, backName, backLore));

        // Slot 47: 放置說明書
        String guideTitle = context.getPlugin().getConfigManager().getText("gui.step3.guide-name", "&e【放入物品操作說明】");
        List<String> guideLore = context.getPlugin().getConfigManager().getStringList(
                "gui.step3.guide-lore",
                List.of(
                        "&7▪ 可直接拿起背包物品點擊上方空格放入",
                        "&7▪ 或點擊背包物品快速放入",
                        "&7▪ 點擊格子可開啟機率調整與管理畫面 (內含移除按鈕)"
                )
        );
        inventory.setItem(47, createButton(Material.BOOK, guideTitle, guideLore));

        // Slot 48: Add Air drop
        String addAirName = context.getPlugin().getConfigManager().getText("gui.step3.add-air-name", "&c新增「無掉落」項目");
        List<String> addAirLore = context.getPlugin().getConfigManager().getStringList(
                "gui.step3.add-air-lore",
                List.of(
                        "&7在獎池中增加一項刷空的機率",
                        "&7當抽中此項目時，方塊將不掉落任何物品",
                        "&e點擊新增"
                )
        );
        inventory.setItem(48, createButton(Material.SHEARS, addAirName, addAirLore));

        // Slot 52: Next page
        if (endIndex < items.size()) {
            String nextName = context.getPlugin().getConfigManager().getText("gui.common.next-page-name", "&e下一頁");
            List<String> nextLore = context.getPlugin().getConfigManager().getStringList("gui.common.next-page-lore", List.of("&7前往第 %page% 頁"), "%page%", page + 2);
            inventory.setItem(52, createButton(Material.ARROW, nextName, nextLore));
        }

        // Slot 53: Next to Step 4
        double total = template.getTotalChance();
        double diff = TextUtil.roundChance(100.00 - total);
        boolean valid = template.isValidTotal();
        if (valid) {
            String nextValidName = context.getPlugin().getConfigManager().getText("gui.step3.next-valid-name", "&a下一步 ➜ &f(設定確認)");
            List<String> nextValidLore = context.getPlugin().getConfigManager().getStringList(
                    "gui.step3.next-valid-lore",
                    List.of(
                            "&7機率總和已達 100.00%",
                            "&7前往 [步驟 4/4] 檢查所有設定並確認送出",
                            "&a點擊前往下一步"
                    )
            );
            inventory.setItem(53, createButton(Material.LIME_CONCRETE, nextValidName, nextValidLore));
        } else {
            String nextInvalidName = context.getPlugin().getConfigManager().getText("gui.step3.next-invalid-name", "&c下一步 ➜ &7(機率非 100%)");
            String diffStr = (diff > 0 ? "+" : "") + TextUtil.formatPercent(diff);
            List<String> nextInvalidLore = context.getPlugin().getConfigManager().getStringList(
                    "gui.step3.next-invalid-lore",
                    List.of(
                            "&c目前總機率: &e%total%",
                            "&c距離 100.00% 尚差: &e%diff%",
                            "&c機率總和必須恰好為 100.00% 才能前往確認頁面！"
                    ),
                    "%total%", TextUtil.formatPercent(total),
                    "%diff%", diffStr
            );
            inventory.setItem(53, createButton(Material.RED_CONCRETE, nextInvalidName, nextInvalidLore));
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
        int rawSlot = event.getRawSlot();
        Player player = context.getPlayer();
        LootTemplate template = context.getTemplate();

        // 玩家點擊自身背包物品
        if (rawSlot >= 54) {
            if (event.isShiftClick() && event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                event.setCancelled(true);
                ItemStack toAdd = event.getCurrentItem().clone();
                toAdd.setAmount(1);

                double remaining = TextUtil.roundChance(100.00 - template.getTotalChance());
                double initChance = (remaining > 0) ? Math.min(remaining, 10.00) : 0.00;

                template.addItem(new LootItem(toAdd, initChance));
                context.getPlugin().getConfigManager().playSound(player, "click");
                render();
            }
            // 非 Shift 點擊自身背包：允許自然拿起/放置物品
            return;
        }

        event.setCancelled(true);
        if (rawSlot < 0 || rawSlot >= 54) return;

        // 掉落物格子區域 (0 ~ 44)
        if (rawSlot < ITEMS_PER_PAGE) {
            ItemStack cursor = event.getCursor();

            // 若鼠標上有拿物品 -> 放入上方格子
            if (cursor != null && !cursor.getType().isAir()) {
                ItemStack toAdd = cursor.clone();
                toAdd.setAmount(1);

                double remaining = TextUtil.roundChance(100.00 - template.getTotalChance());
                double initChance = (remaining > 0) ? Math.min(remaining, 10.00) : 0.00;

                int itemIndex = page * ITEMS_PER_PAGE + rawSlot;
                if (itemIndex < template.getItems().size()) {
                    LootItem old = template.getItems().get(itemIndex);
                    template.getItems().set(itemIndex, new LootItem(toAdd, old.getChance(), old.isBroadcast(), old.getBroadcastMessage(), old.getLimitServerDaily(), old.getLimitServerMonthly(), old.getLimitPlayerDaily(), old.getLimitPlayerTotal()));
                } else {
                    template.addItem(new LootItem(toAdd, initChance));
                }
                context.getPlugin().getConfigManager().playSound(player, "click");
                render();
                return;
            }

            // 鼠標為空：點擊已存在的物品
            int itemIndex = page * ITEMS_PER_PAGE + rawSlot;
            if (itemIndex >= template.getItems().size()) {
                return;
            }

            if (event.isRightClick()) {
                // 右鍵取出移除
                template.getItems().remove(itemIndex);
                context.getPlugin().getConfigManager().playSound(player, "click");
                render();
                return;
            }

            // 左鍵進入機率調整畫面
            context.getPlugin().getConfigManager().playSound(player, "click");
            context.setTransitioning(true);
            ItemChanceGui chanceGui = new ItemChanceGui(context.getPlugin(), player, template, itemIndex, () -> {
                context.setTransitioning(false);
                this.refresh();
                this.open();
            });
            chanceGui.open();
            context.setTransitioning(false);
            return;
        }

        // 控制列 (45 ~ 53)
        switch (rawSlot) {
            case 45 -> {
                if (page > 0) {
                    page--;
                    context.getPlugin().getConfigManager().playSound(player, "click");
                    render();
                }
            }
            case 46 -> {
                // 返回步驟二
                context.getPlugin().getConfigManager().playSound(player, "click");
                context.openStep2();
            }
            case 48 -> {
                // 新增無掉落項目
                double remaining = TextUtil.roundChance(100.00 - template.getTotalChance());
                double initChance = (remaining > 0) ? remaining : 0.00;
                template.addItem(new LootItem(initChance, true));
                context.getPlugin().getConfigManager().playSound(player, "click");
                render();
            }
            case 52 -> {
                if ((page + 1) * ITEMS_PER_PAGE < template.getItems().size()) {
                    page++;
                    context.getPlugin().getConfigManager().playSound(player, "click");
                    render();
                }
            }
            case 53 -> {
                // 前往步驟四
                if (!template.isValidTotal()) {
                    context.getPlugin().getConfigManager().send(player, "sum-not-100",
                            "%.2f%%", template.getTotalChance(),
                            "%+.2f%%", (100.00 - template.getTotalChance())
                    );
                    context.getPlugin().getConfigManager().playSound(player, "error");
                    return;
                }
                context.getPlugin().getConfigManager().playSound(player, "click");
                context.openStep4();
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
