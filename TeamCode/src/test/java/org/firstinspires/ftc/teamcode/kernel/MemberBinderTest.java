package org.firstinspires.ftc.teamcode.kernel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.firstinspires.ftc.teamcode.kernel.errors.BindingPanic;
import org.firstinspires.ftc.teamcode.kernel.telemetry.KernelTelemetry;
import org.firstinspires.ftc.teamcode.kernel.telemetry.TelemetrySink;
import org.firstinspires.ftc.teamcode.kernel.telemetry.Watch;
import org.firstinspires.ftc.teamcode.kernel.time.Every;
import org.firstinspires.ftc.teamcode.kernel.time.Scheduler;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MemberBinderTest {
    private final List<String> shown = new ArrayList<>();
    private long nowNanos = 1;
    private final KernelTelemetry telemetry = new KernelTelemetry(new TelemetrySink() {
        @Override
        public void line(String text) {
            shown.add(text);
        }

        @Override
        public void update() {}
    }, () -> nowNanos);
    private final Scheduler scheduler = new Scheduler(() -> nowNanos);
    private final MemberBinder binder = new MemberBinder(telemetry, scheduler);

    private void advance(long millis) {
        nowNanos += TimeUnit.MILLISECONDS.toNanos(millis);
        scheduler.update();
    }

    private List<String> frame() {
        shown.clear();
        telemetry.publish(Collections.<String>emptyList());
        return shown;
    }

    static class Watched {
        @Watch private int shots = 3;
        @Watch(value = "first", order = -1) String first = "!";

        @Watch("aiming")
        boolean aiming() {
            return shots > 2;
        }
    }

    @Test
    public void watchesSortByOrderThenKeyAndStayLive() {
        Watched target = new Watched();
        binder.bind(target);
        assertEquals(Arrays.asList("first: !", "aiming: true", "shots: 3"), frame());
        target.shots = 1;
        assertEquals(Arrays.asList("first: !", "aiming: false", "shots: 1"), frame());
    }

    static class Timed {
        int fromStart;
        int fromInit;

        @Every(100)
        void afterStart() {
            fromStart++;
        }

        @Every(value = 100, duringInit = true)
        void evenInInit() {
            fromInit++;
        }
    }

    @Test
    public void everyWaitsForStartUnlessToldOtherwise() {
        Timed target = new Timed();
        binder.bind(target);
        advance(250);
        assertEquals(0, target.fromStart);
        assertEquals(1, target.fromInit);
        binder.start();
        advance(99);
        assertEquals(0, target.fromStart);
        advance(1);
        assertEquals(1, target.fromStart);
    }

    @Test
    public void bindingAfterStartArmsImmediately() {
        binder.start();
        Timed target = new Timed();
        binder.bind(target);
        advance(100);
        assertEquals(1, target.fromStart);
    }

    static class Throws {
        @Watch
        int broken() {
            throw new IllegalStateException("sensor unplugged");
        }
    }

    @Test
    public void throwingWatchBecomesBindingPanicNamingIt() {
        binder.bind(new Throws());
        try {
            frame();
            fail("expected BindingPanic");
        } catch (BindingPanic panic) {
            assertTrue(panic.getMessage(), panic.getMessage().contains("@Watch(broken) Throws.broken() threw"));
        }
    }

    static class PrivateWatch {
        @Watch
        private int hidden() {
            return 0;
        }
    }

    static class VoidWatch {
        @Watch
        void nothing() {}
    }

    static class StaticWatch {
        @Watch static int shared;
    }

    static class DuplicateKeys {
        @Watch("lift") int a;
        @Watch("lift") int b;
    }

    static class EveryWithParameter {
        @Every(100)
        void poll(int x) {}
    }

    static class ZeroPeriod {
        @Every(0)
        void poll() {}
    }

    @Test
    public void runtimeRejectsWhatTheProcessorRejects() {
        assertBindPanics(new PrivateWatch(), "must not be private");
        assertBindPanics(new VoidWatch(), "must return the value to show");
        assertBindPanics(new StaticWatch(), "must not be static");
        assertBindPanics(new DuplicateKeys(), "use the key \"lift\"");
        assertBindPanics(new EveryWithParameter(), "must take no parameters");
        assertBindPanics(new ZeroPeriod(), "period must be > 0");
    }

    private void assertBindPanics(Object target, String fragment) {
        try {
            binder.bind(target);
            fail("expected BindingPanic containing: " + fragment);
        } catch (BindingPanic panic) {
            assertTrue(panic.getMessage(), panic.getMessage().contains(fragment));
        }
    }
}
