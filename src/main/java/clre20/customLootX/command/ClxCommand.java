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
import clre20.customLootX.util.TemplateFolders;
import clre20.customLootX.util.TemplateIndex;
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
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ClxCommand implements CommandExecutor, TabCompleter {

    private final CustomLootX plugin;

    public ClxCommand(CustomLootX plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // 所有指令只能由 OP 等級 2 以上（含）或具備 customlootx.admin 權限的管理者執行
        if (!PermissionUtil.hasAdminPermission(sender)) {
            plugin.getConfigManager().send(sender, "no-permission");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender, label);
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
            case "checklimit" -> handleCheckLimit(sender, label, args);
            case "resetlimit" -> handleResetLimit(sender, label, args);
            default -> sendHelp(sender, label);
        }

        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(plugin.getConfigManager().getComponent("commands.help.header", "&8============ &6CustomLootX 指令清單 &8============"));
        sender.sendMessage(plugin.getConfigManager().getComponent("commands.help.create", "&e/%label% create <suspicious|vault|spawner> [ominous|normal] &7- 創建全新空白可疑方塊、試煉寶庫或生怪磚", "%label%", label));
        sender.sendMessage(plugin.getConfigManager().getComponent("commands.help.give", "&e/%label% give <suspicious|vault|spawner> [資料夾] <名稱> [給誰] [數量] &7- 取得或給予已設定的方塊物品", "%label%", label));
        sender.sendMessage(plugin.getConfigManager().getComponent("commands.help.key", "&e/%label% key [資料夾] <寶庫名稱> [給誰] [數量] &7- 發放指定寶庫的專屬防偽鑰匙", "%label%", label));
        sender.sendMessage(plugin.getConfigManager().getComponent("commands.help.edit", "&e/%label% edit <suspicious|vault|spawner> [資料夾] <名稱> &7- 直接開啟指定配置的步驟引導介面", "%label%", label));
        sender.sendMessage(plugin.getConfigManager().getComponent("commands.help.list", "&e/%label% list [suspicious|vault|spawner] [資料夾] &7- 依資料夾列出已儲存的配置", "%label%", label));
        sender.sendMessage(plugin.getConfigManager().getComponent("commands.help.delete", "&e/%label% delete <suspicious|vault|spawner> [資料夾] <名稱> &7- 刪除指定的配置檔案", "%label%", label));
        sender.sendMessage(plugin.getConfigManager().getComponent("commands.help.reload", "&e/%label% reload &7- 重新讀取所有設定與資料檔", "%label%", label));
        sender.sendMessage(plugin.getConfigManager().getComponent("commands.help.checklimit", "&e/%label% checklimit [玩家|all] &7- 查詢今日物品已出貨累計次數", "%label%", label));
        sender.sendMessage(plugin.getConfigManager().getComponent("commands.help.resetlimit", "&e/%label% resetlimit <玩家|all> [daily|total|all] [物品] &7- 重設每日或終生出貨上限紀錄", "%label%", label));
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

    // ==========================================
    // 資料夾分類：配置名稱解析
    // ==========================================

    /** 單次給予數量上限 (36 格 x 64)，避免誤填超大數量造成卡頓 */
    private static final int MAX_GIVE_AMOUNT = 2304;

    private record Resolved(String name, int consumed) {
    }

    private TemplateIndex indexFor(String category) {
        return switch (category == null ? "" : category.toLowerCase()) {
            case "suspicious" -> plugin.getTemplateManager().getIndex();
            case "vault" -> plugin.getVaultTemplateManager().getIndex();
            case "spawner" -> plugin.getSpawnerTemplateManager().getIndex();
            default -> null;
        };
    }

    /**
     * 從 args[i] 開始解析配置名稱：
     * 1. args[i] 是資料夾且 args[i+1] 是該資料夾內的配置 -> 使用兩個參數 (資料夾 名稱)
     * 2. args[i] 是配置名稱 -> 使用一個參數 (配置名稱全域唯一，在資料夾內的配置也能只打名稱)
     * 3. args[i] 寫成「資料夾/名稱」-> 使用一個參數
     */
    private Resolved resolve(TemplateIndex idx, String[] args, int i) {
        if (idx == null || i >= args.length) return null;
        String a = args[i];
        if (a.length() > 200) return null;
        if (i + 1 < args.length && idx.isFolder(a)) {
            String folder = idx.canonicalFolder(a);
            String n = args[i + 1].toLowerCase();
            String actual = idx.folderOf(n);
            if (folder != null && actual != null && actual.equalsIgnoreCase(folder)) {
                return new Resolved(n, 2);
            }
        }
        String n = a.toLowerCase();
        if (idx.contains(n)) {
            return new Resolved(n, 1);
        }
        int slash = a.lastIndexOf('/');
        if (slash > 0 && slash < a.length() - 1) {
            String folder = TemplateFolders.normalize(a.substring(0, slash));
            String name = a.substring(slash + 1).toLowerCase();
            String actual = idx.folderOf(name);
            if (folder != null && actual != null && actual.equalsIgnoreCase(folder)) {
                return new Resolved(name, 1);
            }
        }
        return null;
    }

    /** 找不到配置時顯示使用者輸入的內容 (資料夾 名稱 -> 資料夾/名稱) */
    private String describeInput(TemplateIndex idx, String[] args, int i) {
        if (i >= args.length) return "";
        if (idx != null && i + 1 < args.length && idx.isFolder(args[i])) {
            return TemplateFolders.join(idx.canonicalFolder(args[i]), args[i + 1]);
        }
        return args[i];
    }

    private static int parseAmount(String s) {
        int v = Integer.parseInt(s);
        return Math.max(1, Math.min(MAX_GIVE_AMOUNT, v));
    }

    /**
     * 不連線 Mojang 的離線玩家查詢：線上玩家 -> 伺服器快取；都沒有則回傳 null
     */
    private org.bukkit.OfflinePlayer findPlayerNoLookup(String name) {
        if (name == null || name.isEmpty() || name.length() > 16) return null;
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        return Bukkit.getOfflinePlayerIfCached(name);
    }

    /**
     * 解析 [給誰] [數量]：回傳 null 代表已送出錯誤訊息
     */
    private Object[] parseTargetAndAmount(CommandSender sender, String label, String[] args, int i, String consoleUsageKey) {
        Player target;
        int amount = 1;
        if (i >= args.length) {
            if (sender instanceof Player p) {
                target = p;
            } else {
                plugin.getConfigManager().send(sender, consoleUsageKey, "%label%", label);
                return null;
            }
        } else {
            Player maybePlayer = Bukkit.getPlayerExact(args[i]);
            if (maybePlayer != null) {
                target = maybePlayer;
                if (i + 1 < args.length) {
                    try {
                        amount = parseAmount(args[i + 1]);
                    } catch (NumberFormatException ignored) {
                    }
                }
            } else if (sender instanceof Player p) {
                target = p;
                try {
                    amount = parseAmount(args[i]);
                } catch (NumberFormatException e) {
                    plugin.getConfigManager().send(sender, "player-not-found", "%player%", args[i]);
                    return null;
                }
            } else {
                plugin.getConfigManager().send(sender, "player-not-found", "%player%", args[i]);
                return null;
            }
        }
        return new Object[]{target, amount};
    }

    /**
     * /clx give <suspicious|vault|spawner> [資料夾] <名稱> [給誰] [數量]
     */
    private void handleGive(CommandSender sender, String label, String[] args) {
        if (args.length < 3) {
            plugin.getConfigManager().send(sender, "give-usage", "%label%", label);
            return;
        }

        String category = args[1].toLowerCase();
        TemplateIndex idx = indexFor(category);
        int nameIndex = 2;
        if (idx == null) {
            // 容錯機制：未輸入分類時，依序嘗試比對各類型的配置名稱
            for (String c : new String[]{"suspicious", "vault", "spawner"}) {
                if (resolve(indexFor(c), args, 1) != null) {
                    category = c;
                    idx = indexFor(c);
                    nameIndex = 1;
                    break;
                }
            }
            if (idx == null) {
                plugin.getConfigManager().send(sender, "give-usage", "%label%", label);
                return;
            }
        }

        Resolved r = resolve(idx, args, nameIndex);
        if (r == null) {
            plugin.getConfigManager().send(sender, "template-not-found", "%name%", describeInput(idx, args, nameIndex));
            return;
        }
        String name = r.name();
        Object[] ta = parseTargetAndAmount(sender, label, args, nameIndex + r.consumed(), "give-console-target-required");
        if (ta == null) return;
        Player target = (Player) ta[0];
        int amount = (Integer) ta[1];

        ItemStack item;
        switch (category) {
            case "spawner" -> {
                SpawnerTemplate template = plugin.getSpawnerTemplateManager().getTemplate(name);
                if (template == null) {
                    plugin.getConfigManager().send(sender, "template-not-found", "%name%", name);
                    return;
                }
                item = plugin.getItemManager().createTemplateSpawnerItem(template, amount);
            }
            case "vault" -> {
                VaultTemplate template = plugin.getVaultTemplateManager().getTemplate(name);
                if (template == null) {
                    plugin.getConfigManager().send(sender, "template-not-found", "%name%", name);
                    return;
                }
                item = plugin.getItemManager().createTemplateVaultItem(template, amount);
            }
            default -> {
                LootTemplate template = plugin.getTemplateManager().getTemplate(name);
                if (template == null) {
                    plugin.getConfigManager().send(sender, "template-not-found", "%name%", name);
                    return;
                }
                item = plugin.getItemManager().createTemplateItem(template, amount);
            }
        }

        target.getInventory().addItem(item);
        String path = idx.pathOf(name);

        plugin.getConfigManager().send(sender, "give-success", "%player%", target.getName(), "%amount%", amount, "%name%", path);
        if (!sender.equals(target)) {
            plugin.getConfigManager().send(target, "receive-success", "%amount%", amount, "%name%", path);
        }
        if (sender.equals(target)) {
            plugin.getConfigManager().send(sender, "give-self-tip");
        }

        plugin.getConfigManager().log("cmd-give", "%sender%", sender.getName(), "%label%", label, "%target%", target.getName(), "%name%", path, "%amount%", amount);
    }

    /**
     * /clx key [資料夾] <寶庫名稱> [給誰] [數量]
     */
    private void handleKey(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            plugin.getConfigManager().send(sender, "cmd-key-usage", "%label%", label);
            return;
        }

        TemplateIndex idx = plugin.getVaultTemplateManager().getIndex();
        Resolved r = resolve(idx, args, 1);
        clre20.customLootX.model.VaultTemplate template = r == null ? null : plugin.getVaultTemplateManager().getTemplate(r.name());
        if (template == null) {
            plugin.getConfigManager().send(sender, "template-not-found", "%name%", describeInput(idx, args, 1));
            if (sender instanceof Player p) {
                plugin.getConfigManager().playSound(p, "error");
            }
            return;
        }
        String name = idx.pathOf(r.name());

        Object[] ta = parseTargetAndAmount(sender, label, args, 1 + r.consumed(), "give-console-target-required");
        if (ta == null) return;
        Player target = (Player) ta[0];
        int amount = (Integer) ta[1];

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
        TemplateIndex editIdx = indexFor(category);
        Resolved editResolved = resolve(editIdx, args, 2);
        // 找不到時保留原輸入，交由下方各類型分支顯示「找不到配置」
        String name = editResolved != null ? editResolved.name() : describeInput(editIdx, args, 2).toLowerCase();

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
     * /clx list [suspicious|vault|spawner] [資料夾]
     * <p>
     * 不指定類型：顯示各類型的數量與第一層資料夾 (配置數量多時不洗版)
     * 指定類型：顯示該層的子資料夾 (含數量) 與直接放在該層的配置
     */
    private void handleList(CommandSender sender, String label, String[] args) {
        String filter = (args.length >= 2) ? args[1].toLowerCase() : "all";
        TemplateIndex idx = indexFor(filter);
        if (idx == null && !filter.equals("all")) {
            sender.sendMessage(TextUtil.parse(plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().getText("commands.list.usage", "&c用法: /%label% list [suspicious|vault|spawner] [資料夾]", "%label%", label)));
            return;
        }

        sender.sendMessage(plugin.getConfigManager().getComponent("commands.list.header", "&8========== &6CustomLootX 已儲存配置清單 &8=========="));

        if (idx == null) {
            for (String c : new String[]{"suspicious", "vault", "spawner"}) {
                TemplateIndex ti = indexFor(c);
                sender.sendMessage(TextUtil.parse(plugin.getConfigManager().getText("commands.list.type-summary",
                        "%type% &7共 &f%count% &7個配置、&f%folders% &7個資料夾 &8(/%label% list %key% [資料夾])",
                        "%type%", typeTitle(c), "%count%", ti.size(), "%folders%", ti.folders().size(), "%label%", label, "%key%", c)));
                for (String f : ti.childFolders("")) {
                    sender.sendMessage(TextUtil.parse(plugin.getConfigManager().getText("commands.list.folder-line",
                            " &6📁 %folder%/ &7(%count% 個)", "%folder%", f, "%count%", ti.countUnder(f))));
                }
            }
            sender.sendMessage(plugin.getConfigManager().getComponent("commands.list.footer", "&8=================================================="));
            return;
        }

        String folder = "";
        if (args.length >= 3) {
            folder = idx.canonicalFolder(args[2]);
            if (folder == null || (!folder.isEmpty() && !idx.isFolder(folder))) {
                sender.sendMessage(TextUtil.parse(plugin.getConfigManager().getPrefix()
                        + plugin.getConfigManager().getText("commands.list.folder-not-found", "&c找不到資料夾: &e%folder%", "%folder%", args[2])));
                return;
            }
        }

        String location = folder.isEmpty()
                ? plugin.getConfigManager().getText("commands.list.root", "根目錄")
                : folder + "/";
        sender.sendMessage(TextUtil.parse(plugin.getConfigManager().getText("commands.list.type-header",
                "%type% &7- &f%location%", "%type%", typeTitle(filter), "%location%", location)));

        List<String> subFolders = idx.childFolders(folder);
        List<String> names = idx.namesIn(folder);
        if (subFolders.isEmpty() && names.isEmpty()) {
            sender.sendMessage(TextUtil.parse(plugin.getConfigManager().getText("commands.list.empty-folder", "&7(此處沒有任何配置)")));
        }
        for (String f : subFolders) {
            sender.sendMessage(TextUtil.parse(plugin.getConfigManager().getText("commands.list.folder-line",
                    " &6📁 %folder%/ &7(%count% 個)", "%folder%", f, "%count%", idx.countUnder(f))));
        }
        for (String n : names) {
            String line = describeTemplate(filter, n);
            if (line != null) sender.sendMessage(TextUtil.parse(line));
        }

        sender.sendMessage(plugin.getConfigManager().getComponent("commands.list.footer", "&8=================================================="));
    }

    private String typeTitle(String category) {
        return switch (category) {
            case "suspicious" -> plugin.getConfigManager().getText("commands.list.title-suspicious", "&e【可疑方塊配置】");
            case "vault" -> plugin.getConfigManager().getText("commands.list.title-vault", "&6【試煉寶庫配置】");
            default -> plugin.getConfigManager().getText("commands.list.title-spawner", "&5【試煉生怪磚配置】");
        };
    }

    private String describeTemplate(String category, String name) {
        switch (category) {
            case "suspicious" -> {
                LootTemplate t = plugin.getTemplateManager().getTemplate(name);
                if (t == null) return null;
                String typeStr = (t.getType() == Material.SUSPICIOUS_SAND) ? "&e沙" : "&7礫";
                String resetStr = t.isResetEnabled() ? ("&b[重置: " + TextUtil.formatTimeSeconds(t.getResetSeconds()) + "]") : "&8[無重置]";
                return String.format("&f- &e%s &7(%s&7) &a[100.00%%] %s &7- 包含 &f%d &7件物品",
                        t.getName(), typeStr, resetStr, t.getItems().size());
            }
            case "vault" -> {
                VaultTemplate vt = plugin.getVaultTemplateManager().getTemplate(name);
                if (vt == null) return null;
                String typeStr = vt.isOminous() ? "&5不祥寶庫" : "&6普通寶庫";
                String timeStr = (vt.getCooldownMode() == clre20.customLootX.model.VaultCooldownMode.ONCE_PER_PLAYER)
                        ? ""
                        : (": " + TextUtil.formatTimeSeconds(vt.getCooldownSeconds()));
                String modeStr = "&d[" + vt.getCooldownMode().getDisplay() + timeStr + "]";
                return String.format("&f- &e%s &7(%s&7) &a[100.00%%] %s &7- 出貨 &a%d &7件 - 包含 &f%d &7項掉落",
                        vt.getName(), typeStr, modeStr, vt.getRollCount(), vt.getItems().size());
            }
            default -> {
                SpawnerTemplate st = plugin.getSpawnerTemplateManager().getTemplate(name);
                if (st == null) return null;
                String typeStr = st.isOminous() ? "&5不祥生怪磚" : "&6普通生怪磚";
                String mobStr = (st.getMobPool().size() > 1)
                        ? ("&c" + st.getMobPool().size() + " 種怪物 (機率)")
                        : ("&c" + (st.getMobPool().isEmpty() ? TextUtil.getMobDisplayName(st.getSpawnedType()) : st.getMobPool().get(0).getDisplayName()));
                String timeStr = (st.getCooldownMode() == clre20.customLootX.model.VaultCooldownMode.ONCE_PER_PLAYER)
                        ? ""
                        : (": " + TextUtil.formatTimeSeconds(st.getCooldownSeconds()));
                String modeStr = "&d[" + st.getCooldownMode().getDisplay() + timeStr + "]";
                return String.format("&f- &e%s &7(%s&7) 生物: %s &a[100.00%%] %s &7- 目標: &f%d &7隻 - 出貨 &a%d &7件 - 掉落池 &f%d &7項",
                        st.getName(), typeStr, mobStr, modeStr, st.getTotalMobs(), st.getRollCount(), st.getRewards().size());
            }
        }
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
        TemplateIndex delIdx = indexFor(category);
        if (delIdx == null) {
            plugin.getConfigManager().send(sender, "delete-usage", "%label%", label);
            return;
        }
        Resolved delResolved = resolve(delIdx, args, 2);
        if (delResolved == null) {
            plugin.getConfigManager().send(sender, "template-not-found", "%name%", describeInput(delIdx, args, 2));
            return;
        }
        // 訊息顯示「資料夾/名稱」；實際刪除仍用名稱
        sendDelete(sender, label, category, delResolved.name(), delIdx.pathOf(delResolved.name()));
    }

    private void sendDelete(CommandSender sender, String label, String category, String name, String path) {
        boolean ok = switch (category) {
            case "spawner" -> plugin.getSpawnerTemplateManager().deleteTemplate(name);
            case "vault" -> plugin.getVaultTemplateManager().deleteTemplate(name);
            default -> plugin.getTemplateManager().deleteTemplate(name);
        };
        if (ok) {
            plugin.getConfigManager().send(sender, "template-deleted", "%name%", path);
            String logKey = switch (category) {
                case "spawner" -> "cmd-delete-spawner";
                case "vault" -> "cmd-delete-vault";
                default -> "cmd-delete";
            };
            plugin.getConfigManager().log(logKey, "%sender%", sender.getName(), "%label%", label, "%name%", path);
        } else {
            plugin.getConfigManager().send(sender, "template-not-found", "%name%", path);
        }
    }

    private final java.util.concurrent.atomic.AtomicBoolean reloading = new java.util.concurrent.atomic.AtomicBoolean(false);

    /**
     * 重載：設定檔在主執行緒讀取；數百個配置檔改在背景平行讀取，讀完再回主執行緒一次替換，不會造成 TPS 尖峰
     */
    private void handleReload(CommandSender sender, String label) {
        if (!reloading.compareAndSet(false, true)) {
            plugin.getConfigManager().send(sender, "reload-in-progress");
            return;
        }
        plugin.getConfigManager().load();
        // 先把記憶體中尚未落盤的計數寫入 (背景存檔有最多 30 秒延遲)，再重新讀取，避免重載時遺失最新出貨紀錄
        plugin.getLootLimitManager().save(true);
        plugin.getLootLimitManager().load();
        plugin.getConfigManager().send(sender, "reload-started");
        long start = System.currentTimeMillis();
        final int modSus = plugin.getTemplateManager().getModCount();
        final int modVault = plugin.getVaultTemplateManager().getModCount();
        final int modSpawner = plugin.getSpawnerTemplateManager().getModCount();
        try {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                java.util.List<clre20.customLootX.util.ParallelYamlLoader.Loaded> suspicious = null;
                java.util.List<clre20.customLootX.util.ParallelYamlLoader.Loaded> vault = null;
                java.util.List<clre20.customLootX.util.ParallelYamlLoader.Loaded> spawner = null;
                Throwable error = null;
                long readStart = System.nanoTime();
                try {
                    suspicious = plugin.getTemplateManager().readAll();
                    vault = plugin.getVaultTemplateManager().readAll();
                    spawner = plugin.getSpawnerTemplateManager().readAll();
                } catch (Throwable t) {
                    error = t;
                }
                final var fSus = suspicious;
                final var fVault = vault;
                final var fSpawner = spawner;
                final Throwable fError = error;
                final long readMs = (System.nanoTime() - readStart) / 1_000_000L;
                if (!plugin.isEnabled()) {
                    reloading.set(false);
                    return;
                }
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        if (fError != null) {
                            plugin.logError("&c[重載]&c 讀取配置檔失敗: " + fError);
                            plugin.getConfigManager().send(sender, "save-failed");
                            return;
                        }
                        long applyStart = System.nanoTime();
                        // 背景讀取期間若有人透過精靈儲存或刪除配置，該類型改為同步重讀，確保不遺失剛存的內容
                        if (plugin.getTemplateManager().getModCount() == modSus) plugin.getTemplateManager().applyLoaded(fSus);
                        else plugin.getTemplateManager().loadAll();
                        if (plugin.getVaultTemplateManager().getModCount() == modVault) plugin.getVaultTemplateManager().applyLoaded(fVault);
                        else plugin.getVaultTemplateManager().loadAll();
                        if (plugin.getSpawnerTemplateManager().getModCount() == modSpawner) plugin.getSpawnerTemplateManager().applyLoaded(fSpawner);
                        else plugin.getSpawnerTemplateManager().loadAll();
                        long applyMs = (System.nanoTime() - applyStart) / 1_000_000L;
                        plugin.getConfigManager().send(sender, "reload-success",
                                "%ms%", System.currentTimeMillis() - start,
                                "%read_ms%", readMs,
                                "%apply_ms%", applyMs);
                        plugin.getConfigManager().log("cmd-reload", "%sender%", sender.getName(), "%label%", label);
                    } finally {
                        reloading.set(false);
                    }
                });
            });
        } catch (Throwable t) {
            reloading.set(false);
            throw t;
        }
    }

    private void handleResetLimit(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(TextUtil.parse(plugin.getConfigManager().getPrefix() + "&c用法: /" + label + " resetlimit <玩家|all> [daily|total|all] [物品關鍵字 (選填)]"));
            sender.sendMessage(TextUtil.parse("&7  &edaily&7: 重設每日出貨上限"));
            sender.sendMessage(TextUtil.parse("&7  &etotal&7: 重設終生/個人總出貨上限"));
            sender.sendMessage(TextUtil.parse("&7  &eall&7:   重設全部 (每日+終生) 上限 (預設)"));
            return;
        }

        String targetArg = args[1];
        String mode = "all";
        String itemKeyword = null;

        if (args.length >= 3) {
            String m = args[2].toLowerCase();
            if (m.equals("total") || m.equals("lifetime") || m.equals("終生") || m.equals("個人")) {
                mode = "total";
                if (args.length >= 4) itemKeyword = args[3];
            } else if (m.equals("daily") || m.equals("day") || m.equals("每日") || m.equals("日")) {
                mode = "daily";
                if (args.length >= 4) itemKeyword = args[3];
            } else if (m.equals("all") || m.equals("全部")) {
                mode = "all";
                if (args.length >= 4) itemKeyword = args[3];
            } else {
                // 如果 args[2] 不是模式名稱，視為物品關鍵字，模式為預設 all
                itemKeyword = args[2];
            }
        }

        String modeDisplay = switch (mode) {
            case "total" -> "【終生】";
            case "daily" -> "【每日】";
            default -> "【全部 (每日+終生)】";
        };

        String itemDisplay = (itemKeyword != null) ? ("包含關鍵字 [&e" + itemKeyword + "&a] 的") : "";

        if (targetArg.equalsIgnoreCase("all")) {
            plugin.getLootLimitManager().resetAllLimits(mode, itemKeyword);
            sender.sendMessage(TextUtil.parse(plugin.getConfigManager().getPrefix() + "&a已成功重設全伺服器所有玩家" + itemDisplay + modeDisplay + "出貨上限紀錄！"));
            plugin.getConfigManager().log("cmd-resetlimit-all", "%sender%", sender.getName(), "%mode%", mode, "%item%", (itemKeyword != null ? itemKeyword : "ALL"));
        } else {
            org.bukkit.OfflinePlayer target = findPlayerNoLookup(targetArg);
            if (target == null) {
                plugin.getConfigManager().send(sender, "player-not-found", "%player%", targetArg);
                return;
            }
            String targetName = target.getName() != null ? target.getName() : targetArg;
            plugin.getLootLimitManager().resetPlayerLimits(target.getUniqueId(), mode, itemKeyword);
            sender.sendMessage(TextUtil.parse(plugin.getConfigManager().getPrefix() + "&a已成功重設玩家 &e" + targetName + "&a " + itemDisplay + modeDisplay + "出貨上限紀錄！"));
            plugin.getConfigManager().log("cmd-resetlimit-player", "%sender%", sender.getName(), "%target%", targetName, "%mode%", mode, "%item%", (itemKeyword != null ? itemKeyword : "ALL"));
        }
    }

    private void handleCheckLimit(CommandSender sender, String label, String[] args) {
        if (!PermissionUtil.hasAdminPermission(sender)) {
            plugin.getConfigManager().send(sender, "no-permission");
            return;
        }

        // 管理員查詢 (可查自己、指定玩家或 all 全服所有人)
        if (args.length >= 2) {
            String targetArg = args[1];
            if (targetArg.equalsIgnoreCase("all")) {
                // 管理員查全服所有人
                showAllPlayersLimits(sender);
                return;
            }
            // 管理員查指定玩家
            org.bukkit.OfflinePlayer target = findPlayerNoLookup(targetArg);
            if (target == null) {
                plugin.getConfigManager().send(sender, "player-not-found", "%player%", targetArg);
                return;
            }
            String targetName = target.getName() != null ? target.getName() : targetArg;
            showPlayerLimit(sender, targetName, target.getUniqueId(), true);
        } else {
            // 管理員未輸入參數：若為玩家顯示自己並提示管理員進階指令，若為控制台則提示用法
            if (sender instanceof Player player) {
                showPlayerLimit(sender, player.getName(), player.getUniqueId(), false);
                sender.sendMessage(TextUtil.parse("&7(管理員提示: 可使用 &e/" + label + " checklimit <玩家|all>&7 查詢指定玩家或全體玩家)"));
            } else {
                sender.sendMessage(TextUtil.parse("&c後台控制台請指定參數: /" + label + " checklimit <玩家|all>"));
            }
        }
    }

    private void showPlayerLimit(CommandSender sender, String targetName, UUID targetUuid, boolean showAdminPrefix) {
        Map<String, Integer> counts = plugin.getLootLimitManager().getPlayerTodayCounts(targetUuid);
        Map<String, Integer> totalCounts = (plugin.getPlayerDataManager() != null)
                ? plugin.getPlayerDataManager().getPlayerData(targetUuid).getLootTotalCounts()
                : Collections.emptyMap();

        String headerTitle = showAdminPrefix ? ("&6玩家 &e" + targetName + " &6出貨統計") : "&6【我的出貨額度統計】";
        sender.sendMessage(TextUtil.parse("&8============ " + headerTitle + " &8============"));
        sender.sendMessage(TextUtil.parse("&e[今日出貨統計]"));
        if (counts.isEmpty()) {
            sender.sendMessage(TextUtil.parse(" &7今日尚無任何受限制物品的出貨紀錄。"));
        } else {
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                sender.sendMessage(TextUtil.parse(" &7- &f" + entry.getKey() + ": 今日已出貨 &e" + entry.getValue() + " &7次"));
            }
        }
        if (!totalCounts.isEmpty()) {
            sender.sendMessage(TextUtil.parse("&e[個人累積總出貨 (終身上限)]"));
            for (Map.Entry<String, Integer> entry : totalCounts.entrySet()) {
                sender.sendMessage(TextUtil.parse(" &7- &f" + entry.getKey() + ": 累計已出貨 &e" + entry.getValue() + " &7次"));
            }
        }
        sender.sendMessage(TextUtil.parse("&8=========================================================="));
    }

    private void showAllPlayersLimits(CommandSender sender) {
        Map<UUID, Map<String, Integer>> allData = plugin.getLootLimitManager().getAllPlayersTodayCounts();
        sender.sendMessage(TextUtil.parse("&8============ &6全伺服器今日所有玩家出貨統計 &8============"));
        if (allData.isEmpty()) {
            sender.sendMessage(TextUtil.parse("&7今日全伺服器尚無任何玩家出貨紀錄。"));
        } else {
            for (Map.Entry<UUID, Map<String, Integer>> entry : allData.entrySet()) {
                UUID u = entry.getKey();
                // 只查線上玩家與記憶體快取，不讀玩家檔、不連線
                String pName = plugin.getPlayerDataManager() != null
                        ? plugin.getPlayerDataManager().getPlayerName(u)
                        : u.toString();
                sender.sendMessage(TextUtil.parse("&e▶ 玩家 " + pName + ":"));
                for (Map.Entry<String, Integer> itemEntry : entry.getValue().entrySet()) {
                    sender.sendMessage(TextUtil.parse("   &7- &f" + itemEntry.getKey() + ": &a" + itemEntry.getValue() + " &7次"));
                }
            }
        }
        sender.sendMessage(TextUtil.parse("&8=========================================================="));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!PermissionUtil.hasAdminPermission(sender)) {
            return Collections.emptyList();
        }
        String last = args.length == 0 ? "" : args[args.length - 1];
        if (last.length() > 64) return Collections.emptyList();

        if (args.length == 1) {
            List<String> subs = Arrays.asList("help", "create", "give", "key", "edit", "list", "delete", "reload", "checklimit", "resetlimit");
            return filter(subs, args[0]);
        }

        String sub = args[0].toLowerCase();

        if (args.length == 2) {
            if (sub.equals("checklimit") || sub.equals("resetlimit")) {
                List<String> suggestions = new ArrayList<>();
                suggestions.add("all");
                suggestions.addAll(onlineNames());
                return filter(suggestions, args[1]);
            }
            if (sub.equals("key")) {
                return filter(folderAndRootNames(plugin.getVaultTemplateManager().getIndex()), args[1]);
            }
            if (sub.equals("create") || sub.equals("give") || sub.equals("edit") || sub.equals("delete") || sub.equals("list")) {
                return filter(Arrays.asList("suspicious", "vault", "spawner"), args[1]);
            }
            return Collections.emptyList();
        }

        if (sub.equals("resetlimit")) {
            if (args.length == 3) {
                return filter(Arrays.asList("all", "total", "daily"), args[2]);
            }
            if (args.length == 4) {
                UUID targetUuid = null;
                if (!args[1].equalsIgnoreCase("all")) {
                    org.bukkit.OfflinePlayer op = findPlayerNoLookup(args[1]);
                    if (op == null) return Collections.emptyList();
                    targetUuid = op.getUniqueId();
                }
                return filter(new ArrayList<>(plugin.getLootLimitManager().getKnownItemKeys(targetUuid)), args[3]);
            }
            return Collections.emptyList();
        }

        if (sub.equals("create")) {
            String category = args[1].toLowerCase();
            if (args.length == 3 && (category.equals("spawner") || category.equals("vault"))) {
                return filter(Arrays.asList("normal", "ominous"), args[2]);
            }
            return Collections.emptyList();
        }

        if (sub.equals("list")) {
            TemplateIndex idx = indexFor(args[1]);
            if (args.length == 3 && idx != null) {
                return filter(idx.folders(), args[2]);
            }
            return Collections.emptyList();
        }

        if (sub.equals("key")) {
            // /clx key [資料夾] <名稱> [給誰] [數量]
            return completeNameThenTarget(plugin.getVaultTemplateManager().getIndex(), args, 1, true);
        }

        if (sub.equals("give") || sub.equals("edit") || sub.equals("delete")) {
            TemplateIndex idx = indexFor(args[1]);
            if (idx == null) return Collections.emptyList();
            return completeNameThenTarget(idx, args, 2, sub.equals("give"));
        }

        return Collections.emptyList();
    }

    /**
     * 補全 [資料夾] <名稱> 之後接 [給誰] [數量]
     *
     * @param nameIndex 名稱 (或資料夾) 參數的位置
     */
    private List<String> completeNameThenTarget(TemplateIndex idx, String[] args, int nameIndex, boolean withTarget) {
        int pos = args.length - 1;
        String current = args[pos];
        if (pos == nameIndex) {
            return filter(folderAndRootNames(idx), current);
        }
        String first = args[nameIndex];
        boolean firstIsFolder = idx.isFolder(first);
        if (pos == nameIndex + 1) {
            List<String> suggestions = new ArrayList<>();
            if (firstIsFolder) {
                suggestions.addAll(idx.namesIn(idx.canonicalFolder(first)));
            }
            if (withTarget && (!firstIsFolder || idx.contains(first))) {
                suggestions.addAll(onlineNames());
                suggestions.addAll(Arrays.asList("1", "16", "64"));
            }
            return filter(suggestions, current);
        }
        if (!withTarget) return Collections.emptyList();
        Resolved r = resolve(idx, args, nameIndex);
        int targetIndex = nameIndex + (r != null ? r.consumed() : 1);
        if (pos == targetIndex) {
            List<String> suggestions = new ArrayList<>(onlineNames());
            suggestions.addAll(Arrays.asList("1", "16", "64"));
            return filter(suggestions, current);
        }
        if (pos == targetIndex + 1) {
            return filter(Arrays.asList("1", "16", "64"), current);
        }
        return Collections.emptyList();
    }

    private List<String> folderAndRootNames(TemplateIndex idx) {
        List<String> list = new ArrayList<>(idx.folders().size() + idx.namesIn("").size());
        list.addAll(idx.folders());
        list.addAll(idx.namesIn(""));
        return list;
    }

    private List<String> onlineNames() {
        List<String> names = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            names.add(p.getName());
        }
        return names;
    }

    private List<String> filter(List<String> list, String prefix) {
        String lower = prefix.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(lower)) {
                result.add(s);
                if (result.size() >= 200) break;
            }
        }
        return result;
    }
}
