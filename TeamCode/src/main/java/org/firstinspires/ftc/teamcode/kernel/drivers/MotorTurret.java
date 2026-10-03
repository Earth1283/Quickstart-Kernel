package org.firstinspires.ftc.teamcode.kernel.drivers;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

import org.firstinspires.ftc.teamcode.kernel.Kernel;
import org.firstinspires.ftc.teamcode.kernel.init.InitResult;
import org.firstinspires.ftc.teamcode.kernel.subsystems.Turret;
import org.firstinspires.ftc.teamcode.kernel.telemetry.Report;

public final class MotorTurret implements Turret {
    public static final class Config {
        public String motorName = "turret";
        public DcMotorSimple.Direction direction = DcMotorSimple.Direction.FORWARD;
        public double ticksPerRadian = 1.0;
        public double minRadians = -Math.PI;
        public double maxRadians = Math.PI;
        public double toleranceRadians = Math.toRadians(1.5);
        public double power = 0.8;
    }

    private final Kernel kernel;
    private final DcMotorEx motor;
    private final Config config;

    private double target;
    private boolean targetDirty = true;
    private boolean reportedArrival = true;

    public MotorTurret(Kernel kernel, Config config) {
        this.kernel = kernel;
        this.config = config;
        this.motor = kernel.device(DcMotorEx.class, config.motorName);
        motor.setDirection(config.direction);
        motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motor.setTargetPosition(0);
        motor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
    }

    @Override
    public InitResult init() {
        return InitResult.ok("encoder zeroed; turret must start centered");
    }

    @Override
    public Turret turnTo(double radians) {
        double clamped = Math.max(config.minRadians, Math.min(config.maxRadians, radians));
        if (clamped == target) return this;
        target = clamped;
        targetDirty = true;
        reportedArrival = false;
        return this;
    }

    @Override
    public double angle() {
        return motor.getCurrentPosition() / config.ticksPerRadian;
    }

    @Override
    public double target() {
        return target;
    }

    @Override
    public boolean isMoving() {
        return Math.abs(angle() - target) > config.toleranceRadians;
    }

    @Override
    public void update() {
        if (targetDirty) {
            motor.setTargetPosition((int) Math.round(target * config.ticksPerRadian));
            motor.setPower(config.power);
            targetDirty = false;
        }
        if (!reportedArrival && !isMoving()) {
            reportedArrival = true;
            kernel.emit(AT_ANGLE, target);
        }
    }

    @Override
    public void report(Report report) {
        report.data("angle°", Math.toDegrees(angle()));
        report.data("target°", Math.toDegrees(target));
    }

    @Override
    public void stop() {
        motor.setPower(0);
        targetDirty = true;
    }
}
