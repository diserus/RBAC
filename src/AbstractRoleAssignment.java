import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public abstract class AbstractRoleAssignment  implements RoleAssignment{
    private final String assignmentId;
    private final User user;
    private final Role role;
    private final AssignmentMetadata metadata;
    public AbstractRoleAssignment(User user , Role role , AssignmentMetadata metadata){
        if (user == null) throw new IllegalArgumentException("user не может быть null");
        if (role == null) throw new IllegalArgumentException("role не может быть null");
        if (metadata == null) throw new IllegalArgumentException("metadata не может быть null");

        this.assignmentId = "assignment_" +  UUID.randomUUID();
        this.user = user;
        this.role = role;
        this.metadata = metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractRoleAssignment roleAssignment = (AbstractRoleAssignment) o;
        return Objects.equals(assignmentId, roleAssignment.assignmentId);
    }
    @Override
    public int hashCode(){
        return Objects.hash(assignmentId);
    }

    @Override
    public abstract boolean isActive();

    @Override
    public abstract String assignmentType();

    @Override public String assignmentId() { return assignmentId; }
    @Override public User user() { return user; }
    @Override public Role role() { return role; }
    @Override public AssignmentMetadata metadata() { return metadata; }
    public String summary(){
        return """
                [%s] %s assigned to %s by %s at %s
                Reason: %s
                Status: %s
                """.formatted(assignmentType(),role.getName(),user.username(),
                metadata.assignedBy(),metadata.assignedAt(),metadata.reason(),isActive() ? "ACTIVE" : "INACTIVE");
    }

}
