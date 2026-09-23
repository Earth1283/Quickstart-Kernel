package org.firstinspires.ftc.teamcode.kernel.profiles;

import com.pedropathing.follower.Follower;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.kernel.Kernel;
import org.firstinspires.ftc.teamcode.kernel.motion.MotionTuning;
import org.firstinspires.ftc.teamcode.kernel.subsystems.Claw;
import org.firstinspires.ftc.teamcode.kernel.subsystems.Lift;

// Return null from a subsystem factory when this robot doesn't have that mechanism.
public interface RobotProfile {
    Follower follower(HardwareMap hardwareMap);

    default Claw claw(Kernel kernel) {
        return null;
    }

    default Lift lift(Kernel kernel) {
        return null;
    }

    default MotionTuning motionTuning() {
        return new MotionTuning();
    }

    default double stickDeadband() {
        return 0.05;
    }

    default String name() {
        return getClass().getSimpleName();
    }
}
