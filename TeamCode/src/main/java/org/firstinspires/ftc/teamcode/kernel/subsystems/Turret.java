package org.firstinspires.ftc.teamcode.kernel.subsystems;

import org.firstinspires.ftc.teamcode.kernel.Awaitable;
import org.firstinspires.ftc.teamcode.kernel.Subsystem;
import org.firstinspires.ftc.teamcode.kernel.events.Event;

public interface Turret extends Subsystem, Awaitable {
    Event<Double> AT_ANGLE = Event.of("turret.atAngle");

    // Radians relative to the robot's front, counterclockwise positive; clamped to the profile's limits.
    Turret turnTo(double radians);

    double angle();

    double target();

    boolean isMoving();

    @Override
    default boolean isSettled() {
        return !isMoving();
    }
}
