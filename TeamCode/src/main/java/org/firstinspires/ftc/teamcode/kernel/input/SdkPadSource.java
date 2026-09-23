package org.firstinspires.ftc.teamcode.kernel.input;

import com.qualcomm.robotcore.hardware.Gamepad;

import java.util.function.Supplier;

public final class SdkPadSource implements PadSource {
    private final Supplier<Gamepad> gamepad;

    public SdkPadSource(Supplier<Gamepad> gamepad) {
        this.gamepad = gamepad;
    }

    @Override
    public boolean isDown(Key key) {
        Gamepad g = gamepad.get();
        switch (key) {
            case A: return g.a;
            case B: return g.b;
            case X: return g.x;
            case Y: return g.y;
            case DPAD_UP: return g.dpad_up;
            case DPAD_DOWN: return g.dpad_down;
            case DPAD_LEFT: return g.dpad_left;
            case DPAD_RIGHT: return g.dpad_right;
            case LEFT_BUMPER: return g.left_bumper;
            case RIGHT_BUMPER: return g.right_bumper;
            case LEFT_STICK_BUTTON: return g.left_stick_button;
            case RIGHT_STICK_BUTTON: return g.right_stick_button;
            case BACK: return g.back;
            case START: return g.start;
            case GUIDE: return g.guide;
            case LEFT_TRIGGER: return g.left_trigger >= Trigger.DEFAULT_THRESHOLD;
            case RIGHT_TRIGGER: return g.right_trigger >= Trigger.DEFAULT_THRESHOLD;
            default: throw new IllegalArgumentException(key.name());
        }
    }

    @Override
    public double axis(PadAxis axis) {
        Gamepad g = gamepad.get();
        switch (axis) {
            case LEFT_STICK_RIGHT: return g.left_stick_x;
            case LEFT_STICK_UP: return -g.left_stick_y;
            case RIGHT_STICK_RIGHT: return g.right_stick_x;
            case RIGHT_STICK_UP: return -g.right_stick_y;
            case LEFT_TRIGGER: return g.left_trigger;
            case RIGHT_TRIGGER: return g.right_trigger;
            default: throw new IllegalArgumentException(axis.name());
        }
    }
}
