package org.firstinspires.ftc.teamcode.kernel.subsystems;

import org.firstinspires.ftc.teamcode.kernel.Awaitable;
import org.firstinspires.ftc.teamcode.kernel.Subsystem;
import org.firstinspires.ftc.teamcode.kernel.events.Event;

public interface Flywheel extends Subsystem, Awaitable {
    Event<Double> READY = Event.of("flywheel.ready");

    Flywheel spinTo(double ticksPerSecond);

    void spinDown();

    double target();

    double velocity();

    boolean isReady();

    // A wheel told to stop never reports ready, so waiting on it would never end.
    @Override
    default boolean isSettled() {
        return target() == 0 || isReady();
    }
}
