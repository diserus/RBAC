import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

public class CommandRegistry {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void registerAll(CommandParser parser) {

        // ══════════════════════ ПОЛЬЗОВАТЕЛИ ══════════════════════

        parser.registerCommand("user-list", "Список всех пользователей",
                (scanner, system) -> {
                    List<User> users = system.getUserManager().findAll(
                            u -> true, UserSorters.byUsername()
                    );
                    if (users.isEmpty()) { System.out.println("Пользователи отсутствуют."); return; }
                    System.out.printf("%-20s %-25s %-30s%n", "Username", "Full Name", "Email");
                    System.out.println("─".repeat(75));
                    users.forEach(u ->
                            System.out.printf("%-20s %-25s %-30s%n",
                                    u.username(), u.fullName(), u.email())
                    );
                });

        parser.registerCommand("user-create", "Создать пользователя",
                (scanner, system) -> {
                    System.out.print("Username: ");       String username = scanner.nextLine().trim();
                    System.out.print("Полное имя: ");     String fullName = scanner.nextLine().trim();
                    System.out.print("Email: ");          String email    = scanner.nextLine().trim();
                    try {
                        User user = User.create(username, fullName, email);
                        system.getUserManager().add(user);
                        System.out.println("✓ Пользователь '" + username + "' создан.");
                    } catch (Exception e) {
                        System.out.println("✗ Ошибка: " + e.getMessage());
                    }
                });

        parser.registerCommand("user-view", "Просмотр пользователя",
                (scanner, system) -> {
                    System.out.print("Username: ");
                    String username = scanner.nextLine().trim();
                    Optional<User> opt = system.getUserManager().findByUsername(username);
                    if (opt.isEmpty()) { System.out.println("✗ Пользователь не найден."); return; }
                    User user = opt.get();
                    System.out.println("\n" + user.format());
                    List<RoleAssignment> assignments = system.getAssignmentManager().findByUser(user);
                    if (assignments.isEmpty()) {
                        System.out.println("Роли не назначены.");
                    } else {
                        System.out.println("\nНазначенные роли:");
                        assignments.forEach(a -> System.out.printf(
                                "  [%s] %s (%s)%n",
                                a.isActive() ? "ACTIVE" : "INACTIVE",
                                a.role().getName(), a.assignmentType()
                        ));
                        Set<Permission> perms = system.getAssignmentManager().getUserPermissions(user);
                        System.out.println("\nВсе права (" + perms.size() + "):");
                        perms.stream()
                                .sorted(Comparator.comparing(Permission::name))
                                .forEach(p -> System.out.println("  - " + p.format()));
                    }
                });

        parser.registerCommand("user-update", "Обновить данные пользователя",
                (scanner, system) -> {
                    System.out.print("Username: ");
                    String username = scanner.nextLine().trim();
                    if (!system.getUserManager().exists(username)) {
                        System.out.println("✗ Пользователь не найден."); return;
                    }
                    System.out.print("Новое полное имя: ");  String fullName = scanner.nextLine().trim();
                    System.out.print("Новый email: ");        String email    = scanner.nextLine().trim();
                    try {
                        system.getUserManager().update(username, fullName, email);
                        System.out.println("✓ Данные обновлены.");
                    } catch (Exception e) {
                        System.out.println("✗ Ошибка: " + e.getMessage());
                    }
                });

        parser.registerCommand("user-delete", "Удалить пользователя",
                (scanner, system) -> {
                    System.out.print("Username: ");
                    String username = scanner.nextLine().trim();
                    Optional<User> opt = system.getUserManager().findByUsername(username);
                    if (opt.isEmpty()) { System.out.println("✗ Пользователь не найден."); return; }
                    System.out.print("Подтвердите удаление (да/нет): ");
                    if (!scanner.nextLine().trim().equalsIgnoreCase("да")) {
                        System.out.println("Отменено."); return;
                    }
                    User user = opt.get();
                    system.getAssignmentManager().findByUser(user)
                            .forEach(a -> system.getAssignmentManager().remove(a));
                    system.getUserManager().remove(user);
                    System.out.println("✓ Пользователь '" + username + "' удалён.");
                });

