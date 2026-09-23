package org.firstinspires.ftc.teamcode.kernel.input;

public interface PadSource {
    boolean isDown(Key key);

    double axis(PadAxis axis);
}
