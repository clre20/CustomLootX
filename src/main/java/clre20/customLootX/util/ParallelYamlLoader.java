package clre20.customLootX.util;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 大量配置檔的平行讀取器
 * <p>
 * 只負責「讀檔 + 解析 YAML」這段最耗時的工作 (數百個含物品資料的配置檔)，
 * 轉換成模板物件仍由呼叫端在主執行緒完成。
 * 執行緒池只在讀取期間存在，讀完立即關閉，不會常駐占用副核心。
 */
public final class ParallelYamlLoader {

    /** 少於此數量時直接在呼叫端依序讀取，建立執行緒池反而更慢 */
    private static final int PARALLEL_THRESHOLD = 8;

    private ParallelYamlLoader() {
    }

    /**
     * @param source 來源檔案
     * @param yaml   解析結果；失敗時為 null
     * @param error  失敗原因；成功時為 null
     */
    public record Loaded(TemplateFolders.Scanned source, YamlConfiguration yaml, String error) {
    }

    public static List<Loaded> load(List<TemplateFolders.Scanned> files, int threads) {
        List<Loaded> result = new ArrayList<>(files.size());
        int n = Math.min(Math.max(1, threads), Math.min(4, Math.max(1, Runtime.getRuntime().availableProcessors())));
        n = Math.min(n, (files.size() + PARALLEL_THRESHOLD - 1) / PARALLEL_THRESHOLD);
        if (n <= 1) {
            for (TemplateFolders.Scanned s : files) {
                result.add(read(s));
            }
            return result;
        }

        AtomicInteger counter = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(n, r -> {
            Thread t = new Thread(r, "CustomLootX-Loader-" + counter.incrementAndGet());
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        });
        try {
            List<Callable<Loaded>> tasks = new ArrayList<>(files.size());
            for (TemplateFolders.Scanned s : files) {
                tasks.add(() -> read(s));
            }
            List<Future<Loaded>> futures = pool.invokeAll(tasks);
            for (int i = 0; i < futures.size(); i++) {
                try {
                    result.add(futures.get(i).get());
                } catch (ExecutionException e) {
                    result.add(new Loaded(files.get(i), null, String.valueOf(e.getCause())));
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            pool.shutdownNow();
        }
        return result;
    }

    private static Loaded read(TemplateFolders.Scanned s) {
        try {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.load(s.file());
            return new Loaded(s, yaml, null);
        } catch (Exception e) {
            return new Loaded(s, null, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
