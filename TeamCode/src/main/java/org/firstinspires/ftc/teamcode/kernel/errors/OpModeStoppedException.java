package org.firstinspires.ftc.teamcode.kernel.errors;

// Extends InterruptedException so a linear Auto unwinds through runOpMode()'s existing throws clause.
public class OpModeStoppedException extends InterruptedException {
    public OpModeStoppedException() {
        super("OpMode stopped while the kernel was waiting");
    }
}
