package clre20.customLootX.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 配置檔資料夾分類工具
 * <p>
 * /data/&lt;類型&gt;/ 底下可以用子資料夾分類 (最多 {@link #MAX_DEPTH} 層)。
 * 配置的識別名稱仍是檔名 (同類型內全域唯一)，資料夾只負責整理，
 * 因此把檔案搬到其他資料夾後，世界上已放置的方塊與手上的物品都不受影響。
 */
public final class TemplateFolders {

    /** 資料夾最多幾層 */
    public static final int MAX_DEPTH = 5;
    private static final Pattern SEGMENT = Pattern.compile("^[A-Za-z0-9_-]{1,32}$");

    private TemplateFolders() {
    }

    /**
     * 正規化資料夾路徑：統一使用 '/'、去掉頭尾斜線；格式不合法時回傳 null，根目錄回傳 ""
     */
    public static String normalize(String raw) {
        if (raw == null) return "";
        String s = raw.trim().replace('\\', '/');
        while (s.startsWith("/")) s = s.substring(1);
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        if (s.isEmpty()) return "";
        String[] parts = s.split("/", -1);
        if (parts.length > MAX_DEPTH) return null;
        for (String p : parts) {
            if (!SEGMENT.matcher(p).matches()) return null;
        }
        return s;
    }

    /**
     * 聊天室名稱輸入解析結果
     *
     * @param folder          指定的資料夾 ("" 代表根目錄)，未指定時為 null
     * @param name            配置名稱
     */
    public record NameInput(String folder, String name) {
        public boolean folderSpecified() {
            return folder != null;
        }
    }

    /**
     * 解析「名稱」、「資料夾/名稱」或「/名稱」(放回根目錄)
     *
     * @return 格式不合法時回傳 null
     */
    public static NameInput parseNameInput(String raw, Pattern namePattern) {
        if (raw == null) return null;
        String s = raw.trim().replace('\\', '/');
        if (s.isEmpty() || s.length() > 200) return null;
        int slash = s.lastIndexOf('/');
        if (slash < 0) {
            return namePattern.matcher(s).matches() ? new NameInput(null, s) : null;
        }
        String name = s.substring(slash + 1);
        if (!namePattern.matcher(name).matches()) return null;
        String folder = normalize(s.substring(0, slash));
        if (folder == null) return null;
        return new NameInput(folder, name);
    }

    /**
     * 組合顯示用的相對路徑 (資料夾/名稱)
     */
    public static String join(String folder, String name) {
        if (folder == null || folder.isEmpty()) return name == null ? "" : name;
        return folder + "/" + name;
    }

    /**
     * 取得配置檔案位置，並確認不會跳出基底資料夾
     */
    public static File fileFor(File baseDir, String folder, String name) throws IOException {
        String f = normalize(folder);
        if (f == null) throw new IOException("資料夾名稱不合法: " + folder);
        File dir = f.isEmpty() ? baseDir : new File(baseDir, f.replace('/', File.separatorChar));
        File file = new File(dir, name + ".yml");
        Path base = baseDir.toPath().toAbsolutePath().normalize();
        Path target = file.toPath().toAbsolutePath().normalize();
        if (!target.startsWith(base) || target.equals(base)) {
            throw new IOException("配置路徑不合法: " + name);
        }
        return file;
    }

    /**
     * 掃描結果
     *
     * @param file   配置檔
     * @param folder 相對資料夾 ("" 代表根目錄)
     */
    public record Scanned(File file, String folder) {
    }

    /**
     * 遞迴掃描基底資料夾內的 .yml 配置 (不跟隨符號連結；淺層優先、再依路徑排序，確保重名時結果固定)
     */
    public static List<Scanned> scan(File baseDir, Consumer<String> warn) {
        List<Scanned> result = new ArrayList<>();
        if (baseDir == null || !baseDir.isDirectory()) return result;
        Path base = baseDir.toPath();
        // 深度 = 資料夾層數 + 1 (檔案本身)
        try (Stream<Path> stream = Files.walk(base, MAX_DEPTH + 1, new FileVisitOption[0])) {
            stream.filter(p -> Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS))
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".yml"))
                    // 淺層優先 (根目錄的檔案先)，同層再依路徑字母排序：重名時固定保留較淺層的那一個
                    .sorted(Comparator.<Path>comparingInt(p -> base.relativize(p).getNameCount())
                            .thenComparing(p -> base.relativize(p).toString().toLowerCase(Locale.ROOT)))
                    .forEach(p -> {
                        Path rel = base.relativize(p.getParent());
                        String relStr = rel.toString().replace(File.separatorChar, '/');
                        String folder = normalize(relStr);
                        if (folder == null) {
                            if (warn != null) {
                                warn.accept("略過資料夾名稱不合法的配置 (只能用英文字母、數字、底線、減號): " + base.relativize(p));
                            }
                            return;
                        }
                        result.add(new Scanned(p.toFile(), folder));
                    });
        } catch (IOException | RuntimeException e) {
            if (warn != null) warn.accept("掃描配置資料夾失敗: " + baseDir + " - " + e.getMessage());
        }
        return result;
    }

}
