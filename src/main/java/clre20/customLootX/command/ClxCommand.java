package clre20.customLootX.command;

import clre20.customLootX.CustomLootX;
import clre20.customLootX.gui.wizard.WizardContext;
import clre20.customLootX.gui.wizard.vault.VaultWizardContext;
import clre20.customLootX.gui.wizard.vault.VaultWizardStep1Gui;
import clre20.customLootX.gui.wizard.spawner.SpawnerWizardContext;
import clre20.customLootX.gui.wizard.spawner.SpawnerWizardStep1Gui;
import clre20.customLootX.model.LootTemplate;
import clre20.customLootX.model.VaultTemplate;
import clre20.customLootX.model.SpawnerTemplate;
import clre20.customLootX.util.PermissionUtil;
import clre20.customLootX.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ClxCommand implements CommandExecutor, TabCompleter {

    private final CustomLootX plugin;

    public ClxCommand(CustomLootX plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // 1. /clx 或 /clx help：所有玩家皆可執行查看說明
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender, label);
            return true;
        }

        // 2. 其他所有子指令：只能由 OP 等級 2 以上（含）或具備 customlootx.admin 權限的管理者執行
        if (!PermissionUtil.hasAdminPermission(sender)) {
            plugin.getConfigManager().send(sender, "no-permission");
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "create" -> handleCreate(sender, label, args);
            case "give" -> handleGive(sender, label, args);
            case "key" -> handleKey(sender, label, args);
            case "edit" -> handleEdit(sender, label, args);
            case "list" -> handleList(sender, label, args);
            case "delete" -> handleDelete(sender, label, args);
            case "reload" -> handleReload(sender, label);
            default -> sendHelp(sender, label);
        }

        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(plugin.getConfigManager().getComponent("commands.help.header", "&8============ &6CustomLootX 指令清單 &8============"));
        sender.sendMessage(plugin.getConfigManager().getComponent("commands.help.create", "&e/%label% create <suspicious|vault|spawner> [ominous|normal] &7- 創建全新空白可疑方塊、試煉寶庫或生怪磚", "%label%", label));
        sender.sendMessage(plugin.getConfigManager().getComponent("commands.help.give", "&e/%label% give <suspicious|vault|spawner> <名稱> [給誰] [數量] &7- 取得或給予已設定的方塊物品", "%label%", label));
        sender.sendMessage(plugin.getConfigManager().getComponent("commands.help.key", "&e/%label% key <寶庫名稱> [給誰] [數量] &7- 發放指定寶庫的專屬防偽鑰匙", "%label%", label));
        sender.sendMessage(plugin.getConfigManager().getComponent("commands.help.edit", "&e/%label% edit <suspicious|vault|spawner> <名稱> &7- 直接開啟指定配置的步驟引導介面", "%label%", label));
        sender.sendMessage(plugin.getConfigManager().getComponent("commands.help.list", "&e/%label% list [suspicious|vault|spawner] &7- 列出所有已儲存的配置", "%label%", label));
        sender.sendMessage(plugin.getConfigManager().getComponent("commands.help.delete", "&e/%label% delete <suspicious|vault|spawner> <名稱> &7- 刪除指定的配置檔案", "%label%", label));
        sender.sendMessage(plugin.getConfigManager().getComponent("commands.help.reload", "&e/%label% reload &7- 重新讀取所有設定與資料檔", "%label%", label));
        sender.sendMessage(plugin.getConfigManager().getComponent("commands.help.footer", "&8=============================================="));
    }

    /**
     * /clx create <suspicious|vault|spawner> [ominous|normal]
     */
    private void handleCreate(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getConfigManager().send(sender, "only-player");
            return;
        }

        if (args.length < 2) {
            plugin.getConfigManager().send(player, "create-usage", "%label%", label);
            return;
        }

        String category = args[1].toLowerCase();
        if (category.equals("spawner")) {
            boolean ominous = (args.length >= 3 && args[2].equalsIgnoreCase("ominous"));
            ItemStack item = plugin.getItemManager().createBlankSpawnerItem(ominous);
            player.getInventory().addItem(item);
            String typeName = "試煉生怪磚";
            plugin.getConfigManager().send(player, "spawner-template-created",
                    "%type%", typeName,
                    "%類型%", typeName,
                    "%類型(中文)%", typeName);
            plugin.getConfigManager().playSound(player, "success");
            plugin.getConfigManager().log("cmd-create-spawner", "%player%", player.getName(), "%label%", label);
        } else if (category.equals("vault")) {
            boolean ominous = (args.length >= 3 && args[2].equalsIgnoreCase("ominous"));
            ItemStack item = plugin.getItemManager().createBlankVaultItem(ominous);
            player.getInventory().addItem(item);
            String typeName = "試煉寶庫";
            plugin.getConfigManager().send(player, "vault-template-created",
                    "%type%", typeName,
                    "%類型%", typeName,
                    "%類型(中文)%", typeName);
            plugin.getConfigManager().playSound(player, "success");
            plugin.getConfigManager().log("cmd-create-vault", "%player%", player.getName(), "%label%", label);
        } else if (category.equals("suspicious") || category.equals("sand") || category.equals("gravel")) {
            Material mat = category.equals("gravel") ? Material.SUSPICIOUS_GRAVEL : Material.SUSPICIOUS_SAND;
            ItemStack item = plugin.getItemManager().createBlankItem(mat);
            player.getInventory().addItem(item);
            String typeName = category.equals("gravel") ? "可疑砂礫" : (category.equals("sand") ? "可疑沙" : "可疑");
            plugin.getConfigManager().send(player, "template-created",
                    "%type%", typeName,
                    "%類型%", typeName,
                    "%類型(中文)%", typeName);
            plugin.getConfigManager().playSound(player, "success");
            plugin.getConfigManager().log("cmd-create", "%player%", player.getName(), "%label%", label);
        } else {
            plugin.getConfigManager().send(player, "create-usage", "%label%", label);
        }
    }

    /**
     * /clx give <suspicious|vault> <名稱> [給誰] [數量]
     */
    private void handleGive(CommandSender sender, String label, String[] args) {
        if (args.length < 3) {
            plugin.getConfigManager().send(sender, "give-usage", "%label%", label);
            return;
        }

        String category = args[1].toLowerCase();
        String name = args[2].toLowerCase();

        boolean isVault = category.equals("vault");
        boolean isSuspicious = category.equals("suspicious");
        boolean isSpawner = category.equals("spawner");

        if (!isVault && !isSuspicious && !isSpawner) {
            // 容錯機制：若玩家未輸入分類，嘗試自動比對
            if (plugin.getTemplateManager().getTemplate(category) != null) {
                name = category;
                isSuspicious = true;
            } else if (plugin.getVaultTemplateManager().getTemplate(category) != null) {
                name = category;
                isVault = true;
            } else if (plugin.getSpawnerTemplateManager().getTemplate(category) != null) {
                name = category;
                isSpawner = true;
            } else {
                plugin.getConfigManager().send(sender, "give-usage", "%label%", label);
                return;
            }
        }

        Player target;
        int amount = 1;

        if (isVault || isSuspicious || isSpawner && args.length >= 3) {
            // 格式: /clx give <category> <name> [player] [amount]
            if (args.length == 3) {
                if (sender instanceof Player p) {
                    target = p;
                } else {
                    plugin.getConfigManager().send(sender, "give-console-target-required", "%label%", label);
                    return;
                }
            } else {
                // args.length >= 4
                Player maybePlayer = Bukkit.getPlayer(args[3]);
                if (maybePlayer != null) {
                    target = maybePlayer;
                    if (args.length >= 5) {
                        try {
                            amount = Math.max(1, Integer.parseInt(args[4]));
                        } catch (NumberFormatException ignored) {}
                    }
                } else {
                    if (sender instanceof Player p) {
                        target = p;
                        try {
                            amount = Math.max(1, Integer.parseInt(args[3]));
                        } catch (NumberFormatException e) {
                            plugin.getConfigManager().send(sender, "player-not-found", "%player%", args[3]);
                            return;
                        }
                    } else {
                        plugin.getConfigManager().send(sender, "player-not-found", "%player%", args[3]);
                        return;
                    }
                }
            }
        } else {
            // 舊格式相容
            if (sender instanceof Player p) {
                target = p;
            } else {
                plugin.getConfigManager().send(sender, "give-console-target-required", "%label%", label);
                return;
            }
        }

        ItemStack item;
        if (isSpawner) {
            SpawnerTemplate template = plugin.getSpawnerTemplateManager().getTemplate(name);
            if (template == null) {
                plugin.getConfigManager().send(sender, "template-not-found", "%name%", name);
                return;
            }
            item = plugin.getItemManager().createTemplateSpawnerItem(template, amount);
        } else if (isVault) {
            VaultTemplate template = plugin.getVaultTemplateManager().getTemplate(name);
            if (template == null) {
                plugin.getConfigManager().send(sender, "template-not-found", "%name%", name);
                return;
            }
            item = plugin.getItemManager().createTemplateVaultItem(template, amount);
        } else {
            LootTemplate template = plugin.getTemplateManager().getTemplate(name);
            if (template == null) {
                plugin.getConfigManager().send(sender, "template-not-found", "%name%", name);
                return;
            }
            item = plugin.getItemManager().createTemplateItem(template, amount);
        }

        target.getInventory().addItem(item);

        plugin.getConfigManager().send(sender, "give-success", "%player%", target.getName(), "%amount%", amount, "%name%", name);
        if (!sender.equals(target)) {
            plugin.getConfigManager().send(target, "receive-success", "%amount%", amount, "%name%", name);
        }
        if (sender.equals(target)) {
            plugin.getConfigManager().send(sender, "give-self-tip");
        }

        plugin.getConfigManager().log("cmd-give", "%sender%", sender.getName(), "%label%", label, "%target%", target.getName(), "%name%", name, "%amount%", amount);
    }

    /**
     * /clx key <寶庫名稱> [給誰] [數量]
     */
    private void handleKey(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            plugin.getConfigManager().send(sender, "cmd-key-usage", "%label%", label);
            return;
        }

        String name = args[1].toLowerCase();
        clre20.customLootX.model.VaultTemplate template = plugin.getVaultTemplateManager().getTemplate(name);
        if (template == null) {
            plugin.getConfigManager().send(sender, "template-not-found", "%name%", name);
            if (sender instanceof Player p) {
                plugin.getConfigManager().playSound(p, "error");
            }
            return;
        }

        Player target;
        int amount = 1;

        if (args.length == 2) {
            if (sender instanceof Player p) {
                target = p;
            } else {
                plugin.getConfigManager().send(sender, "give-console-target-required", "%label%", label);
                return;
            }
        } else {
            Player maybe = Bukkit.getPlayer(args[2]);
            if (maybe != null) {
                target = maybe;
                if (args.length >= 4) {
                    try {
                        amount = Math.max(1, Integer.parseInt(args[3]));
                    } catch (NumberFormatException ignored) {}
                }
            } else {
                if (sender instanceof Player p) {
                    target = p;
                    try {
                        amount = Math.max(1, Integer.parseInt(args[2]));
                    } catch (NumberFormatException e) {
                        plugin.getConfigManager().send(sender, "player-not-found", "%player%", args[2]);
                        return;
                    }
                } else {
                    plugin.getConfigManager().send(sender, "player-not-found", "%player%", args[2]);
                    return;
                }
            }
        }

        ItemStack key = template.getKeyItem();
        if (key == null) {
            key = new ItemStack(template.isOminous() ? Material.OMINOUS_TRIAL_KEY : Material.TRIAL_KEY);
        }
        ItemStack toGive = key.clone();
        toGive.setAmount(amount);
        target.getInventory().addItem(toGive);

        plugin.getConfigManager().send(sender, "cmd-key-give", "%player%", target.getName(), "%amount%", amount, "%name%", name);
        if (!sender.equals(target)) {
            plugin.getConfigManager().send(target, "cmd-key-receive", "%amount%", amount, "%name%", name);
        }
        plugin.getConfigManager().playSound(target, "success");
        plugin.getConfigManager().log("cmd-key", "%sender%", sender.getName(), "%label%", label, "%target%", target.getName(), "%name%", name, "%amount%", amount);
    }

    /**
     * /clx edit <suspicious|vault> <名稱>
     */
    private void handleEdit(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getConfigManager().send(sender, "only-player");
            return;
        }

        if (args.length < 3) {
            plugin.getConfigManager().send(player, "edit-usage", "%label%", label);
            return;
        }

        String category = args[1].toLowerCase();
        String name = args[2].toLowerCase();

        if (category.equals("spawner")) {
            SpawnerTemplate template = plugin.getSpawnerTemplateManager().getTemplate(name);
            if (template == null) {
                plugin.getConfigManager().send(player, "template-not-found", "%name%", name);
                plugin.getConfigManager().playSound(player, "error");
                return;
            }

            clre20.customLootX.model.DraftSession draft = plugin.getDraftManager().findDraftByOriginalName(clre20.customLootX.model.DraftType.SPAWNER, name);
            SpawnerWizardContext context;
            if (draft != null && draft.getTemplateData() instanceof SpawnerTemplate dt) {
                context = new SpawnerWizardContext(plugin, player, dt, null, true, name, draft.getDraftId(), true);
                plugin.getConfigManager().send(player, "draft-loaded");
            } else {
                context = new SpawnerWizardContext(plugin, player, template.cloneTemplate(), null, true, name, null, false);
            }
            new SpawnerWizardStep1Gui(context).open();
            plugin.getConfigManager().playSound(player, "click");
            plugin.getConfigManager().log("cmd-edit-spawner", "%player%", player.getName(), "%label%", label, "%name%", name);
        } else if (category.equals("vault")) {
            VaultTemplate template = plugin.getVaultTemplateManager().getTemplate(name);
            if (template == null) {
                plugin.getConfigManager().send(player, "template-not-found", "%name%", name);
                plugin.getConfigManager().playSound(player, "error");
                return;
            }

            clre20.customLootX.model.DraftSession draft = plugin.getDraftManager().findDraftByOriginalName(clre20.customLootX.model.DraftType.VAULT, name);
            VaultWizardContext context;
            if (draft != null && draft.getTemplateData() instanceof VaultTemplate vt) {
                context = new VaultWizardContext(plugin, player, vt, null, true, name, draft.getDraftId(), true);
                plugin.getConfigManager().send(player, "draft-loaded");
            } else {
                context = new VaultWizardContext(plugin, player, template.cloneTemplate(), null, true, name, null, false);
            }
            new VaultWizardStep1Gui(context).open();
            plugin.getConfigManager().playSound(player, "click");
            plugin.getConfigManager().log("cmd-edit-vault", "%player%", player.getName(), "%label%", label, "%name%", name);
        } else if (category.equals("suspicious")) {
            LootTemplate template = plugin.getTemplateManager().getTemplate(name);
            if (template == null) {
                plugin.getConfigManager().send(player, "template-not-found", "%name%", name);
                plugin.getConfigManager().playSound(player, "error");
                return;
            }

            clre20.customLootX.model.DraftSession draft = plugin.getDraftManager().findDraftByOriginalName(clre20.customLootX.model.DraftType.SUSPICIOUS, name);
            WizardContext context;
            if (draft != null && draft.getTemplateData() instanceof LootTemplate lt) {
                context = new WizardContext(plugin, player, lt, false, null, draft.getDraftId(), true);
                plugin.getConfigManager().send(player, "draft-loaded");
            } else {
                context = new WizardContext(plugin, player, template.cloneTemplate(), false, null, null, false);
            }
            context.openStep1();
            plugin.getConfigManager().playSound(player, "click");
            plugin.getConfigManager().log("cmd-edit", "%player%", player.getName(), "%label%", label, "%name%", name);
        } else {
            plugin.getConfigManager().send(player, "edit-usage", "%label%", label);
        }
    }

    /**
     * /clx list [suspicious|vault|spawner]
     */
    private void handleList(CommandSender sender, String label, String[] args) {
        String filter = (args.length >= 2) ? args[1].toLowerCase() : "all";

        sender.sendMessage(plugin.getConfigManager().getComponent("commands.list.header", "&8========== &6CustomLootX 已儲存配置清單 &8=========="));

        boolean showSuspicious = filter.equals("all") || filter.equals("suspicious");
        boolean showVault = filter.equals("all") || filter.equals("vault");
        boolean showSpawner = filter.equals("all") || filter.equals("spawner");

        if (showSuspicious) {
            sender.sendMessage(TextUtil.parse("&e【可疑方塊配置】:"));
            if (plugin.getTemplateManager().getAllTemplates().isEmpty()) {
                sender.sendMessage(TextUtil.parse("&7(暫無可疑方塊配置)"));
            } else {
                for (LootTemplate t : plugin.getTemplateManager().getAllTemplates()) {
                    String typeStr = (t.getType() == Material.SUSPICIOUS_SAND) ? "&e沙" : "&7礫";
                    String resetStr = t.isResetEnabled() ? ("&b[重置: " + t.getResetMinutes() + "m]") : "&8[無重置]";
                    sender.sendMessage(TextUtil.parse(String.format("&f- &e%s &7(%s&7) &a[100.00%%] %s &7- 包含 &f%d &7件物品",
                            t.getName(), typeStr, resetStr, t.getItems().size())));
                }
            }
        }

        if (showVault) {
            sender.sendMessage(TextUtil.parse("&6【試煉寶庫配置】:"));
            if (plugin.getVaultTemplateManager().getAllTemplates().isEmpty()) {
                sender.sendMessage(TextUtil.parse("&7(暫無試煉寶庫配置)"));
            } else {
                for (VaultTemplate vt : plugin.getVaultTemplateManager().getAllTemplates()) {
                    String typeStr = vt.isOminous() ? "&5不祥寶庫" : "&6普通寶庫";
                    String modeStr = "&d[" + vt.getCooldownMode().getDisplay() + "]";
                    sender.sendMessage(TextUtil.parse(String.format("&f- &e%s &7(%s&7) &a[100.00%%] %s &7- 出貨 &a%d &7件 - 包含 &f%d &7項掉落",
                            vt.getName(), typeStr, modeStr, vt.getRollCount(), vt.getItems().size())));
                }
            }
        }

        if (showSpawner) {
            sender.sendMessage(TextUtil.parse("&5【試煉生怪磚配置】:"));
            if (plugin.getSpawnerTemplateManager().getAllTemplates().isEmpty()) {
                sender.sendMessage(TextUtil.parse("&7(暫無試煉生怪磚配置)"));
            } else {
                for (SpawnerTemplate st : plugin.getSpawnerTemplateManager().getAllTemplates()) {
                    String typeStr = st.isOminous() ? "&5不祥生怪磚" : "&6普通生怪磚";
                    String mobStr = (st.getMobPool().size() > 1)
                            ? ("&c" + st.getMobPool().size() + " 種怪物 (機率)")
                            : ("&c" + (st.getMobPool().isEmpty() ? TextUtil.getMobDisplayName(st.getSpawnedType()) : st.getMobPool().get(0).getDisplayName()));
                    String modeStr = "&d[" + st.getCooldownMode().getDisplay() + "]";
                    sender.sendMessage(TextUtil.parse(String.format("&f- &e%s &7(%s&7) 生物: %s &a[100.00%%] %s &7- 目標: &f%d &7隻 - 出貨 &a%d &7件 - 掉落池 &f%d &7項",
                            st.getName(), typeStr, mobStr, modeStr, st.getTotalMobs(), st.getRollCount(), st.getRewards().size())));
                }
            }
        }

        sender.sendMessage(plugin.getConfigManager().getComponent("commands.list.footer", "&8=================================================="));
    }

    /**
     * /clx delete <suspicious|vault|spawner> <名稱>
     */
    private void handleDelete(CommandSender sender, String label, String[] args) {
        if (args.length < 3) {
            plugin.getConfigManager().send(sender, "delete-usage", "%label%", label);
            return;
        }

        String category = args[1].toLowerCase();
        String name = args[2].toLowerCase();

        if (category.equals("spawner")) {
            if (plugin.getSpawnerTemplateManager().deleteTemplate(name)) {
                plugin.getConfigManager().send(sender, "template-deleted", "%name%", name);
                plugin.getConfigManager().log("cmd-delete-spawner", "%sender%", sender.getName(), "%label%", label, "%name%", name);
            } else {
                plugin.getConfigManager().send(sender, "template-not-found", "%name%", name);
            }
        } else if (category.equals("vault")) {
            if (plugin.getVaultTemplateManager().deleteTemplate(name)) {
                plugin.getConfigManager().send(sender, "template-deleted", "%name%", name);
                plugin.getConfigManager().log("cmd-delete-vault", "%sender%", sender.getName(), "%label%", label, "%name%", name);
            } else {
                plugin.getConfigManager().send(sender, "template-not-found", "%name%", name);
            }
        } else if (category.equals("suspicious")) {
            if (plugin.getTemplateManager().deleteTemplate(name)) {
                plugin.getConfigManager().send(sender, "template-deleted", "%name%", name);
                plugin.getConfigManager().log("cmd-delete", "%sender%", sender.getName(), "%label%", label, "%name%", name);
            } else {
                plugin.getConfigManager().send(sender, "template-not-found", "%name%", name);
            }
        } else {
            plugin.getConfigManager().send(sender, "delete-usage", "%label%", label);
        }
    }

    private void handleReload(CommandSender sender, String label) {
        plugin.getConfigManager().load();
        plugin.getTemplateManager().loadAll();
        plugin.getVaultTemplateManager().loadAll();
        plugin.getSpawnerTemplateManager().loadAll();
        plugin.getConfigManager().send(sender, "reload-success");
        plugin.getConfigManager().log("cmd-reload", "%sender%", sender.getName(), "%label%", label);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        boolean isAdmin = PermissionUtil.hasAdminPermission(sender);

        if (args.length == 1) {
            if (isAdmin) {
                List<String> subs = Arrays.asList("help", "create", "give", "key", "edit", "list", "delete", "reload");
                return filter(subs, args[0]);
            } else {
                return filter(Collections.singletonList("help"), args[0]);
            }
        }

        if (!isAdmin) {
            return Collections.emptyList();
        }

        // 第 2 個參數：填入 [suspicious, vault, spawner] 或 key 的寶庫名稱
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("key")) {
                return filter(new ArrayList<>(plugin.getVaultTemplateManager().getTemplateNames()), args[1]);
            }
            if (sub.equals("create") || sub.equals("give") || sub.equals("edit") || sub.equals("delete") || sub.equals("list")) {
                return filter(Arrays.asList("suspicious", "vault", "spawner"), args[1]);
            }
        }

        // 第 3 個參數：依據第 2 個參數補全配置名稱，或 key 指令補全玩家/數量
        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            String category = args[1].toLowerCase();

            if (sub.equals("create")) {
                if (category.equals("spawner") || category.equals("vault")) {
                    return filter(Arrays.asList("normal", "ominous"), args[2]);
                }
            }

            if (sub.equals("key")) {
                List<String> suggestions = new ArrayList<>();
                suggestions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
                suggestions.addAll(Arrays.asList("1", "16", "64"));
                return filter(suggestions, args[2]);
            }

            if (sub.equals("give") || sub.equals("edit") || sub.equals("delete")) {
                if (category.equals("vault")) {
                    return filter(new ArrayList<>(plugin.getVaultTemplateManager().getTemplateNames()), args[2]);
                } else if (category.equals("spawner")) {
                    return filter(new ArrayList<>(plugin.getSpawnerTemplateManager().getTemplateNames()), args[2]);
                } else if (category.equals("suspicious")) {
                    return filter(new ArrayList<>(plugin.getTemplateManager().getTemplateNames()), args[2]);
                } else {
                    List<String> all = new ArrayList<>();
                    all.addAll(plugin.getTemplateManager().getTemplateNames());
                    all.addAll(plugin.getVaultTemplateManager().getTemplateNames());
                    all.addAll(plugin.getSpawnerTemplateManager().getTemplateNames());
                    return filter(all, args[2]);
                }
            }
        }

        // 第 4 個參數 (針對 give 或 key 指令)：補全玩家名稱或預設數量
        if (args.length == 4) {
            String sub = args[0].toLowerCase();
            if (sub.equals("give")) {
                List<String> suggestions = new ArrayList<>();
                suggestions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
                suggestions.addAll(Arrays.asList("1", "16", "64"));
                return filter(suggestions, args[3]);
            } else if (sub.equals("key")) {
                return filter(Arrays.asList("1", "16", "64"), args[3]);
            }
        }

        // 第 5 個參數 (針對 give 指令)：補全數量
        if (args.length == 5 && args[0].equalsIgnoreCase("give")) {
            return Arrays.asList("1", "16", "64");
        }

        return Collections.emptyList();
    }

    private List<String> filter(List<String> list, String prefix) {
        String lower = prefix.toLowerCase();
        return list.stream().filter(s -> s.toLowerCase().startsWith(lower)).collect(Collectors.toList());
    }
}
