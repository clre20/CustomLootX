package clre20.customLootX.gui.wizard.spawner;

import clre20.customLootX.gui.CustomGuiHolder;
import clre20.customLootX.model.SpawnerTemplate;
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

/**
 * 試煉生怪磚 - 完成提示音效專屬設定介面 (精選音效、直接輸入、即時試聽、音量與音調微調)
 */
public class SpawnerVictorySoundGui extends CustomGuiHolder {

    private final SpawnerWizardContext context;

    public record CuratedSound(String soundName, String displayName, Material icon, String category, String desc) {}

    public static final List<CuratedSound> CURATED_SOUNDS = List.of(
            // 試煉大廳與獎勵類
            new CuratedSound("UI_TOAST_CHALLENGE_COMPLETE", "挑戰完成 (預設)", Material.BELL, "試煉經典", "試煉大廳經典挑戰完成之輝煌號角慶祝音"),
            new CuratedSound("ENTITY_PLAYER_LEVELUP", "玩家升級", Material.EXPERIENCE_BOTTLE, "經典音效", "原版玩家升級經典叮咚聲，清脆明亮"),
            new CuratedSound("BLOCK_TRIAL_SPAWNER_OPEN_SHUTTER", "百葉窗開啟", Material.IRON_TRAPDOOR, "試煉大廳", "試煉生怪磚百葉窗打開時的厚重金屬機械音"),
            new CuratedSound("BLOCK_TRIAL_SPAWNER_DETECT_PLAYER", "生怪磚感應", Material.FIRE_CHARGE, "試煉大廳", "試煉生怪磚偵測到挑戰者時的低沉共鳴音"),
            new CuratedSound("BLOCK_VAULT_OPEN_SHUTTER", "寶庫開鎖", Material.CHEST, "試煉寶庫", "試煉寶庫旋轉開啟時的鎖栓機械展開聲"),
            new CuratedSound("BLOCK_VAULT_EJECT_ITEM", "彈射戰利品", Material.DISPENSER, "試煉寶庫", "寶庫向上噴發獎勵時的推射音效"),
            new CuratedSound("UI_TOAST_IN", "成就彈窗", Material.GOLD_INGOT, "提示音", "右上方成就與挑戰彈出時的俐落提示音"),

            // 號角與慶祝類
            new CuratedSound("ITEM_GOAT_HORN_SOUND_0", "山羊號角 - 沉思", Material.GOAT_HORN, "山羊號角", "莊嚴渾厚、迴盪遠方的原野山羊號角聲"),
            new CuratedSound("ITEM_GOAT_HORN_SOUND_1", "山羊號角 - 歌唱", Material.GOAT_HORN, "山羊號角", "悠揚歡樂的高亢歌唱號角旋律"),
            new CuratedSound("ITEM_GOAT_HORN_SOUND_2", "山羊號角 - 尋覓", Material.GOAT_HORN, "山羊號角", "極具穿透力與探索氣息的號角長鳴"),
            new CuratedSound("ITEM_GOAT_HORN_SOUND_7", "山羊號角 - 夢想", Material.GOAT_HORN, "山羊號角", "激勵人心、象徵勝利與希望的戰歌號角"),
            new CuratedSound("ENTITY_FIREWORK_ROCKET_BLAST", "煙火爆鳴", Material.FIREWORK_ROCKET, "慶典煙火", "煙火升空後絢爛綻放的劇烈爆炸轟鳴"),
            new CuratedSound("ENTITY_FIREWORK_ROCKET_TWINKLE", "璀璨微光", Material.GLOWSTONE_DUST, "慶典煙火", "煙火散落時漫天金色微光閃爍啪啪聲"),
            new CuratedSound("EVENT_RAID_HORN", "襲擊角笛", Material.CROSSBOW, "襲擊戰鬥", "災厄陣營深邃沉重的襲擊遠古號角"),

            // 魔法與史詩首領類
            new CuratedSound("ENTITY_ENDER_DRAGON_GROWL", "巨龍咆哮", Material.DRAGON_HEAD, "史詩首領", "天際霸主終界巨龍的震撼狂暴咆哮"),
            new CuratedSound("ENTITY_WITHER_SPAWN", "凋零降臨", Material.NETHER_STAR, "史詩首領", "下界首領凋零降臨時極具壓迫感的轟鳴震音"),
            new CuratedSound("BLOCK_BEACON_ACTIVATE", "信標激活", Material.BEACON, "魔法奇蹟", "烽火台光柱直衝雲霄時的強烈神聖脈衝音"),
            new CuratedSound("BLOCK_BEACON_POWER_SELECT", "神力賜福", Material.DIAMOND, "魔法奇蹟", "信標賦予玩家神聖增益時的清亮共鳴音"),
            new CuratedSound("BLOCK_ENCHANTMENT_TABLE_USE", "奧術附魔", Material.ENCHANTING_TABLE, "魔法奇蹟", "附魔台古老符文翻動時的神秘魔力微光聲"),
            new CuratedSound("BLOCK_END_PORTAL_SPAWN", "終界傳送", Material.ENDER_EYE, "史詩冒險", "終界傳送門激活動盪時的深邃空靈音效"),
            new CuratedSound("BLOCK_RESPAWN_ANCHOR_SET_SPAWN", "錨定重生", Material.RESPAWN_ANCHOR, "下界冒險", "下界重生錨充滿能量並錨定靈魂的共鳴音"),
            new CuratedSound("ITEM_TOTEM_USE", "圖騰庇佑", Material.TOTEM_OF_UNDYING, "神聖奇蹟", "不死圖騰發動奇蹟抵禦致命傷害的神聖音"),

            // 清脆樂器與音階類
            new CuratedSound("BLOCK_NOTE_BLOCK_CHIME", "音階盒 - 管鐘", Material.NOTE_BLOCK, "樂器音階", "清澈悠揚的金屬管鐘打擊音"),
            new CuratedSound("BLOCK_NOTE_BLOCK_BELL", "音階盒 - 鈴鐺", Material.NOTE_BLOCK, "樂器音階", "明亮清脆的小鈴鐺敲擊聲"),
            new CuratedSound("BLOCK_NOTE_BLOCK_FLUTE", "音階盒 - 長笛", Material.NOTE_BLOCK, "樂器音階", "柔美悠遠的木管長笛音調"),
            new CuratedSound("BLOCK_NOTE_BLOCK_PLING", "音階盒 - 叮音", Material.NOTE_BLOCK, "樂器音階", "經典清脆的電子叮音提示聲"),
            new CuratedSound("BLOCK_AMETHYST_BLOCK_CHIME", "紫水晶群共振", Material.AMETHYST_SHARD, "自然清澈", "紫水晶簇受到觸碰時發出的清冽清脆水晶共鳴音")
    );

