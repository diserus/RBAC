public class PermanentAssignment extends AbstractRoleAssignment{
    private volatile boolean revoked;

    public PermanentAssignment(User user, Role role, AssignmentMetadata metadata) {
        super(user, role, metadata);
        this.revoked = false;
    }

    @Override
    public boolean isActive() {
        return !revoked;
    }

    @Override
    public String assignmentType() {
        return "PERMANENT";
    }

    public synchronized void revoke() {
        this.revoked = true;
    }

    public boolean isRevoked() {
        return revoked;
    }
}
