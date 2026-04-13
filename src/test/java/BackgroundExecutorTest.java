import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackgroundExecutorTest {
    private final RBACSystem system = new RBACSystem();

    BackgroundExecutorTest() {
        system.initialize();
    }

    @AfterEach
    void tearDown() {
        system.shutdown();
    }

    @Test
    void generateUserReportAsync_shouldReturnReport() throws Exception {
        Future<String> future = system.generateUserReportAsync();

        String report = future.get(5, TimeUnit.SECONDS);

        assertTrue(report.contains("ОТЧЁТ ПО ПОЛЬЗОВАТЕЛЯМ"));
        assertTrue(report.contains("admin"));
    }

    @Test
    void saveSnapshotAsync_shouldCreateSnapshotFile() throws Exception {
        Path snapshot = Path.of("test_snapshot_async.txt");
        try {
            Future<String> future = system.saveSnapshotAsync(snapshot.toString());

            assertEquals(snapshot.toString(), future.get(5, TimeUnit.SECONDS));
            assertTrue(Files.exists(snapshot));
            assertTrue(Files.readString(snapshot).contains("RBAC SYSTEM SNAPSHOT"));
        } finally {
            Files.deleteIfExists(snapshot);
        }
    }

    @Test
    void backgroundExecutor_shouldBeAvailable() {
        assertNotNull(system.getBackgroundExecutor());
    }
}
