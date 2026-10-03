package org.firstinspires.ftc.teamcode.kernel.drivers;

import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit;
import org.firstinspires.ftc.teamcode.kernel.Kernel;
import org.firstinspires.ftc.teamcode.kernel.subsystems.Intake;
import org.firstinspires.ftc.teamcode.kernel.telemetry.Report;

import java.util.concurrent.TimeUnit;

public final class MotorIntake implements Intake {
    public static final class Config {
        public String motorName = "intake";
        public DcMotorSimple.Direction direction = DcMotorSimple.Direction.FORWARD;
        public double power = 1.0;
        public double jamAmps = 8.0;
        public long jamMillis = 250;
    }

    private final Kernel kernel;
    private final DcMotorEx motor;
    private final Config config;

    private Mode mode = Mode.IDLE;
    private boolean powerDirty = true;
    private long overCurrentSince;
    private boolean reportedJam;

    public MotorIntake(Kernel kernel, Config config) {
        this.kernel = kernel;
        this.config = config;
        this.motor = kernel.device(DcMotorEx.class, config.motorName);
        motor.setDirection(config.direction);
    }

    @Override
    public void in() {
        setMode(Mode.IN);
    }

    @Override
    public void out() {
        setMode(Mode.OUT);
    }

    @Override
    public void idle() {
        setMode(Mode.IDLE);
    }

    private void setMode(Mode mode) {
        if (mode == this.mode) return;
        this.mode = mode;
        powerDirty = true;
        overCurrentSince = 0;
        reportedJam = false;
    }

    @Override
    public Mode mode() {
        return mode;
    }

    @Override
    public void update() {
        if (powerDirty) {
            motor.setPower(mode == Mode.IN ? config.power : mode == Mode.OUT ? -config.power : 0);
            powerDirty = false;
        }
        if (mode == Mode.IN) watchForJam();
    }

    // Current is its own hub transaction, so it's only read while the intake is actually pulling in.
    private void watchForJam() {
        if (reportedJam) return;
        long now = kernel.nanoTime();
        if (motor.getCurrent(CurrentUnit.AMPS) < config.jamAmps) {
            overCurrentSince = 0;
            return;
        }
        if (overCurrentSince == 0) overCurrentSince = now;
        if (now - overCurrentSince >= TimeUnit.MILLISECONDS.toNanos(config.jamMillis)) {
            reportedJam = true;
            kernel.emit(JAMMED);
        }
    }

    @Override
    public void report(Report report) {
        report.data("mode", mode);
    }

    @Override
    public void stop() {
        motor.setPower(0);
        powerDirty = true;
    }
}
