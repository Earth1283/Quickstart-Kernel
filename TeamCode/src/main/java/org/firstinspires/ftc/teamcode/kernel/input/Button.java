package org.firstinspires.ftc.teamcode.kernel.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class Button {
    public static final long DEFAULT_DOUBLE_TAP_WINDOW_MS = 300;

    private final String name;
    private final BooleanSupplier source;
    private final Input input;
    private final Map<Button, Button> chords = new HashMap<>();
    private final List<BooleanSupplier> suppressors = new ArrayList<>();

    private boolean down;
    private boolean wasDown;
    private long pressedAtNanos;
    private long nowNanos;

    Button(String name, BooleanSupplier source, Input input) {
        this.name = name;
        this.source = source;
        this.input = input;
        input.register(this);
    }

    void sample(long nowNanos) {
        this.nowNanos = nowNanos;
        wasDown = down;
        down = source.getAsBoolean() && !suppressed();
        if (justPressed()) pressedAtNanos = nowNanos;
    }

    boolean rawDown() {
        return source.getAsBoolean();
    }

    // Pass a condition built from rawDown(), not isDown(), so sampling order can't make it lag a tick.
    void suppressWhile(BooleanSupplier condition) {
        suppressors.add(condition);
    }

    private boolean suppressed() {
        for (BooleanSupplier suppressor : suppressors) if (suppressor.getAsBoolean()) return true;
        return false;
    }

    public String name() {
        return name;
    }

    public boolean isDown() {
        return down;
    }

    public boolean justPressed() {
        return down && !wasDown;
    }

    public boolean justReleased() {
        return !down && wasDown;
    }

    public long heldMillis() {
        return down ? TimeUnit.NANOSECONDS.toMillis(nowNanos - pressedAtNanos) : 0;
    }

    public Button and(Button other) {
        Button chord = chords.get(other);
        if (chord == null) {
            chord = new Button(name + "+" + other.name, () -> this.down && other.down, input);
            chords.put(other, chord);
        }
        return chord;
    }

    public Button onPress(Runnable action) {
        return onPress(name + ".onPress", action::run);
    }

    public Button onRelease(Runnable action) {
        return onRelease(name + ".onRelease", action::run);
    }

    public Button whileHeld(Runnable action) {
        return whileHeld(name + ".whileHeld", action::run);
    }

    public Button onToggle(Consumer<Boolean> action) {
        return onToggle(name + ".onToggle", action::accept);
    }

    public Button onLongPress(long millis, Runnable action) {
        return onLongPress(name + ".onLongPress", millis, action::run);
    }

    public Button onDoubleTap(Runnable action) {
        return onDoubleTap(DEFAULT_DOUBLE_TAP_WINDOW_MS, action);
    }

    public Button onDoubleTap(long windowMillis, Runnable action) {
        return onDoubleTap(name + ".onDoubleTap", windowMillis, action::run);
    }

    interface Action {
        void run() throws Exception;
    }

    interface ToggleAction {
        void accept(boolean on) throws Exception;
    }

    Button onPress(String description, Action action) {
        return bind(description, now -> {
            if (justPressed()) action.run();
        });
    }

    Button onRelease(String description, Action action) {
        return bind(description, now -> {
            if (justReleased()) action.run();
        });
    }

    Button whileHeld(String description, Action action) {
        return bind(description, now -> {
            if (down) action.run();
        });
    }

    Button onToggle(String description, ToggleAction action) {
        final boolean[] on = {false};
        return bind(description, now -> {
            if (!justPressed()) return;
            on[0] = !on[0];
            action.accept(on[0]);
        });
    }

    Button onLongPress(String description, long millis, Action action) {
        final long thresholdNanos = TimeUnit.MILLISECONDS.toNanos(millis);
        final boolean[] firedThisHold = {false};
        return bind(description, now -> {
            if (!down) {
                firedThisHold[0] = false;
                return;
            }
            if (!firedThisHold[0] && now - pressedAtNanos >= thresholdNanos) {
                firedThisHold[0] = true;
                action.run();
            }
        });
    }

    Button onDoubleTap(String description, long windowMillis, Action action) {
        final long windowNanos = TimeUnit.MILLISECONDS.toNanos(windowMillis);
        final long[] firstTapAt = {Long.MIN_VALUE};
        return bind(description, now -> {
            if (!justPressed()) return;
            boolean isSecondTap = firstTapAt[0] != Long.MIN_VALUE && now - firstTapAt[0] <= windowNanos;
            if (isSecondTap) {
                firstTapAt[0] = Long.MIN_VALUE;
                action.run();
            } else {
                firstTapAt[0] = now;
            }
        });
    }

    private Button bind(String description, Binding.Step step) {
        input.addBinding(new Binding(description, step));
        return this;
    }

    @Override
    public String toString() {
        return name;
    }
}
