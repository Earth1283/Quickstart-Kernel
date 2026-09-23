package org.firstinspires.ftc.teamcode.kernel;

import com.pedropathing.math.Pose;

// Static so it survives from one OpMode to the next (but not an app restart).
public final class PoseStore {
    private static Pose savedFromAutonomous;

    private PoseStore() {}

    static synchronized void save(Pose pose) {
        savedFromAutonomous = pose;
    }

    static synchronized Pose take() {
        Pose pose = savedFromAutonomous;
        savedFromAutonomous = null;
        return pose;
    }

    public static synchronized Pose peek() {
        return savedFromAutonomous;
    }
}
