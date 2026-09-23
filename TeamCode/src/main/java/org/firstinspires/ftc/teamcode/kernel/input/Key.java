package org.firstinspires.ftc.teamcode.kernel.input;

// PlayStation names map onto these: CROSS = A, CIRCLE = B, SQUARE = X, TRIANGLE = Y,
// SHARE = BACK, OPTIONS = START, PS = GUIDE.
public enum Key {
    A, B, X, Y,
    DPAD_UP, DPAD_DOWN, DPAD_LEFT, DPAD_RIGHT,
    LEFT_BUMPER, RIGHT_BUMPER,
    LEFT_STICK_BUTTON, RIGHT_STICK_BUTTON,
    BACK, START, GUIDE,
    LEFT_TRIGGER, RIGHT_TRIGGER;

    public boolean isTrigger() {
        return this == LEFT_TRIGGER || this == RIGHT_TRIGGER;
    }
}
