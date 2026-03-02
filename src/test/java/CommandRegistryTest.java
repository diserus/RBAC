import org.junit.jupiter.api.*;
import java.io.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CommandRegistryTest {
    private CommandParser parser;
    private RBACSystem system;

    // Утилита: подменить System.in и System.out, выполнить команду, вернуть вывод
    private String execute(String commandName, String input) {
        Scanner scanner = new Scanner(new ByteArrayInputStream(input.getBytes()));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream oldOut = System.out;
        System.setOut(new PrintStream(out));
        parser.executeCommand(commandName, scanner, system);
        System.setOut(oldOut);
        return out.toString();
    }

    @BeforeEach
    void setUp() {
        parser = new CommandParser();
        system = new RBACSystem();
        system.initialize();
        CommandRegistry.registerAll(parser);
    }

    // ══════════════════════ user-list ══════════════════════

    @Test
    void userList_shouldContainAdmin() {
        String output = execute("user-list", "");
        assertTrue(output.contains("admin"));
    }

    // ══════════════════════ user-create ══════════════════════

    @Test
    void userCreate_validInput_shouldAddUser() {
        execute("user-create", "newuser\nNew User\nnew@example.com\n");
        assertTrue(system.getUserManager().exists("newuser"));
    }

    @Test
    void userCreate_duplicateUsername_shouldPrintError() {
        String output = execute("user-create", "admin\nAdmin Two\nadmin2@example.com\n");
        assertTrue(output.contains("✗"));
    }

    @Test
    void userCreate_invalidEmail_shouldPrintError() {
        String output = execute("user-create", "validuser\nValid Name\nnot-an-email\n");
        assertTrue(output.contains("✗"));
        assertFalse(system.getUserManager().exists("validuser"));
    }

    // ══════════════════════ user-view ══════════════════════

    @Test
    void userView_existingUser_shouldShowInfo() {
        String output = execute("user-view", "admin\n");
        assertTrue(output.contains("admin"));
        assertTrue(output.contains("Admin")); // роль
    }

    @Test
    void userView_unknownUser_shouldPrintError() {
        String output = execute("user-view", "ghost\n");
        assertTrue(output.contains("✗"));
    }

    // ══════════════════════ user-update ══════════════════════

    @Test
    void userUpdate_shouldChangeData() {
        execute("user-update", "admin\nNew Admin Name\nadmin@system.com\n");
        User updated = system.getUserManager().findByUsername("admin").orElseThrow();
        assertEquals("New Admin Name", updated.fullName());
    }

    @Test
    void userUpdate_unknownUser_shouldPrintError() {
        String output = execute("user-update", "ghost\nName\nemail@x.com\n");
        assertTrue(output.contains("✗"));
    }

    // ══════════════════════ user-delete ══════════════════════

    @Test
    void userDelete_confirmed_shouldRemoveUser() {
        execute("user-create", "todelete\nTo Delete\ntodelete@x.com\n");
        execute("user-delete", "todelete\nда\n");
        assertFalse(system.getUserManager().exists("todelete"));
    }

    @Test
    void userDelete_notConfirmed_shouldKeepUser() {
        execute("user-create", "keepme\nKeep Me\nkeepme@x.com\n");
        execute("user-delete", "keepme\nнет\n");
        assertTrue(system.getUserManager().exists("keepme"));
    }

    @Test
    void userDelete_shouldAlsoRemoveAssignments() {
        execute("user-create", "withRole\nWith Role\nwithrole@x.com\n");
        // Назначаем роль напрямую
        User user = system.getUserManager().findByUsername("withRole").orElseThrow();
        Role viewer = system.getRoleManager().findByName("Viewer").orElseThrow();
        system.getAssignmentManager().add(
                new PermanentAssignment(user, viewer, AssignmentMetadata.now("admin", null))
        );
        execute("user-delete", "withRole\nда\n");
        assertFalse(system.getUserManager().exists("withRole"));
        assertTrue(system.getAssignmentManager().findByUser(user).isEmpty());
    }

    // ══════════════════════ user-search ══════════════════════

    @Test
    void userSearch_byUsername_shouldFindUser() {
        String output = execute("user-search", "1\nadm\n");
        assertTrue(output.contains("admin"));
    }

    @Test
    void userSearch_byEmailDomain_shouldFindUser() {
        String output = execute("user-search", "3\n@system.com\n");
        assertTrue(output.contains("admin"));
    }

    @Test
    void userSearch_noResults_shouldSayNotFound() {
        String output = execute("user-search", "1\nZZZZZZZ\n");
        assertTrue(output.contains("Ничего не найдено"));
    }

    // ══════════════════════ role-list ══════════════════════

    @Test
    void roleList_shouldContainDefaultRoles() {
        String output = execute("role-list", "");
        assertTrue(output.contains("Admin"));
        assertTrue(output.contains("Manager"));
        assertTrue(output.contains("Viewer"));
    }

    // ══════════════════════ role-create ══════════════════════

    @Test
    void roleCreate_shouldAddRole() {
        execute("role-create", "DevOps\nDevOps role\nнет\n");
        assertTrue(system.getRoleManager().exists("DevOps"));
    }

    @Test
    void roleCreate_withPermission_shouldAddPermission() {
        execute("role-create", "Tester\nTesting role\nда\nREAD\nreports\nCan read reports\nнет\n");
        assertTrue(system.getRoleManager().exists("Tester"));
        Role tester = system.getRoleManager().findByName("Tester").orElseThrow();
        assertTrue(tester.hasPermission("READ", "reports"));
    }

    // ══════════════════════ role-view ══════════════════════

    @Test
    void roleView_existingRole_shouldShowDetails() {
        String output = execute("role-view", "Admin\n");
        assertTrue(output.contains("Admin"));
        assertTrue(output.contains("READ"));
    }

    @Test
    void roleView_unknownRole_shouldPrintError() {
        String output = execute("role-view", "GHOST\n");
        assertTrue(output.contains("✗"));
    }

    // ══════════════════════ role-update ══════════════════════

    @Test
    void roleUpdate_shouldChangeDescription() {
        execute("role-update", "Viewer\n\nUpdated viewer description\n");
        Role viewer = system.getRoleManager().findByName("Viewer").orElseThrow();
        assertEquals("Updated viewer description", viewer.getDescription());
    }

    @Test
    void roleUpdate_shouldRenamRole() {
        execute("role-update", "Viewer\nReadOnly\n\n");
        assertFalse(system.getRoleManager().exists("Viewer"));
        assertTrue(system.getRoleManager().exists("ReadOnly"));
    }

    // ══════════════════════ role-delete ══════════════════════

    @Test
    void roleDelete_withoutAssignments_shouldRemove() {
        execute("role-create", "Temp\nTemp role\nнет\n");
        execute("role-delete", "Temp\n");
        assertFalse(system.getRoleManager().exists("Temp"));
    }

    @Test
    void roleDelete_withActiveAssignments_requiresConfirmation() {
        // Роль Admin назначена — должно запросить подтверждение
        String output = execute("role-delete", "Admin\nнет\n");
        assertTrue(output.contains("⚠"));
        assertTrue(system.getRoleManager().exists("Admin")); // не удалена
    }

    // ══════════════════════ role-add-permission / remove ══════════════════════

    @Test
    void roleAddPermission_shouldAddPermission() {
        execute("role-add-permission", "Viewer\nEXPORT\nreports\nCan export reports\n");
        Role viewer = system.getRoleManager().findByName("Viewer").orElseThrow();
        assertTrue(viewer.hasPermission("EXPORT", "reports"));
    }

    @Test
    void roleRemovePermission_shouldRemovePermission() {
        // Viewer имеет READ на users
        execute("role-remove-permission", "Viewer\n1\n");
        Role viewer = system.getRoleManager().findByName("Viewer").orElseThrow();
        // Одно из прав должно быть удалено
        assertTrue(viewer.getPermissions().size() < 3);
    }

    // ══════════════════════ assign-role / revoke-role ══════════════════════

    @Test
    void assignRole_permanent_shouldCreateAssignment() {
        execute("user-create", "employee\nEmployee\nemployee@x.com\n");
        // Выбираем роль Viewer (нужно знать её порядковый номер после сортировки)
        List<Role> sorted = system.getRoleManager().findAll(r -> true, RoleSorters.byName());
        int viewerIdx = -1;
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getName().equals("Viewer")) { viewerIdx = i + 1; break; }
        }
        execute("assign-role", "employee\n" + viewerIdx + "\n1\n\n");
        User emp = system.getUserManager().findByUsername("employee").orElseThrow();
        Role viewer = system.getRoleManager().findByName("Viewer").orElseThrow();
        assertTrue(system.getAssignmentManager().userHasRole(emp, viewer));
    }

    @Test
    void assignRole_unknownUser_shouldPrintError() {
        String output = execute("assign-role", "ghost\n");
        assertTrue(output.contains("✗"));
    }

    @Test
    void revokeRole_shouldDeactivateAssignment() {
        // admin имеет роль Admin
        String output = execute("revoke-role", "admin\n1\n");
        assertTrue(output.contains("✓"));
        User admin = system.getUserManager().findByUsername("admin").orElseThrow();
        Role adminRole = system.getRoleManager().findByName("Admin").orElseThrow();
        assertFalse(system.getAssignmentManager().userHasRole(admin, adminRole));
    }

    // ══════════════════════ assignment-list ══════════════════════

    @Test
    void assignmentList_shouldShowAssignments() {
        String output = execute("assignment-list", "");
        assertTrue(output.contains("admin"));
        assertTrue(output.contains("PERMANENT"));
    }

    // ══════════════════════ assignment-active / expired ══════════════════════

    @Test
    void assignmentActive_shouldShowActiveOnly() {
        String output = execute("assignment-active", "");
        assertTrue(output.contains("admin"));
    }

    @Test
    void assignmentExpired_noExpired_shouldSayNoAssignments() {
        String output = execute("assignment-expired", "");
        assertTrue(output.contains("Нет истёкших"));
    }

    // ══════════════════════ assignment-extend ══════════════════════

    @Test
    void assignmentExtend_temporaryAssignment_shouldUpdateDate() {
        User admin = system.getUserManager().findByUsername("admin").orElseThrow();
        Role viewer = system.getRoleManager().findByName("Viewer").orElseThrow();
        TemporaryAssignment temp = new TemporaryAssignment(
                admin, viewer, AssignmentMetadata.now("system", null),
                "2099-01-01 00:00:00", false
        );
        system.getAssignmentManager().add(temp);
        execute("assignment-extend", temp.assignmentId() + "\n2099-12-31 00:00:00\n");
        assertEquals("2099-12-31 00:00:00", temp.getExpiresAt());
    }

    @Test
    void assignmentExtend_unknownId_shouldPrintError() {
        String output = execute("assignment-extend", "fake-id\n2099-01-01 00:00:00\n");
        assertTrue(output.contains("✗"));
    }

    // ══════════════════════ permissions-user ══════════════════════

    @Test
    void permissionsUser_shouldShowPermissions() {
        String output = execute("permissions-user", "admin\n");
        assertTrue(output.contains("users"));
        assertTrue(output.contains("READ"));
    }

    @Test
    void permissionsUser_unknownUser_shouldPrintError() {
        String output = execute("permissions-user", "ghost\n");
        assertTrue(output.contains("✗"));
    }

    // ══════════════════════ permissions-check ══════════════════════

    @Test
    void permissionsCheck_existingPermission_shouldConfirm() {
        String output = execute("permissions-check", "admin\nREAD\nusers\n");
        assertTrue(output.contains("✓"));
        assertTrue(output.contains("Admin")); // из какой роли
    }

    @Test
    void permissionsCheck_missingPermission_shouldDeny() {
        execute("user-create", "viewer_user\nViewer User\nviewer@x.com\n");
        User vUser = system.getUserManager().findByUsername("viewer_user").orElseThrow();
        Role viewer = system.getRoleManager().findByName("Viewer").orElseThrow();
        system.getAssignmentManager().add(
                new PermanentAssignment(vUser, viewer, AssignmentMetadata.now("admin", null))
        );
        String output = execute("permissions-check", "viewer_user\nDELETE\nusers\n");
        assertTrue(output.contains("✗"));
    }

    // ══════════════════════ stats ══════════════════════

    @Test
    void stats_shouldContainStatistics() {
        String output = execute("stats", "");
        assertTrue(output.contains("СТАТИСТИКА"));
    }

    // ══════════════════════ help ══════════════════════

    @Test
    void help_shouldListAllCommands() {
        String output = execute("help", "");
        assertTrue(output.contains("user-create"));
        assertTrue(output.contains("role-list"));
        assertTrue(output.contains("assign-role"));
    }
}
