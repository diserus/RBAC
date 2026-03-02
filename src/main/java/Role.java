import java.util.*;
import java.util.stream.Collectors;

public class Role {
    private final String id;
    private final String name;
    private final String description;
    private final Set<Permission> permissions;

    public Role(String name, String description) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Название роли не может быть пустым.");
        }
        this.name = name;
        this.description = (description == null) ? "" : description;
        this.permissions = new HashSet<>();

        this.id = "role_" + UUID.randomUUID();
    }

    public void addPermission(Permission permission) {
        if (permission != null) {
            permissions.add(permission);
        }
    }

    public void removePermission(Permission permission) {
        permissions.remove(permission);
    }

    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }

    public boolean hasPermission(String permissionName, String resource) {
        try {
            Permission temp = new Permission(permissionName, resource, "temp");

            for (Permission p : permissions) {
                if (p.name().equals(temp.name()) && p.resource().equals(temp.resource())) {
                    return true;
                }
            }
        } catch (IllegalArgumentException e) {
            return false;
        }
        return false;
    }

    public Set<Permission> getPermissions() {
        return Set.copyOf(permissions);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return Objects.equals(id, role.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Role{id='" + id + "', name='" + name + "', permissionsCount=" + permissions.size() + "}";
    }

    public String format() {
        String permissionsList = permissions.stream()
                .map(p -> "- " + p.format())
                .collect(Collectors.joining("\n"));
        return """
                Role: %s [ID: %s]
                Description: %s
                Permissions (%d):
                %s
                """.formatted(name, id, description, permissions.size(), permissionsList).trim();
    }
    public String getName(){ return name;}
    public String getId() { return id; }
}
