package org.firstinspires.ftc.teamcode.kernel.motion;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

interface FollowerPort {
    void follow(Path path);

    void hold(Pose pose);

    void manual(double forward, double left, double counterClockwise);

    void stop();

    void setPose(Pose pose);

    Pose pose();

    boolean following();

    boolean holding();

    void update();
}
