import java.util.*;
import java.util.stream.Collectors;

public class AssignmentManager implements Repository<RoleAssignment> {
    private final Map<String, RoleAssignment> assignments = new HashMap<>();

    @Override
    public void add(RoleAssignment assignment) {
        if (assignment == null) throw new IllegalArgumentException("assignment не может быть null");

        boolean duplicate = assignments.values().stream()
                .anyMatch(a -> a.isActive()
                        && a.user().equals(assignment.user())
                        && a.role().equals(assignment.role()));
        if (duplicate) {
            throw new IllegalStateException(
                    "Пользователь '" + assignment.user().username() +
                            "' уже имеет активное назначение роли '" + assignment.role().getName() + "'"
            );
        }
        assignments.put(assignment.assignmentId(), assignment);
    }

    @Override
    public boolean remove(RoleAssignment assignment) {
        if (assignment == null) return false;
        return assignments.remove(assignment.assignmentId()) != null;
    }

    @Override
    public Optional<RoleAssignment> findById(String id) {
        return Optional.ofNullable(assignments.get(id));
    }

    @Override
    public List<RoleAssignment> findAll() {
        return new ArrayList<>(assignments.values());
    }

    @Override
    public int count() {
        return assignments.size();
    }

    @Override
    public void clear() {
        assignments.clear();
    }

    public List<RoleAssignment> findByUser(User user) {
        return assignments.values().stream()
                .filter(a -> a.user().equals(user))
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findByRole(Role role) {
        return assignments.values().stream()
                .filter(a -> a.role().equals(role))
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findByFilter(AssignmentFilter filter) {
        return assignments.values().stream()
                .filter(filter::test)
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findAll(AssignmentFilter filter, Comparator<RoleAssignment> sorter) {
        return assignments.values().stream()
                .filter(filter::test)
                .sorted(sorter)
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> getActiveAssignments() {
        return assignments.values().stream()
                .filter(RoleAssignment::isActive)
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> getExpiredAssignments() {
        return assignments.values().stream()
                .filter(a -> !a.isActive())
                .collect(Collectors.toList());
    }

    public boolean userHasRole(User user, Role role) {
        return assignments.values().stream()
                .anyMatch(a -> a.isActive()
                        && a.user().equals(user)
                        && a.role().equals(role));
    }

    public boolean userHasPermission(User user, String permissionName, String resource) {
        return assignments.values().stream()
                .filter(a -> a.isActive() && a.user().equals(user))
                .anyMatch(a -> a.role().hasPermission(permissionName, resource));
    }

    public Set<Permission> getUserPermissions(User user) {
        return assignments.values().stream()
                .filter(a -> a.isActive() && a.user().equals(user))
                .flatMap(a -> a.role().getPermissions().stream())
                .collect(Collectors.toSet());
    }

    public void revokeAssignment(String assignmentId) {
        RoleAssignment assignment = assignments.get(assignmentId);
        if (assignment == null) {
            throw new NoSuchElementException("Назначение '" + assignmentId + "' не найдено");
        }
        if (assignment instanceof PermanentAssignment perm) {
            perm.revoke();
        } else if (assignment instanceof TemporaryAssignment) {
            assignments.remove(assignmentId);
        }
    }

    public void extendTemporaryAssignment(String assignmentId, String newExpirationDate) {
        RoleAssignment assignment = assignments.get(assignmentId);
        if (assignment == null) {
            throw new NoSuchElementException("Назначение '" + assignmentId + "' не найдено");
        }
        if (!(assignment instanceof TemporaryAssignment temp)) {
            throw new IllegalStateException("Назначение '" + assignmentId + "' не является временным");
        }
        temp.extend(newExpirationDate);
    }

    public int deactivateExpiredTemporaryAssignments() {
        List<TemporaryAssignment> expiredAssignments = assignments.values().stream()
                .filter(TemporaryAssignment.class::isInstance)
                .map(TemporaryAssignment.class::cast)
                .filter(assignment -> !assignment.isDeactivated())
                .filter(TemporaryAssignment::isExpired)
                .collect(Collectors.toList());

        expiredAssignments.forEach(TemporaryAssignment::deactivate);
        return expiredAssignments.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssignmentManager that = (AssignmentManager) o;
        return Objects.equals(assignments, that.assignments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assignments);
    }
}
