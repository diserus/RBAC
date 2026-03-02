public interface RoleAssignment {
    String assignmentId();
    User user();
    Role role();
    AssignmentMetadata metadata();
    boolean isActive();
    String assignmentType();
}
