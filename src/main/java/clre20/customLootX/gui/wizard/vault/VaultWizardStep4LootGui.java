package clre20.customLootX.gui.wizard.vault;

import clre20.customLootX.gui.CustomGuiHolder;
import clre20.customLootX.model.LootItem;
import clre20.customLootX.model.VaultTemplate;
import clre20.customLootX.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 試煉寶庫 [步驟 4/5] 掉落物與機率設定介面
 */
public class VaultWizardStep4LootGui extends CustomGuiHolder {

    private final VaultWizardContext context;
    private int page = 0;
    private static final int ITEMS_PER_PAGE = 45;

    public VaultWizardStep4LootGui(VaultWizardContext context, int pageNumber) {
        this.context = context;
        this.page = Math.max(0, pageNumber - 1);
        this.inventory = Bukkit.createInventory(this, 54, context.getPlugin().getConfigManager().getComponent("gui.vault.step4.title", "&8[步驟 4/5] 掉落物與機率設定"));
        render();
    }

    public void open() {
        context.getPlayer().openInventory(this.inventory);
    }

    private void render() {
        inventory.clear();

        VaultTemplate template = context.getTemplate();
        List<LootItem> items = template.getItems();
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, items.size());

        String airName = context.getPlugin().getConfigManager().getText("gui.step3.air-item-name", "&c[落空 / 無掉落]");

