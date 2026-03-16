import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

public class CommandRegistry {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void registerAll(CommandParser parser) {

        parser.registerCommand("user-list", "Список всех пользователей",
                (scanner, system) -> {
                    List<User> users = system.getUserManager().findAll(
                            u -> true, UserSorters.byUsername());
                    if (users.isEmpty()) { System.out.println("Пользователи отсутствуют."); return; }

                    String[] headers = {"Username", "Full Name", "Email"};
                    List<String[]> rows = users.stream()
                            .map(u -> new String[]{u.username(), u.fullName(), u.email()})
                            .collect(Collectors.toList());
                    System.out.println(FormatUtils.formatTable(headers, rows));
                });

        parser.registerCommand("user-create", "Создать пользователя",
                (scanner, system) -> {
                    String username = ConsoleUtils.promptString(scanner, "Username:", true);
                    String fullName = ConsoleUtils.promptString(scanner, "Полное имя:", true);
                    String email    = ConsoleUtils.promptString(scanner, "Email:", true);

                    // Валидация через ValidationUtils
                    if (!ValidationUtils.isValidUsername(username)) {
                        System.out.println("✗ Некорректный username (только a-z, A-Z, 0-9, _, длина 3–20).");
                        return;
                    }
                    if (!ValidationUtils.isValidEmail(email)) {
                        System.out.println("✗ Некорректный email.");
                        return;
                    }
                    try {
                        User user = User.create(username, fullName, email);
                        system.getUserManager().add(user);
                        system.getAuditLog().log("USER_CREATE", system.getCurrentUser(),
                                username, "fullName=" + fullName + ", email=" + email);
                        System.out.println("✓ Пользователь '" + username + "' создан.");
                    } catch (Exception e) {
                        System.out.println("✗ Ошибка: " + e.getMessage());
                    }
                });

        parser.registerCommand("user-view", "Просмотр пользователя",
                (scanner, system) -> {
                    String username = ConsoleUtils.promptString(scanner, "Username:", true);
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
                        perms.stream().sorted(Comparator.comparing(Permission::name))
                                .forEach(p -> System.out.println("  - " + p.format()));
                    }
                });

        parser.registerCommand("user-update", "Обновить данные пользователя",
                (scanner, system) -> {
                    String username = ConsoleUtils.promptString(scanner, "Username:", true);
                    if (!system.getUserManager().exists(username)) {
                        System.out.println("✗ Пользователь не найден."); return;
                    }
                    String fullName = ConsoleUtils.promptString(scanner, "Новое полное имя:", true);
                    String email    = ConsoleUtils.promptString(scanner, "Новый email:", true);
                    if (!ValidationUtils.isValidEmail(email)) {
                        System.out.println("✗ Некорректный email."); return;
                    }
                    try {
                        system.getUserManager().update(username, fullName, email);
                        system.getAuditLog().log("USER_UPDATE", system.getCurrentUser(),
                                username, "fullName=" + fullName + ", email=" + email);
                        System.out.println("✓ Данные обновлены.");
                    } catch (Exception e) {
                        System.out.println("✗ Ошибка: " + e.getMessage());
                    }
                });

        parser.registerCommand("user-delete", "Удалить пользователя",
                (scanner, system) -> {
                    String username = ConsoleUtils.promptString(scanner, "Username:", true);
                    Optional<User> opt = system.getUserManager().findByUsername(username);
                    if (opt.isEmpty()) { System.out.println("✗ Пользователь не найден."); return; }
                    if (!ConsoleUtils.promptYesNo(scanner, "Подтвердите удаление")) {
                        System.out.println("Отменено."); return;
                    }
                    User user = opt.get();
                    system.getAssignmentManager().findByUser(user)
                            .forEach(a -> system.getAssignmentManager().remove(a));
                    system.getUserManager().remove(user);
                    system.getAuditLog().log("USER_DELETE", system.getCurrentUser(),
                            username, "Пользователь удалён со всеми назначениями");
                    System.out.println("✓ Пользователь '" + username + "' удалён.");
                });

