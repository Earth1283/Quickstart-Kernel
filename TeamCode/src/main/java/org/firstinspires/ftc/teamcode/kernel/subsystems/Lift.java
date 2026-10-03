package org.firstinspires.ftc.teamcode.kernel.subsystems;

import org.firstinspires.ftc.teamcode.kernel.Awaitable;
import org.firstinspires.ftc.teamcode.kernel.Subsystem;
import org.firstinspires.ftc.teamcode.kernel.events.Event;

public interface Lift extends Subsystem, Awaitable {
    Event<Level> AT_TARGET = Event.of("lift.atTarget");

    enum Level { GROUND, LOW, HIGH }

    Lift goTo(Level level);

    Level target();

    boolean isMoving();

    @Override
    default boolean isSettled() {
        return !isMoving();
    }
}
