import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class TemporaryAssignment extends AbstractRoleAssignment {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String expiresAt;
    private boolean autoRenew;
    private boolean deactivated;

    public TemporaryAssignment(User user, Role role, AssignmentMetadata metadata,
                               String expiresAt, boolean autoRenew) {
        super(user, role, metadata);
        if (expiresAt == null || expiresAt.isBlank()) {
            throw new IllegalArgumentException("expiresAt не может быть null или empty");
        }
        try {
            LocalDateTime.parse(expiresAt, FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("expiresAt должен быть в формате yyyy-MM-dd HH:mm:ss");
        }
        this.expiresAt = expiresAt;
        this.autoRenew = autoRenew;
        this.deactivated = false;
    }

    @Override
    public boolean isActive() {
        return !deactivated && !isExpired();
    }

    @Override
    public String assignmentType() {
        return "TEMPORARY";
    }

    public boolean isExpired() {
        try {
            LocalDateTime expiration = LocalDateTime.parse(expiresAt, FORMATTER);
            LocalDateTime now = LocalDateTime.now();
            return now.isAfter(expiration);
        } catch (Exception e) {
            return true;
        }
    }

    public void extend(String newExpirationDate) {
        if (newExpirationDate == null || newExpirationDate.isBlank()) {
            throw new IllegalArgumentException("newExpirationDate не может быть null или empty");
        }
        try {
            LocalDateTime.parse(newExpirationDate, FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("newExpirationDate должен быть в формате yyyy-MM-dd HH:mm:ss");
        }
        this.expiresAt = newExpirationDate;
    }

    public String getTimeRemaining() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime exp = LocalDateTime.parse(expiresAt, FORMATTER);
        if (now.isAfter(exp)) return "EXPIRED";

        Duration d = Duration.between(now, exp);
        long totalMinutes = d.toMinutes();
        long days = totalMinutes / (60 * 24);
        long hours = (totalMinutes % (60 * 24)) / 60;
        long minutes = totalMinutes % 60;

        return "%dd %dh %dm".formatted(days, hours, minutes);
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public boolean isAutoRenew() {
        return autoRenew;
    }

    public void deactivate() {
        this.deactivated = true;
    }

    public boolean isDeactivated() {
        return deactivated;
    }

    @Override
    public String summary() {
        String baseSummary = super.summary();
        String expiryInfo = String.format("\nExpires at: %s\nTime remaining: %s\nAuto-renew: %s",
                expiresAt, getTimeRemaining(), autoRenew ? "Yes" : "No");
        return baseSummary + expiryInfo;
    }
}