        parser.registerCommand("user-search", "Поиск пользователей",
                (scanner, system) -> {
                    List<String> filterOptions = List.of("1 — username", "2 — email",
                            "3 — домен email", "4 — полное имя");
                    String choice = ConsoleUtils.promptChoice(scanner,
                            "Выберите фильтр:", filterOptions);
                    String query = ConsoleUtils.promptString(scanner, "Строка поиска:", true);

                    UserFilter filter = switch (choice.charAt(0)) {
                        case '1' -> UserFilters.byUsernameContains(query);
                        case '2' -> UserFilters.byEmail(query);
                        case '3' -> UserFilters.byEmailDomain(query);
                        case '4' -> UserFilters.byFullNameContains(query);
                        default  -> u -> true;
                    };
                    List<User> result = system.getUserManager().findAll(filter, UserSorters.byUsername());
                    if (result.isEmpty()) { System.out.println("Ничего не найдено."); return; }

                    String[] headers = {"Username", "Full Name", "Email"};
                    List<String[]> rows = result.stream()
                            .map(u -> new String[]{u.username(), u.fullName(), u.email()})
                            .collect(Collectors.toList());
                    System.out.println(FormatUtils.formatTable(headers, rows));
                });

        parser.registerCommand("role-list", "Список всех ролей",
                (scanner, system) -> {
                    List<Role> roles = system.getRoleManager().findAll(r -> true, RoleSorters.byName());
                    if (roles.isEmpty()) { System.out.println("Роли отсутствуют."); return; }
                    String[] headers = {"Название", "Прав", "ID"};
                    List<String[]> rows = roles.stream()
                            .map(r -> new String[]{
                                    r.getName(),
                                    String.valueOf(r.getPermissions().size()),
                                    r.getId()})
                            .collect(Collectors.toList());
                    System.out.println(FormatUtils.formatTable(headers, rows));
                });

