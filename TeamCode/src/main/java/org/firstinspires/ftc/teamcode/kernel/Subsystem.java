package org.firstinspires.ftc.teamcode.kernel;

public interface Subsystem {
    default void update() {}

    // Must be idempotent and must not throw: it runs during panics and OpMode shutdown.
    void stop();
}
