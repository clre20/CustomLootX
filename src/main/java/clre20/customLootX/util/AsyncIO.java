package clre20.customLootX.util;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 集中式非同步磁碟 I/O 佇列 (Single-writer I/O queue)
 * <p>
 * 1. 單一專用背景執行緒依序執行所有讀寫 —— 同一檔案的寫入永遠不會並行，徹底杜絕多個 Bukkit 非同步任務同時覆寫同一檔案造成的資料損毀。
 * 2. 先寫入 .tmp 再原子替換 (atomic move)，即使伺服器在寫入途中崩潰也不會留下半截 YAML。
 * 3. 不占用 Bukkit 共用的非同步執行緒池，避免大型伺服器上與其他插件搶資源。
 * 4. 關服時 {@link #shutdown(long)} 會等候佇列清空，保證資料完整落盤。
 */
public final class AsyncIO {

    private static volatile ExecutorService executor;
    private static volatile Consumer<String> errorLogger = System.err::println;

    private AsyncIO() {
    }

    public static synchronized void start(Consumer<String> logger) {
        if (logger != null) {
            errorLogger = logger;
        }
        if (executor != null && !executor.isShutdown()) {
            return;
        }
        ThreadPoolExecutor pool = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), r -> {
            Thread t = new Thread(r, "CustomLootX-IO");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        });
        executor = pool;
    }

    /**
     * 等候佇列中所有工作完成後關閉 (關服時呼叫)
     */
    public static synchronized void shutdown(long timeoutMillis) {
        ExecutorService ex = executor;
        executor = null;
        if (ex == null) return;
        ex.shutdown();
        try {
            if (!ex.awaitTermination(timeoutMillis, TimeUnit.MILLISECONDS)) {
                errorLogger.accept("I/O 佇列在 " + timeoutMillis + "ms 內未完成，剩餘工作將被中止。");
                ex.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ex.shutdownNow();
        }
    }

    public static boolean isRunning() {
        ExecutorService ex = executor;
        return ex != null && !ex.isShutdown();
    }

    /**
     * 提交背景 I/O 工作；若佇列已關閉 (例如關服期間) 則改為在呼叫端同步執行，確保資料不遺失。
     */
    public static void submit(Runnable task) {
        ExecutorService ex = executor;
        if (ex != null && !ex.isShutdown()) {
            try {
                ex.execute(() -> runSafely(task));
                return;
            } catch (RejectedExecutionException ignored) {
                // 落入同步執行
            }
        }
        runSafely(task);
    }

    /**
     * 依 sync 旗標決定同步執行或排入背景佇列
     */
    public static void run(Runnable task, boolean sync) {
        if (sync) {
            runAndWait(task);
        } else {
            submit(task);
        }
    }

    /**
     * 同步存檔：仍排入同一佇列並等候完成，確保不會被排在前面、較舊的非同步寫入覆蓋
     */
    public static void runAndWait(Runnable task) {
        ExecutorService ex = executor;
        if (ex != null && !ex.isShutdown() && !Thread.currentThread().getName().equals("CustomLootX-IO")) {
            try {
                ex.submit(() -> runSafely(task)).get();
                return;
            } catch (RejectedExecutionException ignored) {
                // 落入同步執行
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (java.util.concurrent.ExecutionException e) {
                errorLogger.accept("同步 I/O 工作發生錯誤: " + e.getCause());
                return;
            }
        }
        runSafely(task);
    }

    private static void runSafely(Runnable task) {
        try {
            task.run();
        } catch (Throwable t) {
            errorLogger.accept("背景 I/O 工作發生錯誤: " + t);
        }
    }

    /**
     * 原子寫入 YAML：先寫入同目錄的暫存檔，再以 atomic move 取代原檔
     */
    public static void writeYamlAtomic(YamlConfiguration yaml, File target) throws IOException {
        writeStringAtomic(yaml.saveToString(), target);
    }

    public static void writeStringAtomic(String content, File target) throws IOException {
        File parent = target.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        Path targetPath = target.toPath();
        Path tmp = targetPath.resolveSibling(target.getName() + ".tmp");
        Files.write(tmp, content.getBytes(StandardCharsets.UTF_8));
        try {
            Files.move(tmp, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
