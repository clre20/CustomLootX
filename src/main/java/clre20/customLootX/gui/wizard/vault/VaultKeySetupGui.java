package clre20.customLootX.gui.wizard.vault;

import clre20.customLootX.gui.CustomGuiHolder;
import clre20.customLootX.model.VaultTemplate;
import clre20.customLootX.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 試煉寶庫鑰匙製作與自訂設定介面
 * 支援玩家直接在 Slot 13 放置任意物品作為鑰匙外觀與基底，
 * 同時提供自訂名稱、多行說明獨立編輯、製作防偽鑰匙領取與一鍵重置
 */
public class VaultKeySetupGui extends CustomGuiHolder {

    private final VaultWizardContext context;
    private String customKeyName = null;
    private final List<String> customKeyLore = new ArrayList<>();
    private ItemStack currentKeyItem = null;
    private boolean isPlayerPlacedItem = false;

    public VaultKeySetupGui(VaultWizardContext context) {
        this.context = context;
        this.inventory = Bukkit.createInventory(this, 27, context.getPlugin().getConfigManager().getComponent("gui.vault.step2.title", "&8[步驟 2/5] 專屬鑰匙製作與設定"));

        // 初始化目前設定的鑰匙
        ItemStack existing = context.getTemplate().getKeyItem();
        if (existing != null && !existing.getType().isAir()) {
            this.currentKeyItem = existing.clone();
            if (existing.hasItemMeta()) {
                ItemMeta meta = existing.getItemMeta();
                if (meta.hasDisplayName()) {
                    this.customKeyName = TextUtil.toLegacyText(meta.displayName());
                }
                if (meta.hasLore() && meta.lore() != null) {
                    for (Component c : meta.lore()) {
                        this.customKeyLore.add(TextUtil.toLegacyText(c));
                    }
                }
            }
        } else {
            this.currentKeyItem = new ItemStack(context.getTemplate().isOminous() ? Material.OMINOUS_TRIAL_KEY : Material.TRIAL_KEY);
        }

        render();
    }

    public VaultWizardContext getContext() {
        return context;
    }

    public List<String> getCustomKeyLore() {
        return customKeyLore;
    }

    public void open() {
        render();
        context.getPlayer().openInventory(this.inventory);
    }

