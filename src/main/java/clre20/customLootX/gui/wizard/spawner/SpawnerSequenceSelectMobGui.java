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
 * 試煉生怪磚順序編排 - 單格怪物快速挑選彈窗 (支援波次與刪除該格)
 */
public class SpawnerSequenceSelectMobGui extends CustomGuiHolder {

    private final SpawnerWizardContext context;
    private final int waveIndex;
    private final int mobIndexInWave;
    private final int scrollRow;
    private final int scrollCol;

    public SpawnerSequenceSelectMobGui(SpawnerWizardContext context, int waveIndex, int mobIndexInWave, int scrollRow, int scrollCol) {
        this.context = context;
        this.waveIndex = waveIndex;
        this.mobIndexInWave = mobIndexInWave;
        this.scrollRow = scrollRow;
        this.scrollCol = scrollCol;
        this.inventory = Bukkit.createInventory(
                this,
                54,
                context.getPlugin().getConfigManager().getComponent(
                        "gui.spawner.sequence.select-title",
                        "&8[順序編排] 第 " + (waveIndex + 1) + " 輪 第 " + (mobIndexInWave + 1) + " 隻"
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
        List<SpawnerMobEntry> mobPool = template.getMobPool();

        // Slot 0: 【🎲 隨機抽取 (從怪物池依機率生成)】
        List<String> randomLore = List.of(
                "&7此位置將設為【隨機抽取】",
                "&7戰鬥時將從怪物名單中依機率隨機生成",
                "&7",
                "&e點擊選擇【隨機抽取】"
        );
        inventory.setItem(0, createButton(Material.FIREWORK_STAR, "&d🎲 【隨機抽取】 &7(由怪物池生成)", randomLore));

        // Slot 1 ~: 怪物名單中的所有生物
        int slot = 1;
        for (SpawnerMobEntry entry : mobPool) {
            if (slot >= 45) break;

            Material icon = entry.getIconMaterial();
            String title = (entry.isMythic() ? "&d[Mythic] &f" : "&a[原版] &f") + entry.getDisplayName();
            List<String> lore = List.of(
                    "&7代號: &f" + entry.getMobId(),
                    "&7類型: " + (entry.isMythic() ? "&dMythicMob 自訂怪物" : "&a原版生物"),
                    "&7",
                    "&e點擊將此格指定為此生物"
            );
            inventory.setItem(slot, createButton(icon != null ? icon : Material.SPAWNER, title, lore));
            slot++;
        }

        // 底部工具列背景 (45 ~ 53)
        String fillerName = context.getPlugin().getConfigManager().getText("gui.common.filler-name", " ");
        ItemStack filler = createButton(Material.GRAY_STAINED_GLASS_PANE, fillerName, null);
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        // Slot 45: 返回順序介面
        inventory.setItem(45, createButton(Material.ARROW, "&e⬅ 取消並返回", List.of("&7放棄選擇，返回順序看板")));

        // Slot 49: 說明
        inventory.setItem(49, createButton(Material.BOOK, "&6選擇說明", List.of(
                "&7正為 &a第 " + (waveIndex + 1) + " 輪 的 第 " + (mobIndexInWave + 1) + " 隻 &7怪物指定生成對象",
                "&7可選擇【隨機抽取】或步驟三挑選的特定生物"
        )));

        // Slot 51: 🗑 刪除此怪物格子 (純左鍵操作)
        List<List<String>> waves = template.getWaves();
        int waveCount = (waveIndex >= 0 && waveIndex < waves.size()) ? waves.get(waveIndex).size() : 0;
        List<String> deleteLore = List.of(
                "&7從【第 " + (waveIndex + 1) + " 輪】中移除此怪物格子",
                "&7目前此輪共有: &e" + waveCount + " &7隻怪物",
                "&7(若此輪已無格子，將自動移除該輪)",
                "&7",
                "&c點擊立即刪除此怪物格子"
        );
        inventory.setItem(51, createButton(Material.BARRIER, "&c🗑 刪除此怪物格子", deleteLore));
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

        if (slot == 45) {
            context.getPlugin().getConfigManager().playSound(player, "click");
            context.setTransitioning(true);
            new SpawnerWizardStep4SequenceGui(context, scrollRow, scrollCol).open();
            context.setTransitioning(false);
            return;
        }

        SpawnerTemplate template = context.getTemplate();
        List<List<String>> waves = template.getWaves();
        if (waveIndex < 0 || waveIndex >= waves.size()) {
            return;
        }
        List<String> wave = waves.get(waveIndex);
        if (mobIndexInWave < 0 || mobIndexInWave >= wave.size()) {
            return;
        }

        // Slot 51: 刪除此怪物格子
        if (slot == 51) {
            wave.remove(mobIndexInWave);
            if (wave.isEmpty()) {
                if (waves.size() > 1) {
                    waves.remove(waveIndex);
                    player.sendMessage(TextUtil.parse("&8[&6CustomLootX&8] &c已刪除第 " + (waveIndex + 1) + " 輪！"));
                } else {
                    wave.add("RANDOM");
                    player.sendMessage(TextUtil.parse("&8[&6CustomLootX&8] &c生怪磚至少需保留 1 隻怪物！"));
                }
            } else {
                player.sendMessage(TextUtil.parse("&8[&6CustomLootX&8] &c已刪除該怪物格子！"));
            }
            context.getPlugin().getConfigManager().playSound(player, "click");
            context.setTransitioning(true);
            new SpawnerWizardStep4SequenceGui(context, scrollRow, scrollCol).open();
            context.setTransitioning(false);
            return;
        }

        if (slot == 0) {
            // 選中隨機
            wave.set(mobIndexInWave, "RANDOM");
            context.getPlugin().getConfigManager().playSound(player, "click");
            context.setTransitioning(true);
            new SpawnerWizardStep4SequenceGui(context, scrollRow, scrollCol).open();
            context.setTransitioning(false);
            return;
        }

        List<SpawnerMobEntry> mobPool = template.getMobPool();
        int mobIndex = slot - 1;
        if (mobIndex >= 0 && mobIndex < mobPool.size()) {
            SpawnerMobEntry entry = mobPool.get(mobIndex);
            String seqKey = entry.isMythic() ? ("mm:" + entry.getMobId()) : entry.getMobId();
            wave.set(mobIndexInWave, seqKey);

            context.getPlugin().getConfigManager().playSound(player, "click");
            context.setTransitioning(true);
            new SpawnerWizardStep4SequenceGui(context, scrollRow, scrollCol).open();
            context.setTransitioning(false);
        }
    }

    @Override
    public void handleClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (!context.isTransitioning() && !context.isSavedSuccessfully() && !context.isDraftAbandoned()) {
            context.saveAsDraft();
        }
    }
}
