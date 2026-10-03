package org.firstinspires.ftc.teamcode.kernel.drivers;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

import org.firstinspires.ftc.teamcode.kernel.Kernel;
import org.firstinspires.ftc.teamcode.kernel.errors.ProfileMisconfiguredPanic;
import org.firstinspires.ftc.teamcode.kernel.init.InitResult;
import org.firstinspires.ftc.teamcode.kernel.subsystems.Lift;
import org.firstinspires.ftc.teamcode.kernel.telemetry.Report;

import java.util.EnumMap;
import java.util.Map;

public final class MotorLift implements Lift {
    public static final class Config {
        public String motorName = "lift";
        public DcMotorSimple.Direction direction = DcMotorSimple.Direction.FORWARD;
        public double power = 1.0;
        public int toleranceTicks = 15;
        public final Map<Level, Integer> ticks = new EnumMap<>(Level.class);
    }

    private final Kernel kernel;
    private final DcMotorEx motor;
    private final Config config;

    private Level target = Level.GROUND;
    private boolean targetDirty = true;
    private boolean reportedArrival = true;

    public MotorLift(Kernel kernel, Config config) {
        requireEveryLevel(config, kernel.profileName());
        this.kernel = kernel;
        this.config = config;
        this.motor = kernel.device(DcMotorEx.class, config.motorName);
        motor.setDirection(config.direction);
        motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motor.setTargetPosition(ticksFor(Level.GROUND));
        motor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
    }

    // A missing level would otherwise read as 0 ticks and send the lift to the floor.
    private static void requireEveryLevel(Config config, String profileName) {
        for (Level level : Level.values()) {
            if (config.ticks.containsKey(level)) continue;
            throw new ProfileMisconfiguredPanic(profileName + ": MotorLift config has no ticks for " + level
                    + ". Add config.ticks.put(Lift.Level." + level + ", <ticks>) to the profile.");
        }
    }

    private int ticksFor(Level level) {
        return config.ticks.get(level);
    }

    @Override
    public Lift goTo(Level level) {
        if (level == target) return this;
        target = level;
        targetDirty = true;
        reportedArrival = false;
        return this;
    }

    @Override
    public Level target() {
        return target;
    }

    @Override
    public boolean isMoving() {
        return Math.abs(motor.getCurrentPosition() - ticksFor(target)) > config.toleranceTicks;
    }

    @Override
    public void update() {
        if (targetDirty) {
            motor.setTargetPosition(ticksFor(target));
            motor.setPower(config.power);
            targetDirty = false;
        }
        if (!reportedArrival && !isMoving()) {
            reportedArrival = true;
            kernel.emit(AT_TARGET, target);
        }
    }

    @Override
    public InitResult init() {
        return InitResult.ok("encoder zeroed at GROUND");
    }

    @Override
    public void report(Report report) {
        report.data("target", target);
        report.data("moving", isMoving());
    }

    @Override
    public void stop() {
        motor.setPower(0);
        targetDirty = true;
    }
}
