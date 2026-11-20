package clre20.customLootX.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 配置的資料夾索引：名稱 -> 資料夾 / 檔案，並快取排序後的資料夾與名稱清單 (Tab 補全與 list 指令使用)。
 * <p>
 * 名稱一律以小寫作為鍵；資料夾保留原始大小寫顯示，比對時不分大小寫。
 * 清單快取只在配置新增、刪除、搬移或重載時重建，Tab 補全不需要每次排序數百個名稱。
 */
public final class TemplateIndex {

    private record Entry(String displayName, String folder, File file) {
    }

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private volatile Snapshot snapshot = Snapshot.EMPTY;

    private record Snapshot(List<String> folders,
                            Map<String, String> folderCanonical,
                            Map<String, List<String>> namesByFolder,
                            Map<String, Integer> countByFolder) {
        static final Snapshot EMPTY = new Snapshot(List.of(), Map.of(), Map.of(), Map.of());
    }

    public void put(String displayName, String folder, File file) {
        if (displayName == null) return;
        entries.put(displayName.toLowerCase(Locale.ROOT), new Entry(displayName, folder == null ? "" : folder, file));
        rebuild();
    }

    public void remove(String name) {
        if (name == null) return;
        if (entries.remove(name.toLowerCase(Locale.ROOT)) != null) {
            rebuild();
        }
    }

    /**
     * 整批替換 (重載時使用)，只重建一次快取
     */
    public void replaceAll(Map<String, String> displayNames, Map<String, String> folders, Map<String, File> files) {
        entries.clear();
        for (Map.Entry<String, String> e : displayNames.entrySet()) {
            String key = e.getKey();
            entries.put(key, new Entry(e.getValue(), folders.getOrDefault(key, ""), files.get(key)));
        }
        rebuild();
    }

    public boolean contains(String name) {
        return name != null && entries.containsKey(name.toLowerCase(Locale.ROOT));
    }

    /**
     * 配置所在資料夾 ("" 代表根目錄)；不存在時回傳 null
     */
    public String folderOf(String name) {
        if (name == null) return null;
        Entry e = entries.get(name.toLowerCase(Locale.ROOT));
        return e == null ? null : e.folder();
    }

    public File fileOf(String name) {
        if (name == null) return null;
        Entry e = entries.get(name.toLowerCase(Locale.ROOT));
        return e == null ? null : e.file();
    }

    /**
     * 顯示用路徑：資料夾/名稱
     */
    public String pathOf(String name) {
        if (name == null) return "";
        Entry e = entries.get(name.toLowerCase(Locale.ROOT));
        if (e == null) return name;
        return TemplateFolders.join(e.folder(), e.displayName());
    }

    /**
     * 是否為已存在配置的資料夾 (含上層資料夾，例如 a/b/c.yml 會讓 a 與 a/b 都成立)
     */
    public boolean isFolder(String folder) {
        if (folder == null) return false;
        String n = TemplateFolders.normalize(folder);
        if (n == null || n.isEmpty()) return false;
        return snapshot.folderCanonical().containsKey(n.toLowerCase(Locale.ROOT));
    }

    /**
     * 取得資料夾的原始大小寫寫法；不存在時回傳 null
     */
    public String canonicalFolder(String folder) {
        String n = TemplateFolders.normalize(folder);
        if (n == null) return null;
        if (n.isEmpty()) return "";
        return snapshot.folderCanonical().get(n.toLowerCase(Locale.ROOT));
    }

    /**
     * 所有資料夾 (已排序)
     */
    public List<String> folders() {
        return snapshot.folders();
    }

    /**
     * 指定資料夾「直接」包含的配置名稱 (已排序)；"" 代表根目錄
     */
    public List<String> namesIn(String folder) {
        String key = folder == null ? "" : folder.toLowerCase(Locale.ROOT);
        return snapshot.namesByFolder().getOrDefault(key, List.of());
    }

    /**
     * 指定資料夾 (含所有子資料夾) 的配置數量
     */
    public int countUnder(String folder) {
        String key = folder == null ? "" : folder.toLowerCase(Locale.ROOT);
        return snapshot.countByFolder().getOrDefault(key, 0);
    }

    /**
     * 指定資料夾的「下一層」子資料夾 (已排序)；"" 代表根目錄
     */
    public List<String> childFolders(String folder) {
        String prefix = (folder == null || folder.isEmpty()) ? "" : folder.toLowerCase(Locale.ROOT) + "/";
        List<String> result = new ArrayList<>();
        for (String f : snapshot.folders()) {
            String lower = f.toLowerCase(Locale.ROOT);
            if (!lower.startsWith(prefix) || lower.length() == prefix.length()) continue;
            if (lower.indexOf('/', prefix.length()) < 0) {
                result.add(f);
            }
        }
        return result;
    }

    public int size() {
        return entries.size();
    }

    private synchronized void rebuild() {
        TreeMap<String, String> canonical = new TreeMap<>();
        Map<String, TreeSet<String>> names = new HashMap<>();
        Map<String, Integer> counts = new HashMap<>();
        for (Entry e : entries.values()) {
            String folder = e.folder();
            String folderKey = folder.toLowerCase(Locale.ROOT);
            names.computeIfAbsent(folderKey, k -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)).add(e.displayName());
            counts.merge("", 1, Integer::sum);
            if (!folder.isEmpty()) {
                // 登記每一層上層資料夾
                String[] parts = folder.split("/");
                StringBuilder sb = new StringBuilder();
                for (String part : parts) {
                    if (!sb.isEmpty()) sb.append('/');
                    sb.append(part);
                    String path = sb.toString();
                    String lower = path.toLowerCase(Locale.ROOT);
                    canonical.putIfAbsent(lower, path);
                    counts.merge(lower, 1, Integer::sum);
                }
            }
        }
        List<String> folderList = new ArrayList<>(canonical.values());
        Map<String, List<String>> nameLists = new HashMap<>();
        for (Map.Entry<String, TreeSet<String>> e : names.entrySet()) {
            nameLists.put(e.getKey(), List.copyOf(e.getValue()));
        }
        snapshot = new Snapshot(Collections.unmodifiableList(folderList),
                Collections.unmodifiableMap(canonical),
                Collections.unmodifiableMap(nameLists),
                Collections.unmodifiableMap(counts));
    }
}
