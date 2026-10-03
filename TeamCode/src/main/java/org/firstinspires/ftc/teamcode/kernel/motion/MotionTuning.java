package org.firstinspires.ftc.teamcode.kernel.motion;

public final class MotionTuning {
    public boolean fieldCentric = true;
    public double aimKp = 1.2;
    public double aimKd = 0.08;
    public double aimMaxTurn = 0.8;
    public double turnToleranceRadians = Math.toRadians(2);
    public double driverTakeoverThreshold = 0.05;
    public double goToMinDistance = 0.5;
}
