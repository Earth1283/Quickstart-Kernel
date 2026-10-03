package org.firstinspires.ftc.teamcode.kernel.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.firstinspires.ftc.teamcode.kernel.errors.BindingPanic;
import org.firstinspires.ftc.teamcode.kernel.input.annotations.OnPress;
import org.firstinspires.ftc.teamcode.kernel.input.annotations.OnToggle;
import org.firstinspires.ftc.teamcode.kernel.input.annotations.WhileHeld;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class AnnotationBinderTest {
    private final FakePad pad1 = new FakePad();
    private final FakePad pad2 = new FakePad();
    private final Input input = new Input(pad1, pad2, 0.05, () -> 0L);

    static class Base {
        int basePresses;

        @OnPress(Key.X)
        void inherited() {
            basePresses++;
        }
    }

    static class Driver extends Base {
        int aPresses;
        int chordPresses;
        int gamepad2Presses;
        int heldTicks;
        final List<Boolean> toggles = new ArrayList<>();

        @OnPress(Key.A)
        void a() {
            aPresses++;
        }

        @OnPress(value = Key.B, with = Key.LEFT_BUMPER)
        void chord() {
            chordPresses++;
        }

        @OnPress(value = Key.A, gamepad = 2)
        void secondDriver() {
            gamepad2Presses++;
        }

        @WhileHeld(Key.RIGHT_TRIGGER)
        void held() {
            heldTicks++;
        }

        @OnToggle(Key.Y)
        void toggled(boolean on) {
            toggles.add(on);
        }
    }

    @Test
    public void bindsEveryAnnotationIncludingInheritedOnes() {
        Driver driver = new Driver();
        input.bind(driver);

        pad1.down.add(Key.A);
        pad1.down.add(Key.X);
        pad1.down.add(Key.Y);
        pad1.axes.put(PadAxis.RIGHT_TRIGGER, 1.0);
        input.update();
        pad1.down.add(Key.LEFT_BUMPER);
        pad1.down.add(Key.B);
        pad2.down.add(Key.A);
        input.update();

        assertEquals(1, driver.aPresses);
        assertEquals(1, driver.basePresses);
        assertEquals(1, driver.chordPresses);
        assertEquals(1, driver.gamepad2Presses);
        assertEquals(2, driver.heldTicks);
        assertEquals(java.util.Collections.singletonList(true), driver.toggles);
    }

    static class Throws {
        @OnPress(Key.A)
        void boom() {
            throw new IllegalStateException("claw jammed");
        }
    }

    @Test
    public void handlerExceptionBecomesBindingPanicNamingTheBinding() {
        input.bind(new Throws());
        pad1.down.add(Key.A);
        try {
            input.update();
            fail("expected BindingPanic");
        } catch (BindingPanic panic) {
            assertTrue(panic.getMessage(), panic.getMessage().contains("@OnPress(A) Throws.boom()"));
            assertTrue(panic.getCause() instanceof IllegalStateException);
        }
    }

    static class PrivateBinding {
        @OnPress(Key.A)
        private void hidden() {}
    }

    static class ToggleWithoutBoolean {
        @OnToggle(Key.A)
        void toggled() {}
    }

    static class ThirdGamepad {
        @OnPress(value = Key.A, gamepad = 3)
        void a() {}
    }

    static class SelfModifier {
        @OnPress(value = Key.A, with = Key.A)
        void a() {}
    }

    static class ShiftedY {
        final List<String> fired = new ArrayList<>();

        @OnPress(Key.Y)
        void plain() {
            fired.add("plain");
        }

        @OnPress(value = Key.Y, with = Key.LEFT_BUMPER)
        void shifted() {
            fired.add("shifted");
        }
    }

    @Test
    public void withActsAsALayerSoThePlainBindingStaysQuiet() {
        ShiftedY target = new ShiftedY();
        input.bind(target);
        pad1.down.add(Key.LEFT_BUMPER);
        pad1.down.add(Key.Y);
        input.update();
        assertEquals(java.util.Collections.singletonList("shifted"), target.fired);
    }

    @Test
    public void runtimeRejectsWhatTheProcessorRejects() {
        assertBindPanics(new PrivateBinding(), "must not be private");
        assertBindPanics(new ToggleWithoutBoolean(), "exactly one boolean");
        assertBindPanics(new ThirdGamepad(), "no gamepad 3");
        assertBindPanics(new SelfModifier(), "its own modifier");
    }

    private void assertBindPanics(Object target, String fragment) {
        try {
            input.bind(target);
            fail("expected BindingPanic containing: " + fragment);
        } catch (BindingPanic panic) {
            assertTrue(panic.getMessage(), panic.getMessage().contains(fragment));
        }
    }

    @Test
    public void kernelPanicsFromHandlersPassThroughUnwrapped() {
        BindingPanic original = new BindingPanic("inner");
        input.gp1.a.onPress(() -> {
            throw original;
        });
        pad1.down.add(Key.A);
        try {
            input.update();
            fail();
        } catch (BindingPanic panic) {
            assertSame(original, panic);
        }
    }
}