        parser.registerCommand("user-search", "Поиск пользователей",
                (scanner, system) -> {
                    System.out.println("Фильтр: 1-username  2-email  3-домен  4-полное имя");
                    System.out.print("Выбор: ");
                    String choice = scanner.nextLine().trim();
                    System.out.print("Строка поиска: ");
                    String query = scanner.nextLine().trim();
                    UserFilter filter = switch (choice) {
                        case "1" -> UserFilters.byUsernameContains(query);
                        case "2" -> UserFilters.byEmail(query);
                        case "3" -> UserFilters.byEmailDomain(query);
                        case "4" -> UserFilters.byFullNameContains(query);
                        default  -> u -> true;
                    };
                    List<User> result = system.getUserManager().findAll(filter, UserSorters.byUsername());
                    if (result.isEmpty()) { System.out.println("Ничего не найдено."); return; }
                    System.out.printf("%-20s %-25s %-30s%n", "Username", "Full Name", "Email");
                    System.out.println("─".repeat(75));
                    result.forEach(u ->
                            System.out.printf("%-20s %-25s %-30s%n", u.username(), u.fullName(), u.email())
                    );
                });

        // ══════════════════════ РОЛИ ══════════════════════

        parser.registerCommand("role-list", "Список всех ролей",
                (scanner, system) -> {
                    List<Role> roles = system.getRoleManager().findAll(
                            r -> true, RoleSorters.byName()
                    );
                    if (roles.isEmpty()) { System.out.println("Роли отсутствуют."); return; }
                    System.out.printf("%-20s %-10s %-36s%n", "Название", "Права", "ID");
                    System.out.println("─".repeat(66));
                    roles.forEach(r -> System.out.printf(
                            "%-20s %-10d %-36s%n", r.getName(), r.getPermissions().size(), r.getId())
                    );
                });

