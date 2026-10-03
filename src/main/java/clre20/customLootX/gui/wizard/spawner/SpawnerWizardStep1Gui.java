package clre20.customLootX.gui.wizard.spawner;

import clre20.customLootX.gui.CustomGuiHolder;
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
 * 試煉生怪磚 [步驟 1/6] 生怪磚種類與識別名稱
 */
public class SpawnerWizardStep1Gui extends CustomGuiHolder {

    private final SpawnerWizardContext context;

    public SpawnerWizardStep1Gui(SpawnerWizardContext context) {
        this.context = context;
        this.inventory = Bukkit.createInventory(this, 27, context.getPlugin().getConfigManager().getComponent("gui.spawner.step1.title", "&8[步驟 1/6] 生怪磚種類與識別名稱"));
        render();
    }

    public void open() {
        context.getPlayer().openInventory(this.inventory);
    }

    private void render() {
        String fillerName = context.getPlugin().getConfigManager().getText("gui.common.filler-name", " ");
        ItemStack filler = createButton(Material.GRAY_STAINED_GLASS_PANE, fillerName, null);
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        SpawnerTemplate template = context.getTemplate();
        boolean ominous = template.isOminous();

        // Slot 12: 生怪磚種類切換按鈕
        String typeName = context.getPlugin().getConfigManager().getText("gui.spawner.step1.type-button-name", "&6生怪磚種類");
        String currentDesc = ominous
                ? context.getPlugin().getConfigManager().getText("gui.spawner.step1.type-ominous-desc", "&5不祥試煉生怪磚")
                : context.getPlugin().getConfigManager().getText("gui.spawner.step1.type-normal-desc", "&6普通試煉生怪磚");
        String otherDesc = ominous
                ? context.getPlugin().getConfigManager().getText("gui.spawner.step1.type-normal-desc", "&6普通試煉生怪磚")
                : context.getPlugin().getConfigManager().getText("gui.spawner.step1.type-ominous-desc", "&5不祥試煉生怪磚");

        List<String> typeLore = context.getPlugin().getConfigManager().getStringList(
                "gui.spawner.step1.type-button-lore",
                List.of(
                        "&7目前設定: %type%",
                        "&e點擊切換為: &f%other_type%",
                        "&7",
                        "&8(不祥生怪磚具備靈魂火焰特效與更具挑戰性的試煉音效)"
                ),
                "%type%", currentDesc,
                "%other_type%", otherDesc
        );
        inventory.setItem(12, createButton(Material.TRIAL_SPAWNER, typeName, typeLore));

        // Slot 14: 名稱設定按鈕
        String nameBtnName = context.getPlugin().getConfigManager().getText("gui.spawner.step1.name-button-name", "&e配置名稱");
        String name = template.getName();
        boolean hasName = name != null && !name.trim().isEmpty() && !name.equalsIgnoreCase("<未設定>");

        String nameDisplay = hasName ? ("&a" + name) : context.getPlugin().getConfigManager().getText("gui.step1.name-click-to-set", "&c[未設定]");
        List<String> nameLore = context.getPlugin().getConfigManager().getStringList(
                "gui.spawner.step1.name-button-lore",
                List.of(
                        "&7目前名稱: %current_name%",
                        "&7",
                        "&e點擊此處 &f在聊天室輸入名稱",
                        "&8(只能包含英文字母、數字與底線)"
                ),
                "%current_name%", nameDisplay
        );
        inventory.setItem(14, createButton(Material.NAME_TAG, nameBtnName, nameLore));

        // Slot 18: 取消按鈕
        String cancelName = context.getPlugin().getConfigManager().getText("gui.common.cancel-name", "&c取消編輯");
        List<String> cancelLore = context.getPlugin().getConfigManager().getStringList(
                "gui.common.cancel-lore",
                List.of("&7放棄本次編輯並關閉介面")
        );
        inventory.setItem(18, createButton(Material.BARRIER, cancelName, cancelLore));

        // Slot 26: 下一步按鈕
        if (hasName) {
            String nextName = context.getPlugin().getConfigManager().getText("gui.spawner.step1.next-ready-name", "&a下一步 ➜ &f(選擇生成怪物)");
            List<String> nextLore = context.getPlugin().getConfigManager().getStringList(
                    "gui.spawner.step1.next-ready-lore",
                    List.of(
                            "&7前往 [步驟 2/6] 選擇試煉生成的怪物種類",
                            "&a點擊前往下一步"
                    )
            );
            inventory.setItem(26, createButton(Material.LIME_CONCRETE, nextName, nextLore));
        } else {
            String nextNotReadyName = context.getPlugin().getConfigManager().getText("gui.step1.next-not-ready-name", "&c下一步 ➜ &7(請先設定名稱)");
            List<String> nextNotReadyLore = context.getPlugin().getConfigManager().getStringList(
                    "gui.step1.next-not-ready-lore",
                    List.of(
                            "&c必須先設定有效的配置名稱",
                            "&7請點擊中間的【配置名稱】進行輸入"
                    )
            );
            inventory.setItem(26, createButton(Material.RED_CONCRETE, nextNotReadyName, nextNotReadyLore));
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

        if (slot == 12) {
            // 切換普通/不祥生怪磚
            boolean current = context.getTemplate().isOminous();
            context.getTemplate().setOminous(!current);
            context.getPlugin().getConfigManager().playSound(player, "click");
            render();
        } else if (slot == 14) {
            // 點擊設定名稱
            context.getPlugin().getConfigManager().playSound(player, "click");
            context.getPlugin().getChatInputManager().requestInput(
                    player,
                    context.getPlugin().getConfigManager().getRawMessage("name-prompt"),
                    input -> {
                        String trimmed = input.trim();
                        if (!trimmed.matches("^[a-zA-Z0-9_]+$")) {
                            context.getPlugin().getConfigManager().send(player, "name-invalid");
                            context.getPlugin().getConfigManager().playSound(player, "error");
                        } else {
                            String origName = context.getOriginalName();
                            boolean alreadyUsed = context.getPlugin().getSpawnerTemplateManager().getTemplate(trimmed) != null
                                    && (!context.isEditingExisting() || (origName != null && !origName.equalsIgnoreCase(trimmed)));
                            if (alreadyUsed) {
                                context.getPlugin().getConfigManager().send(player, "name-already-used");
                                context.getPlugin().getConfigManager().playSound(player, "error");
                            } else {
                                context.getTemplate().setName(trimmed);
                                context.getTemplate().setDisplayName(trimmed);
                                context.getPlugin().getConfigManager().playSound(player, "success");
                            }
                        }
                        render();
                        open();
                    },
                    () -> {
                        render();
                        open();
                    }
            );
        } else if (slot == 18) {
            // 取消
            context.getPlugin().getConfigManager().playSound(player, "click");
            player.closeInventory();
        } else if (slot == 26) {
            // 下一步 (前往步驟二：選擇生成怪物種類)
            String name = context.getTemplate().getName();
            if (name == null || name.trim().isEmpty() || name.equalsIgnoreCase("<未設定>")) {
                context.getPlugin().getConfigManager().send(player, "name-not-set");
                context.getPlugin().getConfigManager().playSound(player, "error");
                return;
            }
            context.getPlugin().getConfigManager().playSound(player, "click");
            new SpawnerWizardStep2MobGui(context, 1).open();
        }
    }
}
