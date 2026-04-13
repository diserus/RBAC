import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RBACLoadTest {
    private final RBACSystem system = new RBACSystem();

    RBACLoadTest() {
        system.initialize();
    }

    @AfterEach
    void tearDown() {
        system.shutdown();
    }

    @Test
    void concurrentManagersScenario_shouldKeepConsistentState() throws Exception {
        int workers = 6;
        int operationsPerWorker = 8;
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);

        for (int worker = 0; worker < workers; worker++) {
            int workerIndex = worker;
            executor.submit(() -> {
                ready.countDown();
                await(start);
                for (int i = 0; i < operationsPerWorker; i++) {
                    String username = "load_user_" + workerIndex + "_" + i;
                    String roleName = "LoadRole" + workerIndex + "_" + i;

                    User user = User.create(username, "Worker " + workerIndex, username + "@load.test");
                    Role role = new Role(roleName, "Load role");

                    system.getUserManager().add(user);
                    system.getUserManager().update(username, "Updated " + workerIndex, username + "@load.test");
                    system.getRoleManager().add(role);
                    system.getRoleManager().addPermissionToRole(roleName,
                            new Permission("READ", "reports", "Load permission"));
                    system.getAssignmentManager().add(new PermanentAssignment(
                            user,
                            role,
                            AssignmentMetadata.now("load-test", "parallel-op")
                    ));

                    system.getUserManager().findByFilter(UserFilters.byUsernameContains("load_user_"));
                    system.getRoleManager().findByFilter(RoleFilters.byNameContains("LoadRole"));
                    system.getAssignmentManager().findByFilter(AssignmentFilters.byUsername(username));
                }
            });
        }

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        List<User> users = system.getUserManager().findAll();
        Set<String> usernames = new HashSet<>(users.stream().map(User::username).toList());
        int expectedCreatedUsers = workers * operationsPerWorker + 1;

        assertEquals(expectedCreatedUsers, users.size());
        assertEquals(expectedCreatedUsers, usernames.size());
        assertTrue(system.getAssignmentManager().getActiveAssignments().size() >= workers * operationsPerWorker);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Load test interrupted", e);
        }
    }
}
