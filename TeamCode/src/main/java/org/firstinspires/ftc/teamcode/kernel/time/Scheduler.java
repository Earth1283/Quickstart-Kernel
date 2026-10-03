package org.firstinspires.ftc.teamcode.kernel.time;

import org.firstinspires.ftc.teamcode.kernel.errors.BindingPanic;
import org.firstinspires.ftc.teamcode.kernel.errors.KernelPanic;
import org.firstinspires.ftc.teamcode.kernel.events.Subscription;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

// Runs inside tick() on the kernel clock, so timers never need a thread and never outlive the OpMode.
public final class Scheduler {
    private static final class Timer {
        final Runnable action;
        final long periodNanos;
        long dueAt;
        boolean cancelled;

        Timer(Runnable action, long dueAt, long periodNanos) {
            this.action = action;
            this.dueAt = dueAt;
            this.periodNanos = periodNanos;
        }
    }

    private final LongSupplier nanoClock;
    private final List<Timer> timers = new ArrayList<>();

    public Scheduler(LongSupplier nanoClock) {
        this.nanoClock = nanoClock;
    }

    public Subscription after(long millis, Runnable action) {
        return add(new Timer(action, nanoClock.getAsLong() + TimeUnit.MILLISECONDS.toNanos(millis), 0));
    }

    public Subscription every(long millis, Runnable action) {
        long period = TimeUnit.MILLISECONDS.toNanos(millis);
        return add(new Timer(action, nanoClock.getAsLong() + period, period));
    }

    private Subscription add(Timer timer) {
        timers.add(timer);
        return () -> {
            timer.cancelled = true;
            timers.remove(timer);
        };
    }

    public void update() {
        long now = nanoClock.getAsLong();
        for (Timer timer : new ArrayList<>(timers)) {
            if (timer.cancelled || now < timer.dueAt) continue;
            if (timer.periodNanos == 0) timers.remove(timer);
            else timer.dueAt += timer.periodNanos * Math.max(1, (now - timer.dueAt) / timer.periodNanos + 1);
            run(timer);
        }
    }

    private void run(Timer timer) {
        try {
            timer.action.run();
        } catch (KernelPanic panic) {
            throw panic;
        } catch (RuntimeException e) {
            throw new BindingPanic("A timer callback threw " + e, e);
        }
    }

    public void clear() {
        timers.clear();
    }
}
