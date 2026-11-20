package clre20.customLootX.gui.wizard;

import clre20.customLootX.CustomLootX;
import clre20.customLootX.util.TemplateFolders;
import org.bukkit.entity.Player;

import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * 步驟精靈的名稱輸入：支援「名稱」、「資料夾/名稱」與「/名稱」(放回根目錄)
 */
public final class NameInputHelper {

    public static final Pattern NAME_BASIC = Pattern.compile("^[a-zA-Z0-9_]{1,64}$");
    public static final Pattern NAME_WITH_DASH = Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");

    private NameInputHelper() {
    }

    /**
     * 開啟聊天輸入前，提示資料夾寫法
     */
    public static void sendFolderHint(CustomLootX plugin, Player player, String currentFolder) {
        String folder = (currentFolder == null || currentFolder.isEmpty())
                ? plugin.getConfigManager().getText("commands.list.root", "根目錄")
                : currentFolder + "/";
        plugin.getConfigManager().send(player, "name-folder-hint", "%folder%", folder);
    }

    /**
     * 驗證輸入；不合法時已向玩家送出錯誤訊息並回傳 null
     *
     * @param usedByOther 名稱是否已被其他配置使用
     */
    public static TemplateFolders.NameInput validate(CustomLootX plugin, Player player, String input,
                                                     Pattern namePattern, Predicate<String> usedByOther) {
        TemplateFolders.NameInput parsed = TemplateFolders.parseNameInput(input, namePattern);
        if (parsed == null) {
            boolean folderProblem = input != null && input.contains("/")
                    && namePattern.matcher(input.substring(input.lastIndexOf('/') + 1).trim()).matches();
            plugin.getConfigManager().send(player, folderProblem ? "folder-invalid" : "name-invalid");
            plugin.getConfigManager().playSound(player, "error");
            return null;
        }
        if (usedByOther.test(parsed.name())) {
            plugin.getConfigManager().send(player, "name-already-used");
            plugin.getConfigManager().playSound(player, "error");
            return null;
        }
        return parsed;
    }

    /**
     * 名稱按鈕上顯示的「資料夾/名稱」
     */
    public static String displayPath(String folder, String name) {
        if (folder == null || folder.isEmpty()) return name;
        return "&7" + folder + "/&a" + name;
    }
}
