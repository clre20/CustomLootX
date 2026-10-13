package clre20.customLootX.util;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 區塊空間索引：world -> (chunkKey(long) -> Set&lt;locationKey&gt;)
 * <p>
 * 以 long 型區塊座標作為鍵，查詢時零字串配置；
 * 區塊載入、玩家周邊掃描等高頻路徑僅需 O(1) 查表，不再線性掃描所有已放置方塊。
 */
public final class ChunkIndex {

    private final Map<String, Map<Long, Set<String>>> index = new ConcurrentHashMap<>();

    public static long chunkKey(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
    }

    /**
     * 解析 "world:x:y:z" 的區塊鍵；格式錯誤時回傳 null
     */
    public static ParsedKey parse(String locKey) {
        if (locKey == null) return null;
        int last = locKey.lastIndexOf(':');
        if (last <= 0) return null;
        int mid = locKey.lastIndexOf(':', last - 1);
        if (mid <= 0) return null;
        int first = locKey.lastIndexOf(':', mid - 1);
        if (first <= 0) return null;
        try {
            String world = locKey.substring(0, first);
            int x = Integer.parseInt(locKey, first + 1, mid, 10);
            int y = Integer.parseInt(locKey, mid + 1, last, 10);
            int z = Integer.parseInt(locKey, last + 1, locKey.length(), 10);
            return new ParsedKey(world, x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public record ParsedKey(String world, int x, int y, int z) {
    }

    public void add(String locKey) {
        ParsedKey p = parse(locKey);
        if (p == null) return;
        index.computeIfAbsent(p.world(), w -> new ConcurrentHashMap<>())
                .computeIfAbsent(chunkKey(p.x() >> 4, p.z() >> 4), k -> ConcurrentHashMap.newKeySet())
                .add(locKey);
    }

    public void remove(String locKey) {
        ParsedKey p = parse(locKey);
        if (p == null) return;
        Map<Long, Set<String>> chunks = index.get(p.world());
        if (chunks == null) return;
        long ck = chunkKey(p.x() >> 4, p.z() >> 4);
        Set<String> set = chunks.get(ck);
        if (set == null) return;
        set.remove(locKey);
        if (set.isEmpty()) {
            chunks.remove(ck, set);
            if (chunks.isEmpty()) {
                index.remove(p.world(), chunks);
            }
        }
    }

    public Set<String> get(String world, int cx, int cz) {
        Map<Long, Set<String>> chunks = index.get(world);
        if (chunks == null) return Collections.emptySet();
        Set<String> set = chunks.get(chunkKey(cx, cz));
        return set != null ? set : Collections.emptySet();
    }

    public boolean hasWorld(String world) {
        Map<Long, Set<String>> chunks = index.get(world);
        return chunks != null && !chunks.isEmpty();
    }

    public boolean isEmpty() {
        return index.isEmpty();
    }

    public void clear() {
        index.clear();
    }
}
