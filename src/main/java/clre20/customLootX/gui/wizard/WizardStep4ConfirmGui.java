package clre20.customLootX.gui.wizard;

import clre20.customLootX.gui.CustomGuiHolder;
import clre20.customLootX.model.LootItem;
import clre20.customLootX.model.LootTemplate;
import clre20.customLootX.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class WizardStep4ConfirmGui extends CustomGuiHolder {

    private final WizardContext context;

    public WizardStep4ConfirmGui(WizardContext context) {
        this.context = context;
        this.inventory = Bukkit.createInventory(this, 36, context.getPlugin().getConfigManager().getComponent("gui.step4.title", "&8設定確認"));
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

        LootTemplate template = context.getTemplate();

        // 第一排 (步驟一): 材質與名稱
        Material type = template.getType();
        String typeDesc = (type == Material.SUSPICIOUS_SAND)
                ? context.getPlugin().getConfigManager().getText("items.configured.type-sand", "可疑沙")
                : context.getPlugin().getConfigManager().getText("items.configured.type-gravel", "可疑礫石");

        String step1TypeName = context.getPlugin().getConfigManager().getText("gui.step4.step1-type-name", "&e【步驟一】方塊材質: &f%type%", "%type%", typeDesc);
        List<String> step1TypeLore = context.getPlugin().getConfigManager().getStringList(
                "gui.step4.step1-type-lore",
                List.of(
                        "&7目前選擇的方塊類型: &f%type%",
                        "&7",
                        "&e點擊可返回步驟一修改"
                ),
                "%type%", typeDesc
        );
        inventory.setItem(3, createButton(type, step1TypeName, step1TypeLore));

        String step1NameName = context.getPlugin().getConfigManager().getText("gui.step4.step1-name-name", "&e【步驟一】配置名稱: &a%name%", "%name%", template.getName());
        List<String> step1NameLore = context.getPlugin().getConfigManager().getStringList(
                "gui.step4.step1-name-lore",
                List.of(
                        "&7配置檔案路徑: &f/data/suspicious/%name%.yml",
                        "&7遊戲內標題已自動同步為: &f%display%",
                        "&7",
                        "&e點擊可返回步驟一修改"
                ),
                "%name%", template.getName(),
                "%display%", template.getDisplayName()
        );
        inventory.setItem(5, createButton(Material.NAME_TAG, step1NameName, step1NameLore));

        // 第二排 (步驟二): 重置功能與時間
        boolean resetOn = template.isResetEnabled();
        String resetStatusStr = resetOn
                ? context.getPlugin().getConfigManager().getText("gui.step2.status-on", "&a✔ 開啟")
                : context.getPlugin().getConfigManager().getText("gui.step2.status-off", "&c✖ 關閉");

        String step2ResetName = context.getPlugin().getConfigManager().getText("gui.step4.step2-reset-name", "&6【步驟二】重置模式: %status%", "%status%", resetStatusStr);
        List<String> step2ResetLore = context.getPlugin().getConfigManager().getStringList(
                "gui.step4.step2-reset-lore",
                List.of(
                        "&7自動重置狀態: %status%",
                        "&7(獨立設定儲存於 YML)",
                        "&7",
                        "&e點擊可返回步驟二修改"
                ),
                "%status%", resetStatusStr
        );
        String iconOnStr = context.getPlugin().getConfigManager().getText("gui.step2.icon-on", "LIME_DYE");
        String iconOffStr = context.getPlugin().getConfigManager().getText("gui.step2.icon-off", "RED_DYE");
        Material matOn = Material.matchMaterial(iconOnStr);
        if (matOn == null) matOn = Material.LIME_DYE;
        Material matOff = Material.matchMaterial(iconOffStr);
        if (matOff == null) matOff = Material.RED_DYE;

        inventory.setItem(12, createButton(resetOn ? matOn : matOff, step2ResetName, step2ResetLore));

        String step2TimeName = context.getPlugin().getConfigManager().getText("gui.step4.step2-time-name", "&6【步驟二】間隔時間: &a%minutes% &7分鐘", "%minutes%", template.getResetMinutes());
        List<String> step2TimeLore = context.getPlugin().getConfigManager().getStringList(
                "gui.step4.step2-time-lore",
                List.of(
                        "&7方塊被刷空後的自動重置冷卻時間",
                        "&7目前設定: &a%minutes% &7分鐘",
                        "&7",
                        "&e點擊可返回步驟二修改"
                ),
                "%minutes%", template.getResetMinutes()
        );
        inventory.setItem(14, createButton(Material.CLOCK, step2TimeName, step2TimeLore));

        // 第三排 (步驟三): 掉落物清單 (最多排 9 個)
        List<LootItem> items = template.getItems();
        if (items.isEmpty()) {
            String emptyName = context.getPlugin().getConfigManager().getText("gui.step4.step3-empty-name", "&c【步驟三】尚未設定任何掉落物");
            List<String> emptyLore = context.getPlugin().getConfigManager().getStringList("gui.step4.step3-empty-lore", List.of("&e點擊此處返回步驟三添加物品"));
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

                if (li.isAir()) {
                    List<String> airLore = context.getPlugin().getConfigManager().getStringList(
                            "gui.step4.step3-air-lore",
                            List.of(
                                    "&8------------------------",
                                    "&e機率: &a%chance%",
                                    "&7點擊可返回步驟三調整"
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
                                        "&7點擊可返回步驟三調整"
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
                                "&e點擊此處返回步驟三查看完整清單"
                        ),
                        "%total%", items.size()
                );
                inventory.setItem(26, createButton(Material.CHEST, moreName, moreLore));
            }
        }

        // 第四排: 底部導航
        // Slot 27 (左下角): 回上一頁
        String backName = context.getPlugin().getConfigManager().getText("gui.common.back-to-step3-name", "&e⬅ 上一步 (掉落池)");
        List<String> backLore = context.getPlugin().getConfigManager().getStringList("gui.common.back-to-step3-lore", List.of("&7返回 [步驟 3/4] 調整物品與機率"));
        inventory.setItem(27, createButton(Material.ARROW, backName, backLore));

        // Slot 31 (中間底部): 放棄變更/清除草稿
        String abandonName = "&c✖ 放棄變更並關閉";
        List<String> abandonLore = List.of(
                "&7若有暫存草稿，將自動清除",
                "&7手持物品將還原為原始狀態",
                "&c點擊放棄本次所有編輯"
        );
        inventory.setItem(31, createButton(Material.BARRIER, abandonName, abandonLore));

        // Slot 35 (右下角): 確認送出
        String confirmName = context.getPlugin().getConfigManager().getText("gui.step4.confirm-button-name", "&a✔ 確認送出並儲存");
        List<String> confirmLore = context.getPlugin().getConfigManager().getStringList(
                "gui.step4.confirm-button-lore",
                List.of(
                        "&7點擊後將正式儲存至 YML 檔案",
                        "&e手持的方塊將寫入上述所有屬性！",
                        "&a完成後即可放置在地上使用，或手持點擊進行再次編輯"
                )
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
        int slot = event.getRawSlot();
        Player player = context.getPlayer();
        LootTemplate template = context.getTemplate();

        // Step 1 slots
        if (slot == 3 || slot == 5) {
            context.getPlugin().getConfigManager().playSound(player, "click");
            context.openStep1();
            return;
        }

        // Step 2 slots
        if (slot == 12 || slot == 14) {
            context.getPlugin().getConfigManager().playSound(player, "click");
            context.openStep2();
            return;
        }

        // Step 3 slots or Back button
        if ((slot >= 18 && slot <= 26) || slot == 27) {
            context.getPlugin().getConfigManager().playSound(player, "click");
            context.openStep3();
            return;
        }

        // Slot 31: Abandon draft
        if (slot == 31) {
            context.setDraftAbandoned(true);
            if (context.getDraftId() != null) {
                context.getPlugin().getDraftManager().deleteDraft(clre20.customLootX.model.DraftType.SUSPICIOUS, context.getDraftId());
            }
            ItemStack held = context.getItemInHand();
            if (held == null || held.getType().isAir()) {
                held = player.getInventory().getItemInMainHand();
            }
            context.getPlugin().getItemManager().removeDraft(held);
            String origName = context.getOriginalName();
            if (origName != null && context.getPlugin().getTemplateManager().hasTemplate(origName)) {
                LootTemplate origT = context.getPlugin().getTemplateManager().getTemplate(origName);
                context.getPlugin().getItemManager().updateHoldingItem(player, origT);
            } else {
                ItemStack blank = context.getPlugin().getItemManager().createBlankItem(context.getTemplate().getType());
                player.getInventory().setItemInMainHand(blank);
            }
            context.getPlugin().getConfigManager().send(player, "save-cancelled");
            context.getPlugin().getConfigManager().playSound(player, "click");
            player.closeInventory();
            return;
        }

        // Slot 35: Confirm & Save
        if (slot == 35) {
            if (!template.isValidTotal()) {
                double total = template.getTotalChance();
                double diff = 100.00 - total;
                context.getPlugin().getConfigManager().send(player, "sum-not-100",
                        "%total%", TextUtil.formatPercent(total),
                        "%diff%", (diff > 0 ? "+" : "") + TextUtil.formatPercent(diff)
                );
                context.getPlugin().getConfigManager().playSound(player, "error");
                return;
            }

            // If renamed from an old name, remove the old file
            String origName = context.getOriginalName();
            if (origName != null && !origName.equalsIgnoreCase(template.getName())) {
                context.getPlugin().getTemplateManager().deleteTemplate(origName);
            }

            boolean saved = context.getPlugin().getTemplateManager().saveTemplate(template);
            if (saved) {
                context.setSavedSuccessfully(true);
                // 刪除草稿檔案
                if (context.getDraftId() != null) {
                    context.getPlugin().getDraftManager().deleteDraft(clre20.customLootX.model.DraftType.SUSPICIOUS, context.getDraftId());
                }
                // 移除手持物品草稿標記並更新正式 Lore
                ItemStack held = player.getInventory().getItemInMainHand();
                context.getPlugin().getItemManager().removeDraft(held);
                context.getPlugin().getItemManager().updateHoldingItem(player, template);
                context.getPlugin().getItemManager().updateAllOnlinePlayersItems(template);
                context.getPlugin().getConfigManager().send(player, "save-success", "%name%", template.getName());

                // 播放統一儲存音效 (config.yml sound.save)
                context.getPlugin().getConfigManager().playSound(player, "save");

                // Console log for auditing
                String typeDesc = template.getType() == Material.SUSPICIOUS_SAND
                        ? context.getPlugin().getConfigManager().getText("items.configured.type-sand", "可疑沙")
                        : context.getPlugin().getConfigManager().getText("items.configured.type-gravel", "可疑礫石");
                String resetDesc = template.isResetEnabled() ? (template.getResetMinutes() + "分鐘") : "關閉";

                context.getPlugin().getConfigManager().log("gui-save",
                        "%player%", player.getName(),
                        "%name%", template.getName(),
                        "%type%", typeDesc,
                        "%reset%", resetDesc,
                        "%count%", template.getItems().size(),
                        "%chance%", TextUtil.formatPercent(template.getTotalChance())
                );

                player.closeInventory();
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