    public SpawnerVictorySoundGui(SpawnerWizardContext context) {
        this.context = context;
        this.inventory = Bukkit.createInventory(this, 54, TextUtil.parse("&8試煉完成提示音效設定"));
        render();
    }

    public void open() {
        context.getPlayer().openInventory(this.inventory);
    }

    private void render() {
        inventory.clear();

        SpawnerTemplate template = context.getTemplate();

        // 頂部控制面板背景
        String fillerName = context.getPlugin().getConfigManager().getText("gui.common.filler-name", " ");
        ItemStack filler = createButton(Material.GRAY_STAINED_GLASS_PANE, fillerName, null, false);
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, filler);
        }

        // Slot 0: 開關切換按鈕
        boolean soundOn = template.isVictorySoundEnabled();
        String toggleTitle = soundOn ? "&a✔ 提示音效: 已開啟" : "&c✖ 提示音效: 已關閉";
        List<String> toggleLore = List.of(
                "&7控制該生怪磚是否在挑戰勝利時播放音效",
                "&7目前狀態: " + (soundOn ? "&a開啟" : "&c關閉"),
                "&7",
                "&e[點擊] &f切換 開啟 / 關閉"
        );
        inventory.setItem(0, createButton(soundOn ? Material.LIME_DYE : Material.RED_DYE, toggleTitle, toggleLore, false));

        // Slot 2: 當前設定音效與即時試聽
        String currentSound = template.getVictorySound();
        String currentTitle = "&6【當前設定音效】: &e" + currentSound;
        List<String> currentLore = List.of(
                "&7挑戰成功獲勝時播放之提示音效",
                "&7",
                "&7啟用狀態: " + (soundOn ? "&a✔ 已開啟" : "&c✖ 已關閉"),
                "&7目前音量: &a" + String.format("%.1f", template.getVictorySoundVolume()),
                "&7目前音調: &a" + String.format("%.1f", template.getVictorySoundPitch()),
                "&7",
                "&a[點擊試聽] &f在此處播放一次當前音效"
        );
        inventory.setItem(2, createButton(Material.JUKEBOX, currentTitle, currentLore, true));

        // Slot 4: 聊天欄手動輸入按鈕
        String inputTitle = "&b✍ 聊天室直接輸入音效代號";
        List<String> inputLore = List.of(
                "&7支援手動輸入任意 Minecraft 原版音效名稱",
                "&7亦支援自訂資源包之音效代號 (如 custom.victory)",
                "&7",
                "&e[點擊] &f開始在聊天欄輸入",
                "&7(輸入 cancel 可隨時取消並返回此處)"
        );
        inventory.setItem(4, createButton(Material.WRITABLE_BOOK, inputTitle, inputLore, false));

        // Slot 6: 音量單擊循環調整
        String volTitle = "&d♫ 調整音量: &a" + String.format("%.1f", template.getVictorySoundVolume());
        List<String> volLore = List.of(
                "&7調整提示音效播放時的音量大小",
                "&7目前數值: &a" + String.format("%.1f", template.getVictorySoundVolume()) + " &8(範圍: 0.2 ~ 2.0)",
                "&7",
                "&e[點擊] &f循環增加音量 (+0.2)",
                "&7達到 2.0 後將自動回到 0.2",
                "&7每次點擊皆會即時試聽！"
        );
        inventory.setItem(6, createButton(Material.BELL, volTitle, volLore, false));

        // Slot 7: 音調單擊循環調整
        String pitchTitle = "&e♩ 調整音調: &a" + String.format("%.1f", template.getVictorySoundPitch());
        List<String> pitchLore = List.of(
                "&7調整提示音效播放時的音調高低",
                "&7目前數值: &a" + String.format("%.1f", template.getVictorySoundPitch()) + " &8(範圍: 0.6 ~ 2.0)",
                "&7",
                "&e[點擊] &f循環增加音調 (+0.2)",
                "&7達到 2.0 後將自動回到 0.6",
                "&7每次點擊皆會即時試聽！"
        );
        inventory.setItem(7, createButton(Material.NOTE_BLOCK, pitchTitle, pitchLore, false));

        // Slot 8: 還原預設
        String resetTitle = "&7↺ 還原預設音量/音調";
        List<String> resetLore = List.of(
                "&7將音量與音調重設為原廠建議值",
                "&7預設音量: &a1.0 &7| 預設音調: &a1.2",
                "&7",
                "&e[點擊] &f立即重設為預設值"
        );
        inventory.setItem(8, createButton(Material.HOPPER, resetTitle, resetLore, false));

        // 分隔線 (Row 1: Slots 9 ~ 17)
        for (int i = 9; i < 18; i++) {
            inventory.setItem(i, filler);
        }

        // 精選音效列表 (Row 2 ~ Row 4: Slots 18 ~ 44，共 27 格)
        for (int i = 0; i < CURATED_SOUNDS.size() && i < 27; i++) {
            CuratedSound sound = CURATED_SOUNDS.get(i);
            int slot = 18 + i;
            boolean isSelected = sound.soundName().equalsIgnoreCase(currentSound);

            String title = (isSelected ? "&a✔ &e" : "&f") + sound.displayName();
            List<String> lore = new ArrayList<>();
            lore.add("&8------------------------");
            if (isSelected) {
                lore.add("&a【目前正在使用此音效】");
            }
            lore.add("&7內部代號: &f" + sound.soundName());
            lore.add("&7音效分類: &b" + sound.category());
            lore.add("&7風格說明: &7" + sound.desc());
            lore.add("&8------------------------");
            lore.add(isSelected ? "&e[點擊] &f再次試聽此音效" : "&e[點擊] &f選擇此音效並即時試聽");

            inventory.setItem(slot, createButton(sound.icon(), title, lore, isSelected));
        }

        // 底部工具列 (Row 5: Slots 45 ~ 53)
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        // Slot 45: 返回步驟三
        inventory.setItem(45, createButton(Material.ARROW, "&e⬅ 返回 &f[步驟 3/6] 波次與數量設定", List.of("&7返回波次設定介面"), false));

        // Slot 53: 完成設定並返回
        inventory.setItem(53, createButton(Material.LIME_CONCRETE, "&a✔ 儲存並返回 &f[步驟 3/6]", List.of("&7確認當前音效設定並返回步驟三"), false));
    }

    private ItemStack createButton(Material mat, String name, List<String> loreLines, boolean glint) {
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
            if (glint) {
                meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private void playPreview(Player player, String soundName) {
        float volume = context.getTemplate().getVictorySoundVolume();
        float pitch = context.getTemplate().getVictorySoundPitch();
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            player.playSound(player.getLocation(), soundName.toLowerCase(), volume, pitch);
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        SpawnerTemplate template = context.getTemplate();

        if (slot == 0) {
            // 切換開啟/關閉
            template.setVictorySoundEnabled(!template.isVictorySoundEnabled());
            context.getPlugin().getConfigManager().playSound(player, "click");
            render();
        } else if (slot == 2) {
            // 試聽當前音效
            playPreview(player, template.getVictorySound());
        } else if (slot == 4) {
            // 聊天欄手動輸入
            context.getPlugin().getConfigManager().playSound(player, "click");
            context.setTransitioning(true);
            context.getPlugin().getChatInputManager().requestInput(
                    player,
                    "&e請在聊天室輸入自訂音效名稱 (例如 UI_TOAST_CHALLENGE_COMPLETE, ENTITY_PLAYER_LEVELUP)，輸入 &ccancel &e取消：",
                    input -> {
                        if (input == null || input.trim().isEmpty() || input.equalsIgnoreCase("cancel")) {
                            open();
                            context.setTransitioning(false);
                            return;
                        }
                        String soundName = input.trim();
                        try {
                            Sound testSound = Sound.valueOf(soundName.toUpperCase());
                            template.setVictorySound(testSound.name());
                            playPreview(player, testSound.name());
                            player.sendMessage(TextUtil.parse("&a[CustomLootX] 已將試煉完成提示音效設定為: &e" + testSound.name()));
                        } catch (IllegalArgumentException e) {
                            template.setVictorySound(soundName);
                            playPreview(player, soundName);
                            player.sendMessage(TextUtil.parse("&a[CustomLootX] 已將試煉完成提示音效設定為: &e" + soundName + " &7(自訂名稱)"));
                        }
                        open();
                        context.setTransitioning(false);
                    },
                    () -> {
                        open();
                        context.setTransitioning(false);
                    }
            );
        } else if (slot == 6) {
            // 單擊循環調整音量 (+0.2)
            float v = template.getVictorySoundVolume() + 0.2f;
            if (v > 2.01f) {
                v = 0.2f;
            }
            v = Math.round(v * 10.0f) / 10.0f;
            template.setVictorySoundVolume(v);
            playPreview(player, template.getVictorySound());
            render();
        } else if (slot == 7) {
            // 單擊循環調整音調 (+0.2)
            float p = template.getVictorySoundPitch() + 0.2f;
            if (p > 2.01f) {
                p = 0.6f;
            }
            p = Math.round(p * 10.0f) / 10.0f;
            template.setVictorySoundPitch(p);
            playPreview(player, template.getVictorySound());
            render();
        } else if (slot == 8) {
            // 還原預設值 (音量 1.0, 音調 1.2)
            template.setVictorySoundVolume(1.0f);
            template.setVictorySoundPitch(1.2f);
            context.getPlugin().getConfigManager().playSound(player, "click");
            playPreview(player, template.getVictorySound());
            render();
        } else if (slot >= 18 && slot <= 44) {
            // 精選音效點擊選取
            int index = slot - 18;
            if (index >= 0 && index < CURATED_SOUNDS.size()) {
                CuratedSound selected = CURATED_SOUNDS.get(index);
                template.setVictorySound(selected.soundName());
                playPreview(player, selected.soundName());
                render();
            }
        } else if (slot == 45 || slot == 53) {
            // 返回步驟二
            context.getPlugin().getConfigManager().playSound(player, "click");
            context.setTransitioning(true);
            new SpawnerWizardStep2WavesGui(context).open();
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

