package org.firstinspires.ftc.teamcode.kernel.time;

import static org.junit.Assert.assertEquals;

import org.firstinspires.ftc.teamcode.kernel.events.EventBus;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class MatchClockTest {
    private long nowNanos = 1;
    private final EventBus events = new EventBus();
    private int endgames;
    private int endings;

    private void watch(MatchClock clock) {
        events.on(MatchClock.ENDGAME, () -> endgames++);
        events.on(MatchClock.ENDING, () -> endings++);
        clock.start();
    }

    private void advanceSeconds(MatchClock clock, int seconds) {
        nowNanos += TimeUnit.SECONDS.toNanos(seconds);
        clock.update();
        events.dispatch();
    }

    @Test
    public void teleOpFiresEndgameThenEndingOnce() {
        MatchClock clock = MatchClock.teleOp(events, () -> nowNanos);
        watch(clock);
        advanceSeconds(clock, 89);
        assertEquals(0, endgames);
        advanceSeconds(clock, 1);
        assertEquals(1, endgames);
        assertEquals(0, endings);
        advanceSeconds(clock, 20);
        advanceSeconds(clock, 5);
        assertEquals(1, endgames);
        assertEquals(1, endings);
        assertEquals(5, clock.secondsRemaining(), 1e-9);
    }

    @Test
    public void autonomousHasNoEndgame() {
        MatchClock clock = MatchClock.autonomous(events, () -> nowNanos);
        watch(clock);
        advanceSeconds(clock, 26);
        assertEquals(0, endgames);
        assertEquals(1, endings);
    }

    @Test
    public void nothingFiresBeforeStart() {
        MatchClock clock = MatchClock.teleOp(events, () -> nowNanos);
        events.on(MatchClock.ENDING, () -> endings++);
        advanceSeconds(clock, 500);
        assertEquals(0, endings);
        assertEquals(120, clock.secondsRemaining(), 1e-9);
    }
}
