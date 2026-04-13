import org.junit.jupiter.api.*;
import java.io.*;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AuditLogTest {

    private AuditLog log;

    @BeforeEach
    void setUp() {
        log = new AuditLog();
    }

    // ─── log / getAll ──────────────────────────────────────────────────────

    @Test
    void log_singleEntry_shouldBeRetrievable() {
        log.log("USER_CREATE", "admin", "alice", "email=alice@x.com");
        assertEquals(1, log.getAll().size());
    }

    @Test
    void log_multipleEntries_allStored() {
        log.log("USER_CREATE", "admin", "alice", null);
        log.log("ROLE_ASSIGN", "admin", "alice", "role=Admin");
        log.log("USER_DELETE", "admin", "alice", null);
        assertEquals(3, log.getAll().size());
    }

    @Test
    void log_entryHasCorrectFields() {
        log.log("ROLE_CREATE", "admin", "Manager", "desc=test");
        AuditLog.AuditEntry entry = log.getAll().get(0);
        assertEquals("ROLE_CREATE", entry.action());
        assertEquals("admin",       entry.performer());
        assertEquals("Manager",     entry.target());
        assertEquals("desc=test",   entry.details());
    }

    @Test
    void log_entryTimestamp_matchesDateTimePattern() {
        log.log("TEST", "user", "target", null);
        String ts = log.getAll().get(0).timestamp();
        assertTrue(ts.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"),
                "Timestamp должен быть в формате YYYY-MM-DD HH:MM:SS, получено: " + ts);
    }

    @Test
    void getAll_returnsUnmodifiableCopy() {
        log.log("X", "a", "b", null);
        List<AuditLog.AuditEntry> all = log.getAll();
        assertThrows(UnsupportedOperationException.class, () -> all.add(null));
    }

    // ─── getByPerformer ────────────────────────────────────────────────────

    @Test
    void getByPerformer_existingPerformer_returnsMatchingEntries() {
        log.log("CREATE", "admin", "alice", null);
        log.log("CREATE", "admin", "bob",   null);
        log.log("CREATE", "other", "carol", null);
        List<AuditLog.AuditEntry> result = log.getByPerformer("admin");
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(e -> e.performer().equals("admin")));
    }

    @Test
    void getByPerformer_unknownPerformer_returnsEmpty() {
        log.log("CREATE", "admin", "alice", null);
        assertTrue(log.getByPerformer("nobody").isEmpty());
    }

    // ─── getByAction ───────────────────────────────────────────────────────

    @Test
    void getByAction_existingAction_returnsMatchingEntries() {
        log.log("USER_CREATE", "admin", "alice", null);
        log.log("USER_CREATE", "admin", "bob",   null);
        log.log("ROLE_CREATE", "admin", "Admin", null);
        List<AuditLog.AuditEntry> result = log.getByAction("USER_CREATE");
        assertEquals(2, result.size());
    }

    @Test
    void getByAction_caseInsensitive_shouldMatch() {
        log.log("USER_CREATE", "admin", "alice", null);
        assertEquals(1, log.getByAction("user_create").size());
    }

    @Test
    void getByAction_unknownAction_returnsEmpty() {
        log.log("USER_CREATE", "admin", "alice", null);
        assertTrue(log.getByAction("NONEXISTENT").isEmpty());
    }

    // ─── printLog ──────────────────────────────────────────────────────────

    @Test
    void printLog_empty_printsEmptyMessage() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        log.printLog();
        System.setOut(System.out);
        assertTrue(out.toString().toLowerCase().contains("пуст"));
    }

    @Test
    void printLog_withEntries_printsActions() {
        log.log("USER_CREATE", "admin", "alice", null);
        log.log("ROLE_ASSIGN", "admin", "alice", null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        log.printLog();
        System.setOut(System.out);
        String output = out.toString();
        assertTrue(output.contains("USER_CREATE"));
        assertTrue(output.contains("ROLE_ASSIGN"));
    }

    // ─── saveToFile ────────────────────────────────────────────────────────

    @Test
    void saveToFile_createsFileWithEntries() throws IOException {
        log.log("USER_CREATE", "admin", "alice", "email=a@x.com");
        String filename = "test_audit_log.txt";
        try {
            log.saveToFile(filename);
            String content = Files.readString(Path.of(filename));
            assertTrue(content.contains("USER_CREATE"));
            assertTrue(content.contains("alice"));
        } finally {
            Files.deleteIfExists(Path.of(filename));
        }
    }

    @Test
    void saveToFile_emptyLog_createsFileWithHeader() throws IOException {
        String filename = "test_audit_empty.txt";
        try {
            log.saveToFile(filename);
            assertTrue(Files.exists(Path.of(filename)));
            String content = Files.readString(Path.of(filename));
            assertTrue(content.contains("ЖУРНАЛ"));
        } finally {
            Files.deleteIfExists(Path.of(filename));
        }
    }

    @Test
    void log_asyncQueue_shouldBecomeVisibleOnRead() {
        log.log("USER_CREATE", "admin", "alice", null);
        log.log("ROLE_ASSIGN", "admin", "alice", "role=Admin");

        List<AuditLog.AuditEntry> entries = log.getAll();

        assertEquals(2, entries.size());
        assertEquals("USER_CREATE", entries.get(0).action());
        assertEquals("ROLE_ASSIGN", entries.get(1).action());
    }
}
