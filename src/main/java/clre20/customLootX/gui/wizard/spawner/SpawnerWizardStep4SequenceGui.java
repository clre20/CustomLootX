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
 * 試煉生怪磚 [步驟 4/6] 怪物生成順序編排介面 (橫向 2D 波次滾動看板，支援各輪自訂數量)
 */
public class SpawnerWizardStep4SequenceGui extends CustomGuiHolder {

    private final SpawnerWizardContext context;
    private int scrollRow = 0; // 縱向輪數滾動偏移 (0 = 第 1 輪)
    private int scrollCol = 0; // 橫向隻數滾動偏移 (0 = 第 1 隻)
    private boolean deleteMode = false; // 快速刪除模式 (全流程純左鍵操作)

    private static final int VISIBLE_ROWS = 4; // 上方展示 4 輪
    private static final int VISIBLE_COLS = 7; // 每輪橫向展示 7 隻 (Col 1 ~ 7)

    public SpawnerWizardStep4SequenceGui(SpawnerWizardContext context) {
        this(context, 0, 0, false);
    }

    public SpawnerWizardStep4SequenceGui(SpawnerWizardContext context, int scrollRow, int scrollCol) {
        this(context, scrollRow, scrollCol, false);
    }

    public SpawnerWizardStep4SequenceGui(SpawnerWizardContext context, int scrollRow, int scrollCol, boolean deleteMode) {
        this.context = context;
        this.scrollRow = Math.max(0, scrollRow);
        this.scrollCol = Math.max(0, scrollCol);
        this.deleteMode = deleteMode;
        this.inventory = Bukkit.createInventory(
                this,
                54,
                context.getPlugin().getConfigManager().getComponent(
                        "gui.spawner.step4.sequence-title",
                        "&8[步驟 4/6] 怪物生成順序編排看板"
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
        List<List<String>> waves = template.getWaves();
        int totalWaves = waves.size();
        int totalMobs = template.getTotalMobs();

        int maxWaveLength = 0;
        for (List<String> w : waves) {
            if (w != null && w.size() > maxWaveLength) {
                maxWaveLength = w.size();
            }
        }

        // 確保滾動偏移在有效範圍內 (+1 預留加號欄位)
        int maxScrollRow = Math.max(0, (totalWaves + 1) - VISIBLE_ROWS);
        int maxScrollCol = Math.max(0, (maxWaveLength + 1) - VISIBLE_COLS);
        if (scrollRow > maxScrollRow) scrollRow = maxScrollRow;
        if (scrollCol > maxScrollCol) scrollCol = maxScrollCol;

        // 1. 渲染前 4 行 (Row 0 ~ 3)：波次矩陣
        for (int r = 0; r < VISIBLE_ROWS; r++) {
            int waveIndex = scrollRow + r; // 當前輪次 (0-indexed)

            // Col 0: 該輪標籤 或 新增該輪按鈕
            int labelSlot = r * 9;
            if (waveIndex < totalWaves) {
                List<String> wave = waves.get(waveIndex);
                int countInWave = (wave != null) ? wave.size() : 0;
                List<String> labelLore = new ArrayList<>();
                labelLore.add("&7此輪生成隻數: &a" + countInWave + " &7隻");
                labelLore.add("&7(由左向右依序生成)");
                labelLore.add("&7");
                if (deleteMode) {
                    labelLore.add("&c&l【刪除模式啟用中】");
                    labelLore.add("&e[左鍵點擊] &c刪除此整輪怪物！");
                } else {
                    labelLore.add("&e點擊右側格子挑選怪物或末尾 ➕ 增加格子");
                    if (totalWaves > 1) {
                        labelLore.add("&c[右鍵點擊] 刪除此整輪怪物");
                    }
                }

                ItemStack labelItem = createButton(
                        Material.COMPASS,
                        "&6【第 " + (waveIndex + 1) + " 輪】",
                        labelLore
                );
                inventory.setItem(labelSlot, labelItem);
            } else if (waveIndex == totalWaves) {
                // 接在最後一輪下方的【➕ 新增下一輪】按鈕
                inventory.setItem(labelSlot, createButton(
                        Material.LIME_STAINED_GLASS_PANE,
                        "&a➕ 【新增第 " + (waveIndex + 1) + " 輪】",
                        List.of(
                                "&7為生怪磚新增下一輪波次",
                                "&e點擊立即新增"
                        )
                ));
            } else {
                inventory.setItem(labelSlot, createButton(Material.GRAY_STAINED_GLASS_PANE, " ", null));
            }

            // Col 1 ~ 7: 該輪的各個怪物格子 或 末尾 ➕ 增加格子按鈕
            for (int c = 1; c <= VISIBLE_COLS; c++) {
                int mobPosInWave = scrollCol + (c - 1); // 該輪第幾隻 (0-indexed)
                int cellSlot = r * 9 + c;

                if (waveIndex < totalWaves) {
                    List<String> wave = waves.get(waveIndex);
                    int waveSize = (wave != null) ? wave.size() : 0;

                    if (mobPosInWave < waveSize) {
                        // 有效怪物格子
                        String mobKey = wave.get(mobPosInWave);
                        ItemStack cellItem = createMobCellItem(template, mobKey, waveIndex, mobPosInWave);
                        inventory.setItem(cellSlot, cellItem);
                    } else if (mobPosInWave == waveSize) {
                        // 末尾 ➕ 增加格子按鈕
                        ItemStack addCellBtn = createButton(
                                Material.LIME_STAINED_GLASS_PANE,
                                "&a➕ [為第 " + (waveIndex + 1) + " 輪增加 1 格]",
                                List.of(
                                        "&7在第 " + (waveIndex + 1) + " 輪末尾新增一隻怪物",
                                        "&7目前此輪隻數: &e" + waveSize + " &7隻",
                                        "&7",
                                        "&e點擊立即增加格子"
                                )
                        );
                        inventory.setItem(cellSlot, addCellBtn);
                    } else {
                        // 空白填充
                        inventory.setItem(cellSlot, createButton(Material.GRAY_STAINED_GLASS_PANE, " ", null));
                    }
                } else {
                    inventory.setItem(cellSlot, createButton(Material.GRAY_STAINED_GLASS_PANE, " ", null));
                }
            }

            // Col 8: 最右直行 (Row 0: ⬆ 向上平移, Row 3: ⬇ 向下平移)
            int col8Slot = r * 9 + 8;
            if (r == 0) {
                if (scrollRow > 0) {
                    inventory.setItem(col8Slot, createButton(
                            Material.ARROW,
                            "&b⬆ 向上平移 1 輪",
                            List.of(
                                    "&7將看板向上移動 1 輪",
                                    "&7目前頂部顯示: &e第 " + (scrollRow + 1) + " 輪",
                                    "&e點擊向上平移"
                            )
                    ));
                } else {
                    inventory.setItem(col8Slot, createButton(Material.GRAY_STAINED_GLASS_PANE, "&8(已在最頂端)", null));
                }
            } else if (r == 3) {
                if (scrollRow < maxScrollRow) {
                    inventory.setItem(col8Slot, createButton(
                            Material.ARROW,
                            "&b⬇ 向下平移 1 輪",
                            List.of(
                                    "&7將看板向下移動 1 輪",
                                    "&7目前底部顯示: &e第 " + (scrollRow + VISIBLE_ROWS) + " 輪 &7/ 共 " + totalWaves + " 輪",
                                    "&e點擊向下平移"
                            )
                    ));
                } else {
                    inventory.setItem(col8Slot, createButton(Material.GRAY_STAINED_GLASS_PANE, "&8(已在最底端)", null));
                }
            } else {
                inventory.setItem(col8Slot, createButton(Material.GRAY_STAINED_GLASS_PANE, " ", null));
            }
        }

        // 2. 渲染 Row 4 (Slot 36 ~ 44)：橫向平移控制排
        for (int i = 36; i < 45; i++) {
            inventory.setItem(i, createButton(Material.GRAY_STAINED_GLASS_PANE, " ", null));
        }

        // Slot 37: ◀ 向左平移 1 隻
        if (scrollCol > 0) {
            inventory.setItem(37, createButton(
                    Material.ARROW,
                    "&b◀ 向左平移 1 隻",
                    List.of(
                            "&7將每輪怪物欄位向左移動 1 格",
                            "&7目前左側顯示: &e此輪第 " + (scrollCol + 1) + " 隻",
                            "&e點擊向左平移"
                    )
            ));
        } else {
            inventory.setItem(37, createButton(Material.GRAY_STAINED_GLASS_PANE, "&8(已在最左側)", null));
        }

        // Slot 40: 視野狀態
        int startWaveDisplay = Math.min(totalWaves, scrollRow + 1);
        int endWaveDisplay = Math.min(totalWaves, scrollRow + VISIBLE_ROWS);
        int startColDisplay = scrollCol + 1;
        int endColDisplay = scrollCol + VISIBLE_COLS;
        inventory.setItem(40, createButton(
                Material.MAP,
                "&6看板視野座標",
                List.of(
                        "&7顯示輪次: &e第 " + startWaveDisplay + " ~ " + endWaveDisplay + " 輪 &7(共 " + totalWaves + " 輪)",
                        "&7顯示隻數: &e每輪第 " + startColDisplay + " ~ " + endColDisplay + " 隻",
                        "&7怪物總數: &a" + totalMobs + " &7隻"
                )
        ));

        // Slot 43: ▶ 向右平移 1 隻
        if (scrollCol < maxScrollCol) {
            inventory.setItem(43, createButton(
                    Material.ARROW,
                    "&b▶ 向右平移 1 隻",
                    List.of(
                            "&7將每輪怪物欄位向右移動 1 格",
                            "&7目前右側顯示: &e此輪第 " + endColDisplay + " 隻",
                            "&e點擊向右平移"
                    )
            ));
        } else {
            inventory.setItem(43, createButton(Material.GRAY_STAINED_GLASS_PANE, "&8(已在最右側)", null));
        }

        // 3. 渲染 Row 5 (Slot 45 ~ 53)：工具列
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, createButton(Material.GRAY_STAINED_GLASS_PANE, " ", null));
        }

        // Slot 45: ⬅ 上一步 (返回步驟三：怪物名單挑選)
        inventory.setItem(45, createButton(
                Material.ARROW,
                "&e⬅ 上一步 &f(怪物名單挑選)",
                List.of("&7返回 [步驟 3/6] 挑選或修改備選怪物清單")
        ));

        // Slot 47: ➕ 新增下一輪波次
        inventory.setItem(47, createButton(
                Material.EMERALD,
                "&a➕ 新增下一輪波次",
                List.of(
                        "&7為試煉挑戰新增一輪新的波次 (第 " + (totalWaves + 1) + " 輪)",
                        "&7點擊立即在末尾新增一輪 (預設 1 個格子)",
                        "&e點擊新增波次"
                )
        ));

        // Slot 49: 🎲 一鍵全部隨機
        inventory.setItem(49, createButton(
                Material.FIREWORK_STAR,
                "&d🎲 一鍵全部設為【隨機】",
                List.of(
                        "&7將所有輪次中的全部格子皆設為【隨機抽取】",
                        "&7一鍵套用經典隨機出怪",
                        "&e點擊一鍵套用"
                )
        ));

        // Slot 51: 快速刪除模式按鈕 (點擊切換，全流程支援純左鍵操作)
        if (!deleteMode) {
            inventory.setItem(51, createButton(
                    Material.HOPPER,
                    "&c🗑 開啟刪除模式",
                    List.of(
                            "&7點擊開啟快速刪除模式",
                            "&7開啟後，可透過 &e左鍵點擊 &7上方怪物格子直接刪除",
                            "&7或左鍵點擊輪數標籤直接刪除整輪",
                            "&7(全流程均可純靠左鍵完成)",
                            "&7",
                            "&7當前狀態: &8【已關閉】",
                            "&e[左鍵點擊] &c開啟刪除模式"
                    )
            ));
        } else {
            inventory.setItem(51, createButton(
                    Material.REDSTONE_BLOCK,
                    "&a✔ 關閉刪除模式",
                    List.of(
                            "&e當前已開啟刪除模式！",
                            "&c左鍵點擊上方任意怪物格子即可立即將其刪除",
                            "&c左鍵點擊輪數標籤即可立即刪除該整輪",
                            "&7",
                            "&7當前狀態: &c&l【開啟中 (點格即刪)】",
                            "&e[左鍵點擊] &a關閉刪除模式 (恢復正常)"
                    )
            ));
        }

        // Slot 53: ✔ 完成編排 (下一步，若有未指定的紅色格子則鎖定)
        if (template.hasUnsetInWaves()) {
            inventory.setItem(53, createButton(
                    Material.RED_CONCRETE,
                    "&c✖ 下一步 (鎖定中)",
                    List.of(
                            "&7看板中尚有 &c紅色佔位格子 &7尚未指定怪物！",
                            "&7",
                            "&f▪ 請點擊紅色格子挑選生物或隨機",
                            "&f▪ 或進入格子內點擊 &c[🗑 刪除此怪物格子]",
                            "&7",
                            "&c⚠ 所有格子皆完成指定後才可解鎖下一步"
                    )
            ));
        } else {
            inventory.setItem(53, createButton(
                    Material.LIME_CONCRETE,
                    "&a✔ 完成下一步 ➜",
                    List.of(
                            "&7順序編排完成，前往下一步進行冷卻設定",
                            "&a點擊前往下一步"
                    )
            ));
        }
    }

