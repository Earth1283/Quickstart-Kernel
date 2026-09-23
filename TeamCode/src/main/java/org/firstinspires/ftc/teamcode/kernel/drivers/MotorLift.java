package org.firstinspires.ftc.teamcode.kernel.drivers;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

import org.firstinspires.ftc.teamcode.kernel.Kernel;
import org.firstinspires.ftc.teamcode.kernel.subsystems.Lift;

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
        this.kernel = kernel;
        this.config = config;
        this.motor = kernel.device(DcMotorEx.class, config.motorName);
        motor.setDirection(config.direction);
        motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motor.setTargetPosition(ticksFor(Level.GROUND));
        motor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
    }

    private int ticksFor(Level level) {
        Integer ticks = config.ticks.get(level);
        return ticks == null ? 0 : ticks;
    }

    @Override
    public void goTo(Level level) {
        if (level == target) return;
        target = level;
        targetDirty = true;
        reportedArrival = false;
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
    public void stop() {
        motor.setPower(0);
        targetDirty = true;
    }
}
