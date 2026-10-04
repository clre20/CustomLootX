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
 * 試煉生怪磚 [步驟 3/6] 怪物名單挑選介面 (純挑選生物，完全無機率邏輯)
 */
public class SpawnerWizardStep3MobGui extends CustomGuiHolder {

    private final SpawnerWizardContext context;
    private int page = 0;
    private static final int ITEMS_PER_PAGE = 45;
    private boolean deleteMode = false;

    public SpawnerWizardStep3MobGui(SpawnerWizardContext context, int pageNumber) {
        this.context = context;
        this.page = Math.max(0, pageNumber - 1);
        this.deleteMode = false;
        this.inventory = Bukkit.createInventory(
                this,
                54,
                context.getPlugin().getConfigManager().getComponent("gui.spawner.step3.mob-title", "&8[步驟 3/6] 怪物名單挑選")
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

        // 擺放當前頁面的怪物項目 (純展示與移除，不含機率)
        for (int i = startIndex; i < endIndex; i++) {
            SpawnerMobEntry mob = mobs.get(i);
            int slot = i - startIndex;

            ItemStack displayItem = new ItemStack(mob.getIconMaterial());
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                String title = (mob.isMythic() ? "&d[Mythic] &f" : "&a[原版] &f") + mob.getDisplayName();
                if (deleteMode) {
                    title = "&c[點擊刪除] " + title;
                }
                meta.displayName(TextUtil.parse(title));

                List<Component> lore = new ArrayList<>();
                lore.add(TextUtil.parse("&8------------------------"));
                lore.add(TextUtil.parse("&7內部代號: &f" + mob.getMobId()));
                lore.add(TextUtil.parse("&7怪物類型: " + (mob.isMythic() ? "&dMythicMob 自訂怪物" : "&a原版生物")));
                lore.add(TextUtil.parse("&7"));
                if (deleteMode) {
                    lore.add(TextUtil.parse("&c&l【刪除模式啟用中】"));
                    lore.add(TextUtil.parse("&e[左鍵點擊] &c立即從名單中移除此怪物！"));
                } else {
                    lore.add(TextUtil.parse("&7可在下一步將此生物排入生成順序中"));
                    lore.add(TextUtil.parse("&8(可點擊下方「開啟刪除模式」來移除此怪物)"));
                }
                if (mobs.size() == 1) {
                    lore.add(TextUtil.parse("&8(清單中至少需保留 1 種生物)"));
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

        // Slot 45: 返回步驟二 (波次與數量設定)
        inventory.setItem(45, createButton(
                Material.ARROW,
                "&e⬅ 上一步 &f(波次與數量設定)",
                List.of("&7返回 [步驟 2/6] 修改波次規模與間隔")
        ));

        // Slot 46: 新增原版生物
        inventory.setItem(46, createButton(
                Material.ZOMBIE_SPAWN_EGG,
                "&a➕ 新增原版生物",
                List.of(
                        "&7從 83 種預設原版生物中挑選加入候選名單",
                        "&7分類: 敵對(43)、中立(16)、被動友好(24)",
                        "&e點擊挑選加入"
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

        // Slot 48: 刪除模式按鈕 (點擊切換，全流程支援純左鍵操作)
        if (!deleteMode) {
            inventory.setItem(48, createButton(
                    Material.HOPPER,
                    "&c🗑 開啟刪除模式",
                    List.of(
                            "&7點擊開啟刪除模式",
                            "&7開啟後，可透過 &e左鍵點擊 &7上方生物直接從名單中移除",
                            "&7(全流程均可純靠左鍵完成)",
                            "&7",
                            "&7當前狀態: &8【已關閉】",
                            "&e[左鍵點擊] &c開啟刪除模式"
                    )
            ));
        } else {
            inventory.setItem(48, createButton(
                    Material.REDSTONE_BLOCK,
                    "&a✔ 關閉刪除模式",
                    List.of(
                            "&e當前已開啟刪除模式！",
                            "&c左鍵點擊上方任意怪物即可立即將其移除",
                            "&7",
                            "&7當前狀態: &c&l【開啟中 (點怪即刪)】",
                            "&e[左鍵點擊] &a關閉刪除模式 (恢復正常)"
                    )
            ));
        }

        // Slot 50: 籠內 3D 模型旋轉預覽
        ItemStack previewButton;
        List<SpawnerMobEntry> mobList = template.getMobPool();
        if (template.isDisplayCycle()) {
            previewButton = createButton(
                    Material.CLOCK,
                    "&6籠內預覽模型: &b🔄 循環輪替",
                    List.of(
                            "&7生怪磚方塊內部將定時依序輪替展示怪物名單所有生物",
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
                    lore.add("&7自訂怪物: &d" + currentEntry.getMobId());
                    lore.add("&7內部顯示基底生物: &f" + TextUtil.getMobDisplayName(baseType));
                } else {
                    lore.add("&7實體類型: &f" + baseType.name());
                }
                lore.add("&7當前狀態: &e【固定顯示】");
                lore.add("&7");
                lore.add("&e[點擊] &f切換名單中的下一隻怪物或【循環輪替】");

                String title = "&6籠內預覽模型: &a" + mobName;
                previewButton = createButton(icon != null ? icon : Material.SPAWNER, title, lore);
            } else {
                previewButton = createButton(Material.SPAWNER, "&6籠內預覽模型: &a" + TextUtil.getMobDisplayName(template.getSpawnedType()), List.of("&e[點擊] &f切換"));
            }
        }
        inventory.setItem(50, previewButton);

        // Slot 53: 下一步 (前往步驟四：怪物生成順序編排)
        inventory.setItem(53, createButton(
                Material.LIME_CONCRETE,
                "&a下一步 ➜ &f(怪物生成順序編排)",
                List.of(
                        "&7名單已就緒，前往 [步驟 4/6] 編排各波次怪物生成順序",
                        "&a點擊前往下一步"
                )
        ));
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
                if (deleteMode) {
                    // 刪除模式下：左鍵 (或任意點擊) 直接移除！
                    if (mobs.size() > 1) {
                        SpawnerMobEntry removed = mobs.remove(index);
                        player.sendMessage(TextUtil.parse("&c[CustomLootX] &7已從候選名單移除怪物: &f" + removed.getDisplayName()));
                        context.getPlugin().getConfigManager().playSound(player, "click");

                        // 若當前顯示模型是被刪除的生物，自動更新
                        SpawnerTemplate template = context.getTemplate();
                        if (!template.isDisplayCycle() && removed.getMobId().equalsIgnoreCase(template.getDisplayMobId())) {
                            template.setDisplayMobId(mobs.get(0).getMobId());
                            template.setSpawnedType(mobs.get(0).getPreviewEntityType());
                        }

                        // 若生成順序中有該怪物，將其改為 RANDOM
                        for (List<String> wave : template.getWaves()) {
                            if (wave != null) {
                                for (int s = 0; s < wave.size(); s++) {
                                    if (wave.get(s).equalsIgnoreCase(removed.getMobId()) || wave.get(s).equalsIgnoreCase("mm:" + removed.getMobId())) {
                                        wave.set(s, "RANDOM");
                                    }
                                }
                            }
                        }

                        render();
                    } else {
                        player.sendMessage(TextUtil.parse("&c[CustomLootX] 怪物名單中至少需保留 1 種生物！"));
                        context.getPlugin().getConfigManager().playSound(player, "error");
                    }
                } else {
                    // 非刪除模式：右鍵仍支援移除 (快捷鍵)
                    if (event.isRightClick()) {
                        if (mobs.size() > 1) {
                            SpawnerMobEntry removed = mobs.remove(index);
                            player.sendMessage(TextUtil.parse("&c[CustomLootX] &7已從候選名單移除怪物: &f" + removed.getDisplayName()));
                            context.getPlugin().getConfigManager().playSound(player, "click");

                            SpawnerTemplate template = context.getTemplate();
                            if (!template.isDisplayCycle() && removed.getMobId().equalsIgnoreCase(template.getDisplayMobId())) {
                                template.setDisplayMobId(mobs.get(0).getMobId());
                                template.setSpawnedType(mobs.get(0).getPreviewEntityType());
                            }

                            for (List<String> wave : template.getWaves()) {
                                if (wave != null) {
                                    for (int s = 0; s < wave.size(); s++) {
                                        if (wave.get(s).equalsIgnoreCase(removed.getMobId()) || wave.get(s).equalsIgnoreCase("mm:" + removed.getMobId())) {
                                            wave.set(s, "RANDOM");
                                        }
                                    }
                                }
                            }

                            render();
                        } else {
                            player.sendMessage(TextUtil.parse("&c[CustomLootX] 怪物名單中至少需保留 1 種生物！"));
                            context.getPlugin().getConfigManager().playSound(player, "error");
                        }
                    } else {
                        context.getPlugin().getConfigManager().playSound(player, "click");
                        player.sendMessage(TextUtil.parse("&7[CustomLootX] 提示: 若要移除此怪物，可點擊下方 &c[🗑 開啟刪除模式]&7！"));
                    }
                }
            }
            return;
        }

        // 底部工具列 (45 ~ 53)
        switch (slot) {
            case 45 -> {
                // 返回步驟二 (波次與數量設定)
                context.getPlugin().getConfigManager().playSound(player, "click");
                context.setTransitioning(true);
                new SpawnerWizardStep2WavesGui(context).open();
                context.setTransitioning(false);
            }
            case 46 -> {
                // 新增原版怪物
                context.getPlugin().getConfigManager().playSound(player, "click");
                context.setTransitioning(true);
                new SpawnerVanillaMobSelectGui(context, 1).open();
                context.setTransitioning(false);
            }
            case 47 -> {
                // 新增 MythicMob
                if (MythicMobHook.isEnabled()) {
                    context.getPlugin().getConfigManager().playSound(player, "click");
                    context.setTransitioning(true);
                    new SpawnerMythicMobSelectGui(context, 1).open();
                    context.setTransitioning(false);
                } else {
                    player.sendMessage(TextUtil.parse("&c[CustomLootX] 伺服器未啟用 MythicMobs 插件！"));
                    context.getPlugin().getConfigManager().playSound(player, "error");
                }
            }
            case 48 -> {
                // 切換刪除模式
                deleteMode = !deleteMode;
                context.getPlugin().getConfigManager().playSound(player, "click");
                render();
            }
            case 50 -> {
                // 切換籠內 3D 預覽模型
                List<SpawnerMobEntry> mobs = context.getTemplate().getMobPool();
                if (!mobs.isEmpty()) {
                    int currentIndex = -1;
                    if (context.getTemplate().isDisplayCycle()) {
                        currentIndex = mobs.size();
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

                    int totalOptions = mobs.size() + 1;
                    int nextIndex = (currentIndex + 1) % totalOptions;

                    if (nextIndex < mobs.size()) {
                        SpawnerMobEntry nextEntry = mobs.get(nextIndex);
                        context.getTemplate().setDisplayCycle(false);
                        context.getTemplate().setDisplayMobId(nextEntry.getMobId());
                        context.getTemplate().setSpawnedType(nextEntry.getPreviewEntityType());
                    } else {
                        context.getTemplate().setDisplayCycle(true);
                        context.getTemplate().setDisplayMobId("CYCLE");
                        context.getTemplate().setSpawnedType(mobs.get(0).getPreviewEntityType());
                    }

                    context.getPlugin().getConfigManager().playSound(player, "click");
                    render();
                }
            }
            case 53 -> {
                // 前往步驟四 (怪物生成順序編排)
                context.getPlugin().getConfigManager().playSound(player, "click");
                context.setTransitioning(true);
                new SpawnerWizardStep4SequenceGui(context).open();
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
}
