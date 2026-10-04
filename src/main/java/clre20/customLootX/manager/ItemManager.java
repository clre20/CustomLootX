package clre20.customLootX.manager;

import clre20.customLootX.CustomLootX;
import clre20.customLootX.model.LootItem;
import clre20.customLootX.model.LootTemplate;
import clre20.customLootX.model.VaultCooldownMode;
import clre20.customLootX.model.VaultTemplate;
import clre20.customLootX.model.SpawnerTemplate;
import clre20.customLootX.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ItemManager {

    private final CustomLootX plugin;
    public final NamespacedKey KEY_CUSTOM_LOOT;
    public final NamespacedKey KEY_TEMPLATE_NAME;
    public final NamespacedKey KEY_BLOCK_TYPE;
    public final NamespacedKey KEY_ROLLED_ITEM;

    public final NamespacedKey KEY_CUSTOM_VAULT;
    public final NamespacedKey KEY_VAULT_TEMPLATE_NAME;
    public final NamespacedKey KEY_IS_BLANK_VAULT;

    public final NamespacedKey KEY_CUSTOM_KEY;
    public final NamespacedKey KEY_KEY_TEMPLATE_NAME;

    public final NamespacedKey KEY_CUSTOM_SPAWNER;
    public final NamespacedKey KEY_SPAWNER_TEMPLATE_NAME;
    public final NamespacedKey KEY_IS_BLANK_SPAWNER;
    public final NamespacedKey KEY_SPAWNER_MOB;
    public final NamespacedKey KEY_SPAWNER_LOC;

    public final NamespacedKey KEY_IS_DRAFT;
    public final NamespacedKey KEY_DRAFT_ID;
    public final NamespacedKey KEY_DRAFT_TYPE;
    public final NamespacedKey KEY_DRAFT_EXPIRE;
    public final NamespacedKey KEY_DRAFT_ORIGINAL_NAME;
    public final NamespacedKey KEY_DRAFT_IS_EXPIRED;

    public ItemManager(CustomLootX plugin) {
        this.plugin = plugin;
        this.KEY_CUSTOM_LOOT = new NamespacedKey(plugin, "is_custom_loot");
        this.KEY_TEMPLATE_NAME = new NamespacedKey(plugin, "template_name");
        this.KEY_BLOCK_TYPE = new NamespacedKey(plugin, "block_type");
        this.KEY_ROLLED_ITEM = new NamespacedKey(plugin, "rolled_item");

        this.KEY_CUSTOM_VAULT = new NamespacedKey(plugin, "is_custom_vault");
        this.KEY_VAULT_TEMPLATE_NAME = new NamespacedKey(plugin, "vault_template_name");
        this.KEY_IS_BLANK_VAULT = new NamespacedKey(plugin, "is_blank_vault");

        this.KEY_CUSTOM_KEY = new NamespacedKey(plugin, "is_custom_key");
        this.KEY_KEY_TEMPLATE_NAME = new NamespacedKey(plugin, "key_template_name");

        this.KEY_CUSTOM_SPAWNER = new NamespacedKey(plugin, "is_custom_spawner");
        this.KEY_SPAWNER_TEMPLATE_NAME = new NamespacedKey(plugin, "spawner_template_name");
        this.KEY_IS_BLANK_SPAWNER = new NamespacedKey(plugin, "is_blank_spawner");
        this.KEY_SPAWNER_MOB = new NamespacedKey(plugin, "spawner_mob");
        this.KEY_SPAWNER_LOC = new NamespacedKey(plugin, "spawner_loc");

        this.KEY_IS_DRAFT = new NamespacedKey(plugin, "is_draft");
        this.KEY_DRAFT_ID = new NamespacedKey(plugin, "draft_id");
        this.KEY_DRAFT_TYPE = new NamespacedKey(plugin, "draft_type");
        this.KEY_DRAFT_EXPIRE = new NamespacedKey(plugin, "draft_expire");
        this.KEY_DRAFT_ORIGINAL_NAME = new NamespacedKey(plugin, "draft_orig_name");
        this.KEY_DRAFT_IS_EXPIRED = new NamespacedKey(plugin, "draft_is_expired");
    }

    /**
     * Create a blank custom suspicious block item
     */
    public ItemStack createBlankItem(Material type) {
        Material mat = (type == Material.SUSPICIOUS_GRAVEL) ? Material.SUSPICIOUS_GRAVEL : Material.SUSPICIOUS_SAND;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String title = (mat == Material.SUSPICIOUS_GRAVEL)
                    ? plugin.getConfigManager().getText("items.blank.name-gravel", "&f[全新創建] &7自訂可疑礫石")
                    : plugin.getConfigManager().getText("items.blank.name-sand", "&f[全新創建] &e自訂可疑沙");
            meta.displayName(TextUtil.parse(title));

            List<Component> lore = plugin.getConfigManager().getComponentList(
                    "items.blank.lore",
                    List.of(
                            "&8================================",
                            "&7這是一個新創建的自訂可疑方塊。",
                            "&e手持點擊空氣右鍵 &f即可啟動步驟式編輯精靈！",
                            "&c提示：未完成所有步驟設定前無法放置於地上。",
                            "&8================================"
                    )
            );
            meta.lore(lore);

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(KEY_CUSTOM_LOOT, PersistentDataType.BYTE, (byte) 1);
            pdc.set(KEY_TEMPLATE_NAME, PersistentDataType.STRING, "");
            pdc.set(KEY_BLOCK_TYPE, PersistentDataType.STRING, mat.name());

            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Create a saved custom suspicious block item with detailed properties in Lore
     */
    public ItemStack createTemplateItem(LootTemplate template, int amount) {
        ItemStack item = new ItemStack(template.getType(), Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtil.parse(template.getDisplayName()));

            String typeDesc = (template.getType() == Material.SUSPICIOUS_SAND)
                    ? plugin.getConfigManager().getText("items.configured.type-sand", "可疑沙")
                    : plugin.getConfigManager().getText("items.configured.type-gravel", "可疑礫石");

            String resetDesc = template.isResetEnabled()
                    ? plugin.getConfigManager().getText("items.configured.reset-enabled", "&a開啟 (&f%minutes% 分鐘&a)", "%minutes%", template.getResetMinutes())
                    : plugin.getConfigManager().getText("items.configured.reset-disabled", "&c關閉");

            List<Component> lore = new ArrayList<>(plugin.getConfigManager().getComponentList(
                    "items.configured.lore-header",
                    List.of(
                            "&8================================",
                            "&e配置名稱: &f%name%",
                            "&e方塊種類: &f%type%",
                            "&e重置模式: %reset%",
                            "&e掉落項目: &f%count% 種 (總計 100.00%)"
                    ),
                    "%name%", template.getName(),
                    "%type%", typeDesc,
                    "%reset%", resetDesc,
                    "%count%", template.getItems().size()
            ));

            List<LootItem> items = template.getItems();
            int maxDisplay = 5;
            String itemFormat = plugin.getConfigManager().getText("items.configured.lore-item-format", " &7▪ &f%item%: &a%chance%");
            String airItemName = plugin.getConfigManager().getText("items.configured.air-item-name", "&c[無掉落]");

            for (int i = 0; i < Math.min(items.size(), maxDisplay); i++) {
                LootItem li = items.get(i);
                String itemName;
                if (li.isAir()) {
                    itemName = airItemName;
                } else {
                    ItemStack is = li.getItem();
                    if (is.hasItemMeta() && is.getItemMeta().hasDisplayName()) {
                        itemName = TextUtil.toPlainText(is.getItemMeta().displayName());
                    } else {
                        itemName = is.getType().name();
                    }
                }
                String line = clre20.customLootX.config.ConfigManager.replace(itemFormat, "%item%", itemName, "%chance%", TextUtil.formatPercent(li.getChance()));
                lore.add(TextUtil.parse(line));
            }

            if (items.size() > maxDisplay) {
                int moreCount = items.size() - maxDisplay;
                String moreLine = plugin.getConfigManager().getText("items.configured.lore-item-more", " &7▪ ...以及其他 %more% 項物品", "%more%", moreCount);
                lore.add(TextUtil.parse(moreLine));
            }

            lore.addAll(plugin.getConfigManager().getComponentList(
                    "items.configured.lore-footer",
                    List.of(
                            "&8================================",
                            "&e手持點擊空氣右鍵 &7可重新進入步驟編輯此配置",
                            "&a對地面右鍵 &7可直接放置於世界上",
                            "&8================================"
                    )
            ));

            meta.lore(lore);

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(KEY_CUSTOM_LOOT, PersistentDataType.BYTE, (byte) 1);
            pdc.set(KEY_TEMPLATE_NAME, PersistentDataType.STRING, template.getName());
            pdc.set(KEY_BLOCK_TYPE, PersistentDataType.STRING, template.getType().name());

            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isCustomLootItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(KEY_CUSTOM_LOOT, PersistentDataType.BYTE);
    }

    public boolean isBlankCustomItem(ItemStack item) {
        if (!isCustomLootItem(item)) return false;
        String name = getTemplateName(item);
        return name == null || name.trim().isEmpty();
    }

    public String getTemplateName(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.get(KEY_TEMPLATE_NAME, PersistentDataType.STRING);
    }

    /**
     * Updates the item held in the player's main hand if it matches or is being edited
     */
    public void updateHoldingItem(Player player, LootTemplate template) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (isCustomLootItem(held)) {
            int amount = held.getAmount();
            ItemStack newItem = createTemplateItem(template, amount);
            player.getInventory().setItemInMainHand(newItem);
        }
    }

    /**
     * Updates all items belonging to this template across all online players' inventories and cursors
     */
    public void updateAllOnlinePlayersItems(LootTemplate template) {
        if (template == null || template.getName() == null) return;
        String targetName = template.getName();

        for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            ItemStack[] contents = p.getInventory().getContents();
            boolean changed = false;
            for (int i = 0; i < contents.length; i++) {
                ItemStack item = contents[i];
                if (isCustomLootItem(item) && targetName.equalsIgnoreCase(getTemplateName(item))) {
                    contents[i] = createTemplateItem(template, item.getAmount());
                    changed = true;
                }
            }
            if (changed) {
                p.getInventory().setContents(contents);
            }

            ItemStack cursor = p.getItemOnCursor();
            if (isCustomLootItem(cursor) && targetName.equalsIgnoreCase(getTemplateName(cursor))) {
                p.setItemOnCursor(createTemplateItem(template, cursor.getAmount()));
            }
        }
    }

    // ==========================================
    // 試煉寶庫 (Trial Vault) 物品相關方法
    // ==========================================

    /**
     * 創建一個全新未設定的空白試煉寶庫物品
     */
    public ItemStack createBlankVaultItem(boolean ominous) {
        ItemStack item = new ItemStack(Material.VAULT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String title = ominous
                    ? plugin.getConfigManager().getText("items.vault.blank-name-ominous", "&f[全新創建] &5自訂不祥試煉寶庫")
                    : plugin.getConfigManager().getText("items.vault.blank-name-normal", "&f[全新創建] &6自訂試煉寶庫");
            meta.displayName(TextUtil.parse(title));

            List<Component> lore = plugin.getConfigManager().getComponentList(
                    "items.vault.blank-lore",
                    List.of(
                            "&8================================",
                            "&7這是一個新創建的自訂試煉寶庫。",
                            "&e手持點擊空氣右鍵 &f即可啟動步驟式編輯精靈！",
                            "&c提示：未完成所有步驟設定前無法放置於地上。",
                            "&8================================"
                    )
            );
            meta.lore(lore);

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(KEY_CUSTOM_VAULT, PersistentDataType.BYTE, (byte) 1);
            pdc.set(KEY_IS_BLANK_VAULT, PersistentDataType.BYTE, (byte) 1);
            pdc.set(KEY_BLOCK_TYPE, PersistentDataType.STRING, ominous ? "OMINOUS_VAULT" : "NORMAL_VAULT");

            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 依照 VaultTemplate 配置建立成品試煉寶庫物品
     */
    public ItemStack createTemplateVaultItem(VaultTemplate template, int amount) {
        if (template == null) {
            return createBlankVaultItem(false);
        }

        ItemStack item = new ItemStack(Material.VAULT, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String titleFormat = template.isOminous()
                    ? plugin.getConfigManager().getText("items.vault.template-name-ominous", "&5[不祥寶庫] &d%name%", "%name%", template.getDisplayName())
                    : plugin.getConfigManager().getText("items.vault.template-name-normal", "&6[試煉寶庫] &e%name%", "%name%", template.getDisplayName());
            meta.displayName(TextUtil.parse(titleFormat));

            String typeDesc = template.isOminous() ? "&5不祥試煉寶庫" : "&6普通試煉寶庫";
            String keyDesc = (template.getKeyItem() != null && template.getKeyItem().getItemMeta() != null && template.getKeyItem().getItemMeta().hasDisplayName())
                    ? TextUtil.toLegacyText(template.getKeyItem().getItemMeta().displayName())
                    : (template.isOminous() ? "不祥試煉鑰匙" : "試煉鑰匙");
            String modeDesc = template.getCooldownMode().getDisplay();
            String timeDesc = (template.getCooldownMode() == VaultCooldownMode.ONCE_PER_PLAYER)
                    ? "&7(永久一次)"
                    : ("&a" + template.getCooldownMinutes() + " 分鐘");

            List<Component> lore = new ArrayList<>();
            List<String> header = plugin.getConfigManager().getStringList(
                    "items.vault.lore-header",
                    List.of(
                            "&8================================",
                            "&e配置名稱: &f%name%",
                            "&e寶庫類型: &f%type%",
                            "&e解鎖鑰匙: &f%key%",
                            "&e重置模式: &f%mode% %time%",
                            "&e每次出貨: &a%rolls% 件物品",
                            "&e掉落項目: &f%count% 種 (總計 100.00%)"
                    ),
                    "%name%", template.getName(),
                    "%type%", typeDesc,
                    "%key%", keyDesc,
                    "%mode%", modeDesc,
                    "%time%", timeDesc,
                    "%rolls%", String.valueOf(template.getRollCount()),
                    "%count%", String.valueOf(template.getItems().size())
            );
            for (String line : header) {
                lore.add(TextUtil.parse(line));
            }

            // 掉落物列表前 5 項
            List<LootItem> items = template.getItems();
            int showCount = Math.min(items.size(), 5);
            for (int i = 0; i < showCount; i++) {
                LootItem li = items.get(i);
                String itemName = li.isAir() ? "&c[落空]" : TextUtil.getItemName(li.getItem());
                String itemLine = plugin.getConfigManager().getText(
                        "items.lore-item-format",
                        " &7▪ &f%item%: &a%chance%",
                        "%item%", itemName,
                        "%chance%", String.format("%.2f%%", li.getChance())
                );
                lore.add(TextUtil.parse(itemLine));
            }
            if (items.size() > 5) {
                String more = plugin.getConfigManager().getText(
                        "items.lore-item-more",
                        " &7▪ ...以及其他 %more% 項物品",
                        "%more%", String.valueOf(items.size() - 5)
                );
                lore.add(TextUtil.parse(more));
            }

            List<String> footer = plugin.getConfigManager().getStringList(
                    "items.vault.lore-footer",
                    List.of(
                            "&8================================",
                            "&e手持點擊空氣右鍵 &7可重新進入步驟編輯此配置",
                            "&a對地面右鍵 &7可直接放置於世界上",
                            "&8================================"
                    )
            );
            for (String line : footer) {
                lore.add(TextUtil.parse(line));
            }

            meta.lore(lore);

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(KEY_CUSTOM_VAULT, PersistentDataType.BYTE, (byte) 1);
            pdc.set(KEY_VAULT_TEMPLATE_NAME, PersistentDataType.STRING, template.getName().toLowerCase());
            pdc.set(KEY_BLOCK_TYPE, PersistentDataType.STRING, template.isOminous() ? "OMINOUS_VAULT" : "NORMAL_VAULT");

            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isCustomVaultItem(ItemStack item) {
        if (item == null || item.getType() != Material.VAULT || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(KEY_CUSTOM_VAULT, PersistentDataType.BYTE);
    }

    public boolean isBlankVaultItem(ItemStack item) {
        if (!isCustomVaultItem(item)) return false;
        return item.getItemMeta().getPersistentDataContainer().has(KEY_IS_BLANK_VAULT, PersistentDataType.BYTE);
    }

    public String getVaultTemplateName(ItemStack item) {
        if (!isCustomVaultItem(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(KEY_VAULT_TEMPLATE_NAME, PersistentDataType.STRING);
    }

    /**
     * 深度防偽比對手持物品是否與設定的鑰匙完全相符 (包含 Material, DisplayName, Lore, Enchants, PDC Tags)
     */
    public boolean matchesKey(ItemStack held, ItemStack targetKey) {
        if (held == null || held.getType().isAir()) {
            return false;
        }
        if (targetKey == null) {
            return held.getType() == Material.TRIAL_KEY || held.getType() == Material.OMINOUS_TRIAL_KEY;
        }

        // 1. 材質比對
        if (held.getType() != targetKey.getType()) {
            return false;
        }

        ItemMeta heldMeta = held.getItemMeta();
        ItemMeta targetMeta = targetKey.getItemMeta();

        if (heldMeta == null && targetMeta == null) {
            return true;
        }
        if (heldMeta == null || targetMeta == null) {
            return false;
        }

        // 2. 自訂名稱比對
        if (targetMeta.hasDisplayName()) {
            if (!heldMeta.hasDisplayName() || !Objects.equals(heldMeta.displayName(), targetMeta.displayName())) {
                return false;
            }
        }

        // 3. CustomModelData 比對
        if (targetMeta.hasCustomModelData()) {
            if (!heldMeta.hasCustomModelData() || heldMeta.getCustomModelData() != targetMeta.getCustomModelData()) {
                return false;
            }
        }

        // 4. Lore 比對
        if (targetMeta.hasLore()) {
            if (!heldMeta.hasLore() || !Objects.equals(heldMeta.lore(), targetMeta.lore())) {
                return false;
            }
        }

        // 5. 附魔比對
        if (targetMeta.hasEnchants()) {
            if (!heldMeta.getEnchants().equals(targetMeta.getEnchants())) {
                return false;
            }
        }

        // 6. PDC 標籤深度比對防偽
        PersistentDataContainer heldPdc = heldMeta.getPersistentDataContainer();
        PersistentDataContainer targetPdc = targetMeta.getPersistentDataContainer();
        for (NamespacedKey key : targetPdc.getKeys()) {
            if (!heldPdc.has(key)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 鍛造 / 製作防偽試煉寶庫鑰匙
     */
    public ItemStack craftVaultKey(VaultTemplate template, ItemStack baseItem, String customName, List<String> customLore) {
        ItemStack item;
        if (baseItem != null && !baseItem.getType().isAir()) {
            item = baseItem.clone();
            item.setAmount(1);
        } else {
            item = new ItemStack(template.isOminous() ? Material.OMINOUS_TRIAL_KEY : Material.TRIAL_KEY);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (customName != null && !customName.trim().isEmpty()) {
                meta.displayName(TextUtil.parse(customName));
            } else if (!meta.hasDisplayName()) {
                String defaultName = template.isOminous() ? "&5不祥試煉之鑰" : "&6試煉之鑰";
                meta.displayName(TextUtil.parse(defaultName));
            }

            List<Component> lore = new ArrayList<>();
            if (customLore != null && !customLore.isEmpty()) {
                for (String line : customLore) {
                    lore.add(TextUtil.parseLore(line));
                }
            }
            meta.lore(lore);

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(KEY_CUSTOM_KEY, PersistentDataType.BYTE, (byte) 1);
            pdc.set(KEY_KEY_TEMPLATE_NAME, PersistentDataType.STRING, template.getName().toLowerCase());

            item.setItemMeta(meta);
        }
        return item;
    }

    public void updatePlayerHeldVaultItem(Player player, VaultTemplate template) {
        if (player == null || template == null) return;
        ItemStack held = player.getInventory().getItemInMainHand();
        if (isCustomVaultItem(held)) {
            int amount = held.getAmount();
            ItemStack newItem = createTemplateVaultItem(template, amount);
            player.getInventory().setItemInMainHand(newItem);
        }
    }

    public void updateAllOnlinePlayersVaultItems(VaultTemplate template) {
        if (template == null || template.getName() == null) return;
        String targetName = template.getName();

        for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            ItemStack[] contents = p.getInventory().getContents();
            boolean changed = false;
            for (int i = 0; i < contents.length; i++) {
                ItemStack item = contents[i];
                if (isCustomVaultItem(item) && targetName.equalsIgnoreCase(getVaultTemplateName(item))) {
                    contents[i] = createTemplateVaultItem(template, item.getAmount());
                    changed = true;
                }
            }
            if (changed) {
                p.getInventory().setContents(contents);
            }

            ItemStack cursor = p.getItemOnCursor();
            if (isCustomVaultItem(cursor) && targetName.equalsIgnoreCase(getVaultTemplateName(cursor))) {
                p.setItemOnCursor(createTemplateVaultItem(template, cursor.getAmount()));
            }
        }
    }

    // ==========================================
    // 試煉生怪磚 (Trial Spawner) 物品邏輯
    // ==========================================

    /**
     * 建立全新未設定的空白試煉生怪磚物品
     */
    public ItemStack createBlankSpawnerItem(boolean ominous) {
        ItemStack item = new ItemStack(Material.TRIAL_SPAWNER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String title = ominous
                    ? plugin.getConfigManager().getText("items.spawner.blank-name-ominous", "&f[全新創建] &5自訂不祥試煉生怪磚")
                    : plugin.getConfigManager().getText("items.spawner.blank-name-normal", "&f[全新創建] &6自訂試煉生怪磚");
            meta.displayName(TextUtil.parse(title));

            List<Component> lore = plugin.getConfigManager().getComponentList(
                    "items.spawner.blank-lore",
                    List.of(
                            "&8================================",
                            "&7這是一個新創建的自訂試煉生怪磚。",
                            "&e手持點擊空氣右鍵 &f即可啟動步驟式編輯精靈！",
                            "&c提示：未完成所有步驟設定前無法放置於地上。",
                            "&8================================"
                    )
            );
            meta.lore(lore);

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(KEY_CUSTOM_SPAWNER, PersistentDataType.BYTE, (byte) 1);
            pdc.set(KEY_IS_BLANK_SPAWNER, PersistentDataType.BYTE, (byte) 1);
            pdc.set(KEY_BLOCK_TYPE, PersistentDataType.STRING, ominous ? "OMINOUS_SPAWNER" : "NORMAL_SPAWNER");

            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 依照 SpawnerTemplate 配置建立成品試煉生怪磚物品
     */
    public ItemStack createTemplateSpawnerItem(SpawnerTemplate template, int amount) {
        if (template == null) {
            return createBlankSpawnerItem(false);
        }

        ItemStack item = new ItemStack(Material.TRIAL_SPAWNER, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String titleFormat = template.isOminous()
                    ? plugin.getConfigManager().getText("items.spawner.template-name-ominous", "&5[不祥生怪磚] &d%name%", "%name%", template.getDisplayName())
                    : plugin.getConfigManager().getText("items.spawner.template-name-normal", "&6[試煉生怪磚] &e%name%", "%name%", template.getDisplayName());
            meta.displayName(TextUtil.parse(titleFormat));

            String typeDesc = template.isOminous() ? "&5不祥試煉生怪磚" : "&6普通試煉生怪磚";
            String mobDesc;
            if (template.getMobPool().size() > 1) {
                mobDesc = template.getMobPool().size() + " 種怪物 (機率分配)";
            } else if (!template.getMobPool().isEmpty()) {
                mobDesc = template.getMobPool().get(0).getDisplayName();
            } else {
                mobDesc = TextUtil.getMobDisplayName(template.getSpawnedType());
            }
            String modeDesc = template.getCooldownMode().getDisplay();
            String timeDesc = (template.getCooldownMode() == VaultCooldownMode.ONCE_PER_PLAYER)
                    ? "&7(永久一次)"
                    : ("&a" + template.getCooldownMinutes() + " 分鐘");

            List<Component> lore = new ArrayList<>();
            List<String> header = plugin.getConfigManager().getStringList(
                    "items.spawner.lore-header",
                    List.of(
                            "&8================================",
                            "&e配置名稱: &f%name%",
                            "&e生怪磚類型: &f%type%",
                            "&e生成怪物: &c%mob%",
                            "&e波次配置: &f總共 %total% 隻 (同時上限 %sim% 隻)",
                            "&e生成間隔: &f%delay% 秒 &7| &e感應範圍: &f%range% 格",
                            "&e重置模式: &f%mode% %time%",
                            "&e獲勝獎勵: &a%rolls% 件物品 (掉落池 %count% 種)"
                    ),
                    "%name%", template.getName(),
                    "%type%", typeDesc,
                    "%mob%", mobDesc,
                    "%total%", String.valueOf(template.getTotalMobs()),
                    "%sim%", String.valueOf(template.getSimultaneousMobs()),
                    "%delay%", String.valueOf(template.getSpawnDelaySeconds()),
                    "%range%", String.valueOf(template.getPlayerRange()),
                    "%mode%", modeDesc,
                    "%time%", timeDesc,
                    "%rolls%", String.valueOf(template.getRollCount()),
                    "%count%", String.valueOf(template.getRewards().size())
            );
            for (String line : header) {
                lore.add(TextUtil.parse(line));
            }

            // 獎勵列表前 5 項
            List<LootItem> items = template.getRewards();
            int showCount = Math.min(items.size(), 5);
            for (int i = 0; i < showCount; i++) {
                LootItem li = items.get(i);
                String itemName = li.isAir() ? "&c[落空]" : TextUtil.getItemName(li.getItem());
                String itemLine = plugin.getConfigManager().getText(
                        "items.lore-item-format",
                        " &7▪ &f%item%: &a%chance%",
                        "%item%", itemName,
                        "%chance%", String.format("%.2f%%", li.getChance())
                );
                lore.add(TextUtil.parse(itemLine));
            }
            if (items.size() > 5) {
                String more = plugin.getConfigManager().getText(
                        "items.lore-item-more",
                        " &7▪ ...以及其他 %more% 項物品",
                        "%more%", String.valueOf(items.size() - 5)
                );
                lore.add(TextUtil.parse(more));
            }

            List<String> footer = plugin.getConfigManager().getStringList(
                    "items.spawner.lore-footer",
                    List.of(
                            "&8================================",
                            "&e手持點擊空氣右鍵 &7可重新進入步驟編輯此配置",
                            "&a對地面右鍵 &7可直接放置於世界上",
                            "&8================================"
                    )
            );
            for (String line : footer) {
                lore.add(TextUtil.parse(line));
            }

            meta.lore(lore);

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(KEY_CUSTOM_SPAWNER, PersistentDataType.BYTE, (byte) 1);
            pdc.set(KEY_SPAWNER_TEMPLATE_NAME, PersistentDataType.STRING, template.getName().toLowerCase());
            pdc.set(KEY_BLOCK_TYPE, PersistentDataType.STRING, template.isOminous() ? "OMINOUS_SPAWNER" : "NORMAL_SPAWNER");

            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isCustomSpawnerItem(ItemStack item) {
        if (item == null || item.getType() != Material.TRIAL_SPAWNER || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(KEY_CUSTOM_SPAWNER, PersistentDataType.BYTE);
    }

    public boolean isBlankSpawnerItem(ItemStack item) {
        if (!isCustomSpawnerItem(item)) return false;
        return item.getItemMeta().getPersistentDataContainer().has(KEY_IS_BLANK_SPAWNER, PersistentDataType.BYTE);
    }

    public String getSpawnerTemplateName(ItemStack item) {
        if (!isCustomSpawnerItem(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(KEY_SPAWNER_TEMPLATE_NAME, PersistentDataType.STRING);
    }

    public void updatePlayerHeldSpawnerItem(Player player, SpawnerTemplate template) {
        if (player == null || template == null) return;
        ItemStack held = player.getInventory().getItemInMainHand();
        if (isCustomSpawnerItem(held)) {
            int amount = held.getAmount();
            ItemStack newItem = createTemplateSpawnerItem(template, amount);
            player.getInventory().setItemInMainHand(newItem);
        }
    }

    public void updateAllOnlinePlayersSpawnerItems(SpawnerTemplate template) {
        if (template == null || template.getName() == null) return;
        String targetName = template.getName();

        for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            ItemStack[] contents = p.getInventory().getContents();
            boolean changed = false;
            for (int i = 0; i < contents.length; i++) {
                ItemStack item = contents[i];
                if (isCustomSpawnerItem(item) && targetName.equalsIgnoreCase(getSpawnerTemplateName(item))) {
                    contents[i] = createTemplateSpawnerItem(template, item.getAmount());
                    changed = true;
                }
            }
            if (changed) {
                p.getInventory().setContents(contents);
            }

            ItemStack cursor = p.getItemOnCursor();
            if (isCustomSpawnerItem(cursor) && targetName.equalsIgnoreCase(getSpawnerTemplateName(cursor))) {
                p.setItemOnCursor(createTemplateSpawnerItem(template, cursor.getAmount()));
            }
        }
    }

    // ==========================================
    // Draft (草稿暫存) 管理方法
    // ==========================================

    public boolean isDraftItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(KEY_IS_DRAFT, PersistentDataType.BYTE);
    }

    public String getDraftId(ItemStack item) {
        if (!isDraftItem(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(KEY_DRAFT_ID, PersistentDataType.STRING);
    }

    public clre20.customLootX.model.DraftType getDraftType(ItemStack item) {
        if (!isDraftItem(item)) return null;
        String typeStr = item.getItemMeta().getPersistentDataContainer().get(KEY_DRAFT_TYPE, PersistentDataType.STRING);
        return clre20.customLootX.model.DraftType.fromString(typeStr);
    }

    public long getDraftExpireTime(ItemStack item) {
        if (!isDraftItem(item)) return 0;
        Long expire = item.getItemMeta().getPersistentDataContainer().get(KEY_DRAFT_EXPIRE, PersistentDataType.LONG);
        return expire != null ? expire : 0;
    }

    public String getDraftOriginalName(ItemStack item) {
        if (!isDraftItem(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(KEY_DRAFT_ORIGINAL_NAME, PersistentDataType.STRING);
    }

    public boolean isDraftExpired(ItemStack item) {
        if (!isDraftItem(item)) return false;
        long expire = getDraftExpireTime(item);
        return expire > 0 && System.currentTimeMillis() > expire;
    }

    public boolean isDraftMarkedExpired(ItemStack item) {
        if (!isDraftItem(item)) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(KEY_DRAFT_IS_EXPIRED, PersistentDataType.BYTE);
    }

    /**
     * 套用草稿狀態至物品 (更新 PDC 與未儲存 Lore)
     */
    public void applyDraft(ItemStack item, clre20.customLootX.model.DraftType type, String draftId, String originalName, long expireTime, String draftDisplayName) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(KEY_IS_DRAFT, PersistentDataType.BYTE, (byte) 1);
        pdc.set(KEY_DRAFT_ID, PersistentDataType.STRING, draftId);
        pdc.set(KEY_DRAFT_TYPE, PersistentDataType.STRING, type.name());
        pdc.set(KEY_DRAFT_EXPIRE, PersistentDataType.LONG, expireTime);
        pdc.remove(KEY_DRAFT_IS_EXPIRED);
        if (originalName != null && !originalName.trim().isEmpty()) {
            pdc.set(KEY_DRAFT_ORIGINAL_NAME, PersistentDataType.STRING, originalName);
        } else {
            pdc.remove(KEY_DRAFT_ORIGINAL_NAME);
        }

        String displayName = (draftDisplayName != null && !draftDisplayName.trim().isEmpty()) ? draftDisplayName : "未命名方塊";
        meta.displayName(TextUtil.parse("&e[未儲存草稿] &f" + displayName));

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
        String expireDateStr = sdf.format(new java.util.Date(expireTime));
        int expireDays = plugin.getDraftManager().getExpireDays();

        List<Component> lore = new ArrayList<>();
        lore.add(TextUtil.parse("&8================================"));
        lore.add(TextUtil.parse("&c⚠ 此方塊有尚未儲存的編輯進度！"));
        lore.add(TextUtil.parse("&7草稿編號: &f#" + draftId));
        lore.add(TextUtil.parse("&7有效期限至: &e" + expireDateStr + " &8(" + expireDays + "天內有效)"));
        lore.add(TextUtil.parse("&8================================"));
        lore.add(TextUtil.parse("&e手持點擊空氣右鍵 &a可繼續上次編輯並儲存。"));
        lore.add(TextUtil.parse("&c提示：草稿狀態下禁止放置於地面。"));
        lore.add(TextUtil.parse("&8================================"));

        meta.lore(lore);
        item.setItemMeta(meta);
    }

    /**
     * 當草稿逾期時，更新物品 Lore 為過期狀態
     */
    public void markDraftExpired(ItemStack item) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.getPersistentDataContainer().set(KEY_DRAFT_IS_EXPIRED, PersistentDataType.BYTE, (byte) 1);

        String draftId = getDraftId(item);
        if (draftId == null) draftId = "未知";

        meta.displayName(TextUtil.parse("&c[草稿已過期] &7自訂方塊"));

        List<Component> lore = new ArrayList<>();
        lore.add(TextUtil.parse("&8================================"));
        lore.add(TextUtil.parse("&c⚠ 此草稿已超過有效期限（已過期）！"));
        lore.add(TextUtil.parse("&7草稿編號: &8#" + draftId));
        lore.add(TextUtil.parse("&8================================"));
        lore.add(TextUtil.parse("&e手持點擊空氣右鍵即可編輯此方塊正式版。"));
        lore.add(TextUtil.parse("&c提示：草稿已失效，無法放置於地面。"));
        lore.add(TextUtil.parse("&8================================"));

        meta.lore(lore);
        item.setItemMeta(meta);
    }

    /**
     * 移除物品上的草稿標籤
     */
    public void removeDraft(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.remove(KEY_IS_DRAFT);
        pdc.remove(KEY_DRAFT_ID);
        pdc.remove(KEY_DRAFT_TYPE);
        pdc.remove(KEY_DRAFT_EXPIRE);
        pdc.remove(KEY_DRAFT_ORIGINAL_NAME);
        pdc.remove(KEY_DRAFT_IS_EXPIRED);
        item.setItemMeta(meta);
    }
}