        parser.registerCommand("role-create", "Создать роль",
                (scanner, system) -> {
                    System.out.print("Название роли: ");   String name = scanner.nextLine().trim();
                    System.out.print("Описание: ");        String desc = scanner.nextLine().trim();
                    try {
                        Role role = new Role(name, desc);
                        system.getRoleManager().add(role);
                        System.out.println("✓ Роль создана. Добавить права? (да/нет)");
                        while (scanner.nextLine().trim().equalsIgnoreCase("да")) {
                            System.out.print("Permission name: ");    String pName = scanner.nextLine().trim();
                            System.out.print("Resource: ");           String res   = scanner.nextLine().trim();
                            System.out.print("Description: ");        String pDesc = scanner.nextLine().trim();
                            try {
                                role.addPermission(new Permission(pName, res, pDesc));
                                System.out.println("✓ Право добавлено. Добавить ещё? (да/нет)");
                            } catch (Exception e) {
                                System.out.println("✗ " + e.getMessage() + ". Добавить ещё? (да/нет)");
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("✗ Ошибка: " + e.getMessage());
                    }
                });

        parser.registerCommand("role-view", "Просмотр роли",
                (scanner, system) -> {
                    System.out.print("Имя роли: ");
                    String name = scanner.nextLine().trim();
                    system.getRoleManager().findByName(name).ifPresentOrElse(
                            r -> System.out.println("\n" + r.format()),
                            () -> System.out.println("✗ Роль не найдена.")
                    );
                });

        parser.registerCommand("role-update", "Обновить роль",
                (scanner, system) -> {
                    System.out.print("Текущее имя роли: ");
                    String oldName = scanner.nextLine().trim();
                    if (!system.getRoleManager().exists(oldName)) {
                        System.out.println("✗ Роль не найдена."); return;
                    }
                    System.out.print("Новое название (Enter — без изменений): ");
                    String newName = scanner.nextLine().trim();
                    System.out.print("Новое описание (Enter — без изменений): ");
                    String newDesc = scanner.nextLine().trim();
                    try {
                        Role role = system.getRoleManager().findByName(oldName).get();
                        if (!newDesc.isEmpty()) role.setDescription(newDesc);
                        if (!newName.isEmpty()) system.getRoleManager().renameRole(oldName, newName);
                        System.out.println("✓ Роль обновлена.");
                    } catch (Exception e) {
                        System.out.println("✗ Ошибка: " + e.getMessage());
                    }
                });

        parser.registerCommand("role-delete", "Удалить роль",
                (scanner, system) -> {
                    System.out.print("Имя роли: ");
                    String name = scanner.nextLine().trim();
                    Optional<Role> opt = system.getRoleManager().findByName(name);
                    if (opt.isEmpty()) { System.out.println("✗ Роль не найдена."); return; }
                    Role role = opt.get();
                    List<RoleAssignment> active = system.getAssignmentManager().findByRole(role)
                            .stream().filter(RoleAssignment::isActive).toList();
                    if (!active.isEmpty()) {
                        System.out.println("⚠ Роль назначена пользователям:");
                        active.forEach(a -> System.out.println("  - " + a.user().username()));
                        System.out.print("Всё равно удалить? (да/нет): ");
                        if (!scanner.nextLine().trim().equalsIgnoreCase("да")) {
                            System.out.println("Отменено."); return;
                        }
                        active.forEach(a -> system.getAssignmentManager().remove(a));
                    }
                    system.getRoleManager().remove(role);
                    System.out.println("✓ Роль удалена.");
                });

        parser.registerCommand("role-add-permission", "Добавить право к роли",
                (scanner, system) -> {
                    System.out.print("Имя роли: ");       String roleName = scanner.nextLine().trim();
                    System.out.print("Permission name: "); String pName   = scanner.nextLine().trim();
                    System.out.print("Resource: ");        String res     = scanner.nextLine().trim();
                    System.out.print("Description: ");     String pDesc   = scanner.nextLine().trim();
                    try {
                        system.getRoleManager().addPermissionToRole(roleName, new Permission(pName, res, pDesc));
                        System.out.println("✓ Право добавлено.");
                    } catch (Exception e) {
                        System.out.println("✗ Ошибка: " + e.getMessage());
                    }
                });

        parser.registerCommand("role-remove-permission", "Удалить право из роли",
                (scanner, system) -> {
                    System.out.print("Имя роли: ");
                    String roleName = scanner.nextLine().trim();
                    Optional<Role> opt = system.getRoleManager().findByName(roleName);
                    if (opt.isEmpty()) { System.out.println("✗ Роль не найдена."); return; }
                    List<Permission> perms = new ArrayList<>(opt.get().getPermissions());
                    if (perms.isEmpty()) { System.out.println("У роли нет прав."); return; }
                    for (int i = 0; i < perms.size(); i++)
                        System.out.printf("  %d. %s%n", i + 1, perms.get(i).format());
                    System.out.print("Номер для удаления: ");
                    try {
                        int idx = Integer.parseInt(scanner.nextLine().trim()) - 1;
                        if (idx < 0 || idx >= perms.size()) { System.out.println("✗ Неверный номер."); return; }
                        system.getRoleManager().removePermissionFromRole(roleName, perms.get(idx));
                        System.out.println("✓ Право удалено.");
                    } catch (NumberFormatException e) {
                        System.out.println("✗ Введите число.");
                    }
                });

        parser.registerCommand("role-search", "Поиск ролей",
                (scanner, system) -> {
                    System.out.println("Фильтр: 1-имя  2-наличие права  3-мин.кол-во прав");
                    System.out.print("Выбор: ");
                    String choice = scanner.nextLine().trim();
                    RoleFilter filter = switch (choice) {
                        case "1" -> {
                            System.out.print("Подстрока: ");
                            yield RoleFilters.byNameContains(scanner.nextLine().trim());
                        }
                        case "2" -> {
                            System.out.print("Permission name: "); String pn = scanner.nextLine().trim();
                            System.out.print("Resource: ");        String rs = scanner.nextLine().trim();
                            yield RoleFilters.hasPermission(pn, rs);
                        }
                        case "3" -> {
                            System.out.print("Минимум прав: ");
                            yield RoleFilters.hasAtLeastNPermissions(
                                    Integer.parseInt(scanner.nextLine().trim()));
                        }
                        default -> r -> true;
                    };
                    List<Role> result = system.getRoleManager().findAll(filter, RoleSorters.byName());
                    if (result.isEmpty()) { System.out.println("Ничего не найдено."); return; }
                    result.forEach(r -> System.out.printf("%-20s (%d прав)%n",
                            r.getName(), r.getPermissions().size()));
                });

        // ══════════════════════ НАЗНАЧЕНИЯ ══════════════════════

        parser.registerCommand("assign-role", "Назначить роль пользователю",
                (scanner, system) -> {
                    System.out.print("Username: ");
                    String username = scanner.nextLine().trim();
                    Optional<User> userOpt = system.getUserManager().findByUsername(username);
                    if (userOpt.isEmpty()) { System.out.println("✗ Пользователь не найден."); return; }

                    List<Role> roles = system.getRoleManager().findAll(r -> true, RoleSorters.byName());
                    if (roles.isEmpty()) { System.out.println("✗ Роли отсутствуют."); return; }
                    for (int i = 0; i < roles.size(); i++)
                        System.out.printf("  %d. %s%n", i + 1, roles.get(i).getName());
                    System.out.print("Выберите роль (номер): ");
                    int idx;
                    try {
                        idx = Integer.parseInt(scanner.nextLine().trim()) - 1;
                        if (idx < 0 || idx >= roles.size()) { System.out.println("✗ Неверный номер."); return; }
                    } catch (NumberFormatException e) { System.out.println("✗ Введите число."); return; }

                    Role role = roles.get(idx);
                    System.out.print("Тип: 1-постоянное  2-временное: ");
                    String type   = scanner.nextLine().trim();
                    System.out.print("Причина: ");
                    String reason = scanner.nextLine().trim();

                    try {
                        AssignmentMetadata meta = AssignmentMetadata.now(
                                system.getCurrentUser(), reason.isEmpty() ? null : reason
                        );
                        RoleAssignment assignment;
                        if (type.equals("2")) {
                            System.out.print("Дата истечения (yyyy-MM-dd HH:mm:ss): ");
                            String expires = scanner.nextLine().trim();
                            assignment = new TemporaryAssignment(userOpt.get(), role, meta, expires, false);
                        } else {
                            assignment = new PermanentAssignment(userOpt.get(), role, meta);
                        }
                        system.getAssignmentManager().add(assignment);
                        System.out.println("✓ Роль назначена.");
                    } catch (Exception e) {
                        System.out.println("✗ Ошибка: " + e.getMessage());
                    }
                });

        parser.registerCommand("revoke-role", "Отозвать роль у пользователя",
                (scanner, system) -> {
                    System.out.print("Username: ");
                    String username = scanner.nextLine().trim();
                    Optional<User> userOpt = system.getUserManager().findByUsername(username);
                    if (userOpt.isEmpty()) { System.out.println("✗ Пользователь не найден."); return; }

                    List<RoleAssignment> active = system.getAssignmentManager()
                            .findByUser(userOpt.get()).stream()
                            .filter(RoleAssignment::isActive).toList();
                    if (active.isEmpty()) { System.out.println("Нет активных назначений."); return; }

                    for (int i = 0; i < active.size(); i++)
                        System.out.printf("  %d. %s [%s]%n",
                                i + 1, active.get(i).role().getName(), active.get(i).assignmentType());
                    System.out.print("Номер для отзыва: ");
                    try {
                        int idx = Integer.parseInt(scanner.nextLine().trim()) - 1;
                        if (idx < 0 || idx >= active.size()) { System.out.println("✗ Неверный номер."); return; }
                        system.getAssignmentManager().revokeAssignment(active.get(idx).assignmentId());
                        System.out.println("✓ Роль отозвана.");
                    } catch (Exception e) {
                        System.out.println("✗ Ошибка: " + e.getMessage());
                    }
                });

        parser.registerCommand("assignment-list", "Список всех назначений",
                (scanner, system) -> {
                    List<RoleAssignment> all = system.getAssignmentManager()
                            .findAll(a -> true, AssignmentSorters.byUsername());
                    if (all.isEmpty()) { System.out.println("Назначения отсутствуют."); return; }
                    System.out.printf("%-15s %-15s %-11s %-10s %-16s%n",
                            "User", "Role", "Type", "Status", "Assigned at");
                    System.out.println("─".repeat(67));
                    all.forEach(a -> System.out.printf("%-15s %-15s %-11s %-10s %-16s%n",
                            a.user().username(), a.role().getName(),
                            a.assignmentType(), a.isActive() ? "ACTIVE" : "INACTIVE",
                            a.metadata().assignedAt()));
                });

        parser.registerCommand("assignment-list-user", "Назначения пользователя",
                (scanner, system) -> {
                    System.out.print("Username: ");
                    String username = scanner.nextLine().trim();
                    Optional<User> opt = system.getUserManager().findByUsername(username);
                    if (opt.isEmpty()) { System.out.println("✗ Пользователь не найден."); return; }
                    List<RoleAssignment> list = system.getAssignmentManager().findByUser(opt.get());
                    if (list.isEmpty()) { System.out.println("Назначения отсутствуют."); return; }
                    list.forEach(a -> System.out.println(
                            a instanceof AbstractRoleAssignment ara ? ara.summary() : a.toString()
                    ));
                });

        parser.registerCommand("assignment-list-role", "Пользователи с ролью",
                (scanner, system) -> {
                    System.out.print("Имя роли: ");
                    String roleName = scanner.nextLine().trim();
                    Optional<Role> opt = system.getRoleManager().findByName(roleName);
                    if (opt.isEmpty()) { System.out.println("✗ Роль не найдена."); return; }
                    List<RoleAssignment> list = system.getAssignmentManager().findByRole(opt.get());
                    if (list.isEmpty()) { System.out.println("Никому не назначена."); return; }
                    System.out.printf("%-20s %-11s %-10s%n", "Username", "Type", "Status");
                    System.out.println("─".repeat(41));
                    list.forEach(a -> System.out.printf("%-20s %-11s %-10s%n",
                            a.user().username(), a.assignmentType(),
                            a.isActive() ? "ACTIVE" : "INACTIVE"));
                });

        parser.registerCommand("assignment-active", "Активные назначения",
                (scanner, system) -> {
                    List<RoleAssignment> list = system.getAssignmentManager().getActiveAssignments();
                    if (list.isEmpty()) { System.out.println("Нет активных назначений."); return; }
                    System.out.printf("%-15s %-15s %-11s %-16s%n", "User", "Role", "Type", "Assigned at");
                    System.out.println("─".repeat(57));
                    list.forEach(a -> System.out.printf("%-15s %-15s %-11s %-16s%n",
                            a.user().username(), a.role().getName(),
                            a.assignmentType(), a.metadata().assignedAt()));
                });

        parser.registerCommand("assignment-expired", "Истёкшие назначения",
                (scanner, system) -> {
                    List<RoleAssignment> list = system.getAssignmentManager().getExpiredAssignments();
                    if (list.isEmpty()) { System.out.println("Нет истёкших назначений."); return; }
                    list.forEach(a -> System.out.printf("%-15s %-15s %-11s%n",
                            a.user().username(), a.role().getName(), a.assignmentType()));
                });

        parser.registerCommand("assignment-extend", "Продлить временное назначение",
                (scanner, system) -> {
                    System.out.print("Assignment ID: ");
                    String id = scanner.nextLine().trim();
                    System.out.print("Новая дата истечения (yyyy-MM-dd HH:mm:ss): ");
                    String newDate = scanner.nextLine().trim();
                    try {
                        system.getAssignmentManager().extendTemporaryAssignment(id, newDate);
                        System.out.println("✓ Назначение продлено.");
                    } catch (Exception e) {
                        System.out.println("✗ Ошибка: " + e.getMessage());
                    }
                });

        parser.registerCommand("assignment-search", "Поиск назначений",
                (scanner, system) -> {
                    System.out.println("Фильтр: 1-user  2-role  3-type  4-status  5-после даты  6-до даты истечения");
                    System.out.print("Выбор: ");
                    String choice = scanner.nextLine().trim();
                    AssignmentFilter filter = switch (choice) {
                        case "1" -> { System.out.print("Username: ");
                            yield AssignmentFilters.byUsername(scanner.nextLine().trim()); }
                        case "2" -> { System.out.print("Имя роли: ");
                            yield AssignmentFilters.byRoleName(scanner.nextLine().trim()); }
                        case "3" -> { System.out.print("Тип (PERMANENT/TEMPORARY): ");
                            yield AssignmentFilters.byType(scanner.nextLine().trim()); }
                        case "4" -> { System.out.print("Статус (1-активные / 2-неактивные): ");
                            String s = scanner.nextLine().trim();
                            yield s.equals("1") ? AssignmentFilters.activeOnly() : AssignmentFilters.inactiveOnly(); }
                        case "5" -> { System.out.print("Дата (yyyy-MM-dd): ");
                            yield AssignmentFilters.assignedAfter(scanner.nextLine().trim()); }
                        case "6" -> { System.out.print("Дата (yyyy-MM-dd): ");
                            yield AssignmentFilters.expiringBefore(scanner.nextLine().trim()); }
                        default -> a -> true;
                    };
                    List<RoleAssignment> result = system.getAssignmentManager()
                            .findAll(filter, AssignmentSorters.byUsername());
                    if (result.isEmpty()) { System.out.println("Ничего не найдено."); return; }
                    System.out.printf("%-15s %-15s %-11s %-10s%n", "User", "Role", "Type", "Status");
                    System.out.println("─".repeat(51));
                    result.forEach(a -> System.out.printf("%-15s %-15s %-11s %-10s%n",
                            a.user().username(), a.role().getName(),
                            a.assignmentType(), a.isActive() ? "ACTIVE" : "INACTIVE"));
                });

        // ══════════════════════ ПРАВА ══════════════════════

        parser.registerCommand("permissions-user", "Права пользователя",
                (scanner, system) -> {
                    System.out.print("Username: ");
                    String username = scanner.nextLine().trim();
                    Optional<User> opt = system.getUserManager().findByUsername(username);
                    if (opt.isEmpty()) { System.out.println("✗ Пользователь не найден."); return; }
                    Set<Permission> perms = system.getAssignmentManager().getUserPermissions(opt.get());
                    if (perms.isEmpty()) { System.out.println("Прав нет."); return; }
                    Map<String, List<Permission>> byResource = perms.stream()
                            .collect(Collectors.groupingBy(Permission::resource));
                    byResource.entrySet().stream()
                            .sorted(Map.Entry.comparingByKey())
                            .forEach(e -> {
                                System.out.println("\nРесурс: " + e.getKey());
                                e.getValue().forEach(p -> System.out.println("  - " + p.format()));
                            });
                });

        parser.registerCommand("permissions-check", "Проверить право пользователя",
                (scanner, system) -> {
                    System.out.print("Username: ");          String username = scanner.nextLine().trim();
                    System.out.print("Permission name: ");   String pName    = scanner.nextLine().trim();
                    System.out.print("Resource: ");          String resource = scanner.nextLine().trim();
                    Optional<User> opt = system.getUserManager().findByUsername(username);
                    if (opt.isEmpty()) { System.out.println("✗ Пользователь не найден."); return; }
                    boolean has = system.getAssignmentManager()
                            .userHasPermission(opt.get(), pName, resource);
                    System.out.println(has ? "✓ Право есть." : "✗ Права нет.");
                    if (has) {
                        system.getAssignmentManager().findByUser(opt.get()).stream()
                                .filter(a -> a.isActive() && a.role().hasPermission(pName, resource))
                                .forEach(a -> System.out.println("  Источник: роль '" + a.role().getName() + "'"));
                    }
                });

        // ══════════════════════ СЛУЖЕБНЫЕ ══════════════════════

        parser.registerCommand("help", "Справка по командам",
                (scanner, system) -> parser.printHelp());

        parser.registerCommand("stats", "Статистика системы",
                (scanner, system) -> System.out.println(system.generateStatistics()));

        parser.registerCommand("clear", "Очистить экран",
                (scanner, system) -> {
                    for (int i = 0; i < 40; i++) System.out.println();
                });

        parser.registerCommand("exit", "Выход из программы",
                (scanner, system) -> {
                    System.out.print("Выйти из программы? (да/нет): ");
                    if (scanner.nextLine().trim().equalsIgnoreCase("да")) {
                        System.out.println("До свидания!");
                        System.exit(0);
                    }
                });
    }
}
