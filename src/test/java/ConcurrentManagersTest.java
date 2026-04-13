import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConcurrentManagersTest {

    @Test
    void userManager_shouldHandleParallelAddAndUpdate() throws InterruptedException {
        UserManager manager = new UserManager();
        int threads = 12;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);

        for (int i = 0; i < threads; i++) {
            int index = i;
            executor.submit(() -> {
                ready.countDown();
                await(start);
                String username = "user" + index;
                manager.add(User.create(username, "User " + index, username + "@mail.com"));
                manager.update(username, "Updated " + index, username + "@example.com");
            });
        }

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        assertEquals(threads, manager.count());
        assertTrue(manager.findAll().stream().allMatch(user -> user.email().endsWith("@example.com")));
    }

    @Test
    void roleManager_shouldRejectDuplicateRoleNameUnderParallelAdd() throws InterruptedException {
        RoleManager manager = new RoleManager();
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger failures = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                ready.countDown();
                await(start);
                try {
                    manager.add(new Role("shared-role", "Parallel role"));
                } catch (IllegalStateException ex) {
                    failures.incrementAndGet();
                }
            });
        }

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        assertEquals(1, manager.count());
        assertEquals(threads - 1, failures.get());
    }

    @Test
    void assignmentManager_shouldPreventDuplicateActiveAssignmentUnderParallelAdd() throws InterruptedException {
        AssignmentManager manager = new AssignmentManager();
        User user = User.create("alice", "Alice Smith", "alice@example.com");
        Role role = new Role("ADMIN", "Administrator");

        int threads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger failures = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                ready.countDown();
                await(start);
                try {
                    manager.add(new PermanentAssignment(user, role, AssignmentMetadata.now("system", "parallel")));
                } catch (IllegalStateException ex) {
                    failures.incrementAndGet();
                }
            });
        }

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        assertEquals(1, manager.count());
        assertEquals(1, manager.findByUser(user).size());
        assertEquals(List.of(role), manager.findByUser(user).stream().map(RoleAssignment::role).toList());
        assertEquals(threads - 1, failures.get());
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Thread was interrupted", e);
        }
    }
}
