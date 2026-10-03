package org.firstinspires.ftc.teamcode.kernel.telemetry;

import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.kernel.events.Subscription;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

// Everything you add with data() or line() lasts one frame; watch() lasts until you cancel it.
// The kernel publishes automatically from tick(), a few times a second.
public final class KernelTelemetry {
    private static final long FRAME_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(100);

    private final TelemetrySink sink;
    private final LongSupplier nanoClock;
    private final Map<String, Supplier<?>> watches = new LinkedHashMap<>();
    private final Map<String, Object> data = new LinkedHashMap<>();
    private final Set<String> lines = new LinkedHashSet<>();

    private long lastPublishedAt;
    private boolean everPublished;

    public KernelTelemetry(TelemetrySink sink, LongSupplier nanoClock) {
        this.sink = sink;
        this.nanoClock = nanoClock;
    }

    public void data(String key, Object value) {
        data.put(key, value);
    }

    public void line(String text) {
        lines.add(text);
    }

    public Subscription watch(String key, Supplier<?> value) {
        watches.put(key, value);
        return () -> watches.remove(key);
    }

    public boolean isDue() {
        return !everPublished || nanoClock.getAsLong() - lastPublishedAt >= FRAME_INTERVAL_NANOS;
    }

    // statusLines come first so the kernel's own readout is always in the same place on the screen.
    public void publish(List<String> statusLines) {
        for (String text : statusLines) sink.line(text);
        for (Map.Entry<String, Supplier<?>> watch : watches.entrySet()) sink.line(format(watch.getKey(), watch.getValue().get()));
        for (Map.Entry<String, Object> entry : data.entrySet()) sink.line(format(entry.getKey(), entry.getValue()));
        for (String text : lines) sink.line(text);
        sink.update();
        data.clear();
        lines.clear();
        lastPublishedAt = nanoClock.getAsLong();
        everPublished = true;
    }

    public static Report scoped(String prefix, List<String> into) {
        return (key, value) -> into.add(format(prefix + " " + key, value));
    }

    static String format(String key, Object value) {
        return key + ": " + describe(value);
    }

    public static String describe(Object value) {
        if (value instanceof Pose) {
            Pose pose = (Pose) value;
            return String.format(Locale.US, "(%.1f, %.1f) %.0f°", pose.x(), pose.y(), Math.toDegrees(pose.heading()));
        }
        if (value instanceof Double || value instanceof Float) return String.format(Locale.US, "%.2f", ((Number) value).doubleValue());
        return String.valueOf(value);
    }
}
