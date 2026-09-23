package org.firstinspires.ftc.teamcode.kernel.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class InputTest {
    private final FakePad pad1 = new FakePad();
    private final FakePad pad2 = new FakePad();
    private long nowNanos;
    private Input input;

    @Before
    public void setUp() {
        input = new Input(pad1, pad2, 0.1, () -> nowNanos);
    }

    private void tick(long advanceMillis) {
        nowNanos += TimeUnit.MILLISECONDS.toNanos(advanceMillis);
        input.update();
    }

    private void press(Key key) {
        pad1.down.add(key);
    }

    private void release(Key key) {
        pad1.down.remove(key);
    }

    @Test
    public void pressAndReleaseEdgesLastExactlyOneTick() {
        tick(20);
        press(Key.A);
        tick(20);
        assertTrue(input.gp1.a.justPressed());
        assertTrue(input.gp1.a.isDown());
        tick(20);
        assertFalse(input.gp1.a.justPressed());
        assertTrue(input.gp1.a.isDown());
        release(Key.A);
        tick(20);
        assertTrue(input.gp1.a.justReleased());
        tick(20);
        assertFalse(input.gp1.a.justReleased());
    }

    @Test
    public void onPressFiresOncePerPress() {
        int[] count = {0};
        input.gp1.a.onPress(() -> count[0]++);
        press(Key.A);
        tick(20);
        tick(20);
        tick(20);
        release(Key.A);
        tick(20);
        press(Key.A);
        tick(20);
        assertEquals(2, count[0]);
    }

    @Test
    public void whileHeldFiresEveryTickIncludingThePressTick() {
        int[] count = {0};
        input.gp1.b.whileHeld(() -> count[0]++);
        press(Key.B);
        tick(20);
        tick(20);
        tick(20);
        release(Key.B);
        tick(20);
        assertEquals(3, count[0]);
    }

    @Test
    public void onToggleAlternatesStartingWithTrue() {
        List<Boolean> states = new ArrayList<>();
        input.gp1.x.onToggle(states::add);
        for (int i = 0; i < 3; i++) {
            press(Key.X);
            tick(20);
            release(Key.X);
            tick(20);
        }
        assertEquals(java.util.Arrays.asList(true, false, true), states);
    }

    @Test
    public void onLongPressFiresOnceAfterThresholdAndRearmsOnRelease() {
        int[] count = {0};
        input.gp1.y.onLongPress(500, () -> count[0]++);
        press(Key.Y);
        tick(20);
        tick(300);
        assertEquals(0, count[0]);
        tick(250);
        assertEquals(1, count[0]);
        tick(1000);
        assertEquals(1, count[0]);
        release(Key.Y);
        tick(20);
        press(Key.Y);
        tick(20);
        tick(600);
        assertEquals(2, count[0]);
    }

    @Test
    public void shortPressDoesNotCountAsLongPress() {
        int[] count = {0};
        input.gp1.y.onLongPress(500, () -> count[0]++);
        press(Key.Y);
        tick(20);
        release(Key.Y);
        tick(600);
        assertEquals(0, count[0]);
    }

    @Test
    public void doubleTapNeedsTwoPressesInsideTheWindow() {
        int[] count = {0};
        input.gp1.b.onDoubleTap(300, () -> count[0]++);
        press(Key.B);
        tick(20);
        release(Key.B);
        tick(100);
        press(Key.B);
        tick(100);
        assertEquals(1, count[0]);

        release(Key.B);
        tick(20);
        press(Key.B);
        tick(500);
        release(Key.B);
        tick(500);
        press(Key.B);
        tick(20);
        assertEquals(1, count[0]);
    }

    @Test
    public void tripleTapCountsAsOneDoubleTap() {
        int[] count = {0};
        input.gp1.b.onDoubleTap(300, () -> count[0]++);
        for (int i = 0; i < 3; i++) {
            press(Key.B);
            tick(50);
            release(Key.B);
            tick(50);
        }
        assertEquals(1, count[0]);
    }

    @Test
    public void chordRequiresBothButtonsAndIsCached() {
        Button chord = input.gp1.leftBumper.and(input.gp1.a);
        assertTrue(chord == input.gp1.leftBumper.and(input.gp1.a));
        int[] count = {0};
        chord.onPress(() -> count[0]++);
        press(Key.A);
        tick(20);
        assertEquals(0, count[0]);
        press(Key.LEFT_BUMPER);
        tick(20);
        assertEquals(1, count[0]);
        assertTrue(chord.isDown());
    }

    @Test
    public void triggerActsAsButtonPastThreshold() {
        int[] count = {0};
        input.gp1.rightTrigger.asButton(0.8).onPress(() -> count[0]++);
        pad1.axes.put(PadAxis.RIGHT_TRIGGER, 0.6);
        tick(20);
        assertEquals(0, count[0]);
        pad1.axes.put(PadAxis.RIGHT_TRIGGER, 0.9);
        tick(20);
        assertEquals(1, count[0]);
        assertTrue(input.gp1.rightTrigger.asButton(0.8) == input.gp1.rightTrigger.asButton(0.8));
    }

    @Test
    public void gamepadsAreIndependent() {
        int[] count = {0};
        input.gp2.a.onPress(() -> count[0]++);
        press(Key.A);
        tick(20);
        assertEquals(0, count[0]);
        pad2.down.add(Key.A);
        tick(20);
        assertEquals(1, count[0]);
    }

    @Test
    public void stickDeadbandZeroesSmallInputAndStillReachesFullScale() {
        pad1.axes.put(PadAxis.LEFT_STICK_RIGHT, 0.05);
        assertEquals(0, input.gp1.leftStick.right(), 1e-9);
        pad1.axes.put(PadAxis.LEFT_STICK_RIGHT, 1.0);
        assertEquals(1.0, input.gp1.leftStick.right(), 1e-9);
        assertEquals(-1.0, input.gp1.leftStick.left(), 1e-9);
        pad1.axes.put(PadAxis.LEFT_STICK_RIGHT, 0.0);
        pad1.axes.put(PadAxis.LEFT_STICK_UP, 0.55);
        assertEquals(0.5, input.gp1.leftStick.up(), 1e-9);
    }
}
