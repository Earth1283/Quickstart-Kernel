package org.firstinspires.ftc.teamcode.kernel.input;

public final class Stick {
    private final PadSource source;
    private final PadAxis rightAxis;
    private final PadAxis upAxis;
    private final double deadband;
    private final double exponent;

    Stick(PadSource source, PadAxis rightAxis, PadAxis upAxis, double deadband, double exponent) {
        this.source = source;
        this.rightAxis = rightAxis;
        this.upAxis = upAxis;
        this.deadband = deadband;
        this.exponent = exponent;
    }

    // Exponent above 1 softens small deflections for fine control while still reaching full scale.
    public Stick curve(double exponent) {
        return new Stick(source, rightAxis, upAxis, deadband, exponent);
    }

    public double right() {
        return source.axis(rightAxis) * radialScale();
    }

    public double left() {
        return -right();
    }

    public double up() {
        return source.axis(upAxis) * radialScale();
    }

    public double down() {
        return -up();
    }

    // Rescale outside the deadband so output still starts at 0 and reaches full scale.
    private double radialScale() {
        double r = Math.hypot(source.axis(rightAxis), source.axis(upAxis));
        if (r <= deadband) return 0;
        double magnitude = (Math.min(r, 1) - deadband) / (1 - deadband);
        return Math.pow(magnitude, exponent) / r;
    }
}
