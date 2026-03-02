import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
public record AssignmentMetadata(String assignedBy, String assignedAt, String reason) {
    public AssignmentMetadata {
        if (assignedBy == null || assignedBy.isBlank()) {
            throw new IllegalArgumentException("assignedBy должно быть не null и не blank.");
        }
        if (assignedAt == null || assignedAt.isBlank()) {
            throw new IllegalArgumentException("assignedAt должно быть не null и не blank.");
        }

        assignedBy = assignedBy.trim();
        assignedAt = assignedAt.trim();

        if (reason != null) {
            reason = reason.trim();
            if (reason.isEmpty()) reason = null;
        }
    }

    public static AssignmentMetadata now(String assignedBy, String reason) {
        String at = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        return new AssignmentMetadata(assignedBy, at, reason);
    }

    public String format() {
        return (reason == null)
                ? String.format("Assigned by %s at %s", assignedBy, assignedAt)
                : String.format("Assigned by %s at %s: %s", assignedBy, assignedAt, reason);
    }
}