        // 擺放當前頁面的掉落物
        for (int i = startIndex; i < endIndex; i++) {
            LootItem loot = items.get(i);
            int slot = i - startIndex;

            ItemStack displayItem;
            if (loot.isAir() || loot.getItem() == null) {
                displayItem = new ItemStack(Material.STRUCTURE_VOID);
                ItemMeta meta = displayItem.getItemMeta();
                if (meta != null) {
                    meta.displayName(TextUtil.parse(airName));
                    displayItem.setItemMeta(meta);
                }
            } else {
                displayItem = loot.getItem().clone();
            }

            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                List<Component> currentLore = meta.lore();
                if (currentLore == null) currentLore = new ArrayList<>();

                List<String> chanceLore = context.getPlugin().getConfigManager().getStringList(
                        "gui.step3.item-lore-format",
                        List.of(
                                "",
                                "&8------------------------",
                                "&e設定機率: &a%chance%",
                                "&7",
                                "&e[點擊] &f設定機率或移除此物品"
                        ),
                        "%chance%", String.format("%.2f%%", loot.getChance())
                );
                for (String line : chanceLore) {
                    currentLore.add(TextUtil.parse(line));
                }
                meta.lore(currentLore);
                displayItem.setItemMeta(meta);
            }
            inventory.setItem(slot, displayItem);
        }

        // 底部功能按鈕背景填充 (45 ~ 53)
        String fillerName = context.getPlugin().getConfigManager().getText("gui.common.filler-name", " ");
        ItemStack filler = createButton(Material.GRAY_STAINED_GLASS_PANE, fillerName, null);
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        // Slot 45: 返回步驟三 (冷卻與出貨設定)
        String backName = context.getPlugin().getConfigManager().getText("gui.vault.step4.back-to-step3-name", "&e⬅ 上一步 &f(冷卻與出貨設定)");
        List<String> backLore = context.getPlugin().getConfigManager().getStringList(
                "gui.vault.step4.back-to-step3-lore",
                List.of("&7返回 [步驟 3/5] 修改冷卻或出貨數量")
        );
        inventory.setItem(45, createButton(Material.ARROW, backName, backLore));

        // Slot 46: 添加落空
        String addAirName = context.getPlugin().getConfigManager().getText("gui.step3.add-air-name", "&e➕ 添加落空項目");
        List<String> addAirLore = context.getPlugin().getConfigManager().getStringList(
                "gui.step3.add-air-lore",
                List.of(
                        "&7加入一個【無任何掉落】的落空機率",
                        "&7有助於在未湊齊 100% 前進行平衡",
                        "&e點擊添加"
                )
        );
        inventory.setItem(46, createButton(Material.STRUCTURE_VOID, addAirName, addAirLore));

        // Slot 47: 自動均分機率
        String distributeName = context.getPlugin().getConfigManager().getText("gui.step3.distribute-name", "&b⚖ 自動均分機率");
        List<String> distributeLore = context.getPlugin().getConfigManager().getStringList(
                "gui.step3.distribute-lore",
                List.of(
                        "&7將目前所有物品的機率平均分配",
                        "&7總和自動湊齊至 100.00%",
                        "&e點擊自動均分"
                )
        );
        inventory.setItem(47, createButton(Material.HOPPER, distributeName, distributeLore));

        // Slot 48: 上一頁
        if (page > 0) {
            String prevName = context.getPlugin().getConfigManager().getText("gui.common.prev-page-name", "&b⬅ 上一頁");
            List<String> prevLore = context.getPlugin().getConfigManager().getStringList(
                    "gui.common.prev-page-lore",
                    List.of("&7前往第 " + page + " 頁")
            );
            inventory.setItem(48, createButton(Material.FEATHER, prevName, prevLore));
        }

        // Slot 49: 總機率資訊按鈕
        double totalChance = template.getTotalChance();
        boolean isValid = template.isTotalChanceValid();
        String totalTitle = isValid
                ? context.getPlugin().getConfigManager().getText("gui.step3.total-valid-name", "&a✔ 總機率: 100.00%")
                : context.getPlugin().getConfigManager().getText("gui.step3.total-invalid-name", "&c✖ 總機率: %total%", "%total%", String.format("%.2f%%", totalChance));
        List<String> totalLore;
        if (isValid) {
            totalLore = context.getPlugin().getConfigManager().getStringList(
                    "gui.step3.total-valid-lore",
                    List.of(
                            "&7機率總和完全符合 100.00%",
                            "&a可以隨時前往下一步完成設定！"
                    )
            );
        } else {
            double diff = 100.0 - totalChance;
            String diffStr = (diff > 0 ? ("&e尚缺: &a+" + String.format("%.2f%%", diff)) : ("&c超出: &4-" + String.format("%.2f%%", Math.abs(diff))));
            totalLore = context.getPlugin().getConfigManager().getStringList(
                    "gui.step3.total-invalid-lore",
                    List.of(
                            "&7目前掉落池所有物品總和:",
                            "&f%total%",
                            "%diff%",
                            "&c必須正好等於 100.00% 才能送出儲存！"
                    ),
                    "%total%", String.format("%.2f%%", totalChance),
                    "%diff%", diffStr
            );
        }
        inventory.setItem(49, createButton(isValid ? Material.EMERALD : Material.REDSTONE, totalTitle, totalLore));

        // Slot 50: 下一頁
        int maxPages = (int) Math.ceil((double) items.size() / ITEMS_PER_PAGE);
        if (page + 1 < maxPages) {
            String nextPName = context.getPlugin().getConfigManager().getText("gui.common.next-page-name", "&b下一頁 ➜");
            List<String> nextPLore = context.getPlugin().getConfigManager().getStringList(
                    "gui.common.next-page-lore",
                    List.of("&7前往第 " + (page + 2) + " 頁")
            );
            inventory.setItem(50, createButton(Material.FEATHER, nextPName, nextPLore));
        }

        // Slot 53: 下一步 (前往步驟五：確認與儲存)
        if (isValid) {
            String nextName = context.getPlugin().getConfigManager().getText("gui.step3.next-ready-name", "&a下一步 ➜ &f(設定確認)");
            List<String> nextLore = context.getPlugin().getConfigManager().getStringList(
                    "gui.step3.next-ready-lore",
                    List.of(
                            "&7前往 [步驟 5/5] 確認所有設定並儲存",
                            "&a點擊前往下一步"
                    )
            );
            inventory.setItem(53, createButton(Material.LIME_CONCRETE, nextName, nextLore));
        } else {
            String nextNotReadyName = context.getPlugin().getConfigManager().getText("gui.step3.next-not-ready-name", "&c下一步 ➜ &7(機率未達100%)");
            List<String> nextNotReadyLore = context.getPlugin().getConfigManager().getStringList(
                    "gui.step3.next-not-ready-lore",
                    List.of(
                            "&c總機率必須正好等於 100.00%",
                            "&7目前為: &e%total%",
                            "&7請調整現有物品機率或添加落空項目"
                    ),
                    "%total%", String.format("%.2f%%", totalChance)
            );
            inventory.setItem(53, createButton(Material.RED_CONCRETE, nextNotReadyName, nextNotReadyLore));
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
        Player player = (Player) event.getWhoClicked();
        VaultTemplate template = context.getTemplate();
        List<LootItem> items = template.getItems();

        // 玩家點擊自身背包物品 (rawSlot >= 54)
        if (rawSlot >= 54) {
            if (event.isShiftClick() && event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                event.setCancelled(true);
                ItemStack toAdd = event.getCurrentItem().clone();

                double remaining = TextUtil.roundChance(100.00 - template.getTotalChance());
                double initChance = (remaining > 0) ? Math.min(remaining, 10.00) : 0.00;

                items.add(new LootItem(toAdd, initChance));
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

            // 若鼠標上有拿物品 -> 放入上方格子 (同步可疑系列)
            if (cursor != null && !cursor.getType().isAir()) {
                ItemStack toAdd = cursor.clone();

                double remaining = TextUtil.roundChance(100.00 - template.getTotalChance());
                double initChance = (remaining > 0) ? Math.min(remaining, 10.00) : 0.00;

                int itemIndex = page * ITEMS_PER_PAGE + rawSlot;
                if (itemIndex < items.size()) {
                    LootItem old = items.get(itemIndex);
                    items.set(itemIndex, new LootItem(toAdd, old.getChance()));
                } else {
                    items.add(new LootItem(toAdd, initChance));
                }
                context.getPlugin().getConfigManager().playSound(player, "click");
                render();
                return;
            }

            // 鼠標為空：點擊已存在的物品
            int itemIndex = page * ITEMS_PER_PAGE + rawSlot;
            if (itemIndex >= items.size()) {
                return;
            }

            if (event.isRightClick()) {
                // 右鍵取出移除 (同步可疑系列：直接從清單中移除)
                items.remove(itemIndex);
                context.getPlugin().getConfigManager().playSound(player, "click");
                render();
                return;
            }

            // 左鍵進入機率調整畫面
            context.getPlugin().getConfigManager().playSound(player, "click");
            context.setTransitioning(true);
            new VaultItemChanceGui(context, items.get(itemIndex)).open();
            context.setTransitioning(false);
            return;
        }

        // 控制列 (45 ~ 53)
        switch (rawSlot) {
            case 45 -> {
                // 返回步驟三 (冷卻與出貨設定)
                context.getPlugin().getConfigManager().playSound(player, "click");
                context.setTransitioning(true);
                new VaultWizardStep3CooldownGui(context).open();
                context.setTransitioning(false);
            }
            case 46 -> {
                // ➕ 添加落空
                double remaining = TextUtil.roundChance(100.00 - template.getTotalChance());
                double initChance = (remaining > 0) ? remaining : 0.00;
                items.add(new LootItem(initChance, true));
                context.getPlugin().getConfigManager().playSound(player, "click");
                render();
            }
            case 47 -> {
                // ⚖ 自動均分機率
                if (!items.isEmpty()) {
                    distributeEvenly(items);
                    context.getPlugin().getConfigManager().playSound(player, "success");
                    render();
                } else {
                    context.getPlugin().getConfigManager().playSound(player, "error");
                }
            }
            case 48 -> {
                // ⬅ 上一頁
                if (page > 0) {
                    page--;
                    context.getPlugin().getConfigManager().playSound(player, "click");
                    render();
                }
            }
            case 49 -> {
                // 總機率資訊（點擊播放音效，絕不被拿起）
                context.getPlugin().getConfigManager().playSound(player, "click");
            }
            case 50 -> {
                // 下一頁 ➜
                int maxPages = (int) Math.ceil((double) items.size() / ITEMS_PER_PAGE);
                if (page + 1 < maxPages) {
                    page++;
                    context.getPlugin().getConfigManager().playSound(player, "click");
                    render();
                }
            }
            case 53 -> {
                // 下一步 (前往步驟五：確認與儲存)
                if (!template.isTotalChanceValid()) {
                    double total = template.getTotalChance();
                    double diff = Math.abs(100.0 - total);
                    context.getPlugin().getConfigManager().send(player, "sum-not-100",
                            "%total%", String.format("%.2f%%", total),
                            "%diff%", String.format("%.2f%%", diff));
                    context.getPlugin().getConfigManager().playSound(player, "error");
                    return;
                }
                context.getPlugin().getConfigManager().playSound(player, "click");
                context.setTransitioning(true);
                new VaultWizardStep5ConfirmGui(context).open();
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

    @Override
    public void handleDrag(InventoryDragEvent event) {
        Set<Integer> rawSlots = event.getRawSlots();
        if (rawSlots == null || rawSlots.isEmpty()) return;

        // 若拖曳劃過任何頂部視窗格子 (slot < 54)，全部 cancel，防止干擾介面
        for (int slot : rawSlots) {
            if (slot < 54) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private void distributeEvenly(List<LootItem> items) {
        int count = items.size();
        if (count == 0) return;

        double base = Math.floor((100.0 / count) * 100.0) / 100.0;
        double totalAssigned = base * count;
        double remainder = TextUtil.roundChance(100.0 - totalAssigned);

        for (int i = 0; i < count; i++) {
            double c = base;
            if (i == count - 1) {
                c = TextUtil.roundChance(base + remainder);
            }
            items.get(i).setChance(c);
        }
    }
}
