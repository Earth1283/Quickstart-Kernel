package org.firstinspires.ftc.teamcode.kernel.subsystems;

import org.firstinspires.ftc.teamcode.kernel.Subsystem;
import org.firstinspires.ftc.teamcode.kernel.events.Event;

public interface Claw extends Subsystem {
    Event<Void> OPEN_REFUSED = Event.of("claw.openRefused");

    void open();

    void close();

    void toggle();

    boolean isOpen();
}
