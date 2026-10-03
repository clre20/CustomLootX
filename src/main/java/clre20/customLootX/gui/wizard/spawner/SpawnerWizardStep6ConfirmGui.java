package clre20.customLootX.gui.wizard.spawner;

import clre20.customLootX.gui.CustomGuiHolder;
import clre20.customLootX.model.LootItem;
import clre20.customLootX.model.SpawnerTemplate;
import clre20.customLootX.model.VaultCooldownMode;
import clre20.customLootX.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 試煉生怪磚 [步驟 6/6] 設定確認與儲存介面
 */
public class SpawnerWizardStep6ConfirmGui extends CustomGuiHolder {

    private final SpawnerWizardContext context;

    public SpawnerWizardStep6ConfirmGui(SpawnerWizardContext context) {
        this.context = context;
        this.inventory = Bukkit.createInventory(this, 36, context.getPlugin().getConfigManager().getComponent("gui.spawner.step6.title", "&8[步驟 6/6] 設定確認與儲存"));
        render();
    }

    public void open() {
        context.getPlayer().openInventory(this.inventory);
    }

    private void render() {
        String fillerName = context.getPlugin().getConfigManager().getText("gui.common.filler-name", " ");
        ItemStack filler = createButton(Material.GRAY_STAINED_GLASS_PANE, fillerName, null);
        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, filler);
        }

        SpawnerTemplate template = context.getTemplate();

        // 第一排 (步驟一): 種類與名稱
        String typeDesc = template.isOminous() ? "&5不祥試煉生怪磚" : "&6普通試煉生怪磚";
        String step1TypeName = "&e【步驟一】種類: &f" + typeDesc;
        List<String> step1TypeLore = List.of(
                "&7目前選擇的生怪磚類型: &f" + typeDesc,
                "&7",
                "&e點擊可返回步驟一修改"
        );
        inventory.setItem(3, createButton(Material.TRIAL_SPAWNER, step1TypeName, step1TypeLore));

        String step1NameName = "&e【步驟一】配置名稱: &a" + template.getName();
        List<String> step1NameLore = List.of(
                "&7配置檔案路徑: &f/data/spawner/" + template.getName() + ".yml",
                "&7遊戲內標題已自動同步為: &f" + template.getDisplayName(),
                "&7",
                "&e點擊可返回步驟一修改"
        );
        inventory.setItem(5, createButton(Material.NAME_TAG, step1NameName, step1NameLore));

        // 第二排 (步驟二、三、四): 怪物、波次、冷卻
        List<clre20.customLootX.model.SpawnerMobEntry> mobs = template.getMobPool();
        String step2MobName;
        List<String> step2MobLore = new ArrayList<>();
        if (mobs.size() == 1) {
            clre20.customLootX.model.SpawnerMobEntry single = mobs.get(0);
            step2MobName = "&e【步驟二】生成怪物: &c" + single.getDisplayName();
            step2MobLore.add("&7代號: &f" + single.getMobId());
            step2MobLore.add("&7類型: " + (single.isMythic() ? "&dMythicMob 自訂怪物" : "&a原版生物"));
            step2MobLore.add("&7生成機率: &a100.00%");
        } else {
            step2MobName = "&e【步驟二】怪物池: &c" + mobs.size() + " 種怪物 (機率分配)";
            step2MobLore.add("&7怪物生成池分佈:");
            int showMobs = Math.min(mobs.size(), 4);
            for (int i = 0; i < showMobs; i++) {
                clre20.customLootX.model.SpawnerMobEntry entry = mobs.get(i);
                step2MobLore.add(" &7▪ &f" + entry.getDisplayName() + ": &a" + String.format("%.2f%%", entry.getChance()));
            }
            if (mobs.size() > 4) {
                step2MobLore.add(" &7▪ ...以及其他 &f" + (mobs.size() - 4) + " &7種怪物");
            }
        }
        if (template.isDisplayCycle()) {
            step2MobLore.add("&7生怪磚籠內 3D 旋轉預覽: &b🔄 循環輪替 &7(輪播怪物池所有生物)");
        } else {
            String mobName = TextUtil.getMobDisplayName(template.getSpawnedType());
            if (template.getDisplayMobId() != null) {
                for (clre20.customLootX.model.SpawnerMobEntry e : mobs) {
                    if (e.getMobId().equalsIgnoreCase(template.getDisplayMobId())) {
                        mobName = e.getDisplayName();
                        if (e.isMythic()) {
                            mobName += " &7(基底: " + TextUtil.getMobDisplayName(e.getPreviewEntityType()) + ")";
                        }
                        break;
                    }
                }
            }
            step2MobLore.add("&7生怪磚籠內 3D 旋轉預覽: &a" + mobName + " &e[固定顯示]");
        }
        step2MobLore.add("&7");
        step2MobLore.add("&e點擊可返回步驟二修改");
        inventory.setItem(11, createButton(Material.SPAWNER, step2MobName, step2MobLore));

        String step3WaveName = "&6【步驟三】波次配置";
        List<String> step3WaveLore = List.of(
                "&7總共擊殺目標: &a" + template.getTotalMobs() + " &7隻",
                "&7同時存活上限: &a" + template.getSimultaneousMobs() + " &7隻",
                "&7生成冷卻間隔: &a" + template.getSpawnDelaySeconds() + " &7秒",
                "&7玩家感應距離: &a" + template.getPlayerRange() + " &7格",
                "&7進度提示小字: " + (template.isShowActionBar() ? "&a✔ 開啟 (Action Bar)" : "&c✖ 關閉"),
                "&7挑戰完成音效: " + (template.isVictorySoundEnabled() ? ("&a✔ 開啟 (" + template.getVictorySound() + ")") : "&c✖ 關閉"),
                "&7",
                "&e點擊可返回步驟三修改"
        );
        inventory.setItem(13, createButton(Material.IRON_SWORD, step3WaveName, step3WaveLore));

        String modeDesc = template.getCooldownMode().getDisplay();
        String timeDesc = (template.getCooldownMode() == VaultCooldownMode.ONCE_PER_PLAYER)
                ? "&7(終生一次)"
                : ("&a" + template.getCooldownMinutes() + " &7分鐘");
        String step4CooldownName = "&6【步驟四】冷卻與出貨";
        List<String> step4CooldownLore = List.of(
                "&7冷卻模式: &e" + modeDesc,
                "&7冷卻時間: " + timeDesc,
                "&7獲勝出貨數量: &a" + template.getRollCount() + " &7件",
                "&7",
                "&e點擊可返回步驟四修改"
        );
        inventory.setItem(15, createButton(Material.GOLD_INGOT, step4CooldownName, step4CooldownLore));

        // 第三排 (步驟五): 掉落物展示 (Slot 19 ~ 25)
        List<LootItem> items = template.getRewards();
        if (items.isEmpty()) {
            inventory.setItem(22, createButton(Material.BARRIER, "&c【步驟五】尚未設定任何獲勝獎勵", List.of("&e點擊此處返回步驟五添加物品")));
        } else {
            int displayCount = Math.min(items.size(), 7);
            boolean hasMore = items.size() > 7;
            int limit = hasMore ? 6 : displayCount;

            String airName = context.getPlugin().getConfigManager().getText("gui.step4.step3-air-name", "&c[無掉落]");

            for (int i = 0; i < limit; i++) {
                LootItem li = items.get(i);
                ItemStack it;
                String chanceStr = TextUtil.formatPercent(li.getChance());

                if (li.isAir() || li.getItem() == null) {
                    List<String> airLore = List.of(
                            "&8------------------------",
                            "&e機率: &a" + chanceStr,
                            "&7點擊可返回步驟五調整"
                    );
                    it = createButton(Material.BARRIER, airName, airLore);
                } else {
                    it = li.getItem().clone();
                    ItemMeta meta = it.getItemMeta();
                    if (meta != null) {
                        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                        lore.add(TextUtil.parse("&8------------------------"));
                        lore.add(TextUtil.parse("&e機率: &a" + chanceStr));
                        lore.add(TextUtil.parse("&7點擊可返回步驟五調整"));
                        meta.lore(lore);
                        it.setItemMeta(meta);
                    }
                }
                inventory.setItem(19 + i, it);
            }

            if (hasMore) {
                int moreCount = items.size() - 6;
                List<String> moreLore = List.of(
                        "&7包含落空在內共 &f" + items.size() + " &7項設定",
                        "&7總機率恰好等於 &a100.00%",
                        "&e點擊可返回步驟五查看完整清單"
                );
                inventory.setItem(25, createButton(Material.CHEST, "&e...以及其他 &a" + moreCount + " &e項獎勵", moreLore));
            }
        }

        // 第四排: 返回 (Slot 27) 與 確認送出 (Slot 31)
        String backName = context.getPlugin().getConfigManager().getText("gui.common.back-to-step-name", "&e⬅ 上一步 &f(獲勝獎勵掉落池)");
        List<String> backLore = context.getPlugin().getConfigManager().getStringList("gui.common.back-to-step-lore", List.of("&7返回前一步驟修改機率或物品"));
        inventory.setItem(27, createButton(Material.ARROW, backName, backLore));

        boolean isRewardsValid = template.isTotalChanceValid();
        boolean isMobsValid = template.isTotalMobChanceValid();
        boolean isValid = isRewardsValid && isMobsValid;

        if (isValid) {
            String confirmName = context.getPlugin().getConfigManager().getText("gui.spawner.step6.confirm-name", "&a✔ 確認儲存並取得生怪磚");
            List<String> confirmLore = context.getPlugin().getConfigManager().getStringList(
                    "gui.spawner.step6.confirm-lore",
                    List.of(
                            "&7儲存所有設定至配置檔案",
                            "&7手持物品將自動轉化為成品生怪磚方塊",
                            "&a點擊立即儲存！"
                    )
            );
            inventory.setItem(31, createButton(Material.EMERALD_BLOCK, confirmName, confirmLore));
        } else {
            String notReadyName = "&c✖ 無法儲存 (機率未平衡至 100%)";
            List<String> notReadyLore = new ArrayList<>();
            if (!isMobsValid) {
                notReadyLore.add("&c【步驟二】怪物池總機率: &e" + String.format("%.2f%%", template.getTotalMobChance()) + " &7(必須為 100.00%)");
            }
            if (!isRewardsValid) {
                notReadyLore.add("&c【步驟五】獎勵池總機率: &e" + String.format("%.2f%%", template.getTotalChance()) + " &7(必須為 100.00%)");
            }
            notReadyLore.add("&7請點擊對應步驟使用【自動均分】或調整");
            inventory.setItem(31, createButton(Material.REDSTONE_BLOCK, notReadyName, notReadyLore));
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

        if (slot == 3 || slot == 5) {
            context.getPlugin().getConfigManager().playSound(player, "click");
            new SpawnerWizardStep1Gui(context).open();
        } else if (slot == 11) {
            context.getPlugin().getConfigManager().playSound(player, "click");
            new SpawnerWizardStep2MobGui(context, 1).open();
        } else if (slot == 13) {
            context.getPlugin().getConfigManager().playSound(player, "click");
            new SpawnerWizardStep3WavesGui(context).open();
        } else if (slot == 15) {
            context.getPlugin().getConfigManager().playSound(player, "click");
            new SpawnerWizardStep4CooldownGui(context).open();
        } else if ((slot >= 19 && slot <= 25) || slot == 27) {
            context.getPlugin().getConfigManager().playSound(player, "click");
            new SpawnerWizardStep5LootGui(context, 1).open();
        } else if (slot == 31) {
            // 確認送出與儲存
            SpawnerTemplate template = context.getTemplate();
            if (!template.isTotalMobChanceValid()) {
                player.sendMessage(TextUtil.parse("&c[CustomLootX] 怪物池機率總和必須恰好為 100.00%！"));
                context.getPlugin().getConfigManager().playSound(player, "error");
                return;
            }
            if (!template.isTotalChanceValid()) {
                context.getPlugin().getConfigManager().send(player, "sum-not-100");
                context.getPlugin().getConfigManager().playSound(player, "error");
                return;
            }

            boolean saved = context.getPlugin().getSpawnerTemplateManager().saveTemplate(template);
            if (saved) {
                // 更新玩家手持物品
                ItemStack held = player.getInventory().getItemInMainHand();
                if (context.getPlugin().getItemManager().isCustomSpawnerItem(held)) {
                    int amount = held.getAmount();
                    ItemStack configured = context.getPlugin().getItemManager().createTemplateSpawnerItem(template, amount);
                    player.getInventory().setItemInMainHand(configured);
                } else {
                    ItemStack configured = context.getPlugin().getItemManager().createTemplateSpawnerItem(template, 1);
                    player.getInventory().addItem(configured);
                }

                // 同步線上玩家與世界上已放置的同名方塊
                context.getPlugin().getItemManager().updateAllOnlinePlayersSpawnerItems(template);
                context.getPlugin().getSpawnerTemplateManager().updatePlacedSpawnerBlocks(template);

                context.getPlugin().getConfigManager().send(player, "spawner-save-success", "%name%", template.getName());
                context.getPlugin().getConfigManager().playSound(player, "save");

                player.spawnParticle(Particle.FIREWORK, player.getLocation().add(0, 1.5, 0), 30, 0.5, 0.5, 0.5, 0.1);

                player.closeInventory();
            } else {
                context.getPlugin().getConfigManager().send(player, "template-save-failed");
                context.getPlugin().getConfigManager().playSound(player, "error");
            }
        }
    }
}
