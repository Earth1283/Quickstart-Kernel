package org.firstinspires.ftc.teamcode.kernel.drivers;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

import org.firstinspires.ftc.teamcode.kernel.Kernel;
import org.firstinspires.ftc.teamcode.kernel.subsystems.Flywheel;
import org.firstinspires.ftc.teamcode.kernel.telemetry.Report;

import java.util.concurrent.TimeUnit;

public final class MotorFlywheel implements Flywheel {
    public static final class Config {
        public String motorName = "flywheel";
        public DcMotorSimple.Direction direction = DcMotorSimple.Direction.FORWARD;
        public double toleranceTicksPerSecond = 40;
        public long settleMillis = 100;
    }

    private final Kernel kernel;
    private final DcMotorEx motor;
    private final Config config;

    private double target;
    private boolean targetDirty;
    private long withinToleranceSince;
    private boolean ready;

    public MotorFlywheel(Kernel kernel, Config config) {
        this.kernel = kernel;
        this.config = config;
        this.motor = kernel.device(DcMotorEx.class, config.motorName);
        motor.setDirection(config.direction);
        motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }

    @Override
    public Flywheel spinTo(double ticksPerSecond) {
        if (ticksPerSecond == target) return this;
        target = ticksPerSecond;
        targetDirty = true;
        withinToleranceSince = 0;
        ready = false;
        return this;
    }

    @Override
    public void spinDown() {
        spinTo(0);
    }

    @Override
    public double target() {
        return target;
    }

    @Override
    public double velocity() {
        return motor.getVelocity();
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public void update() {
        if (targetDirty) {
            motor.setVelocity(target);
            targetDirty = false;
        }
        if (target == 0 || ready) return;
        long now = kernel.nanoTime();
        if (Math.abs(velocity() - target) > config.toleranceTicksPerSecond) {
            withinToleranceSince = 0;
            return;
        }
        if (withinToleranceSince == 0) withinToleranceSince = now;
        if (now - withinToleranceSince >= TimeUnit.MILLISECONDS.toNanos(config.settleMillis)) {
            ready = true;
            kernel.emit(READY, target);
        }
    }

    @Override
    public void report(Report report) {
        report.data("velocity", velocity());
        report.data("target", target);
        report.data("ready", ready);
    }

    @Override
    public void stop() {
        motor.setPower(0);
        target = 0;
        ready = false;
    }
}
