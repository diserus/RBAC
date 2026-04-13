import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ReportGenerator {

    public String generateUserReport(UserManager userManager,
                                     AssignmentManager assignmentManager) {
        List<User> users = userManager.findAll(u -> true, UserSorters.byUsername());

        String[] headers = {"Username", "Full Name", "Email", "Active Roles"};
        List<String[]> rows = new ArrayList<>();

        for (User u : users) {
            String roles = assignmentManager.findByUser(u).stream()
                    .filter(RoleAssignment::isActive)
                    .map(a -> a.role().getName())
                    .collect(Collectors.joining(", "));
            rows.add(new String[]{
                    u.username(),
                    u.fullName(),
                    u.email(),
                    roles.isEmpty() ? "—" : roles
            });
        }

        StringBuilder sb = new StringBuilder();
        sb.append(FormatUtils.formatHeader("ОТЧЁТ ПО ПОЛЬЗОВАТЕЛЯМ"));
        sb.append('\n');
        sb.append("Дата генерации: ").append(DateUtils.getCurrentDateTime()).append('\n');
        sb.append("Всего пользователей: ").append(users.size()).append('\n');
        sb.append('\n');
        sb.append(FormatUtils.formatTable(headers, rows));
        return sb.toString();
    }

    public String generateRoleReport(RoleManager roleManager,
                                     AssignmentManager assignmentManager) {
        List<Role> roles = roleManager.findAll(r -> true, RoleSorters.byName());

        String[] headers = {"Role Name", "Description", "Permissions", "Active Users"};
        List<String[]> rows = new ArrayList<>();

        for (Role r : roles) {
            long activeUsers = assignmentManager.findByRole(r).stream()
                    .filter(RoleAssignment::isActive)
                    .count();
            rows.add(new String[]{
                    r.getName(),
                    FormatUtils.truncate(r.getDescription(), 30),
                    String.valueOf(r.getPermissions().size()),
                    String.valueOf(activeUsers)
            });
        }

        StringBuilder sb = new StringBuilder();
        sb.append(FormatUtils.formatHeader("ОТЧЁТ ПО РОЛЯМ"));
        sb.append('\n');
        sb.append("Дата генерации: ").append(DateUtils.getCurrentDateTime()).append('\n');
        sb.append("Всего ролей: ").append(roles.size()).append('\n');
        sb.append('\n');
        sb.append(FormatUtils.formatTable(headers, rows));
        return sb.toString();
    }

    public String generatePermissionMatrix(UserManager userManager,
                                           AssignmentManager assignmentManager) {
        List<User> users = userManager.findAll(u -> true, UserSorters.byUsername());

        List<String> resources = assignmentManager.getActiveAssignments().stream()
                .filter(RoleAssignment::isActive)
                .flatMap(a -> a.role().getPermissions().stream())
                .map(Permission::resource)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        if (resources.isEmpty()) {
            return FormatUtils.formatHeader("МАТРИЦА ПРАВ") + "\nНет данных.";
        }

        String[] headers = new String[resources.size() + 1];
        headers[0] = "User";
        for (int i = 0; i < resources.size(); i++) headers[i + 1] = resources.get(i);

        List<String[]> rows = new ArrayList<>();
        for (User u : users) {
            Set<Permission> perms = assignmentManager.getUserPermissions(u);
            String[] row = new String[headers.length];
            row[0] = u.username();
            for (int i = 0; i < resources.size(); i++) {
                String resource = resources.get(i);
                String permNames = perms.stream()
                        .filter(p -> p.resource().equals(resource))
                        .map(Permission::name)
                        .sorted()
                        .collect(Collectors.joining("/"));
                row[i + 1] = permNames.isEmpty() ? "—" : permNames;
            }
            rows.add(row);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(FormatUtils.formatHeader("МАТРИЦА ПРАВ (пользователи × ресурсы)"));
        sb.append('\n');
        sb.append("Дата генерации: ").append(DateUtils.getCurrentDateTime()).append('\n');
        sb.append('\n');
        sb.append(FormatUtils.formatTable(headers, rows));
        return sb.toString();
    }

    public String generateUserReportParallel(UserManager userManager,
                                             AssignmentManager assignmentManager) {
        List<User> users = userManager.findAll(u -> true, UserSorters.byUsername());

        String[] headers = {"Username", "Full Name", "Email", "Active Roles"};
        List<String[]> rows = users.parallelStream()
                .map(user -> {
                    String roles = assignmentManager.findByUser(user).stream()
                            .filter(RoleAssignment::isActive)
                            .map(a -> a.role().getName())
                            .sorted()
                            .collect(Collectors.joining(", "));
                    return new String[]{
                            user.username(),
                            user.fullName(),
                            user.email(),
                            roles.isEmpty() ? "—" : roles
                    };
                })
                .sorted((left, right) -> left[0].compareTo(right[0]))
                .collect(Collectors.toCollection(ArrayList::new));

        StringBuilder sb = new StringBuilder();
        sb.append(FormatUtils.formatHeader("ОТЧЁТ ПО ПОЛЬЗОВАТЕЛЯМ"));
        sb.append('\n');
        sb.append("Дата генерации: ").append(DateUtils.getCurrentDateTime()).append('\n');
        sb.append("Всего пользователей: ").append(users.size()).append('\n');
        sb.append("Режим генерации: parallel").append('\n');
        sb.append('\n');
        sb.append(FormatUtils.formatTable(headers, rows));
        return sb.toString();
    }

    public String generatePermissionMatrixParallel(UserManager userManager,
                                                   AssignmentManager assignmentManager) {
        List<User> users = userManager.findAll(u -> true, UserSorters.byUsername());

        List<String> resources = assignmentManager.getActiveAssignments().parallelStream()
                .flatMap(a -> a.role().getPermissions().stream())
                .map(Permission::resource)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        if (resources.isEmpty()) {
            return FormatUtils.formatHeader("МАТРИЦА ПРАВ") + "\nНет данных.";
        }

        String[] headers = new String[resources.size() + 1];
        headers[0] = "User";
        for (int i = 0; i < resources.size(); i++) {
            headers[i + 1] = resources.get(i);
        }

        List<String[]> rows = users.parallelStream()
                .map(user -> {
                    Set<Permission> perms = assignmentManager.getUserPermissions(user);
                    String[] row = new String[headers.length];
                    row[0] = user.username();
                    for (int i = 0; i < resources.size(); i++) {
                        String resource = resources.get(i);
                        String permNames = perms.stream()
                                .filter(p -> p.resource().equals(resource))
                                .map(Permission::name)
                                .sorted()
                                .collect(Collectors.joining("/"));
                        row[i + 1] = permNames.isEmpty() ? "—" : permNames;
                    }
                    return row;
                })
                .sorted((left, right) -> left[0].compareTo(right[0]))
                .collect(Collectors.toCollection(ArrayList::new));

        StringBuilder sb = new StringBuilder();
        sb.append(FormatUtils.formatHeader("МАТРИЦА ПРАВ (пользователи × ресурсы)"));
        sb.append('\n');
        sb.append("Дата генерации: ").append(DateUtils.getCurrentDateTime()).append('\n');
        sb.append("Режим генерации: parallel").append('\n');
        sb.append('\n');
        sb.append(FormatUtils.formatTable(headers, rows));
        return sb.toString();
    }

    public void exportToFile(String report, String filename) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename, false))) {
            pw.println(report);
        } catch (IOException e) {
            System.out.println("✗ Ошибка сохранения отчёта: " + e.getMessage());
        }
    }
}
