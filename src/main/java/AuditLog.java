import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
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

    private final List<AuditEntry> entries = new ArrayList<>();

    /**
     * Добавляет запись в лог. Timestamp берётся из DateUtils.
     */
    public void log(String action, String performer, String target, String details) {
        String ts = DateUtils.getCurrentDateTime();
        entries.add(new AuditEntry(ts, action, performer, target, details));
    }

    public List<AuditEntry> getAll() {
        return List.copyOf(entries);
    }

    public List<AuditEntry> getByPerformer(String performer) {
        return entries.stream()
                .filter(e -> e.performer().equals(performer))
                .collect(Collectors.toList());
    }

    public List<AuditEntry> getByAction(String action) {
        return entries.stream()
                .filter(e -> e.action().equalsIgnoreCase(action))
                .collect(Collectors.toList());
    }

    public void printLog() {
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
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename, false))) {
            pw.println("=== ЖУРНАЛ АУДИТА ===");
            entries.forEach(pw::println);
            pw.printf("Итого записей: %d%n", entries.size());
        } catch (IOException e) {
            System.out.println("✗ Ошибка записи в файл: " + e.getMessage());
        }
    }
}