        parser.registerCommand("role-create", "Создать роль",
                (scanner, system) -> {
                    String name = ConsoleUtils.promptString(scanner, "Название роли:", true);
                    String desc = ConsoleUtils.promptString(scanner, "Описание:", false);
                    try {
                        Role role = new Role(name, desc);
                        system.getRoleManager().add(role);
                        system.getAuditLog().log("ROLE_CREATE", system.getCurrentUser(),
                                name, "desc=" + desc);
                        System.out.println("✓ Роль создана. Добавить права? (да/нет)");
                        while (ConsoleUtils.promptYesNo(scanner, "Добавить право?")) {
                            String pName = ConsoleUtils.promptString(scanner, "Permission name:", true);
                            String res   = ConsoleUtils.promptString(scanner, "Resource:", true);
                            String pDesc = ConsoleUtils.promptString(scanner, "Description:", true);
                            try {
                                role.addPermission(new Permission(pName, res, pDesc));
                                System.out.println("✓ Право добавлено.");
                            } catch (Exception e) {
                                System.out.println("✗ " + e.getMessage());
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("✗ Ошибка: " + e.getMessage());
                    }
                });

        parser.registerCommand("role-view", "Просмотр роли",
                (scanner, system) -> {
                    String name = ConsoleUtils.promptString(scanner, "Имя роли:", true);
                    system.getRoleManager().findByName(name).ifPresentOrElse(
                            r -> System.out.println("\n" + r.format()),
                            () -> System.out.println("✗ Роль не найдена.")
                    );
                });

        parser.registerCommand("role-update", "Обновить роль",
                (scanner, system) -> {
                    String oldName = ConsoleUtils.promptString(scanner, "Текущее имя роли:", true);
                    if (!system.getRoleManager().exists(oldName)) {
                        System.out.println("✗ Роль не найдена."); return;
                    }
                    String newName = ConsoleUtils.promptString(scanner, "Новое название (Enter — без изменений):", false);
                    String newDesc = ConsoleUtils.promptString(scanner, "Новое описание (Enter — без изменений):", false);
                    try {
                        Role role = system.getRoleManager().findByName(oldName).get();
                        if (!newDesc.isEmpty()) role.setDescription(newDesc);
                        if (!newName.isEmpty()) system.getRoleManager().renameRole(oldName, newName);
                        system.getAuditLog().log("ROLE_UPDATE", system.getCurrentUser(),
                                oldName, "newName=" + newName + ", newDesc=" + newDesc);
                        System.out.println("✓ Роль обновлена.");
                    } catch (Exception e) {
                        System.out.println("✗ Ошибка: " + e.getMessage());
                    }
                });

        parser.registerCommand("role-delete", "Удалить роль",
                (scanner, system) -> {
                    String name = ConsoleUtils.promptString(scanner, "Имя роли:", true);
                    Optional<Role> opt = system.getRoleManager().findByName(name);
                    if (opt.isEmpty()) { System.out.println("✗ Роль не найдена."); return; }
                    Role role = opt.get();
                    List<RoleAssignment> active = system.getAssignmentManager().findByRole(role)
                            .stream().filter(RoleAssignment::isActive).toList();
                    if (!active.isEmpty()) {
                        System.out.println("⚠ Роль назначена пользователям:");
                        active.forEach(a -> System.out.println("  - " + a.user().username()));
                        if (!ConsoleUtils.promptYesNo(scanner, "Всё равно удалить?")) {
                            System.out.println("Отменено."); return;
                        }
                        active.forEach(a -> system.getAssignmentManager().remove(a));
                    }
                    system.getRoleManager().remove(role);
                    system.getAuditLog().log("ROLE_DELETE", system.getCurrentUser(),
                            name, "Удалено с " + active.size() + " активными назначениями");
                    System.out.println("✓ Роль удалена.");
                });

        parser.registerCommand("role-add-permission", "Добавить право к роли",
                (scanner, system) -> {
                    String roleName = ConsoleUtils.promptString(scanner, "Имя роли:", true);
                    String pName    = ConsoleUtils.promptString(scanner, "Permission name:", true);
                    String res      = ConsoleUtils.promptString(scanner, "Resource:", true);
                    String pDesc    = ConsoleUtils.promptString(scanner, "Description:", true);
                    try {
                        system.getRoleManager().addPermissionToRole(roleName, new Permission(pName, res, pDesc));
                        System.out.println("✓ Право добавлено.");
                    } catch (Exception e) {
                        System.out.println("✗ Ошибка: " + e.getMessage());
                    }
                });

        parser.registerCommand("role-remove-permission", "Удалить право из роли",
                (scanner, system) -> {
                    String roleName = ConsoleUtils.promptString(scanner, "Имя роли:", true);
                    Optional<Role> opt = system.getRoleManager().findByName(roleName);
                    if (opt.isEmpty()) { System.out.println("✗ Роль не найдена."); return; }
                    List<Permission> perms = new ArrayList<>(opt.get().getPermissions());
                    if (perms.isEmpty()) { System.out.println("У роли нет прав."); return; }
                    for (int i = 0; i < perms.size(); i++)
                        System.out.printf("  %d. %s%n", i + 1, perms.get(i).format());
                    int idx = ConsoleUtils.promptInt(scanner, "Номер для удаления", 1, perms.size()) - 1;
                    system.getRoleManager().removePermissionFromRole(roleName, perms.get(idx));
                    System.out.println("✓ Право удалено.");
                });
        parser.registerCommand("role-search", "Поиск ролей",
                (scanner, system) -> {
                    List<String> filterOpts = List.of("1 — имя", "2 — наличие права", "3 — мин.кол-во прав");
                    String choice = ConsoleUtils.promptChoice(scanner, "Выберите фильтр:", filterOpts);
                    RoleFilter filter = switch (choice.charAt(0)) {
                        case '1' -> {
                            String sub = ConsoleUtils.promptString(scanner, "Подстрока:", true);
                            yield RoleFilters.byNameContains(sub);
                        }
                        case '2' -> {
                            String pn = ConsoleUtils.promptString(scanner, "Permission name:", true);
                            String rs = ConsoleUtils.promptString(scanner, "Resource:", true);
                            yield RoleFilters.hasPermission(pn, rs);
                        }
                        case '3' -> {
                            int n = ConsoleUtils.promptInt(scanner, "Минимум прав", 0, 100);
                            yield RoleFilters.hasAtLeastNPermissions(n);
                        }
                        default -> r -> true;
                    };
                    List<Role> result = system.getRoleManager().findAll(filter, RoleSorters.byName());
                    if (result.isEmpty()) { System.out.println("Ничего не найдено."); return; }
                    String[] headers = {"Название", "Прав"};
                    List<String[]> rows = result.stream()
                            .map(r -> new String[]{r.getName(), String.valueOf(r.getPermissions().size())})
                            .collect(Collectors.toList());
                    System.out.println(FormatUtils.formatTable(headers, rows));
                });

        parser.registerCommand("assign-role", "Назначить роль пользователю",
                (scanner, system) -> {
                    String username = ConsoleUtils.promptString(scanner, "Username:", true);
                    Optional<User> userOpt = system.getUserManager().findByUsername(username);
                    if (userOpt.isEmpty()) { System.out.println("✗ Пользователь не найден."); return; }

                    List<Role> roles = system.getRoleManager().findAll(r -> true, RoleSorters.byName());
                    if (roles.isEmpty()) { System.out.println("✗ Роли отсутствуют."); return; }
                    for (int i = 0; i < roles.size(); i++)
                        System.out.printf("  %d. %s%n", i + 1, roles.get(i).getName());
                    int roleIdx = ConsoleUtils.promptInt(scanner, "Выберите роль (номер)", 1, roles.size()) - 1;
                    Role selectedRole = roles.get(roleIdx);

                    System.out.println("1 — постоянное  2 — временное");
                    String typeChoice = ConsoleUtils.promptString(scanner, "Тип:", true);
                    String reason = ConsoleUtils.promptString(scanner, "Причина (опционально):", false);

                    try {
                        AssignmentMetadata meta = AssignmentMetadata.now(
                                system.getCurrentUser(), reason.isEmpty() ? null : reason);
                        RoleAssignment assignment;
                        if (typeChoice.equals("2")) {
                            String expires = ConsoleUtils.promptString(scanner,
                                    "Дата истечения (yyyy-MM-dd HH:mm:ss):", true);
                            assignment = new TemporaryAssignment(userOpt.get(), selectedRole, meta, expires, false);
                        } else {
                            assignment = new PermanentAssignment(userOpt.get(), selectedRole, meta);
                        }
                        system.getAssignmentManager().add(assignment);
                        system.getAuditLog().log("ROLE_ASSIGN", system.getCurrentUser(),
                                username, "role=" + selectedRole.getName() + ", type=" + assignment.assignmentType());
                        System.out.println("✓ Роль назначена.");
                    } catch (Exception e) {
                        System.out.println("✗ Ошибка: " + e.getMessage());
                    }
                });
        parser.registerCommand("revoke-role", "Отозвать роль у пользователя",
                (scanner, system) -> {
                    String username = ConsoleUtils.promptString(scanner, "Username:", true);
                    Optional<User> userOpt = system.getUserManager().findByUsername(username);
                    if (userOpt.isEmpty()) { System.out.println("✗ Пользователь не найден."); return; }

                    List<RoleAssignment> active = system.getAssignmentManager()
                            .findByUser(userOpt.get()).stream()
                            .filter(RoleAssignment::isActive).toList();
                    if (active.isEmpty()) { System.out.println("Нет активных назначений."); return; }

                    for (int i = 0; i < active.size(); i++)
                        System.out.printf("  %d. %s [%s]%n",
                                i + 1, active.get(i).role().getName(), active.get(i).assignmentType());
                    int idx = ConsoleUtils.promptInt(scanner, "Номер для отзыва", 1, active.size()) - 1;
                    try {
                        String roleName = active.get(idx).role().getName();
                        system.getAssignmentManager().revokeAssignment(active.get(idx).assignmentId());
                        system.getAuditLog().log("ROLE_REVOKE", system.getCurrentUser(),
                                username, "role=" + roleName);
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
                    String[] headers = {"User", "Role", "Type", "Status", "Assigned at"};
                    List<String[]> rows = all.stream()
                            .map(a -> new String[]{
                                    a.user().username(), a.role().getName(),
                                    a.assignmentType(), a.isActive() ? "ACTIVE" : "INACTIVE",
                                    a.metadata().assignedAt()})
                            .collect(Collectors.toList());
                    System.out.println(FormatUtils.formatTable(headers, rows));
                });

        parser.registerCommand("assignment-list-user", "Назначения пользователя",
                (scanner, system) -> {
                    String username = ConsoleUtils.promptString(scanner, "Username:", true);
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
                    String roleName = ConsoleUtils.promptString(scanner, "Имя роли:", true);
                    Optional<Role> opt = system.getRoleManager().findByName(roleName);
                    if (opt.isEmpty()) { System.out.println("✗ Роль не найдена."); return; }
                    List<RoleAssignment> list = system.getAssignmentManager().findByRole(opt.get());
                    if (list.isEmpty()) { System.out.println("Никому не назначена."); return; }
                    String[] headers = {"Username", "Type", "Status"};
                    List<String[]> rows = list.stream()
                            .map(a -> new String[]{a.user().username(), a.assignmentType(),
                                    a.isActive() ? "ACTIVE" : "INACTIVE"})
                            .collect(Collectors.toList());
                    System.out.println(FormatUtils.formatTable(headers, rows));
                });

        parser.registerCommand("assignment-active", "Активные назначения",
                (scanner, system) -> {
                    List<RoleAssignment> list = system.getAssignmentManager().getActiveAssignments();
                    if (list.isEmpty()) { System.out.println("Нет активных назначений."); return; }
                    String[] headers = {"User", "Role", "Type", "Assigned at"};
                    List<String[]> rows = list.stream()
                            .map(a -> new String[]{a.user().username(), a.role().getName(),
                                    a.assignmentType(), a.metadata().assignedAt()})
                            .collect(Collectors.toList());
                    System.out.println(FormatUtils.formatTable(headers, rows));
                });

        parser.registerCommand("assignment-expired", "Истёкшие назначения",
                (scanner, system) -> {
                    List<RoleAssignment> list = system.getAssignmentManager().getExpiredAssignments();
                    if (list.isEmpty()) { System.out.println("Нет истёкших назначений."); return; }
                    String[] headers = {"User", "Role", "Type"};
                    List<String[]> rows = list.stream()
                            .map(a -> new String[]{a.user().username(), a.role().getName(), a.assignmentType()})
                            .collect(Collectors.toList());
                    System.out.println(FormatUtils.formatTable(headers, rows));
                });

        parser.registerCommand("assignment-extend", "Продлить временное назначение",
                (scanner, system) -> {
                    String id      = ConsoleUtils.promptString(scanner, "Assignment ID:", true);
                    String newDate = ConsoleUtils.promptString(scanner,
                            "Новая дата истечения (yyyy-MM-dd HH:mm:ss):", true);
                    try {
                        system.getAssignmentManager().extendTemporaryAssignment(id, newDate);
                        system.getAuditLog().log("ASSIGNMENT_EXTEND", system.getCurrentUser(),
                                id, "newExpiry=" + newDate);
                        System.out.println("✓ Назначение продлено.");
                    } catch (Exception e) {
                        System.out.println("✗ Ошибка: " + e.getMessage());
                    }
                });

        parser.registerCommand("assignment-search", "Поиск назначений",
                (scanner, system) -> {
                    List<String> filterOpts = List.of(
                            "1 — user", "2 — role", "3 — type",
                            "4 — status", "5 — после даты", "6 — до даты истечения");
                    String choice = ConsoleUtils.promptChoice(scanner, "Выберите фильтр:", filterOpts);
                    AssignmentFilter filter = switch (choice.charAt(0)) {
                        case '1' -> { String u = ConsoleUtils.promptString(scanner, "Username:", true);
                            yield AssignmentFilters.byUsername(u); }
                        case '2' -> { String r = ConsoleUtils.promptString(scanner, "Имя роли:", true);
                            yield AssignmentFilters.byRoleName(r); }
                        case '3' -> { String t = ConsoleUtils.promptString(scanner, "Тип (PERMANENT/TEMPORARY):", true);
                            yield AssignmentFilters.byType(t); }
                        case '4' -> {
                            List<String> statusOpts = List.of("1 — активные", "2 — неактивные");
                            String s = ConsoleUtils.promptChoice(scanner, "Статус:", statusOpts);
                            yield s.charAt(0) == '1' ? AssignmentFilters.activeOnly() : AssignmentFilters.inactiveOnly();
                        }
                        case '5' -> { String d = ConsoleUtils.promptString(scanner, "Дата (yyyy-MM-dd):", true);
                            yield AssignmentFilters.assignedAfter(d); }
                        case '6' -> { String d = ConsoleUtils.promptString(scanner, "Дата (yyyy-MM-dd):", true);
                            yield AssignmentFilters.expiringBefore(d); }
                        default -> a -> true;
                    };
                    List<RoleAssignment> result = system.getAssignmentManager()
                            .findAll(filter, AssignmentSorters.byUsername());
                    if (result.isEmpty()) { System.out.println("Ничего не найдено."); return; }
                    String[] headers = {"User", "Role", "Type", "Status"};
                    List<String[]> rows = result.stream()
                            .map(a -> new String[]{a.user().username(), a.role().getName(),
                                    a.assignmentType(), a.isActive() ? "ACTIVE" : "INACTIVE"})
                            .collect(Collectors.toList());
                    System.out.println(FormatUtils.formatTable(headers, rows));
                });

        parser.registerCommand("permissions-user", "Права пользователя",
                (scanner, system) -> {
                    String username = ConsoleUtils.promptString(scanner, "Username:", true);
                    Optional<User> opt = system.getUserManager().findByUsername(username);
                    if (opt.isEmpty()) { System.out.println("✗ Пользователь не найден."); return; }
                    Set<Permission> perms = system.getAssignmentManager().getUserPermissions(opt.get());
                    if (perms.isEmpty()) { System.out.println("Прав нет."); return; }
                    String[] headers = {"Resource", "Permission", "Description"};
                    List<String[]> rows = perms.stream()
                            .sorted(Comparator.comparing(Permission::resource)
                                    .thenComparing(Permission::name))
                            .map(p -> new String[]{p.resource(), p.name(), p.description()})
                            .collect(Collectors.toList());
                    System.out.println(FormatUtils.formatTable(headers, rows));
                });

        parser.registerCommand("permissions-check", "Проверить право пользователя",
                (scanner, system) -> {
                    String username = ConsoleUtils.promptString(scanner, "Username:", true);
                    String pName    = ConsoleUtils.promptString(scanner, "Permission name:", true);
                    String resource = ConsoleUtils.promptString(scanner, "Resource:", true);
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

        parser.registerCommand("audit-log", "Просмотр журнала аудита",
                (scanner, system) -> {
                    List<String> opts = List.of(
                            "1 — все записи",
                            "2 — по исполнителю",
                            "3 — по действию",
                            "4 — сохранить в файл");
                    String choice = ConsoleUtils.promptChoice(scanner, "Выберите действие:", opts);
                    switch (choice.charAt(0)) {
                        case '1' -> system.getAuditLog().printLog();
                        case '2' -> {
                            String performer = ConsoleUtils.promptString(scanner, "Исполнитель:", true);
                            List<AuditLog.AuditEntry> entries = system.getAuditLog().getByPerformer(performer);
                            if (entries.isEmpty()) System.out.println("Записей не найдено.");
                            else entries.forEach(System.out::println);
                        }
                        case '3' -> {
                            String action = ConsoleUtils.promptString(scanner, "Действие:", true);
                            List<AuditLog.AuditEntry> entries = system.getAuditLog().getByAction(action);
                            if (entries.isEmpty()) System.out.println("Записей не найдено.");
                            else entries.forEach(System.out::println);
                        }
                        case '4' -> {
                            String filename = ConsoleUtils.promptString(scanner, "Имя файла:", true);
                            system.getAuditLog().saveToFile(filename);
                            System.out.println("✓ Лог сохранён в " + filename);
                        }
                        default -> System.out.println("Неизвестный вариант.");
                    }
                });

        parser.registerCommand("report-users", "Отчёт по пользователям",
                (scanner, system) -> {
                    String report = system.getReportGenerator().generateUserReport(
                            system.getUserManager(), system.getAssignmentManager());
                    System.out.println(report);
                    if (ConsoleUtils.promptYesNo(scanner, "Сохранить в файл?")) {
                        String fname = ConsoleUtils.promptString(scanner, "Имя файла:", true);
                        system.getReportGenerator().exportToFile(report, fname);
                        System.out.println("✓ Отчёт сохранён в " + fname);
                        system.getAuditLog().log("REPORT_EXPORT", system.getCurrentUser(),
                                "report-users", "file=" + fname);
                    }
                });

        parser.registerCommand("report-roles", "Отчёт по ролям",
                (scanner, system) -> {
                    String report = system.getReportGenerator().generateRoleReport(
                            system.getRoleManager(), system.getAssignmentManager());
                    System.out.println(report);
                    if (ConsoleUtils.promptYesNo(scanner, "Сохранить в файл?")) {
                        String fname = ConsoleUtils.promptString(scanner, "Имя файла:", true);
                        system.getReportGenerator().exportToFile(report, fname);
                        System.out.println("✓ Отчёт сохранён в " + fname);
                        system.getAuditLog().log("REPORT_EXPORT", system.getCurrentUser(),
                                "report-roles", "file=" + fname);
                    }
                });

        parser.registerCommand("report-matrix", "Матрица прав",
                (scanner, system) -> {
                    String report = system.getReportGenerator().generatePermissionMatrix(
                            system.getUserManager(), system.getAssignmentManager());
                    System.out.println(report);
                    if (ConsoleUtils.promptYesNo(scanner, "Сохранить в файл?")) {
                        String fname = ConsoleUtils.promptString(scanner, "Имя файла:", true);
                        system.getReportGenerator().exportToFile(report, fname);
                        System.out.println("✓ Отчёт сохранён в " + fname);
                        system.getAuditLog().log("REPORT_EXPORT", system.getCurrentUser(),
                                "report-matrix", "file=" + fname);
                    }
                });

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
                    if (ConsoleUtils.promptYesNo(scanner, "Выйти из программы?")) {
                        System.out.println("До свидания!");
                        System.exit(0);
                    }
                });
    }
}