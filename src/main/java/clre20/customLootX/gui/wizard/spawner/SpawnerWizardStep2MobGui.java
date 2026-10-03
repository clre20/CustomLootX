package clre20.customLootX.gui.wizard.spawner;

import clre20.customLootX.gui.CustomGuiHolder;
import clre20.customLootX.hook.MythicMobHook;
import clre20.customLootX.model.SpawnerMobEntry;
import clre20.customLootX.model.SpawnerTemplate;
import clre20.customLootX.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 試煉生怪磚 [步驟 2/6] 怪物池與生成機率設定介面 (支援原版生物與 MythicMobs 自訂怪物)
 */
public class SpawnerWizardStep2MobGui extends CustomGuiHolder {

    private final SpawnerWizardContext context;
    private int page = 0;
    private static final int ITEMS_PER_PAGE = 45;

    public SpawnerWizardStep2MobGui(SpawnerWizardContext context, int pageNumber) {
        this.context = context;
        this.page = Math.max(0, pageNumber - 1);
        this.inventory = Bukkit.createInventory(
                this,
                54,
                context.getPlugin().getConfigManager().getComponent("gui.spawner.step2.pool-title", "&8[步驟 2/6] 怪物池與生成機率")
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

        // 擺放當前頁面的怪物項目
        for (int i = startIndex; i < endIndex; i++) {
            SpawnerMobEntry mob = mobs.get(i);
            int slot = i - startIndex;

            ItemStack displayItem = new ItemStack(mob.getIconMaterial());
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                String title = (mob.isMythic() ? "&d[Mythic] &f" : "&e[原版] &f") + mob.getDisplayName();
                meta.displayName(TextUtil.parse(title));

                List<Component> lore = new ArrayList<>();
                lore.add(TextUtil.parse("&8------------------------"));
                lore.add(TextUtil.parse("&7內部代號: &f" + mob.getMobId()));
                lore.add(TextUtil.parse("&7怪物類型: " + (mob.isMythic() ? "&dMythicMob 自訂怪物" : "&a原版生物")));
                lore.add(TextUtil.parse("&e生成機率: &a" + String.format("%.2f%%", mob.getChance())));
                lore.add(TextUtil.parse("&7"));
                lore.add(TextUtil.parse("&e[點擊] &f設定機率或移除此怪物"));
                if (mobs.size() == 1) {
                    lore.add(TextUtil.parse("&8(池中唯一怪物無法移除)"));
                }
                lore.add(TextUtil.parse("&8------------------------"));
                meta.lore(lore);
                displayItem.setItemMeta(meta);
            }
            inventory.setItem(slot, displayItem);
        }

        // 底部工具列背景 (45 ~ 53)
        String fillerName = context.getPlugin().getConfigManager().getText("gui.common.filler-name", " ");
        ItemStack filler = createButton(Material.GRAY_STAINED_GLASS_PANE, fillerName, null);
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        // Slot 45: 返回步驟一
        inventory.setItem(45, createButton(Material.ARROW, "&e⬅ 上一步 &f(種類與名稱)", List.of("&7返回 [步驟 1/6] 修改種類或名稱")));

        // Slot 46: 新增原版生物 (生怪蛋代表)
        inventory.setItem(46, createButton(
                Material.ZOMBIE_SPAWN_EGG,
                "&a➕ 新增原版生物",
                List.of(
                        "&7從 83 種預設原版生物中挑選加入",
                        "&7分類: 敵對(43)、中立(16)、被動友好(24)",
                        "&e點擊挑選"
                )
        ));

        // Slot 47: 新增 MythicMob 自訂怪物
        boolean mmEnabled = MythicMobHook.isEnabled();
        if (mmEnabled) {
            inventory.setItem(47, createButton(
                    Material.NETHER_STAR,
                    "&d➕ 新增 MythicMob 自訂怪物",
                    List.of(
                            "&7加入由 MythicMobs 插件定義的自訂怪物",
                            "&7支援瀏覽伺服器已註冊怪物或手動輸入 ID",
                            "&e點擊添加"
                    )
            ));
        } else {
            inventory.setItem(47, createButton(
                    Material.BARRIER,
                    "&8➕ 新增 MythicMob (未啟用)",
                    List.of(
                            "&c伺服器未安裝或未啟用 MythicMobs 插件",
                            "&7無法加入 MythicMob 自訂怪物"
                    )
            ));
        }

        // Slot 48: 自動均分機率
        inventory.setItem(48, createButton(
                Material.HOPPER,
                "&b⚖ 自動均分機率",
                List.of(
                        "&7將目前怪物池中所有怪物的機率平均分配",
                        "&7總和自動湊齊至 100.00%",
                        "&e點擊自動均分"
                )
        ));

        // Slot 49: 總機率狀態
        double totalChance = template.getTotalMobChance();
        boolean isValid = template.isTotalMobChanceValid();
        String totalTitle = isValid
                ? "&a✔ 怪物池總機率: 100.00%"
                : "&c✖ 怪物池總機率: " + String.format("%.2f%%", totalChance);
        List<String> totalLore;
        if (isValid) {
            totalLore = List.of(
                    "&7機率總和完全符合 100.00%",
                    "&a可前往下一步進行波次設定！"
            );
        } else {
            double diff = 100.0 - totalChance;
            String diffStr = (diff > 0 ? ("&e尚缺: &a+" + String.format("%.2f%%", diff)) : ("&c超出: &4-" + String.format("%.2f%%", Math.abs(diff))));
            totalLore = List.of(
                    "&7目前怪物池所有怪物機率總和:",
                    "&f" + String.format("%.2f%%", totalChance),
                    diffStr,
                    "&c必須正好等於 100.00% 才能前往下一步！"
            );
        }
        inventory.setItem(49, createButton(isValid ? Material.EMERALD : Material.REDSTONE, totalTitle, totalLore));

        // Slot 50: 籠內 3D 模型旋轉預覽 (支援固定各怪物與循環輪替)
        ItemStack previewButton;
        List<SpawnerMobEntry> mobList = template.getMobPool();
        if (template.isDisplayCycle()) {
            previewButton = createButton(
                    Material.CLOCK,
                    "&6籠內預覽模型: &b🔄 循環輪替",
                    List.of(
                            "&7生怪磚方塊內部將定時依序輪替展示怪物池所有生物",
                            "&7遇到 MythicMobs 生物時將自動以其基底生物顯示",
                            "&7當前狀態: &a【循環輪播】",
                            "&7",
                            "&e[點擊] &f切換為固定特定怪物"
                    )
            );
        } else {
            SpawnerMobEntry currentEntry = null;
            if (template.getDisplayMobId() != null) {
                for (SpawnerMobEntry e : mobList) {
                    if (e.getMobId().equalsIgnoreCase(template.getDisplayMobId())) {
                        currentEntry = e;
                        break;
                    }
                }
            }
            if (currentEntry == null && !mobList.isEmpty()) {
                currentEntry = mobList.get(0);
            }

            if (currentEntry != null) {
                EntityType baseType = currentEntry.getPreviewEntityType();
                String mobName = currentEntry.getDisplayName();
                Material icon = currentEntry.getIconMaterial();

                List<String> lore = new ArrayList<>();
                lore.add("&7生怪磚方塊內部將固定旋轉展示此模型");
                if (currentEntry.isMythic()) {
                    lore.add("&7自訂怪物: &d" + currentEntry.getMobId() + " &8(MythicMobs)");
                    lore.add("&7內部顯示基底生物: &f" + TextUtil.getMobDisplayName(baseType));
                } else {
                    lore.add("&7實體類型: &f" + baseType.name());
                }
                lore.add("&7當前狀態: &e【固定顯示】");
                lore.add("&7");
                lore.add("&e[點擊] &f切換怪物池中的下一隻怪物或【循環輪替】");

                String title = currentEntry.isMythic()
                        ? ("&6籠內預覽模型: &a" + mobName + " &7(基底: " + TextUtil.getMobDisplayName(baseType) + ")")
                        : ("&6籠內預覽模型: &a" + mobName);

                previewButton = createButton(icon != null ? icon : Material.SPAWNER, title, lore);
            } else {
                previewButton = createButton(Material.SPAWNER, "&6籠內預覽模型: &a" + TextUtil.getMobDisplayName(template.getSpawnedType()), List.of("&e[點擊] &f切換"));
            }
        }
        inventory.setItem(50, previewButton);

        // Slot 53: 下一步 (步驟三：波次與數量設定)
        if (isValid) {
            inventory.setItem(53, createButton(
                    Material.LIME_CONCRETE,
                    "&a下一步 ➜ &f(波次與數量設定)",
                    List.of(
                            "&7前往 [步驟 3/6] 設定生怪數量與間隔",
                            "&a點擊前往下一步"
                    )
            ));
        } else {
            inventory.setItem(53, createButton(
                    Material.RED_CONCRETE,
                    "&c下一步 ➜ &7(機率非 100%)",
                    List.of(
                            "&c怪物池所有怪物機率總和必須恰好為 100.00%",
                            "&7目前總和為 &f" + String.format("%.2f%%", totalChance),
                            "&e可使用【自動均分】或點擊單個怪物調整"
                    )
            ));
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

        // 點擊上方怪物展示區 (0 ~ 44)
        if (slot >= 0 && slot < ITEMS_PER_PAGE) {
            int index = page * ITEMS_PER_PAGE + slot;
            List<SpawnerMobEntry> mobs = context.getTemplate().getMobPool();
            if (index < mobs.size()) {
                SpawnerMobEntry target = mobs.get(index);
                if (event.isRightClick()) {
                    if (mobs.size() > 1) {
                        mobs.remove(index);
                        context.getPlugin().getConfigManager().playSound(player, "click");
                        render();
                    } else {
                        context.getPlugin().getConfigManager().playSound(player, "error");
                    }
                } else {
                    context.getPlugin().getConfigManager().playSound(player, "click");
                    new SpawnerMobChanceGui(context, target).open();
                }
            }
            return;
        }

        // 底部工具列 (45 ~ 53)
        switch (slot) {
            case 45 -> {
                // 返回步驟一
                context.getPlugin().getConfigManager().playSound(player, "click");
                new SpawnerWizardStep1Gui(context).open();
            }
            case 46 -> {
                // 新增原版怪物
                context.getPlugin().getConfigManager().playSound(player, "click");
                new SpawnerVanillaMobSelectGui(context, 1).open();
            }
            case 47 -> {
                // 新增 MythicMob
                if (MythicMobHook.isEnabled()) {
                    context.getPlugin().getConfigManager().playSound(player, "click");
                    new SpawnerMythicMobSelectGui(context, 1).open();
                } else {
                    player.sendMessage(TextUtil.parse("&c[CustomLootX] 伺服器未啟用 MythicMobs 插件！"));
                    context.getPlugin().getConfigManager().playSound(player, "error");
                }
            }
            case 48 -> {
                // 自動均分機率
                List<SpawnerMobEntry> mobs = context.getTemplate().getMobPool();
                if (!mobs.isEmpty()) {
                    distributeEvenly(mobs);
                    context.getPlugin().getConfigManager().playSound(player, "success");
                    render();
                } else {
                    context.getPlugin().getConfigManager().playSound(player, "error");
                }
            }
            case 50 -> {
                // 切換籠內 3D 預覽模型 (選項 0 ~ N-1 為怪物池中的怪物，選項 N 為 🔄 循環輪替)
                List<SpawnerMobEntry> mobs = context.getTemplate().getMobPool();
                if (!mobs.isEmpty()) {
                    int currentIndex = -1;
                    if (context.getTemplate().isDisplayCycle()) {
                        currentIndex = mobs.size(); // 當前為【循環輪替】
                    } else {
                        String currentId = context.getTemplate().getDisplayMobId();
                        if (currentId != null) {
                            for (int i = 0; i < mobs.size(); i++) {
                                if (mobs.get(i).getMobId().equalsIgnoreCase(currentId)) {
                                    currentIndex = i;
                                    break;
                                }
                            }
                        }
                        if (currentIndex == -1) {
                            for (int i = 0; i < mobs.size(); i++) {
                                if (mobs.get(i).getPreviewEntityType() == context.getTemplate().getSpawnedType()) {
                                    currentIndex = i;
                                    break;
                                }
                            }
                        }
                        if (currentIndex == -1) {
                            currentIndex = 0;
                        }
                    }

                    int totalOptions = mobs.size() + 1; // 所有怪物 + 1個循環選項
                    int nextIndex = (currentIndex + 1) % totalOptions;

                    if (nextIndex < mobs.size()) {
                        // 切換為指定怪物 (固定顯示)
                        SpawnerMobEntry nextEntry = mobs.get(nextIndex);
                        context.getTemplate().setDisplayCycle(false);
                        context.getTemplate().setDisplayMobId(nextEntry.getMobId());
                        context.getTemplate().setSpawnedType(nextEntry.getPreviewEntityType());
                    } else {
                        // 切換為【循環輪替】
                        context.getTemplate().setDisplayCycle(true);
                        context.getTemplate().setDisplayMobId("CYCLE");
                        context.getTemplate().setSpawnedType(mobs.get(0).getPreviewEntityType());
                    }

                    context.getPlugin().getConfigManager().playSound(player, "click");
                    render();
                }
            }
            case 53 -> {
                // 下一步 (步驟三：波次與數量設定)
                if (context.getTemplate().isTotalMobChanceValid()) {
                    context.getPlugin().getConfigManager().playSound(player, "click");
                    new SpawnerWizardStep3WavesGui(context).open();
                } else {
                    player.sendMessage(TextUtil.parse("&c[CustomLootX] 怪物池機率總和必須恰好為 100.00%！"));
                    context.getPlugin().getConfigManager().playSound(player, "error");
                }
            }
        }
    }

    private void distributeEvenly(List<SpawnerMobEntry> mobs) {
        int count = mobs.size();
        if (count == 0) return;

        double base = Math.floor((100.0 / count) * 100.0) / 100.0;
        double totalAssigned = base * count;
        double remainder = TextUtil.roundChance(100.0 - totalAssigned);

        for (int i = 0; i < count; i++) {
            double c = base;
            if (i == count - 1) {
                c = TextUtil.roundChance(base + remainder);
            }
            mobs.get(i).setChance(c);
        }
    }
}
