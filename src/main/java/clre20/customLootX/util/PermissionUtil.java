package clre20.customLootX.util;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 權限與 OP 等級檢查工具
 */
public final class PermissionUtil {

    public static final String ADMIN_PERMISSION = "customlootx.admin";

    private PermissionUtil() {}

    /**
     * 檢查發送者是否具備 CustomLootX 管理權限。
     * 規則：
     * 1. 伺服器主控台 (Console) 或命令方塊等非玩家：一律擁有管理權限。
     * 2. 玩家若擁有明確指定的 customlootx.admin 權限（例如 LuckPerms 賦予）：擁有管理權限。
     * 3. 玩家若為 OP，必須具備 OP 等級 2 以上（含等級 2, 3, 4）才具備管理權限；OP 等級 1 視為普通玩家，無管理權限。
     *
     * @param sender 指令或動作發送者
     * @return 若具備管理權限則返回 true，否則返回 false
     */
    public static boolean hasAdminPermission(CommandSender sender) {
        if (sender == null) {
            return false;
        }

        // 1. 控制台、命令方塊等非玩家物件擁有最高管理權限
        if (!(sender instanceof Player player)) {
            return true;
        }

        // 2. 具備明確權限節點 (如透過權限外掛 LuckPerms 給予)
        if (player.hasPermission(ADMIN_PERMISSION)) {
            return true;
        }

        // 3. 非 OP 玩家直接拒絕
        if (!player.isOp()) {
            return false;
        }

        // 4. OP 玩家：檢查是否為 OP 等級 2 以上（含）
        return getOpLevel(player) >= 2;
    }

    /**
     * 取得玩家的 OP 等級。
     *
     * @param player 目標玩家
     * @return 0（非 OP）、1（OP 等級 1）、2（OP 等級 2）、3（OP 等級 3）、4（OP 等級 4）
     */
    public static int getOpLevel(Player player) {
        if (player == null || !player.isOp()) {
            return 0;
        }

        // 方式一：透過 Paper / NMS 反射呼叫 ServerPlayer.hasPermissions(int)
        try {
            Method getHandle = player.getClass().getMethod("getHandle");
            Object serverPlayer = getHandle.invoke(player);

            Method hasPerms = null;
            try {
                hasPerms = serverPlayer.getClass().getMethod("hasPermissions", int.class);
            } catch (NoSuchMethodException e) {
                try {
                    hasPerms = serverPlayer.getClass().getMethod("hasPermission", int.class);
                } catch (NoSuchMethodException ignored) {}
            }

            if (hasPerms != null) {
                for (int lvl = 4; lvl >= 1; lvl--) {
                    Object res = hasPerms.invoke(serverPlayer, lvl);
                    if (res instanceof Boolean b && b) {
                        return lvl;
                    }
                }
                return 0;
            }
        } catch (Throwable ignored) {
        }

        // 方式二：備用方案，讀取伺服器根目錄下的 ops.json
        try {
            File opsFile = new File("ops.json");
            if (!opsFile.exists() && Bukkit.getWorldContainer() != null) {
                opsFile = new File(Bukkit.getWorldContainer(), "ops.json");
            }

            if (opsFile.exists() && opsFile.isFile()) {
                String content = Files.readString(opsFile.toPath(), StandardCharsets.UTF_8);
                String uuidStr = player.getUniqueId().toString();

                Pattern pattern1 = Pattern.compile("\"uuid\"\\s*:\\s*\"" + Pattern.quote(uuidStr) + "\"[^}]*?\"level\"\\s*:\\s*(\\d+)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                Matcher matcher1 = pattern1.matcher(content);
                if (matcher1.find()) {
                    return Integer.parseInt(matcher1.group(1));
                }

                Pattern pattern2 = Pattern.compile("\"level\"\\s*:\\s*(\\d+)[^}]*?\"uuid\"\\s*:\\s*\"" + Pattern.quote(uuidStr) + "\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                Matcher matcher2 = pattern2.matcher(content);
                if (matcher2.find()) {
                    return Integer.parseInt(matcher2.group(1));
                }
            }
        } catch (Throwable ignored) {
        }

        // 方式三：若玩家為 OP 且前述機制皆無法檢測（極罕見情況），原版預設 OP 等級為 4
        return 4;
    }
}
