import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;

public class SystemSnapshot {

    public void save(RBACSystem system, String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename, false))) {
            writer.println("=== RBAC SYSTEM SNAPSHOT ===");
            writer.println("Generated at: " + DateUtils.getCurrentDateTime());
            writer.println();
            writeUsers(system, writer);
            writer.println();
            writeRoles(system, writer);
            writer.println();
            writeAssignments(system, writer);
        } catch (IOException e) {
            throw new IllegalStateException("Ошибка сохранения данных: " + e.getMessage(), e);
        }
    }

    private void writeUsers(RBACSystem system, PrintWriter writer) {
        writer.println("[USERS]");
        system.getUserManager().findAll(u -> true, UserSorters.byUsername())
                .forEach(user -> writer.printf("%s|%s|%s%n", user.username(), user.fullName(), user.email()));
    }

    private void writeRoles(RBACSystem system, PrintWriter writer) {
        writer.println("[ROLES]");
        system.getRoleManager().findAll(r -> true, RoleSorters.byName())
                .forEach(role -> {
                    String permissions = role.getPermissions().stream()
                            .map(permission -> permission.name() + "@" + permission.resource())
                            .sorted()
                            .collect(Collectors.joining(","));
                    writer.printf("%s|%s|%s%n", role.getName(), role.getDescription(), permissions);
                });
    }

    private void writeAssignments(RBACSystem system, PrintWriter writer) {
        writer.println("[ASSIGNMENTS]");
        List<RoleAssignment> assignments = system.getAssignmentManager()
                .findAll(a -> true, AssignmentSorters.byUsername());
        for (RoleAssignment assignment : assignments) {
            writer.printf("%s|%s|%s|%s%n",
                    assignment.user().username(),
                    assignment.role().getName(),
                    assignment.assignmentType(),
                    assignment.isActive());
        }
    }
}
