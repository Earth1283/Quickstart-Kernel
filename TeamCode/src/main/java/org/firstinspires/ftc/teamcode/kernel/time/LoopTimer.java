package org.firstinspires.ftc.teamcode.kernel.time;

import java.util.function.LongSupplier;

public final class LoopTimer {
    private static final double SMOOTHING = 0.1;

    private final LongSupplier nanoClock;
    private long lastMarkAt;
    private double smoothedMillis;

    public LoopTimer(LongSupplier nanoClock) {
        this.nanoClock = nanoClock;
    }

    public void mark() {
        long now = nanoClock.getAsLong();
        if (lastMarkAt != 0) {
            double millis = (now - lastMarkAt) / 1e6;
            smoothedMillis = smoothedMillis == 0 ? millis : smoothedMillis + SMOOTHING * (millis - smoothedMillis);
        }
        lastMarkAt = now;
    }

    public double millis() {
        return smoothedMillis;
    }

    public double hertz() {
        return smoothedMillis == 0 ? 0 : 1000.0 / smoothedMillis;
    }
}
