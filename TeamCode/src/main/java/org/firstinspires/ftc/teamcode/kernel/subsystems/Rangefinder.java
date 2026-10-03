package org.firstinspires.ftc.teamcode.kernel.subsystems;

import org.firstinspires.ftc.teamcode.kernel.Subsystem;
import org.firstinspires.ftc.teamcode.kernel.events.Event;

public interface Rangefinder extends Subsystem {
    Event<Void> DETECTED = Event.of("rangefinder.detected");
    Event<Void> CLEARED = Event.of("rangefinder.cleared");

    double inches();

    boolean isDetecting();
}
