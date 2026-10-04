package clre20.customLootX.gui.wizard.vault;

import clre20.customLootX.gui.CustomGuiHolder;
import clre20.customLootX.model.LootItem;
import clre20.customLootX.model.VaultCooldownMode;
import clre20.customLootX.model.VaultTemplate;
import clre20.customLootX.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 試煉寶庫 [步驟 5/5] 設定確認與儲存介面
 */
public class VaultWizardStep5ConfirmGui extends CustomGuiHolder {

    private final VaultWizardContext context;

    public VaultWizardStep5ConfirmGui(VaultWizardContext context) {
        this.context = context;
        this.inventory = Bukkit.createInventory(this, 36, context.getPlugin().getConfigManager().getComponent("gui.vault.step5.title", "&8[步驟 5/5] 設定確認與儲存"));
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

        VaultTemplate template = context.getTemplate();

        // 第一排 (步驟一): 寶庫材質與名稱
        String typeDesc = template.isOminous() ? "&5不祥試煉寶庫" : "&6普通試煉寶庫";
        String step1TypeName = context.getPlugin().getConfigManager().getText("gui.vault.step5.step1-type-name", "&e【步驟一】寶庫類型: &f%type%", "%type%", typeDesc);
        List<String> step1TypeLore = context.getPlugin().getConfigManager().getStringList(
                "gui.vault.step4.step1-type-lore",
                List.of(
                        "&7目前選擇的寶庫類型: &f%type%",
                        "&7",
                        "&e點擊可返回步驟一修改"
                ),
                "%type%", typeDesc
        );
        inventory.setItem(3, createButton(template.isOminous() ? Material.OMINOUS_TRIAL_KEY : Material.TRIAL_KEY, step1TypeName, step1TypeLore));

        String step1NameName = context.getPlugin().getConfigManager().getText("gui.vault.step5.step1-name-name", "&e【步驟一】配置名稱: &a%name%", "%name%", template.getName());
        List<String> step1NameLore = context.getPlugin().getConfigManager().getStringList(
                "gui.vault.step4.step1-name-lore",
                List.of(
                        "&7配置檔案路徑: &f/data/vault/%name%.yml",
                        "&7遊戲內標題已自動同步為: &f%display%",
                        "&7",
                        "&e點擊可返回步驟一修改"
                ),
                "%name%", template.getName(),
                "%display%", template.getDisplayName()
        );
        inventory.setItem(5, createButton(Material.NAME_TAG, step1NameName, step1NameLore));

        // 第二排 (步驟二 & 步驟三): 鑰匙設定、冷卻模式與出貨數量
        ItemStack keyItem = template.getKeyItem();
        String keyName = (keyItem != null && keyItem.getItemMeta() != null && keyItem.getItemMeta().hasDisplayName())
                ? TextUtil.toLegacyText(keyItem.getItemMeta().displayName())
                : (template.isOminous() ? "不祥試煉鑰匙" : "試煉鑰匙");
        Material keyMat = (keyItem != null) ? keyItem.getType() : Material.TRIAL_KEY;

        String step2KeyName = context.getPlugin().getConfigManager().getText("gui.vault.step5.step2-key-name", "&6【步驟二】解鎖鑰匙: &e%key%", "%key%", keyName);
        List<String> step2KeyLore = context.getPlugin().getConfigManager().getStringList(
                "gui.vault.step4.step2-key-lore",
                List.of(
                        "&7材質: &f%material%",
                        "&7具備 NBT / PDC 防偽比對",
                        "&7",
                        "&e點擊可返回步驟二修改"
                ),
                "%material%", keyMat.name()
        );
        inventory.setItem(11, createButton(keyMat, step2KeyName, step2KeyLore));

        String modeDesc = template.getCooldownMode().getDisplay();
        String timeDesc = (template.getCooldownMode() == VaultCooldownMode.ONCE_PER_PLAYER)
                ? "&7(終生一次)"
                : ("&a" + template.getCooldownMinutes() + " &7分鐘");
        String step3ModeName = context.getPlugin().getConfigManager().getText("gui.vault.step5.step3-mode-name", "&6【步驟三】冷卻機制: &e%mode%", "%mode%", modeDesc);
        List<String> step3ModeLore = context.getPlugin().getConfigManager().getStringList(
                "gui.vault.step4.step2-mode-lore",
                List.of(
                        "&7冷卻模式: &e%mode%",
                        "&7間隔時間: %time%",
                        "&7",
                        "&e點擊可返回步驟三修改"
                ),
                "%mode%", modeDesc,
                "%time%", timeDesc
        );
        inventory.setItem(13, createButton(Material.COMPASS, step3ModeName, step3ModeLore));

        String step3RollName = context.getPlugin().getConfigManager().getText("gui.vault.step5.step3-roll-name", "&6【步驟三】每次出貨: &a%rolls% &f件", "%rolls%", String.valueOf(template.getRollCount()));
        List<String> step3RollLore = context.getPlugin().getConfigManager().getStringList(
                "gui.vault.step4.step2-roll-lore",
                List.of(
                        "&7開鎖成功時彈出的物品數量: &a%rolls% &7件",
                        "&7",
                        "&e點擊可返回步驟三修改"
                ),
                "%rolls%", String.valueOf(template.getRollCount())
        );
        inventory.setItem(15, createButton(Material.GOLD_INGOT, step3RollName, step3RollLore));

        // 第三排 (步驟四): 掉落池物品展示 (Slot 18 ~ 26)
        List<LootItem> items = template.getItems();
        if (items.isEmpty()) {
            String emptyName = context.getPlugin().getConfigManager().getText("gui.vault.step5.step4-empty-name", "&c【步驟四】尚未設定任何掉落物");
            List<String> emptyLore = context.getPlugin().getConfigManager().getStringList("gui.step4.step3-empty-lore", List.of("&e點擊此處返回步驟四添加物品"));
            inventory.setItem(22, createButton(Material.BARRIER, emptyName, emptyLore));
        } else {
            int displayCount = Math.min(items.size(), 9);
            boolean hasMore = items.size() > 9;
            int limit = hasMore ? 8 : displayCount;

            String airName = context.getPlugin().getConfigManager().getText("gui.step4.step3-air-name", "&c[無掉落]");

            for (int i = 0; i < limit; i++) {
                LootItem li = items.get(i);
                ItemStack it;
                String chanceStr = TextUtil.formatPercent(li.getChance());

                if (li.isAir() || li.getItem() == null) {
                    List<String> airLore = context.getPlugin().getConfigManager().getStringList(
                            "gui.step4.step3-air-lore",
                            List.of(
                                    "&8------------------------",
                                    "&e機率: &a%chance%",
                                    "&7點擊可返回步驟四調整"
                            ),
                            "%chance%", chanceStr
                    );
                    it = createButton(Material.BARRIER, airName, airLore);
                } else {
                    it = li.getItem().clone();
                    ItemMeta meta = it.getItemMeta();
                    if (meta != null) {
                        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                        List<Component> extra = context.getPlugin().getConfigManager().getComponentList(
                                "gui.step4.step3-item-lore",
                                List.of(
                                        "",
                                        "&8------------------------",
                                        "&e機率: &a%chance%",
                                        "&7點擊可返回步驟四調整"
                                ),
                                "%chance%", chanceStr
                        );
                        lore.addAll(extra);
                        meta.lore(lore);
                        it.setItemMeta(meta);
                    }
                }
                inventory.setItem(18 + i, it);
            }

            if (hasMore) {
                int moreCount = items.size() - 8;
                String moreName = context.getPlugin().getConfigManager().getText("gui.step4.step3-more-name", "&b...以及其他 %count% 項物品", "%count%", moreCount);
                List<String> moreLore = context.getPlugin().getConfigManager().getStringList(
                        "gui.step4.step3-more-lore",
                        List.of(
                                "&7獎池共包含 &e%total% &7種掉落項目",
                                "&e點擊此處返回步驟四查看完整清單"
                        ),
                        "%total%", items.size()
                );
                inventory.setItem(26, createButton(Material.CHEST, moreName, moreLore));
            }
        }

        // 第四排: 底部導航
        // Slot 27 (左下角): 回步驟四
        String backName = context.getPlugin().getConfigManager().getText("gui.vault.step5.back-to-step4-name", "&e⬅ 上一步 (掉落池)");
        List<String> backLore = context.getPlugin().getConfigManager().getStringList("gui.vault.step5.back-to-step4-lore", List.of("&7返回 [步驟 4/5] 調整物品與機率"));
        inventory.setItem(27, createButton(Material.ARROW, backName, backLore));

        // Slot 31 (中間底部): 放棄變更/清除草稿
        String abandonName = "&c✖ 放棄變更並關閉";
        List<String> abandonLore = List.of(
                "&7若有暫存草稿，將自動清除",
                "&7手持物品將還原為原始狀態",
                "&c點擊放棄本次所有編輯"
        );
        inventory.setItem(31, createButton(Material.BARRIER, abandonName, abandonLore));

        // Slot 35 (右下角): 確認送出儲存
        String confirmName = context.getPlugin().getConfigManager().getText("gui.step4.confirm-button-name", "&a✔ 確認送出並儲存");
        List<String> confirmLore = context.getPlugin().getConfigManager().getStringList(
                "gui.vault.step4.confirm-button-lore",
                List.of(
                        "&7確認所有設定無誤後點擊送出",
                        "&a✔ 檔案將儲存至 /data/vault/%name%.yml",
                        "&a✔ 手持的物品將立即更新所有屬性與外觀",
                        "&a✔ 已放置於世界的方塊將同步更新",
                        "&a✔ 儲存後即可直接放置於世界上"
                ),
                "%name%", template.getName()
        );
        ItemStack confirmBtn = createButton(Material.EMERALD_BLOCK, confirmName, confirmLore);
        ItemMeta cm = confirmBtn.getItemMeta();
        if (cm != null) {
            cm.addEnchant(Enchantment.UNBREAKING, 1, true);
            cm.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            confirmBtn.setItemMeta(cm);
        }
        inventory.setItem(35, confirmBtn);
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
            // 返回步驟一
            context.getPlugin().getConfigManager().playSound(player, "click");
            context.setTransitioning(true);
            new VaultWizardStep1Gui(context).open();
            context.setTransitioning(false);
        } else if (slot == 11) {
            // 返回步驟二 (專屬鑰匙設定)
            context.getPlugin().getConfigManager().playSound(player, "click");
            context.setTransitioning(true);
            new VaultKeySetupGui(context).open();
            context.setTransitioning(false);
        } else if (slot == 13 || slot == 15) {
            // 返回步驟三 (冷卻與出貨設定)
            context.getPlugin().getConfigManager().playSound(player, "click");
            context.setTransitioning(true);
            new VaultWizardStep3CooldownGui(context).open();
            context.setTransitioning(false);
        } else if (slot >= 18 && slot <= 27) {
            // 返回步驟四 (掉落池)
            context.getPlugin().getConfigManager().playSound(player, "click");
            context.setTransitioning(true);
            new VaultWizardStep4LootGui(context, 1).open();
            context.setTransitioning(false);
        } else if (slot == 31) {
            // 放棄變更並清除草稿
            context.setDraftAbandoned(true);
            if (context.getDraftId() != null) {
                context.getPlugin().getDraftManager().deleteDraft(clre20.customLootX.model.DraftType.VAULT, context.getDraftId());
            }
            ItemStack held = context.getItemInHand();
            if (held == null || held.getType().isAir()) {
                held = player.getInventory().getItemInMainHand();
            }
            context.getPlugin().getItemManager().removeDraft(held);
            String origName = context.getOriginalName();
            if (origName != null && context.getPlugin().getVaultTemplateManager().getTemplate(origName) != null) {
                VaultTemplate origT = context.getPlugin().getVaultTemplateManager().getTemplate(origName);
                context.getPlugin().getItemManager().updatePlayerHeldVaultItem(player, origT);
            } else {
                ItemStack blank = context.getPlugin().getItemManager().createBlankVaultItem(context.getTemplate().isOminous());
                player.getInventory().setItemInMainHand(blank);
            }
            context.getPlugin().getConfigManager().send(player, "save-cancelled");
            context.getPlugin().getConfigManager().playSound(player, "click");
            player.closeInventory();
        } else if (slot == 35) {
            // 確認送出
            VaultTemplate template = context.getTemplate();

            // 1. 驗證名稱
            if (template.getName() == null || template.getName().trim().isEmpty() || template.getName().equalsIgnoreCase("<未設定>")) {
                context.getPlugin().getConfigManager().send(player, "name-not-set");
                context.getPlugin().getConfigManager().playSound(player, "error");
                return;
            }

            // 2. 驗證機率總和
            if (!template.isTotalChanceValid()) {
                double total = template.getTotalChance();
                double diff = Math.abs(100.0 - total);
                context.getPlugin().getConfigManager().send(player, "sum-not-100",
                        "%total%", String.format("%.2f%%", total),
                        "%diff%", String.format("%.2f%%", diff));
                context.getPlugin().getConfigManager().playSound(player, "error");
                return;
            }

            // 3. 若有更名且為舊配置，刪除舊的配置檔
            String origName = context.getOriginalName();
            if (origName != null && !origName.equalsIgnoreCase(template.getName())) {
                context.getPlugin().getVaultTemplateManager().deleteTemplate(origName);
            }

            // 4. 儲存至 /data/vault/[name].yml
            boolean success = context.getPlugin().getVaultTemplateManager().saveTemplate(template);
            if (success) {
                context.setSavedSuccessfully(true);
                // 刪除草稿檔案
                if (context.getDraftId() != null) {
                    context.getPlugin().getDraftManager().deleteDraft(clre20.customLootX.model.DraftType.VAULT, context.getDraftId());
                }
                // 移除手持物品草稿標記並更新
                ItemStack held = player.getInventory().getItemInMainHand();
                context.getPlugin().getItemManager().removeDraft(held);
                context.getPlugin().getItemManager().updatePlayerHeldVaultItem(player, template);
                // 同步其他線上玩家
                context.getPlugin().getItemManager().updateAllOnlinePlayersVaultItems(template);
                // 同步已放置於世界上的同名寶庫方塊 (包含不祥模式切換)
                context.getPlugin().getVaultTemplateManager().updatePlacedVaultBlocks(template);

                context.getPlugin().getConfigManager().send(player, "save-success", "%name%", template.getName());
                context.getPlugin().getConfigManager().playSound(player, "save");
                player.closeInventory();

                context.getPlugin().getConfigManager().log("vault-save",
                        "%player%", player.getName(),
                        "%name%", template.getName(),
                        "%type%", template.isOminous() ? "OMINOUS" : "NORMAL",
                        "%mode%", template.getCooldownMode().name(),
                        "%rolls%", String.valueOf(template.getRollCount())
                );
            } else {
                context.getPlugin().getConfigManager().send(player, "save-failed");
                context.getPlugin().getConfigManager().playSound(player, "error");
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
