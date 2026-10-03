package org.firstinspires.ftc.teamcode.kernel;

import org.firstinspires.ftc.teamcode.kernel.init.InitResult;
import org.firstinspires.ftc.teamcode.kernel.telemetry.Report;

public interface Subsystem {
    default void update() {}

    default InitResult init() {
        return InitResult.ok();
    }

    default void report(Report report) {}

    // Must be idempotent and must not throw: it runs during panics and OpMode shutdown.
    void stop();
}
