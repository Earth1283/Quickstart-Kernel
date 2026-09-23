package org.firstinspires.ftc.teamcode.kernel.motion;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

import java.util.ArrayList;
import java.util.List;

final class FakeFollower implements FollowerPort {
    Pose pose = new Pose(0, 0, 0);
    Path following;
    Pose holding;
    boolean finishCurrentPathOnNextUpdate;
    boolean holdAtPathEnd = true;
    double[] lastManual;
    final List<Path> followed = new ArrayList<>();
    int stops;

    @Override
    public void follow(Path path) {
        following = path;
        holding = null;
        followed.add(path);
    }

    @Override
    public void hold(Pose pose) {
        holding = pose;
        following = null;
    }

    @Override
    public void manual(double forward, double left, double counterClockwise) {
        lastManual = new double[]{forward, left, counterClockwise};
        following = null;
        holding = null;
    }

    @Override
    public void stop() {
        stops++;
        following = null;
        holding = null;
    }

    @Override
    public void setPose(Pose pose) {
        this.pose = pose;
    }

    @Override
    public Pose pose() {
        return pose;
    }

    @Override
    public boolean following() {
        return following != null;
    }

    @Override
    public boolean holding() {
        return holding != null;
    }

    @Override
    public void update() {
        if (finishCurrentPathOnNextUpdate && following != null) {
            finishCurrentPathOnNextUpdate = false;
            if (holdAtPathEnd) holding = following.endPose();
            following = null;
        }
    }
}
