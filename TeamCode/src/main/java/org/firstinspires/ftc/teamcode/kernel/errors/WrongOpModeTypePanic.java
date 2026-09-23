package org.firstinspires.ftc.teamcode.kernel.errors;

public class WrongOpModeTypePanic extends KernelPanic {
    public WrongOpModeTypePanic(String method, Class<?> opMode) {
        super("robot." + method + "() blocks, so it only works in a LinearOpMode, but " + opMode.getSimpleName()
                + " is an iterative OpMode. Poll the condition in loop() instead, or extend LinearOpMode.");
    }
}
