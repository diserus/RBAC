import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssignmentMaintenanceServiceTest {
    private final RBACSystem system = new RBACSystem();

    AssignmentMaintenanceServiceTest() {
        system.initialize();
    }

    @AfterEach
    void tearDown() {
        system.shutdown();
    }

    @Test
    void runCycle_shouldDeactivateExpiredTemporaryAssignmentsAndLogStats() {
        User user = User.create("tempuser", "Temp User", "tempuser@example.com");
        Role role = new Role("TempRole", "Temporary role");

        system.getUserManager().add(user);
        system.getRoleManager().add(role);

        TemporaryAssignment assignment = new TemporaryAssignment(
                user,
                role,
                AssignmentMetadata.now("tester", "expired"),
                "2000-01-01 00:00:00",
                false
        );
        system.getAssignmentManager().add(assignment);

        system.getMaintenanceService().runCycle();

        assertFalse(assignment.isActive());
        assertTrue(assignment.isDeactivated());

        List<AuditLog.AuditEntry> maintenanceEntries = system.getAuditLog()
                .getByAction("ASSIGNMENT_MAINTENANCE");
        assertFalse(maintenanceEntries.isEmpty());
        assertTrue(maintenanceEntries.getLast().details().contains("deactivated=1"));

        List<AuditLog.AuditEntry> statsEntries = system.getAuditLog().getByAction("SYSTEM_STATS");
        assertFalse(statsEntries.isEmpty());
        assertTrue(statsEntries.getLast().details().contains("Пользователей"));
    }
}