    private ItemStack createMobCellItem(SpawnerTemplate template, String mobKey, int waveIndex, int mobPosInWave) {
        int waveNum = waveIndex + 1;
        int posNum = mobPosInWave + 1;

        // 若尚未設定或為空，使用紅色玻璃片佔位
        if (mobKey == null || mobKey.trim().isEmpty() || "UNSET".equalsIgnoreCase(mobKey.trim())) {
            ItemStack redGlass = new ItemStack(Material.RED_STAINED_GLASS_PANE);
            ItemMeta meta = redGlass.getItemMeta();
            if (meta != null) {
                String title = "&c第 " + waveNum + " 輪的第 " + posNum + " 隻: &4[未指定]";
                if (deleteMode) {
                    title = "&c[點擊刪除] " + title;
                }
                meta.displayName(TextUtil.parse(title));
                List<Component> lore = new ArrayList<>();
                lore.add(TextUtil.parse("&8------------------------"));
                lore.add(TextUtil.parse("&7狀態: &c🔴 尚未指定 (紅色佔位)"));
                lore.add(TextUtil.parse("&7"));
                if (deleteMode) {
                    lore.add(TextUtil.parse("&c&l【刪除模式啟用中】"));
                    lore.add(TextUtil.parse("&e[左鍵點擊] &c立即刪除此怪物格子！"));
                } else {
                    lore.add(TextUtil.parse("&e[點擊此處] &f開啟怪物列表挑選指定或刪除"));
                }
                lore.add(TextUtil.parse("&8------------------------"));
                meta.lore(lore);
                redGlass.setItemMeta(meta);
            }
            return redGlass;
        }

        // 若為隨機
        if ("RANDOM".equalsIgnoreCase(mobKey.trim())) {
            ItemStack star = new ItemStack(Material.FIREWORK_STAR);
            ItemMeta meta = star.getItemMeta();
            if (meta != null) {
                String title = "&d第 " + waveNum + " 輪的第 " + posNum + " 隻: &e🎲 隨機抽取";
                if (deleteMode) {
                    title = "&c[點擊刪除] " + title;
                }
                meta.displayName(TextUtil.parse(title));
                List<Component> lore = new ArrayList<>();
                lore.add(TextUtil.parse("&8------------------------"));
                lore.add(TextUtil.parse("&7生成對象: &d🎲 隨機抽取 (由怪物池依機率生成)"));
                lore.add(TextUtil.parse("&7"));
                if (deleteMode) {
                    lore.add(TextUtil.parse("&c&l【刪除模式啟用中】"));
                    lore.add(TextUtil.parse("&e[左鍵點擊] &c立即刪除此怪物格子！"));
                } else {
                    lore.add(TextUtil.parse("&e[點擊] &f重新挑選、更換生物或刪除此格"));
                }
                lore.add(TextUtil.parse("&8------------------------"));
                meta.lore(lore);
                star.setItemMeta(meta);
            }
            return star;
        }

        // 若為指定怪物
        SpawnerMobEntry matched = null;
        for (SpawnerMobEntry e : template.getMobPool()) {
            if (e.getMobId().equalsIgnoreCase(mobKey) || ("mm:" + e.getMobId()).equalsIgnoreCase(mobKey)) {
                matched = e;
                break;
            }
        }

        Material icon = (matched != null) ? matched.getIconMaterial() : Material.ZOMBIE_SPAWN_EGG;
        String displayName = (matched != null) ? matched.getDisplayName() : mobKey;
        boolean isMythic = (matched != null) ? matched.isMythic() : mobKey.toLowerCase().startsWith("mm:");

        ItemStack item = new ItemStack(icon != null ? icon : Material.SPAWNER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String prefix = isMythic ? "&d[Mythic] &f" : "&a[原版] &f";
            String title = "&b第 " + waveNum + " 輪的第 " + posNum + " 隻: " + prefix + displayName;
            if (deleteMode) {
                title = "&c[點擊刪除] " + title;
            }
            meta.displayName(TextUtil.parse(title));
            List<Component> lore = new ArrayList<>();
            lore.add(TextUtil.parse("&8------------------------"));
            lore.add(TextUtil.parse("&7內部代號: &f" + mobKey));
            lore.add(TextUtil.parse("&7怪物類型: " + (isMythic ? "&dMythicMob 自訂怪物" : "&a原版生物")));
            lore.add(TextUtil.parse("&7"));
            if (deleteMode) {
                lore.add(TextUtil.parse("&c&l【刪除模式啟用中】"));
                lore.add(TextUtil.parse("&e[左鍵點擊] &c立即刪除此怪物格子！"));
            } else {
                lore.add(TextUtil.parse("&e[點擊] &f重新挑選、更換生物或刪除此格"));
            }
            lore.add(TextUtil.parse("&8------------------------"));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
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
        SpawnerTemplate template = context.getTemplate();
        List<List<String>> waves = template.getWaves();
        int totalWaves = waves.size();

        int maxWaveLength = 0;
        for (List<String> w : waves) {
            if (w != null && w.size() > maxWaveLength) {
                maxWaveLength = w.size();
            }
        }
        int maxScrollRow = Math.max(0, (totalWaves + 1) - VISIBLE_ROWS);
        int maxScrollCol = Math.max(0, (maxWaveLength + 1) - VISIBLE_COLS);

        int row = slot / 9;
        int col = slot % 9;

        // 1. 點擊前 4 行波次矩陣 (Row 0 ~ 3)
        if (row < VISIBLE_ROWS) {
            int waveIndex = scrollRow + row;

            // Col 0: 輪次標籤 或 新增輪次
            if (col == 0) {
                if (waveIndex == totalWaves) {
                    // 點擊新增第 X 輪
                    List<String> newWave = new ArrayList<>();
                    newWave.add("RANDOM");
                    waves.add(newWave);
                    context.getPlugin().getConfigManager().playSound(player, "click");
                    player.sendMessage(TextUtil.parse("&8[&6CustomLootX&8] &a已新增【第 " + waves.size() + " 輪】！"));
                    render();
                    return;
                } else if (waveIndex < totalWaves) {
                    if (deleteMode || (event.isRightClick() && totalWaves > 1)) {
                        if (totalWaves > 1) {
                            waves.remove(waveIndex);
                            context.getPlugin().getConfigManager().playSound(player, "click");
                            player.sendMessage(TextUtil.parse("&8[&6CustomLootX&8] &c已刪除【第 " + (waveIndex + 1) + " 輪】！"));
                            render();
                        } else {
                            player.sendMessage(TextUtil.parse("&8[&6CustomLootX&8] &c生怪磚至少需保留 1 輪！"));
                            context.getPlugin().getConfigManager().playSound(player, "error");
                        }
                        return;
                    }
                }
                return;
            }

            // Col 1 ~ 7: 怪物格子 或 ➕ 增加格子按鈕
            if (col >= 1 && col <= VISIBLE_COLS) {
                int mobPosInWave = scrollCol + (col - 1);

                if (waveIndex < totalWaves) {
                    List<String> wave = waves.get(waveIndex);
                    int waveSize = (wave != null) ? wave.size() : 0;

                    if (mobPosInWave < waveSize) {
                        if (deleteMode) {
                            // 快速刪除模式：左鍵點擊直接刪除此格
                            wave.remove(mobPosInWave);
                            if (wave.isEmpty()) {
                                if (waves.size() > 1) {
                                    waves.remove(waveIndex);
                                    player.sendMessage(TextUtil.parse("&8[&6CustomLootX&8] &c第 " + (waveIndex + 1) + " 輪所有格子已清空，已自動移除該輪！"));
                                } else {
                                    wave.add("RANDOM");
                                    player.sendMessage(TextUtil.parse("&8[&6CustomLootX&8] &c生怪磚至少需保留 1 隻怪物！"));
                                }
                            } else {
                                player.sendMessage(TextUtil.parse("&8[&6CustomLootX&8] &c已刪除第 " + (waveIndex + 1) + " 輪的第 " + (mobPosInWave + 1) + " 個怪物格子！"));
                            }
                            context.getPlugin().getConfigManager().playSound(player, "click");
                            render();
                            return;
                        }

                        // 正常模式：開啟挑選彈窗
                        context.getPlugin().getConfigManager().playSound(player, "click");
                        context.setTransitioning(true);
                        new SpawnerSequenceSelectMobGui(context, waveIndex, mobPosInWave, scrollRow, scrollCol).open();
                        context.setTransitioning(false);
                        return;
                    } else if (mobPosInWave == waveSize) {
                        // 點擊 ➕ 增加格子
                        if (wave != null) {
                            wave.add("RANDOM");
                            context.getPlugin().getConfigManager().playSound(player, "click");
                            render();
                        }
                        return;
                    }
                }
                return;
            }

            // Col 8: 最右直行平移按鈕
            if (col == 8) {
                if (row == 0 && scrollRow > 0) {
                    scrollRow--;
                    context.getPlugin().getConfigManager().playSound(player, "click");
                    render();
                } else if (row == 3 && scrollRow < maxScrollRow) {
                    scrollRow++;
                    context.getPlugin().getConfigManager().playSound(player, "click");
                    render();
                }
                return;
            }
            return;
        }

        // 2. 點擊 Row 4 橫向平移控制按鈕
        if (slot == 37 && scrollCol > 0) {
            scrollCol--;
            context.getPlugin().getConfigManager().playSound(player, "click");
            render();
            return;
        } else if (slot == 43 && scrollCol < maxScrollCol) {
            scrollCol++;
            context.getPlugin().getConfigManager().playSound(player, "click");
            render();
            return;
        }

        // 3. 點擊 Row 5 工具列
        if (slot == 45) {
            // 上一步 (返回步驟三：怪物名單挑選)
            context.getPlugin().getConfigManager().playSound(player, "click");
            context.setTransitioning(true);
            new SpawnerWizardStep3MobGui(context, 1).open();
            context.setTransitioning(false);
        } else if (slot == 47) {
            // ➕ 新增下一輪波次
            List<String> newWave = new ArrayList<>();
            newWave.add("RANDOM");
            waves.add(newWave);
            scrollRow = Math.max(0, waves.size() + 1 - VISIBLE_ROWS);
            context.getPlugin().getConfigManager().playSound(player, "click");
            player.sendMessage(TextUtil.parse("&8[&6CustomLootX&8] &a已新增【第 " + waves.size() + " 輪】！"));
            render();
        } else if (slot == 49) {
            // 一鍵全部設為隨機
            for (List<String> wave : waves) {
                if (wave != null) {
                    for (int i = 0; i < wave.size(); i++) {
                        wave.set(i, "RANDOM");
                    }
                }
            }
            context.getPlugin().getConfigManager().playSound(player, "save");
            player.sendMessage(TextUtil.parse("&8[&6CustomLootX&8] &a已將所有生成位置設為【🎲 隨機抽取】！"));
            render();
        } else if (slot == 51) {
            // 快速刪除模式按鈕
            deleteMode = !deleteMode;
            context.getPlugin().getConfigManager().playSound(player, "click");
            render();
            return;
        } else if (slot == 53) {
            // 完成下一步 (若有未指定格子則阻止)
            if (template.hasUnsetInWaves()) {
                context.getPlugin().getConfigManager().playSound(player, "error");
                player.sendMessage(TextUtil.parse("&8[&6CustomLootX&8] &c看板中尚有紅色未指定格子，請先完成指定或點擊該格刪除！"));
                return;
            }

            boolean hasRandom = template.hasRandomInSequence();
            context.getPlugin().getConfigManager().playSound(player, "click");
            context.setTransitioning(true);

            if (hasRandom) {
                if (!template.isTotalMobChanceValid()) {
                    List<SpawnerMobEntry> pool = template.getMobPool();
                    if (!pool.isEmpty()) {
                        double each = TextUtil.roundChance(100.0 / pool.size());
                        double sum = 0.0;
                        for (int i = 0; i < pool.size(); i++) {
                            if (i == pool.size() - 1) {
                                pool.get(i).setChance(TextUtil.roundChance(100.0 - sum));
                            } else {
                                pool.get(i).setChance(each);
                                sum += each;
                            }
                        }
                    }
                }
                new SpawnerWizardStep4RandomChanceGui(context).open();
            } else {
                new SpawnerWizardStep4CooldownGui(context).open();
            }
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
