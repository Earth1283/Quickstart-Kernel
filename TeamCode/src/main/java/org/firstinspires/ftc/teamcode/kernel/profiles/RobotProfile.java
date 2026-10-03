package org.firstinspires.ftc.teamcode.kernel.profiles;

import com.pedropathing.follower.Follower;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.kernel.Kernel;
import org.firstinspires.ftc.teamcode.kernel.Subsystem;
import org.firstinspires.ftc.teamcode.kernel.init.InitTask;
import org.firstinspires.ftc.teamcode.kernel.motion.MotionTuning;
import org.firstinspires.ftc.teamcode.kernel.subsystems.Claw;
import org.firstinspires.ftc.teamcode.kernel.subsystems.Flywheel;
import org.firstinspires.ftc.teamcode.kernel.subsystems.Intake;
import org.firstinspires.ftc.teamcode.kernel.subsystems.Lift;
import org.firstinspires.ftc.teamcode.kernel.subsystems.Rangefinder;
import org.firstinspires.ftc.teamcode.kernel.subsystems.Turret;

import java.util.Collections;
import java.util.List;
import java.util.Map;

// Return null from a subsystem factory when this robot doesn't have that mechanism.
public interface RobotProfile {
    Follower follower(HardwareMap hardwareMap);

    default Claw claw(Kernel kernel) {
        return null;
    }

    default Lift lift(Kernel kernel) {
        return null;
    }

    default Intake intake(Kernel kernel) {
        return null;
    }

    default Flywheel flywheel(Kernel kernel) {
        return null;
    }

    default Turret turret(Kernel kernel) {
        return null;
    }

    default Rangefinder rangefinder(Kernel kernel) {
        return null;
    }

    // For mechanisms the kernel has no interface for: reach them with robot.get(YourSubsystem.class).
    default Map<Class<? extends Subsystem>, Subsystem> custom(Kernel kernel) {
        return Collections.emptyMap();
    }

    // Run after the kernel's own checks, in order. Failures show on the init screen; critical ones abort.
    default List<InitTask> initTasks(Kernel kernel) {
        return Collections.emptyList();
    }

    default double lowBatteryVolts() {
        return 12.0;
    }

    default MotionTuning motionTuning() {
        return new MotionTuning();
    }

    default double stickDeadband() {
        return 0.05;
    }

    // Applied to every stick. Above 1 softens small deflections for fine control; 1 is linear.
    default double stickCurve() {
        return 1.0;
    }

    default String name() {
        return getClass().getSimpleName();
    }
}
