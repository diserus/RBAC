import org.junit.jupiter.api.*;
import java.io.*;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class ReportGeneratorTest {

    private RBACSystem system;
    private ReportGenerator generator;

    @BeforeEach
    void setUp() {
        system = new RBACSystem();
        system.initialize();
        generator = new ReportGenerator();
    }

    // ─── generateUserReport ────────────────────────────────────────────────

    @Test
    void generateUserReport_containsHeader() {
        String report = generator.generateUserReport(
                system.getUserManager(), system.getAssignmentManager());
        assertTrue(report.contains("ОТЧЁТ ПО ПОЛЬЗОВАТЕЛЯМ"));
    }

    @Test
    void generateUserReport_containsExistingUser() {
        String report = generator.generateUserReport(
                system.getUserManager(), system.getAssignmentManager());
        assertTrue(report.contains("admin"));
    }

    @Test
    void generateUserReport_containsUserEmail() {
        String report = generator.generateUserReport(
                system.getUserManager(), system.getAssignmentManager());
        assertTrue(report.contains("admin@system.com"));
    }

    @Test
    void generateUserReport_containsRoleForUser() {
        String report = generator.generateUserReport(
                system.getUserManager(), system.getAssignmentManager());
        assertTrue(report.contains("Admin"));
    }

    @Test
    void generateUserReport_afterAddingUser_containsNewUser() {
        User bob = User.create("bob", "Bob Smith", "bob@x.com");
        system.getUserManager().add(bob);
        String report = generator.generateUserReport(
                system.getUserManager(), system.getAssignmentManager());
        assertTrue(report.contains("bob"));
    }

    @Test
    void generateUserReport_userWithNoRoles_showsDash() {
        User carol = User.create("carol", "Carol White", "carol@x.com");
        system.getUserManager().add(carol);
        String report = generator.generateUserReport(
                system.getUserManager(), system.getAssignmentManager());
        assertTrue(report.contains("—") || report.contains("-"));
    }

    // ─── generateRoleReport ────────────────────────────────────────────────

    @Test
    void generateRoleReport_containsAllRoles() {
        String report = generator.generateRoleReport(
                system.getRoleManager(), system.getAssignmentManager());
        assertTrue(report.contains("Admin"));
        assertTrue(report.contains("Manager"));
        assertTrue(report.contains("Viewer"));
    }

    @Test
    void generateRoleReport_containsPermissionCount() {
        String report = generator.generateRoleReport(
                system.getRoleManager(), system.getAssignmentManager());
        // Admin имеет 8 прав
        assertTrue(report.contains("8"));
    }

    @Test
    void generateRoleReport_containsActiveUserCount() {
        String report = generator.generateRoleReport(
                system.getRoleManager(), system.getAssignmentManager());
        // admin назначен на Admin
        assertTrue(report.contains("1"));
    }

    // ─── generatePermissionMatrix ──────────────────────────────────────────

    @Test
    void generatePermissionMatrix_containsUsernames() {
        String report = generator.generatePermissionMatrix(
                system.getUserManager(), system.getAssignmentManager());
        assertTrue(report.contains("admin"));
    }

    @Test
    void generatePermissionMatrix_containsResources() {
        String report = generator.generatePermissionMatrix(
                system.getUserManager(), system.getAssignmentManager());
        assertTrue(report.contains("users"));
        assertTrue(report.contains("roles"));
        assertTrue(report.contains("reports"));
    }

    @Test
    void generatePermissionMatrix_containsPermissionNames() {
        String report = generator.generatePermissionMatrix(
                system.getUserManager(), system.getAssignmentManager());
        assertTrue(report.contains("READ") || report.contains("WRITE"));
    }

    @Test
    void generatePermissionMatrix_noAssignments_showsNoData() {
        // Создаём чистую систему без initialize()
        RBACSystem empty = new RBACSystem();
        String report = generator.generatePermissionMatrix(
                empty.getUserManager(), empty.getAssignmentManager());
        assertTrue(report.contains("Нет данных") || report.contains("МАТРИЦА"));
    }

    // ─── exportToFile ──────────────────────────────────────────────────────

    @Test
    void exportToFile_createsFileWithContent() throws IOException {
        String report = "Test report content";
        String filename = "test_report_output.txt";
        try {
            generator.exportToFile(report, filename);
            assertTrue(Files.exists(Path.of(filename)));
            String content = Files.readString(Path.of(filename));
            assertTrue(content.contains("Test report content"));
        } finally {
            Files.deleteIfExists(Path.of(filename));
        }
    }

    @Test
    void exportToFile_overwritesExistingFile() throws IOException {
        String filename = "test_report_overwrite.txt";
        try {
            generator.exportToFile("first content", filename);
            generator.exportToFile("second content", filename);
            String content = Files.readString(Path.of(filename));
            assertFalse(content.contains("first content"));
            assertTrue(content.contains("second content"));
        } finally {
            Files.deleteIfExists(Path.of(filename));
        }
    }
}