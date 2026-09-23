package org.firstinspires.ftc.teamcode.kernel.motion;

import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

final class PedroFollowerPort implements FollowerPort {
    private final Follower follower;

    PedroFollowerPort(Follower follower) {
        this.follower = follower;
    }

    @Override
    public void follow(Path path) {
        follower.follow(path);
    }

    @Override
    public void hold(Pose pose) {
        follower.hold(pose);
    }

    @Override
    public void manual(double forward, double left, double counterClockwise) {
        follower.manual(forward, left, counterClockwise);
    }

    // Follower.stop() only takes effect on the next update(); cut the motors now.
    @Override
    public void stop() {
        follower.stop();
        follower.drivetrain.stop();
    }

    @Override
    public void setPose(Pose pose) {
        follower.setPose(pose);
    }

    @Override
    public Pose pose() {
        return follower.pose();
    }

    @Override
    public boolean following() {
        return follower.following();
    }

    @Override
    public boolean holding() {
        return follower.holding();
    }

    @Override
    public void update() {
        follower.update();
    }
}
