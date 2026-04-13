import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class BackgroundExecutor {
    private final ExecutorService executorService;

    public BackgroundExecutor() {
        AtomicInteger counter = new AtomicInteger(1);
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "rbac-background-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
        executorService = Executors.newFixedThreadPool(
                Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
                factory
        );
    }

    public Future<?> submit(Runnable task) {
        return executorService.submit(task);
    }

    public <T> Future<T> submit(Callable<T> task) {
        return executorService.submit(task);
    }

    public void shutdown() {
        executorService.shutdownNow();
    }
}
