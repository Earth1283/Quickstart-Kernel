package org.firstinspires.ftc.teamcode.kernel.errors;

public class KernelPanic extends RuntimeException {
    public KernelPanic(String message) {
        super(message);
    }

    public KernelPanic(String message, Throwable cause) {
        super(message, cause);
    }
}
