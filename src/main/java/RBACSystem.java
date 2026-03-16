import java.util.*;
import java.util.stream.Collectors;

public class RBACSystem {
    private final UserManager       userManager;
    private final RoleManager       roleManager;
    private final AssignmentManager assignmentManager;
    private final AuditLog          auditLog;
    private final ReportGenerator   reportGenerator;
    private String currentUser = "system";

    public RBACSystem() {
        userManager       = new UserManager();
        roleManager       = new RoleManager();
        assignmentManager = new AssignmentManager();
        auditLog          = new AuditLog();
        reportGenerator   = new ReportGenerator();

        roleManager.setCanRemoveCheck(role ->
                assignmentManager.findByRole(role).stream().noneMatch(RoleAssignment::isActive)
        );
    }

    public UserManager       getUserManager()       { return userManager; }
    public RoleManager       getRoleManager()       { return roleManager; }
    public AssignmentManager getAssignmentManager() { return assignmentManager; }
    public AuditLog          getAuditLog()          { return auditLog; }
    public ReportGenerator   getReportGenerator()   { return reportGenerator; }
    public String            getCurrentUser()        { return currentUser; }
    public void              setCurrentUser(String u) { this.currentUser = u; }

    public void initialize() {
        Permission readUsers    = new Permission("READ",   "users",   "Чтение пользователей");
        Permission writeUsers   = new Permission("WRITE",  "users",   "Изменение пользователей");
        Permission deleteUsers  = new Permission("DELETE", "users",   "Удаление пользователей");
        Permission readRoles    = new Permission("READ",   "roles",   "Чтение ролей");
        Permission writeRoles   = new Permission("WRITE",  "roles",   "Изменение ролей");
        Permission deleteRoles  = new Permission("DELETE", "roles",   "Удаление ролей");
        Permission readReports  = new Permission("READ",   "reports", "Просмотр отчётов");
        Permission writeReports = new Permission("WRITE",  "reports", "Создание отчётов");

        Role adminRole = new Role("Admin", "Полный доступ к системе");
        adminRole.addPermission(readUsers);   adminRole.addPermission(writeUsers);
        adminRole.addPermission(deleteUsers); adminRole.addPermission(readRoles);
        adminRole.addPermission(writeRoles);  adminRole.addPermission(deleteRoles);
        adminRole.addPermission(readReports); adminRole.addPermission(writeReports);

        Role managerRole = new Role("Manager", "Управленческий доступ");
        managerRole.addPermission(readUsers);   managerRole.addPermission(writeUsers);
        managerRole.addPermission(readRoles);   managerRole.addPermission(readReports);
        managerRole.addPermission(writeReports);

        Role viewerRole = new Role("Viewer", "Доступ только для чтения");
        viewerRole.addPermission(readUsers);
        viewerRole.addPermission(readRoles);
        viewerRole.addPermission(readReports);

        roleManager.add(adminRole);
        roleManager.add(managerRole);
        roleManager.add(viewerRole);

        User admin = User.create("admin", "System Administrator", "admin@system.com");
        userManager.add(admin);
        currentUser = "admin";
        assignmentManager.add(new PermanentAssignment(
                admin, adminRole, AssignmentMetadata.now("system", "Initial setup")
        ));

        auditLog.log("SYSTEM_INIT", "system", "RBACSystem", "Система инициализирована");
    }

    public String generateStatistics() {
        int totalUsers  = userManager.count();
        int totalRoles  = roleManager.count();
        int total       = assignmentManager.count();
        int active      = assignmentManager.getActiveAssignments().size();
        int expired     = assignmentManager.getExpiredAssignments().size();
        double avg      = totalUsers == 0 ? 0.0 : (double) active / totalUsers;

        Map<String, Long> roleCounts = assignmentManager.getActiveAssignments().stream()
                .collect(Collectors.groupingBy(a -> a.role().getName(), Collectors.counting()));
        String top3 = roleCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .map(e -> "  - " + e.getKey() + ": " + e.getValue() + " назначений")
                .collect(Collectors.joining("\n"));

        return """
                ╔══════════════════════════════════════╗
                ║         СТАТИСТИКА СИСТЕМЫ           ║
                ╠══════════════════════════════════════╣
                ║ Пользователей:       %-15d ║
                ║ Ролей:               %-15d ║
                ║ Назначений всего:    %-15d ║
                ║ Активных:            %-15d ║
                ║ Истёкших:            %-15d ║
                ║ Среднее ролей/user:  %-15.2f ║
                ╠══════════════════════════════════════╣
                ║ Топ-3 ролей:                         ║
                %s
                ╚══════════════════════════════════════╝
                """.formatted(totalUsers, totalRoles, total, active, expired, avg, top3);
    }
}