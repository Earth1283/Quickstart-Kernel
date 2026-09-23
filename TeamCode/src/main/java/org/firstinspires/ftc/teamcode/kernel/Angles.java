package org.firstinspires.ftc.teamcode.kernel;

public final class Angles {
    private Angles() {}

    public static double wrap(double radians) {
        double wrapped = (radians + Math.PI) % (2 * Math.PI);
        if (wrapped < 0) wrapped += 2 * Math.PI;
        return wrapped - Math.PI;
    }

    public static double error(double from, double to) {
        return wrap(to - from);
    }

    public static double toward(double x, double y, double targetX, double targetY) {
        return Math.atan2(targetY - y, targetX - x);
    }
}
