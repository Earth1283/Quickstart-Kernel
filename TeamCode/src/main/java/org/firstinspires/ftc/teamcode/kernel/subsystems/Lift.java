package org.firstinspires.ftc.teamcode.kernel.subsystems;

import org.firstinspires.ftc.teamcode.kernel.Subsystem;
import org.firstinspires.ftc.teamcode.kernel.events.Event;

public interface Lift extends Subsystem {
    Event<Level> AT_TARGET = Event.of("lift.atTarget");

    enum Level { GROUND, LOW, HIGH }

    void goTo(Level level);

    Level target();

    boolean isMoving();
}
