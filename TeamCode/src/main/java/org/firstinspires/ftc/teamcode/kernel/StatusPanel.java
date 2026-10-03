package org.firstinspires.ftc.teamcode.kernel;

import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.kernel.init.InitLog;
import org.firstinspires.ftc.teamcode.kernel.init.InitResult;
import org.firstinspires.ftc.teamcode.kernel.input.Input;
import org.firstinspires.ftc.teamcode.kernel.input.Pad;
import org.firstinspires.ftc.teamcode.kernel.motion.Motion;
import org.firstinspires.ftc.teamcode.kernel.telemetry.KernelTelemetry;
import org.firstinspires.ftc.teamcode.kernel.time.LoopTimer;
import org.firstinspires.ftc.teamcode.kernel.time.MatchClock;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class StatusPanel {
    private final Kernel kernel;
    private final Motion motion;
    private final Input input;
    private final InitLog initLog;
    private final MatchClock matchClock;
    private final LoopTimer loopTimer;
    private final boolean autonomous;

    StatusPanel(Kernel kernel, Motion motion, Input input, InitLog initLog, MatchClock matchClock, LoopTimer loopTimer,
                boolean autonomous) {
        this.kernel = kernel;
        this.motion = motion;
        this.input = input;
        this.initLog = initLog;
        this.matchClock = matchClock;
        this.loopTimer = loopTimer;
        this.autonomous = autonomous;
    }

    List<String> render(Alliance alliance) {
        List<String> lines = new ArrayList<>();
        lines.add(header(alliance));
        if (matchClock.isStarted()) runningLines(lines);
        else preMatchLines(lines);
        subsystemLines(lines);
        return lines;
    }

    private String header(Alliance alliance) {
        String phase = matchClock.isStarted() ? clock(matchClock.secondsRemaining()) + " left" : "INIT";
        return String.format(Locale.US, "KERNEL %s | %s | %s | %s", kernel.profileName(), alliance, autonomous ? "Auto" : "TeleOp", phase);
    }

    private static String clock(double seconds) {
        int whole = (int) Math.ceil(seconds);
        return String.format(Locale.US, "%d:%02d", whole / 60, whole % 60);
    }

    private void runningLines(List<String> lines) {
        lines.add(String.format(Locale.US, "loop %.1f ms (%.0f Hz) | battery %.1f V",
                loopTimer.millis(), loopTimer.hertz(), kernel.batteryVoltage()));
        lines.add("pose " + KernelTelemetry.describe(motion.pose()) + " | " + motion.mode() + " | queued " + motion.queued());
        for (InitLog.Entry entry : initLog.entries()) {
            if (entry.result.status != InitResult.Status.OK) lines.add(initLine(entry));
        }
    }

    private void preMatchLines(List<String> lines) {
        lines.add(String.format(Locale.US, "init: %d ok, %d warn, %d fail",
                initLog.count(InitResult.Status.OK), initLog.count(InitResult.Status.WARN), initLog.count(InitResult.Status.FAIL)));
        for (InitLog.Entry entry : initLog.entries()) lines.add(initLine(entry));
        lines.add("gamepads: gp1 " + padState(input.gp1) + " | gp2 " + padState(input.gp2));
        lines.add("pose " + KernelTelemetry.describe(motion.pose()) + (autonomous ? "" : handoff()));
    }

    private static String padState(Pad pad) {
        return pad.isConnected() ? "ok" : "NOT CONNECTED";
    }

    private static String handoff() {
        Pose saved = PoseStore.peek();
        return saved == null ? "" : " | Auto handoff waiting: " + PoseStore.peekAlliance() + " " + KernelTelemetry.describe(saved);
    }

    private static String initLine(InitLog.Entry entry) {
        String detail = entry.result.detail.isEmpty() ? "" : " " + entry.result.detail;
        return String.format(Locale.US, "[%-4s] %s%s (%d ms)", entry.result.status, entry.name, detail, entry.millis);
    }

    private void subsystemLines(List<String> lines) {
        for (Map.Entry<Class<?>, Subsystem> installed : kernel.installed().entrySet()) {
            String name = installed.getKey().getSimpleName().toLowerCase(Locale.US);
            installed.getValue().report(KernelTelemetry.scoped(name, lines));
        }
    }
}
