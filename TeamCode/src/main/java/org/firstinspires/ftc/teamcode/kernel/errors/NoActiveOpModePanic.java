package org.firstinspires.ftc.teamcode.kernel.errors;

public class NoActiveOpModePanic extends KernelPanic {
    public NoActiveOpModePanic() {
        super("new Robot() was called with no OpMode running. Create the Robot inside runOpMode() or init(), "
                + "not in a field initializer, a static block, or a background thread started before the OpMode.");
    }
}
