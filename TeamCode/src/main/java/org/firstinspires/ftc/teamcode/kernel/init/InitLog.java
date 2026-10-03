package org.firstinspires.ftc.teamcode.kernel.init;

import org.firstinspires.ftc.teamcode.kernel.errors.InitFailedPanic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

public final class InitLog {
    public static final class Entry {
        public final String name;
        public final InitResult result;
        public final boolean critical;
        public final long millis;

        Entry(String name, InitResult result, boolean critical, long millis) {
            this.name = name;
            this.result = result;
            this.critical = critical;
            this.millis = millis;
        }
    }

    private final LongSupplier nanoClock;
    private final List<Entry> entries = new ArrayList<>();

    public InitLog(LongSupplier nanoClock) {
        this.nanoClock = nanoClock;
    }

    // A task that throws is a FAIL, not a crash: the whole log must still reach the telemetry screen.
    public InitResult run(InitTask task) {
        long startedAt = nanoClock.getAsLong();
        InitResult result;
        try {
            result = task.run();
            if (result == null) result = InitResult.ok();
        } catch (Exception e) {
            result = InitResult.fail(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        long millis = TimeUnit.NANOSECONDS.toMillis(nanoClock.getAsLong() - startedAt);
        entries.add(new Entry(task.name(), result, task.critical(), millis));
        return result;
    }

    public void throwIfCriticalFailure() {
        List<Entry> failed = new ArrayList<>();
        for (Entry entry : entries) {
            if (entry.critical && entry.result.status == InitResult.Status.FAIL) failed.add(entry);
        }
        if (!failed.isEmpty()) throw new InitFailedPanic(failed);
    }

    public List<Entry> entries() {
        return Collections.unmodifiableList(entries);
    }

    public int count(InitResult.Status status) {
        int count = 0;
        for (Entry entry : entries) if (entry.result.status == status) count++;
        return count;
    }
}
