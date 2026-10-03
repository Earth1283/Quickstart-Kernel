package org.firstinspires.ftc.teamcode.kernel.drivers;

import com.qualcomm.robotcore.hardware.DistanceSensor;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.teamcode.kernel.Kernel;
import org.firstinspires.ftc.teamcode.kernel.init.InitResult;
import org.firstinspires.ftc.teamcode.kernel.subsystems.Rangefinder;
import org.firstinspires.ftc.teamcode.kernel.telemetry.Report;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class DistanceRangefinder implements Rangefinder {
    private static final long READ_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(40);

    private final Kernel kernel;
    private final DistanceSensor sensor;
    private final double detectInches;

    private double inches = Double.NaN;
    private boolean detecting;
    private long lastReadAt;

    public DistanceRangefinder(Kernel kernel, String sensorName, double detectInches) {
        this.kernel = kernel;
        this.sensor = kernel.device(DistanceSensor.class, sensorName);
        this.detectInches = detectInches;
    }

    @Override
    public InitResult init() {
        double reading = sensor.getDistance(DistanceUnit.INCH);
        if (Double.isNaN(reading) || Double.isInfinite(reading)) return InitResult.warn("no reading; check wiring and range");
        return InitResult.ok(String.format(Locale.US, "%.1f in", reading));
    }

    @Override
    public double inches() {
        return inches;
    }

    @Override
    public boolean isDetecting() {
        return detecting;
    }

    // Sensor reads are I2C or analog transactions the bulk cache doesn't cover, so they're rate limited.
    @Override
    public void update() {
        long now = kernel.nanoTime();
        if (now - lastReadAt < READ_INTERVAL_NANOS) return;
        lastReadAt = now;
        inches = sensor.getDistance(DistanceUnit.INCH);
        boolean nowDetecting = inches <= detectInches;
        if (nowDetecting == detecting) return;
        detecting = nowDetecting;
        kernel.emit(detecting ? DETECTED : CLEARED);
    }

    @Override
    public void report(Report report) {
        report.data("inches", inches);
        report.data("detecting", detecting);
    }

    @Override
    public void stop() {}
}
