package org.firstinspires.ftc.teamcode.kernel.time;

import static org.junit.Assert.assertEquals;

import org.firstinspires.ftc.teamcode.kernel.errors.BindingPanic;
import org.firstinspires.ftc.teamcode.kernel.events.Subscription;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class SchedulerTest {
    private long nowNanos = 1;
    private final Scheduler scheduler = new Scheduler(() -> nowNanos);
    private int runs;

    private void advance(long millis) {
        nowNanos += TimeUnit.MILLISECONDS.toNanos(millis);
        scheduler.update();
    }

    @Test
    public void afterFiresOnceWhenDue() {
        scheduler.after(100, () -> runs++);
        advance(99);
        assertEquals(0, runs);
        advance(1);
        assertEquals(1, runs);
        advance(500);
        assertEquals(1, runs);
    }

    @Test
    public void everyKeepsFiringAndDoesNotBurstAfterAStall() {
        scheduler.every(100, () -> runs++);
        advance(100);
        advance(100);
        assertEquals(2, runs);
        advance(1000);
        assertEquals(3, runs);
        advance(100);
        assertEquals(4, runs);
    }

    @Test
    public void cancelledTimerNeverFires() {
        Subscription timer = scheduler.after(50, () -> runs++);
        timer.cancel();
        advance(100);
        assertEquals(0, runs);
    }

    @Test
    public void everyCanCancelItselfFromItsOwnCallback() {
        Subscription[] self = new Subscription[1];
        self[0] = scheduler.every(10, () -> {
            runs++;
            self[0].cancel();
        });
        advance(10);
        advance(10);
        assertEquals(1, runs);
    }

    @Test(expected = BindingPanic.class)
    public void callbackExceptionsBecomePanics() {
        scheduler.after(1, () -> {
            throw new IllegalStateException("boom");
        });
        advance(1);
    }
}
