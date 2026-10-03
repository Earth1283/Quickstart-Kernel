package org.firstinspires.ftc.teamcode.kernel.telemetry;

import org.firstinspires.ftc.robotcore.external.Telemetry;

public final class SdkTelemetrySink implements TelemetrySink {
    private final Telemetry telemetry;

    public SdkTelemetrySink(Telemetry telemetry) {
        this.telemetry = telemetry;
    }

    @Override
    public void line(String text) {
        telemetry.addLine(text);
    }

    @Override
    public void update() {
        telemetry.update();
    }
}
