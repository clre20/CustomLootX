package clre20.customLootX.gui.wizard.spawner;

import clre20.customLootX.gui.CustomGuiHolder;
import clre20.customLootX.hook.MythicMobHook;
import clre20.customLootX.model.SpawnerMobEntry;
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
 * 試煉生怪磚 - MythicMobs 怪物挑選與輸入介面
 */
public class SpawnerMythicMobSelectGui extends CustomGuiHolder {

    private final SpawnerWizardContext context;
    private final int page;
    private final List<String> mobIds;
    private static final int ITEMS_PER_PAGE = 45;

    public SpawnerMythicMobSelectGui(SpawnerWizardContext context, int page) {
        this.context = context;
        this.page = Math.max(1, page);
        this.mobIds = MythicMobHook.getAllMobIds();
        this.inventory = Bukkit.createInventory(this, 54, TextUtil.parse("&8選擇或輸入 MythicMob"));
        render();
    }

    public void open() {
        context.getPlayer().openInventory(this.inventory);
    }

    private void render() {
        inventory.clear();

        int totalPages = Math.max(1, (int) Math.ceil((double) mobIds.size() / ITEMS_PER_PAGE));
        int startIndex = (page - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, mobIds.size());

        for (int i = startIndex; i < endIndex; i++) {
            String id = mobIds.get(i);
            int slot = i - startIndex;

            String displayName = MythicMobHook.getDisplayName(id);
            var baseType = MythicMobHook.getBaseEntityType(id);

            List<String> lore = new ArrayList<>();
            lore.add("&7內部識別 ID: &f" + id);
            lore.add("&7基礎實體類型: &f" + baseType.name());
            lore.add("&7");
            lore.add("&a點擊將此 MythicMob 加入怪物生成池！");

            ItemStack item = createButton(Material.NETHER_STAR, "&d" + displayName, lore);
            inventory.setItem(slot, item);
        }

        // 底部工具列 (45 ~ 53)
        String fillerName = context.getPlugin().getConfigManager().getText("gui.common.filler-name", " ");
        ItemStack filler = createButton(Material.GRAY_STAINED_GLASS_PANE, fillerName, null);
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        // Slot 45: 返回怪物池
        inventory.setItem(45, createButton(Material.ARROW, "&e⬅ 返回怪物池", List.of("&7取消選擇並返回怪物池列表")));

        // Slot 48: 上一頁
        if (page > 1) {
            inventory.setItem(48, createButton(Material.FEATHER, "&b⬅ 上一頁", List.of("&7前往第 " + (page - 1) + " 頁")));
        }

        // Slot 49: 聊天室手動輸入 ID
        inventory.setItem(49, createButton(
                Material.NAME_TAG,
                "&b⌨ 聊天室直接輸入代號 (ID)",
                List.of(
                        "&7若清單中沒有或要輸入指定名稱",
                        "&7點擊後在聊天室直接鍵入 MythicMob ID",
                        "&e點擊開始輸入"
                )
        ));

        // Slot 50: 下一頁
        if (page < totalPages) {
            inventory.setItem(50, createButton(Material.FEATHER, "&b下一頁 ➜", List.of("&7前往第 " + (page + 1) + " 頁")));
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

        if (slot >= 0 && slot < ITEMS_PER_PAGE) {
            int index = (page - 1) * ITEMS_PER_PAGE + slot;
            if (index < mobIds.size()) {
                String id = mobIds.get(index);
                addMobToPool(id, player);
            }
        } else if (slot == 45) {
            context.getPlugin().getConfigManager().playSound(player, "click");
            context.setTransitioning(true);
            new SpawnerWizardStep3MobGui(context, 1).open();
            context.setTransitioning(false);
        } else if (slot == 48 && page > 1) {
            context.getPlugin().getConfigManager().playSound(player, "click");
            context.setTransitioning(true);
            new SpawnerMythicMobSelectGui(context, page - 1).open();
            context.setTransitioning(false);
        } else if (slot == 49) {
            // 聊天室直接輸入 ID
            context.getPlugin().getConfigManager().playSound(player, "click");
            context.setTransitioning(true);
            context.getPlugin().getChatInputManager().requestInput(
                    player,
                    "&e請在聊天室輸入 MythicMob 的內部識別代號 (ID)，輸入 &ccancel &e取消：",
                    input -> {
                        String id = input.trim();
                        if (id.isEmpty()) {
                            context.getPlugin().getConfigManager().send(player, "input-invalid");
                            context.getPlugin().getConfigManager().playSound(player, "error");
                            new SpawnerWizardStep3MobGui(context, 1).open();
                            context.setTransitioning(false);
                            return;
                        }
                        addMobToPool(id, player);
                        context.setTransitioning(false);
                    },
                    () -> {
                        new SpawnerWizardStep3MobGui(context, 1).open();
                        context.setTransitioning(false);
                    }
            );
        } else if (slot == 50) {
            int totalPages = Math.max(1, (int) Math.ceil((double) mobIds.size() / ITEMS_PER_PAGE));
            if (page < totalPages) {
                context.getPlugin().getConfigManager().playSound(player, "click");
                context.setTransitioning(true);
                new SpawnerMythicMobSelectGui(context, page + 1).open();
                context.setTransitioning(false);
            }
        }
    }

    private void addMobToPool(String id, Player player) {
        double currentTotal = context.getTemplate().getTotalMobChance();
        double remain = TextUtil.roundChance(100.0 - currentTotal);
        double defaultChance = (remain > 0.0) ? Math.min(remain, 10.0) : 10.0;

        context.getTemplate().getMobPool().add(new SpawnerMobEntry(id, defaultChance));
        context.getPlugin().getConfigManager().playSound(player, "success");
        context.setTransitioning(true);
        new SpawnerWizardStep3MobGui(context, 1).open();
        context.setTransitioning(false);
    }

    @Override
    public void handleClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (!context.isTransitioning() && !context.isSavedSuccessfully() && !context.isDraftAbandoned()) {
            context.saveAsDraft();
        }
    }
}
