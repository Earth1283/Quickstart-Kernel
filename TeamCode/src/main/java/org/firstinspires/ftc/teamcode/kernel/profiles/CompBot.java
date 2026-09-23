package org.firstinspires.ftc.teamcode.kernel.profiles;

import com.pedropathing.algorithm.Foresight;
import com.pedropathing.algorithm.ForesightConfig;
import com.pedropathing.follower.Follower;
import com.pedropathing.revhub.drivetrains.Mecanum;
import com.pedropathing.revhub.drivetrains.MecanumConfig;
import com.pedropathing.revhub.localizers.PinpointConfig;
import com.pedropathing.revhub.localizers.PinpointLocalizer;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.kernel.Kernel;
import org.firstinspires.ftc.teamcode.kernel.drivers.MotorLift;
import org.firstinspires.ftc.teamcode.kernel.drivers.ServoClaw;
import org.firstinspires.ftc.teamcode.kernel.errors.ProfileMisconfiguredPanic;
import org.firstinspires.ftc.teamcode.kernel.subsystems.Claw;
import org.firstinspires.ftc.teamcode.kernel.subsystems.Lift;

public class CompBot implements RobotProfile {
    // Paste the output of the Tuning OpMode's procedures over these nulls.
    public static MecanumConfig drivetrainConfig = null;
    public static PinpointConfig localizerConfig = null;
    public static ForesightConfig foresightConfig = null;

    @Override
    public Follower follower(HardwareMap hardwareMap) {
        requireTuned("drivetrainConfig", drivetrainConfig, "Mecanum Tuner");
        requireTuned("localizerConfig", localizerConfig, "Pinpoint Tuner");
        requireTuned("foresightConfig", foresightConfig, "Foresight Tuner");
        return new Follower(
                new PinpointLocalizer(hardwareMap, localizerConfig),
                new Mecanum(hardwareMap, drivetrainConfig),
                new Foresight(foresightConfig));
    }

    private void requireTuned(String field, Object config, String tuner) {
        if (config != null) return;
        throw new ProfileMisconfiguredPanic(name() + "." + field + " is not set. Run the " + tuner
                + " from the Tuning OpMode and paste its output into profiles/" + name() + ".java.");
    }

    @Override
    public Claw claw(Kernel kernel) {
        return new ServoClaw(kernel, "claw", 0.75, 0.30);
    }

    @Override
    public Lift lift(Kernel kernel) {
        MotorLift.Config config = new MotorLift.Config();
        config.motorName = "lift";
        config.ticks.put(Lift.Level.GROUND, 0);
        config.ticks.put(Lift.Level.LOW, 900);
        config.ticks.put(Lift.Level.HIGH, 2100);
        return new MotorLift(kernel, config);
    }
}
