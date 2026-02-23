import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class AssignmentFilters {

    private static LocalDateTime parseDate(String date) {
        DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd")
        };
        for (DateTimeFormatter fmt : formatters) {
            try {
                if (fmt.toString().contains("d") && !fmt.toString().contains("H")) {
                    return LocalDate.parse(date, fmt).atStartOfDay();
                }
                return LocalDateTime.parse(date, fmt);
            } catch (DateTimeParseException ignored) {}
        }
        try {
            return LocalDate.parse(date).atStartOfDay();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Не удалось разобрать дату: " + date);
        }
    }

    public static AssignmentFilter byUser(User user) {
        return assignment -> assignment.user().equals(user);
    }

    public static AssignmentFilter byUsername(String username) {
        return assignment -> assignment.user().username().equals(username);
    }

    public static AssignmentFilter byRole(Role role) {
        return assignment -> assignment.role().equals(role);
    }

    public static AssignmentFilter byRoleName(String roleName) {
        return assignment -> assignment.role().getName().equals(roleName);
    }

    public static AssignmentFilter activeOnly() {
        return RoleAssignment::isActive;
    }

    public static AssignmentFilter inactiveOnly() {
        return assignment -> !assignment.isActive();
    }

    public static AssignmentFilter byType(String type) {
        return assignment -> assignment.assignmentType().equalsIgnoreCase(type);
    }

    public static AssignmentFilter assignedBy(String username) {
        return assignment -> assignment.metadata().assignedBy().equals(username);
    }

    public static AssignmentFilter assignedAfter(String date) {
        LocalDateTime threshold = parseDate(date);
        return assignment -> {
            try {
                LocalDateTime assignedAt = parseDate(assignment.metadata().assignedAt());
                return assignedAt.isAfter(threshold);
            } catch (IllegalArgumentException e) {
                return false;
            }
        };
    }

    public static AssignmentFilter expiringBefore(String date) {
        LocalDateTime threshold = parseDate(date);
        return assignment -> {
            if (!(assignment instanceof TemporaryAssignment temp)) {
                return false;
            }
            try {
                LocalDateTime expiresAt = parseDate(temp.getExpiresAt());
                return expiresAt.isBefore(threshold);
            } catch (IllegalArgumentException e) {
                return false;
            }
        };
    }
}
