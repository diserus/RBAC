import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class RoleManager implements Repository<Role> {
    private final Map<String, Role> rolesById   = new HashMap<>();
    private final Map<String, Role> rolesByName = new HashMap<>();

    private Predicate<Role> canRemoveCheck = role -> true;

    public void setCanRemoveCheck(Predicate<Role> check) {
        this.canRemoveCheck = check;
    }

    @Override
    public void add(Role role) {
        if (role == null) throw new IllegalArgumentException("role не может быть null");
        if (rolesByName.containsKey(role.getName())) {
            throw new IllegalStateException("Роль '" + role.getName() + "' уже существует");
        }
        rolesById.put(role.getId(), role);
        rolesByName.put(role.getName(), role);
    }

    @Override
    public boolean remove(Role role) {
        if (role == null) return false;
        if (!rolesById.containsKey(role.getId())) return false;
        if (!canRemoveCheck.test(role)) {
            throw new IllegalStateException(
                    "Роль '" + role.getName() + "' назначена пользователям — удаление запрещено"
            );
        }
        rolesById.remove(role.getId());
        rolesByName.remove(role.getName());
        return true;
    }

    @Override
    public Optional<Role> findById(String id) {
        return Optional.ofNullable(rolesById.get(id));
    }

    @Override
    public List<Role> findAll() {
        return new ArrayList<>(rolesById.values());
    }

    @Override
    public int count() {
        return rolesById.size();
    }

    @Override
    public void clear() {
        rolesById.clear();
        rolesByName.clear();
    }

    public Optional<Role> findByName(String name) {
        return Optional.ofNullable(rolesByName.get(name));
    }

    public List<Role> findByFilter(RoleFilter filter) {
        return rolesById.values().stream()
                .filter(filter::test)
                .collect(Collectors.toList());
    }

    public List<Role> findAll(RoleFilter filter, Comparator<Role> sorter) {
        return rolesById.values().stream()
                .filter(filter::test)
                .sorted(sorter)
                .collect(Collectors.toList());
    }

    public boolean exists(String name) {
        return rolesByName.containsKey(name);
    }

    public void addPermissionToRole(String roleName, Permission permission) {
        Role role = rolesByName.get(roleName);
        if (role == null) throw new NoSuchElementException("Роль '" + roleName + "' не найдена");
        role.addPermission(permission);
    }

    public void removePermissionFromRole(String roleName, Permission permission) {
        Role role = rolesByName.get(roleName);
        if (role == null) throw new NoSuchElementException("Роль '" + roleName + "' не найдена");
        role.removePermission(permission);
    }

    public List<Role> findRolesWithPermission(String permissionName, String resource) {
        return rolesById.values().stream()
                .filter(role -> role.hasPermission(permissionName, resource))
                .collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoleManager that = (RoleManager) o;
        return Objects.equals(rolesById, that.rolesById);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rolesById);
    }
}
