package org.firstinspires.ftc.teamcode.kernel.init;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.firstinspires.ftc.teamcode.kernel.errors.InitFailedPanic;
import org.junit.Test;

public class InitLogTest {
    private final InitLog log = new InitLog(() -> 0);

    @Test
    public void recordsEveryResultInOrder() {
        log.run(InitTask.of("a", InitResult::ok));
        log.run(InitTask.of("b", () -> InitResult.warn("meh")));
        assertEquals("a", log.entries().get(0).name);
        assertEquals(1, log.count(InitResult.Status.OK));
        assertEquals(1, log.count(InitResult.Status.WARN));
    }

    @Test
    public void throwingTaskBecomesAFailureInsteadOfCrashing() {
        InitResult result = log.run(InitTask.of("bad", () -> {
            throw new IllegalStateException("nope");
        }));
        assertEquals(InitResult.Status.FAIL, result.status);
        assertTrue(result.detail.contains("nope"));
    }

    @Test
    public void nonCriticalFailureDoesNotAbort() {
        log.run(InitTask.of("soft", () -> InitResult.fail("x")));
        log.throwIfCriticalFailure();
    }

    @Test
    public void criticalFailureAbortsAfterAllTasksRan() {
        log.run(InitTask.critical("hard", () -> InitResult.fail("dead")));
        log.run(InitTask.of("later", InitResult::ok));
        try {
            log.throwIfCriticalFailure();
            fail();
        } catch (InitFailedPanic panic) {
            assertTrue(panic.getMessage().contains("hard: dead"));
            assertEquals(2, log.entries().size());
        }
    }
}
