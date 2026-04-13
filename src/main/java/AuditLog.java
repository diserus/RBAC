import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AuditLog {

    public record AuditEntry(
            String timestamp,
            String action,
            String performer,
            String target,
            String details
    ) {
        @Override
        public String toString() {
            return String.format("[%s] %-20s | performer=%-15s | target=%-20s | %s",
                    timestamp, action, performer, target, details != null ? details : "");
        }
    }

    private final List<AuditEntry> entries = new CopyOnWriteArrayList<>();
    private final BlockingQueue<LogCommand> queue = new LinkedBlockingQueue<>();
    private final Thread workerThread;

    public AuditLog() {
        workerThread = new Thread(this::processQueue, "audit-log-worker");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    public void log(String action, String performer, String target, String details) {
        String ts = DateUtils.getCurrentDateTime();
        queue.offer(LogCommand.entry(new AuditEntry(ts, action, performer, target, details)));
    }

    public List<AuditEntry> getAll() {
        awaitQueueDrain();
        return List.copyOf(entries);
    }

    public List<AuditEntry> getByPerformer(String performer) {
        awaitQueueDrain();
        return entries.stream()
                .filter(e -> e.performer().equals(performer))
                .collect(Collectors.toList());
    }

    public List<AuditEntry> getByAction(String action) {
        awaitQueueDrain();
        return entries.stream()
                .filter(e -> e.action().equalsIgnoreCase(action))
                .collect(Collectors.toList());
    }

    public void printLog() {
        awaitQueueDrain();
        if (entries.isEmpty()) {
            System.out.println("Журнал аудита пуст.");
            return;
        }
        String header = FormatUtils.formatHeader("ЖУРНАЛ АУДИТА");
        System.out.println(header);
        entries.forEach(e -> System.out.println(e));
        System.out.println("─".repeat(80));
        System.out.printf("Итого записей: %d%n", entries.size());
    }

    public void saveToFile(String filename) {
        awaitQueueDrain();
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename, false))) {
            pw.println("=== ЖУРНАЛ АУДИТА ===");
            entries.forEach(pw::println);
            pw.printf("Итого записей: %d%n", entries.size());
        } catch (IOException e) {
            System.out.println("✗ Ошибка записи в файл: " + e.getMessage());
        }
    }

    private void processQueue() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                LogCommand command = queue.take();
                if (command.entry != null) {
                    entries.add(command.entry);
                }
                if (command.afterProcessing != null) {
                    command.afterProcessing.countDown();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void awaitQueueDrain() {
        CountDownLatch latch = new CountDownLatch(1);
        queue.offer(LogCommand.barrier(latch));
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Очередь аудита не обработана вовремя");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Ожидание аудита было прервано", e);
        }
    }

    private static final class LogCommand {
        private final AuditEntry entry;
        private final CountDownLatch afterProcessing;

        private LogCommand(AuditEntry entry, CountDownLatch afterProcessing) {
            this.entry = entry;
            this.afterProcessing = afterProcessing;
        }

        private static LogCommand entry(AuditEntry entry) {
            return new LogCommand(entry, null);
        }

        private static LogCommand barrier(CountDownLatch latch) {
            return new LogCommand(null, latch);
        }
    }
}
