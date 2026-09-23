package org.firstinspires.ftc.teamcode.kernel.input;

import java.util.EnumMap;
import java.util.Map;

public final class Pad {
    public final Button a, b, x, y;
    public final Button dpadUp, dpadDown, dpadLeft, dpadRight;
    public final Button leftBumper, rightBumper;
    public final Button leftStickButton, rightStickButton;
    public final Button back, start, guide;
    public final Trigger leftTrigger, rightTrigger;
    public final Stick leftStick, rightStick;

    private final Map<Key, Button> buttonsByKey = new EnumMap<>(Key.class);

    Pad(String name, PadSource source, double stickDeadband, Input input) {
        for (Key key : Key.values()) {
            if (key.isTrigger()) continue;
            buttonsByKey.put(key, new Button(name + "." + key.name().toLowerCase(), () -> source.isDown(key), input));
        }
        a = buttonsByKey.get(Key.A);
        b = buttonsByKey.get(Key.B);
        x = buttonsByKey.get(Key.X);
        y = buttonsByKey.get(Key.Y);
        dpadUp = buttonsByKey.get(Key.DPAD_UP);
        dpadDown = buttonsByKey.get(Key.DPAD_DOWN);
        dpadLeft = buttonsByKey.get(Key.DPAD_LEFT);
        dpadRight = buttonsByKey.get(Key.DPAD_RIGHT);
        leftBumper = buttonsByKey.get(Key.LEFT_BUMPER);
        rightBumper = buttonsByKey.get(Key.RIGHT_BUMPER);
        leftStickButton = buttonsByKey.get(Key.LEFT_STICK_BUTTON);
        rightStickButton = buttonsByKey.get(Key.RIGHT_STICK_BUTTON);
        back = buttonsByKey.get(Key.BACK);
        start = buttonsByKey.get(Key.START);
        guide = buttonsByKey.get(Key.GUIDE);
        leftTrigger = new Trigger(name + ".left_trigger", source, PadAxis.LEFT_TRIGGER, input);
        rightTrigger = new Trigger(name + ".right_trigger", source, PadAxis.RIGHT_TRIGGER, input);
        leftStick = new Stick(source, PadAxis.LEFT_STICK_RIGHT, PadAxis.LEFT_STICK_UP, stickDeadband);
        rightStick = new Stick(source, PadAxis.RIGHT_STICK_RIGHT, PadAxis.RIGHT_STICK_UP, stickDeadband);
    }

    public Button button(Key key) {
        return button(key, Trigger.DEFAULT_THRESHOLD);
    }

    public Button button(Key key, double triggerThreshold) {
        if (key == Key.LEFT_TRIGGER) return leftTrigger.asButton(triggerThreshold);
        if (key == Key.RIGHT_TRIGGER) return rightTrigger.asButton(triggerThreshold);
        return buttonsByKey.get(key);
    }
}
