package org.firstinspires.ftc.teamcode.kernel.motion;

import com.pedropathing.math.Pose;
import com.pedropathing.math.Vector2D;
import com.pedropathing.paths.Path;

import org.firstinspires.ftc.teamcode.kernel.events.Event;

public interface Motion {
    Event<Path> PATH_DONE = Event.of("motion.pathDone");
    Event<Void> QUEUE_EMPTY = Event.of("motion.queueEmpty");
    Event<Double> TURN_DONE = Event.of("motion.turnDone");

    enum Mode { IDLE, DRIVE, FOLLOW, HOLD, TURN }

    // Must be called every loop while driving: input is consumed each tick, so a loop that stops
    // calling drive() stops the robot. Near-zero input doesn't interrupt a path, turn, or hold.
    void drive(double forward, double left, double counterClockwise);

    void follow(Path path);

    void queue(Path... paths);

    void aimTo(Vector2D target);

    void stopAiming();

    boolean isAiming();

    void turnTo(double heading);

    void hold();

    void cancel();

    Pose pose();

    void setPose(Pose pose);

    void resetDriverForward();

    boolean isBusy();

    int queued();

    Mode mode();
}
