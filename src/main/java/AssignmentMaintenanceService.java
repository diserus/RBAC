import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class AssignmentMaintenanceService {
    private final AssignmentManager assignmentManager;
    private final AuditLog auditLog;
    private final java.util.function.Supplier<String> statisticsSupplier;
    private final ScheduledExecutorService scheduler;

    public AssignmentMaintenanceService(AssignmentManager assignmentManager,
                                        AuditLog auditLog,
                                        java.util.function.Supplier<String> statisticsSupplier) {
        this.assignmentManager = assignmentManager;
        this.auditLog = auditLog;
        this.statisticsSupplier = statisticsSupplier;

        AtomicInteger counter = new AtomicInteger(1);
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "assignment-maintenance-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
        this.scheduler = Executors.newSingleThreadScheduledExecutor(factory);
    }

    public void start(long intervalSeconds) {
        scheduler.scheduleAtFixedRate(this::runCycle, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    public void runCycle() {
        int deactivated = assignmentManager.deactivateExpiredTemporaryAssignments();
        auditLog.log("ASSIGNMENT_MAINTENANCE", "scheduler", "temporary-assignments",
                "deactivated=" + deactivated);
        auditLog.log("SYSTEM_STATS", "scheduler", "rbac-system", statisticsSupplier.get());
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}
