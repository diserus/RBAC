import org.junit.jupiter.api.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class RBACSystemTest {
    private RBACSystem system;

    @BeforeEach
    void setUp() {
        system = new RBACSystem();
        system.initialize();
    }

    @AfterEach
    void tearDown() {
        system.shutdown();
    }

    @Test
    void initialize_shouldCreateDefaultRoles() {
        assertTrue(system.getRoleManager().exists("Admin"));
        assertTrue(system.getRoleManager().exists("Manager"));
        assertTrue(system.getRoleManager().exists("Viewer"));
    }

    @Test
    void initialize_shouldCreateAdminUser() {
        assertTrue(system.getUserManager().exists("admin"));
    }

    @Test
    void initialize_shouldAssignAdminRoleToAdmin() {
        User admin = system.getUserManager().findByUsername("admin").orElseThrow();
        Role adminRole = system.getRoleManager().findByName("Admin").orElseThrow();
        assertTrue(system.getAssignmentManager().userHasRole(admin, adminRole));
    }

    @Test
    void initialize_adminRoleShouldHaveAllPermissions() {
        Role adminRole = system.getRoleManager().findByName("Admin").orElseThrow();
        assertTrue(adminRole.hasPermission("READ",   "users"));
        assertTrue(adminRole.hasPermission("WRITE",  "users"));
        assertTrue(adminRole.hasPermission("DELETE", "users"));
        assertTrue(adminRole.hasPermission("READ",   "roles"));
        assertTrue(adminRole.hasPermission("WRITE",  "roles"));
        assertTrue(adminRole.hasPermission("DELETE", "roles"));
    }

    @Test
    void initialize_viewerRoleShouldHaveOnlyReadPermissions() {
        Role viewer = system.getRoleManager().findByName("Viewer").orElseThrow();
        assertTrue(viewer.hasPermission("READ", "users"));
        assertFalse(viewer.hasPermission("WRITE", "users"));
        assertFalse(viewer.hasPermission("DELETE", "users"));
    }

    @Test
    void setCurrentUser_shouldChangeCurrentUser() {
        system.setCurrentUser("operator");
        assertEquals("operator", system.getCurrentUser());
    }

    @Test
    void generateStatistics_shouldContainCounts() {
        String stats = system.generateStatistics();
        assertTrue(stats.contains("1")); // 1 пользователь
        assertTrue(stats.contains("3")); // 3 роли
    }

    @Test
    void generateStatistics_shouldContainTop3Roles() {
        String stats = system.generateStatistics();
        assertTrue(stats.contains("Admin"));
    }

    @Test
    void managers_shouldNotBeNull() {
        assertNotNull(system.getUserManager());
        assertNotNull(system.getRoleManager());
        assertNotNull(system.getAssignmentManager());
    }

    @Test
    void maintenanceService_shouldNotBeNull() {
        assertNotNull(system.getMaintenanceService());
    }
}
