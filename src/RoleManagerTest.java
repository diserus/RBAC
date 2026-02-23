import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RoleManagerTest {
    private RoleManager manager;
    private Role admin;
    private Role viewer;
    private Permission readPerm;
    private Permission writePerm;

    @BeforeEach
    void setUp() {
        manager   = new RoleManager();
        admin     = new Role("ADMIN",  "Administrator role");
        viewer    = new Role("VIEWER", "Read-only role");
        readPerm  = new Permission("READ",  "documents", "Can read documents");
        writePerm = new Permission("WRITE", "documents", "Can write documents");

        admin.addPermission(readPerm);
        admin.addPermission(writePerm);
        viewer.addPermission(readPerm);

        manager.add(admin);
        manager.add(viewer);
    }

    @Test
    void add_shouldIncreaseCount() {
        assertEquals(2, manager.count());
    }

    @Test
    void add_duplicateName_shouldThrow() {
        Role duplicate = new Role("ADMIN", "Another admin");
        assertThrows(IllegalStateException.class, () -> manager.add(duplicate));
    }

    @Test
    void add_null_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> manager.add(null));
    }

    @Test
    void remove_existingRole_shouldReturnTrue() {
        assertTrue(manager.remove(viewer));
        assertEquals(1, manager.count());
    }

    @Test
    void remove_nonExistingRole_shouldReturnFalse() {
        Role ghost = new Role("GHOST", "Ghost role");
        assertFalse(manager.remove(ghost));
    }

    @Test
    void remove_roleWithActiveAssignments_shouldThrow() {
        manager.setCanRemoveCheck(role -> !role.getName().equals("ADMIN"));
        assertThrows(IllegalStateException.class, () -> manager.remove(admin));
    }

    @Test
    void findById_shouldReturnRole() {
        Optional<Role> found = manager.findById(admin.getId());
        assertTrue(found.isPresent());
        assertEquals(admin, found.get());
    }

    @Test
    void findByName_shouldReturnRole() {
        assertEquals(Optional.of(viewer), manager.findByName("VIEWER"));
    }

    @Test
    void findByName_unknown_shouldReturnEmpty() {
        assertTrue(manager.findByName("UNKNOWN").isEmpty());
    }

    @Test
    void exists_knownName_shouldReturnTrue() {
        assertTrue(manager.exists("ADMIN"));
    }

    @Test
    void exists_unknownName_shouldReturnFalse() {
        assertFalse(manager.exists("GHOST"));
    }

    @Test
    void findByFilter_hasPermission_shouldReturnMatchingRoles() {
        List<Role> result = manager.findByFilter(
                RoleFilters.hasPermission(writePerm)
        );
        assertEquals(1, result.size());
        assertEquals(admin, result.getFirst());
    }

    @Test
    void findByFilter_hasAtLeastNPermissions_shouldWork() {
        List<Role> result = manager.findByFilter(
                RoleFilters.hasAtLeastNPermissions(2)
        );
        assertEquals(1, result.size());
        assertEquals(admin, result.getFirst());
    }

    @Test
    void findAll_withFilterAndSorter_shouldReturnSortedList() {
        Role editor = new Role("EDITOR", "Can edit");
        editor.addPermission(readPerm);
        manager.add(editor);

        List<Role> result = manager.findAll(
                RoleFilters.hasPermission(readPerm),
                RoleSorters.byName()
        );
        assertEquals(3, result.size());
        assertEquals("ADMIN",  result.get(0).getName());
        assertEquals("EDITOR", result.get(1).getName());
        assertEquals("VIEWER", result.get(2).getName());
    }

    @Test
    void addPermissionToRole_shouldAddPermission() {
        Permission deletePerm = new Permission("DELETE", "documents", "Can delete");
        manager.addPermissionToRole("VIEWER", deletePerm);
        assertTrue(viewer.hasPermission(deletePerm));
    }

    @Test
    void addPermissionToRole_unknownRole_shouldThrow() {
        assertThrows(NoSuchElementException.class, () ->
                manager.addPermissionToRole("GHOST", readPerm)
        );
    }

    @Test
    void removePermissionFromRole_shouldRemovePermission() {
        manager.removePermissionFromRole("VIEWER", readPerm);
        assertFalse(viewer.hasPermission(readPerm));
    }

    @Test
    void findRolesWithPermission_shouldReturnRolesContainingPermission() {
        List<Role> result = manager.findRolesWithPermission("READ", "documents");
        assertEquals(2, result.size());
        assertTrue(result.containsAll(List.of(admin, viewer)));
    }

    @Test
    void clear_shouldRemoveAll() {
        manager.clear();
        assertEquals(0, manager.count());
        assertTrue(manager.findAll().isEmpty());
    }
}
