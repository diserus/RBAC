import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class UserManagerTest {
    private UserManager manager;
    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        manager = new UserManager();
        alice = User.create("alice", "Alice Smith", "alice@example.com");
        bob   = User.create("bob123", "Bob Jones", "bob@company.com");
        manager.add(alice);
        manager.add(bob);
    }

    @Test
    void add_shouldIncreaseCount() {
        assertEquals(2, manager.count());
    }

    @Test
    void add_duplicateUsername_shouldThrow() {
        User duplicate = User.create("alice", "Alice Other", "other@example.com");
        assertThrows(IllegalStateException.class, () -> manager.add(duplicate));
    }

    @Test
    void add_null_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> manager.add(null));
    }

    @Test
    void remove_existingUser_shouldReturnTrue() {
        assertTrue(manager.remove(alice));
        assertEquals(1, manager.count());
    }

    @Test
    void remove_nonExistingUser_shouldReturnFalse() {
        User stranger = User.create("stranger", "Unknown", "x@x.com");
        assertFalse(manager.remove(stranger));
    }

    @Test
    void remove_null_shouldReturnFalse() {
        assertFalse(manager.remove(null));
    }

    @Test
    void findById_existingUsername_shouldReturnUser() {
        Optional<User> found = manager.findById("alice");
        assertTrue(found.isPresent());
        assertEquals(alice, found.get());
    }

    @Test
    void findById_unknownId_shouldReturnEmpty() {
        assertTrue(manager.findById("ghost").isEmpty());
    }

    @Test
    void findByUsername_shouldReturnCorrectUser() {
        assertEquals(Optional.of(bob), manager.findByUsername("bob123"));
    }

    @Test
    void findByEmail_shouldReturnCorrectUser() {
        assertEquals(Optional.of(alice), manager.findByEmail("alice@example.com"));
    }

    @Test
    void findByEmail_unknown_shouldReturnEmpty() {
        assertTrue(manager.findByEmail("nobody@x.com").isEmpty());
    }

    @Test
    void exists_knownUsername_shouldReturnTrue() {
        assertTrue(manager.exists("alice"));
    }

    @Test
    void exists_unknownUsername_shouldReturnFalse() {
        assertFalse(manager.exists("ghost"));
    }

    @Test
    void findAll_shouldReturnAllUsers() {
        List<User> all = manager.findAll();
        assertEquals(2, all.size());
        assertTrue(new HashSet<>(all).containsAll(List.of(alice, bob)));
    }

    @Test
    void findByFilter_byEmail_shouldReturnMatchingUsers() {
        List<User> result = manager.findByFilter(
                UserFilters.byEmailDomain("@company.com")
        );
        assertEquals(1, result.size());
        assertEquals(bob, result.getFirst());
    }

    @Test
    void findByFilter_noMatch_shouldReturnEmptyList() {
        List<User> result = manager.findByFilter(
                UserFilters.byEmail("nobody@x.com")
        );
        assertTrue(result.isEmpty());
    }

    @Test
    void findAll_withFilterAndSorter_shouldReturnSortedFilteredList() {
        User charlie = User.create("charlie", "Charlie Brown", "charlie@company.com");
        manager.add(charlie);

        List<User> result = manager.findAll(
                UserFilters.byEmailDomain("@company.com"),
                UserSorters.byUsername()
        );

        assertEquals(2, result.size());
        assertEquals("bob123",  result.get(0).username());
        assertEquals("charlie", result.get(1).username());
    }

    @Test
    void update_shouldChangeFullNameAndEmail() {
        manager.update("alice", "Alice Updated", "new@example.com");
        User updated = manager.findByUsername("alice").orElseThrow();
        assertEquals("Alice Updated", updated.fullName());
        assertEquals("new@example.com", updated.email());
    }

    @Test
    void update_nonExistingUser_shouldThrow() {
        assertThrows(NoSuchElementException.class, () ->
                manager.update("ghost", "Name", "email@x.com")
        );
    }

    @Test
    void update_invalidEmail_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () ->
                manager.update("alice", "Alice", "not-an-email")
        );
    }

    @Test
    void count_afterClear_shouldBeZero() {
        manager.clear();
        assertEquals(0, manager.count());
    }

    @Test
    void clear_findAll_shouldReturnEmptyList() {
        manager.clear();
        assertTrue(manager.findAll().isEmpty());
    }
}
