package org.firstinspires.ftc.teamcode.kernel.telemetry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.kernel.events.Subscription;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class KernelTelemetryTest {
    private final List<String> shown = new ArrayList<>();
    private int updates;
    private long nowNanos = 1;
    private final KernelTelemetry telemetry = new KernelTelemetry(new TelemetrySink() {
        @Override
        public void line(String text) {
            shown.add(text);
        }

        @Override
        public void update() {
            updates++;
        }
    }, () -> nowNanos);

    @Test
    public void statusComesFirstThenWatchesDataAndLines() {
        telemetry.line("hello");
        telemetry.data("n", 3);
        telemetry.watch("w", () -> "live");
        telemetry.publish(Collections.singletonList("STATUS"));
        assertEquals(Arrays.asList("STATUS", "w: live", "n: 3", "hello"), shown);
        assertEquals(1, updates);
    }

    @Test
    public void dataAndLinesLastOneFrameButWatchesPersist() {
        telemetry.data("n", 1);
        telemetry.line("once");
        telemetry.watch("w", () -> 2);
        telemetry.publish(Collections.<String>emptyList());
        shown.clear();
        telemetry.publish(Collections.<String>emptyList());
        assertEquals(Collections.singletonList("w: 2"), shown);
    }

    @Test
    public void repeatedDataForTheSameKeyKeepsTheLatest() {
        telemetry.data("k", 1);
        telemetry.data("k", 2);
        telemetry.line("same");
        telemetry.line("same");
        telemetry.publish(Collections.<String>emptyList());
        assertEquals(Arrays.asList("k: 2", "same"), shown);
    }

    @Test
    public void cancelledWatchDisappears() {
        Subscription watch = telemetry.watch("w", () -> 1);
        watch.cancel();
        telemetry.publish(Collections.<String>emptyList());
        assertTrue(shown.isEmpty());
    }

    @Test
    public void publishesAtMostEveryHundredMillis() {
        assertTrue(telemetry.isDue());
        telemetry.publish(Collections.<String>emptyList());
        assertFalse(telemetry.isDue());
        nowNanos += TimeUnit.MILLISECONDS.toNanos(100);
        assertTrue(telemetry.isDue());
    }

    @Test
    public void formatsDoublesAndPoses() {
        assertEquals("1.50", KernelTelemetry.describe(1.5));
        assertEquals("(12.0, 34.5) 90°", KernelTelemetry.describe(new Pose(12, 34.5, Math.PI / 2)));
    }

    @Test
    public void scopedReportsArePrefixedWithTheSubsystemName() {
        List<String> lines = new ArrayList<>();
        KernelTelemetry.scoped("lift", lines).data("target", "HIGH");
        assertEquals(Collections.singletonList("lift target: HIGH"), lines);
    }
}
