package org.firstinspires.ftc.teamcode.kernel.motion;

import com.pedropathing.math.Pose;
import com.pedropathing.math.Vector2D;
import com.pedropathing.paths.Path;

import org.firstinspires.ftc.teamcode.kernel.Awaitable;
import org.firstinspires.ftc.teamcode.kernel.events.Event;
import org.firstinspires.ftc.teamcode.kernel.input.Stick;

public interface Motion extends Awaitable {
    Event<Path> PATH_DONE = Event.of("motion.pathDone");
    Event<Void> QUEUE_EMPTY = Event.of("motion.queueEmpty");
    Event<Double> TURN_DONE = Event.of("motion.turnDone");

    enum Mode { IDLE, DRIVE, FOLLOW, HOLD, TURN }

    // Must be called every loop while driving: input is consumed each tick, so a loop that stops
    // calling drive() stops the robot. Near-zero input doesn't interrupt a path, turn, or hold.
    void drive(double forward, double left, double counterClockwise);

    default void drive(Stick translate, Stick rotate) {
        drive(translate.up(), translate.left(), rotate.left());
    }

    // Scales driver input only; paths, turns and aiming keep full speed.
    void setSpeedScale(double scale);

    Motion follow(Path path);

    Motion queue(Path... paths);

    // Straight line from wherever the robot is now, turning to the target's heading along the way.
    Motion goTo(Pose target);

    Motion turnBy(double radians);

    void aimTo(Vector2D target);

    void stopAiming();

    boolean isAiming();

    Motion turnTo(double heading);

    Motion hold();

    void cancel();

    Pose pose();

    void setPose(Pose pose);

    void resetDriverForward();

    boolean isBusy();

    @Override
    default boolean isSettled() {
        return !isBusy();
    }

    int queued();

    Mode mode();
}
