package org.firstinspires.ftc.teamcode.kernel.time;

import org.firstinspires.ftc.teamcode.kernel.events.Event;
import org.firstinspires.ftc.teamcode.kernel.events.EventBus;

import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

public final class MatchClock {
    public static final Event<Void> ENDGAME = Event.of("match.endgame");
    public static final Event<Void> ENDING = Event.of("match.ending");

    private static final double AUTONOMOUS_SECONDS = 30;
    private static final double TELEOP_SECONDS = 120;
    private static final double TELEOP_ENDGAME_REMAINING = 30;
    private static final double AUTONOMOUS_ENDING_REMAINING = 5;
    private static final double TELEOP_ENDING_REMAINING = 10;

    private final EventBus events;
    private final LongSupplier nanoClock;
    private final double totalSeconds;
    private final double endgameRemaining;
    private final double endingRemaining;

    private long startedAt;
    private boolean started;
    private boolean endgameFired;
    private boolean endingFired;

    MatchClock(EventBus events, LongSupplier nanoClock, double totalSeconds, double endgameRemaining, double endingRemaining) {
        this.events = events;
        this.nanoClock = nanoClock;
        this.totalSeconds = totalSeconds;
        this.endgameRemaining = endgameRemaining;
        this.endingRemaining = endingRemaining;
    }

    public static MatchClock autonomous(EventBus events, LongSupplier nanoClock) {
        return new MatchClock(events, nanoClock, AUTONOMOUS_SECONDS, 0, AUTONOMOUS_ENDING_REMAINING);
    }

    public static MatchClock teleOp(EventBus events, LongSupplier nanoClock) {
        return new MatchClock(events, nanoClock, TELEOP_SECONDS, TELEOP_ENDGAME_REMAINING, TELEOP_ENDING_REMAINING);
    }

    public void start() {
        startedAt = nanoClock.getAsLong();
        started = true;
    }

    public boolean isStarted() {
        return started;
    }

    public double secondsRemaining() {
        if (!started) return totalSeconds;
        double elapsed = TimeUnit.NANOSECONDS.toMillis(nanoClock.getAsLong() - startedAt) / 1000.0;
        return Math.max(0, totalSeconds - elapsed);
    }

    public void update() {
        if (!started) return;
        double remaining = secondsRemaining();
        if (!endgameFired && endgameRemaining > 0 && remaining <= endgameRemaining) {
            endgameFired = true;
            events.emit(ENDGAME);
        }
        if (!endingFired && remaining <= endingRemaining) {
            endingFired = true;
            events.emit(ENDING);
        }
    }
}
