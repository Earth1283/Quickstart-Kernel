package org.firstinspires.ftc.teamcode.kernel.input;

import org.firstinspires.ftc.teamcode.kernel.errors.BindingPanic;
import org.firstinspires.ftc.teamcode.kernel.errors.KernelPanic;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

public final class Input {
    public final Pad gp1;
    public final Pad gp2;

    // Buttons are sampled in creation order, so a chord always sees its parts' current state.
    private final List<Button> buttons = new ArrayList<>();
    private final List<Binding> bindings = new ArrayList<>();
    private final LongSupplier clock;

    public Input(PadSource gamepad1, PadSource gamepad2, double stickDeadband, LongSupplier nanoClock) {
        this(gamepad1, gamepad2, stickDeadband, 1.0, nanoClock);
    }

    public Input(PadSource gamepad1, PadSource gamepad2, double stickDeadband, double stickCurve, LongSupplier nanoClock) {
        this.clock = nanoClock;
        gp1 = new Pad("gp1", gamepad1, stickDeadband, stickCurve, this);
        gp2 = new Pad("gp2", gamepad2, stickDeadband, stickCurve, this);
    }

    public Pad pad(int gamepad) {
        if (gamepad == 1) return gp1;
        if (gamepad == 2) return gp2;
        throw new BindingPanic("There is no gamepad " + gamepad + "; use 1 or 2.");
    }

    public void bind(Object target) {
        AnnotationBinder.bind(this, target);
    }

    public void update() {
        sample();
        fireBindings();
    }

    public void sample() {
        long now = clock.getAsLong();
        for (Button button : buttons) button.sample(now);
    }

    public void fireBindings() {
        long now = clock.getAsLong();
        for (Binding binding : new ArrayList<>(bindings)) fire(binding, now);
    }

    private static void fire(Binding binding, long now) {
        try {
            binding.step.evaluate(now);
        } catch (KernelPanic panic) {
            throw panic;
        } catch (Exception e) {
            throw new BindingPanic(binding.description + " threw " + e, e);
        }
    }

    void register(Button button) {
        buttons.add(button);
    }

    void addBinding(Binding binding) {
        bindings.add(binding);
    }
}
