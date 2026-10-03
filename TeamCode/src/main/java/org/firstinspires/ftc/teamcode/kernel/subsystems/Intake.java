package org.firstinspires.ftc.teamcode.kernel.subsystems;

import org.firstinspires.ftc.teamcode.kernel.Subsystem;
import org.firstinspires.ftc.teamcode.kernel.events.Event;

public interface Intake extends Subsystem {
    Event<Void> JAMMED = Event.of("intake.jammed");

    enum Mode { IDLE, IN, OUT }

    void in();

    void out();

    void idle();

    Mode mode();
}