    public void render() {
        String fillerName = context.getPlugin().getConfigManager().getText("gui.common.filler-name", " ");
        ItemStack filler = createButton(Material.GRAY_STAINED_GLASS_PANE, fillerName, null);
        for (int i = 0; i < 27; i++) {
            if (i != 13) {
                inventory.setItem(i, filler);
            }
        }

        VaultTemplate template = context.getTemplate();

        // Slot 4: 步驟導覽
        String guideTitle = "&e【步驟 2/5 鑰匙製作導覽】";
        List<String> guideLore = List.of(
                "&7在此步驟設定此寶庫的專屬解鎖鑰匙：",
                "&71. 在第 14 格放上任意基底物品 (可從背包放入或點選取出)",
                "&72. 點擊【設定名稱】或【設定說明】自訂外觀 (可選)",
                "&73. 點擊【🔨 製作防偽鑰匙】可直接領取 1 把正式鑰匙！",
                "&74. 確認無誤後點擊右下角【下一步】繼續設定冷卻與出貨數量"
        );
        inventory.setItem(4, createButton(Material.BOOK, guideTitle, guideLore));

        // Slot 11: 設定鑰匙名稱
        String nameBtnTitle = context.getPlugin().getConfigManager().getText("gui.vault.key-setup.name-btn-name", "&e設定鑰匙名稱");
        String nameDisplay = (customKeyName != null && !customKeyName.trim().isEmpty()) ? customKeyName : "&7(預設)";
        List<String> nameBtnLore = context.getPlugin().getConfigManager().getStringList(
                "gui.vault.key-setup.name-btn-lore",
                List.of(
                        "&7目前設定: &f%name%",
                        "&7",
                        "&e點擊此處 &f在聊天室自訂名稱",
                        "&8(支援彩色代碼如 &6古代鑰匙)"
                ),
                "%name%", nameDisplay
        );
        inventory.setItem(11, createButton(Material.NAME_TAG, nameBtnTitle, nameBtnLore));

        // Slot 12: 設定鑰匙說明 (Lore)
        String loreBtnTitle = context.getPlugin().getConfigManager().getText("gui.vault.key-setup.lore-btn-name", "&e設定鑰匙說明");
        List<String> loreBtnLore = context.getPlugin().getConfigManager().getStringList(
                "gui.vault.key-setup.lore-btn-lore",
                List.of(
                        "&7目前額外行數: &a%lines% &7行",
                        "&7",
                        "&e點擊此處 &f開啟說明管理介面",
                        "&8(可逐行檢視、修改第X行、刪除或新增)"
                ),
                "%lines%", String.valueOf(customKeyLore.size())
        );
        inventory.setItem(12, createButton(Material.WRITABLE_BOOK, loreBtnTitle, loreBtnLore));

        // Slot 13: 鑰匙物品放置格
        if (currentKeyItem != null && !currentKeyItem.getType().isAir()) {
            ItemStack display = currentKeyItem.clone();
            ItemMeta dm = display.getItemMeta();
            if (dm != null) {
                if (customKeyName != null && !customKeyName.trim().isEmpty()) {
                    dm.displayName(TextUtil.parse(customKeyName));
                }
                if (!customKeyLore.isEmpty()) {
                    List<Component> lore = new ArrayList<>();
                    for (String line : customKeyLore) {
                        lore.add(TextUtil.parseLore(line));
                    }
                    dm.lore(lore);
                }
                display.setItemMeta(dm);
            }
            inventory.setItem(13, display);
        } else {
            inventory.setItem(13, null);
        }

        // Slot 14: 重置為原版預設鑰匙
        String defaultName = context.getPlugin().getConfigManager().getText("gui.vault.key-setup.default-name", "&b重置為原版試煉鑰匙");
        String defaultTypeDesc = template.isOminous() ? "不祥試煉鑰匙" : "普通試煉鑰匙";
        List<String> defaultLore = context.getPlugin().getConfigManager().getStringList(
                "gui.vault.key-setup.default-lore",
                List.of(
                        "&7還原為最單純的原版鑰匙",
                        "&f%type%"
                ),
                "%type%", defaultTypeDesc
        );
        inventory.setItem(14, createButton(template.isOminous() ? Material.OMINOUS_TRIAL_KEY : Material.TRIAL_KEY, defaultName, defaultLore));

        // Slot 15: 🔨 製作防偽鑰匙並領取
        String craftBtnTitle = context.getPlugin().getConfigManager().getText("gui.vault.key-setup.craft-btn-name", "&a🔨 製作防偽鑰匙並領取");
        List<String> craftBtnLore = context.getPlugin().getConfigManager().getStringList(
                "gui.vault.key-setup.craft-btn-lore",
                List.of(
                        "&7將中間的物品製作為正式防偽鑰匙：",
                        "&a✔ 植入專屬防偽 PDC 標記",
                        "&a✔ 綁定為此寶庫的唯一解鎖鑰匙",
                        "&a✔ 製作完成後發送 1 把至背包",
                        "&7",
                        "&e點擊立即製作！"
                )
        );
        inventory.setItem(15, createButton(Material.ANVIL, craftBtnTitle, craftBtnLore));

        // Slot 18: 返回上一步 (返回步驟一：種類與名稱)
        String backName = context.getPlugin().getConfigManager().getText("gui.vault.key-setup.back-name", "&e⬅ 上一步 &f(種類與名稱)");
        List<String> backLore = context.getPlugin().getConfigManager().getStringList(
                "gui.vault.key-setup.back-lore",
                List.of("&7返回 [步驟 1/5] 修改寶庫類型或名稱")
        );
        inventory.setItem(18, createButton(Material.ARROW, backName, backLore));

        // Slot 26: 下一步 (前往步驟三：冷卻與出貨設定)
        String nextName = context.getPlugin().getConfigManager().getText("gui.vault.key-setup.next-name", "&a下一步 ➜ &f(冷卻與出貨設定)");
        List<String> nextLore = context.getPlugin().getConfigManager().getStringList(
                "gui.vault.key-setup.next-lore",
                List.of(
                        "&7保存鑰匙設定並前往 [步驟 3/5]",
                        "&a點擊前往下一步"
                )
        );
        inventory.setItem(26, createButton(Material.LIME_CONCRETE, nextName, nextLore));
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

    private void adoptItemAsKey(ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        this.currentKeyItem = item.clone();
        this.currentKeyItem.setAmount(1);

        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName()) {
                this.customKeyName = TextUtil.toLegacyText(meta.displayName());
            }
            if (meta.hasLore() && meta.lore() != null) {
                this.customKeyLore.clear();
                for (Component c : meta.lore()) {
                    this.customKeyLore.add(TextUtil.toLegacyText(c));
                }
            }
        }
    }

    public ItemStack buildFinalCraftedKey() {
        ItemStack base = (currentKeyItem != null && !currentKeyItem.getType().isAir())
                ? currentKeyItem.clone()
                : new ItemStack(context.getTemplate().isOminous() ? Material.OMINOUS_TRIAL_KEY : Material.TRIAL_KEY);

        return context.getPlugin().getItemManager().craftVaultKey(
                context.getTemplate(),
                base,
                customKeyName,
                customKeyLore
        );
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int rawSlot = event.getRawSlot();
        Player player = (Player) event.getWhoClicked();

        // 玩家點擊自己背包
        if (rawSlot >= 27) {
            if (event.isShiftClick()) {
                // Shift 點擊背包物品 -> 放入 Slot 13
                event.setCancelled(true);
                ItemStack current = event.getCurrentItem();
                if (current != null && !current.getType().isAir()) {
                    // 若先前有玩家放入的實體物品，退回玩家背包
                    if (isPlayerPlacedItem && currentKeyItem != null) {
                        player.getInventory().addItem(currentKeyItem.clone());
                    }
                    ItemStack placed = current.clone();
                    placed.setAmount(1);
                    current.setAmount(current.getAmount() - 1);

                    adoptItemAsKey(placed);
                    isPlayerPlacedItem = true;
                    context.getPlugin().getConfigManager().playSound(player, "click");
                    render();
                }
                return;
            }
            // 一般點擊背包：允許自然操作游標
            return;
        }

        // Slot 13: 鑰匙放置與取出格
        if (rawSlot == 13) {
            event.setCancelled(true);
            ItemStack cursor = event.getCursor();

            if (cursor != null && !cursor.getType().isAir()) {
                // 手持物品放入 Slot 13
                if (isPlayerPlacedItem && currentKeyItem != null) {
                    player.getInventory().addItem(currentKeyItem.clone());
                }

                ItemStack placed = cursor.clone();
                placed.setAmount(1);
                cursor.setAmount(cursor.getAmount() - 1);
                event.getView().setCursor(cursor.getAmount() > 0 ? cursor : null);

                adoptItemAsKey(placed);
                isPlayerPlacedItem = true;
                context.getPlugin().getConfigManager().playSound(player, "click");
                render();
                return;
            } else if (currentKeyItem != null && !currentKeyItem.getType().isAir()) {
                // 游標為空，點擊拿起 Slot 13 的物品
                event.getView().setCursor(currentKeyItem.clone());
                currentKeyItem = null;
                isPlayerPlacedItem = false;
                context.getPlugin().getConfigManager().playSound(player, "click");
                render();
                return;
            }
            return;
        }

        // 其他功能按鈕全部攔截點擊
        event.setCancelled(true);

        if (rawSlot == 11) {
            // 設定鑰匙名稱
            context.getPlugin().getConfigManager().playSound(player, "click");
            context.getPlugin().getChatInputManager().requestInput(
                    player,
                    context.getPlugin().getConfigManager().getRawMessage("key-name-prompt"),
                    input -> {
                        this.customKeyName = input.trim();
                        context.getPlugin().getConfigManager().playSound(player, "success");
                        open();
                    },
                    this::open
            );
        } else if (rawSlot == 12) {
            // 開啟說明 (Lore) 管理介面
            context.getPlugin().getConfigManager().playSound(player, "click");
            new VaultKeyLoreGui(this).open();
        } else if (rawSlot == 14) {
            // 重置為原版預設鑰匙
            if (isPlayerPlacedItem && currentKeyItem != null) {
                player.getInventory().addItem(currentKeyItem.clone());
            }
            this.currentKeyItem = new ItemStack(context.getTemplate().isOminous() ? Material.OMINOUS_TRIAL_KEY : Material.TRIAL_KEY);
            this.customKeyName = null;
            this.customKeyLore.clear();
            this.isPlayerPlacedItem = false;
            context.getPlugin().getConfigManager().playSound(player, "click");
            render();
        } else if (rawSlot == 15) {
            // 🔨 製作防偽鑰匙並領取 1 把
            ItemStack keyToGive = buildFinalCraftedKey();
            player.getInventory().addItem(keyToGive);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
            context.getPlugin().getConfigManager().send(player, "key-craft-success");
            render();
        } else if (rawSlot == 18) {
            // 返回步驟一 (種類與名稱)
            if (isPlayerPlacedItem && currentKeyItem != null) {
                player.getInventory().addItem(currentKeyItem.clone()).values().forEach(drop ->
                        player.getWorld().dropItemNaturally(player.getLocation(), drop)
                );
                isPlayerPlacedItem = false;
            }
            context.getPlugin().getConfigManager().playSound(player, "click");
            new VaultWizardStep1Gui(context).open();
        } else if (rawSlot == 26) {
            // 下一步 (前往步驟三：冷卻機制與出貨數量)
            if (currentKeyItem == null || currentKeyItem.getType().isAir()) {
                context.getPlugin().getConfigManager().send(player, "vault-key-empty");
                context.getPlugin().getConfigManager().playSound(player, "error");
                return;
            }

            ItemStack finalKey = buildFinalCraftedKey();
            context.getTemplate().setKeyItem(finalKey);

            // 若為玩家放入的原物品，退還給玩家防止遺失
            if (isPlayerPlacedItem && currentKeyItem != null) {
                player.getInventory().addItem(currentKeyItem.clone()).values().forEach(drop ->
                        player.getWorld().dropItemNaturally(player.getLocation(), drop)
                );
                isPlayerPlacedItem = false;
            }

            context.getPlugin().getConfigManager().playSound(player, "success");
            new VaultWizardStep3CooldownGui(context).open();
        }
    }

    @Override
    public void handleClose(InventoryCloseEvent event) {
        // 若關閉介面時格內仍有玩家放置的實體物品，安全退還以防遺失
        if (isPlayerPlacedItem && currentKeyItem != null && !currentKeyItem.getType().isAir()) {
            Player player = (Player) event.getPlayer();
            player.getInventory().addItem(currentKeyItem.clone()).values().forEach(drop ->
                    player.getWorld().dropItemNaturally(player.getLocation(), drop)
            );
            isPlayerPlacedItem = false;
        }
    }
}
