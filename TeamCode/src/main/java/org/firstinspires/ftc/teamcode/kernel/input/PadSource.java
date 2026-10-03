package org.firstinspires.ftc.teamcode.kernel.input;

public interface PadSource {
    boolean isDown(Key key);

    double axis(PadAxis axis);

    default boolean connected() {
        return true;
    }

    default void rumble(double left, double right, int millis) {}

    default void rumbleBlips(int count) {}

    default void led(double red, double green, double blue) {}
}
