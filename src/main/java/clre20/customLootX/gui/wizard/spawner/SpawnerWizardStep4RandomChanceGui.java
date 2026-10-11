package clre20.customLootX.gui.wizard.spawner;

import clre20.customLootX.gui.CustomGuiHolder;
import clre20.customLootX.model.SpawnerMobEntry;
import clre20.customLootX.model.SpawnerTemplate;
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
 * 試煉生怪磚 [步驟 4-1-2/6] 隨機池機率設定介面 (當順序中包含隨機格時觸發)
 */
public class SpawnerWizardStep4RandomChanceGui extends CustomGuiHolder {

    private final SpawnerWizardContext context;
    private int page = 0;
    private static final int ITEMS_PER_PAGE = 45;

    public SpawnerWizardStep4RandomChanceGui(SpawnerWizardContext context) {
        this(context, 0);
    }

    public SpawnerWizardStep4RandomChanceGui(SpawnerWizardContext context, int pageNumber) {
        this.context = context;
        this.page = Math.max(0, pageNumber);
        this.inventory = Bukkit.createInventory(
                this,
                54,
                context.getPlugin().getConfigManager().getComponent(
                        "gui.spawner.step4.chance-title",
                        "&8[步驟 4-1-2/6] 隨機池機率設定"
                )
        );
        render();
    }

    public void open() {
        context.getPlayer().openInventory(this.inventory);
    }

    private void render() {
        inventory.clear();
        SpawnerTemplate template = context.getTemplate();
        List<SpawnerMobEntry> mobs = template.getMobPool();
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, mobs.size());

