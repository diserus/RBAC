import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AssignmentManagerTest {
    private AssignmentManager manager;
    private User alice;
    private User bob;
    private Role admin;
    private Role viewer;
    private Permission readPerm;
    private PermanentAssignment aliceAdmin;
    private PermanentAssignment bobViewer;

    @BeforeEach
    void setUp() {
        manager   = new AssignmentManager();
        alice     = User.create("alice", "Alice Smith", "alice@example.com");
        bob       = User.create("bob123", "Bob Jones",  "bob@company.com");
        admin     = new Role("ADMIN",  "Admin role");
        viewer    = new Role("VIEWER", "Viewer role");
        readPerm  = new Permission("READ", "documents", "Can read");
        admin.addPermission(readPerm);
        admin.addPermission(new Permission("WRITE", "documents", "Can write"));
        viewer.addPermission(readPerm);

        aliceAdmin = new PermanentAssignment(alice, admin,
                AssignmentMetadata.now("system", "initial setup"));
        bobViewer  = new PermanentAssignment(bob, viewer,
                AssignmentMetadata.now("system", "initial setup"));

        manager.add(aliceAdmin);
        manager.add(bobViewer);
    }

    @Test
    void add_shouldIncreaseCount() {
        assertEquals(2, manager.count());
    }

    @Test
    void add_duplicateActiveAssignment_shouldThrow() {
        PermanentAssignment duplicate = new PermanentAssignment(alice, admin,
                AssignmentMetadata.now("admin", "duplicate"));
        assertThrows(IllegalStateException.class, () -> manager.add(duplicate));
    }

    @Test
    void add_sameRoleAfterRevoke_shouldSucceed() {
        aliceAdmin.revoke();
        PermanentAssignment reAssigned = new PermanentAssignment(alice, admin,
                AssignmentMetadata.now("admin", "re-assigned"));
        assertDoesNotThrow(() -> manager.add(reAssigned));
    }

    @Test
    void add_null_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> manager.add(null));
    }

    @Test
    void remove_existingAssignment_shouldReturnTrue() {
        assertTrue(manager.remove(aliceAdmin));
        assertEquals(1, manager.count());
    }

    @Test
    void remove_nonExisting_shouldReturnFalse() {
        PermanentAssignment ghost = new PermanentAssignment(alice, viewer,
                AssignmentMetadata.now("x", null));
        assertFalse(manager.remove(ghost));
    }

    @Test
    void findById_shouldReturnAssignment() {
        Optional<RoleAssignment> found = manager.findById(aliceAdmin.assignmentId());
        assertTrue(found.isPresent());
        assertEquals(aliceAdmin, found.get());
    }

    @Test
    void findById_unknown_shouldReturnEmpty() {
        assertTrue(manager.findById("fake-id").isEmpty());
    }

    @Test
    void findByUser_shouldReturnUsersAssignments() {
        List<RoleAssignment> result = manager.findByUser(alice);
        assertEquals(1, result.size());
        assertEquals(aliceAdmin, result.get(0));
    }

    @Test
    void findByRole_shouldReturnRoleAssignments() {
        List<RoleAssignment> result = manager.findByRole(admin);
        assertEquals(1, result.size());
        assertEquals(aliceAdmin, result.get(0));
    }

    @Test
    void getActiveAssignments_shouldReturnOnlyActive() {
        aliceAdmin.revoke();
        List<RoleAssignment> active = manager.getActiveAssignments();
        assertEquals(1, active.size());
        assertEquals(bobViewer, active.getFirst());
    }

    @Test
    void getExpiredAssignments_shouldReturnOnlyInactive() {
        aliceAdmin.revoke();
        List<RoleAssignment> inactive = manager.getExpiredAssignments();
        assertEquals(1, inactive.size());
        assertEquals(aliceAdmin, inactive.getFirst());
    }

    @Test
    void userHasRole_activeAssignment_shouldReturnTrue() {
        assertTrue(manager.userHasRole(alice, admin));
    }

    @Test
    void userHasRole_afterRevoke_shouldReturnFalse() {
        aliceAdmin.revoke();
        assertFalse(manager.userHasRole(alice, admin));
    }

    @Test
    void userHasRole_wrongRole_shouldReturnFalse() {
        assertFalse(manager.userHasRole(alice, viewer));
    }

    @Test
    void userHasPermission_shouldReturnTrueForGrantedPermission() {
        assertTrue(manager.userHasPermission(alice, "READ", "documents"));
    }

    @Test
    void userHasPermission_shouldReturnFalseForMissingPermission() {
        assertFalse(manager.userHasPermission(bob, "WRITE", "documents"));
    }

    @Test
    void getUserPermissions_shouldReturnAllPermissions() {
        Set<Permission> perms = manager.getUserPermissions(alice);
        assertEquals(2, perms.size());
    }

    @Test
    void getUserPermissions_revokedAssignment_shouldReturnEmpty() {
        aliceAdmin.revoke();
        Set<Permission> perms = manager.getUserPermissions(alice);
        assertTrue(perms.isEmpty());
    }

    @Test
    void revokeAssignment_permanent_shouldMakeInactive() {
        manager.revokeAssignment(aliceAdmin.assignmentId());
        assertFalse(aliceAdmin.isActive());
    }

    @Test
    void revokeAssignment_temporary_shouldRemoveFromStorage() {
        TemporaryAssignment temp = new TemporaryAssignment(alice, viewer,
                AssignmentMetadata.now("admin", null),
                "2099-01-01 00:00:00", false);
        manager.add(temp);
        manager.revokeAssignment(temp.assignmentId());
        assertTrue(manager.findById(temp.assignmentId()).isEmpty());
    }

    @Test
    void revokeAssignment_unknownId_shouldThrow() {
        assertThrows(NoSuchElementException.class, () ->
                manager.revokeAssignment("fake-id")
        );
    }

    @Test
    void extendTemporaryAssignment_shouldUpdateExpirationDate() {
        TemporaryAssignment temp = new TemporaryAssignment(alice, viewer,
                AssignmentMetadata.now("admin", null),
                "2099-01-01 00:00:00", false);
        manager.add(temp);
        manager.extendTemporaryAssignment(temp.assignmentId(), "2099-12-31 00:00:00");
        assertEquals("2099-12-31 00:00:00", temp.getExpiresAt());
    }

    @Test
    void extendTemporaryAssignment_onPermanent_shouldThrow() {
        assertThrows(IllegalStateException.class, () ->
                manager.extendTemporaryAssignment(aliceAdmin.assignmentId(), "2099-01-01 00:00:00")
        );
    }

    @Test
    void extendTemporaryAssignment_unknownId_shouldThrow() {
        assertThrows(NoSuchElementException.class, () ->
                manager.extendTemporaryAssignment("fake-id", "2099-01-01 00:00:00")
        );
    }

    @Test
    void findByFilter_activeOnly_shouldReturnOnlyActive() {
        aliceAdmin.revoke();
        List<RoleAssignment> result = manager.findByFilter(AssignmentFilters.activeOnly());
        assertEquals(1, result.size());
        assertEquals(bobViewer, result.get(0));
    }

    @Test
    void findByFilterParallel_activeOnly_shouldReturnOnlyActive() {
        aliceAdmin.revoke();
        List<RoleAssignment> result = manager.findByFilterParallel(AssignmentFilters.activeOnly());
        assertEquals(1, result.size());
        assertEquals(bobViewer, result.get(0));
    }

    @Test
    void findAll_withFilterAndSorter_shouldReturnSortedList() {
        List<RoleAssignment> result = manager.findAll(
                AssignmentFilters.activeOnly(),
                AssignmentSorters.byUsername()
        );
        assertEquals(2, result.size());
        assertEquals("alice",  result.get(0).user().username());
        assertEquals("bob123", result.get(1).user().username());
    }

    @Test
    void clear_shouldRemoveAll() {
        manager.clear();
        assertEquals(0, manager.count());
    }
}
