package org.firstinspires.ftc.teamcode.kernel.drivers;

import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.teamcode.kernel.Kernel;
import org.firstinspires.ftc.teamcode.kernel.errors.SubsystemUnavailableException;
import org.firstinspires.ftc.teamcode.kernel.subsystems.Claw;
import org.firstinspires.ftc.teamcode.kernel.subsystems.Lift;
import org.firstinspires.ftc.teamcode.kernel.telemetry.Report;

public final class ServoClaw implements Claw {
    private final Kernel kernel;
    private final Servo servo;
    private final double openPosition;
    private final double closedPosition;

    private boolean open;
    private boolean positionDirty = true;

    public ServoClaw(Kernel kernel, String servoName, double openPosition, double closedPosition) {
        this.kernel = kernel;
        this.servo = kernel.device(Servo.class, servoName);
        this.openPosition = openPosition;
        this.closedPosition = closedPosition;
    }

    @Override
    public void open() {
        if (liftIsMoving()) {
            kernel.emit(OPEN_REFUSED);
            return;
        }
        setOpen(true);
    }

    private boolean liftIsMoving() {
        try {
            return kernel.get(Lift.class).isMoving();
        } catch (SubsystemUnavailableException noLiftOnThisRobot) {
            return false;
        }
    }

    @Override
    public void close() {
        setOpen(false);
    }

    @Override
    public void toggle() {
        if (open) close();
        else open();
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    private void setOpen(boolean open) {
        positionDirty |= this.open != open;
        this.open = open;
    }

    @Override
    public void update() {
        if (!positionDirty) return;
        servo.setPosition(open ? openPosition : closedPosition);
        positionDirty = false;
    }

    @Override
    public void report(Report report) {
        report.data("open", open);
    }

    // A servo holds its last position; leaving it is safer than yanking it somewhere new.
    @Override
    public void stop() {}
}