        for (int i = startIndex; i < endIndex; i++) {
            SpawnerMobEntry mob = mobs.get(i);
            int slot = i - startIndex;

            ItemStack displayItem = new ItemStack(mob.getIconMaterial());
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                String title = (mob.isMythic() ? "&d[Mythic] &f" : "&a[原版] &f") + mob.getDisplayName();
                meta.displayName(TextUtil.parse(title));

                List<Component> lore = new ArrayList<>();
                lore.add(TextUtil.parse("&8------------------------"));
                lore.add(TextUtil.parse("&7內部代號: &f" + mob.getMobId()));
                lore.add(TextUtil.parse("&7怪物類型: " + (mob.isMythic() ? "&dMythicMob 自訂怪物" : "&a原版生物")));
                lore.add(TextUtil.parse("&e隨機抽取機率: &a" + TextUtil.formatPercent(mob.getChance())));
                lore.add(TextUtil.parse("&7"));
                lore.add(TextUtil.parse("&e[點擊] &f設定此怪物的抽取權重"));
                lore.add(TextUtil.parse("&8------------------------"));
                meta.lore(lore);
                displayItem.setItemMeta(meta);
            }
            inventory.setItem(slot, displayItem);
        }

        // 工具列 (45 ~ 53)
        String fillerName = context.getPlugin().getConfigManager().getText("gui.common.filler-name", " ");
        ItemStack filler = createButton(Material.GRAY_STAINED_GLASS_PANE, fillerName, null);
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        // Slot 45: ⬅ 上一步 (返回順序編排)
        inventory.setItem(45, createButton(
                Material.ARROW,
                "&e⬅ 上一步 &f(怪物生成順序)",
                List.of("&7返回 [步驟 4/6] 順序看板修改生成順序")
        ));

        // Slot 48: ⚖ 自動均分機率
        inventory.setItem(48, createButton(
                Material.HOPPER,
                "&b⚖ 自動均分機率",
                List.of(
                        "&7將目前隨機池中所有怪物的機率平均分配",
                        "&7總和自動湊齊至 100.00%",
                        "&e點擊自動均分"
                )
        ));

        // Slot 49: 總機率狀態
        double totalChance = template.getTotalMobChance();
        boolean isValid = Math.abs(totalChance - 100.0) < 0.0001;
        String totalTitle = isValid
                ? "&a✔ 隨機池總機率: 100.00%"
                : "&c✖ 隨機池總機率: " + String.format("%.2f%%", totalChance);
        List<String> totalLore;
        if (isValid) {
            totalLore = List.of(
                    "&7機率總和完全符合 100.00%",
                    "&a可前往下一步進行冷卻設定！"
            );
        } else {
            double diff = 100.0 - totalChance;
            String diffStr = (diff > 0 ? ("&e尚缺: &a+" + TextUtil.formatPercent(diff)) : ("&c超出: &4-" + TextUtil.formatPercent(Math.abs(diff))));
            totalLore = List.of(
                    "&7因順序中含有【隨機】格子",
                    "&7所有怪物的機率總和必須恰好為 100.00%:",
                    "&f" + TextUtil.formatPercent(totalChance),
                    diffStr,
                    "&e可使用【自動均分】或點擊單個怪物調整"
            );
        }
        inventory.setItem(49, createButton(isValid ? Material.EMERALD : Material.REDSTONE, totalTitle, totalLore));

        // Slot 53: 下一步 (前往冷卻與出貨數量)
        if (isValid) {
            inventory.setItem(53, createButton(
                    Material.LIME_CONCRETE,
                    "&a下一步 ➜ &f(冷卻與出貨設定)",
                    List.of(
                            "&7前往 [步驟 4-2/6] 設定生怪磚冷卻模式與出貨數量",
                            "&a點擊前往下一步"
                    )
            ));
        } else {
            inventory.setItem(53, createButton(
                    Material.RED_CONCRETE,
                    "&c下一步 ➜ &7(機率非 100%)",
                    List.of(
                            "&c隨機池所有怪物機率總和必須恰好為 100.00%",
                            "&7目前總和為 &f" + TextUtil.formatPercent(totalChance),
                            "&e請先點擊【自動均分】或調整單隻怪物機率"
                    )
            ));
        }
    }

    private ItemStack createButton(Material mat, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(mat != null ? mat : Material.STONE);
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

        // 點擊上方怪物展示區 (0 ~ 44)
        if (slot >= 0 && slot < ITEMS_PER_PAGE) {
            int index = page * ITEMS_PER_PAGE + slot;
            List<SpawnerMobEntry> mobs = context.getTemplate().getMobPool();
            if (index < mobs.size()) {
                SpawnerMobEntry target = mobs.get(index);
                context.getPlugin().getConfigManager().playSound(player, "click");
                context.setTransitioning(true);
                new SpawnerMobChanceGui(context, target).open();
                context.setTransitioning(false);
            }
            return;
        }

        // 底部工具列
        switch (slot) {
            case 45 -> {
                // 返回順序看板
                context.getPlugin().getConfigManager().playSound(player, "click");
                context.setTransitioning(true);
                new SpawnerWizardStep4SequenceGui(context).open();
                context.setTransitioning(false);
            }
            case 48 -> {
                // 自動均分機率
                List<SpawnerMobEntry> mobs = context.getTemplate().getMobPool();
                if (!mobs.isEmpty()) {
                    double each = TextUtil.roundChance(100.0 / mobs.size());
                    double sum = 0.0;
                    for (int i = 0; i < mobs.size(); i++) {
                        if (i == mobs.size() - 1) {
                            mobs.get(i).setChance(TextUtil.roundChance(100.0 - sum));
                        } else {
                            mobs.get(i).setChance(each);
                            sum += each;
                        }
                    }
                    context.getPlugin().getConfigManager().playSound(player, "success");
                    render();
                }
            }
            case 53 -> {
                // 下一步 (前往冷卻與出貨數量)
                double totalChance = context.getTemplate().getTotalMobChance();
                if (Math.abs(totalChance - 100.0) < 0.00001) {
                    context.getPlugin().getConfigManager().playSound(player, "click");
                    context.setTransitioning(true);
                    new SpawnerWizardStep4CooldownGui(context).open();
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
}
