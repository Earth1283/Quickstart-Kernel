package org.firstinspires.ftc.teamcode.kernel.input;

import java.util.HashMap;
import java.util.Map;

public final class Trigger {
    public static final double DEFAULT_THRESHOLD = 0.5;

    private final String name;
    private final PadSource source;
    private final PadAxis axis;
    private final Input input;
    private final Map<Double, Button> buttonsByThreshold = new HashMap<>();

    Trigger(String name, PadSource source, PadAxis axis, Input input) {
        this.name = name;
        this.source = source;
        this.axis = axis;
        this.input = input;
    }

    public double value() {
        return source.axis(axis);
    }

    public Button asButton() {
        return asButton(DEFAULT_THRESHOLD);
    }

    public Button asButton(double threshold) {
        Button button = buttonsByThreshold.get(threshold);
        if (button == null) {
            String buttonName = threshold == DEFAULT_THRESHOLD ? name : name + ">=" + threshold;
            button = new Button(buttonName, () -> value() >= threshold, input);
            buttonsByThreshold.put(threshold, button);
        }
        return button;
    }
}
